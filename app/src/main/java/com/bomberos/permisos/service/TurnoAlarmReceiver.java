package com.bomberos.permisos.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class TurnoAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long horas = intent.getLongExtra("horas", 8);
        String mensaje = "Llevas más de " + horas + " horas de turno. Recuerda registrar la salida.";

        crearCanal(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                PinselesFirebaseService.CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⏰ Turno superado")
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(9999, builder.build());
    }

    private void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    PinselesFirebaseService.CANAL_ID,
                    PinselesFirebaseService.CANAL_NOMBRE,
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }
}
