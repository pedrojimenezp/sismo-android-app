package com.sismoplatform.sismoapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.ConnectException;

public class LoginActivity extends AppCompatActivity {

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

    public void onClickLoginButton(View view) throws Exception {
        EditText usernameTextView = (EditText)findViewById(R.id.usernameTextView);
        EditText passwordTextView = (EditText)findViewById(R.id.passwordTextView);
        String params = "username="+usernameTextView.getText().toString()+"&password="+passwordTextView.getText().toString();
        new Login().execute(params);

    }

    public class Login extends AsyncTask<String, Void, String> {
        ProgressDialog pDialog;

       @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                HTTPClient httpClient = new HTTPClient("http://192.168.1.184:4000/api/v1/sessions");
                //httpClient.url = "http://www.google.com/search?q=mkyong";

                String bodyParams = params[0];

                httpClient.setMethod("POST");
                httpClient.addParams(bodyParams);
                String response = httpClient.makeRequest();
                System.out.println(response);

                JSONObject jsonObj = new JSONObject(response);
                String responseType = jsonObj.getString("type");
                System.out.println(responseType);
                if(responseType.equals("LOGIN_SUCCESS")) {
                    String accessToken = jsonObj.getString("accessToken");
                    String refreshToken = jsonObj.getString("refreshToken");
                    SharedPreferences sp = getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("accessToken", accessToken);
                    editor.putString("refreshToken", refreshToken);
                    editor.apply();
                }
                return responseType;
            } catch (ConnectException e1) {
                System.out.println(e1.toString());
                return "CONNECTION_ERROR";
            } catch (Exception e2){
                System.out.println(e2.toString());
                return "ANOTHER_ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            Toast toast;
            switch (result) {
                case "LOGIN_SUCCESS" :
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case "BAD_REQUEST" :
                    toast = Toast.makeText(getApplicationContext(), "You must to send an username and password", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "UNAUTHORIZED" :
                    toast = Toast.makeText(getApplicationContext(), "Incorrect username or password", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "CONNECTION_ERROR" :
                    toast = Toast.makeText(getApplicationContext(), "Error trying to connect to the server", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "ANOTHER_ERROR" :
                    toast = Toast.makeText(getApplicationContext(), "Something was wrong", Toast.LENGTH_SHORT);
                    toast.show();
                    break;


            }
        }
    }
}
