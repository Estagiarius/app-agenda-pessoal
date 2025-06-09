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
class AnotacaoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var anotacaoDao: AnotacaoDao
    private lateinit var turmaDao: TurmaDao

    private var t1Id: Int = 0
    private var t2Id: Int = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        anotacaoDao = db.anotacaoDao()
        turmaDao = db.turmaDao()

        val turma1 = Turma(nome = "Anot Turma A", cor = Color.CYAN)
        val turma2 = Turma(nome = "Anot Turma B", cor = Color.MAGENTA)
        t1Id = turmaDao.inserir(turma1).toInt()
        t2Id = turmaDao.inserir(turma2).toInt()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirAnotacao_eBuscarPorId_retornaAnotacaoCorreta() = runBlocking {
        val currentTime = System.currentTimeMillis()
        val anotacao = Anotacao(
            conteudo = "Conteúdo da anotação 1",
            dataCriacao = currentTime,
            dataModificacao = currentTime,
            cor = Color.YELLOW,
            turmaId = t1Id,
            alunoNome = "Aluno Teste",
            tagsString = "#teste #importante"
        )
        val id = anotacaoDao.inserir(anotacao)
        val buscada = anotacaoDao.buscarPorId(id.toInt())
        assertNotNull(buscada)
        assertEquals("Conteúdo da anotação 1", buscada?.conteudo)
        assertEquals(currentTime, buscada?.dataCriacao)
        assertEquals(Color.YELLOW, buscada?.cor)
        assertEquals(t1Id, buscada?.turmaId)
        assertEquals("Aluno Teste", buscada?.alunoNome)
        assertEquals("#teste #importante", buscada?.tagsString)
    }

    @Test
    fun buscarTodas_retornaListaOrdenadaPorDataModificacao() = runBlocking {
        val now = System.currentTimeMillis()
        anotacaoDao.inserir(Anotacao(conteudo = "Anot 2", dataModificacao = now - 1000, turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "Anot 1", dataModificacao = now, turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "Anot 3", dataModificacao = now - 500, turmaId = null))

        val todas = anotacaoDao.buscarTodas().first()
        assertEquals(3, todas.size)
        assertEquals("Anot 1", todas[0].conteudo) // Mais recente
        assertEquals("Anot 3", todas[1].conteudo)
        assertEquals("Anot 2", todas[2].conteudo)
    }

    @Test
    fun buscarPorTurmaId_retornaApenasAnotacoesDaTurmaCorreta() = runBlocking {
        anotacaoDao.inserir(Anotacao(conteudo = "A1 T1", turmaId = t1Id))
        anotacaoDao.inserir(Anotacao(conteudo = "A2 T2", turmaId = t2Id))
        anotacaoDao.inserir(Anotacao(conteudo = "A3 T1", turmaId = t1Id))

        val anotacoesT1 = anotacaoDao.buscarPorTurmaId(t1Id).first()
        assertEquals(2, anotacoesT1.size)
        assertTrue(anotacoesT1.all { it.turmaId == t1Id })

        val anotacoesT2 = anotacaoDao.buscarPorTurmaId(t2Id).first()
        assertEquals(1, anotacoesT2.size)
        assertEquals("A2 T2", anotacoesT2[0].conteudo)
    }

    @Test
    fun buscarPorTag_encontraAnotacoesComTagCorreta() = runBlocking {
        anotacaoDao.inserir(Anotacao(conteudo = "C1", tagsString = "#ideia #projeto", turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "C2", tagsString = "#lembrete", turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "C3", tagsString = "#projeto #final", turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "C4", tagsString = "sem tag aqui", turmaId = null))

        var porTag = anotacaoDao.buscarPorTag("%#projeto%").first()
        assertEquals(2, porTag.size)
        assertTrue(porTag.any { it.conteudo == "C1" })
        assertTrue(porTag.any { it.conteudo == "C3" })

        porTag = anotacaoDao.buscarPorTag("%#ideia%").first()
        assertEquals(1, porTag.size)
        assertEquals("C1", porTag[0].conteudo)

        porTag = anotacaoDao.buscarPorTag("%#final%").first()
        assertEquals(1, porTag.size)
        assertEquals("C3", porTag[0].conteudo)
    }

    @Test
    fun buscarPorTag_naoEncontraAnotacoesSemTagOuComTagDiferente() = runBlocking {
        anotacaoDao.inserir(Anotacao(conteudo = "C1", tagsString = "#ideia", turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "C2", tagsString = null, turmaId = null))

        val porTag = anotacaoDao.buscarPorTag("%#naoexiste%").first()
        assertTrue(porTag.isEmpty())
    }

    @Test
    fun buscarPorConteudo_encontraAnotacoesComTextoCorreto() = runBlocking {
        anotacaoDao.inserir(Anotacao(conteudo = "Este é um teste de conteúdo.", turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "Outro conteúdo para teste.", turmaId = null))
        anotacaoDao.inserir(Anotacao(conteudo = "Nada a ver.", turmaId = null))

        val porConteudo = anotacaoDao.buscarPorConteudo("%teste%").first()
        assertEquals(2, porConteudo.size)
        assertTrue(porConteudo.any { it.conteudo.contains("Este é um teste")})
        assertTrue(porConteudo.any { it.conteudo.contains("Outro conteúdo para teste")})
    }


    @Test
    fun atualizarAnotacao_refleteMudancas() = runBlocking {
        val anot = Anotacao(conteudo = "Original", cor = Color.RED, tagsString = "#old", turmaId = null)
        val id = anotacaoDao.inserir(anot).toInt()

        val paraAtualizar = anotacaoDao.buscarPorId(id)!!.copy(
            conteudo = "Atualizado",
            cor = Color.BLUE,
            tagsString = "#new",
            dataModificacao = System.currentTimeMillis() + 1000 // Garante que a data de modificação mude
        )
        anotacaoDao.atualizar(paraAtualizar)

        val atualizada = anotacaoDao.buscarPorId(id)!!
        assertEquals("Atualizado", atualizada.conteudo)
        assertEquals(Color.BLUE, atualizada.cor)
        assertEquals("#new", atualizada.tagsString)
        assertTrue(atualizada.dataModificacao > anot.dataModificacao)
    }

    @Test
    fun deletarAnotacao_naoDeveSerEncontrada() = runBlocking {
        val anot = Anotacao(conteudo = "Para Deletar", turmaId = null)
        val id = anotacaoDao.inserir(anot).toInt()
        val paraDeletar = anotacaoDao.buscarPorId(id)!!
        anotacaoDao.deletar(paraDeletar)
        assertNull(anotacaoDao.buscarPorId(id))
    }

    @Test
    fun deleteById_anotacaoNaoDeveSerEncontrada() = runBlocking {
        val anot = Anotacao(conteudo = "Para Deletar por ID", turmaId = null)
        val id = anotacaoDao.inserir(anot).toInt()
        anotacaoDao.deleteById(id)
        assertNull(anotacaoDao.buscarPorId(id))
    }


    @Test
    fun onDeleteSetNull_turmaDeletada_anotacaoTurmaIdEhNull() = runBlocking {
        val anot = Anotacao(conteudo = "Anotação com Turma", turmaId = t1Id)
        val id = anotacaoDao.inserir(anot).toInt()

        val turmaParaDeletar = turmaDao.buscarPorId(t1Id)!!
        turmaDao.deletar(turmaParaDeletar) // onDelete = SET_NULL

        val anotacaoAtualizada = anotacaoDao.buscarPorId(id)
        assertNotNull(anotacaoAtualizada)
        assertNull(anotacaoAtualizada?.turmaId)
    }
}
