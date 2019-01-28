package ml.melun.mangaview;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.adapter.mainTagAdapter;

public class AdvSearchActivity extends AppCompatActivity {
    Preference preference;
    List<String> tags,names,releases;
    mainTagAdapter ta, ra, na;
    Context context;
    RecyclerView tr, rr, nr;
    LinearLayoutManager tm, rm, nm;
    Spinner searchMethod;
    Button search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preference = new Preference(this);
        if(preference.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adv_search);
        context = this;

        searchMethod = this.findViewById(R.id.searchMethod);
        search = this.findViewById(R.id.advSearch);

        if(preference.getDarkTheme()) searchMethod.setPopupBackgroundResource(R.color.colorDarkWindowBackground);

        nr = this.findViewById(R.id.search_name);
        rr = this.findViewById(R.id.search_release);
        tr = this.findViewById(R.id.search_tags);

        tm = new LinearLayoutManager(context);
        rm = new LinearLayoutManager(context);
        nm = new LinearLayoutManager(context);

        tm.setOrientation(LinearLayoutManager.HORIZONTAL);
        rm.setOrientation(LinearLayoutManager.HORIZONTAL);
        nm.setOrientation(LinearLayoutManager.HORIZONTAL);

        nr.setLayoutManager(nm);
        rr.setLayoutManager(rm);
        tr.setLayoutManager(tm);

        names = Arrays.asList(getResources().getStringArray(R.array.tag_name));
        releases = Arrays.asList(getResources().getStringArray(R.array.tag_release));
        tags = Arrays.asList(getResources().getStringArray(R.array.tag_genre));
        na = new mainTagAdapter(context, names,1);
        //na.setSingleSelect(true);
        ra = new mainTagAdapter(context, releases, 2);
        ta = new mainTagAdapter(context, tags, 0);

        nr.setAdapter(na);
        rr.setAdapter(ra);
        tr.setAdapter(ta);
        //clicked name 1
        na.setClickListener(new mainTagAdapter.tagOnclick() {
            @Override
            public void onClick(int position, String value) {
                na.toggleSelect(position);
            }
        });
        //clicked release 2
        ra.setClickListener(new mainTagAdapter.tagOnclick() {
            @Override
            public void onClick(int position, String value) {
                ra.toggleSelect(position);
            }
        });
        //clicked tag 3
        ta.setClickListener(new mainTagAdapter.tagOnclick() {
            @Override
            public void onClick(int position, String value) {
                ta.toggleSelect(position);
            }
        });

        //search submit
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = "search_type="+(searchMethod.getSelectedItemPosition()+1)+"&_1="+na.getSelectedIndex()+"&_2="+ra.getSelectedIndex()+"&_3="+ta.getSelectedValues();
                Intent searchActivity = new Intent(context, TagSearchActivity.class);
                searchActivity.putExtra("query", query);
                searchActivity.putExtra("mode", 6);
                startActivity(searchActivity);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
