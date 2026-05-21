package com.bomberos.permisos;

import android.graphics.PointF;
import android.widget.ImageView;
public final class PlanoCoordMapper {

    public static final float ANCHO_LOGICO = 1200f;

    public static final float ALTO_LOGICO  = 800f;

    private final ImageView planoView;

    private float escala;
    private float offsetX;
    private float offsetY;
    private boolean preparado;

    public PlanoCoordMapper(ImageView planoView) {
        this.planoView = planoView;
        recalcular();
    }
    public void recalcular() {
        int vw = planoView.getWidth();
        int vh = planoView.getHeight();
        if (vw == 0 || vh == 0) {
            preparado = false;
            return;
        }float escalaX = vw / ANCHO_LOGICO;
        float escalaY = vh / ALTO_LOGICO;
        escala = Math.min(escalaX, escalaY);
        offsetX = (vw - ANCHO_LOGICO * escala) / 2f;
        offsetY = (vh - ALTO_LOGICO  * escala) / 2f;
        preparado = true;
    }
    public PointF logicoAPixel(float xL, float yL) {
        if (!preparado) recalcular();
        return new PointF(xL * escala + offsetX, yL * escala + offsetY);
    }

    public boolean estaPreparado() {
        return preparado;
    }
}
