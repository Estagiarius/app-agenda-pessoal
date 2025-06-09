package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemChecklistGuiaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: ItemChecklistGuia): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarios(itens: List<ItemChecklistGuia>): List<Long>

    @Update
    suspend fun atualizar(item: ItemChecklistGuia)

    @Update
    suspend fun atualizarVarios(itens: List<ItemChecklistGuia>)


    @Delete
    suspend fun deletar(item: ItemChecklistGuia)

    @Query("DELETE FROM itens_checklist_guia WHERE guiaAprendizagemId = :guiaId")
    suspend fun deletarPorGuiaId(guiaId: Int)

    @Query("SELECT * FROM itens_checklist_guia WHERE guiaAprendizagemId = :guiaId ORDER BY id ASC")
    fun buscarPorGuiaId(guiaId: Int): Flow<List<ItemChecklistGuia>>

    @Query("SELECT * FROM itens_checklist_guia WHERE id = :id")
    suspend fun buscarPorId(id: Int): ItemChecklistGuia?
}
