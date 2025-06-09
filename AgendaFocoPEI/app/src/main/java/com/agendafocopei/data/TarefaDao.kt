package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TarefaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(tarefa: Tarefa): Long

    @Update
    suspend fun atualizar(tarefa: Tarefa)

    @Delete
    suspend fun deletar(tarefa: Tarefa)

    @Query("DELETE FROM tarefas WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM tarefas WHERE id = :id")
    suspend fun buscarPorId(id: Int): Tarefa?

    @Query("SELECT * FROM tarefas ORDER BY data_criacao DESC")
    fun buscarTodas(): Flow<List<Tarefa>>

    // Ordena pendentes por prioridade (mais alta primeiro), depois por prazo (mais cedo primeiro)
    @Query("SELECT * FROM tarefas WHERE concluida = 0 ORDER BY prioridade DESC, prazo_data ASC, prazo_hora ASC, data_criacao ASC")
    fun buscarPendentesOrdenadas(): Flow<List<Tarefa>>

    @Query("SELECT * FROM tarefas WHERE concluida = 1 ORDER BY data_conclusao DESC")
    fun buscarConcluidasOrdenadas(): Flow<List<Tarefa>>

    // Para RF-02.3: Tarefas urgentes (prazo hoje ou amanhã, não concluídas)
    // dataHojeNoFormatoYYYYMMDD e dataAmanhaNoFormatoYYYYMMDD precisam ser passadas como argumento
    @Query("""
        SELECT * FROM tarefas
        WHERE concluida = 0 AND prazo_data IS NOT NULL AND
              (prazo_data = :dataHojeNoFormatoYYYYMMDD OR prazo_data = :dataAmanhaNoFormatoYYYYMMDD)
        ORDER BY prazo_data ASC, prazo_hora ASC, prioridade DESC
        LIMIT 5
    """)
    fun buscarTarefasUrgentes(dataHojeNoFormatoYYYYMMDD: String, dataAmanhaNoFormatoYYYYMMDD: String): Flow<List<Tarefa>>

    // Query para buscar tarefas com prazo em um dia específico
    @Query("SELECT * FROM tarefas WHERE prazo_data = :data ORDER BY prazo_hora ASC, prioridade DESC")
    fun buscarPorDataDePrazo(data: String): Flow<List<Tarefa>>

    // Query para buscar tarefas por disciplina
    @Query("SELECT * FROM tarefas WHERE disciplinaId = :disciplinaId ORDER BY prazo_data ASC, prioridade DESC")
    fun buscarPorDisciplina(disciplinaId: Int): Flow<List<Tarefa>>

    // Query para buscar tarefas por turma
    @Query("SELECT * FROM tarefas WHERE turmaId = :turmaId ORDER BY prazo_data ASC, prioridade DESC")
    fun buscarPorTurma(turmaId: Int): Flow<List<Tarefa>>

    @Transaction
    @Query("""
        SELECT t.*,
               d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina,
               tu.nome_turma AS nome_turma, tu.cor AS cor_turma
        FROM tarefas t
        LEFT JOIN disciplinas d ON t.disciplinaId = d.id
        LEFT JOIN turmas tu ON t.turmaId = tu.id
        WHERE t.concluida = 0
        ORDER BY t.prioridade DESC, t.prazo_data ASC, t.prazo_hora ASC, t.data_criacao ASC
    """)
    fun buscarPendentesParaDisplay(): Flow<List<com.agendafocopei.ui.model.TarefaDisplay>>

    @Transaction
    @Query("""
        SELECT t.*,
               d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina,
               tu.nome_turma AS nome_turma, tu.cor AS cor_turma
        FROM tarefas t
        LEFT JOIN disciplinas d ON t.disciplinaId = d.id
        LEFT JOIN turmas tu ON t.turmaId = tu.id
        WHERE t.concluida = 1
        ORDER BY t.data_conclusao DESC
    """)
    fun buscarConcluidasParaDisplay(): Flow<List<com.agendafocopei.ui.model.TarefaDisplay>>

    @Transaction
    @Query("""
        SELECT t.*,
               d.nome_disciplina AS nome_disciplina, d.cor AS cor_disciplina,
               tu.nome_turma AS nome_turma, tu.cor AS cor_turma
        FROM tarefas t
        LEFT JOIN disciplinas d ON t.disciplinaId = d.id
        LEFT JOIN turmas tu ON t.turmaId = tu.id
        WHERE t.concluida = 0 AND t.prazo_data IS NOT NULL AND
              (t.prazo_data = :dataHoje OR t.prazo_data = :dataAmanha)
        ORDER BY t.prazo_data ASC, t.prazo_hora ASC, t.prioridade DESC
        LIMIT 5
    """)
    fun buscarUrgentesParaDisplay(dataHoje: String, dataAmanha: String): Flow<List<com.agendafocopei.ui.model.TarefaDisplay>>
}
