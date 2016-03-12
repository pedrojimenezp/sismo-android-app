package com.sismoplatform.sismoapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {
    String username = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickLoginButton(View view) {
        EditText usernameTextView = (EditText)findViewById(R.id.usernameTextView);
        EditText passwordTextView = (EditText)findViewById(R.id.passwordTextView);
        username = usernameTextView.getText().toString();
        String password = passwordTextView.getText().toString();
        if(!username.isEmpty() && !password.isEmpty()){
            String params = username+":"+password;
            params = Base64.encodeToString(params.getBytes(),Base64.DEFAULT);
            params = "Basic "+params;
            new Login().execute(params);
        }else{
            Toast toas = Toast.makeText(this, "Debe ingresar un usuario y una contraseña", Toast.LENGTH_SHORT);
            toas.show();
        }
    }

    public void onClickLaunchSignupActivityButton(View view) {
        Intent i = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(i);
    }

    public class Login extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

       @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Iniciando sesion, por favor espere...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                HTTPClient httpClient = new HTTPClient(SISMO.SISMO_API_SERVER_HOST+"/api/v1/login");

                String basicToken = params[0];

                httpClient.setMethod("POST");
                httpClient.addHeader("authorization", basicToken);
                String response = httpClient.makeRequest();
                System.out.println(response);

                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                String responseStatus = jsonObj.getString("status");
                if(responseCode == 200) {
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONObject user = result.getJSONObject("user");
                    String userId = user.getString("_id");

                    SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("userId", userId);
                    editor.putString("username", username);
                    editor.apply();
                    SISMO.MotoList = new ArrayList<>();
                    SISMO.UserId = userId;
                    SISMO.Username = username;
                }
                return responseStatus;
            } catch (ConnectException e1) {
                System.out.println(e1.toString());
                return "Connection error";
            } catch (Exception e2){
                System.out.println(e2.toString());
                return "Another error";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast toast;
            switch (result) {
                case "Ok" :
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case "Bad request" :
                    toast = Toast.makeText(getApplicationContext(), "Debes enviar un usuaro y una contraseña", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Unauthorized" :
                    toast = Toast.makeText(getApplicationContext(), "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Connection error" :
                    toast = Toast.makeText(getApplicationContext(), "Error tratando de conectrse con el servidor", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Another error" :
                    toast = Toast.makeText(getApplicationContext(), "Algo salio mal", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }
}
