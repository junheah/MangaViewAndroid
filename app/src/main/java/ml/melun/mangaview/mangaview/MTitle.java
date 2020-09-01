package ml.melun.mangaview.mangaview;

import java.util.ArrayList;
import java.util.List;

public class MTitle{
    String name;
    int id;
    String thumb;
    String author;
    List<String> tags;
    String release;
    //public static final String[] releases = {"미분류","주간","격주","월간","격월/비정기","단편","단행본","완결"};
    public MTitle(){

    }
    public MTitle(String name, int id, String thumb, String author, List<String> tags, String release) {
        this.name = name;
        this.id = id;
        this.thumb = thumb;
        this.tags = tags;
        this.release = release;
        this.author = author;
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
        this.name = name;
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
        return new MTitle(name, id, thumb, author, tags, release);
    }

    @Override
    public boolean equals(Object obj) {
        return ((MTitle)obj).getId() == this.id;
    }
}
