package com.agendafocopei.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplinaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignora se já existir uma com o mesmo nome (precisaria de unique index no nome) ou PK
    suspend fun inserir(disciplina: Disciplina)

    @Query("SELECT * FROM disciplinas ORDER BY nome_disciplina ASC")
    fun buscarTodas(): Flow<List<Disciplina>>

    @Query("SELECT * FROM disciplinas WHERE nome_disciplina = :nomeDisciplina LIMIT 1")
    suspend fun buscarPorNome(nomeDisciplina: String): Disciplina?

    @Delete
    suspend fun deletar(disciplina: Disciplina)
}
