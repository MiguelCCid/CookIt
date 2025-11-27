package com.example.cookit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataBaseHelper db = new DataBaseHelper(this);
        db.getWritableDatabase();

        Button btnVerRecetas = findViewById(R.id.btnVerRecetas);

        btnVerRecetas.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ListRecipesActivity.class);
            startActivity(i);
        });
    }
}
