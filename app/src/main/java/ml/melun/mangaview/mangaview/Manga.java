package ml.melun.mangaview.mangaview;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.Response;

import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.Title.LOAD_CAPTCHA;
import static ml.melun.mangaview.mangaview.Title.LOAD_OK;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

    /*
    mode:
    0 = online
    1 = offline - old
    2 = offline - old(moa) (title.data)
    3 = offline - latest(toki) (title.gson)
    4 = offline - new(moa) (title.gson)
     */

public class Manga {
    int baseMode = base_comic;

    public Manga(int i, String n, String d, int baseMode) {
        id = i;
        name = n;
        date = d;
        this.baseMode = baseMode;
    }

    public int getBaseMode() {
        return this.baseMode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addThumb(String src) {
        thumb = src;
    }

    public String getDate() {
        return date;
    }

    public void setImgs(List<String> imgs) {
        this.imgs = imgs;
    }

    public String getThumb() {
        if (thumb == null) return "";
        return thumb;
    }

    public int fetch(CustomHttpClient client) {
        return fetch(client, true, null);
    }

    public int fetch(CustomHttpClient client, Map<String, String> cookies) {
        return fetch(client, false, cookies);
    }

    public int fetch(CustomHttpClient client, boolean doLogin, Map<String, String> cookies) {
        mode = 0;
        imgs = new ArrayList<>();
        eps = new ArrayList<>();
        comments = new ArrayList<>();
        bcomments = new ArrayList<>();
        int tries = 0;

        while (imgs.size() == 0 && tries < 2) {
            Response r = client.mget(  baseModeStr(baseMode) + '/' + id, false, cookies);
            try {
                if (r.code() == 302 && r.header("location").contains("captcha.php")) {
                    return LOAD_CAPTCHA;
                }
                String body = r.body().string();
                r.close();
                if (body.contains("Connect Error: Connection timed out")) {
                    //adblock : try again
                    r.close();
                    tries = 0;
                    continue;
                }

                Document d = Jsoup.parse(body);

                System.out.println(body);

                //name
                name = d.selectFirst("div.toon-title").ownText();

                //temp title
                Element navbar = d.selectFirst("div.toon-nav");
                int tid = Integer.parseInt(navbar.select("a")
                        .last()
                        .attr("href")
                        .split(baseModeStr(baseMode) + '/')[1]
                        .split("\\?")[0]);

                if (title == null) title = new Title(name, "", "", null, "", tid, baseMode);

                //eps
                for (Element e : navbar.selectFirst("select").select("option")) {
                    String idstr = e.attr("value");
                    if (idstr.length() > 0)
                        eps.add(new Manga(Integer.parseInt(idstr), e.ownText(), "", baseMode));
                }

                //imgs
                String script = d.select("div.view-padding").get(1).selectFirst("script").data();
                StringBuilder encodedData = new StringBuilder();
                encodedData.append('%');
                for (String line : script.split("\n")) {
                    if (line.contains("html_data+=")) {
                        encodedData.append(line.substring(line.indexOf('\'') + 1, line.lastIndexOf('\'')).replaceAll("[.]", "%"));
                    }
                }
                if (encodedData.lastIndexOf("%") == encodedData.length() - 1)
                    encodedData.deleteCharAt(encodedData.length() - 1);
                String imgdiv = URLDecoder.decode(encodedData.toString(), "UTF-8");

                Document id = Jsoup.parse(imgdiv);
                for (Element e : id.select("img")) {
                    String style = e.attr("style");
                    if (style.length() == 0) {
                        boolean flag = false;
                        for (Attribute a : e.attributes()) {
                            if (a.getKey().contains("data")) {
                                String img = a.getValue();
                                if (!img.isEmpty() && !img.contains("blank") && !img.contains("loading")) {
                                    flag = true;
                                    if (img.startsWith("/"))
                                        imgs.add(client.getUrl() + img);
                                    else
                                        imgs.add(img);
                                }
                            }
                        }
                        if (!flag) {
                            String img = e.attr("src");
                            if (!img.isEmpty() && !img.contains("blank") && !img.contains("loading")) {
                                if (img.startsWith("/"))
                                    imgs.add(client.getUrl() + img);
                                else
                                    imgs.add(img);
                            }
                        }
                    }
                }

                //comments
                Element commentdiv = d.selectFirst("div#viewcomment");


                try {
                    for (Element e : commentdiv.selectFirst("section#bo_vc").select("div.media")) {
                        try {
                            comments.add(parseComment(e));
                        } catch (Exception e3) {
                            e3.printStackTrace();
                        }

                    }
                    for (Element e : commentdiv.selectFirst("section#bo_vcb").select("div.media")) {
                        try {
                            bcomments.add(parseComment(e));
                        } catch (Exception e3) {
                            e3.printStackTrace();
                        }

                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            } catch (Exception e2) {
                e2.printStackTrace();
            }
            if (r != null) {
                r.close();
            }
            tries++;
        }
        return LOAD_OK;
    }

    private Comment parseComment(Element e) {
        String user;
        String icon;
        String content;
        String timestamp;
        int likes;
        int level;
        String lvlstr;
        int indent;
        String indentstr;
        //indent
        indentstr = e.attr("style");
        if (indentstr.length() > 0)
            indent = Integer.parseInt(indentstr.substring(indentstr.lastIndexOf(':') + 1, indentstr.lastIndexOf('p'))) / 64;
        else
            indent = 0;

        //icon
        Element icone = e.selectFirst(".media-object");
        if (icone.is("img"))
            icon = icone.attr("src");
        else
            icon = "";

        Element header = e.selectFirst("div.media-heading");
        Element userSpan = header.selectFirst("span.member");
        user = userSpan.ownText();
        if (userSpan.hasClass("guest"))
            level = 0;
        else {
            lvlstr = userSpan.selectFirst("img").attr("src");
            level = Integer.parseInt(lvlstr.substring(lvlstr.lastIndexOf('/') + 1, lvlstr.lastIndexOf('.')));
        }
        timestamp = header.selectFirst("span.media-info").ownText();

        Element cbody = e.selectFirst("div.media-content");
        content = cbody.selectFirst("div:not([class])").ownText();
        likes = Integer.parseInt(cbody.selectFirst("div.cmt-good-btn").selectFirst("span").ownText());
        return new Comment(user, timestamp, icon, content, indent, likes, level);
    }


    public List<Manga> getEps() {
        return eps;
    }

    public Title getTitle() {
        return title;
    }

    public List<String> getImgs(Context context) {
        if (mode != 0) {
            if (imgs == null) {
                imgs = new ArrayList<>();
                //is offline : read image list
                if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                    DocumentFile[] offimgs = DocumentFile.fromTreeUri(context, Uri.parse(offlinePath)).listFiles();
                    Arrays.sort(offimgs, (documentFile, t1) -> documentFile.getName().compareTo(t1.getName()));
                    for (DocumentFile f : offimgs) {
                        imgs.add(f.getUri().toString());
                    }
                } else {
                    File[] offimgs = new File(offlinePath).listFiles();
                    Arrays.sort(offimgs);
                    for (File img : offimgs) {
                        imgs.add(img.getAbsolutePath());
                    }
                }
            }
        }
        return imgs;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<Comment> getBestComments() {
        return bcomments;
    }

    public int getSeed() {
        return seed;
    }

    public String toString() {
        JSONObject tmp = new JSONObject();
        try {
            tmp.put("id", id);
            tmp.put("name", name);
            tmp.put("date", date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp.toString();
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Manga) obj).getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    public void setOfflinePath(String offlinePath) {
        this.offlinePath = offlinePath;
    }

    public String getOfflinePath() {
        return this.offlinePath;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getUrl() {
        return '/' + baseModeStr(baseMode) + '/' + id;
    }

    public boolean useBookmark() {
        return id > 0 && (mode == 0 || mode == 3);
    }

    public boolean isOnline() {
        return id > 0 && mode == 0;
    }

    public Manga nextEp() {
        if (isOnline()) {
            if (eps == null || eps.size() == 0) {
                return null;
            } else {
                int index = eps.indexOf(this);
                if (index > 0) return eps.get(index - 1);
                else return null;
            }
        } else {
            return nextEp;
        }
    }

    public Manga prevEp() {
        if (isOnline()) {
            if (eps == null || eps.size() == 0) {
                return null;
            } else {
                int index = eps.indexOf(this);
                if (index < eps.size() - 1) return eps.get(index + 1);
                else return null;
            }
        } else {
            return prevEp;
        }
    }

    public void setPrevEp(Manga m) {
        this.prevEp = m;
    }

    public void setNextEp(Manga m) {
        this.nextEp = m;
    }

    private final int id;
    String name;
    List<Manga> eps;
    List<String> imgs;
    List<Comment> comments, bcomments;
    String offlinePath;
    String thumb;
    Title title;
    String date;
    int seed;
    int mode;
    Listener listener;
    Manga nextEp, prevEp;

    public interface Listener {
        void setMessage(String msg);
    }
}

