package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.EventoRecorrente
import com.agendafocopei.databinding.ListItemEventoRecorrenteBinding
import java.text.DateFormatSymbols
import java.util.Locale

class EventoRecorrenteAdapter(
    private val onEditClickListener: (EventoRecorrente) -> Unit,
    private val onDeleteClickListener: (EventoRecorrente) -> Unit
) : ListAdapter<EventoRecorrente, EventoRecorrenteAdapter.EventoViewHolder>(EventoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ListItemEventoRecorrenteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventoViewHolder(binding, onEditClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventoViewHolder(
        private val binding: ListItemEventoRecorrenteBinding,
        private val onEdit: (EventoRecorrente) -> Unit,
        private val onDelete: (EventoRecorrente) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private fun getDiaDaSemanaFormatado(diaDaSemana: Int): String {
            val weekdays = DateFormatSymbols(Locale("pt", "BR")).weekdays
            return if (diaDaSemana in 1..7) weekdays[diaDaSemana] else "Inválido"
        }

        fun bind(evento: EventoRecorrente) {
            binding.textViewNomeEvento.text = evento.nomeEvento
            binding.textViewDiaSemanaEvento.text = getDiaDaSemanaFormatado(evento.diaDaSemana)
            binding.textViewIntervaloHorarioEvento.text = "${evento.horaInicio} - ${evento.horaFim}"

            if (evento.salaLocal.isNullOrEmpty()) {
                binding.textViewSalaLocalEvento.visibility = View.GONE
            } else {
                binding.textViewSalaLocalEvento.visibility = View.VISIBLE
                binding.textViewSalaLocalEvento.text = evento.salaLocal
            }

            if (evento.observacoes.isNullOrEmpty()) {
                binding.textViewObservacoesEvento.visibility = View.GONE
            } else {
                binding.textViewObservacoesEvento.visibility = View.VISIBLE
                binding.textViewObservacoesEvento.text = evento.observacoes
            }

            binding.viewCorEvento.setBackgroundColor(evento.cor ?: Color.TRANSPARENT)

            binding.buttonEditarEvento.setOnClickListener { onEdit(evento) }
            binding.buttonDeletarEvento.setOnClickListener { onDelete(evento) }
        }
    }

    class EventoDiffCallback : DiffUtil.ItemCallback<EventoRecorrente>() {
        override fun areItemsTheSame(oldItem: EventoRecorrente, newItem: EventoRecorrente): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EventoRecorrente, newItem: EventoRecorrente): Boolean {
            return oldItem == newItem
        }
    }
}
