package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import okhttp3.Response;

public class Bookmark {
    List<MTitle> result;
    int page = -1;
    boolean last = false;

    public Bookmark(){

    }

    public int fetch(CustomHttpClient client){
        result = new ArrayList<>();
        //todo implement this
        return 0;
    }

    public boolean isLast() {
        return last;
    }

    public List<MTitle> getResult(){
        return this.result;
    }

    public static int importBookmark(Preference p, CustomHttpClient client){
        try {
            Bookmark b = new Bookmark();
            List<MTitle> bookmarks = new ArrayList<>();
            while (!b.isLast()) {
                if (b.fetch(client) == 0)
                    bookmarks.addAll(b.getResult());
                else
                    return 1;
            }

            for (MTitle t : bookmarks) {
                if (p.findFavorite(t) < 0)
                    p.toggleFavorite(t, 0);
            }
        }catch (Exception e){
            return 1;
        }
        return 0;
    }
}
