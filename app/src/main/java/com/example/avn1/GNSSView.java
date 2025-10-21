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

    // chaves SharedPreferences
    private static final String PREFS_NAME = "GNSSViewPrefs";
    private static final String PREF_MOSTRAR_GPS = "MOSTRARGPS";
    private static final String PREF_MOSTRAR_GLONASS = "MOSTRARGLONASS";
    private static final String PREF_MOSTRAR_GALILEO = "MOSTRARGALILEO";
    private static final String PREF_MOSTRAR_BEIDOU = "MOSTRARBEIDOU";
    private static final String PREF_MOSTRAR_naoUsado = "MOSTRARnaoUsado";

    private GnssStatus gnssStatus = null; // dados dos satélites do sistema GNSS
    private int r;  // raio da esfera celeste (pixels)
    private int height, width; // dimensões do componente
    private Paint paint = new Paint(); // pincel
    private Paint textPaint = new Paint(); // pincel p texto

    // Configuráveis via attrs
    private int circleColor = Color.BLUE;
    private int CorSateliteUsado = Color.GREEN;
    private int CorSateliteNaoUsado = Color.RED;
    private int textColor = Color.BLACK;
    private float radiusScale = 0.9f;

    // Opções do usuário (persistidas em SharedPreferences)
    private boolean MOSTRARGPS = true;
    private boolean MOSTRARGLONASS = true;
    private boolean MOSTRARGALILEO = true;
    private boolean MOSTRARBEIDOU = true;
    private boolean MOSTRARnaoUsado = true;

    private SharedPreferences prefs;

    public GNSSView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inicializarAtributos(context, attrs);// Lê atributos do XML
        inicializarPaints();  // configura pincéis
        carregarPreferencias(); //// carrega preferências salvas
        // possibilita clique para abrir diálogo
        setClickable(true);
    }

    private void inicializarAtributos(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            //classe que lê atributos personalizados do attr
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GNSSView);
            circleColor = ta.getColor(R.styleable.GNSSView_circle_color, circleColor);
            CorSateliteUsado = ta.getColor(R.styleable.GNSSView_Usado_cor_satelite, CorSateliteUsado);
            CorSateliteNaoUsado = ta.getColor(R.styleable.GNSSView_naoUsado_cor_satelite, CorSateliteNaoUsado);
            textColor = ta.getColor(R.styleable.GNSSView_text_color, textColor);
            radiusScale = ta.getFloat(R.styleable.GNSSView_radius_scale, radiusScale);
            ta.recycle(); //Libera recursos de memória usados pelo array
        }
    }

    private void inicializarPaints() {
        //Inicializando e configurando os pinceis
        paint.setAntiAlias(true); //suaviza as bordas dos desenhos
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(circleColor);

        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(30f);
    }

    private void carregarPreferencias() {
        prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        MOSTRARGPS = prefs.getBoolean(PREF_MOSTRAR_GPS, true);
        MOSTRARGLONASS = prefs.getBoolean(PREF_MOSTRAR_GLONASS, true);
        MOSTRARGALILEO = prefs.getBoolean(PREF_MOSTRAR_GALILEO, true);
        MOSTRARBEIDOU = prefs.getBoolean(PREF_MOSTRAR_BEIDOU, true);
        MOSTRARnaoUsado = prefs.getBoolean(PREF_MOSTRAR_naoUsado, true);
    }

    private void savePreferences() {
        if (prefs == null) prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_MOSTRAR_GPS, MOSTRARGPS)
                .putBoolean(PREF_MOSTRAR_GLONASS, MOSTRARGLONASS)
                .putBoolean(PREF_MOSTRAR_GALILEO, MOSTRARGALILEO)
                .putBoolean(PREF_MOSTRAR_BEIDOU, MOSTRARBEIDOU)
                .putBoolean(PREF_MOSTRAR_naoUsado, MOSTRARnaoUsado)
                .apply();
    }

    public void newStatus(GnssStatus gnssStatus) {
        // recebe novos dados dos satélites de GNSSPlotActivity
        this.gnssStatus = gnssStatus;
        invalidate(); // força o redesenho
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //Calcula o raio da esfera celeste baseado no tamanho da tela.
        super.onSizeChanged(w, h, oldw, oldh); //Ver se o dispositivo foi rotacionado para recalcular
                                                // a posição dos satélites (manter a direção do norte)
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
    private void desenharMarcadorSatelite(Canvas canvas, float xc, float yc, Constellation c, boolean Usado) {
        Paint markerPaint = new Paint();
        markerPaint.setAntiAlias(true);
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(Usado ? CorSateliteUsado : CorSateliteNaoUsado);

        final float size = 18f; // radius of marker

        switch (c) {
            case GPS:
                // círculo
                canvas.drawCircle(xc, yc, size, markerPaint);
                break;
            case GLONASS:
                // quadrado
                canvas.drawRect(xc - size, yc - size, xc + size, yc + size, markerPaint);
                break;
            case GALILEO:
                // triângulo
                Path p = new Path();
                p.moveTo(xc, yc - size);
                p.lineTo(xc - size, yc + size);
                p.lineTo(xc + size, yc + size);
                p.close();
                canvas.drawPath(p, markerPaint);
                break;
            case BEIDOU:
                // losango
                Path d = new Path();
                d.moveTo(xc, yc - size);
                d.lineTo(xc - size, yc);
                d.lineTo(xc, yc + size);
                d.lineTo(xc + size, yc);
                d.close();
                canvas.drawPath(d, markerPaint);
                break;
            default:
                canvas.drawCircle(xc, yc, size, markerPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // desenha a projeção da esfera celeste fixa
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(circleColor);

        int radius = r;
        // círculo principal
        canvas.drawCircle(computeXc(0), computeYc(0), radius, paint);

        // círculos centrais
        int radius45 = (int) (radius * Math.cos(Math.toRadians(45)));
        int radius60 = (int) (radius * Math.cos(Math.toRadians(60)));
        canvas.drawCircle(computeXc(0), computeYc(0), radius45, paint);
        canvas.drawCircle(computeXc(0), computeYc(0), radius60, paint);

        // eixos
        canvas.drawLine(computeXc(0), computeYc(-radius), computeXc(0), computeYc(radius), paint); // y
        canvas.drawLine(computeXc(-radius), computeYc(0), computeXc(radius), computeYc(0), paint); // x

        // Verifica se há dados. Se não tem status, só mostra texto
        if (gnssStatus == null) {
            textPaint.setColor(textColor);
            canvas.drawText("Sistemas de Satélites não disponível", 20, 40, textPaint);
            return;
        }

        // Contadores
        int count = gnssStatus.getSatelliteCount(); //total d satelites
        int CountUsado = 0;
        int CountVisiveis = 0; //quantos são exibidos depois  dos filtros

        // desenha satélites
        for (int i = 0; i < count; i++) {
            try {
                //obtem dados dos satelites
                float az = gnssStatus.getAzimuthDegrees(i);    // 0 = norte, 90 = leste
                float el = gnssStatus.getElevationDegrees(i);  // 0 = horizonte, 90 = zenit
                boolean Usado = gnssStatus.usedInFix(i); //retorna true se o satélite está sendo usado na solução atual do fix (posição GPS válida)

                //Identifica constelação
                int constellationType;
                Constellation c = Constellation.UNKNOWN;

                // tenta obter o tipo de constelação
                try {
                    constellationType = gnssStatus.getConstellationType(i);
                    c = constelacaoPorTipo(constellationType);
                } catch (Throwable t) {
                    c = Constellation.UNKNOWN;
                }

                // checa filtro do usuário sobre a constelação
                boolean MostrarThisConstellation = false;
                switch (c) {
                    case GPS: MostrarThisConstellation = MOSTRARGPS; break;
                    case GLONASS: MostrarThisConstellation = MOSTRARGLONASS; break;
                    case GALILEO: MostrarThisConstellation = MOSTRARGALILEO; break;
                    case BEIDOU: MostrarThisConstellation = MOSTRARBEIDOU; break;
                    default: MostrarThisConstellation = true; // unknown sempre mostra
                }

                //logica de exibição de satélites

                boolean anyConstellationSelected = MOSTRARGPS || MOSTRARGLONASS || MOSTRARGALILEO || MOSTRARBEIDOU;

                // Caso 1: Nenhuma constelação marcada
                if (!anyConstellationSelected) {
                    // MOSTRAR apenas os não usados, se o usuário marcar a opção
                    if (MOSTRARnaoUsado && !Usado) {
                        // ok, mostra este satélite não usado
                    } else {
                        continue; // pular todos os outros
                    }
                } else {
                    // Caso 2: Existem constelações marcadas
                    if (!MostrarThisConstellation) {
                        continue; // pular constelações não marcadas
                    }

                    // Caso 3: Dentro das constelações selecionadas
                    // Se o usuário NÃO quer MOSTRAR não usados e este não é usado -> pula
                    if (!MOSTRARnaoUsado && !Usado) {
                        continue;
                    }
                    // Caso 4: Se MOSTRARnaoUsado = true, mostra todos (usados e não usados) dessa constelação
                }



                CountVisiveis++;//satelit será exibido

                if (Usado) CountUsado++; //conta se for usado no fix

                //Convertendo coordenadas para pixels

                // conversão de azimute e elevação em coordenadas X Y
                float x = (float) (r * Math.cos(Math.toRadians(el)) * Math.sin(Math.toRadians(az)));
                float y = (float) (r * Math.cos(Math.toRadians(el)) * Math.cos(Math.toRadians(az)));

                //conversão para coordenadas de tela
                float xc = computeXc(x);
                float yc = computeYc(y);

                // desenhar marcador (forma por constelação e  cor dependendo se é  usado ou não)
                desenharMarcadorSatelite(canvas, xc, yc, c, Usado);

                // desenhar texto com ID e constelação
                textPaint.setColor(textColor);
                textPaint.setTextSize(28f);
                String label = gnssStatus.getSvid(i) + " " + abreviacaoConstelacao(c) + (Usado ? " *" : "");
                canvas.drawText(label, xc + 22, yc + 10, textPaint);

            } catch (Exception e) {
                // prevenir crash por satélite com dados faltantes
                e.printStackTrace();
            }
        }

        // desenhar contadores no topo esquerdo
        textPaint.setColor(textColor);
        textPaint.setTextSize(36f);
        int yBase = 45;
        canvas.drawText("Visíveis: " + CountVisiveis, 20, yBase, textPaint);
        canvas.drawText("Em uso (FIX): " + CountUsado, 20, yBase + 40, textPaint);
    }

    private String abreviacaoConstelacao(Constellation c) {
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
            MostrarDialogoConfiguracao();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void MostrarDialogoConfiguracao() {
        // criar uma view simples com CheckBoxes programaticamente
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        final CheckBox cbGPS = new CheckBox(getContext());
        cbGPS.setText("MOSTRAR GPS");
        cbGPS.setChecked(MOSTRARGPS);

        final CheckBox cbGLONASS = new CheckBox(getContext());
        cbGLONASS.setText("MOSTRAR GLONASS");
        cbGLONASS.setChecked(MOSTRARGLONASS);

        final CheckBox cbGALILEO = new CheckBox(getContext());
        cbGALILEO.setText("MOSTRAR GALILEO");
        cbGALILEO.setChecked(MOSTRARGALILEO);

        final CheckBox cbBEIDOU = new CheckBox(getContext());
        cbBEIDOU.setText("MOSTRAR BEIDOU");
        cbBEIDOU.setChecked(MOSTRARBEIDOU);

        final CheckBox cbMOSTRARnaoUsado = new CheckBox(getContext());
        cbMOSTRARnaoUsado.setText("MOSTRAR satélites NÃO usados no FIX");
        cbMOSTRARnaoUsado.setChecked(MOSTRARnaoUsado);

        layout.addView(cbGPS);
        layout.addView(cbGLONASS);
        layout.addView(cbGALILEO);
        layout.addView(cbBEIDOU);
        layout.addView(cbMOSTRARnaoUsado);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Configurar visualização");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // aplicar e salvar
                MOSTRARGPS = cbGPS.isChecked();
                MOSTRARGLONASS = cbGLONASS.isChecked();
                MOSTRARGALILEO = cbGALILEO.isChecked();
                MOSTRARBEIDOU = cbBEIDOU.isChecked();
                MOSTRARnaoUsado = cbMOSTRARnaoUsado.isChecked();

                savePreferences();
                invalidate(); // redesenhar com nova configuração
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}
