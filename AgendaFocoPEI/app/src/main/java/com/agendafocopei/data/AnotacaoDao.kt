package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnotacaoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(anotacao: Anotacao): Long

    @Update
    suspend fun atualizar(anotacao: Anotacao)

    @Delete
    suspend fun deletar(anotacao: Anotacao)

    @Query("DELETE FROM anotacoes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM anotacoes WHERE id = :id")
    suspend fun buscarPorId(id: Int): Anotacao?

    @Query("SELECT * FROM anotacoes ORDER BY data_modificacao DESC")
    fun buscarTodas(): Flow<List<Anotacao>>

    @Query("SELECT * FROM anotacoes WHERE turmaId = :turmaId ORDER BY data_modificacao DESC")
    fun buscarPorTurmaId(turmaId: Int): Flow<List<Anotacao>>

    // Query para buscar por tags (simples, usando LIKE)
    // Adicionar % antes e depois do termoTag para buscar em qualquer parte da string
    // Ex: Para buscar #ideia, termoTag seria "%#ideia%"
    @Query("SELECT * FROM anotacoes WHERE tags_string LIKE :termoTag ORDER BY data_modificacao DESC")
    fun buscarPorTag(termoTag: String): Flow<List<Anotacao>>

    // Query para buscar por conteúdo (usando FTS se configurado, ou LIKE para simples)
    @Query("SELECT * FROM anotacoes WHERE conteudo LIKE :textoConteudo ORDER BY data_modificacao DESC")
    fun buscarPorConteudo(textoConteudo: String): Flow<List<Anotacao>>
}
