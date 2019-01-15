package ml.melun.mangaview.mangaview;

public class Comment {

    public Comment(String user, String ts, String icon, String content, int indent, int likes) {
        this.user = user;
        this.icon = icon;
        this.content = content;
        this.timestamp = ts;
        this.indent = indent;
        this.likes = likes;
    }
    public String getContent() {return content;}
    public String getUser() {return user;}
    public String getIcon() {return icon;}
    public String getTimestamp() { return timestamp;}
    public int getIndent() { return indent; }
    public int getLikes() { return likes; }

    String content, user, icon, timestamp;
    int indent = 0;
    int likes = 0;
}
