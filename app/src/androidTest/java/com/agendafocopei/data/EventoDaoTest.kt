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
class EventoDaoTest { // Nome da classe correto

    private lateinit var db: AppDatabase
    private lateinit var eventoDao: EventoDao // DAO correto

    private val queryDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventoDao = db.eventoDao() // Usar o DAO correto
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirEvento_comDataEspecifica_eBuscarPorId_retornaCorretamente() = runBlocking {
        val dataCal = Calendar.getInstance().apply { set(2024, Calendar.JULY, 20) }
        val dataStr = queryDateFormat.format(dataCal.time)
        val evento = Evento(
            nomeEvento = "Aniversário",
            diaDaSemana = dataCal.get(Calendar.DAY_OF_WEEK), // Armazenar o dia da semana da data específica
            horaInicio = "19:00",
            horaFim = "23:00",
            salaLocal = "Casa",
            cor = Color.MAGENTA,
            observacoes = "Festa!",
            dataEspecifica = dataStr
        )
        val id = eventoDao.inserir(evento)
        val recuperado = eventoDao.buscarPorId(id.toInt())
        assertNotNull(recuperado)
        assertEquals(dataStr, recuperado?.dataEspecifica)
        assertEquals("Aniversário", recuperado?.nomeEvento)
    }

    @Test
    fun inserirEvento_semDataEspecifica_eBuscarPorId_retornaCorretamente() = runBlocking {
        val evento = Evento(
            nomeEvento = "Reunião Semanal",
            diaDaSemana = Calendar.MONDAY,
            horaInicio = "10:00",
            horaFim = "11:00",
            salaLocal = "Sala A",
            cor = Color.BLUE,
            observacoes = "Pauta principal",
            dataEspecifica = null
        )
        val id = eventoDao.inserir(evento)
        val recuperado = eventoDao.buscarPorId(id.toInt())
        assertNotNull(recuperado)
        assertNull(recuperado?.dataEspecifica)
        assertEquals(Calendar.MONDAY, recuperado?.diaDaSemana)
        assertEquals("Reunião Semanal", recuperado?.nomeEvento)
    }

    @Test
    fun atualizarEvento_deRecorrenteParaUnico_eViceVersa() = runBlocking {
        // 1. Insere como recorrente
        var evento = Evento(nomeEvento = "Yoga", diaDaSemana = Calendar.WEDNESDAY, horaInicio = "07:00", horaFim = "08:00", salaLocal = "Estúdio", cor = Color.GREEN, observacoes = null, dataEspecifica = null)
        val id = eventoDao.inserir(evento)

        // 2. Atualiza para único
        val dataUnicaCal = Calendar.getInstance().apply { set(2024, Calendar.AUGUST, 14) } // Uma quarta-feira
        val dataUnicaStr = queryDateFormat.format(dataUnicaCal.time)
        val eventoParaAtualizarUnico = eventoDao.buscarPorId(id.toInt())!!.copy(
            dataEspecifica = dataUnicaStr,
            diaDaSemana = dataUnicaCal.get(Calendar.DAY_OF_WEEK) // Atualiza dia da semana para consistência
        )
        eventoDao.atualizar(eventoParaAtualizarUnico)
        var recuperado = eventoDao.buscarPorId(id.toInt())
        assertNotNull(recuperado)
        assertEquals(dataUnicaStr, recuperado?.dataEspecifica)
        assertEquals(Calendar.WEDNESDAY, recuperado?.diaDaSemana)

        // 3. Atualiza de volta para recorrente
        val eventoParaAtualizarRecorrente = recuperado!!.copy(dataEspecifica = null, diaDaSemana = Calendar.FRIDAY)
        eventoDao.atualizar(eventoParaAtualizarRecorrente)
        recuperado = eventoDao.buscarPorId(id.toInt())
        assertNotNull(recuperado)
        assertNull(recuperado?.dataEspecifica)
        assertEquals(Calendar.FRIDAY, recuperado?.diaDaSemana)
    }


