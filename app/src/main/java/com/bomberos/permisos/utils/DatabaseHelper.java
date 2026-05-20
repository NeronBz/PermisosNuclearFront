package com.bomberos.permisos.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "pinseles.db";
    private static final int VERSION_BD = 2;

    public static final String TABLA_FICHAJE = "fichaje_activo";
    public static final String COL_ID = "id";
    public static final String COL_ID_SERVIDOR = "id_servidor";
    public static final String COL_HORA_ENT = "hora_entrada";
    public static final String COL_UBICACION = "ubicacion";

    public static final String TABLA_CACHE = "permisos_cache";
    public static final String COL_JSON = "json";

    private static DatabaseHelper instancia;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instancia == null) {
            instancia = new DatabaseHelper(context.getApplicationContext());
        }
        return instancia;
    }

    private DatabaseHelper(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLA_FICHAJE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ID_SERVIDOR + " TEXT, "
                + COL_HORA_ENT + " TEXT, "
                + COL_UBICACION + " TEXT"
                + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLA_CACHE + " ("
                + COL_ID + " INTEGER PRIMARY KEY, "
                + COL_JSON + " TEXT"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLA_CACHE + " ("
                    + COL_ID + " INTEGER PRIMARY KEY, "
                    + COL_JSON + " TEXT"
                    + ")");
        }
    }

    public void guardarFichajeActivo(String idServidor, String horaEntrada, String ubicacion) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLA_FICHAJE, null, null);
        ContentValues valores = new ContentValues();
        valores.put(COL_ID_SERVIDOR, idServidor);
        valores.put(COL_HORA_ENT, horaEntrada);
        valores.put(COL_UBICACION, ubicacion);
        db.insert(TABLA_FICHAJE, null, valores);
        db.close();
    }

    public String[] getFichajeActivo() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLA_FICHAJE, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String idServidor = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_SERVIDOR));
            String horaEntrada = cursor.getString(cursor.getColumnIndexOrThrow(COL_HORA_ENT));
            String ubicacion = cursor.getString(cursor.getColumnIndexOrThrow(COL_UBICACION));
            cursor.close();
            db.close();
            return new String[]{ idServidor, horaEntrada, ubicacion };
        }
        if (cursor != null) cursor.close();
        db.close();
        return null;
    }

    public void borrarFichajeActivo() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLA_FICHAJE, null, null);
        db.close();
    }

    public void guardarCachePermisos(String json) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLA_CACHE, null, null);
        ContentValues valores = new ContentValues();
        valores.put(COL_ID, 1);
        valores.put(COL_JSON, json);
        db.insert(TABLA_CACHE, null, valores);
        db.close();
    }

    public String getCachePermisos() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLA_CACHE, new String[]{COL_JSON},
                null, null, null, null, null);
        String resultado = null;
        if (cursor != null && cursor.moveToFirst()) {
            resultado = cursor.getString(0);
            cursor.close();
        }
        db.close();
        return resultado;
    }
}
