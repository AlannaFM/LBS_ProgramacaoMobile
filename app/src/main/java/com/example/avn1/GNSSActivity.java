package com.example.avn1;

import android.content.Context;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.LocationListener;

public class GNSSActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_UPDATES=1;
    private LocationManager locationManager;
    LocationListener locationListener;

    // Listener consumir as novas informações contínuas do sistema de satélites
    GnssStatus.Callback gnssCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnssactivity);
        //obtem o location maneger
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);



    }
}