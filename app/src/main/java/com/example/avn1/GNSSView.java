package com.example.avn1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.GnssStatus;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class GNSSView extends View {

    // SharedPreferences keys
    private static final String PREFS_NAME = "GNSSViewPrefs";
    private static final String PREF_SHOW_GPS = "showGPS";
    private static final String PREF_SHOW_GLONASS = "showGLONASS";
    private static final String PREF_SHOW_GALILEO = "showGALILEO";
    private static final String PREF_SHOW_BEIDOU = "showBEIDOU";
    private static final String PREF_SHOW_naoUsado = "shownaoUsado";

    private GnssStatus gnssStatus = null; // satélites do sistema GNSS
    private int r;  // raio da esfera celeste (pixels)
    private int height, width; // dimensões do componente
    private Paint paint = new Paint(); // pincel
    private Paint textPaint = new Paint();

    // Configuráveis via attrs / prefs
    private int circleColor = Color.BLUE;
    private int UsadoSatelliteColor = Color.GREEN;
    private int naoUsadoSatelliteColor = Color.RED;
    private int textColor = Color.BLACK;
    private float radiusScale = 0.9f;

    // Opções do usuário (persistidas em SharedPreferences)
    private boolean showGPS = true;
    private boolean showGLONASS = true;
    private boolean showGALILEO = true;
    private boolean showBEIDOU = true;
    private boolean shownaoUsado = true;

    private SharedPreferences prefs;

    public GNSSView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inicializarAtributos(context, attrs);
        inicializarPaints();
        carregarPreferencias();
        // possibilita clique para abrir diálogo
        setClickable(true);
    }

    private void inicializarAtributos(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GNSSView);
            circleColor = ta.getColor(R.styleable.GNSSView_circle_color, circleColor);
            UsadoSatelliteColor = ta.getColor(R.styleable.GNSSView_Usado_cor_satelite, UsadoSatelliteColor);
            naoUsadoSatelliteColor = ta.getColor(R.styleable.GNSSView_naoUsado_cor_satelite, naoUsadoSatelliteColor);
            textColor = ta.getColor(R.styleable.GNSSView_text_color, textColor);
            radiusScale = ta.getFloat(R.styleable.GNSSView_radius_scale, radiusScale);
            ta.recycle();
        }
    }

    private void inicializarPaints() {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(circleColor);

        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(30f);
    }

    private void carregarPreferencias() {
        prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        showGPS = prefs.getBoolean(PREF_SHOW_GPS, true);
        showGLONASS = prefs.getBoolean(PREF_SHOW_GLONASS, true);
        showGALILEO = prefs.getBoolean(PREF_SHOW_GALILEO, true);
        showBEIDOU = prefs.getBoolean(PREF_SHOW_BEIDOU, true);
        shownaoUsado = prefs.getBoolean(PREF_SHOW_naoUsado, true);
    }

    private void savePreferences() {
        if (prefs == null) prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_SHOW_GPS, showGPS)
                .putBoolean(PREF_SHOW_GLONASS, showGLONASS)
                .putBoolean(PREF_SHOW_GALILEO, showGALILEO)
                .putBoolean(PREF_SHOW_BEIDOU, showBEIDOU)
                .putBoolean(PREF_SHOW_naoUsado, shownaoUsado)
                .apply();
    }

    public void newStatus(GnssStatus gnssStatus) {
        this.gnssStatus = gnssStatus;
        invalidate(); // força o redesenho
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        int min = Math.min(width, height);
        r = (int) (min / 2f * radiusScale);
    }

    // utilitários de conversão de coordenadas
    private int computeXc(double x) {
        return (int) (x + width / 2.0);
    }

    private int computeYc(double y) {
        return (int) (-y + height / 2.0);
    }

    // identifica constelação por valor retornado por GnssStatus.getConstellationType(i)
    private enum Constellation {
        GPS, GLONASS, GALILEO, BEIDOU, UNKNOWN
    }

    private Constellation constelacaoPorTipo(int constellationType) {
        // GnssStatus const types: GnssStatus.CONSTELLATION_*
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_GPS: return Constellation.GPS;
            case GnssStatus.CONSTELLATION_GLONASS: return Constellation.GLONASS;
            case GnssStatus.CONSTELLATION_BEIDOU: return Constellation.BEIDOU;
            case GnssStatus.CONSTELLATION_GALILEO: return Constellation.GALILEO;
            default: return Constellation.UNKNOWN;
        }
    }

    // forma simples para cada constelação: círculo, quadrado, triângulo, losango
    private void desenharMarcadorSatelite(Canvas canvas, float cx, float cy, Constellation c, boolean Usado) {
        Paint markerPaint = new Paint();
        markerPaint.setAntiAlias(true);
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(Usado ? UsadoSatelliteColor : naoUsadoSatelliteColor);

        final float size = 18f; // radius of marker

        switch (c) {
            case GPS:
                // círculo
                canvas.drawCircle(cx, cy, size, markerPaint);
                break;
            case GLONASS:
                // quadrado
                canvas.drawRect(cx - size, cy - size, cx + size, cy + size, markerPaint);
                break;
            case GALILEO:
                // triângulo
                Path p = new Path();
                p.moveTo(cx, cy - size);
                p.lineTo(cx - size, cy + size);
                p.lineTo(cx + size, cy + size);
                p.close();
                canvas.drawPath(p, markerPaint);
                break;
            case BEIDOU:
                // losango
                Path d = new Path();
                d.moveTo(cx, cy - size);
                d.lineTo(cx - size, cy);
                d.lineTo(cx, cy + size);
                d.lineTo(cx + size, cy);
                d.close();
                canvas.drawPath(d, markerPaint);
                break;
            default:
                canvas.drawCircle(cx, cy, size, markerPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // desenha a projeção da esfera celeste fixa (topo = norte)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(circleColor);

        int radius = r;
        // círculo principal
        canvas.drawCircle(computeXc(0), computeYc(0), radius, paint);

        // círculos concêntricos (45°, 60° projeção)
        int radius45 = (int) (radius * Math.cos(Math.toRadians(45)));
        int radius60 = (int) (radius * Math.cos(Math.toRadians(60)));
        canvas.drawCircle(computeXc(0), computeYc(0), radius45, paint);
        canvas.drawCircle(computeXc(0), computeYc(0), radius60, paint);

        // eixos (Norte em cima)
        canvas.drawLine(computeXc(0), computeYc(-radius), computeXc(0), computeYc(radius), paint); // y
        canvas.drawLine(computeXc(-radius), computeYc(0), computeXc(radius), computeYc(0), paint); // x

        // Se não tem status, só mostra texto
        if (gnssStatus == null) {
            textPaint.setColor(textColor);
            canvas.drawText("Sistemas de Satélites não disponível", 20, 40, textPaint);
            return;
        }

        // Contadores
        int count = gnssStatus.getSatelliteCount();
        int CountUsado = 0;
        int CountVisiveis = 0;

        // desenha satélites
        for (int i = 0; i < count; i++) {
            try {
                float az = gnssStatus.getAzimuthDegrees(i);    // 0 = norte, 90 = leste
                float el = gnssStatus.getElevationDegrees(i);  // 0 = horizonte, 90 = zenit
                boolean Usado = gnssStatus.usedInFix(i); //retorna true se o satélite está sendo usado na solução atual do fix (posição GPS válida)
                int constellationType;
                Constellation c = Constellation.UNKNOWN;

                // tenta obter o tipo de constelação (API moderna)
                try {
                    constellationType = gnssStatus.getConstellationType(i);
                    c = constelacaoPorTipo(constellationType);
                } catch (Throwable t) {
                    // fallback: inferir por SVID (não ideal) — deixar UNKNOWN
                    c = Constellation.UNKNOWN;
                }

                // checa filtro do usuário sobre a constelação
                boolean showThisConstellation = false;
                switch (c) {
                    case GPS: showThisConstellation = showGPS; break;
                    case GLONASS: showThisConstellation = showGLONASS; break;
                    case GALILEO: showThisConstellation = showGALILEO; break;
                    case BEIDOU: showThisConstellation = showBEIDOU; break;
                    default: showThisConstellation = true; // unknown -> mostrar
                }

                // --- Nova lógica de exibição de satélites ---

                boolean anyConstellationSelected = showGPS || showGLONASS || showGALILEO || showBEIDOU;

                // Caso 1: Nenhuma constelação marcada
                if (!anyConstellationSelected) {
                    // Mostrar apenas os não usados, se o usuário marcar a opção
                    if (shownaoUsado && !Usado) {
                        // ok, mostra este satélite não usado
                    } else {
                        continue; // pular todos os outros
                    }
                } else {
                    // Caso 2: Existem constelações marcadas
                    if (!showThisConstellation) {
                        continue; // pular constelações não marcadas
                    }

                    // Caso 3: Dentro das constelações selecionadas
                    // Se o usuário NÃO quer mostrar não usados e este não é usado -> pula
                    if (!shownaoUsado && !Usado) {
                        continue;
                    }
                    // Caso 4: Se shownaoUsado = true, mostra todos (usados e não usados) dessa constelação
                }



                CountVisiveis++;

                if (Usado) CountUsado++;

                // posição projetada: x,y em pixels (com topo = norte)
                float x = (float) (r * Math.cos(Math.toRadians(el)) * Math.sin(Math.toRadians(az)));
                float y = (float) (r * Math.cos(Math.toRadians(el)) * Math.cos(Math.toRadians(az)));

                float cx = computeXc(x);
                float cy = computeYc(y);

                // desenhar marcador (forma por constelação; cor depende de usado ou não)
                desenharMarcadorSatelite(canvas, cx, cy, c, Usado);

                // desenhar texto com ID e constelação pequena
                textPaint.setColor(textColor);
                textPaint.setTextSize(28f);
                String label = gnssStatus.getSvid(i) + " " + shortNameForConstellation(c) + (Usado ? " *" : "");
                canvas.drawText(label, cx + 22, cy + 10, textPaint);

            } catch (Exception e) {
                // prevenir crash por satélite com dados faltantes
                e.printStackTrace();
            }
        }

        // desenhar contadores no topo-esquerdo
        textPaint.setColor(textColor);
        textPaint.setTextSize(36f);
        int yBase = 45;
        canvas.drawText("Visíveis: " + CountVisiveis, 20, yBase, textPaint);
        canvas.drawText("Em uso (FIX): " + CountUsado, 20, yBase + 40, textPaint);
    }

    private String shortNameForConstellation(Constellation c) {
        switch (c) {
            case GPS: return "GPS";
            case GLONASS: return "GLO";
            case GALILEO: return "GAL";
            case BEIDOU: return "BDS";
            default: return "UNK";
        }
    }

    // abrir diálogo interno ao tocar na view (diálogo persiste as escolhas)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mostrarDialogoConfiguracao();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void mostrarDialogoConfiguracao() {
        // criar uma view simples com CheckBoxes programaticamente
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        final CheckBox cbGPS = new CheckBox(getContext());
        cbGPS.setText("Mostrar GPS");
        cbGPS.setChecked(showGPS);

        final CheckBox cbGLONASS = new CheckBox(getContext());
        cbGLONASS.setText("Mostrar GLONASS");
        cbGLONASS.setChecked(showGLONASS);

        final CheckBox cbGALILEO = new CheckBox(getContext());
        cbGALILEO.setText("Mostrar GALILEO");
        cbGALILEO.setChecked(showGALILEO);

        final CheckBox cbBEIDOU = new CheckBox(getContext());
        cbBEIDOU.setText("Mostrar BEIDOU");
        cbBEIDOU.setChecked(showBEIDOU);

        final CheckBox cbShownaoUsado = new CheckBox(getContext());
        cbShownaoUsado.setText("Mostrar satélites NÃO usados no FIX");
        cbShownaoUsado.setChecked(shownaoUsado);

        layout.addView(cbGPS);
        layout.addView(cbGLONASS);
        layout.addView(cbGALILEO);
        layout.addView(cbBEIDOU);
        layout.addView(cbShownaoUsado);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Configurar visualização");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // aplicar e salvar
                showGPS = cbGPS.isChecked();
                showGLONASS = cbGLONASS.isChecked();
                showGALILEO = cbGALILEO.isChecked();
                showBEIDOU = cbBEIDOU.isChecked();
                shownaoUsado = cbShownaoUsado.isChecked();

                savePreferences();
                invalidate(); // redesenhar com nova configuração
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}
