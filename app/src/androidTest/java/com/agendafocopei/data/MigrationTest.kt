package com.agendafocopei.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB_NAME = "migration-test"

    // Helper para testar migrações.
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName ?: AppDatabase::class.java.simpleName, // Nome canônico da classe AppDatabase
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Cria o banco de dados com a versão 1
        var db = helper.createDatabase(TEST_DB_NAME, 1).apply {
            // Não precisamos inserir dados aqui, pois a MIGRATION_1_2 apenas adiciona uma tabela
            // e o Room lida com isso automaticamente quando exportSchema=false.
            // Se estivéssemos testando dados sendo preservados ou transformados, inseriríamos aqui.
            close()
        }

        // Abre o banco de dados com a versão 2, aplicando a MIGRATION_1_2
        // runMigrationsAndValidate irá verificar se o esquema corresponde ao que o Room espera para a versão 2.
        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, AppDatabase.MIGRATION_1_2)

        // Verifica se a tabela disciplina_turma_cross_ref foi criada (opcional, pois validate faz isso)
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='disciplina_turma_cross_ref'")
        assertTrue("Tabela disciplina_turma_cross_ref não foi criada.", cursor.moveToFirst())
        cursor.close()

        db.close()
    }


    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        // Cria o banco de dados com a versão 2
        // Primeiro, executa a migração 1->2 para ter o esquema da versão 2 pronto
        var db = helper.createDatabase(TEST_DB_NAME, 1).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, AppDatabase.MIGRATION_1_2)

        // Insere dados de teste na versão 2 (sem a coluna 'cor')
        // Disciplinas
        val disciplinaValues = ContentValues().apply {
            put("id", 1) // ID explícito para teste
            put("nome_disciplina", "Matemática V2")
        }
        db.insert("disciplinas", SQLiteDatabase.CONFLICT_REPLACE, disciplinaValues)

        // Turmas
        val turmaValues = ContentValues().apply {
            put("id", 1) // ID explícito para teste
            put("nome_turma", "9A V2")
        }
        db.insert("turmas", SQLiteDatabase.CONFLICT_REPLACE, turmaValues)
        db.close()


        // Abre o banco de dados com a versão 3, aplicando a MIGRATION_2_3
        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
                                            // Note que precisamos passar todas as migrações até a versão alvo.

        // Verifica se a coluna 'cor' foi adicionada e é NULL por padrão
        val disciplinaCursor = db.query("SELECT id, nome_disciplina, cor FROM disciplinas WHERE id = 1")
        assertTrue("Cursor de disciplina está vazio.", disciplinaCursor.moveToFirst())
        val corDisciplinaColumnIndex = disciplinaCursor.getColumnIndex("cor")
        assertTrue("Coluna 'cor' não encontrada em disciplinas.", corDisciplinaColumnIndex >= 0)
        assertEquals("Matemática V2", disciplinaCursor.getString(disciplinaCursor.getColumnIndexOrThrow("nome_disciplina")))
        assertTrue("Coluna 'cor' em disciplinas não é NULL para dados antigos.", disciplinaCursor.isNull(corDisciplinaColumnIndex))
        disciplinaCursor.close()

        val turmaCursor = db.query("SELECT id, nome_turma, cor FROM turmas WHERE id = 1")
        assertTrue("Cursor de turma está vazio.", turmaCursor.moveToFirst())
        val corTurmaColumnIndex = turmaCursor.getColumnIndex("cor")
        assertTrue("Coluna 'cor' não encontrada em turmas.", corTurmaColumnIndex >= 0)
        assertEquals("9A V2", turmaCursor.getString(turmaCursor.getColumnIndexOrThrow("nome_turma")))
        assertTrue("Coluna 'cor' em turmas não é NULL para dados antigos.", turmaCursor.isNull(corTurmaColumnIndex))
        turmaCursor.close()

        // Opcional: Insira um novo dado com cor na v3 e verifique
        val newDisciplinaValues = ContentValues().apply {
            put("id", 2)
            put("nome_disciplina", "Física V3")
            put("cor", Color.BLUE)
        }
        db.insert("disciplinas", SQLiteDatabase.CONFLICT_REPLACE, newDisciplinaValues)
        val newDisciplinaCursor = db.query("SELECT cor FROM disciplinas WHERE id = 2")
        assertTrue(newDisciplinaCursor.moveToFirst())
        assertEquals(Color.BLUE, newDisciplinaCursor.getInt(newDisciplinaCursor.getColumnIndexOrThrow("cor")))
        newDisciplinaCursor.close()

        db.close()
    }

    @Test
