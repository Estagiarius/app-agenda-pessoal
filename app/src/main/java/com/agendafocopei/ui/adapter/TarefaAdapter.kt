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
import com.agendafocopei.data.Tarefa // Import da entidade Tarefa, não TarefaDisplay diretamente aqui
import com.agendafocopei.databinding.ListItemTarefaBinding
import com.agendafocopei.ui.model.TarefaDisplay // Import do modelo de display
import java.text.SimpleDateFormat
import java.util.*

class TarefaAdapter(
    private val onTarefaCheckChanged: (TarefaDisplay, Boolean) -> Unit,
    private val onEditClickListener: (TarefaDisplay) -> Unit
    // Adicionar onItemClickListener se necessário no futuro
) : ListAdapter<TarefaDisplay, TarefaAdapter.TarefaViewHolder>(TarefaDiffCallback()) {

    // Formatos de data movidos para dentro do ViewHolder ou para um local mais acessível se necessário fora dele.
    // Para este adapter, eles são usados apenas no ViewHolder.

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val binding = ListItemTarefaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TarefaViewHolder(binding, onTarefaCheckChanged, onEditClickListener)
    }

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TarefaViewHolder(
        private val binding: ListItemTarefaBinding,
        private val onCheckChanged: (TarefaDisplay, Boolean) -> Unit,
        private val onEditClick: (TarefaDisplay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context = itemView.context
        private val displayPrazoFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR"))
        private val storedPrazoDataFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(tarefaDisplay: TarefaDisplay) {
            val tarefa = tarefaDisplay.tarefa
            binding.textViewTarefaDescricao.text = tarefa.descricao

            binding.checkBoxTarefaConcluida.setOnCheckedChangeListener(null)
            binding.checkBoxTarefaConcluida.isChecked = tarefa.concluida
            binding.checkBoxTarefaConcluida.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(tarefaDisplay, isChecked)
            }

            if (tarefa.concluida) {
                binding.textViewTarefaDescricao.paintFlags = binding.textViewTarefaDescricao.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewTarefaDescricao.paintFlags = binding.textViewTarefaDescricao.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            when (tarefa.prioridade) {
                0 -> binding.viewPrioridadeTarefa.setBackgroundColor(ContextCompat.getColor(context, R.color.prioridade_baixa))
                1 -> binding.viewPrioridadeTarefa.setBackgroundColor(ContextCompat.getColor(context, R.color.prioridade_media))
                2 -> binding.viewPrioridadeTarefa.setBackgroundColor(ContextCompat.getColor(context, R.color.prioridade_alta))
                else -> binding.viewPrioridadeTarefa.setBackgroundColor(Color.TRANSPARENT)
            }

            if (tarefa.prazoData != null) {
                var prazoStr = ""
                try {
                    val date = storedPrazoDataFormat.parse(tarefa.prazoData!!)
                    prazoStr = SimpleDateFormat("dd/MM/yy", Locale("pt","BR")).format(date!!)
                } catch (e: Exception) {
                    prazoStr = tarefa.prazoData!!
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
                val corChipDisciplina = tarefaDisplay.corDisciplina ?: ContextCompat.getColor(context, R.color.cor_padrao_cinza_claro)
                binding.chipDisciplinaTarefa.chipBackgroundColor = ColorStateList.valueOf(corChipDisciplina)
                // Verificar contraste do texto do chip
                binding.chipDisciplinaTarefa.setTextColor(getTextColorForBackground(corChipDisciplina))
                binding.chipDisciplinaTarefa.visibility = View.VISIBLE
            } else {
                binding.chipDisciplinaTarefa.visibility = View.GONE
            }

            // Chip Turma
            if (tarefaDisplay.nomeTurma != null) {
                binding.chipTurmaTarefa.text = tarefaDisplay.nomeTurma
                val corChipTurma = tarefaDisplay.corTurma ?: ContextCompat.getColor(context, R.color.cor_padrao_cinza_claro)
                binding.chipTurmaTarefa.chipBackgroundColor = ColorStateList.valueOf(corChipTurma)
                binding.chipTurmaTarefa.setTextColor(getTextColorForBackground(corChipTurma))
                binding.chipTurmaTarefa.visibility = View.VISIBLE
            } else {
                binding.chipTurmaTarefa.visibility = View.GONE
            }

            binding.chipGroupAssociacoesTarefa.visibility =
                if(binding.chipDisciplinaTarefa.visibility == View.VISIBLE || binding.chipTurmaTarefa.visibility == View.VISIBLE) View.VISIBLE else View.GONE

            binding.buttonEditarTarefaItem.setOnClickListener { onEditClick(tarefaDisplay) }
        }

        // Helper para determinar a cor do texto com base na luminância do fundo
        private fun getTextColorForBackground(backgroundColor: Int): Int {
            return if (androidx.core.graphics.ColorUtils.calculateLuminance(backgroundColor) > 0.6) { // Limiar pode ser ajustado
                Color.BLACK // Fundo claro, texto escuro
            } else {
                Color.WHITE // Fundo escuro, texto claro
            }
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
