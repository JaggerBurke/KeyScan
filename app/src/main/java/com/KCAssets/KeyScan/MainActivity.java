package com.KCAssets.KeyScan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";
    private GoogleSignInClient mGoogleSignInClient;
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION = "access_token_expiration";
    private SharedPreferences sharedPreferences;


    /***********************************************************
     * OnCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setVisibility(View.GONE);

        // Retrieve the message from the Intent
        String message = getIntent().getStringExtra("message");

        // Check if a message was passed
        if (message != null) {
            // Display the message as a Toast
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestServerAuthCode(getString(R.string.client_id))
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive"), new Scope("https://www.googleapis.com/auth/spreadsheets"), new Scope("https://www.googleapis.com/auth/docs"), new Scope("https://www.googleapis.com/auth/documents"))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.googleBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String serverAuthCode = account.getServerAuthCode();

            // Exchange server authentication code for tokens
            exchangeCodeForTokens(serverAuthCode);
            navigateToWelcomeActivity();
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(MainActivity.this, "Sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void exchangeCodeForTokens(String serverAuthCode) {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new FormBody.Builder()
                .add("code", serverAuthCode)
                .add("client_id", getString(R.string.client_id))
                .add("client_secret", getString(R.string.client_secret))
                .add("grant_type", "authorization_code")
                .build();

        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Gson gson = new Gson();
                    JsonObject jsonObject;

                    try {
                        jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

                        JsonElement accessTokenElement = jsonObject.get("access_token");
                        JsonElement expiresInElement = jsonObject.get("expires_in");

                        String accessToken = accessTokenElement != null ? accessTokenElement.getAsString() : null;
                        long expiresIn = expiresInElement != null ? expiresInElement.getAsLong() : 0;

                        long expirationTime = System.currentTimeMillis() / 1000 + expiresIn;

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(PREF_ACCESS_TOKEN, accessToken);
                        editor.putLong(PREF_ACCESS_TOKEN_EXPIRATION, expirationTime);
                        editor.apply();

                        Log.d(TAG, "Access Token: " + accessToken);

                    } catch (JsonParseException e) {
                        Log.e(TAG, "Failed to parse response JSON", e);
                    }
                } else {
                    Log.e(TAG, "Failed to exchange code for tokens. Response code: " + response.code());
                }
            }
        });
    }
    void navigateToWelcomeActivity(){
        finish();
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        startActivity(intent);
    }

}








