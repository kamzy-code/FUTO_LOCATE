package io.kamzy.futolocate.Tools;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Tools {
    public static String baseURL = "http://192.168.0.106:8080/";

    public static Request prepPostServerRequest(String endpoint, RequestBody request_body){
        return new Request.Builder()
                .url(baseURL+endpoint)
                .post(request_body)
                .build();
    }

    public static Request prepGetRequestWithoutBody (String endpoint, String token){
        return new Request.Builder()
                .url(baseURL+endpoint)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}
