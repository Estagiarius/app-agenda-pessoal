package com.agendafocopei.data

import android.content.Context
import android.graphics.Color
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
class GuiaDeAprendizagemDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var guiaDeAprendizagemDao: GuiaDeAprendizagemDao

    private var d1Id: Int = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        disciplinaDao = db.disciplinaDao()
        guiaDeAprendizagemDao = db.guiaDeAprendizagemDao()

        val disc1 = Disciplina(nome = "Português Guia", cor = Color.BLUE)
        disciplinaDao.inserir(disc1)
        d1Id = disciplinaDao.buscarPorNome("Português Guia")!!.id
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirGuia_eBuscarPorId_retornaGuiaCorreto() = runBlocking {
        val guia = GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "1º Bimestre", ano = 2024, tituloGuia = "Guia Inicial", caminhoAnexoGuia = null, tipoAnexoGuia = null)
        val id = guiaDeAprendizagemDao.inserir(guia)
        val buscado = guiaDeAprendizagemDao.buscarPorId(id.toInt())
        assertNotNull(buscado)
        assertEquals("Guia Inicial", buscado?.tituloGuia)
        assertEquals(d1Id, buscado?.disciplinaId)
    }

    @Test
    fun buscarTodosParaDisplay_retornaGuiasComNomeDisciplina() = runBlocking {
        guiaDeAprendizagemDao.inserir(GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "1º Bimestre", ano = 2024, tituloGuia = "Guia 1", null, null))
        guiaDeAprendizagemDao.inserir(GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "2º Bimestre", ano = 2024, tituloGuia = "Guia 2", null, null))

        val todosDisplay = guiaDeAprendizagemDao.buscarTodosParaDisplay().first()
        assertEquals(2, todosDisplay.size)
        assertEquals("Guia 2", todosDisplay[0].guiaDeAprendizagem.tituloGuia) // Ordenado por ano DESC, bimestre DESC
        assertEquals("Português Guia", todosDisplay[0].nomeDisciplina)
        assertEquals(Color.BLUE, todosDisplay[0].corDisciplina)
    }

    @Test
    fun atualizarGuia_refleteMudancas() = runBlocking {
        val guia = GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "3º Bimestre", ano = 2023, tituloGuia = "Antigo", null, null)
        val id = guiaDeAprendizagemDao.inserir(guia)
        val guiaParaAtualizar = guiaDeAprendizagemDao.buscarPorId(id.toInt())!!.copy(tituloGuia = "Novo Título")
        guiaDeAprendizagemDao.atualizar(guiaParaAtualizar)
        val buscado = guiaDeAprendizagemDao.buscarPorId(id.toInt())
        assertEquals("Novo Título", buscado?.tituloGuia)
    }

    @Test
    fun deletarGuia_naoDeveSerEncontrado() = runBlocking {
        val guia = GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "4º Bimestre", ano = 2023, tituloGuia = "Para Deletar", null, null)
        val id = guiaDeAprendizagemDao.inserir(guia)
        val guiaParaDeletar = guiaDeAprendizagemDao.buscarPorId(id.toInt())!!
        guiaDeAprendizagemDao.deletar(guiaParaDeletar)
        assertNull(guiaDeAprendizagemDao.buscarPorId(id.toInt()))
    }

    @Test
    fun onDeleteCascade_disciplinaDeletada_guiasSaoDeletados() = runBlocking {
        val guia = GuiaDeAprendizagem(disciplinaId = d1Id, bimestre = "1º Bimestre", ano = 2025, tituloGuia = "Guia D1", null, null)
        val idGuia = guiaDeAprendizagemDao.inserir(guia)
        val disciplinaParaDeletar = disciplinaDao.buscarPorId(d1Id)!!
        disciplinaDao.deletar(disciplinaParaDeletar)
        assertNull(guiaDeAprendizagemDao.buscarPorId(idGuia.toInt()))
    }
}
