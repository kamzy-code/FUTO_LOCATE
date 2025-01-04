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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Signup extends AppCompatActivity {
    EditText name, email, confirmEmail, password, confirmPassword, phone;
    Button signupButton;
    TextView loginTextButton;
    Context ctx;
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ctx = this;
        name = findViewById(R.id.name_input);
        email = findViewById(R.id.signup_email_input);
        confirmEmail = findViewById(R.id.c_email_input);
        password = findViewById(R.id.signup_password_input);
        confirmPassword = findViewById(R.id.c_password_input);
        phone = findViewById(R.id.phone);
        signupButton = findViewById(R.id.signup_button);
        loginTextButton = findViewById(R.id.Login_text_button);

        loginTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ctx, MainActivity.class);
                startActivity(intent);
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = name.getText().toString();
                String userEmail = email.getText().toString();
                String userConfirmEmail = confirmEmail.getText().toString();
                String userPhone = phone.getText().toString();
                String userPassword = password.getText().toString();
                String userConfirmPassword = confirmPassword.getText().toString();

                if (userName.isEmpty()) name.setError("enter your name");
                if (userEmail.isEmpty()) email.setError("enter email");
                if (userConfirmEmail.isEmpty()) confirmEmail.setError("confirm your email address");
                if (userPhone.isEmpty()) phone.setError("enter phone number");
                if (userPassword.isEmpty()) password.setError("enter password");
                if (userConfirmPassword.isEmpty()) confirmPassword.setError("confirm password");

                if (!userName.isEmpty() && !userEmail.isEmpty() && !userConfirmEmail.isEmpty() &&
                        !userPhone.isEmpty() && !userPassword.isEmpty() && !userConfirmPassword.isEmpty()){
                    Log.i("Field Status", "No field is empty");
                    if (!userEmail.equals(userConfirmEmail)){
                        confirmEmail.setError("Email mismatch");
                    }
                    if (!userPassword.equals(userConfirmPassword)){
                        confirmPassword.setError("Password mismatch");
                    }
                    if (userEmail.equals(userConfirmEmail) && userPassword.equals(userConfirmPassword)){
                        Log.i("Params Status", "Parameters match");
                        try {
                            callSignupAPI("api/auth/signup", userName, userEmail, userPassword, userPhone);
                        } catch (IOException | JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            }
        });
    }













    private void callSignupAPI (String endpoint, String...parameters) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("full_name", parameters[0])
                .put("email", parameters[1])
                .put("password", parameters[2])
                .put("phone_number", parameters[3])
                .put("role", "user");

        Log.i("Array", jsonObject.toString());

        RequestBody body = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        new Thread(()->{
            try (Response response = client.newCall(prepPostServerRequest(endpoint, body)).execute()){
            int statusCode = response.code();
            Log.i("statusCode", String.valueOf(statusCode));
            if (response.isSuccessful()){
                JSONObject responseBody = new JSONObject(response.body().string());
                String signUpStatus = responseBody.getString("status");
                runOnUiThread(()->{
                    switch (signUpStatus){
                        case "Signup Successful":
                            Toast.makeText(ctx, signUpStatus, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ctx, MainActivity.class);
                            startActivity(intent);
                            break;
                        case "Weak Password":
                            Toast.makeText(ctx, "Password should contain upper case, lower case " +
                                    "and special character", Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(ctx, signUpStatus, Toast.LENGTH_LONG).show();
                            break;
                    }
                });
            }else {
                Log.e("API call error", "error connecting to API");
            }
        }
        catch (IOException | JSONException e){
            throw new RuntimeException(e);
        }
        }).start();
    }
}