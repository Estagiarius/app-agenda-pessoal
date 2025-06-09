package com.agendafocopei.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Disciplina::class,
        Turma::class,
        DisciplinaTurmaCrossRef::class,
        HorarioAula::class,
        Evento::class,
        PlanoDeAula::class,
        GuiaDeAprendizagem::class,
        ItemChecklistGuia::class,
        TemplatePlanoAula::class,
        Tarefa::class,
        Subtarefa::class,
        Anotacao::class // <<< ADICIONADO Anotacao
    ],
    version = 10, // <<< INCREMENTADO PARA 10
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun disciplinaDao(): DisciplinaDao
    abstract fun turmaDao(): TurmaDao
    abstract fun disciplinaTurmaDao(): DisciplinaTurmaDao
    abstract fun horarioAulaDao(): HorarioAulaDao
    abstract fun eventoDao(): EventoDao
    abstract fun planoDeAulaDao(): PlanoDeAulaDao
    abstract fun guiaDeAprendizagemDao(): GuiaDeAprendizagemDao
    abstract fun itemChecklistGuiaDao(): ItemChecklistGuiaDao
    abstract fun templatePlanoAulaDao(): TemplatePlanoAulaDao
    abstract fun tarefaDao(): TarefaDao
    abstract fun subtarefaDao(): SubtarefaDao
    abstract fun anotacaoDao(): AnotacaoDao // <<< NOVO DAO


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ... (Migrações MIGRATION_1_2 até MIGRATION_8_9 permanecem as mesmas) ...
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `disciplina_turma_cross_ref` (`disciplinaId` INTEGER NOT NULL, `turmaId` INTEGER NOT NULL, PRIMARY KEY(`disciplinaId`, `turmaId`), FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_disciplina_turma_cross_ref_disciplinaId` ON `disciplina_turma_cross_ref` (`disciplinaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_disciplina_turma_cross_ref_turmaId` ON `disciplina_turma_cross_ref` (`turmaId`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
             override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE disciplinas ADD COLUMN cor INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE turmas ADD COLUMN cor INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `horarios_aula` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dia_da_semana` INTEGER NOT NULL, `hora_inicio` TEXT NOT NULL, `hora_fim` TEXT NOT NULL, `disciplinaId` INTEGER NOT NULL, `turmaId` INTEGER NOT NULL, `sala_aula` TEXT, FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_horarios_aula_disciplinaId` ON `horarios_aula` (`disciplinaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_horarios_aula_turmaId` ON `horarios_aula` (`turmaId`)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `eventos_recorrentes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nome_evento` TEXT NOT NULL, `dia_da_semana` INTEGER NOT NULL, `hora_inicio` TEXT NOT NULL, `hora_fim` TEXT NOT NULL, `sala_local` TEXT, `cor` INTEGER, `observacoes` TEXT)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE eventos_recorrentes ADD COLUMN data_especifica TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `planos_de_aula` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `horarioAulaId` INTEGER, `data_aula` TEXT, `disciplinaId` INTEGER NOT NULL, `turmaId` INTEGER, `titulo_plano` TEXT, `texto_plano` TEXT, `caminho_anexo` TEXT, `tipo_anexo` TEXT, `template_usado_id` INTEGER, FOREIGN KEY(`horarioAulaId`) REFERENCES `horarios_aula`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_planos_de_aula_horarioAulaId` ON `planos_de_aula` (`horarioAulaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_planos_de_aula_disciplinaId` ON `planos_de_aula` (`disciplinaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_planos_de_aula_turmaId` ON `planos_de_aula` (`turmaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_planos_de_aula_template_usado_id` ON `planos_de_aula` (`template_usado_id`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `guias_de_aprendizagem` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bimestre` TEXT NOT NULL, `ano` INTEGER NOT NULL, `disciplinaId` INTEGER NOT NULL, `caminho_anexo_guia` TEXT, `tipo_anexo_guia` TEXT, `titulo_guia` TEXT, FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_guias_de_aprendizagem_disciplinaId` ON `guias_de_aprendizagem` (`disciplinaId`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `itens_checklist_guia` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `guiaAprendizagemId` INTEGER NOT NULL, `descricao_item` TEXT NOT NULL, `concluido` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`guiaAprendizagemId`) REFERENCES `guias_de_aprendizagem`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_itens_checklist_guia_guiaAprendizagemId` ON `itens_checklist_guia` (`guiaAprendizagemId`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `templates_plano_aula` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nome_template` TEXT NOT NULL, `campo_habilidades` TEXT, `campo_recursos` TEXT, `campo_metodologia` TEXT, `campo_avaliacao` TEXT, `outros_campos` TEXT)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `tarefas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `descricao` TEXT NOT NULL, `prazo_data` TEXT, `prazo_hora` TEXT, `prioridade` INTEGER NOT NULL, `disciplinaId` INTEGER, `turmaId` INTEGER, `concluida` INTEGER NOT NULL DEFAULT 0, `data_criacao` INTEGER NOT NULL DEFAULT 0, `data_conclusao` INTEGER, `lembrete_configurado` INTEGER NOT NULL DEFAULT 0, `lembrete_datetime` INTEGER, FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tarefas_disciplinaId` ON `tarefas` (`disciplinaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tarefas_turmaId` ON `tarefas` (`turmaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tarefas_prazo_data` ON `tarefas` (`prazo_data`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tarefas_concluida` ON `tarefas` (`concluida`)")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `subtarefas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tarefaId` INTEGER NOT NULL, `descricao_subtarefa` TEXT NOT NULL, `concluida` INTEGER NOT NULL DEFAULT 0, `ordem` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`tarefaId`) REFERENCES `tarefas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_subtarefas_tarefaId` ON `subtarefas` (`tarefaId`)")
            }
        }

        // NOVA MIGRAÇÃO DE 9 PARA 10
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `anotacoes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conteudo` TEXT NOT NULL,
                        `data_criacao` INTEGER NOT NULL DEFAULT 0,
                        `data_modificacao` INTEGER NOT NULL DEFAULT 0,
                        `cor` INTEGER,
                        `turmaId` INTEGER,
                        `aluno_nome` TEXT,
                        `tags_string` TEXT,
                        FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_anotacoes_turmaId` ON `anotacoes` (`turmaId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenda_foco_pei_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10 // <<< ADICIONAR NOVA MIGRAÇÃO
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
