package ml.melun.mangaview.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenWidth;

public class LayoutEditActivity extends AppCompatActivity {
    Button left;
    Button right;
    boolean leftRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_edit);

        Context context = this;

        left = this.findViewById(R.id.layoutLeftButton);
        right = this.findViewById(R.id.layoutRightButton);
        leftRight = p.getLeftRight();
        setButtonText();

        SeekBar seekBar = this.findViewById(R.id.seekBar);

        // set seekbar max to current screen width
        int max = getScreenWidth(getWindowManager().getDefaultDisplay());
        seekBar.setMax(max);

        // set button width to saved value
        ViewGroup.LayoutParams params = left.getLayoutParams();
        int width = p.getPageControlButtonOffset();
        if(width != -1){
            params.width = width;
            left.setLayoutParams(params);
        }

        // set seekbar progress to current button width
        seekBar.setProgress(params.width);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                params.width = i;
                left.setLayoutParams(params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.findViewById(R.id.layout_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p.setPageControlButtonOffset(seekBar.getProgress());
                p.setLeftRight(leftRight);
                Toast.makeText(context, "설정 완료", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        this.findViewById(R.id.layout_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p.setPageControlButtonOffset(-1);
                p.setLeftRight(false);
                Toast.makeText(context, "기본값으로 설정됨", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        this.findViewById(R.id.layout_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        this.findViewById(R.id.layout_reverse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                leftRight = !leftRight;
                setButtonText();
            }
        });
    }

    private void setButtonText(){
        if(leftRight){
            left.setText(R.string.next_page);
            right.setText(R.string.prev_page);
        }else{
            right.setText(R.string.next_page);
            left.setText(R.string.prev_page);
        }
    }
}
