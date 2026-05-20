package com.bomberos.permisos.utils;

import android.content.Context;

import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiHelper {

    private static final String BASE_URL = "https://pinseles-backend.onrender.com/api/v1/";

    private static ApiHelper instancia;
    private final RequestQueue cola;
    private final SessionManager session;

    private ApiHelper(Context context) {
        cola = Volley.newRequestQueue(context.getApplicationContext());
        session = new SessionManager(context);
    }

    public static synchronized ApiHelper getInstance(Context context) {
        if (instancia == null) {
            instancia = new ApiHelper(context);
        }
        return instancia;
    }

    public void get(String endpoint,
                    Response.Listener<JSONObject> onExito,
                    Response.ErrorListener onError) {

        String url = BASE_URL + endpoint;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                onExito, onError) {
            @Override
            public Map<String, String> getHeaders() {
                return cabecerasAuth();
            }
        };
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                60000, 0, 1.0f));
        cola.add(request);
    }

    public void post(String endpoint,
                     JSONObject cuerpo,
                     Response.Listener<JSONObject> onExito,
                     Response.ErrorListener onError) {

        String url = BASE_URL + endpoint;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, cuerpo,
                onExito, onError) {
            @Override
            public Map<String, String> getHeaders() {
                return cabecerasAuth();
            }
        };
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                60000, 0, 1.0f));
        cola.add(request);
    }

    public void patch(String endpoint,
                      JSONObject cuerpo,
                      Response.Listener<JSONObject> onExito,
                      Response.ErrorListener onError) {

        String url = BASE_URL + endpoint;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PATCH, url, cuerpo,
                onExito, onError) {
            @Override
            public Map<String, String> getHeaders() {
                return cabecerasAuth();
            }
        };
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                60000, 0, 1.0f));
        cola.add(request);
    }

    public void delete(String endpoint,
                       Response.Listener<JSONObject> onExito,
                       Response.ErrorListener onError) {

        String url = BASE_URL + endpoint;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                onExito, onError) {
            @Override
            public Map<String, String> getHeaders() {
                return cabecerasAuth();
            }
        };
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                60000, 0, 1.0f));
        cola.add(request);
    }

    // POST sin autenticacion, se usa solo para el login
    public void postPublico(String endpoint,
                            JSONObject cuerpo,
                            Response.Listener<JSONObject> onExito,
                            Response.ErrorListener onError) {

        String url = BASE_URL + endpoint;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, cuerpo,
                onExito, onError);
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                60000, 0, 1.0f));
        cola.add(request);
    }

    private Map<String, String> cabecerasAuth() {
        Map<String, String> cabeceras = new HashMap<>();
        cabeceras.put("Content-Type", "application/json");
        String token = session.getToken();
        if (token != null && !token.isEmpty()) {
            cabeceras.put("Authorization", "Bearer " + token);
        }
        return cabeceras;
    }

    public static String mensajeError(VolleyError error) {
        if (error.networkResponse != null) {
            int codigo = error.networkResponse.statusCode;
            if (codigo == 401) return "Sesión expirada. Inicia sesión de nuevo.";
            if (codigo == 403) return "Sin permisos para esta acción.";
            if (codigo == 404) return "Recurso no encontrado.";
            if (codigo == 400) {
                try {
                    String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(body);
                    return json.optString("message", "Datos incorrectos.");
                } catch (Exception ignored) {}
            }
            return "Error del servidor (" + codigo + ").";
        }
        if (error instanceof NetworkError) {
            return "El servidor tardó en responder. Comprueba si el cambio se aplicó.";
        }
        return "Sin conexión con el servidor.";
    }
}
