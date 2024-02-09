package ml.melun.mangaview.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.UrlUpdater;
import ml.melun.mangaview.interfaces.StringCallback;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.Utils.readPreferenceFromFile;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.showStringInputPopup;
import static ml.melun.mangaview.Utils.showYesNoPopup;
import static ml.melun.mangaview.Utils.writePreferenceToFile;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SELECT;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FOLDER_SELECT;

public class SettingsActivity extends AppCompatActivity {

    //다운로드 위치 설정
    //데이터 절약 모드 : 외부 이미지 로드 안함
    //
    Context context;
    ConstraintLayout s_setHomeDir, s_resetHistory, s_dark, s_viewer, s_reverse, s_pageRtl, s_dataSave, s_tab, s_stretch, s_double, s_double_reverse;
    Spinner s_tab_spinner, s_viewer_spinner;
    Switch s_dark_switch, s_reverse_switch, s_pageRtl_switch, s_dataSave_switch, s_stretch_switch, s_double_switch, s_double_reverse_switch;
    Boolean dark;
    public static final String prefExtension = ".mvpref";
    public static final int RESULT_NEED_RESTART = 7;

    View.OnClickListener pbtnClear, nbtnClear, pbtnSet, nbtnSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();

        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        context = this;
        s_setHomeDir = this.findViewById(R.id.setting_dir);
        s_setHomeDir.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                // Choose a directory using the system's file picker.
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                Uri uri = Uri.parse(p.getHomeDir());
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                Toast.makeText(context, "다운로드 위치를 선택해 주세요", Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, MODE_FOLDER_SELECT);
            }else{
                Intent intent = new Intent(context, FolderSelectActivity.class);
                startActivityForResult(intent, MODE_FOLDER_SELECT);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        s_getSd = this.findViewById(R.id.setting_externalSd);
//        s_getSd.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                p.setHomeDir("/sdcard");
//            }
//        });
        s_resetHistory = this.findViewById(R.id.setting_reset);
        s_resetHistory.setOnClickListener(v -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        p.resetBookmark();
                        p.resetViewerBookmark();
                        p.resetRecent();
                        Toast.makeText(context, "초기화 되었습니다.", Toast.LENGTH_LONG).show();
                        setResult(RESULT_NEED_RESTART);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            };
            AlertDialog.Builder builder;
            if(dark) builder = new AlertDialog.Builder(context, R.style.darkDialog);
            else builder = new AlertDialog.Builder(context);
            builder.setMessage("최근 본 만화, 북마크 및 모든 만화 열람 기록이 사라집니다. 계속 하시겠습니까?\n(좋아요, 저장한 만화 제외)").setPositiveButton("네", dialogClickListener)
                    .setNegativeButton("아니오", dialogClickListener).show();
        });
        this.findViewById(R.id.setting_key).setOnClickListener(new View.OnClickListener() {
            int prevKeyCode;
            int nextKeyCode;
            InputCallback inputCallback = null;
            @Override
            public void onClick(View view) {
                prevKeyCode = p.getPrevPageKey();
                nextKeyCode = p.getNextPageKey();

                View v = getLayoutInflater().inflate(R.layout.content_key_set_popup, null);
                Button pbtn = v.findViewById(R.id.key_prev);
                Button nbtn = v.findViewById(R.id.key_next);
                TextView ptext = v.findViewById(R.id.key_prev_text);
                TextView ntext = v.findViewById(R.id.key_next_text);

                if(prevKeyCode == -1)
                    ptext.setText("-");
                else
                    ptext.setText(KeyEvent.keyCodeToString(prevKeyCode));
                if(nextKeyCode == -1)
                    ntext.setText("-");
                else
                    ntext.setText(KeyEvent.keyCodeToString(nextKeyCode));

                pbtnClear = view14 -> {
                    if(prevKeyCode == -1)
                        ptext.setText("-");
                    else
                        ptext.setText(KeyEvent.keyCodeToString(prevKeyCode));
                    inputCallback = null;
                    view14.setOnClickListener(pbtnSet);
                };
                nbtnClear = view13 -> {
                    if(nextKeyCode == -1)
                        ntext.setText("-");
                    else
                        ntext.setText(KeyEvent.keyCodeToString(nextKeyCode));
                    inputCallback = null;
                    view13.setOnClickListener(nbtnSet);
                };
                pbtnSet = view12 -> {
                    if(inputCallback == null) {
                        view12.setOnClickListener(pbtnClear);
                        ptext.setText("키를 입력해 주세요");
                        inputCallback = event -> {
                            prevKeyCode = event.getKeyCode();
                            ptext.setText(KeyEvent.keyCodeToString(prevKeyCode));
                            view12.setEnabled(true);
                            view12.setOnClickListener(pbtnSet);
                        };
                    }
                };
                nbtnSet = view1 -> {
                    if(inputCallback == null) {
                        view1.setOnClickListener(nbtnClear);
                        ntext.setText("키를 입력해 주세요");
                        inputCallback = event -> {
                            nextKeyCode = event.getKeyCode();
                            ntext.setText(KeyEvent.keyCodeToString(nextKeyCode));
                            view1.setEnabled(true);
                            view1.setOnClickListener(nbtnSet);
                        };
                    }
                };

                pbtn.setOnClickListener(pbtnSet);
                nbtn.setOnClickListener(nbtnSet);

                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setTitle("단축키 설정")
                        .setView(v)
                        .setOnKeyListener((dialogInterface, i, keyEvent) -> {
                            if(inputCallback != null){
                                if(keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                                    inputCallback.onKeyEvent(keyEvent);
                                    inputCallback = null;
                                }
                                return true;
                            }
                            return false;
                        })
                        .setNeutralButton("초기화", (dialogInterface, i) -> {
                            p.setPrevPageKey(-1);
                            p.setNextPageKey(-1);
                            inputCallback = null;
                        })
                        .setNegativeButton("취소", (dialogInterface, i) -> inputCallback = null)
                        .setPositiveButton("적용", (dialogInterface, i) -> {
                            inputCallback = null;
                            p.setNextPageKey(nextKeyCode);
                            p.setPrevPageKey(prevKeyCode);
                        })
                        .setOnCancelListener(dialogInterface -> inputCallback = null)
                        .show();
            }
        });

        s_dark = this.findViewById(R.id.setting_dark);
        s_dark_switch = this.findViewById(R.id.setting_dark_switch);
        s_dark_switch.setChecked(p.getDarkTheme());
        s_dark.setOnClickListener(v -> s_dark_switch.toggle());
        s_dark_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            p.setDarkTheme(isChecked);
            if(isChecked != dark) setResult(RESULT_NEED_RESTART);
            else setResult(RESULT_CANCELED);
        });

        s_viewer = this.findViewById(R.id.setting_viewer);
        s_viewer_spinner = this.findViewById(R.id.setting_viewer_spinner);
        if(dark) s_viewer_spinner.setPopupBackgroundResource(R.color.colorDarkWindowBackground);
        s_viewer_spinner.setSelection(p.getViewerType());
        s_viewer_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                p.setViewerType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });

        s_reverse = this.findViewById(R.id.setting_reverse);
        s_reverse_switch = this.findViewById(R.id.setting_reverse_switch);
        s_reverse_switch.setChecked(p.getReverse());
        s_reverse.setOnClickListener(v -> s_reverse_switch.toggle());
        s_reverse_switch.setOnCheckedChangeListener((buttonView, isChecked) -> p.setReverse(isChecked));
        s_pageRtl = this.findViewById(R.id.setting_pageRtl);
        s_pageRtl_switch = this.findViewById(R.id.setting_pageRtl_switch);
        s_pageRtl_switch.setChecked(p.getPageRtl());
        s_pageRtl.setOnClickListener(v -> s_pageRtl_switch.toggle());
        s_pageRtl_switch.setOnCheckedChangeListener((buttonView, isChecked) -> p.setPageRtl(isChecked));

        s_dataSave = this.findViewById(R.id.setting_dataSave);
        s_dataSave_switch = this.findViewById(R.id.setting_dataSave_switch);
        s_dataSave_switch.setChecked(p.getDataSave());
        s_dataSave.setOnClickListener(v -> s_dataSave_switch.toggle());
        s_dataSave_switch.setOnCheckedChangeListener((buttonView, isChecked) -> p.setDataSave(isChecked));

        s_tab = this.findViewById(R.id.setting_startTab);
        s_tab_spinner = this.findViewById(R.id.setting_startTab_spinner);
        if(dark) s_tab_spinner.setPopupBackgroundResource(R.color.colorDarkWindowBackground);
        s_tab_spinner.setSelection(p.getStartTab());
        s_tab_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                p.setStartTab(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });


        this.findViewById(R.id.setting_license).setOnClickListener(v -> {
            Intent l = new Intent(context,LicenseActivity.class);
            startActivity(l);
        });


        this.findViewById(R.id.setting_url).setOnClickListener(v -> urlSettingPopup(context, p));

        s_stretch = this.findViewById(R.id.setting_stretch);
        s_stretch_switch = this.findViewById(R.id.setting_stretch_switch);
        s_stretch_switch.setChecked(p.getStretch());
        s_stretch.setOnClickListener(v -> s_stretch_switch.toggle());
        s_stretch_switch.setOnCheckedChangeListener((buttonView, isChecked) -> p.setStretch(isChecked));

        this.findViewById(R.id.setting_buttonLayout).setOnClickListener(view -> startActivity(new Intent(context, LayoutEditActivity.class)));

        this.findViewById(R.id.setting_dataExport).setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                Uri uri = Uri.parse(p.getHomeDir());
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                Toast.makeText(context, "백업 파일을 저장할 폴더를 선택해 주세요", Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, MODE_FILE_SAVE);
            }else{
                Intent intent = new Intent(context, FolderSelectActivity.class);
                intent.putExtra("mode", MODE_FILE_SAVE);
                intent.putExtra("title", "파일 저장");
                startActivityForResult(intent, MODE_FILE_SAVE);
            }
        });

        this.findViewById(R.id.setting_dataImport).setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                Uri uri = Uri.parse(p.getHomeDir());
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/*");
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                Toast.makeText(context, "백업 파일 선택", Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, MODE_FILE_SELECT);
            }else {
                Intent intent = new Intent(context, FolderSelectActivity.class);
                intent.putExtra("mode", MODE_FILE_SELECT);
                intent.putExtra("title", "파일 선택");
                startActivityForResult(intent, MODE_FILE_SELECT);
            }
        });

        s_double = this.findViewById(R.id.setting_double);
        s_double_switch = this.findViewById(R.id.setting_double_switch);
        s_double_switch.setChecked(p.getDoublep());
        s_double.setOnClickListener(v -> s_double_switch.toggle());
        s_double_switch.setOnCheckedChangeListener((buttonView, isChecked) -> p.setDoublep(isChecked));

        s_double_reverse = this.findViewById(R.id.setting_double_leftright);
        s_double_reverse_switch = this.findViewById(R.id.setting_double_leftright_switch);
        s_double_reverse_switch.setChecked(p.getDoublepReverse());
        s_double_reverse.setOnClickListener(v -> s_double_reverse_switch.toggle());
        s_double_reverse_switch.setOnCheckedChangeListener((buttonView, isChecked) -> p.setDoublepReverse(isChecked));

    }

    public static void urlSettingPopup(Context context, Preference p){
        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout switch_layout = new LinearLayout(context);
        switch_layout.setOrientation(LinearLayout.HORIZONTAL);
        switch_layout.setGravity(Gravity.RIGHT);
        switch_layout.setPadding(0,0,10,0);
        final TextView definputtext = new TextView(context);
        final EditText definput = new EditText(context);
        final TextView inputtext = new TextView(context);
        final EditText input = new EditText(context);
        final TextView toggle_lbl = new TextView(context);
        toggle_lbl.setText("URL 자동 설정");
        final Switch toggle = new Switch(context);

        definputtext.setText("기본 URL (숫자 없는 주소):");
        inputtext.setText("URL:");

        switch_layout.addView(toggle_lbl);
        switch_layout.addView(toggle);
        layout.addView(definputtext);
        layout.addView(definput);
        layout.addView(inputtext);
        layout.addView(input);
        layout.addView(switch_layout);

        toggle.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                definput.setEnabled(true);
                input.setEnabled(false);
                input.setText("...");
            }else{
                definput.setEnabled(false);
                input.setEnabled(true);
                input.setText(p.getUrl());
            }
        });

        toggle.setChecked(p.getAutoUrl());
        if(toggle.isChecked()){
            definput.setEnabled(true);
            input.setEnabled(false);
            input.setText("...");
        }else{
            definput.setEnabled(false);
            input.setEnabled(true);
            input.setText(p.getUrl());
        }

        input.setText(p.getUrl());
        input.setHint(p.getUrl());
        definput.setText(p.getDefUrl());
        definput.setHint(p.getDefUrl());

        AlertDialog.Builder builder;
        if(p.getDarkTheme()) builder = new AlertDialog.Builder(context,R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle("URL 설정")
                .setView(layout)
                .setPositiveButton("설정", (dialog, button) -> {
                    if(toggle.isChecked()){
                        // 자동 설정
                        if(definput.getText().length()>0)
                            p.setDefUrl(definput.getText().toString());
                        else
                            p.setDefUrl(definput.getHint().toString());
                        p.setAutoUrl(true);
                        new UrlUpdater(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }else {
                        // 수동 설정
                        p.setAutoUrl(false);
                        if (input.getText().length() > 0)
                            p.setUrl(input.getText().toString());
                        else
                            p.setUrl(input.getHint().toString());
                    }
                })
                .setNegativeButton("취소", (dialog, button) -> {
                    //do nothing
                })
                .show();
    }


    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                final Uri uri = data.getData();
                switch (requestCode) {
                    case MODE_FOLDER_SELECT:
                        getContentResolver().takePersistableUriPermission(uri, (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                        p.setHomeDir(uri.toString());
                        break;
                    case MODE_FILE_SAVE:
                        showStringInputPopup(context, "백업 파일 이름", s -> {
                            DocumentFile d = DocumentFile.fromTreeUri(context, uri);
                            if(!s.endsWith(".mvpref")) s += ".mvpref";

                            final DocumentFile target = d.findFile(s);
                            if(target != null){
                                String finalS = s;
                                showYesNoPopup(context, "파일이 이미 존재합니다.", "덮어 쓸까요?", (dialogInterface, i) -> {
                                    target.delete();
                                    if (writePreferenceToFile(context, d.createFile("application", finalS).getUri()))
                                        Toast.makeText(context, "내보내기 완료!", Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(context, "내보내기 실패", Toast.LENGTH_LONG).show();
                                }, null, null);
                            } else {
                                if (writePreferenceToFile(context, d.createFile("application", s).getUri()))
                                    Toast.makeText(context, "내보내기 완료!", Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(context, "내보내기 실패", Toast.LENGTH_LONG).show();
                            }
                        }, p.getDarkTheme());
                        break;
                    case MODE_FILE_SELECT:
                        showYesNoPopup(context, "데이터 불러오기", "이 작업은 되돌릴 수 없습니다.\n복원을 진행 하시겠습니까?", (dialogInterface, i) -> {
                            if (readPreferenceFromFile(p, context, uri)) {
                                setResult(RESULT_NEED_RESTART);
                                showPopup(context, "데이터 불러오기", "데이터 불러오기를 성공했습니다. 변경사항을 적용하기 위해 앱을 재시작 합니다.", (dialogInterface12, i1) -> finish(), dialogInterface1 -> finish());
                            } else
                                Toast.makeText(context, "불러오기 실패", Toast.LENGTH_LONG).show();

                        }, (dialogInterface, i) -> Toast.makeText(context,"취소되었습니다", Toast.LENGTH_SHORT).show(), dialogInterface -> Toast.makeText(context,"취소되었습니다", Toast.LENGTH_SHORT).show());
                        break;
                }
            }
        }else {
            if (data != null) {
                String path = data.getStringExtra("path");
                if (path != null) {
                    switch (requestCode) {
                        case MODE_FILE_SELECT:
                            if (readPreferenceFromFile(p, context, new File(path))) {
                                setResult(RESULT_NEED_RESTART);
                                showPopup(context, "데이터 불러오기", "데이터 불러오기를 성공했습니다. 변경사항을 적용하기 위해 앱을 재시작 합니다.", (dialogInterface, i) -> finish(), dialogInterface -> finish());
                            } else
                                Toast.makeText(context, "불러오기 실패", Toast.LENGTH_LONG).show();
                            break;
                        case MODE_FOLDER_SELECT:
                            p.setHomeDir(path);
                            Toast.makeText(context, "설정 완료!", Toast.LENGTH_LONG).show();
                            break;
                        case MODE_FILE_SAVE:
                            if (writePreferenceToFile(context, new File(path)))
                                Toast.makeText(context, "내보내기 완료!", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(context, "내보내기 실패", Toast.LENGTH_LONG).show();

                            break;
                    }
                }
            }
        }

    }

    private interface InputCallback{
        void onKeyEvent(KeyEvent event);
    }
}
