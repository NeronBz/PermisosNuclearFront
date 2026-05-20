package com.bomberos.permisos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NuevoPermisoActivity extends AppCompatActivity {

    private EditText etEdificio;
    private EditText etCota;
    private EditText etZonaFuego;
    private EditText etDescripcion;
    private EditText etResponsable;
    private CheckBox cbSoldaduraElectrica;
    private CheckBox cbSoldaduraTig;
    private CheckBox cbCorteRadial;
    private CheckBox cbLanzaTermica;
    private CheckBox cbSoplete;
    private CheckBox cbDistensionado;
    private CheckBox cbOtros;
    private CheckBox cbZonaControlada;
    private Button btnEnviar;
    private ProgressBar progressBar;

    private ApiHelper api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_permiso);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nuevo PTRI");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etEdificio = findViewById(R.id.etEdificio);
        etCota = findViewById(R.id.etCota);
        etZonaFuego = findViewById(R.id.etZonaFuego);
        etDescripcion = findViewById(R.id.etDescripcion);
        etResponsable = findViewById(R.id.etResponsable);
        cbSoldaduraElectrica = findViewById(R.id.cbSoldaduraElectrica);
        cbSoldaduraTig = findViewById(R.id.cbSoldaduraTig);
        cbCorteRadial = findViewById(R.id.cbCorteRadial);
        cbLanzaTermica = findViewById(R.id.cbLanzaTermica);
        cbSoplete = findViewById(R.id.cbSoplete);
        cbDistensionado = findViewById(R.id.cbDistensionado);
        cbOtros = findViewById(R.id.cbOtros);
        cbZonaControlada = findViewById(R.id.cbZonaControlada);
        btnEnviar = findViewById(R.id.btnEnviar);
        progressBar = findViewById(R.id.progressBar);

        api = ApiHelper.getInstance(this);

        btnEnviar.setOnClickListener(v -> enviarPermiso());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void enviarPermiso() {
        String edificio = etEdificio.getText().toString().trim();
        if (edificio.isEmpty()) {
            etEdificio.setError("Campo obligatorio");
            return;
        }

        List<String> tipos = new ArrayList<>();
        if (cbSoldaduraElectrica.isChecked()) tipos.add("SOLDADURA_ELECTRICA");
        if (cbSoldaduraTig.isChecked())       tipos.add("SOLDADURA_TIG");
        if (cbCorteRadial.isChecked())        tipos.add("CORTE_RADIAL");
        if (cbLanzaTermica.isChecked())       tipos.add("LANZA_TERMICA");
        if (cbSoplete.isChecked())            tipos.add("CORTE_SOPLETE");
        if (cbDistensionado.isChecked())      tipos.add("DISTENSIONADO");
        if (cbOtros.isChecked())              tipos.add("OTROS");

        mostrarCarga(true);

        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("edificio", edificio);
            cuerpo.put("cota", etCota.getText().toString().trim());
            cuerpo.put("zona_fuego", etZonaFuego.getText().toString().trim());
            cuerpo.put("descripcion_trabajo", etDescripcion.getText().toString().trim());
            cuerpo.put("responsable_solicitante", etResponsable.getText().toString().trim());
            cuerpo.put("tipos_trabajo", new JSONArray(tipos));
            cuerpo.put("zona_controlada", cbZonaControlada.isChecked());
            cuerpo.put("periodo_validez", "DIARIO");

            api.post("permisos-operativos", cuerpo,
                    response -> {
                        mostrarCarga(false);
                        Toast.makeText(this, "PTRI creado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        mostrarCarga(false);
                        Toast.makeText(this, ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            mostrarCarga(false);
            Toast.makeText(this, "Error al preparar la solicitud", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarCarga(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnEnviar.setEnabled(!cargando);
    }
}
