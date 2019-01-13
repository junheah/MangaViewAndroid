package ml.melun.mangaview.mangaview;

public class Comment {

    public Comment(String user, String ts, String icon, String content) {
        this.user = user;
        this.icon = icon;
        this.content = content;
        this.timestamp = ts;
    }
    public String getContent() {return content;}
    public String getUser() {return user;}
    public String getIcon() {return icon;}
    public String getTimestamp() { return timestamp;}

    String content, user, icon, timestamp;
}
