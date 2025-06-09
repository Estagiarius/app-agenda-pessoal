package com.agendafocopei

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Disciplina
import com.agendafocopei.data.DisciplinaDao
import com.agendafocopei.databinding.ActivityMainBinding
import com.agendafocopei.ui.activity.* // Import para as activities de UI
import com.agendafocopei.ui.dialog.ColorPickerDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), ColorPickerDialogFragment.ColorPickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var disciplinaAdapter: DisciplinaAdapter
    private lateinit var disciplinaDao: DisciplinaDao
    private var corSelecionadaDisciplina: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        disciplinaDao = AppDatabase.getDatabase(applicationContext).disciplinaDao()

        disciplinaAdapter = DisciplinaAdapter(emptyList())
        binding.recyclerViewDisciplinas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDisciplinas.adapter = disciplinaAdapter

        lifecycleScope.launch {
            disciplinaDao.buscarTodas().collect { disciplinasDaBase ->
                disciplinaAdapter.updateDisciplinas(disciplinasDaBase)
            }
        }

        binding.buttonAdicionarDisciplina.setOnClickListener {
            val nomeDisciplina = binding.editTextDisciplina.text.toString().trim()
            if (nomeDisciplina.isNotEmpty()) {
                lifecycleScope.launch {
                    val disciplinaExistente = withContext(Dispatchers.IO) {
                        disciplinaDao.buscarPorNome(nomeDisciplina)
                    }
                    if (disciplinaExistente == null) {
                        val novaDisciplina = Disciplina(nome = nomeDisciplina, cor = corSelecionadaDisciplina)
                        withContext(Dispatchers.IO) {
                            disciplinaDao.inserir(novaDisciplina)
                        }
                        binding.editTextDisciplina.text?.clear()
                        binding.editTextDisciplina.error = null
                        corSelecionadaDisciplina = null
                        binding.viewPreviewCorSelecionadaDisciplina.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        binding.editTextDisciplina.error = "Disciplina já existe"
                    }
                }
            } else {
                binding.editTextDisciplina.error = "Nome não pode ser vazio"
            }
        }

        binding.buttonEscolherCorDisciplina.setOnClickListener {
            val dialog = ColorPickerDialogFragment.newInstance(
                preselectedColor = corSelecionadaDisciplina,
                requestTag = "disciplina_color"
            )
            dialog.show(supportFragmentManager, ColorPickerDialogFragment.TAG)
        }

        binding.buttonGerenciarTurmas.setOnClickListener {
            val intent = Intent(this, CadastroTurmaActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGerenciarHorarios.setOnClickListener {
            val intent = Intent(this, GerenciarHorarioActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGerenciarEventos.setOnClickListener {
            val intent = Intent(this, GerenciarEventosActivity::class.java)
            startActivity(intent)
        }

        binding.buttonVerDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        // Novo botão para Planos de Aula
        binding.buttonGerenciarPlanosDeAula.setOnClickListener {
            val intent = Intent(this, PlanosDeAulaGeralActivity::class.java)
            startActivity(intent)
        }

        // Novo botão para Guias de Aprendizagem
        binding.buttonGerenciarGuias.setOnClickListener {
            val intent = Intent(this, GuiasDeAprendizagemActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGerenciarTemplatesPlanoAula.setOnClickListener { // Supondo ID buttonGerenciarTemplatesPlanoAula
            val intent = Intent(this, TemplatesPlanoAulaActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGerenciarTarefas.setOnClickListener { // Supondo ID buttonGerenciarTarefas
            val intent = Intent(this, TarefasActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMinhasAnotacoes.setOnClickListener { // Supondo ID buttonMinhasAnotacoes
            val intent = Intent(this, AnotacoesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onColorSelected(color: Int, tag: String?) {
        if (tag == "disciplina_color") {
            corSelecionadaDisciplina = color
            binding.viewPreviewCorSelecionadaDisciplina.setBackgroundColor(color)
        }
    }
}
