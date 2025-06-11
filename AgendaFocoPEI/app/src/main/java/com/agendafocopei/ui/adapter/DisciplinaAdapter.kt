package com.agendafocopei

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.Disciplina
import com.agendafocopei.databinding.ListItemDisciplinaBinding

class DisciplinaAdapter(private var items: List<Disciplina>) :
    RecyclerView.Adapter<DisciplinaAdapter.DisciplinaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplinaViewHolder {
        val itemBinding = ListItemDisciplinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DisciplinaViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: DisciplinaViewHolder, position: Int) {
        val disciplina = items[position]
        holder.bind(disciplina)
    }

    override fun getItemCount(): Int = items.size

    fun updateDisciplinas(newDisciplinas: List<Disciplina>) {
        items = newDisciplinas
        // TODO: Usar DiffUtil para melhor performance em atualizações futuras.
        // Por enquanto, notifyDataSetChanged é mais simples para a refatoração inicial.
        notifyDataSetChanged()
    }

import android.graphics.Color // Import necessário

    inner class DisciplinaViewHolder(private val itemBinding: ListItemDisciplinaBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(disciplina: Disciplina) {
            itemBinding.textViewNomeDisciplina.text = disciplina.nome
            if (disciplina.cor != null) {
                itemBinding.viewCorDisciplina.setBackgroundColor(disciplina.cor)
            } else {
                // Define uma cor padrão ou torna transparente se a cor for nula
                itemBinding.viewCorDisciplina.setBackgroundColor(Color.LTGRAY) // Ou Color.TRANSPARENT
            }
        }
    }
}
