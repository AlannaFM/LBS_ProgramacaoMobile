package com.example.avn1;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;




public class GNSSActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_UPDATES=1;
    private LocationManager locationManager;
    LocationListener locationListener; // consome novas informações do sistema de localização

    // Listener para consumir as novas informações contínuas de status do sistema de satélites
    GnssStatus.Callback gnssCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnssactivity);
        //obtem o location maneger
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Button btnStartGNSS=findViewById(R.id.btnStartGNSS);
        Button btnStopGNSS=findViewById(R.id.btnStopGNSS);

        btnStartGNSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startGnssUpdate();
            }
        });
        btnStopGNSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopGNSSUpdate();
            }
        });
    }

        public void startGnssUpdate() {
            // Se a app já possui a permissão, ativa a chamada para atualiazações
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                // A permissão foi dada– OK vá em frente
                // objeto intância de uma classe anônima que implementa LocationListener
                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        atualizaLocationTextView(location);// Processa nova localização
                    }

                    //é obrigatório declarar todos os métodos da interface LocationListener,
                    // mesmo quando não é necessário usa-los


                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    //Indica quando o status do provedor muda (temporariamente indisponível ou
                        // se voltou a funcionar)

                        //Usaria logs para análise
                    }
                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        //chamado quando o provedor de localização é ativado
                    }
                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                    //quando o provedor de localização é desativado.
                    }
                };

                //Informa o escutador, tempo e distancia minimos ao provedor de localização
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

                // consumir as novas informações do sistema de satélites
                gnssCallback = new GnssStatus.Callback() {
                    @Override
                    public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                        super.onSatelliteStatusChanged(status);
                        atualizaGNSSTextView(status); //processa as informações do sistema de satélite
                    }
                };

                //informa o escutador do sistema de staelites e a thread para processar as informações
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
                    e.printStackTrace();
                }
            }
            // desliga o location listener
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            atualizaGNSSTextView(null);
            atualizaLocationTextView(null);
        }

        // método que percorre todos os satélites visiveis
        private void atualizaGNSSTextView (GnssStatus status) {
            TextView textViewGNSS = findViewById(R.id.textViewGNSS);
            if (status==null) {
                String s="Sistemas de Satélites não disponível";
                textViewGNSS.setText(s);
                return;
            }
            StringBuilder sb = new StringBuilder();
            int count = status.getSatelliteCount();
            //Montando as strings das informações
            sb.append("Satélites visíveis: ").append(count).append("\n");
            for (int i = 0; i < count; i++) {
                int svid = status.getSvid(i); // ID do satélite
                float azimuth = status.getAzimuthDegrees(i); // Azimute (0°=Norte, 90°=Leste)
                float elevation = status.getElevationDegrees(i); //// Elevação (0°=horizonte, 90°=zênite)
                boolean used = status.usedInFix(i); //diz se o satélite participou do cálculo da localização atual
                sb.append("SVID: ").append(svid)
                        .append(" | Azimute: ").append(azimuth).append("°")
                        .append(" | Elevação: ").append(elevation).append("°")
                        .append(" | Usado no fix: ").append(used)
                        .append("\n");
            }
            textViewGNSS.setText(sb.toString());
        }
        private void atualizaLocationTextView (Location location) {
            TextView locationTextView=(TextView) findViewById(R.id.textViewLocationManager);
            if (location==null) {
                String s="Dados de Localização não disponíveis";
                locationTextView.setText(s);
                return;
            }
            String s="Dados da Última Localização:\n";
            if (location!=null) {
                s+="Latitude: "+ location.getLatitude()+"\n";
                s+="Longitude: "+ location.getLongitude()+"\n";
                s+="Altitude: "+ location.getAltitude()+"\n";
                s+="Rumo: (radianos)"+ location.getBearing()+"\n";
                s+="Velocidade (m/s): "+ location.getSpeed()+"\n";
                s+="Precisão: (m)"+ location.getAccuracy()+"\n";
            }
            locationTextView.setText(s);
        }
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode,permissions,grantResults);
            if (requestCode == REQUEST_LOCATION_UPDATES) {
                if(grantResults.length == 1 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    //O usuário deu a permissão
                    startGnssUpdate();
                }
                else {
                    //o usuário não deu a permissão
                    Toast.makeText(this,"Sem permissão para mostrar informações do sistema GNSS",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }




