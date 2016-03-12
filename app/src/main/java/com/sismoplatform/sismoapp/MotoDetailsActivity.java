package com.sismoplatform.sismoapp;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.ConnectException;

public class MotoDetailsActivity extends AppCompatActivity {
    int vibrantColor = R.color.primary;

    ImageView ImageView_MotoImage;
    EditText EditText_Mac;
    EditText EditText_Brand;
    EditText EditText_Line;
    EditText EditText_Model;
    EditText EditText_Plate;
    EditText EditText_Color;
    EditText EditText_CylinderCapacity;

    Moto moto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(SISMO.LOG_TAG, "MotoDetailsActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moto_details);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            int index = extras.getInt("listIndex");
            moto = SISMO.MotoList.get(index);
        }


        final Toolbar toolbar = (Toolbar) findViewById(R.id.anim_toolbar);

        ImageView_MotoImage = (ImageView)findViewById(R.id.MotoDetailsActivity_ImageView_MotoImage);
        EditText_Mac = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_Mac);
        EditText_Brand = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_Brand);
        EditText_Line = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_Line);
        EditText_Model = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_Model);
        EditText_Plate = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_Plate);
        EditText_Color = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_Color);
        EditText_CylinderCapacity = (EditText)findViewById(R.id.MotoDetailsActivity_EditText_CylinderCapacity);

        ImageView_MotoImage.setImageBitmap(moto.BitmapImage);
        EditText_Mac.setText(moto.Mac);
        EditText_Brand.setText(moto.Brand);
        EditText_Line.setText(moto.Line);
        EditText_Model.setText(String.valueOf(moto.Model));
        EditText_Plate.setText(moto.Plate);
        EditText_Color.setText(moto.Color);
        EditText_CylinderCapacity.setText(String.valueOf(moto.CylinderCapacity));

        EditText_Mac.clearFocus();

        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.moto);
        toolbar.setTitle(moto.getBrandAndLine());

        Palette.from(moto.BitmapImage).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {

                vibrantColor = palette.getVibrantColor(R.color.primary);
                toolbar.setBackgroundColor(vibrantColor);
                toolbar.setTitleTextColor(R.color.white);
                toolbar.setSubtitleTextColor(R.color.white);
            }
        });

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_moto_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

    public void onClickUpdateMotoButton(View view) {
        Log.i(SISMO.LOG_TAG, "Click");

        String mac = EditText_Mac.getText().toString();
        String brand = EditText_Brand.getText().toString();
        String line = EditText_Line.getText().toString();
        String stringModel = EditText_Model.getText().toString();
        String plate = EditText_Plate.getText().toString();
        String color = EditText_Color.getText().toString();
        String stringCylinderCapacity = EditText_CylinderCapacity.getText().toString();

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
            Log.i(SISMO.LOG_TAG, "Updating moto");
            String bodyParams = "mac="+mac+"&brand="+brand+"&line="+line+"&model="+stringModel+"&plate="+
                        plate+"&color="+color+"&cylinderCapacity="+stringCylinderCapacity;
            UpdateMotos updateMoto = new UpdateMotos();
            updateMoto.execute(moto.Mac, bodyParams);
        }else{
            Toast toast = Toast.makeText(getApplicationContext(), "Necesitas llenar todos los campos", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    public class UpdateMotos extends AsyncTask<String, Void, String> {
        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MotoDetailsActivity.this);
            pDialog.setMessage("Actualizando datos de la moto, por favor espere.");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String mac = params[0];
                String url = SISMO.SISMO_API_SERVER_HOST+"/api/v1/motos/"+mac;
                HTTPClient httpClient = new HTTPClient(url);
                //httpClient.url = "http://www.google.com/search?q=mkyong";
                httpClient.setMethod("PUT");

                httpClient.addParams(params[1]);
                String response = httpClient.makeRequest();
                System.out.println("Response: "+response);

                JSONObject jsonObj = new JSONObject(response);
                String responseStatus = jsonObj.getString("status");
                return responseStatus;
            } catch (ConnectException e1) {
                System.out.println(e1.toString());
                return "Connection error";
            } catch (Exception e2){
                System.out.println(e2.toString());
                return "ANOTHER_ERROR";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            Toast toast;
            switch (result) {
                case "Ok" :
                    Toast.makeText(MotoDetailsActivity.this, "Datos actualizados", Toast.LENGTH_SHORT).show();
                    break;
                case "Bad request" :
                    Toast.makeText(MotoDetailsActivity.this, "Bad request", Toast.LENGTH_SHORT).show();
                    break;
                case "Unauthorized" :
                    Toast.makeText(MotoDetailsActivity.this, "Invalid access token", Toast.LENGTH_SHORT).show();
                    break;
                case "Connection error" :
                    Toast.makeText(MotoDetailsActivity.this, "Error tratando de conectarse con el servidor", Toast.LENGTH_SHORT).show();
                    break;
                case "Another error":
                    Toast.makeText(MotoDetailsActivity.this, "Algo salio mal", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
