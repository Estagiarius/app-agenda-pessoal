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
class SubtarefaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var tarefaDao: TarefaDao
    private lateinit var subtarefaDao: SubtarefaDao

    private var tarefaPaiId: Int = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        tarefaDao = db.tarefaDao()
        subtarefaDao = db.subtarefaDao()

        val tarefaPai = Tarefa(descricao = "Tarefa Pai para Subtarefas", prioridade = 1)
        tarefaPaiId = tarefaDao.inserir(tarefaPai).toInt()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirSubtarefa_eBuscarPorTarefaId_retornaSubtarefasCorretasEOrdenadas() = runBlocking {
        val sub1 = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub A", ordem = 1)
        val sub2 = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub B", ordem = 0)
        subtarefaDao.inserir(sub1)
        subtarefaDao.inserir(sub2)

        val subtarefas = subtarefaDao.buscarPorTarefaId(tarefaPaiId).first()
        assertEquals(2, subtarefas.size)
        assertEquals("Sub B", subtarefas[0].descricaoSubtarefa) // Ordem 0 primeiro
        assertEquals("Sub A", subtarefas[1].descricaoSubtarefa)
    }

    @Test
    fun atualizarSubtarefa_refleteMudancas() = runBlocking {
        val sub = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Original Sub", concluida = false)
        val id = subtarefaDao.inserir(sub).toInt()

        val paraAtualizar = subtarefaDao.buscarPorId(id)!!.copy(descricaoSubtarefa = "Atualizada Sub", concluida = true)
        subtarefaDao.atualizar(paraAtualizar)

        val atualizada = subtarefaDao.buscarPorId(id)!!
        assertEquals("Atualizada Sub", atualizada.descricaoSubtarefa)
        assertTrue(atualizada.concluida)
    }

    @Test
    fun deletarSubtarefa_naoDeveSerEncontrada() = runBlocking {
        val sub = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub para Deletar")
        val id = subtarefaDao.inserir(sub).toInt()
        val paraDeletar = subtarefaDao.buscarPorId(id)!!
        subtarefaDao.deletar(paraDeletar)
        assertNull(subtarefaDao.buscarPorId(id))
    }

    @Test
    fun deleteById_subtarefaNaoDeveSerEncontrada() = runBlocking {
        val sub = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub para Deletar por ID")
        val id = subtarefaDao.inserir(sub).toInt()
        subtarefaDao.deleteById(id)
        assertNull(subtarefaDao.buscarPorId(id))
    }


    @Test
    fun deleteTodasPorTarefaId_removeApenasSubtarefasDaTarefaCorreta() = runBlocking {
        // Tarefa 2 para isolamento
        val tarefaPai2 = Tarefa(descricao = "Tarefa Pai 2", prioridade = 0)
        val tarefaPai2Id = tarefaDao.inserir(tarefaPai2).toInt()

        subtarefaDao.inserir(Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub 1 T1"))
        subtarefaDao.inserir(Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub 2 T1"))
        subtarefaDao.inserir(Subtarefa(tarefaId = tarefaPai2Id, descricaoSubtarefa = "Sub 1 T2"))

        subtarefaDao.deleteTodasPorTarefaId(tarefaPaiId)

        assertTrue(subtarefaDao.buscarPorTarefaId(tarefaPaiId).first().isEmpty())
        assertFalse(subtarefaDao.buscarPorTarefaId(tarefaPai2Id).first().isEmpty())
        assertEquals(1, subtarefaDao.buscarPorTarefaId(tarefaPai2Id).first().size)
    }

    @Test
    fun onDeleteCascade_tarefaDeletada_subtarefasSaoDeletadas() = runBlocking {
        val sub1 = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub X")
        val sub2 = Subtarefa(tarefaId = tarefaPaiId, descricaoSubtarefa = "Sub Y")
        val idSub1 = subtarefaDao.inserir(sub1).toInt()
        val idSub2 = subtarefaDao.inserir(sub2).toInt()

        val tarefaParaDeletar = tarefaDao.buscarPorId(tarefaPaiId)!!
        tarefaDao.deletar(tarefaParaDeletar) // CASCADE deve deletar sub1 e sub2

        assertNull(subtarefaDao.buscarPorId(idSub1))
        assertNull(subtarefaDao.buscarPorId(idSub2))
        assertTrue(subtarefaDao.buscarPorTarefaId(tarefaPaiId).first().isEmpty())
    }
}