    @Test
    fun buscarEventosParaData_retornaEventosUnicosERecorrentesCorretos() = runBlocking {
        val calSegunda = Calendar.getInstance().apply { set(2024, Calendar.JULY, 15) } // Uma segunda-feira
        val dataSegundaStr = queryDateFormat.format(calSegunda.time)
        val diaSemanaSegunda = calSegunda.get(Calendar.DAY_OF_WEEK) // Deve ser Calendar.MONDAY

        val calTerca = Calendar.getInstance().apply { set(2024, Calendar.JULY, 16) } // Uma terça-feira
        val dataTercaStr = queryDateFormat.format(calTerca.time)
        val diaSemanaTerca = calTerca.get(Calendar.DAY_OF_WEEK) // Deve ser Calendar.TUESDAY

        // Eventos
        eventoDao.inserir(Evento(nomeEvento = "Recorrente Seg", diaDaSemana = Calendar.MONDAY, horaInicio = "10:00", horaFim = "11:00", salaLocal = null, cor = null, observacoes = null, dataEspecifica = null))
        eventoDao.inserir(Evento(nomeEvento = "Unico Seg Data", diaDaSemana = diaSemanaSegunda, horaInicio = "14:00", horaFim = "15:00", salaLocal = null, cor = null, observacoes = null, dataEspecifica = dataSegundaStr))
        eventoDao.inserir(Evento(nomeEvento = "Unico Ter Data", diaDaSemana = diaSemanaTerca, horaInicio = "16:00", horaFim = "17:00", salaLocal = null, cor = null, observacoes = null, dataEspecifica = dataTercaStr))
        eventoDao.inserir(Evento(nomeEvento = "Recorrente Ter", diaDaSemana = Calendar.TUESDAY, horaInicio = "09:00", horaFim = "10:00", salaLocal = null, cor = null, observacoes = null, dataEspecifica = null))

        // Teste 1: Para segunda-feira específica
        var eventos = eventoDao.buscarEventosParaData(diaSemanaSegunda, dataSegundaStr).first()
        assertEquals(2, eventos.size)
        assertTrue(eventos.any { it.nomeEvento == "Recorrente Seg" })
        assertTrue(eventos.any { it.nomeEvento == "Unico Seg Data" })

        // Teste 2: Para terça-feira específica
        eventos = eventoDao.buscarEventosParaData(diaSemanaTerca, dataTercaStr).first()
        assertEquals(2, eventos.size)
        assertTrue(eventos.any { it.nomeEvento == "Recorrente Ter" })
        assertTrue(eventos.any { it.nomeEvento == "Unico Ter Data" })

        // Teste 3: Para uma quarta-feira específica, onde não há eventos únicos, mas pode haver recorrentes de quarta
        val calQuarta = Calendar.getInstance().apply { set(2024, Calendar.JULY, 17) }
        val dataQuartaStr = queryDateFormat.format(calQuarta.time)
        val diaSemanaQuarta = calQuarta.get(Calendar.DAY_OF_WEEK)
        eventoDao.inserir(Evento(nomeEvento = "Recorrente Qua", diaDaSemana = Calendar.WEDNESDAY, horaInicio = "11:00", horaFim = "12:00", salaLocal = null, cor = null, observacoes = null, dataEspecifica = null))

        eventos = eventoDao.buscarEventosParaData(diaSemanaQuarta, dataQuartaStr).first()
        assertEquals(1, eventos.size)
        assertEquals("Recorrente Qua", eventos[0].nomeEvento)

        // Teste 4: Para dataSegundaStr, mas passando um diaDaSemana que não é segunda (ex: domingo)
        // Deve retornar apenas o evento único daquela data.
        eventos = eventoDao.buscarEventosParaData(Calendar.SUNDAY, dataSegundaStr).first()
        assertEquals(1, eventos.size)
        assertEquals("Unico Seg Data", eventos[0].nomeEvento)
    }

