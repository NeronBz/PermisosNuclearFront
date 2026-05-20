package com.bomberos.permisos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bomberos.permisos.service.PinselesFirebaseService;
import com.bomberos.permisos.utils.SessionManager;

public class HomeActivity extends AppCompatActivity {

    private SessionManager session;

    private final ActivityResultLauncher<String> permisosNotif =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        crearCanalNotificacion();
        pedirPermisoNotificaciones();

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

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    PinselesFirebaseService.CANAL_ID,
                    PinselesFirebaseService.CANAL_NOMBRE,
                    NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Notificaciones de cambios en permisos PTRI");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
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
