package com.bomberos.permisos;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.adapter.PermisosAdapter;
import com.bomberos.permisos.utils.ApiHelper;
import com.bomberos.permisos.utils.DatabaseHelper;
import com.bomberos.permisos.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
public class PermisosActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView tvVacio;
    private TextView tvError;
    private TextView tvOffline;
    private ListView listView;
    private Button btnNuevo;
    private Button btnHistorial;
    private SessionManager session;
    private ApiHelper api;
    private DatabaseHelper db;
    private String rol;
    private boolean mostrandoTodos = false;
    private final List<JSONObject> listaPermisos = new ArrayList<>();
    private PermisosAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permisos);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Permisos PTRI");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        tvVacio = findViewById(R.id.tvVacio);
        tvError = findViewById(R.id.tvError);
        tvOffline = findViewById(R.id.tvOffline);
        listView = findViewById(R.id.listViewPermisos);
        btnNuevo = findViewById(R.id.btnNuevo);
        btnHistorial = findViewById(R.id.btnHistorial);
        session = new SessionManager(this);
        api = ApiHelper.getInstance(this);
        db = DatabaseHelper.getInstance(this);
        rol = session.getRol();
        adapter = new PermisosAdapter(this, listaPermisos, rol, this::onAccionPermiso);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) ->
                mostrarDetallePermiso(listaPermisos.get(position)));
        if ("SOLICITANTE".equals(rol) || "ADMIN".equals(rol)) {
            btnNuevo.setVisibility(View.VISIBLE);
            btnNuevo.setOnClickListener(v ->
                    startActivity(new Intent(this, PlanoActivity.class)));
        }
        if ("JEFE".equals(rol) || "ADMIN".equals(rol)) {
            btnHistorial.setVisibility(View.VISIBLE);
            btnHistorial.setOnClickListener(v -> {
                mostrandoTodos = !mostrandoTodos;
                btnHistorial.setText(mostrandoTodos ?
                        getString(R.string.permisos_activos) :
                        getString(R.string.permisos_historial));
                cargarPermisos();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPermisos();
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    private void cargarPermisos() {
        mostrarEstado("cargando");

        String endpoint = "permisos-operativos";
        if (mostrandoTodos) endpoint += "?todos=true";

        api.get(endpoint,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        db.guardarCachePermisos(data.toString());
                        listaPermisos.clear();
                        for (int i = 0; i < data.length(); i++) {
                            listaPermisos.add(data.getJSONObject(i));
                        }
                        if (listaPermisos.isEmpty()) {
                            mostrarEstado("vacio");
                        } else {
                            mostrarEstado("lista");
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        mostrarEstado("error");
                        tvError.setText("Error al procesar la respuesta");
                    }
                },
                error -> {
                    String cacheJson = db.getCachePermisos();
                    if (cacheJson != null) {
                        try {
                            JSONArray data = new JSONArray(cacheJson);
                            listaPermisos.clear();
                            for (int i = 0; i < data.length(); i++) {
                                listaPermisos.add(data.getJSONObject(i));
                            }
                            mostrarEstado(listaPermisos.isEmpty() ? "vacio" : "lista_offline");
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            mostrarEstado("error");
                            tvError.setText(ApiHelper.mensajeError(error));
                        }
                    } else {
                        mostrarEstado("error");
                        tvError.setText(ApiHelper.mensajeError(error));
                    }
                });
    }

    private void mostrarEstado(String estado) {
        progressBar.setVisibility("cargando".equals(estado) ? View.VISIBLE : View.GONE);
        tvVacio.setVisibility("vacio".equals(estado) ? View.VISIBLE : View.GONE);
        tvError.setVisibility("error".equals(estado) ? View.VISIBLE : View.GONE);
        tvOffline.setVisibility("lista_offline".equals(estado) ? View.VISIBLE : View.GONE);
        listView.setVisibility(
                "lista".equals(estado) || "lista_offline".equals(estado)
                        ? View.VISIBLE : View.GONE);
    }
    private void mostrarDetallePermiso(JSONObject permiso) {
        try {
            String estado = permiso.optString("estado", "");
            String edificio = permiso.optString("edificio", "").replace("-", " ").toUpperCase();
            String solicit = permiso.optJSONObject("solicitante") != null
                    ? permiso.getJSONObject("solicitante").optString("nombre", "")
                    : permiso.optString("solicitante", "");

            StringBuilder sb = new StringBuilder();
            sb.append("Zona: ").append(edificio).append("\n");
            sb.append("Estado: ").append(estado).append("\n");
            if (!solicit.isEmpty()) sb.append("Solicitante: ").append(solicit).append("\n");

            String motivo = permiso.optString("motivo_rechazo", "");
            if (!motivo.isEmpty() && !motivo.equals("null")) {
                sb.append("\nMotivo: ").append(motivo);
            }

            JSONArray historico = permiso.optJSONArray("historico");
            if ((motivo.isEmpty() || motivo.equals("null")) && historico != null && historico.length() > 0) {
                JSONObject ultimo = historico.getJSONObject(historico.length() - 1);
                String motivoHist = ultimo.optString("motivo", "");
                if (!motivoHist.isEmpty() && !motivoHist.equals("null")) {
                    sb.append("\nMotivo: ").append(motivoHist);
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Detalle del PTRI")
                    .setMessage(sb.toString())
                    .setPositiveButton("Cerrar", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al mostrar detalle", Toast.LENGTH_SHORT).show();
        }
    }
    private void onAccionPermiso(String accion, JSONObject permiso) {
        try {
            String id = permiso.getString("_id");
            switch (accion) {
                case "EVALUAR":   mostrarDialogoEvaluar(id); break;
                case "AUTORIZAR": autorizarPermiso(id); break;
                case "RECHAZAR":  mostrarDialogoRechazar(id); break;
                case "IMPLANTAR": mostrarDialogoImplantar(id); break;
                case "CERRAR":    mostrarDialogoCerrar(id); break;
                case "ANULAR":    mostrarDialogoAnular(id); break;
                case "BORRAR":    mostrarDialogoBorrar(id); break;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error inesperado", Toast.LENGTH_SHORT).show();
        }
    }
    private void mostrarDialogoEvaluar(String id) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_evaluar, null);
        String[][] medidasDef = {
            {"RETIRAR_COMBUSTIBLE",    "Retirar material combustible de la zona"},
            {"APANTALLAR_IGNIFUGO",    "Apantallar con material ignífugo"},
            {"PROTEGER_ABERTURAS",     "Proteger aberturas y pasos adyacentes"},
            {"ACOPIO_COMBUSTIBLE",     "Sin acopio de material combustible"},
            {"DESPLAZAMIENTO_MULTIPLE","Trabajos con desplazamiento múltiple"},
            {"MANTENER_LIMPIO",        "Mantener la zona libre de combustibles"},
            {"AVISAR_PCI_FINALIZAR",   "Avisar al PCI-EC al finalizar"},
            {"PROTEGER_EQUIPOS",       "Proteger equipos y materiales adyacentes"},
            {"ZONA_ATEX",              "Zona ATEX — precauciones adicionales"},
            {"VIGILANCIA_CONTINUA",    "Vigilancia continua del PTRI"}
        };
        LinearLayout layoutMedidas = vista.findViewById(R.id.layoutMedidasPci);
        List<CheckBox> checkboxes = new ArrayList<>();
        for (String[] medida : medidasDef) {
            CheckBox cb = new CheckBox(this);
            cb.setText(medida[1]);
            cb.setTag(medida[0]);
            layoutMedidas.addView(cb);
            checkboxes.add(cb);
        }
        EditText etMedios = vista.findViewById(R.id.etMediosPci);
        EditText etPrecauciones = vista.findViewById(R.id.etPrecauciones);
        new AlertDialog.Builder(this)
                .setTitle("Evaluación PCI — PTRI")
                .setView(vista)
                .setPositiveButton("Enviar", (dialog, which) -> {
                    List<String> medidas = new ArrayList<>();
                    for (CheckBox cb : checkboxes) {
                        if (cb.isChecked()) medidas.add((String) cb.getTag());
                    }
                    evaluarPermiso(id, medidas,
                            etMedios.getText().toString().trim(),
                            etPrecauciones.getText().toString().trim());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void evaluarPermiso(String id, List<String> medidas,
                                String mediosPci, String precauciones) {
        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("medidas_pci", new JSONArray(medidas));
            cuerpo.put("medios_pci_zona", mediosPci);
            cuerpo.put("precauciones_especiales", precauciones);
            llamarMutacion("permisos-operativos/" + id + "/evaluar", cuerpo,
                    "Permiso evaluado correctamente");
        } catch (Exception e) {
            Toast.makeText(this, "Error al preparar la petición", Toast.LENGTH_SHORT).show();
        }
    }

    private void autorizarPermiso(String id) {
        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("motivo", "Autorizado");
            llamarMutacion("permisos-operativos/" + id + "/autorizar", cuerpo,
                    "Permiso autorizado");
        } catch (Exception e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoRechazar(String id) {
        EditText etMotivo = new EditText(this);
        etMotivo.setHint(getString(R.string.hint_motivo_rechazo));
        etMotivo.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        new AlertDialog.Builder(this)
                .setTitle("Rechazar PTRI")
                .setView(etMotivo)
                .setPositiveButton("Rechazar", (dialog, which) -> {
                    String motivo = etMotivo.getText().toString().trim();
                    if (motivo.isEmpty()) {
                        Toast.makeText(this, "El motivo es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject cuerpo = new JSONObject();
                        cuerpo.put("motivo", motivo);
                        llamarMutacion("permisos-operativos/" + id + "/rechazar", cuerpo,
                                "Permiso rechazado");
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoImplantar(String id) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_implantar, null);
        EditText etExtintor = vista.findViewById(R.id.etExtintor);
        CheckBox cbInspeccion = vista.findViewById(R.id.cbInspeccionInicial);
        CheckBox cbAvisoSala = vista.findViewById(R.id.cbAvisoSalaInicio);

        new AlertDialog.Builder(this)
                .setTitle("Implantar PTRI")
                .setView(vista)
                .setPositiveButton("Implantar", (dialog, which) -> {
                    try {
                        JSONObject cuerpo = new JSONObject();
                        cuerpo.put("extintor", etExtintor.getText().toString().trim());
                        cuerpo.put("inspeccion_inicial", cbInspeccion.isChecked());
                        cuerpo.put("aviso_sala_control_inicio", cbAvisoSala.isChecked());
                        llamarMutacion("permisos-operativos/" + id + "/implantar", cuerpo,
                                "Trabajo implantado");
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoCerrar(String id) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_cerrar, null);
        CheckBox cbInspeccionFinal = vista.findViewById(R.id.cbInspeccionFinal);
        CheckBox cbAvisoSala = vista.findViewById(R.id.cbAvisoSalaCierre);
        CheckBox cbNoSeRealiza = vista.findViewById(R.id.cbNoSeRealiza);
        EditText etObservaciones = vista.findViewById(R.id.etObservaciones);

        new AlertDialog.Builder(this)
                .setTitle("Cerrar PTRI")
                .setView(vista)
                .setPositiveButton("Cerrar", (dialog, which) -> {
                    try {
                        JSONObject cuerpo = new JSONObject();
                        cuerpo.put("inspeccion_final", cbInspeccionFinal.isChecked());
                        cuerpo.put("aviso_sala_control_cierre", cbAvisoSala.isChecked());
                        cuerpo.put("no_se_realiza", cbNoSeRealiza.isChecked());
                        cuerpo.put("observaciones_cierre", etObservaciones.getText().toString().trim());
                        llamarMutacion("permisos-operativos/" + id + "/cerrar", cuerpo,
                                "PTRI cerrado correctamente");
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoAnular(String id) {
        EditText etMotivo = new EditText(this);
        etMotivo.setHint(getString(R.string.hint_motivo_anulacion));
        etMotivo.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Anular PTRI")
                .setView(etMotivo)
                .setPositiveButton("Anular", (dialog, which) -> {
                    try {
                        JSONObject cuerpo = new JSONObject();
                        cuerpo.put("motivo", etMotivo.getText().toString().trim());
                        llamarMutacion("permisos-operativos/" + id + "/anular", cuerpo,
                                "Permiso anulado");
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoBorrar(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar PTRI")
                .setMessage("¿Seguro que quieres eliminar este permiso? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        api.delete("permisos-operativos/" + id,
                                response -> {
                                    Toast.makeText(this, "Permiso eliminado", Toast.LENGTH_SHORT).show();
                                    cargarPermisos();
                                },
                                error -> Toast.makeText(this,
                                        ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void llamarMutacion(String endpoint, JSONObject cuerpo, String mensajeOk) {
        mostrarEstado("cargando");
        api.post(endpoint, cuerpo,
                response -> {
                    Toast.makeText(this, mensajeOk, Toast.LENGTH_SHORT).show();
                    cargarPermisos();
                },
                error -> {
                    Toast.makeText(this, ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show();
                    cargarPermisos();
                });
    }
}
