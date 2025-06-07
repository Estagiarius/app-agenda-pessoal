package com.agendafocopei.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "horarios_aula",
    foreignKeys = [
        ForeignKey(
            entity = Disciplina::class,
            parentColumns = ["id"],
            childColumns = ["disciplinaId"],
            onDelete = ForeignKey.CASCADE // Se a Disciplina for deletada, os HorariosAula associados são deletados.
        ),
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
            onDelete = ForeignKey.CASCADE // Se a Turma for deletada, os HorariosAula associados são deletados.
        )
    ],
    indices = [Index(value = ["disciplinaId"]), Index(value = ["turmaId"])]
)
data class HorarioAula(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "dia_da_semana")
    val diaDaSemana: Int, // Ex: 1 para Segunda, 2 para Terça, etc. (java.util.Calendar.DAY_OF_WEEK)

    @ColumnInfo(name = "hora_inicio")
    val horaInicio: String, // Formato "HH:mm"

    @ColumnInfo(name = "hora_fim")
    val horaFim: String, // Formato "HH:mm"

    @ColumnInfo(name = "disciplinaId")
    val disciplinaId: Int,

    @ColumnInfo(name = "turmaId")
    val turmaId: Int,

    @ColumnInfo(name = "sala_aula")
    val salaAula: String? = null // Sala opcional
)
