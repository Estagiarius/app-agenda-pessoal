package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HorarioAulaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(horarioAula: HorarioAula): Long // Retorna o ID da linha inserida/substituída

    @Update
    suspend fun atualizar(horarioAula: HorarioAula)

    @Delete
    suspend fun deletar(horarioAula: HorarioAula)

    @Query("SELECT * FROM horarios_aula ORDER BY dia_da_semana ASC, hora_inicio ASC")
    fun buscarTodosOrdenados(): Flow<List<HorarioAula>>

    @Query("SELECT * FROM horarios_aula WHERE dia_da_semana = :diaDaSemana ORDER BY hora_inicio ASC")
    fun buscarPorDia(diaDaSemana: Int): Flow<List<HorarioAula>>

    @Query("SELECT * FROM horarios_aula WHERE turmaId = :turmaId ORDER BY dia_da_semana ASC, hora_inicio ASC")
    fun buscarPorTurma(turmaId: Int): Flow<List<HorarioAula>>

    @Query("SELECT * FROM horarios_aula WHERE disciplinaId = :disciplinaId ORDER BY dia_da_semana ASC, hora_inicio ASC")
    fun buscarPorDisciplina(disciplinaId: Int): Flow<List<HorarioAula>>

    @Query("SELECT * FROM horarios_aula WHERE id = :id")
    suspend fun buscarPorId(id: Int): HorarioAula?

    @Query("DELETE FROM horarios_aula WHERE id = :horarioId")
    suspend fun deleteById(horarioId: Int)

    @Query("DELETE FROM horarios_aula WHERE turmaId = :turmaId AND disciplinaId = :disciplinaId")
    suspend fun deletarPorTurmaEDisciplina(turmaId: Int, disciplinaId: Int)

    @Transaction
    @Query("""
        SELECT
            h.id,
            h.dia_da_semana AS diaDaSemana,
            h.hora_inicio AS horaInicio,
            h.hora_fim AS horaFim,
            d.nome_disciplina AS nomeDisciplina,
            d.cor AS corDisciplina,
            t.nome_turma AS nomeTurma,
            t.cor AS corTurma,
            h.sala_aula AS salaAula
        FROM horarios_aula h
        INNER JOIN disciplinas d ON h.disciplinaId = d.id
        INNER JOIN turmas t ON h.turmaId = t.id
        ORDER BY h.dia_da_semana ASC, h.hora_inicio ASC
    """)
    fun buscarTodosParaDisplay(): Flow<List<com.agendafocopei.ui.model.HorarioAulaDisplay>>

    @Transaction
    @Query("""
        SELECT
            h.id,
            h.dia_da_semana AS diaDaSemana,
            h.hora_inicio AS horaInicio,
            h.hora_fim AS horaFim,
            d.nome_disciplina AS nomeDisciplina,
            d.cor AS corDisciplina,
            t.nome_turma AS nomeTurma,
            t.cor AS corTurma,
            h.sala_aula AS salaAula
        FROM horarios_aula h
        INNER JOIN disciplinas d ON h.disciplinaId = d.id
        INNER JOIN turmas t ON h.turmaId = t.id
        WHERE h.dia_da_semana = :diaSemanaAtual AND h.hora_inicio > :horaAtual
        ORDER BY h.hora_inicio ASC
        LIMIT 1
    """)
    suspend fun buscarProximoHorarioAulaDoDia(diaSemanaAtual: Int, horaAtual: String): com.agendafocopei.ui.model.HorarioAulaDisplay?

    @Transaction
    @Query("""
        SELECT
            h.id,
            h.dia_da_semana AS diaDaSemana,
            h.hora_inicio AS horaInicio,
            h.hora_fim AS horaFim,
            d.nome_disciplina AS nomeDisciplina,
            d.cor AS corDisciplina,
            t.nome_turma AS nomeTurma,
            t.cor AS corTurma,
            h.sala_aula AS salaAula
        FROM horarios_aula h
        INNER JOIN disciplinas d ON h.disciplinaId = d.id
        INNER JOIN turmas t ON h.turmaId = t.id
        WHERE h.dia_da_semana = :diaSemanaAtual
        ORDER BY h.hora_inicio ASC
    """)
    fun buscarTodosParaDisplayPorDia(diaSemanaAtual: Int): Flow<List<com.agendafocopei.ui.model.HorarioAulaDisplay>>

    @Transaction
    @Query("""
        SELECT
            h.id,
            h.dia_da_semana AS diaDaSemana,
            h.hora_inicio AS horaInicio,
            h.hora_fim AS horaFim,
            d.nome_disciplina AS nomeDisciplina,
            d.cor AS corDisciplina,
            t.nome_turma AS nomeTurma,
            t.cor AS corTurma,
            h.sala_aula AS salaAula
        FROM horarios_aula h
        INNER JOIN disciplinas d ON h.disciplinaId = d.id
        INNER JOIN turmas t ON h.turmaId = t.id
        WHERE h.id = :horarioId
    """)
    suspend fun buscarDisplayPorId(horarioId: Int): com.agendafocopei.ui.model.HorarioAulaDisplay?
}
