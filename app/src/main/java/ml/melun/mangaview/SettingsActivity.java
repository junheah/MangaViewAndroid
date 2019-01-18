package ml.melun.mangaview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.net.URI;

public class SettingsActivity extends AppCompatActivity {

    //다운로드 위치 설정
    //데이터 절약 모드 : 외부 이미지 로드 안함
    //
    Downloader d;
    Context context;
    ConstraintLayout s_setHomeDir, s_resetHistory, s_volumeKey, s_getSd, s_dark, s_scroll, s_reverse, s_dataSave, s_tab;
    Spinner s_tab_spinner;
    Switch s_volumeKey_switch, s_dark_switch, s_scroll_switch, s_reverse_switch, s_dataSave_switch;
    Preference p;
    Boolean dark;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        d = new Downloader(this);
        context = this;
        s_setHomeDir = this.findViewById(R.id.setting_dir);
        s_setHomeDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (d.getStatus() == 0) {
                    Intent intent = new Intent(context, FolderSelectActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(context,"현재 다운로드가 진행중입니다. 작업이 끝난 후 변경 해 주세요",Toast.LENGTH_LONG).show();
                }
            }
        });
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

        s_scroll = this.findViewById(R.id.setting_scroll);
        s_scroll_switch = this.findViewById(R.id.setting_scroll_switch);
        s_scroll_switch.setChecked(p.getScrollViewer());
        s_scroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                s_scroll_switch.toggle();
            }
        });
        s_scroll_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                p.setScrollViewer(isChecked);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try{
            if(requestCode==2){
                String tmp = data.getData().getPath();
                String tmp2 = java.net.URLDecoder.decode(tmp, "UTF-8");
                System.out.println("pppppp "+tmp2);
                p.setHomeDir(tmp2);
            }
        }catch (Exception e){}
    }
}
