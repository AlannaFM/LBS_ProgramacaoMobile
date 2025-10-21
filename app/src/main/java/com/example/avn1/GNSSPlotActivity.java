package com.example.avn1;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;





//inicia e controla a coleta de dados GNSS e exibe graficamente os satélites
// visíveis através da  Custom View GNSSView
public class GNSSPlotActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_UPDATES = 1;
    private LocationManager locationManager;
    LocationListener locationListener;
    GnssStatus.Callback gnssCallback;
    GNSSView gnssView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnssplot);
        // pega o Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // referencia ele para o componente customizado
        gnssView=findViewById(R.id.GNSSViewid);
        // começa o processo de aquisição automaticamente
        startGnssUpdate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGNSSUpdate();
    }

    public void startGnssUpdate() {
        // Se a app já possui a permissão, ativa a chamada para ataaliazações
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // A permissão foi dada– OK vá em frente
            // objeto intância de uma classe anônima que implementa LocationListener
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    // Processa nova localização
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) { }
                @Override
                public void onProviderEnabled(@NonNull String provider) { }
                @Override
                public void onProviderDisabled(@NonNull String provider) { }
            };
            // Informa o provedor de localização, tempo e distância mínimos e o escutador
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,   // provedor de localização
                    1000,                           // intervalo mínimo (ms)
                    0,                              // distância mínima (m)
                    locationListener);              // objeto que irá processar as localizações

            // objeto intância de uma classe anônima que implementa GnssStatus.Callback
            gnssCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    // processa as informações do sistema de satélite
                    gnssView.newStatus(status);
                }
            };
            // informa o escutador do sistema de satelites e a thread para processar essas infos
            locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.getMainLooper()));
        } else {
            // Solicite a permissão
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_UPDATES);
        }
    }
    public void stopGNSSUpdate() {
        // desliga a GnssCallback
        if (gnssCallback != null) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssCallback);
            } catch (Exception e) {
                e.printStackTrace(); //Ferramenta de depuração para mostrar qual tipo de exceção
                // evita crash se já tiver sido desregistrado
            }
        }
        // desliga o location listener
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == REQUEST_LOCATION_UPDATES) {
            if(grantResults.length == 1 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                // O usuário acabou de dar a permissão
                startGnssUpdate();
            }
            else {
                // O usuário não deu a permissão solicitada
                Toast.makeText(this,"Sem permissão para mostrar informações do sistema GNSS",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}