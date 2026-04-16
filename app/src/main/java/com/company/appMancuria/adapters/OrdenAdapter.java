package com.company.appMancuria.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.appMancuria.R;
import com.company.appMancuria.models.OrdenTrabajo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrdenAdapter extends RecyclerView.Adapter<OrdenAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(OrdenTrabajo orden);
    }

    private List<OrdenTrabajo> lista;
    private final OnItemClickListener listener;
    private int ultimaPos = -1;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());

    public OrdenAdapter(List<OrdenTrabajo> lista, OnItemClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orden, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        OrdenTrabajo o = lista.get(pos);

        h.tvPlaca.setText(o.getPlaca().isEmpty() ? "Sin placa" : o.getPlaca());
        h.tvCliente.setText(o.getClienteNombre().isEmpty() ? "Sin cliente" : o.getClienteNombre());
        h.tvModelo.setText(o.getMarcaModelo().isEmpty() ? "" : o.getMarcaModelo());
        h.tvModelo.setVisibility(o.getMarcaModelo().isEmpty() ? View.GONE : View.VISIBLE);

        // Falla reportada
        if (o.getFallaReportada() != null && !o.getFallaReportada().isEmpty()) {
            h.tvFalla.setText(o.getFallaReportada());
            h.layoutFalla.setVisibility(View.VISIBLE);
        } else {
            h.layoutFalla.setVisibility(View.GONE);
        }

        // Fecha
        h.tvFecha.setText(o.getFechaIngreso() > 0
                ? SDF.format(new Date(o.getFechaIngreso())) : "—");

        // Kilometraje
        if (o.getKilometraje() > 0) {
            h.tvKm.setText(String.format(Locale.getDefault(), "%,d km", o.getKilometraje()));
            h.tvKm.setVisibility(View.VISIBLE);
        } else {
            h.tvKm.setVisibility(View.GONE);
        }

        // Monto
        h.tvMonto.setText(String.format(Locale.getDefault(), "S/ %.2f", o.getMontoTotal()));

        // Badge de estado con color
        String estado = o.getEstado();
        h.tvEstado.setText(estado);
        int color;
        switch (estado) {
            case "En Proceso": color = Color.parseColor("#F57C00"); break;
            case "Listo":      color = Color.parseColor("#2E7D32"); break;
            case "Entregado":  color = Color.parseColor("#757575"); break;
            default:           color = Color.parseColor("#E53935"); break;
        }
        h.tvEstado.setBackgroundTintList(ColorStateList.valueOf(color));

        // Click para ver detalle
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(o);
        });

        // Animación de entrada escalonada (solo una vez por ítem)
        if (pos > ultimaPos) {
            Context ctx = h.itemView.getContext();
            h.itemView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.item_anim));
            ultimaPos = pos;
        }
    }

    @Override public int getItemCount() { return lista.size(); }

    public void filtrar(List<OrdenTrabajo> filtrada) {
        this.lista = filtrada;
        ultimaPos  = -1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaca, tvCliente, tvModelo, tvEstado, tvFecha, tvKm, tvMonto, tvFalla;
        View layoutFalla;
        ViewHolder(@NonNull View v) {
            super(v);
            tvPlaca   = v.findViewById(R.id.tvPlacaItem);
            tvCliente = v.findViewById(R.id.tvClienteItem);
            tvModelo  = v.findViewById(R.id.tvModeloItem);
            tvEstado  = v.findViewById(R.id.tvEstadoItem);
            tvFecha   = v.findViewById(R.id.tvFechaItem);
            tvKm      = v.findViewById(R.id.tvKilometrajeItem);
            tvMonto   = v.findViewById(R.id.tvMontoItem);
            tvFalla   = v.findViewById(R.id.tvFallaItem);
            layoutFalla = v.findViewById(R.id.layoutFallaItem);
        }
    }
}
