package com.bomberos.permisos.utils;

import android.graphics.PointF;
import android.widget.ImageView;

/**
 * PlanoCoordMapper
 * --------------------------------------------------------------------------
 * Convierte coordenadas del sistema lógico del plano de Almaraz (1200 x 800)
 * a píxeles reales del ImageView que lo contiene.
 *
 * Pensado para usarse desde PlanoActivity: cada zona del JSON declara su
 * centro (x, y) en coordenadas lógicas, y este helper devuelve dónde hay
 * que colocar el botón en pantalla.
 *
 * Uso típico:
 *     mapaContainer.post(() -> {
 *         PlanoCoordMapper mapper = new PlanoCoordMapper(planoImage);
 *         PointF centro = mapper.logicoAPixel(260f, 280f);
 *         boton.setX(centro.x - boton.getWidth()  / 2f);
 *         boton.setY(centro.y - boton.getHeight() / 2f);
 *     });
 *
 * El ImageView del plano DEBE tener scaleType="fitCenter" para que el
 * cálculo sea correcto.
 *
 * Proyecto: PIN-SELES (CNA) — TFG 2º DAM IES Augustóbriga.
 */
public final class PlanoCoordMapper {

    /** Ancho del sistema lógico del plano (viewBox del SVG/VectorDrawable). */
    public static final float ANCHO_LOGICO = 1200f;

    /** Alto del sistema lógico del plano (viewBox del SVG/VectorDrawable). */
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

    /**
     * Recalcula la escala y los offsets según el tamaño actual del ImageView.
     * Llamar tras un cambio de tamaño (rotación, configuración, etc.).
     */
    public void recalcular() {
        int vw = planoView.getWidth();
        int vh = planoView.getHeight();
        if (vw == 0 || vh == 0) {
            preparado = false;
            return;
        }
        // fitCenter: la imagen ocupa lo máximo posible respetando el aspect ratio
        float escalaX = vw / ANCHO_LOGICO;
        float escalaY = vh / ALTO_LOGICO;
        escala = Math.min(escalaX, escalaY);

        // Centrar la imagen escalada dentro del ImageView
        offsetX = (vw - ANCHO_LOGICO * escala) / 2f;
        offsetY = (vh - ALTO_LOGICO  * escala) / 2f;
        preparado = true;
    }

    /** Convierte una coordenada lógica (xL, yL) a píxel del ImageView. */
    public PointF logicoAPixel(float xL, float yL) {
        if (!preparado) recalcular();
        return new PointF(xL * escala + offsetX, yL * escala + offsetY);
    }

    public boolean estaPreparado() {
        return preparado;
    }
}
