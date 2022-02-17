package ml.melun.mangaview.mangaview;

import java.util.List;

public class UpdatedManga extends Manga{
    String author;
    List<String> tag;

    UpdatedManga(int i, String n, String d, int baseMode, String author, List<String> tag){
        super(i, n, d, baseMode);
        this.author = author;
        this.tag = tag;
    }

    public String getAuthor() {
        return author;
    }

    public List<String> getTag() {
        return tag;
    }
}
