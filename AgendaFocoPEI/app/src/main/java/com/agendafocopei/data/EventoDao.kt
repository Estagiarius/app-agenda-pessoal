package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao { // INTERFACE RENOMEADA

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(evento: Evento): Long // Tipo atualizado

    @Update
    suspend fun atualizar(evento: Evento) // Tipo atualizado

    @Delete
    suspend fun deletar(evento: Evento) // Tipo atualizado

    @Query("SELECT * FROM eventos_recorrentes ORDER BY dia_da_semana ASC, hora_inicio ASC")
    fun buscarTodosOrdenados(): Flow<List<Evento>> // Tipo atualizado

    @Query("SELECT * FROM eventos_recorrentes WHERE id = :id")
    suspend fun buscarPorId(id: Int): Evento? // Tipo atualizado

    @Query("DELETE FROM eventos_recorrentes WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Query modificada para buscar eventos para uma data específica (eventos únicos)
    // OU eventos recorrentes que caem naquele dia da semana (se não tiverem data_especifica)
    @Query("""
        SELECT * FROM eventos_recorrentes
        WHERE
            (data_especifica = :dataComoString) OR
            (data_especifica IS NULL AND dia_da_semana = :diaDaSemana)
        ORDER BY hora_inicio ASC
    """)
    fun buscarEventosParaData(diaDaSemana: Int, dataComoString: String): Flow<List<Evento>> // Tipo atualizado

    // Query modificada para buscar o próximo evento para uma data específica,
    // considerando eventos únicos para aquela data ou eventos recorrentes para aquele dia da semana.
    @Query("""
        SELECT * FROM eventos_recorrentes
        WHERE
            ((data_especifica = :dataAtualComoString) OR (data_especifica IS NULL AND dia_da_semana = :diaSemanaAtual))
            AND hora_inicio > :horaAtual
        ORDER BY hora_inicio ASC
        LIMIT 1
    """)
    suspend fun buscarProximoEventoParaData(diaSemanaAtual: Int, dataAtualComoString: String, horaAtual: String): Evento? // Tipo atualizado
}
