package com.agendafocopei.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.R
import com.agendafocopei.data.Tarefa
import com.agendafocopei.databinding.ListItemTarefaBinding
import com.agendafocopei.ui.model.TarefaDisplay
import java.text.SimpleDateFormat
import java.util.*

class TarefaAdapter(
    private val onTarefaCheckChanged: (TarefaDisplay, Boolean) -> Unit,
    private val onEditClickListener: (TarefaDisplay) -> Unit
) : ListAdapter<TarefaDisplay, TarefaAdapter.TarefaViewHolder>(TarefaDiffCallback()) {

    private val displayPrazoFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val storedPrazoDataFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val binding = ListItemTarefaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TarefaViewHolder(binding, onTarefaCheckChanged, onEditClickListener, displayPrazoFormat, storedPrazoDataFormat)
    }

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TarefaViewHolder(
        private val binding: ListItemTarefaBinding,
        private val onCheckChanged: (TarefaDisplay, Boolean) -> Unit,
        private val onEditClick: (TarefaDisplay) -> Unit,
        private val displayPrazoFormatter: SimpleDateFormat,
        private val storedPrazoDataFormatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context = itemView.context

        fun bind(tarefaDisplay: TarefaDisplay) {
            val tarefa = tarefaDisplay.tarefa
            binding.textViewTarefaDescricao.text = tarefa.descricao

            // Checkbox
            binding.checkBoxTarefaConcluida.setOnCheckedChangeListener(null) // Evitar trigger no bind
            binding.checkBoxTarefaConcluida.isChecked = tarefa.concluida
            binding.checkBoxTarefaConcluida.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(tarefaDisplay, isChecked)
            }

            // Strikethrough
            if (tarefa.concluida) {
                binding.textViewTarefaDescricao.paintFlags = binding.textViewTarefaDescricao.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewTarefaDescricao.paintFlags = binding.textViewTarefaDescricao.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Prioridade
            when (tarefa.prioridade) {
                0 -> binding.viewPrioridadeTarefa.setBackgroundColor(ContextCompat.getColor(context, R.color.prioridade_baixa)) // Definir cores em colors.xml
                1 -> binding.viewPrioridadeTarefa.setBackgroundColor(ContextCompat.getColor(context, R.color.prioridade_media))
                2 -> binding.viewPrioridadeTarefa.setBackgroundColor(ContextCompat.getColor(context, R.color.prioridade_alta))
                else -> binding.viewPrioridadeTarefa.setBackgroundColor(Color.TRANSPARENT)
            }

            // Prazo
            if (tarefa.prazoData != null) {
                var prazoStr = ""
                try {
                    val date = storedPrazoDataFormatter.parse(tarefa.prazoData!!)
                    prazoStr = SimpleDateFormat("dd/MM/yy", Locale("pt","BR")).format(date!!)
                } catch (e: Exception) {
                    prazoStr = tarefa.prazoData!! // fallback
                }

                if (tarefa.prazoHora != null) {
                    prazoStr += " ${tarefa.prazoHora}"
                }
                binding.textViewTarefaPrazo.text = "Prazo: $prazoStr"
                binding.textViewTarefaPrazo.visibility = View.VISIBLE
            } else {
                binding.textViewTarefaPrazo.visibility = View.GONE
            }

            // Chip Disciplina
            if (tarefaDisplay.nomeDisciplina != null) {
                binding.chipDisciplinaTarefa.text = tarefaDisplay.nomeDisciplina
                binding.chipDisciplinaTarefa.chipBackgroundColor = ColorStateList.valueOf(tarefaDisplay.corDisciplina ?: Color.LTGRAY)
                binding.chipDisciplinaTarefa.visibility = View.VISIBLE
            } else {
                binding.chipDisciplinaTarefa.visibility = View.GONE
            }

            // Chip Turma
            if (tarefaDisplay.nomeTurma != null) {
                binding.chipTurmaTarefa.text = tarefaDisplay.nomeTurma
                binding.chipTurmaTarefa.chipBackgroundColor = ColorStateList.valueOf(tarefaDisplay.corTurma ?: Color.LTGRAY)
                binding.chipTurmaTarefa.visibility = View.VISIBLE
            } else {
                binding.chipTurmaTarefa.visibility = View.GONE
            }

            binding.chipGroupAssociacoesTarefa.visibility =
                if(binding.chipDisciplinaTarefa.visibility == View.VISIBLE || binding.chipTurmaTarefa.visibility == View.VISIBLE) View.VISIBLE else View.GONE


            binding.buttonEditarTarefaItem.setOnClickListener { onEditClick(tarefaDisplay) }
        }
    }

    class TarefaDiffCallback : DiffUtil.ItemCallback<TarefaDisplay>() {
        override fun areItemsTheSame(oldItem: TarefaDisplay, newItem: TarefaDisplay): Boolean {
            return oldItem.tarefa.id == newItem.tarefa.id
        }

        override fun areContentsTheSame(oldItem: TarefaDisplay, newItem: TarefaDisplay): Boolean {
            return oldItem == newItem
        }
    }
}
