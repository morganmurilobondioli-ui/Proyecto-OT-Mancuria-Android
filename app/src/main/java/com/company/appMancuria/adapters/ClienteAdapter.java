package com.company.appMancuria.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.appMancuria.R;
import com.company.appMancuria.models.Cliente;

import java.util.List;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ViewHolder> {

    public interface OnItemClickListener { void onItemClick(Cliente cliente); }

    private final List<Cliente> lista;
    private final OnItemClickListener listener;

    public ClienteAdapter(List<Cliente> lista, OnItemClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Cliente c = lista.get(pos);
        h.tvNombre.setText(c.getNombre());
        h.tvTipo.setText(c.getTipo());
        h.tvTelefono.setText(c.getTelefono().isEmpty() ? "Sin teléfono" : c.getTelefono());
        h.tvVehiculos.setText(c.getCantidadVehiculos() + " vehículo(s)");

        // Ícono persona vs empresa
        h.ivTipo.setImageResource("Empresa".equals(c.getTipo())
                ? android.R.drawable.ic_menu_manage
                : R.drawable.ic_person);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(c);
        });
    }

    @Override public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTipo;
        TextView  tvNombre, tvTipo, tvTelefono, tvVehiculos;
        ViewHolder(@NonNull View v) {
            super(v);
            ivTipo      = v.findViewById(R.id.ivTipoCliente);
            tvNombre    = v.findViewById(R.id.tvNombreCliente);
            tvTipo      = v.findViewById(R.id.tvTipoBadge);
            tvTelefono  = v.findViewById(R.id.tvTelefonoCliente);
            tvVehiculos = v.findViewById(R.id.tvVehiculosCliente);
        }
    }
}
