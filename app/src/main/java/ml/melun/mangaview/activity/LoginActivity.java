package ml.melun.mangaview.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ThreadPoolExecutor;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Login;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.mangaview.Bookmark.importBookmark;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Id to identity READ_CONTACTS permission request.
     */

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mCaptchaView;
    private ImageView captchaImg;
    private View mProgressView;
    private View mLoginFormView;
    private View accountPanel;
    private Button logoutBtn;
    Context context;

    Login login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set up the login form.
        context = this;
        accountPanel = this.findViewById(R.id.account_panel);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mCaptchaView = findViewById(R.id.captcha_answer);
        captchaImg = findViewById(R.id.captcha_img);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        logoutBtn = findViewById(R.id.logout_button);

        if(p.getLogin() != null && p.getLogin().isValid()){
            mLoginFormView.setVisibility(View.GONE);
            accountPanel.setVisibility(View.VISIBLE);
            logoutBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    p.setLogin(null);
                    httpClient.resetCookie();
                    mLoginFormView.setVisibility(View.VISIBLE);
                    accountPanel.setVisibility(View.GONE);
                    new PreLoginTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });

            this.findViewById(R.id.bookmark_list_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("mode",7);
                    startActivity(i);
                }
            });

            this.findViewById(R.id.bookmark_import_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AsyncTask<Void, Void, Integer>(){
                        ProgressDialog pd;
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            if(p.getDarkTheme()) pd = new ProgressDialog(context, R.style.darkDialog);
                            else pd = new ProgressDialog(context);
                            pd.setMessage("불러오는중");
                            pd.setCancelable(false);
                            pd.show();
                        }

                        @Override
                        protected void onPostExecute(Integer integer) {
                            super.onPostExecute(integer);
                            if (pd.isShowing()) {
                                pd.dismiss();
                            }
                            if(integer == 0)
                                Toast.makeText(context, "작업을 성공적으로 완료했습니다.", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(context, "작업을 실패했습니다.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        protected Integer doInBackground(Void... voids) {
                            return importBookmark(p, httpClient);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
        }else{
            new PreLoginTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }


    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Callback received when a permissions request has been completed.
     */



    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String answer = mCaptchaView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if(TextUtils.isEmpty(answer)){
            Toast.makeText(context, "자동입력 방지문자를 입력하세요", Toast.LENGTH_SHORT);
            cancel = true;
            focusView = mCaptchaView;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password, answer);
            mAuthTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return true;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class PreLoginTask extends AsyncTask<Void, Void, Void>{
        byte[] image;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(true);
            login = new Login();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            image = login.prepare(httpClient, p);
            return null;
        }

        @Override
        protected void onPostExecute(Void a) {
            super.onPostExecute(a);
            Glide.with(context).asBitmap().load(image).into(captchaImg);
            showProgress(false);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        String answer;
        UserLoginTask(String email, String password, String answer) {
            login.set(email, password);
            this.answer = answer;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String url = p.getUrl();
            return login.submit(httpClient, answer);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                //save login credentials
                p.setLogin(login);
                Toast.makeText(context,"로그인 성공",Toast.LENGTH_SHORT).show();
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

