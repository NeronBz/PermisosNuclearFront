package com.bomberos.permisos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bomberos.permisos.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);

        Intent destino;
        if (session.isLoggedIn()) {
            destino = new Intent(this, HomeActivity.class);
        } else {
            destino = new Intent(this, LoginActivity.class);
        }

        startActivity(destino);
        finish();
    }
}
