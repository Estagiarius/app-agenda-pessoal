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
class DisciplinaDaoTest {

    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Permitir queries na thread principal para testes
            .build()
        disciplinaDao = db.disciplinaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun inserirDisciplina_eBuscarTodas_retornaListaComDisciplinasCorretas() = runBlocking {
        val d1 = Disciplina(nome = "Matemática", cor = Color.RED)
        val d2 = Disciplina(nome = "Português", cor = Color.BLUE)
        disciplinaDao.inserir(d1)
        disciplinaDao.inserir(d2)

        val todasDisciplinas = disciplinaDao.buscarTodas().first()
        assertEquals(2, todasDisciplinas.size)
        // buscarTodas() ordena por nome ASC
        assertEquals("Matemática", todasDisciplinas[0].nome)
        assertEquals(Color.RED, todasDisciplinas[0].cor)
        assertEquals("Português", todasDisciplinas[1].nome)
        assertEquals(Color.BLUE, todasDisciplinas[1].cor)
    }

    @Test
    @Throws(Exception::class)
    fun inserirDisciplinaComCor_eBuscarPorNome_retornaDisciplinaComCorCorreta() = runBlocking {
        val nomeDisciplina = "História"
        val corDisciplina = Color.GREEN
        val disciplina = Disciplina(nome = nomeDisciplina, cor = corDisciplina)
        disciplinaDao.inserir(disciplina)

        val encontrada = disciplinaDao.buscarPorNome(nomeDisciplina)
        assertNotNull(encontrada)
        assertEquals(nomeDisciplina, encontrada?.nome)
        assertEquals(corDisciplina, encontrada?.cor)
    }

    @Test
    @Throws(Exception::class)
    fun buscarPorNome_disciplinaNaoExistente_retornaNull() = runBlocking {
        val encontrada = disciplinaDao.buscarPorNome("Geografia")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirDisciplinaDuplicadaComNomeDiferenteCor_OnConflictIgnoreAplicaSePKConflitar() = runBlocking {
        // Este teste verifica o comportamento de OnConflictStrategy.IGNORE na PK.
        // Como o nome não tem unique index, duas disciplinas com mesmo nome mas cores (e IDs) diferentes serão inseridas.
        val disciplina1 = Disciplina(nome = "Física", cor = Color.YELLOW)
        disciplinaDao.inserir(disciplina1) // Room gera ID 1

        val disciplina2 = Disciplina(nome = "Física", cor = Color.CYAN)
        disciplinaDao.inserir(disciplina2) // Room gera ID 2

        val todasDisciplinas = disciplinaDao.buscarTodas().first().filter { it.nome == "Física" }
        assertEquals(2, todasDisciplinas.size)
    }


    @Test
    @Throws(Exception::class)
    fun deletarDisciplina_aposInsercao_buscarPorNomeRetornaNull() = runBlocking {
        val disciplina = Disciplina(nome = "Química", cor = Color.MAGENTA)
        disciplinaDao.inserir(disciplina)

        // Para deletar, precisamos do objeto com o ID correto.
        // Como o ID é auto-gerado, buscamos a disciplina inserida primeiro.
        val disciplinaInserida = disciplinaDao.buscarPorNome("Química")
        assertNotNull(disciplinaInserida)

        disciplinaDao.deletar(disciplinaInserida!!)
        val encontrada = disciplinaDao.buscarPorNome("Química")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirMultiplasDisciplinas_buscarTodas_retornaTodasOrdenadasPorNome() = runBlocking {
        disciplinaDao.inserir(Disciplina(nome = "Programação", cor = Color.BLACK))
        disciplinaDao.inserir(Disciplina(nome = "Algoritmos", cor = Color.DKGRAY))
        disciplinaDao.inserir(Disciplina(nome = "Banco de Dados", cor = Color.WHITE))

        val todas = disciplinaDao.buscarTodas().first()
        assertEquals(3, todas.size)
        assertEquals("Algoritmos", todas[0].nome)
        assertEquals(Color.DKGRAY, todas[0].cor)
        assertEquals("Banco de Dados", todas[1].nome)
        assertEquals(Color.WHITE, todas[1].cor)
        assertEquals("Programação", todas[2].nome)
        assertEquals(Color.BLACK, todas[2].cor)
    }
}
