package ml.melun.mangaview.mangaview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static ml.melun.mangaview.Utils.getSample;


public class Decoder {
    int __seed=0;
    int id=0;
    int view_cnt;
    int cx=5, cy=5;

    public int getCnt(){
        return view_cnt;
    }

    public Decoder(int seed, int id){
        view_cnt = seed;
        __seed = seed/10;
        this.id = id;
        if(__seed>30000){
            cx = 1;
            cy = 6;
        }else if(__seed>20000){
            cx = 1;
        } else if (__seed>10000) {
            cy = 1;
        }
    }

    public Bitmap decode(Bitmap input, int width){
        input = getSample(input,width);
        return decode(input);
    }
    public Bitmap downSample(final Bitmap input, int maxBytes) {
        if(input.getByteCount() > maxBytes) {
            Float ratio = (maxBytes*1.0f/input.getByteCount());
            return downSize(input, ratio);
        }
        return input;
    }
    public Bitmap downSize(final Bitmap input, Float ratio) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap bitmap = Bitmap.createScaledBitmap(input, ((Float)(input.getWidth()*ratio)).intValue(), ((Float)(input.getHeight()*ratio)).intValue(), true);
        return bitmap;
    }

    public Bitmap decode(Bitmap input){
        input = downSample(input, 100000000);
        if(view_cnt==0) return input;
        int[][] order = new int[cx*cy][2];
        for (int i = 0; i < cx*cy; i++) {
            order[i][0] = i;
            if (id < 554714) order[i][1] = _random(i);
            else order[i][1] = newRandom(i);
        }
        java.util.Arrays.sort(order, (a, b) -> {
            //return Double.compare(a[1], b[1]);
            return a[1] != b[1] ? a[1] - b[1] : a[0] - b[0];
        });
        //create new bitmap
        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        int row_w = input.getWidth() / cx;
        int row_h = input.getHeight() / cy;
        for (int i = 0; i < cx*cy; i++) {
            int[] o = order[i];
            int ox = i % cx;
            int oy = i / cx;
            int tx = o[0] % cx;
            int ty = o[0] / cx;
            Bitmap cropped = Bitmap.createBitmap(input, ox * row_w, oy * row_h, row_w, row_h);
            canvas.drawBitmap(cropped, tx * row_w, ty * row_h, null);
        }
        return output;
    }

    private int _random(int index){
        double x = Math.sin(__seed+index) * 10000;
        return (int) Math.floor((x - Math.floor(x)) * 100000);
    }

    private int newRandom(int index){
        index++;
        double t = 100 * Math.sin(10 * (__seed+index))
                , n = 1000 * Math.cos(13 * (__seed+index))
                , a = 10000 * Math.tan(14 * (__seed+index));
        t = Math.floor(100 * (t - Math.floor(t)));
        n = Math.floor(1000 * (n - Math.floor(n)));
        a = Math.floor(10000 * (a - Math.floor(a)));
        return (int)(t + n + a);
    }
}