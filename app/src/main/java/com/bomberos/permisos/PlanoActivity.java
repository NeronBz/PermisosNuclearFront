package com.bomberos.permisos;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bomberos.permisos.utils.ApiHelper;
import com.bomberos.permisos.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlanoActivity extends AppCompatActivity {

    private static final String ZONA_REACTOR = "zona-reactor";
    private static final String ZONA_TURBINA = "zona-turbina";
    private static final String ZONA_ALMACEN = "zona-almacen";

    private static final List<String> ESTADOS_ACTIVOS = Arrays.asList(
            "PENDIENTE", "EVALUADO", "AUTORIZADO", "EN_EJECUCION");

    private FrameLayout mapaContainer;
    private Button btnReactor;
    private Button btnTurbina;
    private Button btnAlmacen;
    private ProgressBar progressBar;

    private ApiHelper api;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plano);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mapaContainer = findViewById(R.id.mapaContainer);
        btnReactor = findViewById(R.id.btnReactor);
        btnTurbina = findViewById(R.id.btnTurbina);
        btnAlmacen = findViewById(R.id.btnAlmacen);
        progressBar = findViewById(R.id.progressBar);

        FloatingActionButton fabVolver = findViewById(R.id.fabVolver);
        fabVolver.setOnClickListener(v -> finish());

        api = ApiHelper.getInstance(this);
        session = new SessionManager(this);

        mapaContainer.post(() -> {
            posicionarBoton(btnReactor, 0.18f, 0.25f);
            posicionarBoton(btnTurbina, 0.55f, 0.42f);
            posicionarBoton(btnAlmacen, 0.72f, 0.68f);
        });

        btnReactor.setOnClickListener(v -> mostrarFormulario(ZONA_REACTOR, "Reactor"));
        btnTurbina.setOnClickListener(v -> mostrarFormulario(ZONA_TURBINA, "Turbina"));
        btnAlmacen.setOnClickListener(v -> mostrarFormulario(ZONA_ALMACEN, "Almacén"));

        cargarEstadoZonas();
    }

    private void cargarEstadoZonas() {
        progressBar.setVisibility(View.VISIBLE);
        api.get("permisos-operativos",
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray permisos = response.getJSONArray("data");
                        boolean reactorActivo = false;
                        boolean turbinaActivo = false;
                        boolean almacenActivo = false;

                        for (int i = 0; i < permisos.length(); i++) {
                            JSONObject p = permisos.getJSONObject(i);
                            String edificio = p.optString("edificio", "");
                            String estado = p.optString("estado", "");
                            if (!ESTADOS_ACTIVOS.contains(estado)) continue;

                            if (ZONA_REACTOR.equals(edificio)) reactorActivo = true;
                            if (ZONA_TURBINA.equals(edificio)) turbinaActivo = true;
                            if (ZONA_ALMACEN.equals(edificio)) almacenActivo = true;
                        }

                        colorearBoton(btnReactor, reactorActivo);
                        colorearBoton(btnTurbina, turbinaActivo);
                        colorearBoton(btnAlmacen, almacenActivo);

                    } catch (Exception ignored) {}
                },
                error -> progressBar.setVisibility(View.GONE));
    }

    private void colorearBoton(Button btn, boolean activo) {
        int color = ContextCompat.getColor(this,
                activo ? R.color.alert_red : R.color.nuclear_blue);
        btn.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void posicionarBoton(Button btn, float xPct, float yPct) {
        btn.setX(mapaContainer.getWidth() * xPct);
        btn.setY(mapaContainer.getHeight() * yPct);
    }

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
        CheckBox cbSoldTig = vista.findViewById(R.id.cbSoldaduraTig);
        CheckBox cbCorteRad = vista.findViewById(R.id.cbCorteRadial);
        CheckBox cbLanza = vista.findViewById(R.id.cbLanzaTermica);
        CheckBox cbSoplete = vista.findViewById(R.id.cbCorteSoplete);
        CheckBox cbDisten = vista.findViewById(R.id.cbDistensionado);
        CheckBox cbOtros = vista.findViewById(R.id.cbOtros);

        TextView tvHerramientasLabel = vista.findViewById(R.id.tvHerramientasLabel);
        ProgressBar progressHerramientas = vista.findViewById(R.id.progressHerramientas);
        LinearLayout layoutHerramientas = vista.findViewById(R.id.layoutHerramientas);
        TextView tvHerramientasVacio = vista.findViewById(R.id.tvHerramientasVacio);

        // Se ejecuta cada vez que cambia cualquier checkbox de tipo de trabajo
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

            String param = android.net.Uri.encode(String.join(",", tipos));
            api.get("inventario?tipos_trabajo=" + param + "&estado=DISPONIBLE&activo=true",
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
}
