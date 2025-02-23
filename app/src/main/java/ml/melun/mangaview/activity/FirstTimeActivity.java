package ml.melun.mangaview.activity;

import static ml.melun.mangaview.CheckInfo.COLOR_DARK;
import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showYesNoPopup;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import ml.melun.mangaview.CheckInfo;
import ml.melun.mangaview.R;

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
        cinfo.setColorMode(COLOR_DARK);
        cinfo.update(true);

        setResult(RESULT_EULA_DISAGREE);
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_first_time);
        input = this.findViewById(R.id.first_def_url);

        pd = new ProgressDialog(context, R.style.darkDialog);
        pd.setMessage("url 확인중...");
        pd.setCancelable(false);

        p.getSharedPref().edit().putLong("eula2", -1).apply();
        this.findViewById(R.id.eulaNoUrlBtn).setOnClickListener(view -> showYesNoPopup(true, context, "주의",
                "기본주소를 설정하지 않으면\n[설정] > [URL 설정] 에서 직접 유효한 주소를 설정해 줘야 앱 사용이 가능합니다.\nURL 자동 설정이 작동하지 않을때만 이 옵션을 사용해 주세요.\n계속 하시겠습니까?",
                (dialogInterface, i) -> {
                    p.setAutoUrl(false);
                    long time = System.currentTimeMillis();
                    p.getSharedPref().edit().putLong("eula2", time).apply();
                    // not a migrator
                    p.getSharedPref().edit().putBoolean("manamoa", false).apply();
                    Toast.makeText(context,
                            new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(time) + " 부로 EULA에 동의했습니다.",
                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_EULA_AGREE);
                    finish();
                }, null, null));
    }

    public boolean containsDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private void urlError(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        input.requestFocus();
        if (pd.isShowing()) {
            pd.dismiss();
        }
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
