package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.Parcelable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ml.melun.mangaview.fragment.ViewerPageFragment;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;

import static ml.melun.mangaview.MainApplication.p;

public class ViewerPagerAdapter extends FragmentStatePagerAdapter
{
    List<Fragment> fragments;
    FragmentManager fm;
    int width;
    Context context;
    PageInterface itf;
    public ViewerPagerAdapter(FragmentManager fm, int width, Context context, PageInterface i) {
        super(fm);
        this.fm = fm;
        this.width = width;
        this.context = context;
        this.itf = i;
        fragments = new ArrayList<>();
    }

    public void setManga(Manga m){
        fragments.clear();
        List<String> imgs = m.getImgs();
        if (p.getPageRtl()) Collections.reverse(imgs);
        for(int i = 0; i<imgs.size(); i++){
            String s = imgs.get(i);

            fragments.add(ViewerPageFragment.create(s, new Decoder(m.getSeed(), m.getId()), width, context, new PageInterface() {
                @Override
                public void onPageClick() {
                    itf.onPageClick();
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
    public Fragment getItem(int position)
    {
        return fragments.get(position);
    }
    @Override
    public int getCount()
    {
        return fragments.size();
    }

    @Override
    public Parcelable saveState()
    {
        return null;
    }

}
