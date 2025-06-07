package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.ListItemAgendaDashboardBinding
import com.agendafocopei.ui.model.ItemAgendaDashboard

class AgendaHojeAdapter(
    private val onItemClickListener: (ItemAgendaDashboard) -> Unit
) : ListAdapter<ItemAgendaDashboard, AgendaHojeAdapter.AgendaViewHolder>(AgendaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
        val binding = ListItemAgendaDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgendaViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AgendaViewHolder(
        private val binding: ListItemAgendaDashboardBinding,
        private val onItemClick: (ItemAgendaDashboard) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemAgendaDashboard) {
            binding.textViewHorarioItemAgenda.text = "${item.horaInicio} - ${item.horaFim}"
            binding.textViewNomeItemAgenda.text = item.nomePrincipal

            if (item.detalheSecundario.isNullOrEmpty()) {
                binding.textViewDetalheItemAgenda.visibility = View.GONE
            } else {
                binding.textViewDetalheItemAgenda.visibility = View.VISIBLE
                binding.textViewDetalheItemAgenda.text = item.detalheSecundario
            }

            binding.viewCorItemAgenda.setBackgroundColor(item.cor ?: Color.TRANSPARENT)

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class AgendaDiffCallback : DiffUtil.ItemCallback<ItemAgendaDashboard>() {
        override fun areItemsTheSame(oldItem: ItemAgendaDashboard, newItem: ItemAgendaDashboard): Boolean {
            return oldItem.idUnico == newItem.idUnico
        }

        override fun areContentsTheSame(oldItem: ItemAgendaDashboard, newItem: ItemAgendaDashboard): Boolean {
            return oldItem == newItem
        }
    }
}
