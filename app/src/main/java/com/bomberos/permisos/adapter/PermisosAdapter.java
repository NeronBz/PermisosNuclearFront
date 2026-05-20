package com.bomberos.permisos.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bomberos.permisos.R;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class PermisosAdapter extends BaseAdapter {

    public interface AccionListener {
        void onAccion(String accion, JSONObject permiso);
    }

    private final Context contexto;
    private final List<JSONObject> permisos;
    private final String rol;
    private final AccionListener listener;

    public PermisosAdapter(Context contexto, List<JSONObject> permisos,
                           String rol, AccionListener listener) {
        this.contexto = contexto;
        this.permisos = permisos;
        this.rol = rol;
        this.listener = listener;
    }

    @Override
    public int getCount() { return permisos.size(); }

    @Override
    public Object getItem(int pos) { return permisos.get(pos); }

    @Override
    public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(contexto)
                    .inflate(R.layout.item_permiso, parent, false);
        }

        JSONObject permiso = permisos.get(position);

        try {
            String estado = permiso.optString("estado", "");
            String edificio = permiso.optString("edificio", "—");
            String descripcion = permiso.optString("descripcion_trabajo", "");
            String createdAt = permiso.optString("createdAt", "").replace("T", " ").substring(0, Math.min(16, permiso.optString("createdAt", "").length()));

            JSONObject solicitanteObj = permiso.optJSONObject("solicitante");
            String solicitante = solicitanteObj != null ?
                    solicitanteObj.optString("nombre", "—") : "—";

            ((TextView) convertView.findViewById(R.id.tvEdificio)).setText(edificio);

            TextView tvEstado = convertView.findViewById(R.id.tvEstado);
            tvEstado.setText(labelEstado(estado));
            tvEstado.setTextColor(colorEstado(estado));

            String desc = descripcion.isEmpty() ? cargarTipos(permiso) : descripcion;
            ((TextView) convertView.findViewById(R.id.tvDescripcion)).setText(desc);
            ((TextView) convertView.findViewById(R.id.tvSolicitante)).setText("Solicitante: " + solicitante);
            ((TextView) convertView.findViewById(R.id.tvFecha)).setText("Creado: " + createdAt);

            LinearLayout layoutBotones = convertView.findViewById(R.id.layoutBotones);
            layoutBotones.removeAllViews();
            agregarBotones(layoutBotones, permiso, estado);

        } catch (Exception e) {
            // ignorar item con error de parseo
        }

        return convertView;
    }

    private void agregarBotones(LinearLayout container, JSONObject permiso, String estado) {
        List<String> activos = Arrays.asList("PENDIENTE", "EVALUADO", "AUTORIZADO", "EN_EJECUCION");
        List<String> terminales = Arrays.asList("COMPLETADO", "RECHAZADO", "ANULADO");

        switch (rol) {
            case "BOMBERO":
                if ("PENDIENTE".equals(estado))    addBtn(container, "Evaluar",   "EVALUAR",   permiso, false);
                if ("AUTORIZADO".equals(estado))   addBtn(container, "Implantar", "IMPLANTAR", permiso, false);
                if ("EN_EJECUCION".equals(estado)) addBtn(container, "Cerrar",    "CERRAR",    permiso, false);
                break;

            case "JEFE":
                if ("EVALUADO".equals(estado)) {
                    addBtn(container, "Autorizar", "AUTORIZAR", permiso, false);
                    addBtn(container, "Rechazar",  "RECHAZAR",  permiso, true);
                }
                if (activos.contains(estado)) addBtn(container, "Anular", "ANULAR", permiso, true);
                break;

            case "ADMIN":
                if ("PENDIENTE".equals(estado))    addBtn(container, "Evaluar",   "EVALUAR",   permiso, false);
                if ("EVALUADO".equals(estado)) {
                    addBtn(container, "Autorizar", "AUTORIZAR", permiso, false);
                    addBtn(container, "Rechazar",  "RECHAZAR",  permiso, true);
                }
                if ("AUTORIZADO".equals(estado))   addBtn(container, "Implantar", "IMPLANTAR", permiso, false);
                if ("EN_EJECUCION".equals(estado)) addBtn(container, "Cerrar",    "CERRAR",    permiso, false);
                if (activos.contains(estado))      addBtn(container, "Anular",    "ANULAR",    permiso, true);
                if (terminales.contains(estado))   addBtn(container, "Borrar",    "BORRAR",    permiso, true);
                break;

            case "SOLICITANTE":
                if ("PENDIENTE".equals(estado))  addBtn(container, "Anular", "ANULAR", permiso, true);
                if (terminales.contains(estado)) addBtn(container, "Borrar", "BORRAR", permiso, true);
                break;
        }
    }

    private void addBtn(LinearLayout container, String texto, String accion,
                        JSONObject permiso, boolean destructivo) {
        Button btn = new Button(contexto);
        btn.setText(texto);
        btn.setTextSize(12f);
        if (destructivo) btn.setTextColor(Color.parseColor("#E53935"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 8, 0);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> listener.onAccion(accion, permiso));
        container.addView(btn);
    }

    private String labelEstado(String estado) {
        switch (estado) {
            case "PENDIENTE": return "PENDIENTE";
            case "EVALUADO": return "EVALUADO";
            case "AUTORIZADO": return "AUTORIZADO";
            case "RECHAZADO": return "RECHAZADO";
            case "EN_EJECUCION": return "EN EJECUCIÓN";
            case "COMPLETADO": return "COMPLETADO";
            case "ANULADO": return "ANULADO";
            default: return estado;
        }
    }

    private int colorEstado(String estado) {
        switch (estado) {
            case "PENDIENTE": return Color.parseColor("#F57C00");
            case "EVALUADO": return Color.parseColor("#1565C0");
            case "AUTORIZADO": return Color.parseColor("#2E7D32");
            case "RECHAZADO": return Color.parseColor("#C62828");
            case "EN_EJECUCION": return Color.parseColor("#6A1B9A");
            case "COMPLETADO": return Color.parseColor("#546E7A");
            case "ANULADO": return Color.parseColor("#757575");
            default: return Color.BLACK;
        }
    }

    private String cargarTipos(JSONObject permiso) {
        try {
            org.json.JSONArray tipos = permiso.optJSONArray("tipos_trabajo");
            if (tipos == null || tipos.length() == 0) return "—";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tipos.length(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatTipo(tipos.getString(i)));
            }
            return sb.toString();
        } catch (Exception e) { return "—"; }
    }

    private String formatTipo(String tipo) {
        switch (tipo) {
            case "SOLDADURA_ELECTRICA": return "Sold. eléctrica";
            case "SOLDADURA_TIG": return "Sold. TIG";
            case "CORTE_RADIAL": return "Corte radial";
            case "LANZA_TERMICA": return "Lanza térmica";
            case "CORTE_SOPLETE": return "Soplete";
            case "DISTENSIONADO": return "Distensionado";
            case "OTROS": return "Otros";
            default: return tipo;
        }
    }
}
