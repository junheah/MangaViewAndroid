package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.PopupMenu;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.ReservedChars;
import static ml.melun.mangaview.Utils.checkWriteable;
import static ml.melun.mangaview.Utils.deleteRecursive;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.activity.SettingsActivity.prefExtension;

public class FolderSelectActivity extends AppCompatActivity {

    ListView dirList;
    Button select, storageSelectBtn;
    File currentDir;
    ArrayList<String> listContent;
    Context context;
    int requestCode;
    ActionBar actionBar;
    File defDir;
    ArrayAdapter<String> arrayAdapter;
    TextView path;
    Boolean dark;
    EditText input;
    public final static int MODE_FOLDER_SELECT = 0;
    public final static int MODE_FILE_SELECT = 1;
    public final static int MODE_FILE_SAVE = 2;
    int mode = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_select);

        Intent intent = getIntent();
        mode = intent.getIntExtra("mode", MODE_FOLDER_SELECT);
        String title = intent.getStringExtra("title");
        if(title == null)
            title = "폴더 선택기";


        context = this;
        currentDir = new File(p.getHomeDir());
        defDir = new File("/sdcard/MangaView/saved/");
        if(!currentDir.exists()){
            p.setHomeDir(defDir.getAbsolutePath());
            showPopup(context, "알림","설정된 폴더를 찾을 수 없습니다. 기본 폴더로 이동 합니다.");
            currentDir = defDir;
            if(!defDir.exists()) currentDir.mkdirs();
        }

        dirList = this.findViewById(R.id.dirList);
        select = this.findViewById(R.id.dirSelectBtn);
        input = this.findViewById(R.id.fileNameInput);

        actionBar = getSupportActionBar();
        actionBar.setTitle(title);
        actionBar.setDisplayHomeAsUpEnabled(true);
        path = this.findViewById(R.id.path);
        //adapter create
        listContent = refresh();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listContent);


        if(mode == MODE_FILE_SAVE || mode == MODE_FILE_SELECT) {
            if(mode == MODE_FILE_SAVE)
                select.setText("저장");

            select.setEnabled(false);
            input.setFilters(new InputFilter[] {new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int s, int e, Spanned spanned, int i2, int i3) {
                    for (int i = s; i < e; i++) {
                        if (ReservedChars.indexOf(source.charAt(i))>-1) {
                            return "";
                        }
                    }
                    return null;
                }
            }});
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if(charSequence.length() == 0)
                        select.setEnabled(false);
                    else
                        select.setEnabled(true);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }else{
            input.setVisibility(View.GONE);
        }
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkWriteable(currentDir)) {
                    Intent resultIntent = new Intent();
                    String path;
                    if(mode == MODE_FILE_SAVE){
                        String filename = input.getText().toString();
                        if(!filename.endsWith(prefExtension)) {
                            filename += prefExtension;
                            input.setText(filename);
                        }
                        File target = new File(currentDir, filename);
                        if(target.exists()) {
                            showPopup(context, "오류", "파일이 이미 존재합니다.");
                            return;
                        }
                        path = target.getAbsolutePath();
                    }else if(mode == MODE_FILE_SELECT){
                        File target = new File(currentDir, input.getText().toString());
                        if(!target.exists()) {
                            showPopup(context, "오류", "파일이 존재하지 않습니다.");
                            return;
                        }
                        path = target.getAbsolutePath();
                    }else{
                        path = currentDir.getAbsolutePath();
                    }

                    resultIntent.putExtra("path", path);
                    setResult(0, resultIntent);
                    finish();
                }else{
                    showPopup(context, "알림","쓰기가 불가능한 위치 입니다. 다른 위치를 선택해 주세요.");
                }
            }
        });

        dirList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position>0){
                    //select dir
                    // is folder
                    if(listContent.get(position).indexOf('/')>-1) {
                        currentDir = new File(currentDir, listContent.get(position));
                        if(mode == MODE_FILE_SAVE || mode == MODE_FILE_SELECT)
                            input.setText("");
                        populate();
                    }else if(mode == MODE_FILE_SAVE || mode == MODE_FILE_SELECT){
                        input.setText(listContent.get(position));
                    }
                }else{
                    //parent
                    if(currentDir.getAbsolutePath().length()>1) currentDir = currentDir.getParentFile();
                    populate();
                }
            }
        });
        dirList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(position>0) {
                    String target = listContent.get(position);
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    File folder = new File(currentDir, target);
                                    if (deleteRecursive(folder))
                                        Toast.makeText(context, "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                    else showPopup(context, "알림", "삭제를 실패했습니다.");
                                    populate();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder builder;
                    if (dark) builder = new AlertDialog.Builder(context, R.style.darkDialog);
                    else builder = new AlertDialog.Builder(context);
                    builder.setMessage(target + " 을(를) 삭제 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                            .setNegativeButton("아니오", dialogClickListener).show();
                }
                return true;
            }
        });
        dirList.setAdapter(arrayAdapter);
        storageSelectBtn = this.findViewById(R.id.storageSelectBtn);
        storageSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File[] dirs =  ContextCompat.getExternalFilesDirs(context, null);
                dirs[0] = defDir; //기본 내부 저장소로 설정
                PopupMenu popup = new PopupMenu(FolderSelectActivity.this, storageSelectBtn);
                for(int i=0;i<dirs.length;i++){
                    if(i==0) popup.getMenu().add(i+".내부 저장소");
                    else popup.getMenu().add(i+".외부 저장소 ");
                }
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int index = Integer.parseInt(item.getTitle().toString().split("\\.")[0]);
                        currentDir = dirs[index];
                        if(!currentDir.exists()) currentDir.mkdirs();
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
                input.setHint("폴더 이름");
                alert.setView(input);
                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        //확인 시
                        String name = input.getText().toString();
                        if(name.length()>0) {
                            File f = new File(currentDir,name);
                            if(!f.exists()) {
                                if(f.mkdir()) Toast.makeText(context,"성공", Toast.LENGTH_SHORT).show();
                                else showPopup(context,"알림","폴더 생성 실패");
                            }else showPopup(context,"알림","폴더 생성 실패");
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
        try {
            for (File f : files) {
                if (f.isDirectory()) {
                    tmp.add(f.getName() + '/');
                }else {
                    if (mode == MODE_FILE_SELECT || mode == MODE_FILE_SAVE) {
                        if(f.getName().toLowerCase().endsWith(prefExtension))
                            tmp.add(f.getName());
                    }
                }
            }

            Collections.sort(tmp, new Comparator<String>() {
                // dir goes top file goes bottom
                @Override
                public int compare(String t, String t1) {
                    if(t.indexOf('/')>-1 && t1.indexOf('/')>-1){
                        return t.compareTo(t1);
                    }else if(t.indexOf('/')>-1){
                        return -1;
                    }else if(t1.indexOf('/')>-1){
                        return 1;
                    }else{
                        return t.compareTo(t1);
                    }
                }
            });
            tmp.add(0, "..");
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
            listContent.clear();
            arrayAdapter.notifyDataSetChanged();
            listContent.addAll(refresh());
            arrayAdapter.notifyDataSetChanged();
        }catch (Exception e){
            showPopup(context,"알림","접근이 불가능한 디렉토리 입니다.");
            currentDir = currentDir.getParentFile();
            listContent.clear();
            arrayAdapter.notifyDataSetChanged();
            listContent.addAll(refresh());
            arrayAdapter.notifyDataSetChanged();
        }
        dirList.setSelection(0);
    }
}
