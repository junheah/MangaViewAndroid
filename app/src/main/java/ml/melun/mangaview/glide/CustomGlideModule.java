package ml.melun.mangaview.glide;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import static ml.melun.mangaview.MainApplication.httpClient;

import java.io.InputStream;

public class CustomGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        System.out.println("glide module create");
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(httpClient.client));
    }
}
