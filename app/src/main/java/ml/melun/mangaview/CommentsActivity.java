package ml.melun.mangaview;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import ml.melun.mangaview.adapter.commentsAdapter;
import ml.melun.mangaview.fragment.tabFragment;
import ml.melun.mangaview.mangaview.Comment;

public class CommentsActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    Preference p;
    ArrayList<Comment> comments, bcomments;
    public commentsAdapter adapter, badapter;
    Context context;
    TabLayout tab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        //swipe = this.findViewById(R.id.commentSwipe);
        Intent intent = getIntent();
        tab = this.findViewById(R.id.tab_layout);

        String gsonData = intent.getStringExtra("comments");
        if(gsonData.length()>0){
            Gson gson = new Gson();
            comments = gson.fromJson(gsonData,new TypeToken<ArrayList<Comment>>(){}.getType());
            adapter = new commentsAdapter(context, comments);
            getSupportActionBar().setTitle("댓글 "+comments.size());
        }else{
            getSupportActionBar().setTitle("댓글 없음");
        }

        gsonData = intent.getStringExtra("bestComments");
        if(gsonData.length()>0){
            Gson gson = new Gson();
            bcomments = gson.fromJson(gsonData,new TypeToken<ArrayList<Comment>>(){}.getType());
            badapter = new commentsAdapter(context, bcomments);
            //((TextView)toolbar.findViewById(R.id.comments_title)).setText("댓글 ["+comments.size()+"]");
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());



        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        tab.addTab(tab.newTab().setText("베스트 댓글"));
        tab.addTab(tab.newTab().setText("전체 댓글"));

        mViewPager.addOnPageChangeListener(new
                TabLayout.TabLayoutOnPageChangeListener(tab));
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //
            }
        });

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            tabFragment tab = new tabFragment();
            switch(position){
                case 0:
                    //best
                    tab.setAdapter(badapter);
                    return tab;
                case 1:
                    //comments
                    tab.setAdapter(adapter);
                    return tab;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }
    }
}
