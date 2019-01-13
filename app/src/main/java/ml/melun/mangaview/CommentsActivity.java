package ml.melun.mangaview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;

import ml.melun.mangaview.mangaview.Comment;
import ml.melun.mangaview.mangaview.Manga;

public class CommentsActivity extends AppCompatActivity {
    Intent intent;
    Context context;
    ArrayList<Comment> comments;
    SwipyRefreshLayout swipe;
    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        context = this;
        swipe = this.findViewById(R.id.commentSwipe);
        list = this.findViewById(R.id.commentList);
        intent = getIntent();


        String gsonData = intent.getStringExtra("comments");
        if(gsonData.length()>0){
            Gson gson = new Gson();
            comments = gson.fromJson(gsonData,new TypeToken<ArrayList<Comment>>(){}.getType());
            for(Comment comment: comments){
                System.out.println(comment.getContent());
            }
        }else{
            //no comments
        }
        swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                refresh();
            }
        });
    }

    public void refresh(){
        //refresh comments

    }

}
