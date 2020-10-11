package ml.melun.mangaview.model;

import androidx.annotation.Nullable;

import ml.melun.mangaview.mangaview.Manga;

public class PageItem{
    public PageItem(int index, String img, Manga manga) {
        this.index = index;
        this.img = img;
        this.manga = manga;
    }

    @Override
    public int hashCode() {
        return manga.getId()*10000 + index;
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
    public String img;
    public Manga manga;
};