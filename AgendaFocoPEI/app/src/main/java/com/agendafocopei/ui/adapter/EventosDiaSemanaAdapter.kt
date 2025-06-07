package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.ListItemEventoSemanaCompactoBinding
import com.agendafocopei.ui.agenda.EventoSemanaCompacto

class EventosDiaSemanaAdapter(
    private val onItemClickListener: (EventoSemanaCompacto) -> Unit
) : ListAdapter<EventoSemanaCompacto, EventosDiaSemanaAdapter.EventoCompactoViewHolder>(EventoCompactoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoCompactoViewHolder {
        val binding = ListItemEventoSemanaCompactoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventoCompactoViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: EventoCompactoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventoCompactoViewHolder(
        private val binding: ListItemEventoSemanaCompactoBinding,
        private val onItemClick: (EventoSemanaCompacto) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(evento: EventoSemanaCompacto) {
            binding.textViewHorarioEventoSemana.text = evento.horario
            binding.textViewNomeEventoSemana.text = evento.nome
            binding.viewCorEventoSemana.setBackgroundColor(evento.cor ?: Color.TRANSPARENT)

            itemView.setOnClickListener {
                onItemClick(evento)
            }
        }
    }

    class EventoCompactoDiffCallback : DiffUtil.ItemCallback<EventoSemanaCompacto>() {
        override fun areItemsTheSame(oldItem: EventoSemanaCompacto, newItem: EventoSemanaCompacto): Boolean {
            return oldItem.idUnico == newItem.idUnico
        }

        override fun areContentsTheSame(oldItem: EventoSemanaCompacto, newItem: EventoSemanaCompacto): Boolean {
            return oldItem == newItem
        }
    }
}
