package io.kamzy.futolocate.Tools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import io.kamzy.futolocate.Models.Events;
import io.kamzy.futolocate.Models.Landmarks;
import io.kamzy.futolocate.Models.Routes;
import io.kamzy.futolocate.Models.Users;

public class GsonHelper {
    public GsonHelper() {
    }

    // convert JSONArray landmarks to List<Landmarks>
    public List<Landmarks> parseJSONArrayUsingGson(String jsonArrayString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Landmarks>>() {}.getType();
        return gson.fromJson(jsonArrayString, listType);
    }

    // convert JSONObject landmark to Landmark object
    public Landmarks parseJSONObjectUsingGson(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Landmarks>() {}.getType();
        return gson.fromJson(jsonObjectString, type);
    }

    // convert JSONObject landmark to Landmark object
    public Users parseJSONObjectToUser(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Users>() {}.getType();
        return gson.fromJson(jsonObjectString, type);
    }

    // convert JSONObject to Route object
    public Routes parseJSONObjecttoRoute(String jsonObjectString) {
        Gson gson = new Gson();
        Type type = new TypeToken<Routes>() {}.getType();
        return gson.fromJson(jsonObjectString, type);
    }

    public List<Events> parseJsonArrayToEventList(String string) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Events>>() {}.getType();
        return gson.fromJson(string, listType);
    }
}
