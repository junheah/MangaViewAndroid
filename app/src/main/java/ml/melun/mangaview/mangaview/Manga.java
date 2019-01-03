package ml.melun.mangaview.mangaview;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;

public class Manga {
    public Manga(int i, String n) {
        id = i;
        name = n;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getImg(int index){return imgs.get(index);}
    public void addThumb(String src){
        thumb = src;
    }

    public String getThumb() {
        if(thumb == null) return "";
        return thumb;
    }


    public void fetch() {
        imgs = new ArrayList<>();
        eps = new ArrayList<>();
        int tries = 0;
        //get images
        while(imgs.size()==0 && tries < 3) {
            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("https://mangashow.me/bbs/board.php?bo_table=msm_manga&wr_id="+id);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Encoding", "*");
                connection.setRequestProperty("Accept", "*");
                connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                //StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    if(line.contains("var img_list")) {
                        String imgStr = line;
                        if(imgStr!=null) {
                            String[] imgStrs = imgStr.split("\"");
                            //remove backslash
                            for (int i = 1; i < imgStrs.length; i += 2) {
                                imgs.add(imgStrs[i].replace("\\",""));
                            }
                        }
                    }else if(line.contains("var only_chapter")){
                        String epsStr = line;
                        String[] epsStrs = epsStr.split("\"");
                        //remove backslash
                        for (int i = 3; i < epsStrs.length; i += 4) {
                            eps.add(new Manga(Integer.parseInt(epsStrs[i]),epsStrs[i-2]));
                        }
                    }else if(line.contains("<h1>")){
                        name = line.substring(line.indexOf('>')+1,line.lastIndexOf('<'));
                    }else if(line.contains("manga_name") && title==null){
                        String name = line.substring(line.indexOf("manga_name")+11,line.indexOf("class=")-2);
                        title = new Title(java.net.URLDecoder.decode(name, "UTF-8"),"","",new ArrayList<String>());
                    }
                    if(imgs.size()>0 && eps.size()>0) break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //get string inside double quotes : 1,3,5,7,9,...

            /*
            Document items = Jsoup.connect("https://mangashow.me/bbs/board.php?bo_table=msm_manga&wr_id=" + id)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .get();
            //System.out.println(items.html());
            //Deprecated since 18/12/25

            for (Element e : items.selectFirst("div.view-content.scroll-viewer").select("img")) {
                imgs.add(e.attr("src"));
            }
            */
            //now its contained inside javascript var
            tries++;
        }

    }

    public ArrayList<Manga> getEps() {
        return eps;
    }

    public Title getTitle() {
        return title;
    }

    public ArrayList<String> getImgs(){
        return imgs;
    }
    public String toString(){
        JSONObject tmp = new JSONObject();
        try {
            tmp.put("id", id);
            tmp.put("name", name);
        }catch (Exception e){

        }
        return tmp.toString();
    }

    private int id;
    String name;
    ArrayList<Manga> eps;
    private ArrayList<String> imgs;
    String thumb;
    Title title;
}

