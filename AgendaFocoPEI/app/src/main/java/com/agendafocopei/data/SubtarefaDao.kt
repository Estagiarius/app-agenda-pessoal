package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtarefaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(subtarefa: Subtarefa): Long

    @Update
    suspend fun atualizar(subtarefa: Subtarefa)

    @Delete
    suspend fun deletar(subtarefa: Subtarefa)

    @Query("SELECT * FROM subtarefas WHERE tarefaId = :tarefaId ORDER BY ordem ASC, id ASC")
    fun buscarPorTarefaId(tarefaId: Int): Flow<List<Subtarefa>>

    @Query("SELECT * FROM subtarefas WHERE id = :subtarefaId") // Adicionado para buscar uma subtarefa específica
    suspend fun buscarPorId(subtarefaId: Int): Subtarefa?

    @Query("DELETE FROM subtarefas WHERE id = :subtarefaId")
    suspend fun deleteById(subtarefaId: Int)

    @Query("DELETE FROM subtarefas WHERE tarefaId = :tarefaId")
    suspend fun deleteTodasPorTarefaId(tarefaId: Int)
}
