package com.agendafocopei.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.Anotacao
import com.agendafocopei.data.TurmaDao
import com.agendafocopei.databinding.ListItemAnotacaoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnotacaoAdapter(
    private val turmaDao: TurmaDao, // Passar o DAO para buscar nome da turma
    private val onItemClickListener: (Anotacao) -> Unit,
    private val onEditClickListener: (Anotacao) -> Unit,
    private val onDeleteClickListener: (Anotacao) -> Unit
) : ListAdapter<Anotacao, AnotacaoAdapter.AnotacaoViewHolder>(AnotacaoDiffCallback()) {

    private val displayDateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnotacaoViewHolder {
        val binding = ListItemAnotacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnotacaoViewHolder(binding, turmaDao, displayDateFormat, onItemClickListener, onEditClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: AnotacaoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AnotacaoViewHolder(
        private val binding: ListItemAnotacaoBinding,
        private val turmaDao: TurmaDao,
        private val dateFormatter: SimpleDateFormat,
        private val onItemClick: (Anotacao) -> Unit,
        private val onEditClick: (Anotacao) -> Unit,
        private val onDeleteClick: (Anotacao) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(anotacao: Anotacao) {
            binding.textViewConteudoAnotacaoItem.text = anotacao.conteudo
            binding.textViewDataModificacaoAnotacaoItem.text = "Modif.: ${dateFormatter.format(Date(anotacao.dataModificacao))}"

            binding.viewCorAnotacaoItem.setBackgroundColor(anotacao.cor ?: Color.TRANSPARENT)

            if (!anotacao.tagsString.isNullOrEmpty()) {
                binding.textViewTagsAnotacaoItem.text = "Tags: ${anotacao.tagsString}"
                binding.textViewTagsAnotacaoItem.visibility = View.VISIBLE
            } else {
                binding.textViewTagsAnotacaoItem.visibility = View.GONE
            }

            // Associacao (Turma e Aluno)
            var associacaoText = ""
            if (anotacao.turmaId != null) {
                // Busca nome da turma de forma assíncrona
                itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                    val turma = withContext(Dispatchers.IO) { turmaDao.buscarPorId(anotacao.turmaId!!) }
                    associacaoText = "Turma: ${turma?.nome ?: "ID: "+anotacao.turmaId}"
                    if (!anotacao.alunoNome.isNullOrEmpty()) {
                        associacaoText += " (Aluno: ${anotacao.alunoNome})"
                    }
                    binding.textViewAssociacaoAnotacaoItem.text = associacaoText
                    binding.textViewAssociacaoAnotacaoItem.visibility = View.VISIBLE
                }
            } else if (!anotacao.alunoNome.isNullOrEmpty()) {
                associacaoText = "Aluno: ${anotacao.alunoNome}"
                binding.textViewAssociacaoAnotacaoItem.text = associacaoText
                binding.textViewAssociacaoAnotacaoItem.visibility = View.VISIBLE
            } else {
                binding.textViewAssociacaoAnotacaoItem.visibility = View.GONE
            }


            binding.buttonEditarAnotacaoItem.setOnClickListener { onEditClick(anotacao) }
            binding.buttonDeletarAnotacaoItem.setOnClickListener { onDeleteClick(anotacao) }
            itemView.setOnClickListener { onItemClick(anotacao) }
        }
    }

    class AnotacaoDiffCallback : DiffUtil.ItemCallback<Anotacao>() {
        override fun areItemsTheSame(oldItem: Anotacao, newItem: Anotacao): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Anotacao, newItem: Anotacao): Boolean {
            return oldItem == newItem
        }
    }
}
