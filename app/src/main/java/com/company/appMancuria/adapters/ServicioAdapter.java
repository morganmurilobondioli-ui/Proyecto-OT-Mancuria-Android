package com.company.appMancuria.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.company.appMancuria.R;
import com.company.appMancuria.models.Servicio;
import java.util.ArrayList;
import java.util.List;

public class ServicioAdapter extends RecyclerView.Adapter<ServicioAdapter.ServicioViewHolder> {

    private List<Servicio> listaOriginal;
    private List<Servicio> listaFiltrada;
    private final OnServicioClickListener listener;

    public interface OnServicioClickListener {
        void onServicioClick(Servicio servicio, View view);
    }

    public ServicioAdapter(List<Servicio> lista, OnServicioClickListener listener) {
        this.listaOriginal = lista;
        this.listaFiltrada = new ArrayList<>(lista);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño de tarjeta blanca personalizado
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_servicio, parent, false);
        return new ServicioViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ServicioViewHolder holder, int position) {
        Servicio s = listaFiltrada.get(position);
        holder.tvNombre.setText(s.getNombre());
        holder.itemView.setOnClickListener(v -> listener.onServicioClick(s, v));
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    public void filtrar(String texto) {
        listaFiltrada.clear();
        if (texto.isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            String q = texto.toLowerCase().trim();
            for (Servicio s : listaOriginal) {
                if (s.getNombre().toLowerCase().contains(q)) {
                    listaFiltrada.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void actualizarLista(List<Servicio> nuevaLista) {
        this.listaOriginal = new ArrayList<>(nuevaLista);
        filtrar(""); // Resetear filtro y notificar cambios
    }

    static class ServicioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        ServicioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreServicio);
        }
    }
}
