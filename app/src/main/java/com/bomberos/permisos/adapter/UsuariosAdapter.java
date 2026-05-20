package com.bomberos.permisos.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.bomberos.permisos.R;

import org.json.JSONObject;

import java.util.List;

public class UsuariosAdapter extends BaseAdapter {

    public interface EditarListener {
        void onEditar(JSONObject usuario);
    }

    private final Context contexto;
    private final List<JSONObject> usuarios;
    private final EditarListener listener;

    public UsuariosAdapter(Context contexto, List<JSONObject> usuarios, EditarListener listener) {
        this.contexto = contexto;
        this.usuarios = usuarios;
        this.listener = listener;
    }

    @Override
    public int getCount() { return usuarios.size(); }

    @Override
    public Object getItem(int pos) { return usuarios.get(pos); }

    @Override
    public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(contexto)
                    .inflate(R.layout.item_usuario, parent, false);
        }

        JSONObject usuario = usuarios.get(position);

        try {
            String nombre = usuario.optString("nombre", "—");
            String email = usuario.optString("email", "—");
            String rol = usuario.optString("rol", "—");
            String cargo = usuario.optString("cargo", "");
            boolean activo = usuario.optBoolean("activo", true);

            String rolAnterior = position > 0
                    ? usuarios.get(position - 1).optString("rol", "") : "";
            TextView tvSeccion = convertView.findViewById(R.id.tvSeccion);
            if (!rol.equals(rolAnterior)) {
                tvSeccion.setText(labelRol(rol));
                tvSeccion.setVisibility(View.VISIBLE);
            } else {
                tvSeccion.setVisibility(View.GONE);
            }

            ((TextView) convertView.findViewById(R.id.tvNombre)).setText(nombre);
            ((TextView) convertView.findViewById(R.id.tvEmail)).setText(email);
            ((TextView) convertView.findViewById(R.id.tvRol)).setText(labelRol(rol));

            TextView tvCargo = convertView.findViewById(R.id.tvCargo);
            tvCargo.setText(cargo.isEmpty() ? "" : "Cargo: " + cargo);
            tvCargo.setVisibility(cargo.isEmpty() ? View.GONE : View.VISIBLE);

            TextView tvActivo = convertView.findViewById(R.id.tvActivo);
            tvActivo.setText(activo ? "Activo" : "Inactivo");
            tvActivo.setTextColor(activo ?
                    android.graphics.Color.parseColor("#2E7D32") :
                    android.graphics.Color.parseColor("#C62828"));

            Button btnEditar = convertView.findViewById(R.id.btnEditar);
            btnEditar.setOnClickListener(v -> listener.onEditar(usuario));

        } catch (Exception ignored) {}

        return convertView;
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
