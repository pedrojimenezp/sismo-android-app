package com.sismoplatform.sismoapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.ConnectException;

public class SignupActivity extends AppCompatActivity {
    String username = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
    }

    public void onClickSignupButton(View view) {
        EditText usernameTextView = (EditText)findViewById(R.id.SignupActivity_EditText_Username);
        EditText password1TextView = (EditText)findViewById(R.id.SignupActivity_EditText_Password1);
        EditText password2TextView = (EditText)findViewById(R.id.SignupActivity_EditText_Password2);
        username = usernameTextView.getText().toString();
        String password1 = password1TextView.getText().toString();
        String password2 = password2TextView.getText().toString();
        if(!username.isEmpty() && !password1.isEmpty() && !password2.isEmpty()){
            if(password1.equals(password2)){
                String params = "username="+username+"&password="+password1;
                new Signup().execute(params);
            }else{
                Toast.makeText(this, "Contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "Debe llenar todos los campos", Toast.LENGTH_SHORT).show();
        }
    }

    public class Signup extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog = new ProgressDialog(SignupActivity.this);
            progressDialog.setMessage("Registrandose en el sistema, por favor espere...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                HTTPClient httpClient = new HTTPClient(SISMO.SISMO_API_SERVER_HOST+"/api/v1/users");

                String bodyParams = params[0];

                httpClient.setMethod("POST");
                httpClient.addParams(bodyParams);
                String response = httpClient.makeRequest();
                System.out.println(response);

                JSONObject jsonObj = new JSONObject(response);
                int responseCode = jsonObj.getInt("code");
                String responseStatus = jsonObj.getString("status");
                if(responseCode == 201) {
                    JSONObject result = jsonObj.getJSONObject("result");
                    JSONObject user = result.getJSONObject("user");
                    String userId = user.getString("_id");

                    SharedPreferences sp = getSharedPreferences(SISMO.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("userId", userId);
                    editor.putString("username", username);
                    editor.apply();
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
                case "Created" :
                    Toast.makeText(getApplicationContext(), "Registrado", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case "Bad request" :
                    toast = Toast.makeText(getApplicationContext(), "Debes enviar un usuario y una contraseña.", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "Conflict" :
                    Toast.makeText(getApplicationContext(), "El usurio que desea registrar ya existe en el sistema.\nIngrese uno diferente.", Toast.LENGTH_SHORT).show();
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
