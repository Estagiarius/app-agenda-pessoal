package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.Evento
import com.agendafocopei.databinding.ListItemEventoBinding // ATUALIZADO para novo nome de binding
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

// Classe e construtor atualizados para Evento
class EventoAdapter(
    private val onEditClickListener: (Evento) -> Unit,
    private val onDeleteClickListener: (Evento) -> Unit
) : ListAdapter<Evento, EventoAdapter.EventoViewHolder>(EventoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ListItemEventoBinding.inflate(LayoutInflater.from(parent.context), parent, false) // ATUALIZADO
        return EventoViewHolder(binding, onEditClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder atualizado para Evento
    class EventoViewHolder(
        private val binding: ListItemEventoBinding, // ATUALIZADO
        private val onEdit: (Evento) -> Unit,
        private val onDelete: (Evento) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private fun getDiaDaSemanaFormatado(diaDaSemana: Int, dataEspecifica: String?): String {
            if (dataEspecifica != null) {
                try {
                    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = sdfInput.parse(dataEspecifica)
                    date?.let {
                        val sdfOutput = SimpleDateFormat("dd/MM/yyyy (EEE)", Locale("pt", "BR"))
                        return sdfOutput.format(it).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt","BR")) else it.toString() }
                            .split(" ").joinToString(" ") { word -> word.replaceFirstChar { char -> char.titlecase(Locale("pt", "BR")) } } // Capitaliza dia da semana também
                    }
                } catch (e: Exception) {
                    // Fallback para o dia da semana se o parse falhar
                }
            }
            val weekdays = DateFormatSymbols(Locale("pt", "BR")).weekdays
            return if (diaDaSemana in 1..7) weekdays[diaDaSemana] else "Recorrente" // "Recorrente" como fallback
        }


        fun bind(evento: Evento) { // Atualizado para Evento
            binding.textViewNomeEvento.text = evento.nomeEvento
            binding.textViewDiaSemanaEvento.text = getDiaDaSemanaFormatado(evento.diaDaSemana, evento.dataEspecifica)
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

    // DiffCallback atualizado para Evento
    class EventoDiffCallback : DiffUtil.ItemCallback<Evento>() {
        override fun areItemsTheSame(oldItem: Evento, newItem: Evento): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Evento, newItem: Evento): Boolean {
            return oldItem == newItem
        }
    }
}
