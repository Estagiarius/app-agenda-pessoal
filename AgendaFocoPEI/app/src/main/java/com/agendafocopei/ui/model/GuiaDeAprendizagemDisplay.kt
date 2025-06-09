package com.agendafocopei.ui.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.agendafocopei.data.GuiaDeAprendizagem

data class GuiaDeAprendizagemDisplay(
    @Embedded val guiaDeAprendizagem: GuiaDeAprendizagem,

    @ColumnInfo(name = "nome_disciplina")
    val nomeDisciplina: String?,

    @ColumnInfo(name = "cor_disciplina")
    val corDisciplina: Int?
    // Adicionar contagem de itens do checklist se necessário diretamente aqui,
    // ou calcular depois.
)
