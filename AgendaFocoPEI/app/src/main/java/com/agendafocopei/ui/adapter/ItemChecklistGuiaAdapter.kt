package com.agendafocopei.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.ItemChecklistGuia
import com.agendafocopei.databinding.ListItemChecklistGuiaBinding

class ItemChecklistGuiaAdapter(
    private val onItemCheckChanged: (ItemChecklistGuia, Boolean) -> Unit,
    private val onDeleteItemClickListener: (ItemChecklistGuia) -> Unit
) : ListAdapter<ItemChecklistGuia, ItemChecklistGuiaAdapter.ItemChecklistGuiaViewHolder>(ItemChecklistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemChecklistGuiaViewHolder {
        val binding = ListItemChecklistGuiaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemChecklistGuiaViewHolder(binding, onItemCheckChanged, onDeleteItemClickListener)
    }

    override fun onBindViewHolder(holder: ItemChecklistGuiaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemChecklistGuiaViewHolder(
        private val binding: ListItemChecklistGuiaBinding,
        private val onCheckChanged: (ItemChecklistGuia, Boolean) -> Unit,
        private val onDeleteClick: (ItemChecklistGuia) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemChecklistGuia) {
            binding.textViewDescricaoItemChecklist.text = item.descricaoItem

            // Remove listener temporariamente para evitar trigger ao setar o estado inicial
            binding.checkBoxItemChecklist.setOnCheckedChangeListener(null)
            binding.checkBoxItemChecklist.isChecked = item.concluido
            binding.checkBoxItemChecklist.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(item, isChecked)
            }

            binding.buttonDeletarItemChecklist.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    class ItemChecklistDiffCallback : DiffUtil.ItemCallback<ItemChecklistGuia>() {
        override fun areItemsTheSame(oldItem: ItemChecklistGuia, newItem: ItemChecklistGuia): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemChecklistGuia, newItem: ItemChecklistGuia): Boolean {
            return oldItem == newItem
        }
    }
}
