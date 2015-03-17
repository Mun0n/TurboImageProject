package com.munon.turboimageproject;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.munon.turboimageview.TurboImageView;


public class SampleActivity extends ActionBarActivity {

    private Button drawButton;
    private TurboImageView turboImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        turboImageView = (TurboImageView) findViewById(R.id.turboImageView);

        drawButton = (Button) findViewById(R.id.addButton);
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turboImageView.loadImages1(SampleActivity.this, R.drawable.ic_launcher);
            }
        });
    }

}
