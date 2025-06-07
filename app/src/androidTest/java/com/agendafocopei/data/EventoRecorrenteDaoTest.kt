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
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class EventoRecorrenteDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Para simplicidade nos testes
            .build()
        eventoRecorrenteDao = db.eventoRecorrenteDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirEvento_eBuscarPorId_retornaEventoCorreto() = runBlocking {
        val evento = EventoRecorrente(
            nomeEvento = "Reunião Semanal",
            diaDaSemana = Calendar.MONDAY,
            horaInicio = "10:00",
            horaFim = "11:00",
            salaLocal = "Sala A",
            cor = Color.BLUE,
            observacoes = "Pauta principal"
        )
        val idInserido = eventoRecorrenteDao.inserir(evento)

        val buscado = eventoRecorrenteDao.buscarPorId(idInserido.toInt())
        assertNotNull(buscado)
        assertEquals(idInserido.toInt(), buscado?.id)
        assertEquals("Reunião Semanal", buscado?.nomeEvento)
        assertEquals(Calendar.MONDAY, buscado?.diaDaSemana)
        assertEquals("10:00", buscado?.horaInicio)
        assertEquals("11:00", buscado?.horaFim)
        assertEquals("Sala A", buscado?.salaLocal)
        assertEquals(Color.BLUE, buscado?.cor)
        assertEquals("Pauta principal", buscado?.observacoes)
    }

    @Test
    fun inserirMultiplosEventos_buscarTodosOrdenados_retornaListaOrdenada() = runBlocking {
        val evento1 = EventoRecorrente(nomeEvento = "Evento Tarde", diaDaSemana = Calendar.TUESDAY, horaInicio = "14:00", horaFim = "15:00", salaLocal = "S1", cor = Color.RED, observacoes = null)
        val evento2 = EventoRecorrente(nomeEvento = "Evento Manhã", diaDaSemana = Calendar.MONDAY, horaInicio = "09:00", horaFim = "10:00", salaLocal = "S2", cor = Color.GREEN, observacoes = "Obs")
        val evento3 = EventoRecorrente(nomeEvento = "Evento Meio Dia", diaDaSemana = Calendar.MONDAY, horaInicio = "12:00", horaFim = "13:00", salaLocal = "S3", cor = Color.YELLOW, observacoes = null)

        eventoRecorrenteDao.inserir(evento1)
        eventoRecorrenteDao.inserir(evento2)
        eventoRecorrenteDao.inserir(evento3)

        val todos = eventoRecorrenteDao.buscarTodosOrdenados().first()
        assertEquals(3, todos.size)
        assertEquals("Evento Manhã", todos[0].nomeEvento) // Segunda 09:00
        assertEquals("Evento Meio Dia", todos[1].nomeEvento) // Segunda 12:00
        assertEquals("Evento Tarde", todos[2].nomeEvento) // Terça 14:00
    }

    @Test
    fun atualizarEvento_buscarPorId_retornaEventoAtualizado() = runBlocking {
        val evento = EventoRecorrente(nomeEvento = "Planejamento", diaDaSemana = Calendar.WEDNESDAY, horaInicio = "16:00", horaFim = "17:00", salaLocal = "Lab C", cor = Color.CYAN, observacoes = "Trazer notebook")
        val idInserido = eventoRecorrenteDao.inserir(evento)

        val eventoParaAtualizar = eventoRecorrenteDao.buscarPorId(idInserido.toInt())!!
        val eventoAtualizado = eventoParaAtualizar.copy(nomeEvento = "Planejamento Quinzenal", observacoes = "Revisar metas")
        eventoRecorrenteDao.atualizar(eventoAtualizado)

        val buscado = eventoRecorrenteDao.buscarPorId(idInserido.toInt())
        assertEquals("Planejamento Quinzenal", buscado?.nomeEvento)
        assertEquals("Revisar metas", buscado?.observacoes)
    }

    @Test
    fun deletarEvento_buscarPorId_retornaNull() = runBlocking {
        val evento = EventoRecorrente(nomeEvento = "Workshop", diaDaSemana = Calendar.THURSDAY, horaInicio = "18:00", horaFim = "20:00", salaLocal = "Auditório", cor = Color.MAGENTA, observacoes = null)
        val idInserido = eventoRecorrenteDao.inserir(evento)

        val eventoParaDeletar = eventoRecorrenteDao.buscarPorId(idInserido.toInt())!!
        eventoRecorrenteDao.deletar(eventoParaDeletar)

        val buscado = eventoRecorrenteDao.buscarPorId(idInserido.toInt())
        assertNull(buscado)
    }

    @Test
    fun deleteById_buscarPorId_retornaNull() = runBlocking {
        val evento = EventoRecorrente(nomeEvento = "Curso Online", diaDaSemana = Calendar.FRIDAY, horaInicio = "08:00", horaFim = "12:00", salaLocal = "Online", cor = Color.LTGRAY, observacoes = "Link na descrição")
        val idInserido = eventoRecorrenteDao.inserir(evento)
        assertNotNull(eventoRecorrenteDao.buscarPorId(idInserido.toInt()))

        eventoRecorrenteDao.deleteById(idInserido.toInt())
        assertNull(eventoRecorrenteDao.buscarPorId(idInserido.toInt()))
    }

    @Test
    fun buscarPorDia_retornaApenasEventosDoDiaCorreto() = runBlocking {
        eventoRecorrenteDao.inserir(EventoRecorrente(nomeEvento = "Yoga", diaDaSemana = Calendar.SATURDAY, horaInicio = "09:00", horaFim = "10:00", salaLocal = "Parque", cor = Color.GREEN, observacoes = null))
        eventoRecorrenteDao.inserir(EventoRecorrente(nomeEvento = "Leitura", diaDaSemana = Calendar.SUNDAY, horaInicio = "15:00", horaFim = "16:00", salaLocal = "Casa", cor = Color.BLUE, observacoes = null))
        eventoRecorrenteDao.inserir(EventoRecorrente(nomeEvento = "Yoga Matinal", diaDaSemana = Calendar.SATURDAY, horaInicio = "07:00", horaFim = "08:00", salaLocal = "Parque", cor = Color.CYAN, observacoes = null))

        val eventosSabado = eventoRecorrenteDao.buscarPorDia(Calendar.SATURDAY).first()
        assertEquals(2, eventosSabado.size)
        assertTrue(eventosSabado.all { it.diaDaSemana == Calendar.SATURDAY })
        assertEquals("Yoga Matinal", eventosSabado[0].nomeEvento) // Ordenado por hora
        assertEquals("Yoga", eventosSabado[1].nomeEvento)

        val eventosDomingo = eventoRecorrenteDao.buscarPorDia(Calendar.SUNDAY).first()
        assertEquals(1, eventosDomingo.size)
        assertTrue(eventosDomingo.all { it.diaDaSemana == Calendar.SUNDAY })
    }
}
