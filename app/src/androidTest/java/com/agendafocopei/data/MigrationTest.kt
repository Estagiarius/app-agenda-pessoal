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

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // 1. Cria o banco de dados com o schema da versão 5.
        // O helper aplicará MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, e MIGRATION_4_5.
        val dbV5 = helper.createDatabase(TEST_DB_NAME, 5).apply {
            // Insere um evento na v5 (sem a coluna data_especifica)
            val eventoValuesV5 = ContentValues().apply {
                put("id", 1) // ID explícito para facilitar a consulta depois
                put("nome_evento", "Evento V5 Teste")
                put("dia_da_semana", Calendar.MONDAY)
                put("hora_inicio", "10:00")
                put("hora_fim", "11:00")
                put("cor", Color.RED)
            }
            insert("eventos_recorrentes", SQLiteDatabase.CONFLICT_REPLACE, eventoValuesV5)
            close()
        }

        // 2. Abre o banco de dados com a versão 6, aplicando SOMENTE a MIGRATION_5_6.
        val dbV6 = helper.runMigrationsAndValidate(TEST_DB_NAME, 6, true, AppDatabase.MIGRATION_5_6)

        // 3. Verifica se a coluna data_especifica foi adicionada e é NULL para dados antigos.
        val cursor = dbV6.query("SELECT id, nome_evento, data_especifica, dia_da_semana FROM eventos_recorrentes WHERE id = 1")
        assertTrue("Cursor do evento V5 está vazio.", cursor.moveToFirst())
        assertEquals("Evento V5 Teste", cursor.getString(cursor.getColumnIndexOrThrow("nome_evento")))

        val dataEspecificaColumnIndex = cursor.getColumnIndex("data_especifica")
        assertTrue("Coluna 'data_especifica' não encontrada.", dataEspecificaColumnIndex >= 0)
        assertTrue("Coluna 'data_especifica' não é NULL para dados antigos.", cursor.isNull(dataEspecificaColumnIndex))
        assertEquals("Dia da semana deve ser mantido para evento recorrente antigo", Calendar.MONDAY, cursor.getInt(cursor.getColumnIndexOrThrow("dia_da_semana")))
        cursor.close()

        // 4. Opcional: Tenta inserir um novo Evento com data_especifica na v6.
        val dataCal = Calendar.getInstance().apply { set(2024, Calendar.DECEMBER, 25) }
        val dataStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dataCal.time)

        val eventoValuesV6 = ContentValues().apply {
            put("nome_evento", "Natal V6")
            put("dia_da_semana", dataCal.get(Calendar.DAY_OF_WEEK)) // Deve ser o dia da semana de dataEspecifica
            put("hora_inicio", "00:00")
            put("hora_fim", "23:59")
            put("data_especifica", dataStr)
            put("cor", Color.GREEN)
        }
        val insertResult = dbV6.insert("eventos_recorrentes", SQLiteDatabase.CONFLICT_REPLACE, eventoValuesV6)
        assertTrue("Falha ao inserir evento V6 com data_especifica.", insertResult != -1L)

        val newEventCursor = dbV6.query("SELECT data_especifica, dia_da_semana FROM eventos_recorrentes WHERE nome_evento = 'Natal V6'")
        assertTrue("Novo evento V6 não encontrado.", newEventCursor.moveToFirst())
        assertEquals(dataStr, newEventCursor.getString(newEventCursor.getColumnIndexOrThrow("data_especifica")))
        assertEquals(dataCal.get(Calendar.DAY_OF_WEEK), newEventCursor.getInt(newEventCursor.getColumnIndexOrThrow("dia_da_semana")))
        newEventCursor.close()

        dbV6.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        // 1. Cria o banco de dados com o schema da versão 6.
        // O helper aplicará todas as migrações de 1 até 6.
        val dbV6 = helper.createDatabase(TEST_DB_NAME, 6).apply {
            // Insere dados de teste relevantes para as tabelas existentes na versão 6,
            // especialmente se as novas tabelas tiverem FKs para elas.
            val disciplinaValues = ContentValues().apply {
                put("id", 101); put("nome_disciplina", "Disc Mig6"); put("cor", Color.RED)
            }
            insert("disciplinas", SQLiteDatabase.CONFLICT_REPLACE, disciplinaValues)

            val turmaValues = ContentValues().apply {
                put("id", 201); put("nome_turma", "Turma Mig6"); put("cor", Color.BLUE)
            }
            insert("turmas", SQLiteDatabase.CONFLICT_REPLACE, turmaValues)

            val horarioValues = ContentValues().apply {
                put("id", 301)
                put("dia_da_semana", Calendar.MONDAY)
                put("hora_inicio", "07:00")
                put("hora_fim", "07:45")
                put("disciplinaId", 101)
                put("turmaId", 201)
            }
            insert("horarios_aula", SQLiteDatabase.CONFLICT_REPLACE, horarioValues)
            close()
        }

        // 2. Abre o banco de dados com a versão 7, aplicando SOMENTE a MIGRATION_6_7.
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB_NAME, 7, true, AppDatabase.MIGRATION_6_7)

        // 3. Verifica se as novas tabelas foram criadas e estão vazias.
        val tabelasParaVerificar = listOf(
            "planos_de_aula",
            "guias_de_aprendizagem",
            "itens_checklist_guia",
            "templates_plano_aula"
        )
        for (tabela in tabelasParaVerificar) {
            val cursor = dbV7.query("SELECT * FROM $tabela")
            assertNotNull("Cursor para $tabela não deve ser nulo.", cursor)
            assertEquals("Tabela $tabela deve estar vazia após migração.", 0, cursor.count)
            cursor.close()
        }

        // 4. Opcional: Tente inserir dados nas novas tabelas para verificar constraints e FKs.
        // Exemplo para planos_de_aula (assumindo disciplinaId=101 e turmaId=201 existem):
        val planoValues = ContentValues().apply {
            put("disciplinaId", 101)
            put("turmaId", 201)
            put("horarioAulaId", 301)
            put("titulo_plano", "Plano Teste Pós-Migração V7")
            put("data_aula", "2024-09-01")
        }
        var insertResult = dbV7.insert("planos_de_aula", SQLiteDatabase.CONFLICT_REPLACE, planoValues)
        assertTrue("Falha ao inserir em planos_de_aula pós-migração.", insertResult != -1L)

        // Exemplo para guias_de_aprendizagem
        val guiaValues = ContentValues().apply {
            put("disciplinaId", 101)
            put("bimestre", "3º Bimestre")
            put("ano", 2024)
            put("titulo_guia", "Guia Teste V7")
        }
        insertResult = dbV7.insert("guias_de_aprendizagem", SQLiteDatabase.CONFLICT_REPLACE, guiaValues)
        assertTrue("Falha ao inserir em guias_de_aprendizagem pós-migração.", insertResult != -1L)
        // Buscar o ID do guia inserido para usar no item do checklist
        val guiaCursor = dbV7.query("SELECT id FROM guias_de_aprendizagem WHERE titulo_guia = 'Guia Teste V7'")
        assertTrue(guiaCursor.moveToFirst())
        val guiaIdInserido = guiaCursor.getInt(guiaCursor.getColumnIndexOrThrow("id"))
        guiaCursor.close()

        // Exemplo para itens_checklist_guia
        val itemChecklistValues = ContentValues().apply {
            put("guiaAprendizagemId", guiaIdInserido)
            put("descricao_item", "Item de teste V7")
            put("concluido", 0)
        }
        insertResult = dbV7.insert("itens_checklist_guia", SQLiteDatabase.CONFLICT_REPLACE, itemChecklistValues)
        assertTrue("Falha ao inserir em itens_checklist_guia pós-migração.", insertResult != -1L)

        // Exemplo para templates_plano_aula
        val templateValues = ContentValues().apply {
            put("nome_template", "Template Básico V7")
        }
        insertResult = dbV7.insert("templates_plano_aula", SQLiteDatabase.CONFLICT_REPLACE, templateValues)
        assertTrue("Falha ao inserir em templates_plano_aula pós-migração.", insertResult != -1L)

        dbV7.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        // 1. Cria o banco de dados com o schema da versão 7.
        val dbV7 = helper.createDatabase(TEST_DB_NAME, 7).apply {
            // Insere dados de teste relevantes para as tabelas existentes na versão 7,
            // especialmente para FKs se a nova tabela 'tarefas' as usasse (disciplinaId, turmaId).
            val disciplinaValues = ContentValues().apply {
                put("id", 201); put("nome_disciplina", "Disc Mig7"); put("cor", Color.GREEN)
            }
            insert("disciplinas", SQLiteDatabase.CONFLICT_REPLACE, disciplinaValues)
            val turmaValues = ContentValues().apply {
                put("id", 301); put("nome_turma", "Turma Mig7"); put("cor", Color.YELLOW)
            }
            insert("turmas", SQLiteDatabase.CONFLICT_REPLACE, turmaValues)
            close()
        }

        // 2. Abre o banco de dados com a versão 8, aplicando SOMENTE a MIGRATION_7_8.
        val dbV8 = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true, AppDatabase.MIGRATION_7_8)

        // 3. Verifica se a tabela 'tarefas' foi criada corretamente e está vazia.
        val cursor = dbV8.query("SELECT * FROM tarefas")
        assertNotNull("Cursor para tarefas não deve ser nulo.", cursor)
        assertEquals("Tabela tarefas deve estar vazia após migração.", 0, cursor.count)
        cursor.close()

        // 4. Opcional: Tente inserir dados na nova tabela 'tarefas'.
        val tarefaValues = ContentValues().apply {
            put("descricao", "Tarefa Teste Pós-Migração V8")
            put("prioridade", 1) // Média
            put("disciplinaId", 201) // FK para a disciplina inserida na v7
            put("turmaId", 301)     // FK para a turma inserida na v7
            put("data_criacao", System.currentTimeMillis())
            put("concluida", 0)
            put("lembrete_configurado", 0)
        }
        val insertResult = dbV8.insert("tarefas", SQLiteDatabase.CONFLICT_REPLACE, tarefaValues)
        assertTrue("Falha ao inserir em tarefas pós-migração.", insertResult != -1L)

        val tarefaCursor = dbV8.query("SELECT * FROM tarefas WHERE descricao = 'Tarefa Teste Pós-Migração V8'")
        assertTrue("Tarefa inserida não encontrada.", tarefaCursor.moveToFirst())
        assertEquals(201, tarefaCursor.getInt(tarefaCursor.getColumnIndexOrThrow("disciplinaId")))
        tarefaCursor.close()

        dbV8.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        // 1. Cria o banco de dados com o schema da versão 8.
        val dbV8 = helper.createDatabase(TEST_DB_NAME, 8).apply {
            // Insere uma tarefa na v8 para ser pai da subtarefa.
            val tarefaValuesV8 = ContentValues().apply {
                put("id", 501) // ID explícito
                put("descricao", "Tarefa Pai V8")
                put("prioridade", 2)
                put("data_criacao", System.currentTimeMillis())
                put("concluida", 0)
                put("lembrete_configurado", 0)
            }
            insert("tarefas", SQLiteDatabase.CONFLICT_REPLACE, tarefaValuesV8)
            close()
        }

        // 2. Abre o banco de dados com a versão 9, aplicando SOMENTE a MIGRATION_8_9.
        val dbV9 = helper.runMigrationsAndValidate(TEST_DB_NAME, 9, true, AppDatabase.MIGRATION_8_9)

        // 3. Verifica se a tabela 'subtarefas' foi criada e está vazia.
        val cursor = dbV9.query("SELECT * FROM subtarefas")
        assertNotNull("Cursor para subtarefas não deve ser nulo.", cursor)
        assertEquals("Tabela subtarefas deve estar vazia após migração.", 0, cursor.count)
        cursor.close()

        // 4. Opcional: Tente inserir dados na nova tabela 'subtarefas'.
        val subtarefaValues = ContentValues().apply {
            put("tarefaId", 501) // FK para a tarefa inserida na v8
            put("descricao_subtarefa", "Subtarefa Teste Pós-Migração V9")
            put("concluida", 0)
            put("ordem", 0)
        }
        val insertResult = dbV9.insert("subtarefas", SQLiteDatabase.CONFLICT_REPLACE, subtarefaValues)
        assertTrue("Falha ao inserir em subtarefas pós-migração.", insertResult != -1L)

        val subtarefaCursor = dbV9.query("SELECT * FROM subtarefas WHERE tarefaId = 501")
        assertTrue("Subtarefa inserida não encontrada.", subtarefaCursor.moveToFirst())
        assertEquals("Subtarefa Teste Pós-Migração V9", subtarefaCursor.getString(subtarefaCursor.getColumnIndexOrThrow("descricao_subtarefa")))
        subtarefaCursor.close()

        // 5. Verifica se os dados da tabela 'tarefas' (da v8) ainda existem.
        val tarefaPreservadaCursor = dbV9.query("SELECT * FROM tarefas WHERE id = 501")
        assertTrue("Tarefa da v8 não foi preservada.", tarefaPreservadaCursor.moveToFirst())
        assertEquals("Tarefa Pai V8", tarefaPreservadaCursor.getString(tarefaPreservadaCursor.getColumnIndexOrThrow("descricao")))
        tarefaPreservadaCursor.close()

        dbV9.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate9To10() {
        // 1. Cria o banco de dados com o schema da versão 9.
        // O helper aplicará todas as migrações de 1 até 9.
        val dbV9 = helper.createDatabase(TEST_DB_NAME, 9).apply {
            // Insere dados de teste relevantes para as tabelas existentes na versão 9,
            // especialmente para FKs se a nova tabela 'anotacoes' as usasse (turmaId).
            val turmaValues = ContentValues().apply {
                put("id", 401); put("nome_turma", "Turma Mig9"); put("cor", Color.BLUE)
            }
            insert("turmas", SQLiteDatabase.CONFLICT_REPLACE, turmaValues)
            // Poderia inserir uma Tarefa e Subtarefa também se quisesse ser exaustivo
            // na verificação de preservação de todos os dados da v9.
            close()
        }

        // 2. Abre o banco de dados com a versão 10, aplicando SOMENTE a MIGRATION_9_10.
        val dbV10 = helper.runMigrationsAndValidate(TEST_DB_NAME, 10, true, AppDatabase.MIGRATION_9_10)

        // 3. Verifica se a tabela 'anotacoes' foi criada corretamente e está vazia.
        val cursor = dbV10.query("SELECT * FROM anotacoes")
        assertNotNull("Cursor para anotacoes não deve ser nulo.", cursor)
        assertEquals("Tabela anotacoes deve estar vazia após migração.", 0, cursor.count)
        cursor.close()

        // 4. Opcional: Tente inserir dados na nova tabela 'anotacoes'.
        val anotacaoValues = ContentValues().apply {
            put("conteudo", "Anotação Teste Pós-Migração V10")
            put("data_criacao", System.currentTimeMillis())
            put("data_modificacao", System.currentTimeMillis())
            put("turmaId", 401) // FK para a turma inserida na v9
            put("cor", Color.GREEN)
        }
        val insertResult = dbV10.insert("anotacoes", SQLiteDatabase.CONFLICT_REPLACE, anotacaoValues)
        assertTrue("Falha ao inserir em anotacoes pós-migração.", insertResult != -1L)

        val anotacaoCursor = dbV10.query("SELECT * FROM anotacoes WHERE conteudo = 'Anotação Teste Pós-Migração V10'")
        assertTrue("Anotação inserida não encontrada.", anotacaoCursor.moveToFirst())
        assertEquals(401, anotacaoCursor.getInt(anotacaoCursor.getColumnIndexOrThrow("turmaId")))
        assertEquals(Color.GREEN, anotacaoCursor.getInt(anotacaoCursor.getColumnIndexOrThrow("cor")))
        anotacaoCursor.close()

        // 5. Verifica se os dados da tabela 'turmas' (da v9) ainda existem.
        val turmaPreservadaCursor = dbV10.query("SELECT * FROM turmas WHERE id = 401")
        assertTrue("Turma da v9 não foi preservada.", turmaPreservadaCursor.moveToFirst())
        assertEquals("Turma Mig9", turmaPreservadaCursor.getString(turmaPreservadaCursor.getColumnIndexOrThrow("nome_turma")))
        turmaPreservadaCursor.close()

        dbV10.close()
    }
}
