package com.agendafocopei.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplinaTurmaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirAssociacao(crossRef: DisciplinaTurmaCrossRef)

    @Delete
    suspend fun deletarAssociacao(crossRef: DisciplinaTurmaCrossRef)

    /**
     * Deleta todas as associações para uma turma específica.
     * Útil, por exemplo, ao redefinir as disciplinas de uma turma.
     */
    @Query("DELETE FROM disciplina_turma_cross_ref WHERE turmaId = :turmaId")
    suspend fun deletarAssociacoesPorTurmaId(turmaId: Int)

    /**
     * Deleta todas as associações para uma disciplina específica.
     * Útil, por exemplo, ao redefinir as turmas de uma disciplina.
     */
    @Query("DELETE FROM disciplina_turma_cross_ref WHERE disciplinaId = :disciplinaId")
    suspend fun deletarAssociacoesPorDisciplinaId(disciplinaId: Int)

    /**
     * Obtém todas as disciplinas associadas a uma turma específica.
     */
    @Transaction // Garante que a operação de junção seja atômica.
    @Query("SELECT * FROM disciplinas INNER JOIN disciplina_turma_cross_ref ON disciplinas.id = disciplina_turma_cross_ref.disciplinaId WHERE disciplina_turma_cross_ref.turmaId = :turmaId ORDER BY disciplinas.nome_disciplina ASC")
    fun getDisciplinasForTurma(turmaId: Int): Flow<List<Disciplina>>

    /**
     * Obtém todas as turmas associadas a uma disciplina específica.
     */
    @Transaction
    @Query("SELECT * FROM turmas INNER JOIN disciplina_turma_cross_ref ON turmas.id = disciplina_turma_cross_ref.turmaId WHERE disciplina_turma_cross_ref.disciplinaId = :disciplinaId ORDER BY turmas.nome_turma ASC")
    fun getTurmasForDisciplina(disciplinaId: Int): Flow<List<Turma>>

    /**
     * Obtém todas as referências cruzadas para uma turma específica.
     * Pode ser útil para saber quais associações (IDs de disciplina) existem para uma turma.
     */
    @Query("SELECT * FROM disciplina_turma_cross_ref WHERE turmaId = :turmaId")
    fun getAssociacoesParaTurma(turmaId: Int): Flow<List<DisciplinaTurmaCrossRef>>

    /**
     * Obtém todas as referências cruzadas para uma disciplina específica.
     */
    @Query("SELECT * FROM disciplina_turma_cross_ref WHERE disciplinaId = :disciplinaId")
    fun getAssociacoesParaDisciplina(disciplinaId: Int): Flow<List<DisciplinaTurmaCrossRef>>
}
