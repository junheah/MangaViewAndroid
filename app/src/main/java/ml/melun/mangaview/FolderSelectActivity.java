package ml.melun.mangaview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class FolderSelectActivity extends AppCompatActivity {

    ListView dirList;
    Button select, changeSource;
    File currentDir;
    ArrayList<String> folders;
    Preference p;
    Context context;
    int requestCode;
    ActionBar actionBar;
    String defDir = "/sdcard/MangaView/saved/";
    ArrayAdapter<String> arrayAdapter;
    TextView path;
    Boolean dark;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_select);
        currentDir = new File(p.getHomeDir());
        File hd = new File(p.getHomeDir());
        if(!hd.exists()) hd.mkdir();

        dirList = this.findViewById(R.id.dirList);
        select = this.findViewById(R.id.dirSelectBtn);
        context = this;
        actionBar = getSupportActionBar();
        actionBar.setTitle("폴더 선택기");
        actionBar.setDisplayHomeAsUpEnabled(true);
        path = this.findViewById(R.id.path);
        folders = refresh();
        arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                folders );

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkWriteable()) {
                    p.setHomeDir(currentDir.getAbsolutePath() + '/');
                    Toast.makeText(context, "설정 완료!", Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    Toast.makeText(context, "쓰기가 불가능한 위치 입니다. 다른 위치를 설정해 주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });

        dirList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position>0){
                    //select dir
                    currentDir = new File(currentDir, folders.get(position));
                    populate();
                }else{
                    //parent
                    if(currentDir.getAbsolutePath().length()>1) currentDir = currentDir.getParentFile();
                    populate();
                }
            }
        });
        dirList.setAdapter(arrayAdapter);
        changeSource = this.findViewById(R.id.changeSourceBtn);
        changeSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File[] dirs =  ContextCompat.getExternalFilesDirs(context, null);
                dirs[0] = new File(defDir); //기본 내부 저장소로 설정
                PopupMenu popup = new PopupMenu(FolderSelectActivity.this,changeSource);
                for(int i=0;i<dirs.length;i++){
                    if(i==0) popup.getMenu().add(i+".내부 저장소");
                    else popup.getMenu().add(i+".외부 저장소 ");
                }
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int index = Integer.parseInt(item.getTitle().toString().split("\\.")[0]);
                        System.out.println("ppppppp"+index);
                        currentDir = dirs[index];
                        populate();
                        return true;
                    }
                });
                popup.show(); //showing popup menu
            }
        });
        this.findViewById(R.id.createFolderBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create folder
                AlertDialog.Builder alert;
                if(dark) alert = new AlertDialog.Builder(context,R.style.darkDialog);
                else alert = new AlertDialog.Builder(context);
                alert.setTitle("새로운 폴더 생성");
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                alert.setView(input);
                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        //확인 시
                        String name = input.getText().toString();
                        if(name.length()>0) {
                            File f = new File(currentDir,name);
                            if(!f.exists()) {
                                if(f.mkdir()) Toast.makeText(context,"성공", Toast.LENGTH_SHORT).show();
                                else Toast.makeText(context,"실패", Toast.LENGTH_SHORT).show();
                            }else Toast.makeText(context,"실패", Toast.LENGTH_SHORT).show();
                            populate();
                        }
                    }
                });
                alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        //취소 시
                    }
                });
                alert.show();
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ArrayList<String> refresh(){
        File[] files = currentDir.listFiles();
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add("..");
        try {
            Arrays.sort(files);
            for (File f : files) {
                if (f.isDirectory()) tmp.add(f.getName());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //change actionbar text
        path.setText(currentDir.getAbsolutePath());
        path.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        path.setMarqueeRepeatLimit(-1);
        path.setSingleLine(true);
        path.setSelected(true);
        return tmp;
    }

    public void populate() {
        try {
            folders.clear();
            arrayAdapter.notifyDataSetChanged();
            folders.addAll(refresh());
            arrayAdapter.notifyDataSetChanged();
        }catch (Exception e){
            Toast.makeText(context,"접근이 불가능한 디렉토리 입니다.",Toast.LENGTH_SHORT).show();
            currentDir = currentDir.getParentFile();
            folders.clear();
            arrayAdapter.notifyDataSetChanged();
            folders.addAll(refresh());
            arrayAdapter.notifyDataSetChanged();
        }
        dirList.smoothScrollToPosition(0);
    }

    public boolean checkWriteable(){
        File tmp = new File(currentDir, "mangaViewTestFile");
        try{
            if(tmp.createNewFile()) tmp.delete();
            else return false;
        }catch (Exception e){
            return false;
        }
        return true;
    }

}
