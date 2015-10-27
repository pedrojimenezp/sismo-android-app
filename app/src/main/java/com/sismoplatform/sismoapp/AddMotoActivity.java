package com.sismoplatform.sismoapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.ConnectException;

public class AddMotoActivity extends AppCompatActivity {
    Button LoadMotoImageButtton;
    String base64EncodedImage = "";
    String mac = "";
    String brand = "";
    String line = "";
    String stringModel = "";
    String plate = "";
    String color = "";
    String stringCylinderCapacity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_moto);
        Toolbar toolbar = (Toolbar) findViewById(R.id.add_moto_toolbar);
        toolbar.setTitle("Add moto");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_add_moto, menu);
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

    public void onClickLoadImageButton(View view) {
        Log.i(SISMO.LOG_TAG,"LoadImageButton click");
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, 0);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == 0 && resultCode == RESULT_OK
                    && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String path = cursor.getString(columnIndex);
                cursor.close();

                Log.i(SISMO.LOG_TAG, "Path: " + path);

                ImageView imageViewMotoImage = (ImageView) findViewById(R.id.AddMotoActivity_ImageView_MotoImage);

                Bitmap bitmap = BitmapFactory.decodeFile(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                base64EncodedImage = Base64.encodeToString(b, Base64.URL_SAFE);

                //System.out.println(base64EncodedImage);
                //Log.i(MainActivity.LOG_TAG, base64EncodedImage);

                //byte[] decodedString = Base64.decode(img, Base64.DEFAULT);
                //Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                //Log.i(MainActivity.LOG_TAG, img);

                imageViewMotoImage.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "You haven't picked an image from galery",
                        Toast.LENGTH_LONG).show();
                this.base64EncodedImage = "";
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }

    public void onClickAddMotoButton(View view) {
        Log.i(SISMO.LOG_TAG, "Click");
        EditText editTextMotoMac = (EditText)findViewById(R.id.AddMotoActivity_EditText_Mac);
        EditText editTextMotoBrand = (EditText)findViewById(R.id.AddMotoActivity_EditText_Brand);
        EditText editTextMotoLine = (EditText)findViewById(R.id.AddMotoActivity_EditText_Line);
        EditText editTextMotoModel = (EditText)findViewById(R.id.AddMotoActivity_EditText_Model);
        EditText editTextMotoPlate = (EditText)findViewById(R.id.AddMotoActivity_EditText_Plate);
        EditText editTextMotoColor = (EditText)findViewById(R.id.AddMotoActivity_EditText_Color);
        EditText editTextMotoCylinderCapacity = (EditText)findViewById(R.id.AddMotoActivity_EditText_CylinderCapacity);
        ImageView imageViewMotoImage = (ImageView) findViewById(R.id.AddMotoActivity_ImageView_MotoImage);


        mac = editTextMotoMac.getText().toString();
        brand = editTextMotoBrand.getText().toString();
        line = editTextMotoLine.getText().toString();
        stringModel = editTextMotoModel.getText().toString();
        plate = editTextMotoPlate.getText().toString();
        color = editTextMotoColor.getText().toString();
        stringCylinderCapacity = editTextMotoCylinderCapacity.getText().toString();

        Log.i(SISMO.LOG_TAG, "Mac: " + mac);
        Log.i(SISMO.LOG_TAG, "Brand: " + brand);
        Log.i(SISMO.LOG_TAG, "Line: " + line);
        Log.i(SISMO.LOG_TAG, "Model: " + stringModel);
        Log.i(SISMO.LOG_TAG, "Plate: " + plate);
        Log.i(SISMO.LOG_TAG, "Color: " + color);
        Log.i(SISMO.LOG_TAG, "Cylinder capaicty: " + stringCylinderCapacity);
        //System.out.println(base64EncodedImage);
        if(!brand.isEmpty() && !line.isEmpty() && !stringModel.isEmpty() &&
                !plate.isEmpty() && !color.isEmpty() && !stringCylinderCapacity.isEmpty()){
            if(!this.base64EncodedImage.isEmpty()){
                Log.i(SISMO.LOG_TAG, "Adding moto");
                String bodyParams = "mac="+mac+"&brand="+brand+"&line="+line+"&model="+stringModel+"&plate="+
                        plate+"&color="+color+"&cylinderCapacity="+stringCylinderCapacity+"&image="+base64EncodedImage+"&imageEncodeType=base64_url_safe";
                AddMotos addMoto = new AddMotos();
                addMoto.execute(bodyParams);
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), "You need to pick a image if your moto", Toast.LENGTH_SHORT);
                toast.show();
            }
        }else{
            Toast toast = Toast.makeText(getApplicationContext(), "You need to complete all fields", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    public class AddMotos extends AsyncTask<String, Void, String> {
        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(AddMotoActivity.this);
            pDialog.setMessage("Saving moto");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String url = "http://192.168.1.184:4000/api/v1/users/"+ SISMO.Username+"/motos";
                HTTPClient httpClient = new HTTPClient(url);
                //httpClient.url = "http://www.google.com/search?q=mkyong";
                System.out.println("Access token: " + SISMO.AccessToken);
                httpClient.setMethod("POST");
                httpClient.addHeader("access-token", SISMO.AccessToken);


                httpClient.addParams(params[0]);
                String response = httpClient.makeRequest();
                System.out.println("Response: "+response);

                JSONObject jsonObj = new JSONObject(response);
                String responseType = jsonObj.getString("type");
                /*System.out.println(responseType);
                if(responseType.equals("LOGIN_SUCCESS")) {
                    String accessToken = jsonObj.getString("accessToken");
                    String refreshToken = jsonObj.getString("refreshToken");
                    SharedPreferences sp = getSharedPreferences(MainActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("accessToken", accessToken);
                    editor.putString("refreshToken", refreshToken);
                    editor.putString("username", username);
                    editor.apply();
                }*/
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
                case "CREATED" :
                    //Intent intent = new Intent(acti, HomeActivity.class);
                    //startActivity(intent);
                    //finish();
                    toast = Toast.makeText(AddMotoActivity.this, "Moto registered", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "BAD_REQUEST" :
                    toast = Toast.makeText(AddMotoActivity.this, "Bad request", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "UNAUTHORIZED" :
                    toast = Toast.makeText(AddMotoActivity.this, "Invalid access token", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "CONNECTION_ERROR" :
                    toast = Toast.makeText(AddMotoActivity.this, "Error trying to connect to the server", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case "ANOTHER_ERROR" :
                    toast = Toast.makeText(AddMotoActivity.this, "Something was wrong", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }
}
