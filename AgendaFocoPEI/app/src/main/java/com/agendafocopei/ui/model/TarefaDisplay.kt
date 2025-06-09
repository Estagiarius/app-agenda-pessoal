package com.agendafocopei.ui.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.agendafocopei.data.Tarefa

data class TarefaDisplay(
    @Embedded val tarefa: Tarefa,

    @ColumnInfo(name = "nome_disciplina")
    val nomeDisciplina: String?,

    @ColumnInfo(name = "cor_disciplina")
    val corDisciplina: Int?,

    @ColumnInfo(name = "nome_turma")
    val nomeTurma: String?,

    @ColumnInfo(name = "cor_turma")
    val corTurma: Int?
)
