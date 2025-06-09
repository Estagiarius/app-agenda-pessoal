package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplatePlanoAulaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(template: TemplatePlanoAula): Long

    @Update
    suspend fun atualizar(template: TemplatePlanoAula)

    @Delete
    suspend fun deletar(template: TemplatePlanoAula)

    @Query("SELECT * FROM templates_plano_aula WHERE id = :id")
    suspend fun buscarPorId(id: Int): TemplatePlanoAula?

    @Query("SELECT * FROM templates_plano_aula ORDER BY nome_template ASC")
    fun buscarTodos(): Flow<List<TemplatePlanoAula>>
}
