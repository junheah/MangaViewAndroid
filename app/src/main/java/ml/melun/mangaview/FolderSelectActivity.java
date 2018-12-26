package ml.melun.mangaview;

import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FolderSelectActivity extends AppCompatActivity {

    ListView dirList;
    Button select;
    String currentDir;
    ArrayList<String> folders;
    Preference p;
    Context context;
    int requestCode;
    ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_select);
        p= new Preference();
        currentDir = p.getHomeDir();
        dirList = this.findViewById(R.id.dirList);
        select = this.findViewById(R.id.dirSelectBtn);
        context = this;
        actionBar = getSupportActionBar();
        actionBar.setTitle(currentDir);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p.setHomeDir(currentDir);
                Toast.makeText(context,"설정 완료!",Toast.LENGTH_LONG).show();
                finish();
            }
        });
        folders = refresh();
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                folders );

        dirList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position>0){
                    //select dir
                    currentDir += '/'+folders.get(position);
                    try {
                        folders.clear();
                        arrayAdapter.notifyDataSetChanged();
                        folders.addAll(refresh());
                        arrayAdapter.notifyDataSetChanged();
                    }catch (Exception e){
                        Toast.makeText(context,"접근이 불가능한 디렉토리 입니다.",Toast.LENGTH_SHORT).show();
                        currentDir = currentDir.substring(0,currentDir.lastIndexOf("/"));
                        folders.clear();
                        arrayAdapter.notifyDataSetChanged();
                        folders.addAll(refresh());
                        arrayAdapter.notifyDataSetChanged();
                    }

                }else{
                    //parent
                    if(currentDir.matches("/sdcard")) {

                    }else {
                        currentDir = currentDir.substring(0, currentDir.lastIndexOf("/"));
                        if (currentDir.length() < 2) currentDir = "/";
                        folders.clear();
                        arrayAdapter.notifyDataSetChanged();
                        folders.addAll(refresh());
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
                dirList.smoothScrollToPosition(0);
            }
        });
        dirList.setAdapter(arrayAdapter);
    }

    public ArrayList<String> refresh(){
        File[] files = new File(currentDir).listFiles();
        Arrays.sort(files);
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add("..");
        for(File f: files){
            if(f.isDirectory()) tmp.add(f.getName());
        }
        actionBar.setTitle(currentDir);
        return tmp;
    }
}
