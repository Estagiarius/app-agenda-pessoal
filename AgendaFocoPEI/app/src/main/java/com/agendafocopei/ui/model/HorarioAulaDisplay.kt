package com.agendafocopei.ui.model

import java.text.DateFormatSymbols
import java.util.Locale

data class HorarioAulaDisplay(
    val id: Int,
    val diaDaSemana: Int, // java.util.Calendar.DAY_OF_WEEK (1=Domingo, 2=Segunda,...)
    val horaInicio: String,
    val horaFim: String,
    val nomeDisciplina: String,
    val corDisciplina: Int?,
    val disciplinaId: Int, // NOVO
    val nomeTurma: String,
    val corTurma: Int?,
    val turmaId: Int, // NOVO
    val salaAula: String?
) {
    fun getDiaDaSemanaFormatado(): String {
        // java.util.Calendar.DAY_OF_WEEK (1=Domingo, 2=Segunda, ..., 7=Sábado)
        // DateFormatSymbols().weekdays retorna um array onde o índice 0 é vazio, 1 é Domingo, etc.
        val weekdays = DateFormatSymbols(Locale("pt", "BR")).weekdays
        return if (diaDaSemana in 1..7) weekdays[diaDaSemana] else "Dia inválido"
    }
}
