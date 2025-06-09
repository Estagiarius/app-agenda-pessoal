package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.ListItemGuiaDeAprendizagemBinding
import com.agendafocopei.ui.model.GuiaDeAprendizagemDisplay

class GuiaDeAprendizagemAdapter(
    private val onDetalhesClickListener: (GuiaDeAprendizagemDisplay) -> Unit,
    private val onEditClickListener: (GuiaDeAprendizagemDisplay) -> Unit,
    private val onDeleteClickListener: (GuiaDeAprendizagemDisplay) -> Unit
) : ListAdapter<GuiaDeAprendizagemDisplay, GuiaDeAprendizagemAdapter.GuiaViewHolder>(GuiaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuiaViewHolder {
        val binding = ListItemGuiaDeAprendizagemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GuiaViewHolder(binding, onDetalhesClickListener, onEditClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: GuiaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GuiaViewHolder(
        private val binding: ListItemGuiaDeAprendizagemBinding,
        private val onDetalhes: (GuiaDeAprendizagemDisplay) -> Unit,
        private val onEdit: (GuiaDeAprendizagemDisplay) -> Unit,
        private val onDelete: (GuiaDeAprendizagemDisplay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(guiaDisplay: GuiaDeAprendizagemDisplay) {
            val guia = guiaDisplay.guiaDeAprendizagem

            val tituloGerado = "Guia: ${guiaDisplay.nomeDisciplina ?: ""} - ${guia.bimestre} ${guia.ano}"
            binding.textViewTituloGuiaItem.text = guia.tituloGuia ?: tituloGerado

            // Placeholder para progresso - a ser implementado com contagem de itens do checklist
            binding.textViewProgressoChecklistGuia.text = "Checklist: (N/A)"

            binding.imageViewAnexoIconGuia.visibility = if (guia.caminhoAnexoGuia != null) View.VISIBLE else View.GONE
            binding.viewCorGuia.setBackgroundColor(guiaDisplay.corDisciplina ?: Color.LTGRAY)

            binding.buttonDetalhesGuia.setOnClickListener { onDetalhes(guiaDisplay) }
            binding.buttonEditarGuia.setOnClickListener { onEdit(guiaDisplay) }
            binding.buttonDeletarGuia.setOnClickListener { onDelete(guiaDisplay) }

            // Clique no item inteiro também pode levar aos detalhes
            itemView.setOnClickListener { onDetalhes(guiaDisplay) }
        }
    }

    class GuiaDiffCallback : DiffUtil.ItemCallback<GuiaDeAprendizagemDisplay>() {
        override fun areItemsTheSame(oldItem: GuiaDeAprendizagemDisplay, newItem: GuiaDeAprendizagemDisplay): Boolean {
            return oldItem.guiaDeAprendizagem.id == newItem.guiaDeAprendizagem.id
        }

        override fun areContentsTheSame(oldItem: GuiaDeAprendizagemDisplay, newItem: GuiaDeAprendizagemDisplay): Boolean {
            return oldItem == newItem
        }
    }
}
