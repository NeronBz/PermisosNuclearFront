package com.bomberos.permisos;

import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bomberos.permisos.utils.ApiHelper;
import com.bomberos.permisos.utils.PlanoCoordMapper;
import com.bomberos.permisos.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlanoActivity extends AppCompatActivity {

    private static final String TAG = "PlanoActivity";

    private static final List<String> ESTADOS_ACTIVOS = Arrays.asList(
            "PENDIENTE", "EVALUADO", "AUTORIZADO", "EN_EJECUCION");

    private FrameLayout mapaContainer;
    private ImageView planoImage;
    private ProgressBar progressBar;

    private ApiHelper api;
    private SessionManager session;

    /** Botones generados dinámicamente, indexados por zonaId. */
    private final Map<String, Button> botonesZona = new HashMap<>();

    /** Configuración de zonas cargada desde assets/zonas_almaraz.json. */
    private final List<Zona> zonas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forzar landscape: el plano tiene aspect ratio 3:2 y se aprovecha
        // mucho mejor en horizontal. El resto de la app sigue en portrait.
        setRequestedOrientation(
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plano);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mapaContainer = findViewById(R.id.mapaContainer);
        planoImage    = findViewById(R.id.planoImage);
        progressBar   = findViewById(R.id.progressBar);

        FloatingActionButton fabVolver = findViewById(R.id.fabVolver);
        fabVolver.setOnClickListener(v -> finish());

        api     = ApiHelper.getInstance(this);
        session = new SessionManager(this);

        cargarZonasDesdeAssets();

        // Esperar a que el ImageView tenga dimensiones reales antes de
        // posicionar los botones encima
        planoImage.post(() -> {
            crearBotonesZona();
            cargarEstadoZonas();
        });
    }

    // ===================================================================
    //   CARGA DE ZONAS DESDE assets/zonas_almaraz.json
    // ===================================================================

    private void cargarZonasDesdeAssets() {
        zonas.clear();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getAssets().open("zonas_almaraz.json"),
                        StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) sb.append(linea);

            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr   = root.getJSONArray("zonas");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject z = arr.getJSONObject(i);
                zonas.add(new Zona(
                        z.getString("id"),
                        z.getString("nombre"),
                        z.optString("tipo", "GENERICO"),
                        (float) z.getDouble("x"),
                        (float) z.getDouble("y"),
                        z.optString("criticidad", "MEDIA")));
            }
        } catch (Exception e) {
            Log.e(TAG, "No se pudo cargar zonas_almaraz.json", e);
            Toast.makeText(this, "Error al cargar el plano", Toast.LENGTH_LONG).show();
        }
    }

    // ===================================================================
    //   CREACIÓN DINÁMICA DE BOTONES SOBRE EL PLANO
    // ===================================================================

    private void crearBotonesZona() {
        botonesZona.clear();

        PlanoCoordMapper mapper = new PlanoCoordMapper(planoImage);
        if (!mapper.estaPreparado()) return;

        int tamPx = dpAPx(36);  // tamaño del botón en píxeles

        for (Zona z : zonas) {
            PointF centro = mapper.logicoAPixel(z.x, z.y);

            Button b = new Button(this);
            b.setText(etiquetaCorta(z));
            b.setAllCaps(false);
            b.setTextSize(9f);
            b.setTextColor(ContextCompat.getColor(this, R.color.white));
            b.setPadding(0, 0, 0, 0);
            b.setMinimumWidth(0);
            b.setMinimumHeight(0);
            // Fondo circular con borde blanco — definido en bg_boton_zona.xml
            b.setBackgroundResource(R.drawable.bg_boton_zona);
            b.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.nuclear_blue)));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tamPx, tamPx);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.leftMargin = (int) (centro.x - tamPx / 2f);
            lp.topMargin  = (int) (centro.y - tamPx / 2f);

            b.setOnClickListener(v -> mostrarFormulario(z.id, z.nombre));

            mapaContainer.addView(b, lp);
            botonesZona.put(z.id, b);
        }
    }

    /** Genera una etiqueta corta a partir del id (zona-reactor-2 → "R-2"). */
    private String etiquetaCorta(Zona z) {
        String[] partes = z.id.split("-");
        if (partes.length < 2) return z.id;
        StringBuilder sb = new StringBuilder();
        // Inicial del tipo
        sb.append(Character.toUpperCase(partes[1].charAt(0)));
        // Número o letra final si la hay
        if (partes.length >= 3) sb.append("-").append(partes[2].toUpperCase());
        return sb.toString();
    }

    // ===================================================================
    //   ESTADO DE ZONAS (consulta a /permisos-operativos)
    // ===================================================================

    private void cargarEstadoZonas() {
        progressBar.setVisibility(View.VISIBLE);
        api.get("permisos-operativos",
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray permisos = response.getJSONArray("data");

                        // Reset: ninguna zona activa
                        for (Button b : botonesZona.values()) {
                            colorearBoton(b, false);
                        }

                        // Marcar zonas con permisos en estado activo
                        for (int i = 0; i < permisos.length(); i++) {
                            JSONObject p = permisos.getJSONObject(i);
                            String edificio = p.optString("edificio", "");
                            String estado   = p.optString("estado", "");
                            if (!ESTADOS_ACTIVOS.contains(estado)) continue;
                            Button b = botonesZona.get(edificio);
                            if (b != null) colorearBoton(b, true);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al parsear permisos-operativos", e);
                    }
                },
                error -> progressBar.setVisibility(View.GONE));
    }

    private void colorearBoton(Button btn, boolean activo) {
        int color = ContextCompat.getColor(this,
                activo ? R.color.alert_red : R.color.nuclear_blue);
        btn.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    // ===================================================================
    //   FORMULARIO DE NUEVO PERMISO (sin cambios respecto a v1)
    // ===================================================================

    private void mostrarFormulario(String zoneId, String nombreZona) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_permiso_form, null);

        String hora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        ((TextView) vista.findViewById(R.id.tvHoraActual)).setText(hora);
        ((TextView) vista.findViewById(R.id.tvSolicitante)).setText(session.getNombre());
        ((TextView) vista.findViewById(R.id.tvZona)).setText(nombreZona);

        Switch switchZona = vista.findViewById(R.id.switchZonaControlada);
        EditText etResponsable = vista.findViewById(R.id.etResponsable);
        EditText etDescripcion = vista.findViewById(R.id.etDescripcion);

        CheckBox cbSoldElec = vista.findViewById(R.id.cbSoldaduraElectrica);
        CheckBox cbSoldTig  = vista.findViewById(R.id.cbSoldaduraTig);
        CheckBox cbCorteRad = vista.findViewById(R.id.cbCorteRadial);
        CheckBox cbLanza    = vista.findViewById(R.id.cbLanzaTermica);
        CheckBox cbSoplete  = vista.findViewById(R.id.cbCorteSoplete);
        CheckBox cbDisten   = vista.findViewById(R.id.cbDistensionado);
        CheckBox cbOtros    = vista.findViewById(R.id.cbOtros);

        TextView tvHerramientasLabel       = vista.findViewById(R.id.tvHerramientasLabel);
        ProgressBar progressHerramientas   = vista.findViewById(R.id.progressHerramientas);
        LinearLayout layoutHerramientas    = vista.findViewById(R.id.layoutHerramientas);
        TextView tvHerramientasVacio       = vista.findViewById(R.id.tvHerramientasVacio);

        Runnable[] actualizarRef = new Runnable[1];
        actualizarRef[0] = () -> {
            List<String> tipos = tiposSeleccionados(
                    cbSoldElec, cbSoldTig, cbCorteRad, cbLanza, cbSoplete, cbDisten, cbOtros);

            if (tipos.isEmpty()) {
                tvHerramientasLabel.setVisibility(View.GONE);
                progressHerramientas.setVisibility(View.GONE);
                layoutHerramientas.setVisibility(View.GONE);
                tvHerramientasVacio.setVisibility(View.GONE);
                return;
            }

            tvHerramientasLabel.setVisibility(View.VISIBLE);
            progressHerramientas.setVisibility(View.VISIBLE);
            layoutHerramientas.setVisibility(View.GONE);
            tvHerramientasVacio.setVisibility(View.GONE);

            api.get("inventario?estado=DISPONIBLE&activo=true",
                    response -> {
                        progressHerramientas.setVisibility(View.GONE);
                        try {
                            JSONArray data = response.optJSONArray("data");
                            layoutHerramientas.removeAllViews();
                            if (data == null || data.length() == 0) {
                                tvHerramientasVacio.setVisibility(View.VISIBLE);
                            } else {
                                layoutHerramientas.setVisibility(View.VISIBLE);
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject item = data.getJSONObject(i);
                                    String id = item.optString("_id", "");
                                    String desc = item.optString("descripcion",
                                            item.optString("codigo", "Herramienta"));
                                    CheckBox cb = new CheckBox(this);
                                    cb.setText(desc);
                                    cb.setTag(id);
                                    cb.setTextSize(13f);
                                    layoutHerramientas.addView(cb);
                                }
                            }
                        } catch (Exception e) {
                            tvHerramientasVacio.setVisibility(View.VISIBLE);
                        }
                    },
                    error -> {
                        progressHerramientas.setVisibility(View.GONE);
                        tvHerramientasVacio.setVisibility(View.VISIBLE);
                    });
        };

        CompoundButton.OnCheckedChangeListener tipoListener =
                (btn, checked) -> actualizarRef[0].run();
        cbSoldElec.setOnCheckedChangeListener(tipoListener);
        cbSoldTig.setOnCheckedChangeListener(tipoListener);
        cbCorteRad.setOnCheckedChangeListener(tipoListener);
        cbLanza.setOnCheckedChangeListener(tipoListener);
        cbSoplete.setOnCheckedChangeListener(tipoListener);
        cbDisten.setOnCheckedChangeListener(tipoListener);
        cbOtros.setOnCheckedChangeListener(tipoListener);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Abrir permiso — " + nombreZona)
                .setView(vista)
                .setPositiveButton("Abrir permiso", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.show();

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<String> tipos = tiposSeleccionados(
                    cbSoldElec, cbSoldTig, cbCorteRad, cbLanza, cbSoplete, cbDisten, cbOtros);

            if (tipos.isEmpty()) {
                Toast.makeText(this,
                        "Selecciona al menos un tipo de trabajo", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> herramientasIds = new ArrayList<>();
            for (int i = 0; i < layoutHerramientas.getChildCount(); i++) {
                View child = layoutHerramientas.getChildAt(i);
                if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
                    String id = (String) child.getTag();
                    if (id != null && !id.isEmpty()) herramientasIds.add(id);
                }
            }

            if (herramientasIds.isEmpty()) {
                Toast.makeText(this,
                        "Selecciona al menos una herramienta", Toast.LENGTH_SHORT).show();
                return;
            }

            dialogo.dismiss();
            crearPermiso(zoneId, nombreZona, tipos, switchZona.isChecked(),
                    etResponsable.getText().toString().trim(),
                    etDescripcion.getText().toString().trim(),
                    herramientasIds);
        });
    }

    private List<String> tiposSeleccionados(CheckBox soldElec, CheckBox soldTig,
                                            CheckBox corteRad, CheckBox lanza,
                                            CheckBox soplete, CheckBox disten,
                                            CheckBox otros) {
        List<String> tipos = new ArrayList<>();
        if (soldElec.isChecked()) tipos.add("SOLDADURA_ELECTRICA");
        if (soldTig.isChecked())  tipos.add("SOLDADURA_TIG");
        if (corteRad.isChecked()) tipos.add("CORTE_RADIAL");
        if (lanza.isChecked())    tipos.add("LANZA_TERMICA");
        if (soplete.isChecked())  tipos.add("CORTE_SOPLETE");
        if (disten.isChecked())   tipos.add("DISTENSIONADO");
        if (otros.isChecked())    tipos.add("OTROS");
        return tipos;
    }

    private void crearPermiso(String zoneId, String nombreZona,
                              List<String> tipos, boolean zonaControlada,
                              String responsable, String descripcion,
                              List<String> herramientasIds) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("edificio", zoneId);
            cuerpo.put("tipos_trabajo", new JSONArray(tipos));
            cuerpo.put("zona_controlada", zonaControlada);
            cuerpo.put("responsable_solicitante", responsable);
            cuerpo.put("descripcion_trabajo", descripcion);
            cuerpo.put("periodo_validez", "DIARIO");
            cuerpo.put("herramientas", new JSONArray(herramientasIds));

            api.post("permisos-operativos", cuerpo,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "Permiso abierto en zona " + nombreZona,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error al preparar la solicitud", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================================================================
    //   UTILIDADES
    // ===================================================================

    private int dpAPx(int dp) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.round(dp * dm.density);
    }

    /** Representa una zona del plano cargada desde el JSON. */
    private static class Zona {
        final String id;
        final String nombre;
        final String tipo;
        final float x, y;
        final String criticidad;

        Zona(String id, String nombre, String tipo,
             float x, float y, String criticidad) {
            this.id = id;
            this.nombre = nombre;
            this.tipo = tipo;
            this.x = x;
            this.y = y;
            this.criticidad = criticidad;
        }
    }
}
