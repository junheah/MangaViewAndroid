package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
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

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.readPreferenceFromFile;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.writePreferenceToFile;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SELECT;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FOLDER_SELECT;

public class SettingsActivity extends AppCompatActivity {

    //다운로드 위치 설정
    //데이터 절약 모드 : 외부 이미지 로드 안함
    //
    Context context;
    ConstraintLayout s_setHomeDir, s_resetHistory, s_dark, s_viewer, s_reverse, s_dataSave, s_tab, s_stretch;
    Spinner s_tab_spinner, s_viewer_spinner;
    Switch s_dark_switch, s_reverse_switch, s_dataSave_switch, s_stretch_switch;
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
        s_setHomeDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        s_resetHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                p.resetBookmark();
                                p.resetViewerBookmark();
                                p.resetRecent();
                                Toast.makeText(context,"초기화 되었습니다.",Toast.LENGTH_LONG).show();
                                setResult(RESULT_NEED_RESTART);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(context, R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setMessage("최근 본 만화, 북마크 및 모든 만화 열람 기록이 사라집니다. 계속 하시겠습니까?\n(좋아요, 저장한 만화 제외)").setPositiveButton("네", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener).show();
            }
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

                pbtnClear = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(prevKeyCode == -1)
                            ptext.setText("-");
                        else
                            ptext.setText(KeyEvent.keyCodeToString(prevKeyCode));
                        inputCallback = null;
                        view.setOnClickListener(pbtnSet);
                    }
                };
                nbtnClear = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(nextKeyCode == -1)
                            ntext.setText("-");
                        else
                            ntext.setText(KeyEvent.keyCodeToString(nextKeyCode));
                        inputCallback = null;
                        view.setOnClickListener(nbtnSet);
                    }
                };
                pbtnSet = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(inputCallback == null) {
                            view.setOnClickListener(pbtnClear);
                            ptext.setText("키를 입력해 주세요");
                            inputCallback = new InputCallback() {
                                @Override
                                public void onKeyEvent(KeyEvent event) {
                                    prevKeyCode = event.getKeyCode();
                                    ptext.setText(KeyEvent.keyCodeToString(prevKeyCode));
                                    view.setEnabled(true);
                                    view.setOnClickListener(pbtnSet);
                                }
                            };
                        }
                    }
                };
                nbtnSet = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(inputCallback == null) {
                            view.setOnClickListener(nbtnClear);
                            ntext.setText("키를 입력해 주세요");
                            inputCallback = new InputCallback() {
                                @Override
                                public void onKeyEvent(KeyEvent event) {
                                    nextKeyCode = event.getKeyCode();
                                    ntext.setText(KeyEvent.keyCodeToString(nextKeyCode));
                                    view.setEnabled(true);
                                    view.setOnClickListener(nbtnSet);
                                }
                            };
                        }
                    }
                };

                pbtn.setOnClickListener(pbtnSet);
                nbtn.setOnClickListener(nbtnSet);

                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setTitle("단축키 설정")
                        .setView(v)
                        .setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                                if(inputCallback != null){
                                    if(keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                                        inputCallback.onKeyEvent(keyEvent);
                                        inputCallback = null;
                                    }
                                    return true;
                                }
                                return false;
                            }
                        })
                        .setNeutralButton("초기화", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                p.setPrevPageKey(-1);
                                p.setNextPageKey(-1);
                                inputCallback = null;
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                inputCallback = null;
                            }
                        })
                        .setPositiveButton("적용", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                inputCallback = null;
                                p.setNextPageKey(nextKeyCode);
                                p.setPrevPageKey(prevKeyCode);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                inputCallback = null;
                            }
                        })
                        .show();
            }
        });

        s_dark = this.findViewById(R.id.setting_dark);
        s_dark_switch = this.findViewById(R.id.setting_dark_switch);
        s_dark_switch.setChecked(p.getDarkTheme());
        s_dark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_dark_switch.toggle();
            }
        });
        s_dark_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setDarkTheme(isChecked);
                if(isChecked != dark) setResult(RESULT_NEED_RESTART);
                else setResult(RESULT_CANCELED);
            }
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
        s_reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_reverse_switch.toggle();
            }
        });
        s_reverse_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setReverse(isChecked);
            }
        });

        s_dataSave = this.findViewById(R.id.setting_dataSave);
        s_dataSave_switch = this.findViewById(R.id.setting_dataSave_switch);
        s_dataSave_switch.setChecked(p.getDataSave());
        s_dataSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_dataSave_switch.toggle();
            }
        });
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


        this.findViewById(R.id.setting_url).setOnClickListener(v -> {
            urlSettingPopup(context, p);
        });

        s_stretch = this.findViewById(R.id.setting_stretch);
        s_stretch_switch = this.findViewById(R.id.setting_stretch_switch);
        s_stretch_switch.setChecked(p.getStretch());
        s_stretch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_stretch_switch.toggle();
            }
        });
        s_stretch_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setStretch(isChecked);
            }
        });

        this.findViewById(R.id.setting_buttonLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, LayoutEditActivity.class));
            }
        });

        this.findViewById(R.id.setting_dataExport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, FolderSelectActivity.class);
                intent.putExtra("mode", MODE_FILE_SAVE);
                intent.putExtra("title", "파일 저장");
                startActivityForResult(intent, MODE_FILE_SAVE);
            }
        });

        this.findViewById(R.id.setting_dataImport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, FolderSelectActivity.class);
                intent.putExtra("mode", MODE_FILE_SELECT);
                intent.putExtra("title", "파일 선택");
                startActivityForResult(intent, MODE_FILE_SELECT);
            }
        });

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

        toggle.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    definput.setEnabled(true);
                    input.setEnabled(false);
                    input.setText("...");
                }else{
                    definput.setEnabled(false);
                    input.setEnabled(true);
                    input.setText(p.getUrl());
                }
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
                .setPositiveButton("설정", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
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
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        //do nothing
                    }
                })
                .show();
    }


    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null) {
            String path = data.getStringExtra("path");
            if (path != null) {
                switch (requestCode) {
                    case MODE_FILE_SELECT:
                        if(readPreferenceFromFile(p, context, new File(path))) {
                            setResult(RESULT_NEED_RESTART);
                            showPopup(context, "데이터 불러오기", "데이터 불러오기를 성공했습니다. 변경사항을 적용하기 위해 앱을 재시작 합니다.", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }, new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    finish();
                                }
                            });
                        }else
                            Toast.makeText(context, "불러오기 실패", Toast.LENGTH_LONG).show();
                        break;
                    case MODE_FOLDER_SELECT:
                        p.setHomeDir(path);
                        Toast.makeText(context, "설정 완료!", Toast.LENGTH_LONG).show();
                        break;
                    case MODE_FILE_SAVE:

                        if(writePreferenceToFile(context, new File(path)))
                            Toast.makeText(context, "내보내기 완료!", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(context, "내보내기 실패", Toast.LENGTH_LONG).show();

                        break;
                }
            }
        }

    }

    private interface InputCallback{
        void onKeyEvent(KeyEvent event);
    }
}
