package com.agendafocopei.data

import androidx.room.*

@Entity(
    tableName = "subtarefas",
    foreignKeys = [
        ForeignKey(
            entity = Tarefa::class,
            parentColumns = ["id"],
            childColumns = ["tarefaId"],
            onDelete = ForeignKey.CASCADE // Se a Tarefa pai for deletada, as subtarefas também serão
        )
    ],
    indices = [Index(value = ["tarefaId"])]
)
data class Subtarefa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tarefaId: Int,
    @ColumnInfo(name = "descricao_subtarefa") var descricaoSubtarefa: String,
    var concluida: Boolean = false,
    @ColumnInfo(defaultValue = "0") // Para ordenação futura, ou pode ser timestamp para ordem de criação
    var ordem: Int = 0
)
