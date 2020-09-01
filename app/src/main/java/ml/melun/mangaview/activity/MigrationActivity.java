package ml.melun.mangaview.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;

public class MigrationActivity extends AppCompatActivity {
    private Context context;

    public static final int MIGRATION_ACTIVITY = 13;
    public static final int MIGRATION_SUCCESS = 33;
    public static final int MIGRATION_FAIL = 23;
    public static final int MIGRATION_RESET = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_migration);
        context = this;

        this.findViewById(R.id.migrate_backup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, FolderSelectActivity.class);
                intent.putExtra("mode", MODE_FILE_SAVE);
                intent.putExtra("title", "백업");
                startActivityForResult(intent, MODE_FILE_SAVE);
            }
        });

        this.findViewById(R.id.migrate_proceed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder;
                if (p.getDarkTheme())
                    builder = new AlertDialog.Builder(context, R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setTitle("기록 업데이트")
                        .setCancelable(false)
                        .setMessage("이 작업은 되돌릴 수 없습니다. 계속 하시겠습니까?")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //update data
                                new Migrator().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        });

        this.findViewById(R.id.migrate_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                p.reset();
                                Toast.makeText(context,"초기화 되었습니다.",Toast.LENGTH_LONG).show();
                                setResult(MIGRATION_RESET);
                                finish();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder;
                if(p.getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setMessage("최근 본 만화, 북마크 및 모든 만화 열람 기록이 사라집니다. 계속 하시겠습니까?\n(저장한 만화 제외)").setPositiveButton("네", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // don't allow user to quit
        //super.onBackPressed();
    }

    private class Migrator extends AsyncTask<Void, Void, Integer>{

        ProgressDialog pd;
        int sum = 0;
        int current = 0;
        List<MTitle> newFavorites, newRecents;
        List<String> failed;

        @Override
        protected void onPreExecute() {
            if(p.getDarkTheme()) pd = new ProgressDialog(context, R.style.darkDialog);
            else pd = new ProgressDialog(context);
            pd.setMessage("시작중");
            pd.setCancelable(false);
            pd.show();
        }


        @Override
        protected void onProgressUpdate(Void... values) {
            pd.setMessage(current +" / " + sum+"\n 앱을 종료하지 말아주세요.");
        }

        @Override
        protected Integer doInBackground(Void... voids) {

            //test
            Search a = new Search("아이",0);
            a.fetch(httpClient);
            if(a.getResult().size()<1){
                return 1;
            }


            List<MTitle> recents = p.getRecent();
            sum += recents.size();
            List<MTitle> favorites = p.getFavorite();
            sum += favorites.size();
            //recent data

            //test only favorites
            removeDups(favorites);
            removeDups(recents);

            newRecents = new ArrayList<>();
            newFavorites = new ArrayList<>();
            failed = new ArrayList<>();

            for(int i=0; i<recents.size(); i++){
                try {
                    current++;
                    publishProgress();
                    MTitle newTitle = findTitle(recents.get(i));
                    if(newTitle !=null)
                        newRecents.add(newTitle);
                    else
                        failed.add(recents.get(i).getName());
                }catch (Exception e){
                    e.printStackTrace();
                    failed.add(recents.get(i).getName());
                }
            }
            for(int i=0; i<favorites.size(); i++){
                try {
                    current++;
                    publishProgress();
                    MTitle newTitle = findTitle(favorites.get(i));
                    if(newTitle !=null)
                        newFavorites.add(newTitle);
                    else
                        failed.add(favorites.get(i).getName());
                }catch (Exception e){
                    e.printStackTrace();
                    failed.add(favorites.get(i).getName());
                }
            }

            p.setFavorites(newFavorites);
            p.setRecents(newRecents);

            return 0;
        }

        void removeDups(List<MTitle> titles){
            for(int i=0; i<titles.size(); i++){
                MTitle target = titles.get(i);
                for(int j =0 ; j<titles.size(); j++){
                    if(j!=i && titles.get(j).getId() == target.getId()){
                        titles.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        MTitle findTitle(String title){
            return findTitle(new MTitle(title,-1,"", "",new ArrayList<>(),""));
        }

        MTitle findTitle(MTitle title){
            String name = title.getName();
            Search s = new Search(name,0);
            while(!s.isLast()){
                s.fetch(httpClient);
                for(Title t : s.getResult()){
                    if(t.getName().equals(name)){
                        return t.minimize();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer resCode) {
            if(pd.isShowing()){
                pd.dismiss();
            }

            if(resCode == 0){
                if(failed.size()>0){
                    StringBuilder builder = new StringBuilder();
                    builder.append("(총 ");
                    builder.append(failed.size());
                    builder.append("개)");
                    for(String t : failed){
                        builder.append("\n"+t);
                    }
                    showPopup(context, "알림", "기록 업데이트 완료.\n실패한 항목:\n" + builder.toString(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            setResult(MIGRATION_SUCCESS);
                            finish();
                        }
                    }, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            setResult(MIGRATION_SUCCESS);
                            finish();
                        }
                    });
                }else
                    showPopup(context,"알림","기록 업데이트 완료", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            setResult(MIGRATION_SUCCESS);
                            finish();
                        }
                    }, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            setResult(MIGRATION_SUCCESS);
                            finish();
                        }
                    });
            }
            else if(resCode == 1) showPopup(context,"연결 오류","연결을 확인하고 다시 시도해 주세요.");
        }

        @Override
        protected void onCancelled() {
            if(pd.isShowing()){
                pd.dismiss();
            }
        }


    }
}