package com.munon.turboimageproject;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.munon.turboimageview.TurboImageView;


public class SampleActivity extends ActionBarActivity {

    private Button drawButton;
    private TurboImageView turboImageView;
    private Button removeButton;
    private Button deselectButon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        turboImageView = (TurboImageView) findViewById(R.id.turboImageView);

        drawButton = (Button) findViewById(R.id.addButton);
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turboImageView.loadImages(SampleActivity.this, R.drawable.ic_launcher);
            }
        });

        removeButton = (Button) findViewById(R.id.removeButton);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turboImageView.deleteSelectedObject();
            }
        });

        deselectButon = (Button) findViewById(R.id.deselectButton);
        deselectButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turboImageView.deselectAll();
            }
        });
    }

}
