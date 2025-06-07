package com.agendafocopei.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "disciplina_turma_cross_ref",
    primaryKeys = ["disciplinaId", "turmaId"],
    foreignKeys = [
        ForeignKey(
            entity = Disciplina::class,
            parentColumns = ["id"],
            childColumns = ["disciplinaId"],
            onDelete = ForeignKey.CASCADE // Se uma Disciplina for deletada, as referências cruzadas são deletadas.
        ),
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
            onDelete = ForeignKey.CASCADE // Se uma Turma for deletada, as referências cruzadas são deletadas.
        )
    ],
    // Índices para otimizar queries que filtram por disciplinaId ou turmaId.
    indices = [Index(value = ["disciplinaId"]), Index(value = ["turmaId"])]
)
data class DisciplinaTurmaCrossRef(
    val disciplinaId: Int,
    val turmaId: Int
)
