package ml.melun.mangaview;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

/*
getters return default value instead of throwing exceptions
 */

public class CustomJSONObject extends JSONObject {
    public CustomJSONObject() {
        super();
    }

    public CustomJSONObject(Map copyFrom) {
        super(copyFrom);
    }

    public CustomJSONObject(JSONTokener readFrom) throws JSONException {
        super(readFrom);
    }

    public CustomJSONObject(String json) throws JSONException {
        super(json);
    }

    public CustomJSONObject(JSONObject copyFrom, String[] names) throws JSONException {
        super(copyFrom, names);
    }



    public Object get(String name, Object def){
        try {
            return super.get(name);
        }catch (JSONException e){
            return def;
        }
    }

    public boolean getBoolean(String name, boolean def){
        try {
            return super.getBoolean(name);
        }catch (JSONException e){
            return def;
        }
    }

    public double getDouble(String name, double def){
        try {
            return super.getDouble(name);
        }catch (JSONException e){
            return def;
        }
    }

    public int getInt(String name, int def){
        try {
            return super.getInt(name);
        }catch (JSONException e){
            return def;
        }
    }

    public long getLong(String name, long def){
        try {
            return super.getLong(name);
        }catch (JSONException e){
            return def;
        }
    }

    public String getString(String name, String def){
        try {
            return super.getString(name);
        }catch (JSONException e){
            return def;
        }
    }

    public JSONArray getJSONArray(String name, JSONArray def){
        try {
            return super.getJSONArray(name);
        }catch (JSONException e){
            return def;
        }
    }

    public JSONObject getJSONObject(String name, JSONObject def){
        try {
            return super.getJSONObject(name);
        }catch (JSONException e){
            return def;
        }
    }
}
