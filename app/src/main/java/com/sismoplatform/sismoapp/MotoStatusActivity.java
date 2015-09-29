package com.sismoplatform.sismoapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class MotoStatusActivity extends AppCompatActivity {
    CollapsingToolbarLayout collapsingToolbarLayout;
    int vibrantColor = R.color.primary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moto_status);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.anim_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle("Yamaha FZ");
        collapsingToolbarLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        final ImageView header = (ImageView) findViewById(R.id.header);
        final ImageView header2 = (ImageView) findViewById(R.id.header2);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.moto);
        header2.setBackgroundColor(Color.argb(150, 255, 0, 0));
        header.setImageBitmap(bitmap);

        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {

                vibrantColor = palette.getVibrantColor(R.color.primary);
                header2.setBackgroundColor(Color.argb(100, Color.red(vibrantColor), Color.green(vibrantColor), Color.blue(vibrantColor)));
                collapsingToolbarLayout.setContentScrimColor(vibrantColor);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_moto_status, menu);
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

    public void onClickGetLocation(View view) throws Exception {
        Intent i = new Intent(MotoStatusActivity.this, MapsActivity.class);

        startActivity(i);

    }
}
