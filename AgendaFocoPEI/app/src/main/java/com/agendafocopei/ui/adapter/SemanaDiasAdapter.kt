package com.agendafocopei.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.ListItemDiaDaSemanaBinding
import com.agendafocopei.ui.agenda.DiaDaSemanaComEventos
import com.agendafocopei.ui.agenda.EventoSemanaCompacto // Import para o tipo do callback

class SemanaDiasAdapter(
    private val onEventoClickListener: (EventoSemanaCompacto) -> Unit // Callback
) : ListAdapter<DiaDaSemanaComEventos, SemanaDiasAdapter.DiaSemanaViewHolder>(DiaSemanaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaSemanaViewHolder {
        val binding = ListItemDiaDaSemanaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaSemanaViewHolder(binding, onEventoClickListener) // Passar o callback
    }

    override fun onBindViewHolder(holder: DiaSemanaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiaSemanaViewHolder(
        private val binding: ListItemDiaDaSemanaBinding,
        private val onEventoClick: (EventoSemanaCompacto) -> Unit // Callback
        ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(diaComEventos: DiaDaSemanaComEventos) {
            binding.textViewNomeDiaSemana.text = diaComEventos.nomeDiaAbrev
            binding.textViewDataDiaSemana.text = diaComEventos.dataFormatadaCurta

            if (diaComEventos.eventos.isEmpty()) {
                binding.textViewNenhumEventoNoDiaSemana.visibility = View.VISIBLE
                binding.recyclerViewEventosDoDiaSemana.visibility = View.GONE
            } else {
                binding.textViewNenhumEventoNoDiaSemana.visibility = View.GONE
                binding.recyclerViewEventosDoDiaSemana.visibility = View.VISIBLE

                // Configurar o adapter aninhado, passando o callback
                val eventosAdapter = EventosDiaSemanaAdapter(onEventoClick)
                binding.recyclerViewEventosDoDiaSemana.apply {
                    layoutManager = LinearLayoutManager(itemView.context)
                    adapter = eventosAdapter
                    // Opcional: desabilitar nested scrolling se causar problemas com a rolagem horizontal principal
                    // isNestedScrollingEnabled = false
                }
                eventosAdapter.submitList(diaComEventos.eventos)
            }
        }
    }

    class DiaSemanaDiffCallback : DiffUtil.ItemCallback<DiaDaSemanaComEventos>() {
        override fun areItemsTheSame(oldItem: DiaDaSemanaComEventos, newItem: DiaDaSemanaComEventos): Boolean {
            // Data é um bom candidato para identidade única aqui, se não houver IDs estáveis
            return oldItem.dataFormatadaCurta == newItem.dataFormatadaCurta && oldItem.nomeDiaAbrev == newItem.nomeDiaAbrev
        }

        override fun areContentsTheSame(oldItem: DiaDaSemanaComEventos, newItem: DiaDaSemanaComEventos): Boolean {
            return oldItem == newItem
        }
    }
}
