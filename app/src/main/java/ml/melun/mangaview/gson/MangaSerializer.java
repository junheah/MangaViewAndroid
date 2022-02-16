package ml.melun.mangaview.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import ml.melun.mangaview.mangaview.Manga;

public class MangaSerializer implements JsonSerializer<Manga> {
    @Override
    public JsonElement serialize(Manga src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new Gson().toJsonTree(src).getAsJsonObject();
        System.out.println("pppp " + obj.toString());
        obj.remove("offlinePath");
        JsonObject file = new JsonObject();
        file.addProperty("path", src.getOfflinePath().getAbsolutePath());
        obj.add("offlinePath",file);

        return obj;
    }
}
