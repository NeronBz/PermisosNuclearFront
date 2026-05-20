package com.bomberos.permisos.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.bomberos.permisos.HomeActivity;
import com.bomberos.permisos.R;
import com.bomberos.permisos.utils.ApiHelper;
import com.bomberos.permisos.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Random;

public class PinselesFirebaseService extends FirebaseMessagingService {

    public static final String CANAL_ID = "pinseles_ptri";
    public static final String CANAL_NOMBRE = "Permisos PTRI";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        registrarToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        RemoteMessage.Notification notif = message.getNotification();
        if (notif == null) return;

        String titulo = notif.getTitle();
        String cuerpo = notif.getBody() != null ? notif.getBody() : "";

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        crearCanalNotificacion();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(new Random().nextInt(), builder.build());
        }
    }

    private void registrarToken(String fcmToken) {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) return;

        try {
            JSONObject cuerpo = new JSONObject();
            cuerpo.put("token", fcmToken);
            ApiHelper.getInstance(this).post("auth/fcm-token", cuerpo, r -> {}, e -> {});
        } catch (Exception ignored) {}
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Notificaciones de cambios en permisos PTRI");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }
}
