package com.agendafocopei.ui.model

import com.agendafocopei.data.EventoRecorrente
// HorarioAulaDisplay já está em com.agendafocopei.ui.model

sealed class ItemAgendaDashboard {
    abstract val idUnico: String // Para DiffUtil, pode ser "aula_id" ou "evento_id"
    abstract val horaInicio: String
    abstract val horaFim: String
    abstract val nomePrincipal: String
    abstract val detalheSecundario: String? // Ex: Turma para aula, Local para evento
    abstract val cor: Int?

    data class AulaItem(val aula: HorarioAulaDisplay) : ItemAgendaDashboard() {
        override val idUnico: String = "aula_${aula.id}"
        override val horaInicio: String = aula.horaInicio
        override val horaFim: String = aula.horaFim
        override val nomePrincipal: String = aula.nomeDisciplina
        // Combina turma e sala, se disponíveis
        override val detalheSecundario: String = listOfNotNull(aula.nomeTurma, aula.salaAula).joinToString(" - ")
        override val cor: Int? = aula.corDisciplina
    }

    data class EventoItem(val evento: EventoRecorrente) : ItemAgendaDashboard() {
        override val idUnico: String = "evento_${evento.id}"
        override val horaInicio: String = evento.horaInicio
        override val horaFim: String = evento.horaFim
        override val nomePrincipal: String = evento.nomeEvento
        override val detalheSecundario: String? = evento.salaLocal
        override val cor: Int? = evento.cor
    }
}
