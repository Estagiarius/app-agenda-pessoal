package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoRecorrenteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(evento: EventoRecorrente): Long // Retorna o ID da linha inserida/substituída

    @Update
    suspend fun atualizar(evento: EventoRecorrente)

    @Delete
    suspend fun deletar(evento: EventoRecorrente)

    @Query("SELECT * FROM eventos_recorrentes ORDER BY dia_da_semana ASC, hora_inicio ASC")
    fun buscarTodosOrdenados(): Flow<List<EventoRecorrente>>

    @Query("SELECT * FROM eventos_recorrentes WHERE id = :id")
    suspend fun buscarPorId(id: Int): EventoRecorrente?

    @Query("DELETE FROM eventos_recorrentes WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM eventos_recorrentes WHERE dia_da_semana = :diaDaSemana ORDER BY hora_inicio ASC")
    fun buscarPorDia(diaDaSemana: Int): Flow<List<EventoRecorrente>>
}
