package com.agendafocopei.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ItemChecklistGuiaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var guiaDeAprendizagemDao: GuiaDeAprendizagemDao
    private lateinit var itemChecklistGuiaDao: ItemChecklistGuiaDao

    private var d1Id: Int = 0
    private var g1Id: Int = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        disciplinaDao = db.disciplinaDao()
        guiaDeAprendizagemDao = db.guiaDeAprendizagemDao()
        itemChecklistGuiaDao = db.itemChecklistGuiaDao()

        val disc1 = Disciplina(nome = "Checklist Disc")
        disciplinaDao.inserir(disc1)
        d1Id = disciplinaDao.buscarPorNome("Checklist Disc")!!.id

        val guia1 = GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "1º Check", ano = 2024)
        g1Id = guiaDeAprendizagemDao.inserir(guia1).toInt()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirItemChecklist_eBuscarPorGuiaId_retornaItensCorretos() = runBlocking {
        val item1 = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item A", concluido = false)
        val item2 = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item B", concluido = true)
        itemChecklistGuiaDao.inserir(item1)
        itemChecklistGuiaDao.inserir(item2)

        val itens = itemChecklistGuiaDao.buscarPorGuiaId(g1Id).first()
        assertEquals(2, itens.size)
        assertEquals("Item A", itens[0].descricaoItem)
        assertFalse(itens[0].concluido)
        assertEquals("Item B", itens[1].descricaoItem)
        assertTrue(itens[1].concluido)
    }

    @Test
    fun inserirVariosItens_eBuscarPorGuiaId_retornaTodos() = runBlocking {
        val itensParaInserir = listOf(
            ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item C", concluido = false),
            ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item D", concluido = true)
        )
        itemChecklistGuiaDao.inserirVarios(itensParaInserir)
        val itens = itemChecklistGuiaDao.buscarPorGuiaId(g1Id).first()
        assertEquals(2, itens.size)
    }


    @Test
    fun atualizarItemChecklist_concluidoMudou() = runBlocking {
        val item = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item E", concluido = false)
        val id = itemChecklistGuiaDao.inserir(item).toInt()

        val itemParaAtualizar = itemChecklistGuiaDao.buscarPorId(id)!!.copy(concluido = true)
        itemChecklistGuiaDao.atualizar(itemParaAtualizar)

        val atualizado = itemChecklistGuiaDao.buscarPorId(id)
        assertTrue(atualizado!!.concluido)
    }

    @Test
    fun atualizarVariosItens_refleteMudancas() = runBlocking {
        val itemF = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item F", concluido = false)
        val itemG = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item G", concluido = false)
        val idF = itemChecklistGuiaDao.inserir(itemF).toInt()
        val idG = itemChecklistGuiaDao.inserir(itemG).toInt()

        val itensParaAtualizar = listOf(
            itemChecklistGuiaDao.buscarPorId(idF)!!.copy(concluido = true),
            itemChecklistGuiaDao.buscarPorId(idG)!!.copy(descricaoItem = "Item G Modificado")
        )
        itemChecklistGuiaDao.atualizarVarios(itensParaAtualizar)

        assertTrue(itemChecklistGuiaDao.buscarPorId(idF)!!.concluido)
        assertEquals("Item G Modificado", itemChecklistGuiaDao.buscarPorId(idG)!!.descricaoItem)
    }


    @Test
    fun deletarItemChecklist_naoDeveSerEncontrado() = runBlocking {
        val item = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item H", concluido = false)
        val id = itemChecklistGuiaDao.inserir(item).toInt()
        val itemParaDeletar = itemChecklistGuiaDao.buscarPorId(id)!!
        itemChecklistGuiaDao.deletar(itemParaDeletar)
        assertNull(itemChecklistGuiaDao.buscarPorId(id))
    }

    @Test
    fun deletarPorGuiaId_removeTodosOsItensDoGuia() = runBlocking {
        itemChecklistGuiaDao.inserir(ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item I"))
        itemChecklistGuiaDao.inserir(ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item J"))
        // Adiciona item de outro guia para garantir que não seja deletado
        val guia2 = GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "Outro", ano = 2024)
        val g2Id = guiaDeAprendizagemDao.inserir(guia2).toInt()
        itemChecklistGuiaDao.inserir(ItemChecklistGuia(guiaAprendizagemId = g2Id, descricaoItem = "Item K"))

        itemChecklistGuiaDao.deletarPorGuiaId(g1Id)

        assertTrue(itemChecklistGuiaDao.buscarPorGuiaId(g1Id).first().isEmpty())
        assertFalse(itemChecklistGuiaDao.buscarPorGuiaId(g2Id).first().isEmpty())
    }


    @Test
    fun onDeleteCascade_guiaDeletado_itensChecklistSaoDeletados() = runBlocking {
        val item = ItemChecklistGuia(guiaAprendizagemId = g1Id, descricaoItem = "Item L", concluido = false)
        val idItem = itemChecklistGuiaDao.inserir(item).toInt()

        val guiaParaDeletar = guiaDeAprendizagemDao.buscarPorId(g1Id)!!
        guiaDeAprendizagemDao.deletar(guiaParaDeletar) // CASCADE deve deletar o item

        assertNull(itemChecklistGuiaDao.buscarPorId(idItem))
    }
}
