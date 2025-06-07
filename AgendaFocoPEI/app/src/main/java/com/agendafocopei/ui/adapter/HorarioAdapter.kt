package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.ListItemHorarioAulaBinding
import com.agendafocopei.ui.model.HorarioAulaDisplay

class HorarioAdapter(
    private val onEditClickListener: (HorarioAulaDisplay) -> Unit,
    private val onDeleteClickListener: (HorarioAulaDisplay) -> Unit
) : ListAdapter<HorarioAulaDisplay, HorarioAdapter.HorarioViewHolder>(HorarioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioViewHolder {
        val binding = ListItemHorarioAulaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HorarioViewHolder(binding, onEditClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: HorarioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HorarioViewHolder(
        private val binding: ListItemHorarioAulaBinding,
        private val onEdit: (HorarioAulaDisplay) -> Unit,
        private val onDelete: (HorarioAulaDisplay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(horario: HorarioAulaDisplay) {
            binding.textViewDiaSemanaHorario.text = horario.getDiaDaSemanaFormatado()
            binding.textViewIntervaloHorario.text = "${horario.horaInicio} - ${horario.horaFim}"
            binding.textViewNomeDisciplinaHorario.text = horario.nomeDisciplina
            binding.textViewNomeTurmaHorario.text = horario.nomeTurma

            if (horario.salaAula.isNullOrEmpty()) {
                binding.textViewSalaAulaHorario.visibility = View.GONE
            } else {
                binding.textViewSalaAulaHorario.visibility = View.VISIBLE
                binding.textViewSalaAulaHorario.text = horario.salaAula
            }

            binding.viewCorDisciplinaHorario.setBackgroundColor(horario.corDisciplina ?: Color.TRANSPARENT)
            binding.viewCorTurmaHorario.setBackgroundColor(horario.corTurma ?: Color.TRANSPARENT)

            binding.buttonEditarHorario.setOnClickListener { onEdit(horario) }
            binding.buttonDeletarHorario.setOnClickListener { onDelete(horario) }
        }
    }

    class HorarioDiffCallback : DiffUtil.ItemCallback<HorarioAulaDisplay>() {
        override fun areItemsTheSame(oldItem: HorarioAulaDisplay, newItem: HorarioAulaDisplay): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HorarioAulaDisplay, newItem: HorarioAulaDisplay): Boolean {
            return oldItem == newItem
        }
    }
}
