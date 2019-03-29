package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.fragment.ViewerPageFragment;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.DecodeTransform;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;

public class ViewerPagerAdapter extends FragmentStatePagerAdapter
{
    List<Fragment> fragments;
    FragmentManager fm;
    int width;
    Context context;
    PageInterface i;
    public ViewerPagerAdapter(FragmentManager fm, int width, Context context, PageInterface i) {
        super(fm);
        this.fm = fm;
        this.width = width;
        this.context = context;
        this.i = i;
        fragments = new ArrayList<>();
    }

    public void setManga(Manga m){
        fragments.clear();
        for(String s: m.getImgs()){
            fragments.add(new ViewerPageFragment().init(s, new Decoder(m.getSeed(), m.getId()), width, context, new PageInterface() {
                @Override
                public void onPageClick() {
                    i.onPageClick();
                }
            }));
        }
        notifyDataSetChanged();
    }
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
    @Override
    public android.support.v4.app.Fragment getItem(int position)
    {
        return fragments.get(position);
    }
    @Override
    public int getCount()
    {
        return fragments.size();
    }

}
