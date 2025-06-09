package com.agendafocopei.data

import androidx.room.*

@Entity(
    tableName = "tarefas",
    foreignKeys = [
        ForeignKey(
            entity = Disciplina::class,
            parentColumns = ["id"],
            childColumns = ["disciplinaId"],
            onDelete = ForeignKey.SET_NULL // Se Disciplina for deletada, disciplinaId se torna NULL
        ),
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
            onDelete = ForeignKey.SET_NULL // Se Turma for deletada, turmaId se torna NULL
        )
    ],
    indices = [
        Index(value = ["disciplinaId"]),
        Index(value = ["turmaId"]),
        Index(value = ["prazo_data"]), // Índice para query de tarefas urgentes e por data
        Index(value = ["concluida"])   // Índice para query de pendentes/concluídas
    ]
)
data class Tarefa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var descricao: String,
    @ColumnInfo(name = "prazo_data") var prazoData: String?, // Formato "YYYY-MM-DD"
    @ColumnInfo(name = "prazo_hora") var prazoHora: String?, // Formato "HH:mm"
    var prioridade: Int, // 0: Baixa, 1: Média, 2: Alta
    var disciplinaId: Int?,
    var turmaId: Int?,
    var concluida: Boolean = false,
    @ColumnInfo(name = "data_criacao", defaultValue = "0") // DefaultValue para migração e inserção
    val dataCriacao: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "data_conclusao") var dataConclusao: Long? = null,
    @ColumnInfo(name = "lembrete_configurado", defaultValue = "0") // DefaultValue para migração e inserção
    var lembreteConfigurado: Boolean = false,
    @ColumnInfo(name = "lembrete_datetime") var lembreteDateTime: Long? = null // Timestamp Epoch Millis
)
