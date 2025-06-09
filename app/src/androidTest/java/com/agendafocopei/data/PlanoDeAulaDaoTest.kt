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
class PlanoDeAulaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao
    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var planoDeAulaDao: PlanoDeAulaDao

    private var d1Id: Int = 0
    private var t1Id: Int = 0
    private var h1Id: Int = 0
    private var d2Id: Int = 0 // Para testar FKs

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration() // Para simplificar, se houver problemas com outras migrações não testadas aqui
            .build()
        disciplinaDao = db.disciplinaDao()
        turmaDao = db.turmaDao()
        horarioAulaDao = db.horarioAulaDao()
        planoDeAulaDao = db.planoDeAulaDao()

        val disc1 = Disciplina(nome = "História PDA", cor = Color.YELLOW)
        val disc2 = Disciplina(nome = "Geografia PDA", cor = Color.CYAN)
        disciplinaDao.inserir(disc1)
        disciplinaDao.inserir(disc2)
        d1Id = disciplinaDao.buscarPorNome("História PDA")!!.id
        d2Id = disciplinaDao.buscarPorNome("Geografia PDA")!!.id


        val turma1 = Turma(nome = "6A PDA", cor = Color.MAGENTA)
        turmaDao.inserir(turma1)
        t1Id = turmaDao.buscarPorNome("6A PDA")!!.id

        val horario1 = HorarioAula(disciplinaId = d1Id, turmaId = t1Id, diaDaSemana = Calendar.MONDAY, horaInicio = "08:00", horaFim = "09:00", salaAula = "S1")
        h1Id = horarioAulaDao.inserir(horario1).toInt()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun inserirPlanoDeAula_eBuscarPorId_retornaPlanoCorreto() = runBlocking {
        val plano = PlanoDeAula(horarioAulaId = h1Id, dataAula = "2024-08-01", disciplinaId = d1Id, turmaId = t1Id, tituloPlano = "Descobrimento", textoPlano = "Detalhes...", caminhoAnexo = null, tipoAnexo = null)
        val id = planoDeAulaDao.inserir(plano)
        val buscado = planoDeAulaDao.buscarPorId(id.toInt())
        assertNotNull(buscado)
        assertEquals("Descobrimento", buscado?.tituloPlano)
        assertEquals(d1Id, buscado?.disciplinaId)
    }

    @Test
    fun buscarPorHorarioAulaId_retornaPlanosCorretos() = runBlocking {
        planoDeAulaDao.inserir(PlanoDeAula(horarioAulaId = h1Id, dataAula = "2024-08-01", disciplinaId = d1Id, turmaId = t1Id, tituloPlano = "Plano A", textoPlano = null, null, null))
        planoDeAulaDao.inserir(PlanoDeAula(horarioAulaId = h1Id, dataAula = "2024-08-08", disciplinaId = d1Id, turmaId = t1Id, tituloPlano = "Plano B", textoPlano = null, null, null))
        // Outro horário
        val horario2 = HorarioAula(disciplinaId = d2Id, turmaId = t1Id, diaDaSemana = Calendar.TUESDAY, horaInicio = "10:00", horaFim = "11:00")
        val h2Id = horarioAulaDao.inserir(horario2).toInt()
        planoDeAulaDao.inserir(PlanoDeAula(horarioAulaId = h2Id, dataAula = "2024-08-02", disciplinaId = d2Id, turmaId = t1Id, tituloPlano = "Plano C", textoPlano = null, null, null))

        val planosH1 = planoDeAulaDao.buscarPorHorarioAulaId(h1Id).first()
        assertEquals(2, planosH1.size)
        assertTrue(planosH1.all { it.horarioAulaId == h1Id })
    }

    @Test
    fun buscarPorDisciplina_retornaPlanosCorretosEOrdenados() = runBlocking {
        planoDeAulaDao.inserir(PlanoDeAula(disciplinaId = d1Id, dataAula = "2024-08-10", horarioAulaId = null, turmaId = null, tituloPlano = "Tardio D1", textoPlano = null, null, null))
        planoDeAulaDao.inserir(PlanoDeAula(disciplinaId = d1Id, dataAula = "2024-08-01", horarioAulaId = null, turmaId = null, tituloPlano = "Cedo D1", textoPlano = null, null, null))
        planoDeAulaDao.inserir(PlanoDeAula(disciplinaId = d2Id, dataAula = "2024-08-05", horarioAulaId = null, turmaId = null, tituloPlano = "Unico D2", textoPlano = null, null, null))

        val planosD1 = planoDeAulaDao.buscarPorDisciplina(d1Id).first()
        assertEquals(2, planosD1.size)
        assertEquals("Tardio D1", planosD1[0].tituloPlano) // Ordenado por data DESC
        assertEquals("Cedo D1", planosD1[1].tituloPlano)
    }

    @Test
    fun buscarTodosParaDisplay_retornaDadosCompletosEOrdenados() = runBlocking {
        planoDeAulaDao.inserir(PlanoDeAula(disciplinaId = d1Id, turmaId = t1Id, dataAula = "2024-08-10", tituloPlano = "Plano D1T1", horarioAulaId = null, textoPlano = null, null, null))
        planoDeAulaDao.inserir(PlanoDeAula(disciplinaId = d2Id, turmaId = null, dataAula = "2024-08-01", tituloPlano = "Plano D2 Geral", horarioAulaId = null, textoPlano = null, null, null))

        val todosDisplay = planoDeAulaDao.buscarTodosParaDisplay().first()
        assertEquals(2, todosDisplay.size)
        assertEquals("Plano D1T1", todosDisplay[0].planoDeAula.tituloPlano)
        assertEquals("História PDA", todosDisplay[0].nomeDisciplina)
        assertEquals("6A PDA", todosDisplay[0].nomeTurma)
        assertEquals(Color.YELLOW, todosDisplay[0].corDisciplina)

        assertEquals("Plano D2 Geral", todosDisplay[1].planoDeAula.tituloPlano)
        assertEquals("Geografia PDA", todosDisplay[1].nomeDisciplina)
        assertNull(todosDisplay[1].nomeTurma) // Turma é opcional (LEFT JOIN)
    }


    @Test
    fun atualizarPlanoDeAula_refleteMudancas() = runBlocking {
        val plano = PlanoDeAula(disciplinaId = d1Id, tituloPlano = "Original", horarioAulaId = null, turmaId = null, dataAula = null, textoPlano = null, null, null)
        val id = planoDeAulaDao.inserir(plano)
        val planoParaAtualizar = planoDeAulaDao.buscarPorId(id.toInt())!!.copy(tituloPlano = "Atualizado")
        planoDeAulaDao.atualizar(planoParaAtualizar)
        val buscado = planoDeAulaDao.buscarPorId(id.toInt())
        assertEquals("Atualizado", buscado?.tituloPlano)
    }

    @Test
    fun deletarPlanoDeAula_naoDeveSerEncontrado() = runBlocking {
        val plano = PlanoDeAula(disciplinaId = d1Id, tituloPlano = "Para Deletar", horarioAulaId = null, turmaId = null, dataAula = null, textoPlano = null, null, null)
        val id = planoDeAulaDao.inserir(plano)
        val planoParaDeletar = planoDeAulaDao.buscarPorId(id.toInt())!!
        planoDeAulaDao.deletar(planoParaDeletar)
        assertNull(planoDeAulaDao.buscarPorId(id.toInt()))
    }

    @Test
    fun onDeleteCascade_disciplinaDeletada_planosSaoDeletados() = runBlocking {
        val plano = PlanoDeAula(disciplinaId = d1Id, tituloPlano = "Plano D1", horarioAulaId = null, turmaId = null, dataAula = null, textoPlano = null, null, null)
        val idPlano = planoDeAulaDao.inserir(plano)
        val disciplinaParaDeletar = disciplinaDao.buscarPorId(d1Id)!!
        disciplinaDao.deletar(disciplinaParaDeletar)
        assertNull(planoDeAulaDao.buscarPorId(idPlano.toInt()))
    }

    @Test
    fun onDeleteSetNull_horarioAulaDeletado_planoHorarioAulaIdEhNull() = runBlocking {
        val plano = PlanoDeAula(horarioAulaId = h1Id, disciplinaId = d1Id, tituloPlano = "Plano H1", turmaId = null, dataAula = null, textoPlano = null, null, null)
        val idPlano = planoDeAulaDao.inserir(plano)
        val horarioParaDeletar = horarioAulaDao.buscarPorId(h1Id)!!
        horarioAulaDao.deletar(horarioParaDeletar) // onDelete = SET_NULL para horarioAulaId
        val planoRecuperado = planoDeAulaDao.buscarPorId(idPlano.toInt())
        assertNotNull(planoRecuperado)
        assertNull(planoRecuperado?.horarioAulaId)
    }

    @Test
    fun onDeleteSetNull_turmaDeletada_planoTurmaIdEhNull() = runBlocking {
        val plano = PlanoDeAula(turmaId = t1Id, disciplinaId = d1Id, tituloPlano = "Plano T1", horarioAulaId = null, dataAula = null, textoPlano = null, null, null)
        val idPlano = planoDeAulaDao.inserir(plano)
        val turmaParaDeletar = turmaDao.buscarPorId(t1Id)!!
        turmaDao.deletar(turmaParaDeletar) // onDelete = SET_NULL para turmaId
        val planoRecuperado = planoDeAulaDao.buscarPorId(idPlano.toInt())
        assertNotNull(planoRecuperado)
        assertNull(planoRecuperado?.turmaId)
    }
}
