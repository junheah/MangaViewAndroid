package ml.melun.mangaview.model;

import androidx.annotation.Nullable;

import ml.melun.mangaview.mangaview.Manga;

public class PageItem{
    public static final int FIRST = 0;
    public static final int SECOND = 1;
    public PageItem(int index, String img, Manga manga) {
        this.index = index;
        this.img = img;
        this.manga = manga;
        this.side = FIRST;
    }
    public PageItem(int index, String img, Manga manga, int side){
        this.index = index;
        this.img = img;
        this.manga = manga;
        this.side = side;
    }

    @Override
    public int hashCode() {
        return manga.getId()*10000 + index*10 + this.side;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof PageItem){
            PageItem p = (PageItem)obj;
            return p.index == this.index && p.manga.equals(this.manga);
        }else
            return false;
    }

    public int index;
    public int side;
    public String img;
    public Manga manga;
}