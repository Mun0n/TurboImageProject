package com.munon.turboimageproject;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import android.view.View;
import com.munon.turboimageview.MultiTouchObject;
import com.munon.turboimageview.TurboImageView;
import com.munon.turboimageview.TurboImageViewListener;
import java.io.IOException;
import java.io.InputStream;


public class SampleActivity extends ActionBarActivity implements TurboImageViewListener {
    private static final String TAG = "SampleActivity";

    private TurboImageView turboImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        turboImageView = (TurboImageView) findViewById(R.id.turboImageView);
        turboImageView.setListener(this);

        findViewById(R.id.addButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turboImageView.addObject(SampleActivity.this, getBitmapFromAsset(SampleActivity.this, "bitmaps/bitmap.png"));
            }
        });

        findViewById(R.id.removeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean deleted = turboImageView.removeSelectedObject();
                Log.d(TAG, "deleted: " + deleted);
            }
        });

        findViewById(R.id.removeAllButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turboImageView.removeAllObjects();
            }
        });

        findViewById(R.id.deselectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turboImageView.deselectAll();
            }
        });

        findViewById(R.id.flipButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turboImageView.toggleFlippedHorizontallySelectedObject();
            }
        });
    }

    @Override
    public void onImageObjectSelected(MultiTouchObject multiTouchObject) {
        Log.d(TAG, "image object selected");
    }

    @Override
    public void onImageObjectDropped() {
        Log.d(TAG, "image object dropped");
    }

    @Override
    public void onCanvasTouched() {
        turboImageView.deselectAll();
        Log.d(TAG, "canvas touched");
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream inputStream;
        Bitmap bitmap = null;
        try {
            inputStream = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException ignored) {
        }

        return bitmap;
    }
}
