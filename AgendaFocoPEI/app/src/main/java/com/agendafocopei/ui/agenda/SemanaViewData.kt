package com.agendafocopei.ui.agenda

import java.util.Calendar

// Representa um evento compacto para a lista aninhada
data class EventoSemanaCompacto(
    val idOriginal: Int, // ID real do HorarioAula ou EventoRecorrente
    val tipo: String, // "aula" ou "evento"
    val idUnico: String, // "aula_idOriginal" ou "evento_idOriginal"
    val horario: String, // "HH:mm" (apenas início)
    val nome: String,
    val cor: Int?
)

// Representa um dia na visualização da semana
data class DiaDaSemanaComEventos(
    val data: Calendar, // Contém a data completa do dia
    val nomeDiaAbrev: String, // "SEG", "TER", etc.
    val dataFormatadaCurta: String, // "10/06"
    var eventos: List<EventoSemanaCompacto> // Lista de eventos para este dia
)
