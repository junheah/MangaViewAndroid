package ml.melun.mangaview.mangaview;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MTitle{
    String name;
    int id;
    String thumb;
    String author;
    List<String> tags;
    String release;
    String path;
    int baseMode = base_comic; // default is comic
    //public static final String[] releases = {"미분류","주간","격주","월간","격월/비정기","단편","단행본","완결"};
    public MTitle(){

    }
    public MTitle(String name, int id, String thumb, String author, List<String> tags, String release, int baseMode) {
        this.name = name.replace("\"", "");
        this.id = id;
        this.thumb = thumb;
        this.tags = tags;
        this.release = release;
        this.author = author;
        this.baseMode = baseMode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getBaseMode() {
        if(baseMode == base_auto)
            baseMode = base_comic;
        return baseMode;
    }

    public String getBaseModeStr(){
        return baseModeKorStr(baseMode);
    }

    public void setBaseMode(int baseMode) {
        this.baseMode = baseMode;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getThumb() {
        return thumb;
    }

    public String getAuthor() {
        if(author == null) return "";
        return author;
    }

    public List<String> getTags(){
        if(tags==null) return new ArrayList<>();
        return tags;
    }

    public String getRelease() {
        return release;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name.replace("\"", "");
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    @Override
    public MTitle clone() {
        return new MTitle(name, id, thumb, author, tags, release, baseMode);
    }

    public static final int base_auto = 0;
    public static final int base_comic = 1;
    public static final int base_webtoon = 2;

    public static String baseModeStr(int mode){
        switch(mode){
            case base_comic:
                return "comic";
            case base_webtoon:
                return "webtoon";
            default:
                return "comic";
        }
    }
    public static String baseModeKorStr(int mode){
        switch(mode){
            case base_comic:
                return "만화";
            case base_webtoon:
                return "웹툰";
            default:
                return "만화";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return name + " . " + id + " . " +  thumb + " . " + author + " . " + baseMode;
    }

    @Override
    public boolean equals(Object obj) {
        return ((MTitle)obj).getBaseMode() == this.baseMode && ((MTitle)obj).getId() == this.id ;
    }
}
