package ml.melun.mangaview.mangaview;

import java.util.List;

public class SingleSearch {
    int srows = 100;
    String baseUrl = "/bbs/search.php?";
    int page = 1;
    boolean last = false;
    String query;
    List<Title> result;
    int baseMode;
    int count;

    SingleSearch(String query, int baseMode){
        this.query = query;
        this.baseMode = baseMode;
    }

    SingleSearch(String query, int baseMode, int count){
        this.query = query;
        this.baseMode = baseMode;
        this.count = count;
    }


}
