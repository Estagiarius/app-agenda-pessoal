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
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class TarefaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var tarefaDao: TarefaDao
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao

    private var d1Id: Int = 0
    private var t1Id: Int = 0
    private val queryDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        tarefaDao = db.tarefaDao()
        disciplinaDao = db.disciplinaDao()
        turmaDao = db.turmaDao()

        val disc1 = Disciplina(nome = "Tarefa Disc", cor = Color.GREEN)
        disciplinaDao.inserir(disc1)
        d1Id = disciplinaDao.buscarPorNome("Tarefa Disc")!!.id

        val turma1 = Turma(nome = "Tarefa Turma", cor = Color.BLUE)
        turmaDao.inserir(turma1)
        t1Id = turmaDao.buscarPorNome("Tarefa Turma")!!.id
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirTarefa_eBuscarPorId_retornaTarefaCorreta() = runBlocking {
        val prazoCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 5); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 30) }
        val lembreteTime = prazoCal.timeInMillis - (60 * 60 * 1000) // 1 hora antes
        val tarefa = Tarefa(
            descricao = "Tarefa Teste 1",
            prazoData = queryDateFormat.format(prazoCal.time),
            prazoHora = "10:30",
            prioridade = 2, // Alta
            disciplinaId = d1Id,
            turmaId = t1Id,
            concluida = false,
            lembreteConfigurado = true,
            lembreteDateTime = lembreteTime
        )
        val id = tarefaDao.inserir(tarefa)
        val buscada = tarefaDao.buscarPorId(id.toInt())
        assertNotNull(buscada)
        assertEquals("Tarefa Teste 1", buscada?.descricao)
        assertEquals(queryDateFormat.format(prazoCal.time), buscada?.prazoData)
        assertEquals("10:30", buscada?.prazoHora)
        assertEquals(2, buscada?.prioridade)
        assertEquals(d1Id, buscada?.disciplinaId)
        assertEquals(t1Id, buscada?.turmaId)
        assertFalse(buscada!!.concluida)
        assertTrue(buscada.lembreteConfigurado)
        assertEquals(lembreteTime, buscada.lembreteDateTime)
    }

    @Test
    fun buscarPendentesOrdenadas_retornaApenasPendentesEOrdenadas() = runBlocking {
        val hojeCal = Calendar.getInstance()
        val amanhaCal = Calendar.getInstance().apply{ add(Calendar.DAY_OF_YEAR, 1)}

        tarefaDao.inserir(Tarefa(descricao = "Pendente Alta Prio Hoje", prazoData = queryDateFormat.format(hojeCal.time), prazoHora = "10:00", prioridade = 2, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Pendente Media Prio Amanha", prazoData = queryDateFormat.format(amanhaCal.time), prazoHora = "11:00", prioridade = 1, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Pendente Baixa Prio Hoje", prazoData = queryDateFormat.format(hojeCal.time), prazoHora = "12:00", prioridade = 0, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Concluída", prazoData = null, prazoHora = null, prioridade = 2, concluida = true, disciplinaId = null, turmaId = null))

        val pendentes = tarefaDao.buscarPendentesOrdenadas().first()
        assertEquals(3, pendentes.size)
        assertEquals("Pendente Alta Prio Hoje", pendentes[0].descricao) // Alta prioridade primeiro
        assertEquals("Pendente Media Prio Amanha", pendentes[1].descricao) // Depois por prazo (Amanha > Hoje, mas prioridade manda)
        assertEquals("Pendente Baixa Prio Hoje", pendentes[2].descricao) // Baixa prioridade por último
    }

    @Test
    fun buscarConcluidasOrdenadas_retornaApenasConcluidasEOrdenadas() = runBlocking {
        val t1 = Tarefa(descricao = "Concluida Antes", concluida = true, dataConclusao = System.currentTimeMillis() - 10000, prioridade = 0, disciplinaId = null, turmaId = null)
        val t2 = Tarefa(descricao = "Concluida Agora", concluida = true, dataConclusao = System.currentTimeMillis(), prioridade = 0, disciplinaId = null, turmaId = null)
        tarefaDao.inserir(t1)
        tarefaDao.inserir(t2)
        tarefaDao.inserir(Tarefa(descricao = "Pendente", concluida = false, prioridade = 0, disciplinaId = null, turmaId = null))

        val concluidas = tarefaDao.buscarConcluidasOrdenadas().first()
        assertEquals(2, concluidas.size)
        assertEquals("Concluida Agora", concluidas[0].descricao) // Mais recente primeiro
        assertEquals("Concluida Antes", concluidas[1].descricao)
    }


    @Test
    fun buscarTarefasUrgentes_retornaTarefasCorretas() = runBlocking {
        val formatoQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hojeCal = Calendar.getInstance()
        val amanhaCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val depoisAmanhaCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }

        val hojeStr = formatoQuery.format(hojeCal.time)
        val amanhaStr = formatoQuery.format(amanhaCal.time)
        val depoisAmanhaStr = formatoQuery.format(depoisAmanhaCal.time)

        // Urgentes
        tarefaDao.inserir(Tarefa(descricao = "Urgente Hoje 10h Prio Alta", prazoData = hojeStr, prazoHora = "10:00", prioridade = 2, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Urgente Amanha 09h Prio Media", prazoData = amanhaStr, prazoHora = "09:00", prioridade = 1, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Urgente Hoje 08h Prio Media", prazoData = hojeStr, prazoHora = "08:00", prioridade = 1, disciplinaId = null, turmaId = null))
        // Não urgentes
        tarefaDao.inserir(Tarefa(descricao = "Concluída Urgente", prazoData = hojeStr, prazoHora = "11:00", prioridade = 2, concluida = true, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Depois de Amanhã", prazoData = depoisAmanhaStr, prazoHora = "10:00", prioridade = 2, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "Sem Prazo", prazoData = null, prazoHora = null, prioridade = 2, disciplinaId = null, turmaId = null))


        val urgentes = tarefaDao.buscarTarefasUrgentes(hojeStr, amanhaStr).first()
        assertEquals(3, urgentes.size)
        assertEquals("Urgente Hoje 08h Prio Media", urgentes[0].descricao) // Ordenado por data, hora, prioridade
        assertEquals("Urgente Hoje 10h Prio Alta", urgentes[1].descricao)
        assertEquals("Urgente Amanha 09h Prio Media", urgentes[2].descricao)
    }

    @Test
    fun buscarPorDataDePrazo_retornaTarefasCorretas() = runBlocking {
        val dataAlvo = "2024-10-20"
        val outraData = "2024-10-21"
        tarefaDao.inserir(Tarefa(descricao = "T1 Data Alvo 10h", prazoData = dataAlvo, prazoHora = "10:00", prioridade = 1, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "T2 Data Alvo 08h", prazoData = dataAlvo, prazoHora = "08:00", prioridade = 2, disciplinaId = null, turmaId = null))
        tarefaDao.inserir(Tarefa(descricao = "T3 Outra Data", prazoData = outraData, prazoHora = "09:00", prioridade = 1, disciplinaId = null, turmaId = null))

        val porData = tarefaDao.buscarPorDataDePrazo(dataAlvo).first()
        assertEquals(2, porData.size)
        assertEquals("T2 Data Alvo 08h", porData[0].descricao) // Ordenado por hora, prioridade
        assertEquals("T1 Data Alvo 10h", porData[1].descricao)
    }

    @Test
    fun atualizarTarefa_refleteMudancas() = runBlocking {
        val tarefa = Tarefa(descricao = "Original", prioridade = 0, concluida = false, disciplinaId = null, turmaId = null)
        val id = tarefaDao.inserir(tarefa).toInt()
        val paraAtualizar = tarefaDao.buscarPorId(id)!!.copy(descricao = "Atualizada", concluida = true, prioridade = 2)
        tarefaDao.atualizar(paraAtualizar)
        val atualizada = tarefaDao.buscarPorId(id)!!
        assertEquals("Atualizada", atualizada.descricao)
        assertTrue(atualizada.concluida)
        assertEquals(2, atualizada.prioridade)
    }

    @Test
    fun deletarTarefa_naoDeveSerEncontrada() = runBlocking {
        val tarefa = Tarefa(descricao = "Para Deletar", prioridade = 0, disciplinaId = null, turmaId = null)
        val id = tarefaDao.inserir(tarefa).toInt()
        val paraDeletar = tarefaDao.buscarPorId(id)!!
        tarefaDao.deletar(paraDeletar)
        assertNull(tarefaDao.buscarPorId(id))
    }

    @Test
    fun deleteById_tarefaNaoDeveSerEncontrada() = runBlocking {
        val tarefa = Tarefa(descricao = "Para Deletar por ID", prioridade = 0, disciplinaId = null, turmaId = null)
        val id = tarefaDao.inserir(tarefa).toInt()
        tarefaDao.deleteById(id)
        assertNull(tarefaDao.buscarPorId(id))
    }


    @Test
    fun onDeleteSetNull_disciplinaDeletada_tarefaDisciplinaIdEhNull() = runBlocking {
        val tarefa = Tarefa(descricao = "Com Disc", disciplinaId = d1Id, prioridade = 0, turmaId = null)
        val id = tarefaDao.inserir(tarefa).toInt()
        val disciplina = disciplinaDao.buscarPorId(d1Id)!!
        disciplinaDao.deletar(disciplina) // onDelete = SET_NULL
        val tarefaAtualizada = tarefaDao.buscarPorId(id)
        assertNotNull(tarefaAtualizada)
        assertNull(tarefaAtualizada?.disciplinaId)
    }

    @Test
    fun onDeleteSetNull_turmaDeletada_tarefaTurmaIdEhNull() = runBlocking {
        val tarefa = Tarefa(descricao = "Com Turma", turmaId = t1Id, prioridade = 0, disciplinaId = null)
        val id = tarefaDao.inserir(tarefa).toInt()
        val turma = turmaDao.buscarPorId(t1Id)!!
        turmaDao.deletar(turma) // onDelete = SET_NULL
        val tarefaAtualizada = tarefaDao.buscarPorId(id)
        assertNotNull(tarefaAtualizada)
        assertNull(tarefaAtualizada?.turmaId)
    }
}
