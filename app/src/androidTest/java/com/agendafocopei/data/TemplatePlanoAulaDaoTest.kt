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
class TemplatePlanoAulaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var templatePlanoAulaDao: TemplatePlanoAulaDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        templatePlanoAulaDao = db.templatePlanoAulaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirTemplate_eBuscarPorId_retornaTemplateCorreto() = runBlocking {
        val template = TemplatePlanoAula(nomeTemplate = "Padrão Expositivo", campoHabilidades = "H1, H2", campoRecursos = "Livro, Lousa", campoMetodologia = "Aula expositiva", campoAvaliacao = "Prova", outrosCampos = null)
        val id = templatePlanoAulaDao.inserir(template)
        val buscado = templatePlanoAulaDao.buscarPorId(id.toInt())
        assertNotNull(buscado)
        assertEquals("Padrão Expositivo", buscado?.nomeTemplate)
        assertEquals("H1, H2", buscado?.campoHabilidades)
    }

    @Test
    fun buscarTodos_retornaListaOrdenadaPorNome() = runBlocking {
        templatePlanoAulaDao.inserir(TemplatePlanoAula(nomeTemplate = "Template C", null,null,null,null,null))
        templatePlanoAulaDao.inserir(TemplatePlanoAula(nomeTemplate = "Template A", null,null,null,null,null))
        templatePlanoAulaDao.inserir(TemplatePlanoAula(nomeTemplate = "Template B", null,null,null,null,null))

        val todos = templatePlanoAulaDao.buscarTodos().first()
        assertEquals(3, todos.size)
        assertEquals("Template A", todos[0].nomeTemplate)
        assertEquals("Template B", todos[1].nomeTemplate)
        assertEquals("Template C", todos[2].nomeTemplate)
    }

    @Test
    fun atualizarTemplate_refleteMudancas() = runBlocking {
        val template = TemplatePlanoAula(nomeTemplate = "Original", campoMetodologia = "Antiga", null,null,null,null)
        val id = templatePlanoAulaDao.inserir(template)
        val paraAtualizar = templatePlanoAulaDao.buscarPorId(id.toInt())!!.copy(campoMetodologia = "Nova Metodologia")
        templatePlanoAulaDao.atualizar(paraAtualizar)
        val buscado = templatePlanoAulaDao.buscarPorId(id.toInt())
        assertEquals("Nova Metodologia", buscado?.campoMetodologia)
    }

    @Test
    fun deletarTemplate_naoDeveSerEncontrado() = runBlocking {
        val template = TemplatePlanoAula(nomeTemplate = "Para Deletar", null,null,null,null,null)
        val id = templatePlanoAulaDao.inserir(template)
        val paraDeletar = templatePlanoAulaDao.buscarPorId(id.toInt())!!
        templatePlanoAulaDao.deletar(paraDeletar)
        assertNull(templatePlanoAulaDao.buscarPorId(id.toInt()))
    }
}
