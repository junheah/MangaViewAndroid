package ml.melun.mangaview.mangaview;

import java.util.List;

public class DownloadTitle extends MTitle {
    private List<Manga> eps;

    public DownloadTitle(Title t){
        super(t.getName(), t.getId(), t.getThumb(), t.getAuthor(), t.getTags(), t.getRelease());
        eps = t.getEps();
    }

    public List<Manga> getEps() {
        return eps;
    }

    public void setEps(List<Manga> eps) {
        this.eps = eps;
    }
}
