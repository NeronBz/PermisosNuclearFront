package com.bomberos.permisos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bomberos.permisos.service.TurnoAlarmReceiver;
import com.bomberos.permisos.utils.ApiHelper;
import com.bomberos.permisos.utils.DatabaseHelper;
import com.bomberos.permisos.utils.SessionManager;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

public class FichajeActivity extends AppCompatActivity {

    private TextView tvEstado;
    private TextView tvHoraEntrada;
    private TextView tvGps;
    private LinearLayout layoutAbierto;
    private LinearLayout layoutCerrado;
    private LinearLayout layoutHistorial;
    private TextView tvHistorialVacio;
    private Button btnEntrada;
    private Button btnSalida;
    private CheckBox cbRecarga;
    private ProgressBar progressBar;

    private ApiHelper api;
    private SessionManager session;
    private DatabaseHelper db;

    private Double lastLat = null;
    private Double lastLng = null;

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    perms -> {
                        Boolean fine = perms.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarse = perms.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                            obtenerUbicacion();
                        } else {
                            tvGps.setText(getString(R.string.fichaje_gps_sin_permiso));
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fichaje);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Fichaje");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvEstado = findViewById(R.id.tvEstado);
        tvHoraEntrada = findViewById(R.id.tvHoraEntrada);
        tvGps = findViewById(R.id.tvGps);
        layoutAbierto = findViewById(R.id.layoutAbierto);
        layoutCerrado = findViewById(R.id.layoutCerrado);
        layoutHistorial = findViewById(R.id.layoutHistorial);
        tvHistorialVacio = findViewById(R.id.tvHistorialVacio);
        btnEntrada = findViewById(R.id.btnEntrada);
        btnSalida = findViewById(R.id.btnSalida);
        cbRecarga = findViewById(R.id.cbRecarga);
        progressBar = findViewById(R.id.progressBar);

        api = ApiHelper.getInstance(this);
        session = new SessionManager(this);
        db = DatabaseHelper.getInstance(this);

        btnEntrada.setOnClickListener(v -> registrarEntrada());
        btnSalida.setOnClickListener(v -> registrarSalida());

        pedirUbicacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEstado();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void pedirUbicacion() {
        tvGps.setText(getString(R.string.fichaje_gps_obteniendo));
        String fine = Manifest.permission.ACCESS_FINE_LOCATION;
        String coarse = Manifest.permission.ACCESS_COARSE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        } else {
            permLauncher.launch(new String[]{fine, coarse});
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacion() {
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLat = location.getLatitude();
                        lastLng = location.getLongitude();
                        tvGps.setText(String.format("📍 %.5f, %.5f", lastLat, lastLng));
                    } else {
                        tvGps.setText(getString(R.string.fichaje_gps_error));
                    }
                })
                .addOnFailureListener(e ->
                        tvGps.setText(getString(R.string.fichaje_gps_error)));
    }

    private void cargarEstado() {
        progressBar.setVisibility(View.VISIBLE);
        api.get("fichajes/mi-fichaje-abierto",
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject data = response.optJSONObject("data");
                        if (data != null && !data.isNull("horaEntrada")) {
                            mostrarAbierto(data.optString("horaEntrada", ""));
                        } else {
                            mostrarCerrado();
                        }
                    } catch (Exception e) {
                        mostrarCerrado();
                    }
                    cargarHistorial();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    String[] local = db.getFichajeActivo();
                    if (local != null) {
                        mostrarAbierto(local[1]);
                    } else {
                        mostrarCerrado();
                    }
                    cargarHistorial();
                });
    }

    private void cargarHistorial() {
        api.get("fichajes?limit=10",
                response -> {
                    try {
                        JSONArray data = response.optJSONArray("data");
                        renderHistorial(data);
                    } catch (Exception e) {
                        tvHistorialVacio.setVisibility(View.VISIBLE);
                    }
                },
                error -> tvHistorialVacio.setVisibility(View.VISIBLE));
    }

    private void mostrarAbierto(String horaEntrada) {
        layoutAbierto.setVisibility(View.VISIBLE);
        layoutCerrado.setVisibility(View.GONE);
        tvEstado.setText(getString(R.string.fichaje_activo));
        String hora = horaEntrada.replace("T", " ");
        if (hora.length() > 16) hora = hora.substring(0, 16);
        tvHoraEntrada.setText("Entrada: " + hora);
    }

    private void mostrarCerrado() {
        layoutAbierto.setVisibility(View.GONE);
        layoutCerrado.setVisibility(View.VISIBLE);
        tvEstado.setText(getString(R.string.fichaje_sin_entrada));
    }

    private void registrarEntrada() {
        progressBar.setVisibility(View.VISIBLE);
        btnEntrada.setEnabled(false);
        try {
            JSONObject cuerpo = new JSONObject();
            if (lastLat != null && lastLng != null) {
                JSONArray coordenadas = new JSONArray();
                coordenadas.put(lastLat);
                coordenadas.put(lastLng);
                cuerpo.put("coordenadas", coordenadas);
                cuerpo.put("ubicacion", String.format("%.5f, %.5f", lastLat, lastLng));
            }

            api.post("fichajes/entrada", cuerpo,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        btnEntrada.setEnabled(true);
                        try {
                            JSONObject data = response.optJSONObject("data");
                            String id = data != null ? data.optString("_id", "") : "";
                            String hora = data != null ? data.optString("horaEntrada", "") : "";
                            String ubi = data != null ? data.optString("ubicacionEntrada", "") : "";
                            db.guardarFichajeActivo(id, hora, ubi);
                            long horas = cbRecarga.isChecked() ? 12L : 8L;
                            programarAlarmaTurno(horas);
                            Toast.makeText(this, "Entrada registrada", Toast.LENGTH_SHORT).show();
                            cargarEstado();
                        } catch (Exception e) {
                            cargarEstado();
                        }
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnEntrada.setEnabled(true);
                        Toast.makeText(this, ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnEntrada.setEnabled(true);
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void registrarSalida() {
        progressBar.setVisibility(View.VISIBLE);
        btnSalida.setEnabled(false);
        api.post("fichajes/salida", new JSONObject(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btnSalida.setEnabled(true);
                    db.borrarFichajeActivo();
                    cancelarAlarmaTurno();
                    Toast.makeText(this, "Salida registrada", Toast.LENGTH_SHORT).show();
                    cargarEstado();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnSalida.setEnabled(true);
                    Toast.makeText(this, ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show();
                });
    }

    private void renderHistorial(JSONArray fichajes) {
        layoutHistorial.removeAllViews();
        if (fichajes == null || fichajes.length() == 0) {
            tvHistorialVacio.setVisibility(View.VISIBLE);
            return;
        }
        tvHistorialVacio.setVisibility(View.GONE);
        for (int i = 0; i < Math.min(fichajes.length(), 10); i++) {
            try {
                JSONObject f = fichajes.getJSONObject(i);
                String entrada = f.optString("horaEntrada", "").replace("T", " ");
                if (entrada.length() > 16) entrada = entrada.substring(0, 16);
                String salida = f.optString("horaSalida", "");
                String ubicacion = f.optString("ubicacionEntrada", "");
                String texto;
                if (salida.isEmpty() || salida.equals("null")) {
                    texto = "▶ " + entrada + "  (en curso)";
                } else {
                    salida = salida.replace("T", " ");
                    if (salida.length() > 16) salida = salida.substring(0, 16);
                    texto = "▶ " + entrada + "  -  " + salida;
                }
                if (!ubicacion.isEmpty() && !ubicacion.equals("null")) {
                    texto += "\n   📍 " + ubicacion;
                }
                TextView tv = new TextView(this);
                tv.setText(texto);
                tv.setTextSize(13f);
                tv.setPadding(0, 4, 0, 8);
                layoutHistorial.addView(tv);
            } catch (Exception ignored) {}
        }
    }

    private void programarAlarmaTurno(long horas) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TurnoAlarmReceiver.class);
        intent.putExtra("horas", horas);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long trigger = System.currentTimeMillis() + horas * 3600 * 1000;
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }

    private void cancelarAlarmaTurno() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TurnoAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) alarmManager.cancel(pi);
    }
}
