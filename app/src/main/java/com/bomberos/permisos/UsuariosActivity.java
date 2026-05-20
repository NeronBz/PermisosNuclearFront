package com.bomberos.permisos;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.adapter.UsuariosAdapter;
import com.bomberos.permisos.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UsuariosActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvVacio;
    private ListView listView;
    private Button btnNuevo;

    private ApiHelper api;
    private final List<JSONObject> listaUsuarios = new ArrayList<>();
    private UsuariosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestión de usuarios");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        tvVacio = findViewById(R.id.tvVacio);
        listView = findViewById(R.id.listViewUsuarios);
        btnNuevo = findViewById(R.id.btnNuevo);

        api = ApiHelper.getInstance(this);

        adapter = new UsuariosAdapter(this, listaUsuarios, this::editarUsuario);
        listView.setAdapter(adapter);

        btnNuevo.setOnClickListener(v -> mostrarDialogoCrear());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void cargarUsuarios() {
        progressBar.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        api.get("usuarios",
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray data = response.getJSONArray("data");
                        List<JSONObject> temp = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            temp.add(data.getJSONObject(i));
                        }
                        final List<String> orden = Arrays.asList(
                                "ADMIN", "JEFE", "BOMBERO", "SOLICITANTE");
                        Collections.sort(temp, (a, b) -> {
                            int ia = orden.indexOf(a.optString("rol", ""));
                            int ib = orden.indexOf(b.optString("rol", ""));
                            if (ia < 0) ia = orden.size();
                            if (ib < 0) ib = orden.size();
                            return Integer.compare(ia, ib);
                        });
                        listaUsuarios.clear();
                        listaUsuarios.addAll(temp);
                        if (listaUsuarios.isEmpty()) {
                            tvVacio.setVisibility(View.VISIBLE);
                        } else {
                            listView.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        tvVacio.setText("Error al cargar usuarios");
                        tvVacio.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    tvVacio.setText(ApiHelper.mensajeError(error));
                    tvVacio.setVisibility(View.VISIBLE);
                });
    }

    private void mostrarDialogoCrear() {
        View vista = getLayoutInflater().inflate(R.layout.dialog_usuario, null);
        EditText etNombre = vista.findViewById(R.id.etNombre);
        EditText etDoc = vista.findViewById(R.id.etDocumento);
        EditText etEmail = vista.findViewById(R.id.etEmail);
        EditText etPassword = vista.findViewById(R.id.etPassword);
        EditText etCargo = vista.findViewById(R.id.etCargo);
        Spinner spinnerRol = vista.findViewById(R.id.spinnerRol);

        String[] roles = {"BOMBERO", "JEFE", "SOLICITANTE", "ADMIN"};
        ArrayAdapter<String> adaptadorRol = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        adaptadorRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adaptadorRol);

        new AlertDialog.Builder(this)
                .setTitle("Crear usuario")
                .setView(vista)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String doc = etDoc.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    String cargo = etCargo.getText().toString().trim();
                    String rol = (String) spinnerRol.getSelectedItem();

                    if (nombre.isEmpty() || doc.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Rellena los campos obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject cuerpo = new JSONObject();
                        cuerpo.put("nombre", nombre);
                        cuerpo.put("documento", doc);
                        cuerpo.put("email", email);
                        cuerpo.put("password", password);
                        cuerpo.put("rol", rol);
                        if (!cargo.isEmpty()) cuerpo.put("cargo", cargo);

                        api.post("usuarios", cuerpo,
                                resp -> {
                                    Toast.makeText(this, "Usuario creado", Toast.LENGTH_SHORT).show();
                                    cargarUsuarios();
                                },
                                err -> Toast.makeText(this,
                                        ApiHelper.mensajeError(err), Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void editarUsuario(JSONObject usuario) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_editar_usuario, null);
        EditText etCargo = vista.findViewById(R.id.etCargo);
        Spinner spinnerRol = vista.findViewById(R.id.spinnerRol);
        CheckBox cbActivo = vista.findViewById(R.id.cbActivo);

        String[] roles = {"BOMBERO", "JEFE", "SOLICITANTE", "ADMIN"};
        ArrayAdapter<String> adaptadorRol = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        adaptadorRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adaptadorRol);

        try {
            String cargoActual = usuario.optString("cargo", "");
            String rolActual = usuario.optString("rol", "BOMBERO");
            boolean activo = usuario.optBoolean("activo", true);
            etCargo.setText(cargoActual);
            cbActivo.setChecked(activo);
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equals(rolActual)) { spinnerRol.setSelection(i); break; }
            }
        } catch (Exception ignored) {}

        String nombre = usuario.optString("nombre", "Usuario");
        new AlertDialog.Builder(this)
                .setTitle("Editar: " + nombre)
                .setView(vista)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    try {
                        String id = usuario.getString("_id");
                        JSONObject cuerpo = new JSONObject();
                        cuerpo.put("cargo", etCargo.getText().toString().trim());
                        cuerpo.put("rol", spinnerRol.getSelectedItem().toString());
                        cuerpo.put("activo", cbActivo.isChecked());

                        api.patch("usuarios/" + id, cuerpo,
                                resp -> {
                                    Toast.makeText(this, "Usuario actualizado", Toast.LENGTH_SHORT).show();
                                    cargarUsuarios();
                                },
                                err -> Toast.makeText(this,
                                        ApiHelper.mensajeError(err), Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
