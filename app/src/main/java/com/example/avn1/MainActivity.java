package com.example.avn1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnLocation = findViewById(R.id.BtnLocation);
        Button btnGNSS = findViewById(R.id.BtnGNSS);
        //Obtém informações contínuas do sistema de
        //satélites + localizações com o
        //LocationManager

        btnLocation.setOnClickListener(new View.OnClickListener() {
        //localizações contínuas com o FusedLocationProviderCLient
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(),LocationActivity.class);
                startActivity(i);
            }
        });

        btnGNSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(getApplicationContext(),GNSSActivity.class);
                startActivity(i);
            }
        });

    }
}