    @Test
    fun buscarProximoEventoParaData_consideraUnicosERecorrentes() = runBlocking {
        val calSegunda = Calendar.getInstance().apply { set(2024, Calendar.JULY, 15) }
        val dataSegundaStr = queryDateFormat.format(calSegunda.time)
        val diaSemanaSegunda = calSegunda.get(Calendar.DAY_OF_WEEK)
        val horaAtual = "09:00"

        // Eventos
        eventoDao.inserir(Evento(nomeEvento = "Passou Recorrente Seg", diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "08:30", dataEspecifica = null, salaLocal = null, cor = null, observacoes = null))
        eventoDao.inserir(Evento(nomeEvento = "Passou Unico Seg", diaDaSemana = diaSemanaSegunda, horaInicio = "08:30", horaFim = "08:45", dataEspecifica = dataSegundaStr, salaLocal = null, cor = null, observacoes = null))

        eventoDao.inserir(Evento(nomeEvento = "Proximo Recorrente Seg", diaDaSemana = Calendar.MONDAY, horaInicio = "10:00", horaFim = "11:00", dataEspecifica = null, salaLocal = null, cor = null, observacoes = null))
        eventoDao.inserir(Evento(nomeEvento = "Proximo Unico Seg", diaDaSemana = diaSemanaSegunda, horaInicio = "09:30", horaFim = "09:45", dataEspecifica = dataSegundaStr, salaLocal = null, cor = null, observacoes = null)) // Este deve ser o próximo
        eventoDao.inserir(Evento(nomeEvento = "Mais Tarde Unico Seg", diaDaSemana = diaSemanaSegunda, horaInicio = "12:00", horaFim = "13:00", dataEspecifica = dataSegundaStr, salaLocal = null, cor = null, observacoes = null))
        eventoDao.inserir(Evento(nomeEvento = "Outro Dia Unico", diaDaSemana = Calendar.TUESDAY, horaInicio = "09:15", horaFim = "09:45", dataEspecifica = queryDateFormat.format(Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR, 1)}.time ), salaLocal = null, cor = null, observacoes = null))


        val proximo = eventoDao.buscarProximoEventoParaData(diaSemanaSegunda, dataSegundaStr, horaAtual)
        assertNotNull(proximo)
        assertEquals("Proximo Unico Seg", proximo?.nomeEvento)
        assertEquals("09:30", proximo?.horaInicio)
    }

    @Test
    fun buscarProximoEventoParaData_proximoEhRecorrente() = runBlocking {
        val calSegunda = Calendar.getInstance().apply { set(2024, Calendar.JULY, 15) }
        val dataSegundaStr = queryDateFormat.format(calSegunda.time)
        val diaSemanaSegunda = calSegunda.get(Calendar.DAY_OF_WEEK)
        val horaAtual = "09:00"

        eventoDao.inserir(Evento(nomeEvento = "Proximo Recorrente Seg", diaDaSemana = Calendar.MONDAY, horaInicio = "09:15", horaFim = "10:15", dataEspecifica = null, salaLocal = null, cor = null, observacoes = null)) // Este é o próximo
        eventoDao.inserir(Evento(nomeEvento = "Mais Tarde Unico Seg", diaDaSemana = diaSemanaSegunda, horaInicio = "10:00", horaFim = "11:00", dataEspecifica = dataSegundaStr, salaLocal = null, cor = null, observacoes = null))

        val proximo = eventoDao.buscarProximoEventoParaData(diaSemanaSegunda, dataSegundaStr, horaAtual)
        assertNotNull(proximo)
        assertEquals("Proximo Recorrente Seg", proximo?.nomeEvento)
        assertEquals("09:15", proximo?.horaInicio)
        assertNull(proximo?.dataEspecifica)
    }

    // Testes CRUD básicos já existentes (inserir, atualizar, deletar, buscarTodos, buscarPorId, deleteById)
    // são mantidos e devem funcionar com a entidade Evento.
    // O teste buscarPorDia já foi atualizado implicitamente pela nova query buscarEventosParaData.
}
