package com.bomberos.permisos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.utils.SessionManager;

public class HomeActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);

        String nombre = session.getNombre();
        String rol = session.getRol();

        TextView tvBienvenida = findViewById(R.id.tvBienvenida);
        tvBienvenida.setText("Bienvenido, " + nombre);

        TextView tvRol = findViewById(R.id.tvRol);
        tvRol.setText(labelRol(rol));

        Button btnPermisos = findViewById(R.id.btnPermisos);
        Button btnFichaje = findViewById(R.id.btnFichaje);
        Button btnPerfil = findViewById(R.id.btnPerfil);
        Button btnUsuarios = findViewById(R.id.btnUsuarios);

        btnPermisos.setOnClickListener(v ->
                startActivity(new Intent(this, PermisosActivity.class)));

        btnFichaje.setOnClickListener(v ->
                startActivity(new Intent(this, FichajeActivity.class)));

        btnPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class)));

        if ("JEFE".equals(rol) || "ADMIN".equals(rol)) {
            btnUsuarios.setVisibility(View.VISIBLE);
            btnUsuarios.setOnClickListener(v ->
                    startActivity(new Intent(this, UsuariosActivity.class)));
        } else {
            btnUsuarios.setVisibility(View.GONE);
        }
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
