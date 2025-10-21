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
        Button btnGNSSPlot = findViewById(R.id.btnGnssPlot);

        //Obtém informações contínuas do sistema de
        //satélites + localizações com o
        //LocationManager
        btnLocation.setOnClickListener(new View.OnClickListener() {
        //localizações contínuas com o FusedLocationProviderCLient
        //mostra as informações básicas de localização (latitude, longitude, altitude e precisão).
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(),LocationActivity.class);
                startActivity(i);
            }
        });

        btnGNSS.setOnClickListener(new View.OnClickListener() {
            //dados detalhados do sistema GNSS em tempo real (número de satélites, status, constelações e sinal)
            @Override
            public void onClick(View view) {
                Intent i=new Intent(getApplicationContext(),GNSSActivity.class);
                startActivity(i);
            }
        });
        btnGNSSPlot.setOnClickListener(new View.OnClickListener() {
            //tela que desenha a projeção da esfera celeste com os satélites visíveis, indicando os usados e não usados no FIX
            @Override
            public void onClick(View view) {
                Intent i=new Intent(getApplicationContext(), GNSSPlotActivity.class);
                startActivity(i);
            }
        });
    }
}