import java.util.Calendar // Import para Calendar

    @Throws(IOException::class)
    fun migrate3To4() {
        // 1. Cria o banco de dados com o schema da versão 3.
        // O helper.createDatabase aplicará as migrações MIGRATION_1_2 e MIGRATION_2_3.
        val dbV3 = helper.createDatabase(TEST_DB_NAME, 3).apply {
            // Insere dados de teste que existiriam na versão 3 e são necessários para FKs
            val disciplinaValues = ContentValues().apply {
                put("id", 31) // ID explícito para teste de FK
                put("nome_disciplina", "Ciências V3")
                put("cor", Color.MAGENTA)
            }
            insert("disciplinas", SQLiteDatabase.CONFLICT_REPLACE, disciplinaValues)

            val turmaValues = ContentValues().apply {
                put("id", 41) // ID explícito para teste de FK
                put("nome_turma", "7C V3")
                put("cor", Color.DKGRAY)
            }
            insert("turmas", SQLiteDatabase.CONFLICT_REPLACE, turmaValues)
            close()
        }

        // 2. Abre o banco de dados com a versão 4, aplicando SOMENTE a MIGRATION_3_4.
        // O helper.runMigrationsAndValidate valida o schema após a migração.
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, AppDatabase.MIGRATION_3_4)

        // 3. Verifica se a tabela horarios_aula foi criada corretamente
        val cursor = dbV4.query("SELECT * FROM horarios_aula")
        assertNotNull("Cursor para horarios_aula não deve ser nulo.", cursor)
        assertEquals("Tabela horarios_aula deve estar vazia após a migração.", 0, cursor.count)
        cursor.close()

        // Opcional: Tente inserir dados na nova tabela horarios_aula
        // Para isso, precisamos de IDs de disciplina e turma que sabemos que existem.
        // Vamos usar os IDs inseridos na v3.
        val horarioValues = ContentValues().apply {
            put("dia_da_semana", Calendar.TUESDAY)
            put("hora_inicio", "10:00")
            put("hora_fim", "10:45")
            put("disciplinaId", 31) // ID correto da "Ciências V3"
            put("turmaId", 41)     // ID correto da "7C V3"
            put("sala_aula", "S202")
        }
        val insertResult = dbV4.insert("horarios_aula", SQLiteDatabase.CONFLICT_REPLACE, horarioValues) // Usar dbV4
        assertTrue("Falha ao inserir horário na tabela migrada.", insertResult != -1L)

        val horarioCursor = dbV4.query("SELECT * FROM horarios_aula WHERE disciplinaId = 31") // Usar dbV4 e ID correto
        assertTrue("Horário inserido não encontrado.", horarioCursor.moveToFirst())
        assertEquals("S202", horarioCursor.getString(horarioCursor.getColumnIndexOrThrow("sala_aula")))
        horarioCursor.close()

        dbV4.close() // Fechar o banco da versão 4
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        // 1. Cria o banco de dados com o schema da versão 4.
        // O helper aplicará MIGRATION_1_2, MIGRATION_2_3, e MIGRATION_3_4.
        val dbV4 = helper.createDatabase(TEST_DB_NAME, 4).apply {
            // Insere dados de teste que existiriam na versão 4.
            // Disciplina e Turma para FK em HorarioAula
            val disciplinaValues = ContentValues().apply {
                put("id", 51); put("nome_disciplina", "Artes V4"); put("cor", Color.BLACK)
            }
            insert("disciplinas", SQLiteDatabase.CONFLICT_REPLACE, disciplinaValues)
            val turmaValues = ContentValues().apply {
                put("id", 61); put("nome_turma", "6A V4"); put("cor", Color.WHITE)
            }
            insert("turmas", SQLiteDatabase.CONFLICT_REPLACE, turmaValues)

            // HorarioAula
            val horarioValues = ContentValues().apply {
                put("id", 101)
                put("dia_da_semana", Calendar.FRIDAY)
                put("hora_inicio", "13:00")
                put("hora_fim", "13:45")
                put("disciplinaId", 51)
                put("turmaId", 61)
                put("sala_aula", "Auditório V4")
            }
            insert("horarios_aula", SQLiteDatabase.CONFLICT_REPLACE, horarioValues)
            close()
        }

        // 2. Abre o banco de dados com a versão 5, aplicando SOMENTE a MIGRATION_4_5.
        val dbV5 = helper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, AppDatabase.MIGRATION_4_5)

        // 3. Verifica se a tabela eventos_recorrentes foi criada corretamente
        val cursor = dbV5.query("SELECT * FROM eventos_recorrentes")
        assertNotNull("Cursor para eventos_recorrentes não deve ser nulo.", cursor)
        assertEquals("Tabela eventos_recorrentes deve estar vazia após a migração.", 0, cursor.count)
        cursor.close()

        // 4. Opcional: Tente inserir dados na nova tabela eventos_recorrentes
        val eventoValues = ContentValues().apply {
            put("nome_evento", "Clube do Livro V5")
            put("dia_da_semana", Calendar.WEDNESDAY)
            put("hora_inicio", "16:00")
            put("hora_fim", "17:00")
            put("sala_local", "Biblioteca")
            put("cor", Color.parseColor("#FF9800")) // Laranja
            put("observacoes", "Ler capítulo 3")
        }
        val insertResult = dbV5.insert("eventos_recorrentes", SQLiteDatabase.CONFLICT_REPLACE, eventoValues)
        assertTrue("Falha ao inserir evento na tabela migrada.", insertResult != -1L)

        val eventoCursor = dbV5.query("SELECT * FROM eventos_recorrentes WHERE nome_evento = 'Clube do Livro V5'")
        assertTrue("Evento inserido não encontrado.", eventoCursor.moveToFirst())
        assertEquals(Color.parseColor("#FF9800"), eventoCursor.getInt(eventoCursor.getColumnIndexOrThrow("cor")))
        assertEquals("Ler capítulo 3", eventoCursor.getString(eventoCursor.getColumnIndexOrThrow("observacoes")))
        eventoCursor.close()

        // 5. Verifica se os dados da tabela horarios_aula (da v4) ainda existem
        val horarioPreservadoCursor = dbV5.query("SELECT * FROM horarios_aula WHERE id = 101")
        assertTrue("Horário da v4 não foi preservado.", horarioPreservadoCursor.moveToFirst())
        assertEquals("Auditório V4", horarioPreservadoCursor.getString(horarioPreservadoCursor.getColumnIndexOrThrow("sala_aula")))
        horarioPreservadoCursor.close()

        dbV5.close()
    }
}
