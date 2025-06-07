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
        EventoRecorrente::class // <<< ADICIONADO EventoRecorrente
    ],
    version = 5, // <<< INCREMENTADO PARA 5
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun disciplinaDao(): DisciplinaDao
    abstract fun turmaDao(): TurmaDao
    abstract fun disciplinaTurmaDao(): DisciplinaTurmaDao
    abstract fun horarioAulaDao(): HorarioAulaDao
    abstract fun eventoRecorrenteDao(): EventoRecorrenteDao // <<< NOVO DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `disciplina_turma_cross_ref` (
                        `disciplinaId` INTEGER NOT NULL,
                        `turmaId` INTEGER NOT NULL,
                        PRIMARY KEY(`disciplinaId`, `turmaId`),
                        FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
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
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `horarios_aula` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dia_da_semana` INTEGER NOT NULL,
                        `hora_inicio` TEXT NOT NULL,
                        `hora_fim` TEXT NOT NULL,
                        `disciplinaId` INTEGER NOT NULL,
                        `turmaId` INTEGER NOT NULL,
                        `sala_aula` TEXT,
                        FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`turmaId`) REFERENCES `turmas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_horarios_aula_disciplinaId` ON `horarios_aula` (`disciplinaId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_horarios_aula_turmaId` ON `horarios_aula` (`turmaId`)")
            }
        }

        // NOVA MIGRAÇÃO DE 4 PARA 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `eventos_recorrentes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome_evento` TEXT NOT NULL,
                        `dia_da_semana` INTEGER NOT NULL,
                        `hora_inicio` TEXT NOT NULL,
                        `hora_fim` TEXT NOT NULL,
                        `sala_local` TEXT,
                        `cor` INTEGER,
                        `observacoes` TEXT
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenda_foco_pei_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5) // <<< ADICIONAR NOVA MIGRAÇÃO
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
