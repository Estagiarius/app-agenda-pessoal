package com.agendafocopei

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agendafocopei.data.Disciplina
import com.agendafocopei.databinding.ListItemDisciplinaSelecaoBinding

// Classe auxiliar para manter o estado da seleção
data class DisciplinaComSelecao(
    val disciplina: Disciplina,
    var isSelecionada: Boolean = false
)

class DisciplinaSelecaoAdapter(
    // Inicializa com uma lista mutável vazia para poder adicionar itens depois
    private var disciplinasComSelecao: MutableList<DisciplinaComSelecao> = mutableListOf()
) : RecyclerView.Adapter<DisciplinaSelecaoAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ListItemDisciplinaSelecaoBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Listener para o checkbox
            binding.checkBoxDisciplinaSelecao.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    disciplinasComSelecao[adapterPosition].isSelecionada = isChecked
                }
            }
            // Listener para o item todo (muda o estado do checkbox)
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    binding.checkBoxDisciplinaSelecao.isChecked = !binding.checkBoxDisciplinaSelecao.isChecked
                    // O listener do checkbox acima já atualiza o 'isSelecionada' no modelo de dados
                }
            }
        }

        fun bind(item: DisciplinaComSelecao) {
            binding.textViewNomeDisciplinaSelecao.text = item.disciplina.nome
            // Remove o listener temporariamente para evitar que a atribuição de isChecked dispare o listener
            binding.checkBoxDisciplinaSelecao.setOnCheckedChangeListener(null)
            binding.checkBoxDisciplinaSelecao.isChecked = item.isSelecionada
            // Restaura o listener
            binding.checkBoxDisciplinaSelecao.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    disciplinasComSelecao[adapterPosition].isSelecionada = isChecked
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemDisciplinaSelecaoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(disciplinasComSelecao[position])
    }

    override fun getItemCount(): Int = disciplinasComSelecao.size

    fun setDisciplinas(disciplinas: List<Disciplina>, associacoesExistentesIds: Set<Int>) {
        disciplinasComSelecao.clear()
        disciplinas.forEach { disciplina ->
            disciplinasComSelecao.add(
                DisciplinaComSelecao(
                    disciplina = disciplina,
                    isSelecionada = associacoesExistentesIds.contains(disciplina.id)
                )
            )
        }
        // Ordena para manter uma exibição consistente, se necessário
        // disciplinasComSelecao.sortBy { it.disciplina.nome }
        notifyDataSetChanged() // Usar DiffUtil seria melhor para performance
    }

    fun getDisciplinasSelecionadasIds(): Set<Int> {
        return disciplinasComSelecao.filter { it.isSelecionada }.map { it.disciplina.id }.toSet()
    }

    // Método para obter todos os itens, pode ser útil para debug ou outras lógicas
    fun getTodasDisciplinasComSelecao(): List<DisciplinaComSelecao> {
        return disciplinasComSelecao.toList() // Retorna uma cópia para evitar modificação externa direta
    }
}
