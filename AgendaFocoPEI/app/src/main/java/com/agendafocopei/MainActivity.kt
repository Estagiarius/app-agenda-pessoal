package com.agendafocopei

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Disciplina
import com.agendafocopei.data.DisciplinaDao
import com.agendafocopei.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
import android.graphics.Color // Import para Color
import android.util.Log // Import para Log
import com.agendafocopei.ui.dialog.ColorPickerDialogFragment // Import do Dialog

class MainActivity : AppCompatActivity(), ColorPickerDialogFragment.ColorPickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var disciplinaAdapter: DisciplinaAdapter
    private lateinit var disciplinaDao: DisciplinaDao
    private var corSelecionadaDisciplina: Int? = null // Variável para cor

    // A lista em memória que o adapter usa será atualizada pelo Flow do Room.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o DAO
        disciplinaDao = AppDatabase.getDatabase(applicationContext).disciplinaDao()

        // Configura o Adapter e RecyclerView
        // Inicializa com uma lista vazia, que será preenchida pelo Flow
        disciplinaAdapter = DisciplinaAdapter(emptyList())
        binding.recyclerViewDisciplinas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDisciplinas.adapter = disciplinaAdapter

        // Coleta o Flow de disciplinas do banco de dados
        lifecycleScope.launch {
            disciplinaDao.buscarTodas().collect { disciplinasDaBase ->
                // Atualiza o adapter com as novas disciplinas
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
                        // Usar a corSelecionadaDisciplina ao criar o objeto
                        val novaDisciplina = Disciplina(nome = nomeDisciplina, cor = corSelecionadaDisciplina)
                        withContext(Dispatchers.IO) {
                            disciplinaDao.inserir(novaDisciplina)
                        }
                        binding.editTextDisciplina.text?.clear()
                        binding.editTextDisciplina.error = null
                        // Resetar a cor selecionada e o preview
                        corSelecionadaDisciplina = null
                        binding.viewPreviewCorSelecionadaDisciplina.setBackgroundColor(Color.TRANSPARENT)
                        // A UI da lista será atualizada automaticamente pelo Flow
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
                requestTag = "disciplina_color" // Tag para identificar a origem da chamada
            )
            // dialog.setListener(this) // O Fragment tenta obter o listener da Activity
            dialog.show(supportFragmentManager, ColorPickerDialogFragment.TAG)
        }

        binding.buttonGerenciarTurmas.setOnClickListener {
            val intent = android.content.Intent(this, CadastroTurmaActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGerenciarHorarios.setOnClickListener {
            val intent = android.content.Intent(this, com.agendafocopei.ui.activity.GerenciarHorarioActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGerenciarEventos.setOnClickListener {
            val intent = android.content.Intent(this, com.agendafocopei.ui.activity.GerenciarEventosActivity::class.java)
            startActivity(intent)
        }
    }

    // Não precisamos mais de:
    // ... (comentários existentes)

    // Implementação do ColorPickerListener
    override fun onColorSelected(color: Int, tag: String?) {
        if (tag == "disciplina_color") {
            corSelecionadaDisciplina = color
            binding.viewPreviewCorSelecionadaDisciplina.setBackgroundColor(color)
        }
    }
}
