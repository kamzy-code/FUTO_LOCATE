package io.kamzy.futolocate.Tools;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Tools {

    public static Request prepPostServerRequest(String endpoint, RequestBody request_body){
        String baseURL = "http://192.168.0.159:8080/";
        return new Request.Builder()
                .url(baseURL+endpoint)
                .post(request_body)
                .build();
    }
}
