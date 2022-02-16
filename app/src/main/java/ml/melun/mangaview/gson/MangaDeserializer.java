package ml.melun.mangaview.gson;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;

import ml.melun.mangaview.mangaview.Manga;

public class MangaDeserializer implements JsonDeserializer<Manga> {
    @Override
    public Manga deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Manga m = new Gson().fromJson(json, new TypeToken<Manga>(){}.getType());
        m.setOfflinePath(new File(json.getAsJsonObject().getAsJsonObject("offlinePath").get("path").getAsString()));
        System.out.println("ppp deserial " + m.getOfflinePath().getAbsolutePath());
        return m;
    }
}
