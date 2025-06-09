package com.agendafocopei.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.databinding.ListItemPlanoDeAulaBinding
import com.agendafocopei.ui.model.PlanoDeAulaDisplay
import java.text.SimpleDateFormat
import java.util.*

class PlanoDeAulaAdapter(
    private val onEditClickListener: (PlanoDeAulaDisplay) -> Unit,
    private val onDeleteClickListener: (PlanoDeAulaDisplay) -> Unit,
    private val onItemClickListener: (PlanoDeAulaDisplay) -> Unit
) : ListAdapter<PlanoDeAulaDisplay, PlanoDeAulaAdapter.PlanoViewHolder>(PlanoDiffCallback()) {

    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val storedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanoViewHolder {
        val binding = ListItemPlanoDeAulaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanoViewHolder(binding, onEditClickListener, onDeleteClickListener, onItemClickListener)
    }

    override fun onBindViewHolder(holder: PlanoViewHolder, position: Int) {
        holder.bind(getItem(position), displayDateFormat, storedDateFormat)
    }

    class PlanoViewHolder(
        private val binding: ListItemPlanoDeAulaBinding,
        private val onEdit: (PlanoDeAulaDisplay) -> Unit,
        private val onDelete: (PlanoDeAulaDisplay) -> Unit,
        private val onItemClick: (PlanoDeAulaDisplay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(planoDisplay: PlanoDeAulaDisplay, displayFormatter: SimpleDateFormat, storedFormatter: SimpleDateFormat) {
            val plano = planoDisplay.planoDeAula

            binding.textViewTituloPlanoItem.text = plano.tituloPlano ?: "Plano para ${planoDisplay.nomeDisciplina ?: "Disciplina"}"

            if (plano.dataAula != null) {
                try {
                    val date = storedFormatter.parse(plano.dataAula)
                    binding.textViewDataPlanoItem.text = "Data: ${date?.let { displayFormatter.format(it) } ?: "N/A"}"
                    binding.textViewDataPlanoItem.visibility = View.VISIBLE
                } catch (e: Exception) {
                    binding.textViewDataPlanoItem.text = "Data: ${plano.dataAula}" // Formato original se falhar
                    binding.textViewDataPlanoItem.visibility = View.VISIBLE
                }
            } else {
                binding.textViewDataPlanoItem.visibility = View.GONE
            }

            binding.textViewDisciplinaPlanoItem.text = "Disciplina: ${planoDisplay.nomeDisciplina ?: "Não especificada"}"

            if (planoDisplay.nomeTurma != null) {
                binding.textViewTurmaPlanoItem.text = "Turma: ${planoDisplay.nomeTurma}"
                binding.textViewTurmaPlanoItem.visibility = View.VISIBLE
            } else {
                binding.textViewTurmaPlanoItem.visibility = View.GONE
            }

            binding.imageViewAnexoIconPlanoItem.visibility = if (plano.caminhoAnexo != null) View.VISIBLE else View.GONE

            // Usar cor da disciplina como indicador
            binding.viewCorIndicadorPlano.setBackgroundColor(planoDisplay.corDisciplina ?: Color.LTGRAY)

            binding.buttonEditarPlano.setOnClickListener { onEdit(planoDisplay) }
            binding.buttonDeletarPlano.setOnClickListener { onDelete(planoDisplay) }
            itemView.setOnClickListener { onItemClick(planoDisplay) }
        }
    }

    class PlanoDiffCallback : DiffUtil.ItemCallback<PlanoDeAulaDisplay>() {
        override fun areItemsTheSame(oldItem: PlanoDeAulaDisplay, newItem: PlanoDeAulaDisplay): Boolean {
            return oldItem.planoDeAula.id == newItem.planoDeAula.id
        }

        override fun areContentsTheSame(oldItem: PlanoDeAulaDisplay, newItem: PlanoDeAulaDisplay): Boolean {
            return oldItem == newItem
        }
    }
}
