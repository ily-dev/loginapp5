package com.meinname.loginapp3;

import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenTerminal = findViewById(R.id.btnOpenTerminal);
        btnOpenTerminal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.rk.terminal.ui.activities.terminal.MainActivity.class);
            startActivity(intent);
        });
    }
}