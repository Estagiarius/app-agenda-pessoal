package com.agendafocopei

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.DisciplinaDao
import com.agendafocopei.data.DisciplinaTurmaCrossRef
import com.agendafocopei.data.DisciplinaTurmaDao
import com.agendafocopei.databinding.ActivityAssociarDisciplinasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssociarDisciplinasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssociarDisciplinasBinding
    private lateinit var disciplinaSelecaoAdapter: DisciplinaSelecaoAdapter
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var disciplinaTurmaDao: DisciplinaTurmaDao

    private var turmaId: Int = -1
    private var nomeTurma: String? = null

    companion object {
        const val EXTRA_TURMA_ID = "com.agendafocopei.EXTRA_TURMA_ID"
        const val EXTRA_NOME_TURMA = "com.agendafocopei.EXTRA_NOME_TURMA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssociarDisciplinasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        turmaId = intent.getIntExtra(EXTRA_TURMA_ID, -1)
        nomeTurma = intent.getStringExtra(EXTRA_NOME_TURMA)

        if (turmaId == -1) {
            Toast.makeText(this, "ID da Turma inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Associar à Turma" // Título inicial
        binding.textViewNomeTurmaContexto.text = "Associar disciplinas à Turma: ${nomeTurma ?: "Desconhecida"}"


        val database = AppDatabase.getDatabase(applicationContext)
        disciplinaDao = database.disciplinaDao()
        disciplinaTurmaDao = database.disciplinaTurmaDao()

        setupRecyclerView()
        loadData()

        binding.buttonSalvarAssociacoes.setOnClickListener {
            salvarAssociacoes()
        }
    }

    private fun setupRecyclerView() {
        disciplinaSelecaoAdapter = DisciplinaSelecaoAdapter()
        binding.recyclerViewDisciplinasSelecao.apply {
            adapter = disciplinaSelecaoAdapter
            layoutManager = LinearLayoutManager(this@AssociarDisciplinasActivity)
        }
    }

import android.view.View // Importar View para controle de visibilidade

class AssociarDisciplinasActivity : AppCompatActivity() {

    // ... (variáveis existentes) ...

    companion object {
        const val EXTRA_TURMA_ID = "com.agendafocopei.EXTRA_TURMA_ID"
        const val EXTRA_NOME_TURMA = "com.agendafocopei.EXTRA_NOME_TURMA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssociarDisciplinasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        turmaId = intent.getIntExtra(EXTRA_TURMA_ID, -1)
        nomeTurma = intent.getStringExtra(EXTRA_NOME_TURMA)

        if (turmaId == -1) {
            Toast.makeText(this, "ID da Turma inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Associar à Turma"
        binding.textViewNomeTurmaContexto.text = "Associar disciplinas à Turma: ${nomeTurma ?: "Desconhecida"}"

        val database = AppDatabase.getDatabase(applicationContext)
        disciplinaDao = database.disciplinaDao()
        disciplinaTurmaDao = database.disciplinaTurmaDao()

        setupRecyclerView()
        loadDataWithCombine() // Renomeado para clareza

        binding.buttonSalvarAssociacoes.setOnClickListener {
            salvarAssociacoes()
        }
    }

    private fun setupRecyclerView() {
        disciplinaSelecaoAdapter = DisciplinaSelecaoAdapter()
        binding.recyclerViewDisciplinasSelecao.apply {
            adapter = disciplinaSelecaoAdapter
            layoutManager = LinearLayoutManager(this@AssociarDisciplinasActivity)
        }
    }

    private fun loadDataWithCombine() {
        lifecycleScope.launch {
            // Combina o Flow de todas as disciplinas com o Flow de associações existentes para a turma
            disciplinaDao.buscarTodas()
                .combine(disciplinaTurmaDao.getAssociacoesParaTurma(turmaId)) { todasDisciplinas, associacoesExistentes ->
                    // Mapeia as associações existentes para um Set de IDs de disciplina
                    val associacoesExistentesIds = associacoesExistentes.map { it.disciplinaId }.toSet()
                    // Retorna um Pair com a lista de todas as disciplinas e o Set de IDs associados
                    Pair(todasDisciplinas, associacoesExistentesIds)
                }
                .collect { (todasDisciplinas, associacoesExistentesIds) ->
                    if (todasDisciplinas.isEmpty()) {
                        binding.textViewMensagemDisciplinasVazias.visibility = View.VISIBLE
                        binding.recyclerViewDisciplinasSelecao.visibility = View.GONE
                        binding.buttonSalvarAssociacoes.isEnabled = false
                    } else {
                        binding.textViewMensagemDisciplinasVazias.visibility = View.GONE
                        binding.recyclerViewDisciplinasSelecao.visibility = View.VISIBLE
                        binding.buttonSalvarAssociacoes.isEnabled = true
                        disciplinaSelecaoAdapter.setDisciplinas(todasDisciplinas, associacoesExistentesIds)
                    }
                }
        }
    }

    private fun salvarAssociacoes() {
        // Verifica se o botão está habilitado (ou seja, se há disciplinas para associar)
        if (!binding.buttonSalvarAssociacoes.isEnabled) {
            Toast.makeText(this, "Nenhuma disciplina disponível para associação.", Toast.LENGTH_SHORT).show()
            return
        }

        val disciplinasSelecionadasIds = disciplinaSelecaoAdapter.getDisciplinasSelecionadasIds()

        lifecycleScope.launch { // IO é implícito pelas funções suspend do DAO ou pode ser especificado
            withContext(Dispatchers.IO) {
                // 1. Deletar todas as associações existentes para esta turma
                disciplinaTurmaDao.deletarAssociacoesPorTurmaId(turmaId)

                // 2. Inserir as novas associações
                disciplinasSelecionadasIds.forEach { disciplinaId ->
                    disciplinaTurmaDao.inserirAssociacao(
                        DisciplinaTurmaCrossRef(disciplinaId = disciplinaId, turmaId = turmaId)
                    )
                }
            }
            // Operações de UI na Main thread
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AssociarDisciplinasActivity, "Associações salvas!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish() // Fecha a activity após salvar
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
