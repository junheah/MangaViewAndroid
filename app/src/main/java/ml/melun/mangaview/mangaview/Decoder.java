package ml.melun.mangaview.mangaview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

public class Decoder {
    int __seed=0;
    int id=0;
    public Decoder(int seed, int id){
        __seed = seed/10;
        this.id = id;
    }
    public Bitmap decode(Bitmap input){
        if(__seed==0) return input;
        int[][] order = new int[25][2];
        for (int i = 0; i < 25; i++) {
            order[i][0] = i;
            if (id < 554714) order[i][1] = _random(i);
            else order[i][1] = newRandom(i);
        }
        java.util.Arrays.sort(order, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                //return Double.compare(a[1], b[1]);
                return a[1] != b[1] ? a[1] - b[1] : a[0] - b[0];
            }
        });
        //create new bitmap
        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int row_w = input.getWidth() / 5;
        int row_h = input.getHeight() / 5;
        for (int i = 0; i < 25; i++) {
            int[] o = order[i];
            int ox = i % 5;
            int oy = i / 5;
            int tx = o[0] % 5;
            int ty = o[0] / 5;
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
