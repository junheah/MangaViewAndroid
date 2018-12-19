package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //variables
    private ViewFlipper contentHolder;
    private EditText searchBox;
    private Button searchBtn;
    public Context context = this;
    ProgressDialog pd;
    Search search;
    TitleAdapter searchAdapter;
    RecyclerView searchResult;
    private int version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //custom var init starts here
        contentHolder = this.findViewById(R.id.contentHolder);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //code starts here

        refreshViews(0);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            // Handle the main action
            contentHolder.setDisplayedChild(0);
        } else if (id == R.id.nav_search) {
            // Handle the search action
            contentHolder.setDisplayedChild(1);
        }else{
            //don't refresh views
            if(id==R.id.nav_update){
                //check update
                updateCheck u = new updateCheck();
                u.execute();
            }else if(id==R.id.nav_share){

            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        refreshViews(id);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void refreshViews(int id){
        //set views according to selected layout
        if(id==R.id.nav_main){
            //main content
        }else if(id==R.id.nav_search){
            //search content
            searchBox = this.findViewById(R.id.searchBox);
            searchBtn = this.findViewById(R.id.searchBtn);
            searchResult = this.findViewById(R.id.searchResult);
            searchResult.setLayoutManager(new LinearLayoutManager(this));
            searchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String query = searchBox.getText().toString();
                    if(query.length()>0) {
                        searchManga sm = new searchManga();
                        sm.execute(query);
                    }
                }
            });
        }
    }

    private class searchManga extends AsyncTask<String,String,String>{
        protected void onPreExecute(){
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
        }
        protected String doInBackground(String... params){
            String query = params[0];
            search = new Search(query);
            ArrayList<Title> titles = search.getResult();
//            int i=0;
//            for(Title t:titles) {
//                System.out.println(i+ ". " + t.getName()+"  |  " + t.getThumb());
//                i++;
//            }
            searchAdapter = new TitleAdapter(context,titles);
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            searchResult.setAdapter(searchAdapter);
            searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    // start intent : Episode viewer

                    Title selected = searchAdapter.getItem(position);
                    System.out.println("onItemClick position: " + position);
                    Intent episodeView= new Intent(context, EpisodeActivity.class);
                    episodeView.putExtra("title",selected.getName());
                    startActivity(episodeView);
                }
//
//                @Override
//                public void onItemLongClick(int position, View v) {
//                    Log.d(TAG, "onItemLongClick pos = " + position);
//                }
            });
            if (pd.isShowing()){
                pd.dismiss();
            }
        }
    }

    public String httpsGet(String urlin){
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlin);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "*");
            connection.setRequestProperty("Accept", "*");
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private class updateCheck extends AsyncTask<Void, Integer, Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("업데이트 확인중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/version.json");
                JSONObject data = new JSONObject(rawdata);
                int lver = data.getInt("version");
                String link = data.getString("link");
                if(version<lver){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                    return 1;
                }
            }catch(Exception e){
                return -1;
            }return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch(result){
                case -1:
                    Toast.makeText(getApplicationContext(), "오류가 발생했습니다. 나중에 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    Toast.makeText(getApplicationContext(), "최신버전 입니다.", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), "새로운 버전을 찾았습니다. 다운로드 페이지로 이동합니다.", Toast.LENGTH_LONG).show();
                    break;
            }
            if (pd.isShowing()){
                pd.dismiss();
            }
        }
    }
}
