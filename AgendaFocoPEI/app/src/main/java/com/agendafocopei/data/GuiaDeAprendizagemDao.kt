package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuiaDeAprendizagemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(guia: GuiaDeAprendizagem): Long

    @Update
    suspend fun atualizar(guia: GuiaDeAprendizagem)

    @Delete
    suspend fun deletar(guia: GuiaDeAprendizagem)

    @Query("SELECT * FROM guias_de_aprendizagem WHERE id = :id")
    suspend fun buscarPorId(id: Int): GuiaDeAprendizagem?

    @Query("SELECT * FROM guias_de_aprendizagem ORDER BY ano DESC, bimestre DESC")
    fun buscarTodos(): Flow<List<GuiaDeAprendizagem>>

    @Query("SELECT * FROM guias_de_aprendizagem WHERE disciplinaId = :disciplinaId ORDER BY ano DESC, bimestre DESC")
    fun buscarPorDisciplina(disciplinaId: Int): Flow<List<GuiaDeAprendizagem>>

    @Query("SELECT * FROM guias_de_aprendizagem WHERE ano = :ano ORDER BY bimestre DESC")
    fun buscarPorAno(ano: Int): Flow<List<GuiaDeAprendizagem>>

    @Transaction
    @Query("""
        SELECT g.*, d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina
        FROM guias_de_aprendizagem g
        INNER JOIN disciplinas d ON g.disciplinaId = d.id
        ORDER BY g.ano DESC, g.bimestre DESC
    """)
    fun buscarTodosParaDisplay(): Flow<List<com.agendafocopei.ui.model.GuiaDeAprendizagemDisplay>>

    @Transaction
    @Query("""
        SELECT g.*, d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina
        FROM guias_de_aprendizagem g
        INNER JOIN disciplinas d ON g.disciplinaId = d.id
        WHERE g.id = :guiaId
    """)
    suspend fun buscarDisplayPorId(guiaId: Int): com.agendafocopei.ui.model.GuiaDeAprendizagemDisplay?
}
