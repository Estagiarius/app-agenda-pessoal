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
class DisciplinaDaoTest {

    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Permitir queries na thread principal para testes (não recomendado em produção)
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
    fun inserirDisciplina_eBuscarTodas_retornaListaComDisciplinaInserida() = runBlocking {
        val disciplina = Disciplina(nome = "Matemática")
        disciplinaDao.inserir(disciplina)

        val todasDisciplinas = disciplinaDao.buscarTodas().first() // Coleta o primeiro item do Flow
        assertEquals(1, todasDisciplinas.size)
        assertTrue(todasDisciplinas.any { it.nome == "Matemática" })
    }

    @Test
    @Throws(Exception::class)
    fun inserirDisciplina_eBuscarPorNome_retornaDisciplinaCorreta() = runBlocking {
        val disciplina = Disciplina(nome = "História")
        disciplinaDao.inserir(disciplina)

        val encontrada = disciplinaDao.buscarPorNome("História")
        assertNotNull(encontrada)
        assertEquals("História", encontrada?.nome)
    }

    @Test
    @Throws(Exception::class)
    fun buscarPorNome_disciplinaNaoExistente_retornaNull() = runBlocking {
        val encontrada = disciplinaDao.buscarPorNome("Geografia")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirDisciplinaDuplicada_comOnConflictIgnore_naoDeveLancarErroEContagemNaoAumentaExcessivamente() = runBlocking {
        val disciplina1 = Disciplina(nome = "Física") // Room irá gerar ID 1 (ou outro)
        disciplinaDao.inserir(disciplina1)

        // Tenta inserir com o mesmo nome, mas Room geraria novo ID se não houvesse constraint unique no nome.
        // Como OnConflictStrategy.IGNORE é para a PK, e o nome não é PK nem unique, ele insere duas.
        // Para testar IGNORE no nome, precisaríamos de @Entity(tableName = "disciplinas", indices = [Index(value = ["nome_disciplina"], unique = true)])
        // Por agora, o teste verifica que a segunda inserção (com PK diferente) ocorre.
        // Se a entidade Disciplina tivesse id fixo:
        // val disciplina2 = Disciplina(id=1, nome = "Física Avançada") -> IGNORE funcionaria na PK
        // disciplinaDao.inserir(disciplina2)
        // val todasDisciplinas = disciplinaDao.buscarTodas().first()
        // assertEquals(1, todasDisciplinas.size)
        // assertEquals("Física", todasDisciplinas[0].nome) // A primeira inserção é mantida

        // Cenário atual: OnConflictStrategy.IGNORE na Primary Key (id auto-gerado)
        // Se tentarmos inserir um objeto Disciplina com um ID que já existe, ele será ignorado.
        // Isso é mais difícil de testar diretamente com id auto-gerado sem antes buscar o id.

        // Testando o comportamento atual sem unique constraint no nome:
        val disciplinaComMesmoNome = Disciplina(nome = "Física") // Novo objeto, novo ID será gerado
        disciplinaDao.inserir(disciplinaComMesmoNome)

        val todasDisciplinas = disciplinaDao.buscarTodas().first()
        assertEquals(2, todasDisciplinas.size) // Espera-se duas, pois os IDs são diferentes
    }


    @Test
    @Throws(Exception::class)
    fun deletarDisciplina_aposInsercao_buscarPorNomeRetornaNull() = runBlocking {
        val disciplina = Disciplina(nome = "Química")
        disciplinaDao.inserir(disciplina)

        // Precisamos do objeto com o ID correto para deletar
        val disciplinaInserida = disciplinaDao.buscarPorNome("Química")
        assertNotNull(disciplinaInserida)

        disciplinaDao.deletar(disciplinaInserida!!)
        val encontrada = disciplinaDao.buscarPorNome("Química")
        assertNull(encontrada)
    }

    @Test
    @Throws(Exception::class)
    fun inserirMultiplasDisciplinas_buscarTodas_retornaTodasOrdenadas() = runBlocking {
        disciplinaDao.inserir(Disciplina(nome = "Programação"))
        disciplinaDao.inserir(Disciplina(nome = "Algoritmos"))
        disciplinaDao.inserir(Disciplina(nome = "Banco de Dados"))

        val todas = disciplinaDao.buscarTodas().first()
        assertEquals(3, todas.size)
        assertEquals("Algoritmos", todas[0].nome)
        assertEquals("Banco de Dados", todas[1].nome)
        assertEquals("Programação", todas[2].nome)
    }
}
