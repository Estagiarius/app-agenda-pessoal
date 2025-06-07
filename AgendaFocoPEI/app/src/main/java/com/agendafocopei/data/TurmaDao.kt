package com.agendafocopei.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TurmaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(turma: Turma)

    @Query("SELECT * FROM turmas ORDER BY nome_turma ASC")
    fun buscarTodas(): Flow<List<Turma>>

import androidx.room.Delete // Import necessário

    @Query("SELECT * FROM turmas WHERE nome_turma = :nomeTurma LIMIT 1")
    suspend fun buscarPorNome(nomeTurma: String): Turma?

    @Delete
    suspend fun deletar(turma: Turma)

    // @Update
    // suspend fun atualizar(turma: Turma) // Pode ser adicionado se necessário
}
