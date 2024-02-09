package ml.melun.mangaview;

public class Notice{
    int id = -1;
    String title, date, content;

    Notice(int id, String title, String date, String content){
        this.id = id;
        this.title = title;
        this.date = date;
        this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Notice)obj).getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }
}
