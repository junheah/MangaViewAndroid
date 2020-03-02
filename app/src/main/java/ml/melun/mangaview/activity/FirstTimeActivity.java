package ml.melun.mangaview.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.p;

public class FirstTimeActivity extends AppCompatActivity {

    public static final int RESULT_EULA_AGREE = 0;
    public static final int RESULT_EULA_DISAGREE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_EULA_DISAGREE);
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_first_time);

        p.getSharedPref().edit().putBoolean("eula", false).commit();
        this.findViewById(R.id.eulaAgreeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p.getSharedPref().edit().putBoolean("eula", true).commit();
                setResult(RESULT_EULA_AGREE);
                finish();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
