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
class TurmaDaoTest {

    private lateinit var turmaDao: TurmaDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        turmaDao = db.turmaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun inserirTurma_eBuscarTodas_retornaListaComTurmasCorretas() = runBlocking {
        val t1 = Turma(nome = "Turma A1", cor = Color.RED)
        val t2 = Turma(nome = "Turma B2", cor = Color.BLUE)
        turmaDao.inserir(t1)
        turmaDao.inserir(t2)

        val todasTurmas = turmaDao.buscarTodas().first()
        assertEquals(2, todasTurmas.size)
        // buscarTodas() ordena por nome ASC
        assertEquals("Turma A1", todasTurmas[0].nome)
        assertEquals(Color.RED, todasTurmas[0].cor)
        assertEquals("Turma B2", todasTurmas[1].nome)
        assertEquals(Color.BLUE, todasTurmas[1].cor)
    }

    @Test
    @Throws(Exception::class)
    fun inserirTurmaComCor_eBuscarPorNome_retornaTurmaComCorCorreta() = runBlocking {
        val nomeTurma = "3C"
        val corTurma = Color.BLUE
        val turma = Turma(nome = nomeTurma, cor = corTurma)
        turmaDao.inserir(turma)

        val turmaRecuperada = turmaDao.buscarPorNome(nomeTurma)
        assertNotNull(turmaRecuperada)
        assertEquals(nomeTurma, turmaRecuperada?.nome)
        assertEquals(corTurma, turmaRecuperada?.cor)
    }

    @Test
    @Throws(Exception::class)
    fun buscarPorNome_turmaNaoExistente_retornaNull() = runBlocking {
        val encontrada = turmaDao.buscarPorNome("Turma C3")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirTurmaDuplicadaComNomeDiferenteCor_OnConflictIgnoreAplicaSePKConflitar() = runBlocking {
        val turma1 = Turma(nome = "Turma D4", cor = Color.YELLOW)
        turmaDao.inserir(turma1)

        val turmaComMesmoNome = Turma(nome = "Turma D4", cor = Color.CYAN)
        turmaDao.inserir(turmaComMesmoNome)

        val todasTurmas = turmaDao.buscarTodas().first().filter { it.nome == "Turma D4" }
        assertEquals(2, todasTurmas.size)
    }

    @Test
    @Throws(Exception::class)
    fun deletarTurma_aposInsercao_buscarPorNomeRetornaNull() = runBlocking {
        val turma = Turma(nome = "Turma E5", cor = Color.GREEN)
        turmaDao.inserir(turma)

        val turmaInserida = turmaDao.buscarPorNome("Turma E5")
        assertNotNull(turmaInserida)

        turmaDao.deletar(turmaInserida!!)
        val encontrada = turmaDao.buscarPorNome("Turma E5")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirMultiplasTurmas_buscarTodas_retornaTodasOrdenadasPorNome() = runBlocking {
        turmaDao.inserir(Turma(nome = "Manhã", cor = Color.YELLOW))
        turmaDao.inserir(Turma(nome = "Tarde", cor = Color.rgb(255, 165, 0))) // Orange
        turmaDao.inserir(Turma(nome = "Noite", cor = Color.BLUE))
        turmaDao.inserir(Turma(nome = "Integral", cor = Color.GREEN))
        turmaDao.inserir(Turma(nome = "Comercial", cor = Color.GRAY))

        val todas = turmaDao.buscarTodas().first()
        assertEquals(5, todas.size)
        assertEquals("Comercial", todas[0].nome)
        assertEquals(Color.GRAY, todas[0].cor)
        assertEquals("Integral", todas[1].nome)
        assertEquals(Color.GREEN, todas[1].cor)
        assertEquals("Manhã", todas[2].nome)
        assertEquals(Color.YELLOW, todas[2].cor)
        assertEquals("Noite", todas[3].nome)
        assertEquals(Color.BLUE, todas[3].cor)
        assertEquals("Tarde", todas[4].nome)
        assertEquals(Color.rgb(255, 165, 0), todas[4].cor)
    }
}
