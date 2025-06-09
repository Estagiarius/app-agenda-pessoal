package com.agendafocopei.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.Subtarefa
import com.agendafocopei.databinding.ListItemSubtarefaBinding

class SubtarefaAdapter(
    private val onItemCheckedChange: (Subtarefa, Boolean) -> Unit,
    private val onItemDescriptionChange: (Subtarefa, String) -> Unit,
    private val onDeleteClickListener: (Subtarefa) -> Unit
) : ListAdapter<Subtarefa, SubtarefaAdapter.SubtarefaViewHolder>(SubtarefaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtarefaViewHolder {
        val binding = ListItemSubtarefaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubtarefaViewHolder(binding, onItemCheckedChange, onItemDescriptionChange, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: SubtarefaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Limpa os listeners para evitar que o TextWatcher antigo atue em um ViewHolder reutilizado
    override fun onViewRecycled(holder: SubtarefaViewHolder) {
        super.onViewRecycled(holder)
        holder.clearListeners()
    }


    class SubtarefaViewHolder(
        private val binding: ListItemSubtarefaBinding,
        private val onCheckChanged: (Subtarefa, Boolean) -> Unit,
        private val onDescriptionChange: (Subtarefa, String) -> Unit,
        private val onDeleteClick: (Subtarefa) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentSubtarefa: Subtarefa? = null
        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSubtarefa?.let {
                    // Evita loop infinito e atualiza apenas se o texto realmente mudou
                    if (it.descricaoSubtarefa != s.toString()) {
                        onDescriptionChange(it, s.toString())
                    }
                }
            }
        }

        fun bind(subtarefa: Subtarefa) {
            currentSubtarefa = subtarefa
            binding.editTextDescricaoSubtarefaItem.setText(subtarefa.descricaoSubtarefa)
            binding.checkBoxSubtarefaConcluida.isChecked = subtarefa.concluida

            // É crucial remover o listener antigo antes de adicionar um novo ou setar o estado do checkbox
            binding.checkBoxSubtarefaConcluida.setOnCheckedChangeListener(null)
            binding.checkBoxSubtarefaConcluida.isChecked = subtarefa.concluida
            binding.checkBoxSubtarefaConcluida.setOnCheckedChangeListener { _, isChecked ->
                currentSubtarefa?.let { onCheckChanged(it, isChecked) }
            }

            binding.editTextDescricaoSubtarefaItem.removeTextChangedListener(textWatcher) // Remove antes para evitar múltiplos listeners
            binding.editTextDescricaoSubtarefaItem.setText(subtarefa.descricaoSubtarefa)
            binding.editTextDescricaoSubtarefaItem.addTextChangedListener(textWatcher)


            binding.buttonDeletarSubtarefa.setOnClickListener {
                currentSubtarefa?.let { onDeleteClick(it) }
            }
        }

        fun clearListeners() {
            binding.editTextDescricaoSubtarefaItem.removeTextChangedListener(textWatcher)
            binding.checkBoxSubtarefaConcluida.setOnCheckedChangeListener(null)
        }
    }

    class SubtarefaDiffCallback : DiffUtil.ItemCallback<Subtarefa>() {
        override fun areItemsTheSame(oldItem: Subtarefa, newItem: Subtarefa): Boolean {
            return oldItem.id == newItem.id && oldItem.tarefaId == newItem.tarefaId // Considerar tarefaId se id for 0 para novos
        }

        override fun areContentsTheSame(oldItem: Subtarefa, newItem: Subtarefa): Boolean {
            return oldItem == newItem
        }
    }
}
