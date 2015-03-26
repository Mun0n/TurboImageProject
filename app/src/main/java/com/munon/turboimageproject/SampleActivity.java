package com.munon.turboimageproject;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import com.munon.turboimageview.TurboImageView;


public class SampleActivity extends ActionBarActivity {
    private static final String TAG = "SampleActivity";

    private TurboImageView turboImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        turboImageView = (TurboImageView) findViewById(R.id.turboImageView);

        Button drawButton = (Button) findViewById(R.id.addButton);
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turboImageView.loadImages(SampleActivity.this, R.drawable.ic_launcher);
            }
        });

        Button removeButton = (Button) findViewById(R.id.removeButton);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean deleted = turboImageView.deleteSelectedObject();
                Log.d(TAG, "deleted: " + deleted);
            }
        });

        Button deselectButon = (Button) findViewById(R.id.deselectButton);
        deselectButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turboImageView.deselectAll();
            }
        });
    }

}
