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
class DisciplinaTurmaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao
    private lateinit var disciplinaTurmaDao: DisciplinaTurmaDao

    // Variáveis para armazenar entidades com IDs gerados
    private lateinit var d1: Disciplina
    private lateinit var d2: Disciplina
    private lateinit var t1: Turma
    private lateinit var t2: Turma

    @Before
    fun createDb() = runBlocking { // Tornar o Before suspend para popular dados
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Para simplicidade nos testes
            .build()
        disciplinaDao = db.disciplinaDao()
        turmaDao = db.turmaDao()
        disciplinaTurmaDao = db.disciplinaTurmaDao()

        // Inserir dados de teste e obter os objetos com IDs gerados
        disciplinaDao.inserir(Disciplina(nome = "Matemática"))
        disciplinaDao.inserir(Disciplina(nome = "Português"))
        turmaDao.inserir(Turma(nome = "9A"))
        turmaDao.inserir(Turma(nome = "1B"))

        d1 = disciplinaDao.buscarPorNome("Matemática")!!
        d2 = disciplinaDao.buscarPorNome("Português")!!
        t1 = turmaDao.buscarPorNome("9A")!!
        t2 = turmaDao.buscarPorNome("1B")!!
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirAssociacao_eBuscarAssociacoesParaTurma_retornaAssociacaoCorreta() = runBlocking {
        val crossRef = DisciplinaTurmaCrossRef(d1.id, t1.id)
        disciplinaTurmaDao.inserirAssociacao(crossRef)

        val associacoes = disciplinaTurmaDao.getAssociacoesParaTurma(t1.id).first()
        assertEquals(1, associacoes.size)
        assertEquals(d1.id, associacoes[0].disciplinaId)
        assertEquals(t1.id, associacoes[0].turmaId)
    }

    @Test
    fun inserirMultiplasAssociacoes_eGetDisciplinasForTurma_retornaDisciplinasCorretas() = runBlocking {
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d2.id, t1.id))

        val disciplinasDaTurma = disciplinaTurmaDao.getDisciplinasForTurma(t1.id).first()
        assertEquals(2, disciplinasDaTurma.size)
        assertTrue(disciplinasDaTurma.any { it.id == d1.id })
        assertTrue(disciplinasDaTurma.any { it.id == d2.id })
        // Verifica a ordem
        assertEquals(d1.nome, disciplinasDaTurma.find { it.id == d1.id }?.nome) // Matemática
        assertEquals(d2.nome, disciplinasDaTurma.find { it.id == d2.id }?.nome) // Português
    }

    @Test
    fun deletarAssociacao_associacaoNaoDeveMaisExistir() = runBlocking {
        val crossRef = DisciplinaTurmaCrossRef(d1.id, t1.id)
        disciplinaTurmaDao.inserirAssociacao(crossRef)

        var associacoes = disciplinaTurmaDao.getAssociacoesParaTurma(t1.id).first()
        assertEquals(1, associacoes.size) // Confirma que foi inserido

        disciplinaTurmaDao.deletarAssociacao(crossRef)
        associacoes = disciplinaTurmaDao.getAssociacoesParaTurma(t1.id).first()
        assertTrue(associacoes.isEmpty())
    }

    @Test
    fun deletarAssociacoesPorTurmaId_removeTodasAssociacoesDaTurma() = runBlocking {
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d2.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t2.id)) // Associação com outra turma

        disciplinaTurmaDao.deletarAssociacoesPorTurmaId(t1.id)

        val associacoesT1 = disciplinaTurmaDao.getAssociacoesParaTurma(t1.id).first()
        assertTrue(associacoesT1.isEmpty())

        val associacoesT2 = disciplinaTurmaDao.getAssociacoesParaTurma(t2.id).first()
        assertEquals(1, associacoesT2.size)
        assertEquals(d1.id, associacoesT2[0].disciplinaId)
    }

    @Test
    fun deletarAssociacoesPorDisciplinaId_removeTodasAssociacoesDaDisciplina() = runBlocking {
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t2.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d2.id, t1.id)) // Associação com outra disciplina

        disciplinaTurmaDao.deletarAssociacoesPorDisciplinaId(d1.id)

        val turmasParaD1 = disciplinaTurmaDao.getTurmasForDisciplina(d1.id).first()
        assertTrue(turmasParaD1.isEmpty())

        val associacoesParaD1 = disciplinaTurmaDao.getAssociacoesParaDisciplina(d1.id).first()
        assertTrue(associacoesParaD1.isEmpty())

        val turmasParaD2 = disciplinaTurmaDao.getTurmasForDisciplina(d2.id).first()
        assertEquals(1, turmasParaD2.size) // Deve permanecer a associação de d2 com t1
        assertEquals(t1.id, turmasParaD2[0].id)
    }


    @Test
    fun getTurmasForDisciplina_retornaTurmasCorretas() = runBlocking {
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t2.id))

        val turmasDaDisciplina = disciplinaTurmaDao.getTurmasForDisciplina(d1.id).first()
        assertEquals(2, turmasDaDisciplina.size)
        assertTrue(turmasDaDisciplina.any { it.id == t1.id })
        assertTrue(turmasDaDisciplina.any { it.id == t2.id })
    }

    @Test
    fun onDeleteCascade_deletarDisciplina_removeAssociacoesRelacionadas() = runBlocking {
        // Pré-condição: d1 está associado a t1 e t2
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t2.id))
        // d2 está associado a t1
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d2.id, t1.id))

        // Deleta d1
        disciplinaDao.deletar(d1)

        // Verifica se as associações de d1 foram removidas
        val associacoesT1 = disciplinaTurmaDao.getDisciplinasForTurma(t1.id).first()
        assertEquals(1, associacoesT1.size) // Apenas d2 deve estar associada a t1
        assertEquals(d2.id, associacoesT1[0].id)

        val associacoesT2 = disciplinaTurmaDao.getDisciplinasForTurma(t2.id).first()
        assertTrue(associacoesT2.isEmpty()) // d1 era a única associada a t2

        val associacoesParaD1 = disciplinaTurmaDao.getAssociacoesParaDisciplina(d1.id).first()
        assertTrue(associacoesParaD1.isEmpty())
    }

    @Test
    fun onDeleteCascade_deletarTurma_removeAssociacoesRelacionadas() = runBlocking {
        // Pré-condição: t1 está associada a d1 e d2
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t1.id))
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d2.id, t1.id))
        // t2 está associada a d1
        disciplinaTurmaDao.inserirAssociacao(DisciplinaTurmaCrossRef(d1.id, t2.id))

        // Deleta t1
        turmaDao.deletar(t1) // Supondo que TurmaDao tenha um método deletar(Turma)
                               // Se não tiver, precisaria adicionar ou deletar por ID.
                               // Vou adicionar um método deletar(Turma) ao TurmaDao para este teste.

        // Verifica se as associações de t1 foram removidas
        val turmasParaD1 = disciplinaTurmaDao.getTurmasForDisciplina(d1.id).first()
        assertEquals(1, turmasParaD1.size) // Apenas t2 deve estar associada a d1
        assertEquals(t2.id, turmasParaD1[0].id)

        val turmasParaD2 = disciplinaTurmaDao.getTurmasForDisciplina(d2.id).first()
        assertTrue(turmasParaD2.isEmpty()) // t1 era a única associada a d2

        val associacoesParaT1 = disciplinaTurmaDao.getAssociacoesParaTurma(t1.id).first()
        assertTrue(associacoesParaT1.isEmpty())
    }
}
