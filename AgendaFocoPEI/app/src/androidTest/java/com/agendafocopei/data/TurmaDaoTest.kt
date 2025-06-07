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
    fun inserirTurma_eBuscarTodas_retornaListaComTurmaInserida() = runBlocking {
        val turma = Turma(nome = "Turma A1")
        turmaDao.inserir(turma)

        val todasTurmas = turmaDao.buscarTodas().first()
        assertEquals(1, todasTurmas.size)
        assertTrue(todasTurmas.any { it.nome == "Turma A1" })
    }

    @Test
    @Throws(Exception::class)
    fun inserirTurma_eBuscarPorNome_retornaTurmaCorreta() = runBlocking {
        val turma = Turma(nome = "Turma B2")
        turmaDao.inserir(turma)

        val encontrada = turmaDao.buscarPorNome("Turma B2")
        assertNotNull(encontrada)
        assertEquals("Turma B2", encontrada?.nome)
    }

    @Test
    @Throws(Exception::class)
    fun buscarPorNome_turmaNaoExistente_retornaNull() = runBlocking {
        val encontrada = turmaDao.buscarPorNome("Turma C3")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirTurmaDuplicada_comOnConflictIgnore_comportamentoSemUniqueIndexNoNome() = runBlocking {
        // Similar ao DisciplinaDaoTest, sem unique index no nome, OnConflictStrategy.IGNORE
        // na PK (auto-gerada) permitirá inserções de turmas com mesmo nome mas IDs diferentes.
        val turma1 = Turma(nome = "Turma D4")
        turmaDao.inserir(turma1)

        val turmaComMesmoNome = Turma(nome = "Turma D4")
        turmaDao.inserir(turmaComMesmoNome)

        val todasTurmas = turmaDao.buscarTodas().first()
        assertEquals(2, todasTurmas.size)
    }

    // Não foi solicitado um método 'deletar' para TurmaDao na tarefa original,
    // então não há teste para deleção aqui. Se fosse adicionado, o teste seria similar
    // ao de DisciplinaDaoTest.

    @Test
    @Throws(Exception::class)
    fun inserirMultiplasTurmas_buscarTodas_retornaTodasOrdenadas() = runBlocking {
        turmaDao.inserir(Turma(nome = "Manhã"))
        turmaDao.inserir(Turma(nome = "Tarde"))
        turmaDao.inserir(Turma(nome = "Noite"))

        // Re-inserindo para garantir que a ordem é do BD e não da inserção
        // e para ter mais itens para verificar a ordenação
        turmaDao.inserir(Turma(nome = "Integral"))
        turmaDao.inserir(Turma(nome = "Comercial"))


        val todas = turmaDao.buscarTodas().first()
        // Esperamos 5 turmas, pois todas têm nomes diferentes ou são inseridas como objetos distintos
        // (e portanto, com IDs diferentes, mesmo que tivessem o mesmo nome)
        assertEquals(5, todas.size)
        assertEquals("Comercial", todas[0].nome) // Ordem Alfabética
        assertEquals("Integral", todas[1].nome)
        assertEquals("Manhã", todas[2].nome)
        assertEquals("Noite", todas[3].nome)
        assertEquals("Tarde", todas[4].nome)
    }
}
