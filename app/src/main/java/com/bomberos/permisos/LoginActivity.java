package com.bomberos.permisos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.utils.ApiHelper;
import com.bomberos.permisos.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentificador;
    private EditText etPassword;
    private Button btnEntrar;
    private ProgressBar progressBar;

    private SessionManager session;
    private ApiHelper api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etIdentificador = findViewById(R.id.etIdentificador);
        etPassword = findViewById(R.id.etPassword);
        btnEntrar = findViewById(R.id.btnEntrar);
        progressBar = findViewById(R.id.progressBar);

        session = new SessionManager(this);
        api = ApiHelper.getInstance(this);

        btnEntrar.setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {
        String identificador = etIdentificador.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (identificador.isEmpty()) {
            etIdentificador.setError("Campo obligatorio");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Campo obligatorio");
            return;
        }

        mostrarCarga(true);

        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("identificador", identificador);
            cuerpo.put("password", password);

            api.postPublico("auth/login", cuerpo,
                    response -> {
                        mostrarCarga(false);
                        procesarRespuestaLogin(response);
                    },
                    error -> {
                        mostrarCarga(false);
                        Toast.makeText(this, ApiHelper.mensajeError(error), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            mostrarCarga(false);
            Toast.makeText(this, "Error inesperado", Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarRespuestaLogin(JSONObject response) {
        try {
            JSONObject data = response.getJSONObject("data");
            String token = data.getString("token");
            JSONObject usuario = data.getJSONObject("usuario");

            session.guardarToken(token);
            session.guardarUsuario(
                    usuario.getString("_id"),
                    usuario.getString("nombre"),
                    usuario.getString("email"),
                    usuario.getString("rol"),
                    usuario.optString("documento", ""),
                    usuario.optString("cargo", "")
            );

            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(fcmToken -> {
                try {
                    JSONObject cuerpoFcm = new JSONObject();
                    cuerpoFcm.put("token", fcmToken);
                    api.post("auth/fcm-token", cuerpoFcm, r -> {}, e -> {});
                } catch (Exception ignored) {}
            });

            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar la respuesta", Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarCarga(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!cargando);
    }
}
