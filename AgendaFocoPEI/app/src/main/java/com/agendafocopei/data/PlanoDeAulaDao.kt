package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanoDeAulaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(plano: PlanoDeAula): Long

    @Update
    suspend fun atualizar(plano: PlanoDeAula)

    @Delete
    suspend fun deletar(plano: PlanoDeAula)

    @Query("SELECT * FROM planos_de_aula WHERE id = :id")
    suspend fun buscarPorId(id: Int): PlanoDeAula?

    @Query("SELECT * FROM planos_de_aula WHERE horarioAulaId = :horarioId")
    fun buscarPorHorarioAulaId(horarioId: Int): Flow<List<PlanoDeAula>>

    // Buscar planos por disciplinaId, ordenados pela data da aula (mais recentes primeiro)
    // Se data_aula for nula, esses podem aparecer no final ou no início dependendo do NULLS LAST/FIRST do SQLite
    @Query("SELECT * FROM planos_de_aula WHERE disciplinaId = :disciplinaId ORDER BY data_aula DESC, id DESC")
    fun buscarPorDisciplina(disciplinaId: Int): Flow<List<PlanoDeAula>>

    // Buscar todos os planos, ordenados pela data da aula (mais recentes primeiro)
    @Query("SELECT * FROM planos_de_aula ORDER BY data_aula DESC, id DESC")
    fun buscarTodos(): Flow<List<PlanoDeAula>>

    // Query para buscar planos associados a uma disciplina e uma turma específica
    @Query("SELECT * FROM planos_de_aula WHERE disciplinaId = :disciplinaId AND turmaId = :turmaId ORDER BY data_aula DESC, id DESC")
    fun buscarPorDisciplinaETurma(disciplinaId: Int, turmaId: Int): Flow<List<PlanoDeAula>>

    // Query para buscar planos por uma data específica
    @Query("SELECT * FROM planos_de_aula WHERE data_aula = :data ORDER BY id DESC")
    fun buscarPorData(data: String): Flow<List<PlanoDeAula>>

    @Transaction
    @Query("""
        SELECT pda.*,
               d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina,
               t.nome_turma AS nome_turma, t.cor AS cor_turma
        FROM planos_de_aula pda
        INNER JOIN disciplinas d ON pda.disciplinaId = d.id
        LEFT JOIN turmas t ON pda.turmaId = t.id
        ORDER BY pda.data_aula DESC, pda.id DESC
    """)
    fun buscarTodosParaDisplay(): Flow<List<com.agendafocopei.ui.model.PlanoDeAulaDisplay>>

    @Transaction
    @Query("""
        SELECT pda.*,
               d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina,
               t.nome_turma AS nome_turma, t.cor AS cor_turma
        FROM planos_de_aula pda
        INNER JOIN disciplinas d ON pda.disciplinaId = d.id
        LEFT JOIN turmas t ON pda.turmaId = t.id
        WHERE pda.id = :planoId
    """)
    suspend fun buscarDisplayPorId(planoId: Int): com.agendafocopei.ui.model.PlanoDeAulaDisplay?

    @Query("SELECT * FROM planos_de_aula WHERE horarioAulaId = :horarioId LIMIT 1")
    suspend fun buscarUmPorHorarioAulaId(horarioId: Int): PlanoDeAula?
}
