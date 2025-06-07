package com.agendafocopei.data

import android.content.Context
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agendafocopei.ui.model.HorarioAulaDisplay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class HorarioAulaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao
    private lateinit var horarioAulaDao: HorarioAulaDao

    private var d1Id: Int = 0
    private var d2Id: Int = 0
    private var t1Id: Int = 0
    private var t2Id: Int = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Simplifica testes, mas não para produção
            .build()
        disciplinaDao = db.disciplinaDao()
        turmaDao = db.turmaDao()
        horarioAulaDao = db.horarioAulaDao()

        // Inserir disciplinas de teste
        disciplinaDao.inserir(Disciplina(nome = "Matemática Teste", cor = Color.RED))
        disciplinaDao.inserir(Disciplina(nome = "Português Teste", cor = Color.BLUE))
        d1Id = disciplinaDao.buscarPorNome("Matemática Teste")!!.id
        d2Id = disciplinaDao.buscarPorNome("Português Teste")!!.id

        // Inserir turmas de teste
        turmaDao.inserir(Turma(nome = "9A Teste", cor = Color.GREEN))
        turmaDao.inserir(Turma(nome = "1B Teste", cor = Color.YELLOW))
        t1Id = turmaDao.buscarPorNome("9A Teste")!!.id
        t2Id = turmaDao.buscarPorNome("1B Teste")!!.id
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirHorarioAula_eBuscarPorId_retornaHorarioCorreto() = runBlocking {
        val horario = HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", disciplinaId = d1Id, turmaId = t1Id, salaAula = "S1")
        val idInserido = horarioAulaDao.inserir(horario)

        val buscado = horarioAulaDao.buscarPorId(idInserido.toInt())
        assertNotNull(buscado)
        assertEquals(idInserido.toInt(), buscado?.id)
        assertEquals(Calendar.MONDAY, buscado?.diaDaSemana)
        assertEquals("08:00", buscado?.horaInicio)
        assertEquals(d1Id, buscado?.disciplinaId)
        assertEquals(t1Id, buscado?.turmaId)
    }

    @Test
    fun inserirHorarioAula_buscarTodosOrdenados_retornaListaOrdenada() = runBlocking {
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.TUESDAY, horaInicio = "10:00", horaFim = "11:00", disciplinaId = d1Id, turmaId = t1Id))
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", disciplinaId = d2Id, turmaId = t1Id))

        val todos = horarioAulaDao.buscarTodosOrdenados().first()
        assertEquals(2, todos.size)
        assertEquals(Calendar.MONDAY, todos[0].diaDaSemana)
        assertEquals("08:00", todos[0].horaInicio)
        assertEquals(Calendar.TUESDAY, todos[1].diaDaSemana)
    }

    @Test
    fun atualizarHorarioAula_buscarPorId_retornaHorarioAtualizado() = runBlocking {
        val horario = HorarioAula(diaDaSemana = Calendar.WEDNESDAY, horaInicio = "14:00", horaFim = "15:00", disciplinaId = d1Id, turmaId = t1Id, salaAula = "S2")
        val idInserido = horarioAulaDao.inserir(horario)

        val horarioParaAtualizar = horarioAulaDao.buscarPorId(idInserido.toInt())!!
        val horarioAtualizado = horarioParaAtualizar.copy(salaAula = "S101", horaFim = "15:30")
        horarioAulaDao.atualizar(horarioAtualizado)

        val buscado = horarioAulaDao.buscarPorId(idInserido.toInt())
        assertEquals("S101", buscado?.salaAula)
        assertEquals("15:30", buscado?.horaFim)
    }

    @Test
    fun deletarHorarioAula_buscarPorId_retornaNull() = runBlocking {
        val horario = HorarioAula(diaDaSemana = Calendar.THURSDAY, horaInicio = "07:00", horaFim = "07:45", disciplinaId = d1Id, turmaId = t1Id)
        val idInserido = horarioAulaDao.inserir(horario)

        val horarioParaDeletar = horarioAulaDao.buscarPorId(idInserido.toInt())!!
        horarioAulaDao.deletar(horarioParaDeletar)

        val buscado = horarioAulaDao.buscarPorId(idInserido.toInt())
        assertNull(buscado)
    }

    @Test
    fun deleteById_buscarPorId_retornaNull() = runBlocking {
        val horario = HorarioAula(diaDaSemana = Calendar.FRIDAY, horaInicio = "09:00", horaFim = "09:45", disciplinaId = d1Id, turmaId = t1Id)
        val idInserido = horarioAulaDao.inserir(horario)
        assertNotNull(horarioAulaDao.buscarPorId(idInserido.toInt()))

        horarioAulaDao.deleteById(idInserido.toInt())
        assertNull(horarioAulaDao.buscarPorId(idInserido.toInt()))
    }

    @Test
    fun buscarPorDia_retornaApenasHorariosDoDiaCorreto() = runBlocking {
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", disciplinaId = d1Id, turmaId = t1Id))
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.TUESDAY, horaInicio = "10:00", horaFim = "11:00", disciplinaId = d2Id, turmaId = t1Id))
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "10:00", horaFim = "11:00", disciplinaId = d1Id, turmaId = t2Id))

        val horariosSegunda = horarioAulaDao.buscarPorDia(Calendar.MONDAY).first()
        assertEquals(2, horariosSegunda.size)
        assertTrue(horariosSegunda.all { it.diaDaSemana == Calendar.MONDAY })

        val horariosTerca = horarioAulaDao.buscarPorDia(Calendar.TUESDAY).first()
        assertEquals(1, horariosTerca.size)
        assertTrue(horariosTerca.all { it.diaDaSemana == Calendar.TUESDAY })
    }

    @Test
    fun buscarTodosParaDisplay_retornaDadosCorretos() = runBlocking {
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", disciplinaId = d1Id, turmaId = t1Id, salaAula = "S101"))

        val displayList = horarioAulaDao.buscarTodosParaDisplay().first()
        assertEquals(1, displayList.size)
        val displayItem = displayList[0]

        assertEquals(Calendar.MONDAY, displayItem.diaDaSemana)
        assertEquals("08:00", displayItem.horaInicio)
        assertEquals("09:00", displayItem.horaFim)
        assertEquals("Matemática Teste", displayItem.nomeDisciplina)
        assertEquals(Color.RED, displayItem.corDisciplina)
        assertEquals("9A Teste", displayItem.nomeTurma)
        assertEquals(Color.GREEN, displayItem.corTurma)
        assertEquals("S101", displayItem.salaAula)
    }

    @Test
    fun onDeleteCascade_disciplinaDeletada_horariosSaoDeletados() = runBlocking {
        val horario1 = HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", disciplinaId = d1Id, turmaId = t1Id)
        val horario2 = HorarioAula(diaDaSemana = Calendar.TUESDAY, horaInicio = "10:00", horaFim = "11:00", disciplinaId = d2Id, turmaId = t1Id) // Outra disciplina
        val idHorario1 = horarioAulaDao.inserir(horario1)
        horarioAulaDao.inserir(horario2)

        val disciplinaParaDeletar = disciplinaDao.buscarPorId(d1Id)!!
        disciplinaDao.deletar(disciplinaParaDeletar)

        assertNull(horarioAulaDao.buscarPorId(idHorario1.toInt()))
        assertNotNull(horarioAulaDao.buscarPorId(horarioAulaDao.buscarPorDisciplina(d2Id).first()[0].id)) // Horário da d2 ainda existe
    }

    @Test
    fun onDeleteCascade_turmaDeletada_horariosSaoDeletados() = runBlocking {
        val horario1 = HorarioAula(diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", disciplinaId = d1Id, turmaId = t1Id)
        val horario2 = HorarioAula(diaDaSemana = Calendar.TUESDAY, horaInicio = "10:00", horaFim = "11:00", disciplinaId = d1Id, turmaId = t2Id) // Outra turma
        val idHorario1 = horarioAulaDao.inserir(horario1)
        horarioAulaDao.inserir(horario2)

        val turmaParaDeletar = turmaDao.buscarPorId(t1Id)!!
        turmaDao.deletar(turmaParaDeletar)

        assertNull(horarioAulaDao.buscarPorId(idHorario1.toInt()))
        assertNotNull(horarioAulaDao.buscarPorId(horarioAulaDao.buscarPorTurma(t2Id).first()[0].id))
    }

    @Test
    fun buscarProximoHorarioAulaDoDia_encontraProximoCorretamente() = runBlocking {
        val diaSemanaAtual = Calendar.TUESDAY
        val horaAtualSimulada = "09:30"

        // Horários de teste
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = diaSemanaAtual, horaInicio = "08:00", horaFim = "08:45", disciplinaId = d1Id, turmaId = t1Id)) // Passou
        val proximoEsperado = HorarioAula(diaDaSemana = diaSemanaAtual, horaInicio = "10:00", horaFim = "10:45", disciplinaId = d2Id, turmaId = t1Id) // Próximo
        horarioAulaDao.inserir(proximoEsperado)
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = diaSemanaAtual, horaInicio = "11:00", horaFim = "11:45", disciplinaId = d1Id, turmaId = t2Id)) // Mais tarde
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.WEDNESDAY, horaInicio = "09:00", horaFim = "09:45", disciplinaId = d1Id, turmaId = t1Id)) // Outro dia

        val resultado = horarioAulaDao.buscarProximoHorarioAulaDoDia(diaSemanaAtual, horaAtualSimulada)
        assertNotNull(resultado)
        assertEquals(d2Id, resultado?.disciplinaId) // Verifica pela disciplinaId se é o correto
        assertEquals("10:00", resultado?.horaInicio)
        assertEquals("Português Teste", resultado?.nomeDisciplina) // Verifica o nome da disciplina do HorarioAulaDisplay
    }

    @Test
    fun buscarProximoHorarioAulaDoDia_semProximos_retornaNull() = runBlocking {
        val diaSemanaAtual = Calendar.TUESDAY
        val horaAtualSimulada = "12:00"

        horarioAulaDao.inserir(HorarioAula(diaDaSemana = diaSemanaAtual, horaInicio = "08:00", horaFim = "08:45", disciplinaId = d1Id, turmaId = t1Id)) // Passou
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.WEDNESDAY, horaInicio = "09:00", horaFim = "09:45", disciplinaId = d1Id, turmaId = t1Id)) // Outro dia

        val resultado = horarioAulaDao.buscarProximoHorarioAulaDoDia(diaSemanaAtual, horaAtualSimulada)
        assertNull(resultado)
    }

    @Test
    fun buscarTodosParaDisplayPorDia_retornaListaCorretaEOrdenada() = runBlocking {
        val diaSemanaAtual = Calendar.MONDAY
        // Inserir horários para o dia atual e outros dias
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = diaSemanaAtual, horaInicio = "10:00", horaFim = "10:45", disciplinaId = d1Id, turmaId = t1Id, salaAula = "S1"))
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = diaSemanaAtual, horaInicio = "08:00", horaFim = "08:45", disciplinaId = d2Id, turmaId = t2Id, salaAula = "S2"))
        horarioAulaDao.inserir(HorarioAula(diaDaSemana = Calendar.TUESDAY, horaInicio = "09:00", horaFim = "09:45", disciplinaId = d1Id, turmaId = t1Id))

        val resultado = horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaAtual).first()
        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.diaDaSemana == diaSemanaAtual })

        // Verifica a ordem (08:00 primeiro)
        assertEquals("08:00", resultado[0].horaInicio)
        assertEquals(d2Id, resultado[0].disciplinaId)
        assertEquals("Português Teste", resultado[0].nomeDisciplina)
        assertEquals(Color.BLUE, resultado[0].corDisciplina) // Cor da d2
        assertEquals("1B Teste", resultado[0].nomeTurma)    // Nome da t2
        assertEquals(Color.YELLOW, resultado[0].corTurma)   // Cor da t2
        assertEquals("S2", resultado[0].salaAula)

        assertEquals("10:00", resultado[1].horaInicio)
        assertEquals(d1Id, resultado[1].disciplinaId)
        assertEquals("Matemática Teste", resultado[1].nomeDisciplina)
        assertEquals(Color.RED, resultado[1].corDisciplina)    // Cor da d1
        assertEquals("9A Teste", resultado[1].nomeTurma)     // Nome da t1
        assertEquals(Color.GREEN, resultado[1].corTurma)    // Cor da t1
        assertEquals("S1", resultado[1].salaAula)
    }
}
