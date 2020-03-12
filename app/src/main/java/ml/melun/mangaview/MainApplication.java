package ml.melun.mangaview;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraMailSender;

import ml.melun.mangaview.mangaview.CustomHttpClient;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.REPORT_ID;
import static org.acra.ReportField.STACK_TRACE;


@AcraMailSender(mailTo = "mangaview@protonmail.com")
@AcraCore(reportContent = { APP_VERSION_NAME, ANDROID_VERSION, PHONE_MODEL, STACK_TRACE, REPORT_ID})
@AcraDialog(resText=R.string.acra_dialog_text)

public class MainApplication extends Application{
    public static CustomHttpClient httpClient;
    public static Preference p;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        System.out.println("main app start");
        //ACRA.init(this);
    }

    @Override
    public void onCreate() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        p = new Preference(this);
        httpClient = new CustomHttpClient(p);
        super.onCreate();
    }
}
