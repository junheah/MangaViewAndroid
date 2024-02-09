package ml.melun.mangaview.mangaview;

import java.util.ArrayList;
import java.util.Collection;

public class Ranking<E> extends ArrayList<E> {
    String name;
    public Ranking(String name){
        super();
        this.name = name;
    }
    public Ranking(Collection<? extends E> c, String name) {
        super(c);
        this.name = name;
    }
    public String getName(){
        return this.name;
    }
}
