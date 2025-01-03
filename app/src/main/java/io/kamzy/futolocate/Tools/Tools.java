package io.kamzy.futolocate.Tools;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Tools {

    public static Request prepServerRequest(String endpoint, FormBody.Builder request_parameters){
        RequestBody formBody = request_parameters.build();
        String baseURL = "http://192.168.0.106:8080/";
        return new Request.Builder()
                .url(baseURL+endpoint)
                .post(formBody)
                .build();
    }
}
