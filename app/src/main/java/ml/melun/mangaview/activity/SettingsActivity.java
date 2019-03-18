package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;

public class SettingsActivity extends AppCompatActivity {

    //다운로드 위치 설정
    //데이터 절약 모드 : 외부 이미지 로드 안함
    //
    Context context;
    ConstraintLayout s_setHomeDir, s_resetHistory, s_volumeKey, s_getSd, s_dark, s_viewer, s_reverse, s_dataSave, s_tab, s_url, s_stretch;
    Spinner s_tab_spinner, s_viewer_spinner;
    Switch s_volumeKey_switch, s_dark_switch, s_reverse_switch, s_dataSave_switch, s_stretch_switch, s_leftRight_switch;
    Preference p;
    Boolean dark;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
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
                startActivity(intent);
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
        s_volumeKey = this.findViewById(R.id.setting_volume);
        s_volumeKey_switch = this.findViewById(R.id.setting_volume_switch);
        s_volumeKey_switch.setChecked(p.getVolumeControl());
        s_volumeKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_volumeKey_switch.toggle();
            }
        });
        s_volumeKey_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setVolumeControl(isChecked);
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
            }
        });
//
//        s_scroll = this.findViewById(R.id.setting_scroll);
//        s_scroll_switch = this.findViewById(R.id.setting_scroll_switch);
//        s_scroll_spinner.setChecked(p.getScrollViewer());
//        s_scroll.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                s_scroll_switch.toggle();
//            }
//        });
//        s_scroll_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                p.setScrollViewer(isChecked);
//            }
//        });

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
        s_dataSave_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setDataSave(isChecked);
            }
        });

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


        this.findViewById(R.id.setting_license).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent l = new Intent(context,LicenseActivity.class);
                startActivity(l);
            }
        });


        this.findViewById(R.id.setting_url).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(context);
                input.setText(p.getUrl());
                input.setHint("http://188.214.128.5");
                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setTitle("URL 설정")
                        .setView(input)
                        .setPositiveButton("설정", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int button) {
                                if(input.getText().length()>0) p.setUrl(input.getText().toString());
                                else p.setUrl(input.getHint().toString());
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int button) {
                                //do nothing
                            }
                        })
                        .setNeutralButton("주소 확인 (트위터)", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/MSM64933093")));
                            }
                        })
                        .show();
            }
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

        s_leftRight_switch = this.findViewById(R.id.setting_leftRight_switch);
        s_leftRight_switch.setChecked(p.getLeftRight());
        this.findViewById(R.id.setting_leftRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_leftRight_switch.toggle();
            }
        });
        s_leftRight_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setLeftRight(isChecked);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try{
            if(requestCode==2){
                String tmp = data.getData().getPath();
                String tmp2 = java.net.URLDecoder.decode(tmp, "UTF-8");
                p.setHomeDir(tmp2);
            }
        }catch (Exception e){}
    }
}
