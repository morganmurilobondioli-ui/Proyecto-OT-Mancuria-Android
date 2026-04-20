package com.company.appMancuria.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.company.appMancuria.R;
import com.company.appMancuria.models.Usuario;

import java.util.List;

public class TrabajadorAdapter extends RecyclerView.Adapter<TrabajadorAdapter.VH> {

    private List<Usuario> lista;
    private OnTrabajadorClickListener listener;

    public interface OnTrabajadorClickListener {
        void onOpcionesClick(Usuario u, View view);
    }

    public TrabajadorAdapter(List<Usuario> lista, OnTrabajadorClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trabajador, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Usuario u = lista.get(position);
        holder.tvNombre.setText(u.getNombre());
        holder.tvRol.setText(u.getRol().toUpperCase());
        holder.tvEstado.setText(u.getEstado().toUpperCase());
        
        if ("suspendido".equals(u.getEstado())) {
            holder.tvEstado.setTextColor(Color.RED);
        } else {
            holder.tvEstado.setTextColor(Color.parseColor("#2E7D32")); // Verde oscuro
        }

        Glide.with(holder.itemView.getContext())
                .load(u.getFotoUrl())
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher_round)
                .into(holder.ivFoto);

        holder.btnOpciones.setOnClickListener(v -> listener.onOpcionesClick(u, v));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNombre, tvRol, tvEstado;
        ImageButton btnOpciones;

        public VH(@NonNull View v) {
            super(v);
            ivFoto = v.findViewById(R.id.ivTrabajador);
            tvNombre = v.findViewById(R.id.tvNombreTrabajador);
            tvRol = v.findViewById(R.id.tvRolTrabajador);
            tvEstado = v.findViewById(R.id.tvEstadoTrabajador);
            btnOpciones = v.findViewById(R.id.btnOpcionesTrabajador);
        }
    }
}
