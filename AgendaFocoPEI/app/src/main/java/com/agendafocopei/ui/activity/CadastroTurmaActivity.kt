package com.agendafocopei

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Turma
import com.agendafocopei.data.TurmaDao
import com.agendafocopei.databinding.ActivityCadastroTurmaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CadastroTurmaActivity : AppCompatActivity() {

import android.graphics.Color // Import para Color
import android.util.Log // Import para Log
import com.agendafocopei.ui.dialog.ColorPickerDialogFragment // Import do Dialog

class CadastroTurmaActivity : AppCompatActivity(), ColorPickerDialogFragment.ColorPickerListener {

    private lateinit var binding: ActivityCadastroTurmaBinding
    private lateinit var turmaAdapter: TurmaAdapter
    private lateinit var turmaDao: TurmaDao
    private var corSelecionadaTurma: Int? = null // Variável para cor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroTurmaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar ActionBar para botão "Up"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // O título é definido no AndroidManifest.xml android:label
        // supportActionBar?.title = "Cadastrar Turmas"


        turmaDao = AppDatabase.getDatabase(applicationContext).turmaDao()

        turmaAdapter = TurmaAdapter(emptyList())
        binding.recyclerViewTurmas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTurmas.adapter = turmaAdapter

        lifecycleScope.launch {
            turmaDao.buscarTodas().collect { turmasDaBase ->
                turmaAdapter.updateTurmas(turmasDaBase)
            }
        }

        binding.buttonAdicionarTurma.setOnClickListener {
            val nomeTurma = binding.editTextTurma.text.toString()

            if (isNomeTurmaValido(nomeTurma)) {
                val nomeTurmaTratado = nomeTurma.trim()
                lifecycleScope.launch {
                    val turmaExistente = withContext(Dispatchers.IO) {
                        turmaDao.buscarPorNome(nomeTurmaTratado)
                    }

                    if (turmaExistente == null) {
                        // Usar a corSelecionadaTurma ao criar o objeto
                        val novaTurma = Turma(nome = nomeTurmaTratado, cor = corSelecionadaTurma)
                        withContext(Dispatchers.IO) {
                            turmaDao.inserir(novaTurma)
                        }
                        binding.editTextTurma.text?.clear()
                        binding.editTextTurma.error = null
                        // Resetar a cor selecionada e o preview
                        corSelecionadaTurma = null
                        binding.viewPreviewCorSelecionadaTurma.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        binding.editTextTurma.error = "Turma já existe"
                    }
                }
            } else {
                binding.editTextTurma.error = "Nome não pode ser vazio"
            }
        }

        binding.buttonEscolherCorTurma.setOnClickListener {
            val dialog = ColorPickerDialogFragment.newInstance(
                preselectedColor = corSelecionadaTurma,
                requestTag = "turma_color" // Tag para identificar a origem da chamada
            )
            // dialog.setListener(this) // O Fragment tenta obter o listener da Activity
            dialog.show(supportFragmentManager, ColorPickerDialogFragment.TAG)
        }
    }

    /**
     * Valida se o nome da turma é válido (não vazio após trim).
     * @param nomeTurma O nome da turma a ser validado.
     * @return true se o nome é válido, false caso contrário.
     */
    internal fun isNomeTurmaValido(nomeTurma: String?): Boolean {
        return nomeTurma != null && nomeTurma.trim().isNotEmpty()
    }

    // Implementação do ColorPickerListener
    override fun onColorSelected(color: Int, tag: String?) {
        if (tag == "turma_color") {
            corSelecionadaTurma = color
            binding.viewPreviewCorSelecionadaTurma.setBackgroundColor(color)
        }
    }

    // Para o botão "Up" na ActionBar funcionar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
