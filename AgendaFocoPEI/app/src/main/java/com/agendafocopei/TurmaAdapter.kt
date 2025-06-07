package com.agendafocopei

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.Turma
import com.agendafocopei.databinding.ListItemTurmaBinding // Assume-se que o ViewBinding gerará este nome

class TurmaAdapter(private var items: List<Turma>) :
    RecyclerView.Adapter<TurmaAdapter.TurmaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TurmaViewHolder {
        val itemBinding = ListItemTurmaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TurmaViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: TurmaViewHolder, position: Int) {
        val turma = items[position]
        holder.bind(turma)
    }

    override fun getItemCount(): Int = items.size

    fun updateTurmas(newTurmas: List<Turma>) {
        items = newTurmas
        // Para simplicidade inicial, usando notifyDataSetChanged.
        // Considere DiffUtil para otimizações futuras.
        notifyDataSetChanged()
    }

import android.content.Intent // Import necessário

import android.content.Intent // Import necessário
import android.graphics.Color // Import para Color

    inner class TurmaViewHolder(private val itemBinding: ListItemTurmaBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        init {
            itemBinding.buttonAssociarDisciplinasItem.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val turma = items[position]
                    val context = itemView.context
                    val intent = Intent(context, AssociarDisciplinasActivity::class.java).apply {
                        putExtra(AssociarDisciplinasActivity.EXTRA_TURMA_ID, turma.id)
                        putExtra(AssociarDisciplinasActivity.EXTRA_NOME_TURMA, turma.nome)
                    }
                    context.startActivity(intent)
                }
            }
        }

        fun bind(turma: Turma) {
            itemBinding.textViewNomeTurma.text = turma.nome
            if (turma.cor != null) {
                itemBinding.viewCorTurma.setBackgroundColor(turma.cor)
            } else {
                itemBinding.viewCorTurma.setBackgroundColor(Color.LTGRAY) // Ou Color.TRANSPARENT
            }
            // O listener do botão é configurado no init {}
        }
    }
}
