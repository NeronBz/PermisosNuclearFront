package com.bomberos.permisos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.utils.SessionManager;

public class PerfilActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mi perfil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        session = new SessionManager(this);

        TextView tvNombre = findViewById(R.id.tvNombre);
        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvRol = findViewById(R.id.tvRol);
        TextView tvDocumento = findViewById(R.id.tvDocumento);
        TextView tvCargo = findViewById(R.id.tvCargo);
        Button btnLogout = findViewById(R.id.btnLogout);

        tvNombre.setText(session.getNombre());
        tvEmail.setText(session.getEmail());
        tvRol.setText(labelRol(session.getRol()));
        tvDocumento.setText(session.getDocumento().isEmpty() ? "—" : session.getDocumento());
        String cargo = session.getCargo();
        tvCargo.setText(cargo.isEmpty() ? "Sin definir" : cargo);

        btnLogout.setOnClickListener(v -> confirmarLogout());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres cerrar la sesión?")
                .setPositiveButton("Cerrar sesión", (dialog, which) -> {
                    session.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String labelRol(String rol) {
        switch (rol) {
            case "ADMIN": return "Administrador";
            case "JEFE": return "Jefe de Turno";
            case "BOMBERO": return "Bombero PCI-EC";
            case "SOLICITANTE": return "Solicitante";
            default: return rol;
        }
    }
}
