package ml.melun.mangaview.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import ml.melun.mangaview.CheckInfo;
import ml.melun.mangaview.R;
import ml.melun.mangaview.UrlUpdater;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showYesNoPopup;

public class FirstTimeActivity extends AppCompatActivity {

    public static final int RESULT_EULA_AGREE = 0;
    public static final int RESULT_EULA_DISAGREE = 1;
    Context context;
    EditText input;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        //check update
        CheckInfo cinfo = new CheckInfo(context, httpClient, true);
        cinfo.update(true);

        setResult(RESULT_EULA_DISAGREE);
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_first_time);
        input = this.findViewById(R.id.first_def_url);


        pd = new ProgressDialog(context, R.style.darkDialog);
        pd.setMessage("url 확인중...");
        pd.setCancelable(false);

        p.getSharedPref().edit().putLong("eula2", -1).commit();
        this.findViewById(R.id.eulaAgreeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pd.show();
                String defurl = input.getText().toString();
                if(defurl == null || defurl.length() == 0){
                    urlError("기본주소를 입력해 주세요.");
                }else if(containsDigit(defurl)) {
                    urlError("기본주소는 숫자를 포함하지 않은 주소입니다.");
                }else if(!defurl.contains("https://")) {
                    urlError("기본주소는 https 프로토콜을 사용해야 합니다.");
                }else{
                    //check url
                    new UrlUpdater(context, true, new UrlUpdater.UrlUpdaterCallback() {
                        @Override
                        public void callback(boolean success) {
                            if(pd.isShowing())
                                pd.dismiss();
                            if(success){
                                p.setDefUrl(defurl);
                                p.setAutoUrl(true);
                                long time = System.currentTimeMillis();
                                p.getSharedPref().edit().putLong("eula2", time).commit();
                                // not a migrator
                                p.getSharedPref().edit().putBoolean("manamoa", false).commit();
                                Toast.makeText(context, new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(time) + " 부로 EULA에 동의했습니다.",Toast.LENGTH_LONG).show();
                                setResult(RESULT_EULA_AGREE);
                                finish();
                            }else{
                                urlError("주소 업데이트에 실패했습니다.");
                            }
                        }
                    }, defurl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        this.findViewById(R.id.eulaNoUrlBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showYesNoPopup(true, context, "주의",
                        "기본주소를 설정하지 않으면\n[설정] > [URL 설정] 에서 직접 유효한 주소를 설정해 줘야 앱 사용이 가능합니다.\nURL 자동 설정이 작동하지 않을때만 이 옵션을 사용해 주세요.\n계속 하시겠습니까?",
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                p.setAutoUrl(false);
                                long time = System.currentTimeMillis();
                                p.getSharedPref().edit().putLong("eula2", time).commit();
                                // not a migrator
                                p.getSharedPref().edit().putBoolean("manamoa", false).commit();
                                Toast.makeText(context, new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(time) + " 부로 EULA에 동의했습니다.",Toast.LENGTH_LONG).show();
                                setResult(RESULT_EULA_AGREE);
                                finish();
                            }
                        },null, null);
            }
        });
    }

    public boolean containsDigit(String s){
        for(char c : s.toCharArray()) {
            if(Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private void urlError(String text){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        input.requestFocus();
        if(pd.isShowing())
            pd.dismiss();
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
