package com.agendafocopei.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.TemplatePlanoAula
import com.agendafocopei.databinding.ListItemTemplatePlanoAulaBinding

class TemplatePlanoAulaAdapter(
    private val onEditClickListener: (TemplatePlanoAula) -> Unit,
    private val onDeleteClickListener: (TemplatePlanoAula) -> Unit
) : ListAdapter<TemplatePlanoAula, TemplatePlanoAulaAdapter.TemplateViewHolder>(TemplateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ListItemTemplatePlanoAulaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TemplateViewHolder(binding, onEditClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TemplateViewHolder(
        private val binding: ListItemTemplatePlanoAulaBinding,
        private val onEdit: (TemplatePlanoAula) -> Unit,
        private val onDelete: (TemplatePlanoAula) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(template: TemplatePlanoAula) {
            binding.textViewNomeTemplateItem.text = template.nomeTemplate
            binding.buttonEditarTemplate.setOnClickListener { onEdit(template) }
            binding.buttonDeletarTemplate.setOnClickListener { onDelete(template) }
            // Adicionar um listener ao itemView se quiser que o clique no item faça algo (ex: preview)
            // itemView.setOnClickListener { /* ... */ }
        }
    }

    class TemplateDiffCallback : DiffUtil.ItemCallback<TemplatePlanoAula>() {
        override fun areItemsTheSame(oldItem: TemplatePlanoAula, newItem: TemplatePlanoAula): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TemplatePlanoAula, newItem: TemplatePlanoAula): Boolean {
            return oldItem == newItem
        }
    }
}
