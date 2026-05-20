package com.bomberos.permisos.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "pinseles_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROL = "rol";
    private static final String KEY_DOCUMENTO = "documento";
    private static final String KEY_CARGO = "cargo";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void guardarToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void guardarUsuario(String id, String nombre, String email,
                               String rol, String documento, String cargo) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, id);
        editor.putString(KEY_NOMBRE, nombre);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROL, rol);
        editor.putString(KEY_DOCUMENTO, documento != null ? documento : "");
        editor.putString(KEY_CARGO, cargo != null ? cargo : "");
        editor.apply();
    }

    public String getUserId() { return prefs.getString(KEY_USER_ID, ""); }
    public String getNombre() { return prefs.getString(KEY_NOMBRE, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getRol() { return prefs.getString(KEY_ROL, ""); }
    public String getDocumento() { return prefs.getString(KEY_DOCUMENTO, ""); }
    public String getCargo() { return prefs.getString(KEY_CARGO, ""); }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
