package com.agendafocopei.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos_recorrentes")
data class EventoRecorrente(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "nome_evento")
    val nomeEvento: String,

    @ColumnInfo(name = "dia_da_semana") // Ex: 1 para Domingo (Calendar.SUNDAY), 2 para Segunda (Calendar.MONDAY), etc.
    val diaDaSemana: Int,

    @ColumnInfo(name = "hora_inicio") // Formato "HH:mm"
    val horaInicio: String,

    @ColumnInfo(name = "hora_fim") // Formato "HH:mm"
    val horaFim: String,

    @ColumnInfo(name = "sala_local")
    val salaLocal: String?,

    @ColumnInfo(name = "cor")
    val cor: Int?, // ARGB Int color

    @ColumnInfo(name = "observacoes")
    val observacoes: String?
)
