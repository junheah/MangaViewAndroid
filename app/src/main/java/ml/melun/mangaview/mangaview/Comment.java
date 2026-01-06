package ml.melun.mangaview.mangaview;

public class Comment {

    public Comment(String user, String ts, String icon, String content, int indent, int likes, int level) {
        this.user = user;
        this.icon = icon;
        this.content = content;
        this.timestamp = ts;
        this.indent = indent;
        this.likes = likes;
        this.level = level;
    }

    public String getContent() {
        return content;
    }

    public String getUser() {
        return user;
    }

    public String getIcon() {
        return icon;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getIndent() {
        return indent;
    }

    public int getLikes() {
        return likes;
    }

    public int getLevel() {
        return level;
    }

    String content, user, icon, timestamp;
    int indent = 0;
    int likes = 0;
    int level = 0;
}
