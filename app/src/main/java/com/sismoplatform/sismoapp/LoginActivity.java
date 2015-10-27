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

public class LoginActivity extends AppCompatActivity {
    String username = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
            Toast toas = Toast.makeText(this, "Yous must to insert unername and password", Toast.LENGTH_SHORT);
            toas.show();
        }
    }

    public class Login extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

       @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Loging in, Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                HTTPClient httpClient = new HTTPClient("http://192.168.1.184:4000/api/v1/access-token");

                String basicToken = params[0];

                httpClient.setMethod("GET");
                httpClient.addHeader("authorization", basicToken);
                String response = httpClient.makeRequest();
                System.out.println(response);

                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                String responseStatus = jsonObj.getString("status");
                if(responseCode == 200) {
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONObject tokens = result.getJSONObject("tokens");
                    String accessToken = tokens.getString("accessToken");
                    String refreshToken = tokens.getString("refreshToken");
                    SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("accessToken", accessToken);
                    editor.putString("refreshToken", refreshToken);
                    editor.putString("username", username);
                    editor.apply();
                    SISMO.AccessToken = accessToken;
                    SISMO.RefreshToken = refreshToken;
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
                    toast = Toast.makeText(getApplicationContext(), "You must to send an username and password", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Unauthorized" :
                    toast = Toast.makeText(getApplicationContext(), "Incorrect username or password", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Connection error" :
                    toast = Toast.makeText(getApplicationContext(), "Error trying to connect to the server", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Another error" :
                    toast = Toast.makeText(getApplicationContext(), "Something was wrong", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }
}
