package com.agendafocopei.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.TarefaDao
import com.agendafocopei.databinding.ActivityTarefasBinding
import com.agendafocopei.ui.adapter.TarefaAdapter
import com.agendafocopei.ui.dialog.FormularioTarefaFragment // Será criado no próximo subtask
import com.agendafocopei.ui.model.TarefaDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TarefasActivity : AppCompatActivity() { // Removido FormularioTarefaFragment.FormularioTarefaListener por enquanto

    private lateinit var binding: ActivityTarefasBinding
    private lateinit var tarefaAdapter: TarefaAdapter
    private lateinit var tarefaDao: TarefaDao

    // Launcher para o formulário de tarefa
    // private lateinit var formularioTarefaLauncher: ActivityResultLauncher<Intent> // Se FormularioTarefaActivity
    // Se for DialogFragment, a comunicação é via listener

    companion object {
        const val EXTRA_ABRIR_FORMULARIO_NOVA_TAREFA = "abrir_formulario_nova_tarefa"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTarefasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tarefaDao = AppDatabase.getDatabase(applicationContext).tarefaDao()

        setSupportActionBar(binding.toolbarTarefas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeTarefasPendentes() // Por padrão, mostra pendentes

        binding.fabAdicionarTarefa.setOnClickListener {
            abrirFormularioTarefa(null)
        }

        if (intent.getBooleanExtra(EXTRA_ABRIR_FORMULARIO_NOVA_TAREFA, false)) {
            abrirFormularioTarefa(null)
        }
    }

    private fun setupRecyclerView() {
        tarefaAdapter = TarefaAdapter(
            onTarefaCheckChanged = { tarefaDisplay, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val tarefaAtualizada = tarefaDisplay.tarefa.copy(
                        concluida = isChecked,
                        dataConclusao = if (isChecked) System.currentTimeMillis() else null
                    )
                    tarefaDao.atualizar(tarefaAtualizada)
                    // A lista será atualizada pelo Flow
                }
            },
            onEditClickListener = { tarefaDisplay ->
                abrirFormularioTarefa(tarefaDisplay.tarefa.id)
            }
        )
        binding.recyclerViewTarefas.apply {
            adapter = tarefaAdapter
            layoutManager = LinearLayoutManager(this@TarefasActivity)
        }
    }

    private fun observeTarefasPendentes() {
        lifecycleScope.launch {
            tarefaDao.buscarPendentesParaDisplay().collect { listaTarefas ->
                tarefaAdapter.submitList(listaTarefas)
                binding.textViewListaVaziaTarefas.visibility = if (listaTarefas.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewTarefas.visibility = if (listaTarefas.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun abrirFormularioTarefa(tarefaId: Int?) {
        // A implementação do FormularioTarefaFragment será no próximo subtask
        // Por enquanto, um Toast.
        val mensagem = if (tarefaId == null) "Abrir formulário para Nova Tarefa" else "Abrir formulário para Editar Tarefa ID: $tarefaId"
        Toast.makeText(this, "$mensagem (a ser implementado)", Toast.LENGTH_LONG).show()

        // Quando FormularioTarefaFragment estiver pronto:
        // val formularioDialog = FormularioTarefaFragment.newInstance(tarefaId)
        // formularioDialog.setListener(this) // Se TarefasActivity implementar o listener
        // formularioDialog.show(supportFragmentManager, FormularioTarefaFragment.TAG)
    }

    // Implementar FormularioTarefaFragment.FormularioTarefaListener quando o fragmento for criado
    // override fun onTarefaSalva(tarefa: Tarefa) {
    //     lifecycleScope.launch(Dispatchers.IO) {
    //         tarefaDao.inserir(tarefa) // OnConflict REPLACE
    //         runOnUiThread {
    //             Toast.makeText(this@TarefasActivity, "Tarefa salva!", Toast.LENGTH_SHORT).show()
    //         }
    //     }
    // }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
