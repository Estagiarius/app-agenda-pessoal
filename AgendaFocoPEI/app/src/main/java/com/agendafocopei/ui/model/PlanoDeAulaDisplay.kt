package com.agendafocopei.ui.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.agendafocopei.data.PlanoDeAula

data class PlanoDeAulaDisplay(
    @Embedded val planoDeAula: PlanoDeAula,

    @ColumnInfo(name = "nome_disciplina")
    val nomeDisciplina: String?,

    @ColumnInfo(name = "cor_disciplina") // Adicionando cor para exibição
    val corDisciplina: Int?,

    @ColumnInfo(name = "nome_turma")
    val nomeTurma: String?,

    @ColumnInfo(name = "cor_turma") // Adicionando cor para exibição
    val corTurma: Int?
)
