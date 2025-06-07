package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.GridItemColorBinding

class ColorGridAdapter(
    private val colors: List<Int>,
    private val preselectedColor: Int? // Para destacar a cor já selecionada, se houver
) : RecyclerView.Adapter<ColorGridAdapter.ColorViewHolder>() {

    var onColorSelected: ((Int) -> Unit)? = null

    inner class ColorViewHolder(val binding: GridItemColorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(color: Int) {
            binding.viewColorChip.setBackgroundColor(color)
            // TODO: Adicionar lógica para destacar visualmente a 'preselectedColor'
            // Por exemplo, mudar a borda, ou mostrar um ícone de marca de seleção.
            // if (color == preselectedColor) {
            //    // Adicionar destaque
            // }

            itemView.setOnClickListener {
                onColorSelected?.invoke(color)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = GridItemColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size
}
