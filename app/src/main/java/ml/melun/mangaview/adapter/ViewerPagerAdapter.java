package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.fragment.ViewerPageFragment;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;

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
        List<String> imgs1 = m.getImgs(true);
        for(int i = 0; i<imgs.size(); i++){
            String s = imgs.get(i);
            String s1 = "";
            if(imgs1!=null && imgs1.size() == imgs.size()){
                s1 = imgs1.get(i);
            }

            fragments.add(new ViewerPageFragment().init(s,s1, new Decoder(m.getSeed(), m.getId()), width, context, new PageInterface() {
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
