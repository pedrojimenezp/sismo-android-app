package com.sismoplatform.sismoapp;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

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

    String base64EncodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(SISMO.LOG_TAG, "MotoDetailsActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moto_details);

        Bundle extras = getIntent().getExtras();
        int index = extras.getInt("listIndex");

        moto = SISMO.MotoList.get(index);

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
        getMenuInflater().inflate(R.menu.menu_moto_details, menu);
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
            if (requestCode == 0 && resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();

                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String path = cursor.getString(columnIndex);
                cursor.close();

                Bitmap bitmap = BitmapFactory.decodeFile(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                base64EncodedImage = Base64.encodeToString(b, Base64.URL_SAFE);

                ImageView_MotoImage.setImageBitmap(bitmap);
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

}
