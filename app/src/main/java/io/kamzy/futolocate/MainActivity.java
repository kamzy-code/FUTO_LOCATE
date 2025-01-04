package io.kamzy.futolocate;

import static io.kamzy.futolocate.Tools.Tools.prepPostServerRequest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    EditText email, password;
    TextView forgot_password, signup;
    Button login;
    Context ctx;
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ctx = this;
        email = findViewById(R.id.login_email_input);
        password = findViewById(R.id.login_password_input);
        forgot_password = findViewById(R.id.forgot_password);
        signup = findViewById(R.id.Signup_text_button);
        login = findViewById(R.id.login_button);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ctx, Signup.class);
                startActivity(intent);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user_email = email.getText().toString();
                String user_password = password.getText().toString();

                if (user_email.isEmpty()){
                    email.setError("enter email address");
                }
                if (user_password.isEmpty()){
                    password.setError("enter password");
                }

                if (!user_email.isEmpty() && !user_password.isEmpty()){
                    try {
                        callLoginAPI("api/auth/login", user_email, user_password);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }


            }
        });
    }








    private void callLoginAPI(String endpoint, String email, String password) throws IOException {
        FormBody.Builder requestParams = new FormBody.Builder()
                .add("email", email)
                .add("password", password);

        RequestBody formBody = requestParams.build();

        new Thread(()->{
            try (Response response = client.newCall(prepPostServerRequest(endpoint, formBody)).execute()) {
                int statusCode = response.code();
                Log.i("statusCode", String.valueOf(statusCode));
                if (response.isSuccessful()){
                    JSONObject responseBody = new JSONObject(response.body().string());
                    String token = responseBody.getString("token");
                    Log.i("response", token);
                    runOnUiThread(()->{
                            Intent intent = new Intent(ctx, Dashboard.class);
                            intent.putExtra("token", token);
                            startActivity(intent);
                    });
                } else if (statusCode == 401){
                    runOnUiThread(()->{
                        Toast.makeText(ctx, "invalid username or password", Toast.LENGTH_LONG).show();
                    });
                } else {
                    Log.e("API call error", "error connecting to API");
                }
            }
            catch (IOException | JSONException e){
                throw new RuntimeException(e);
            }
        }).start();


    }
}