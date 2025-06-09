package com.agendafocopei.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Subtarefa // Import Subtarefa
import com.agendafocopei.data.SubtarefaDao // Import SubtarefaDao
import com.agendafocopei.data.Tarefa
import com.agendafocopei.data.TarefaDao
import com.agendafocopei.databinding.ActivityTarefasBinding
import com.agendafocopei.ui.adapter.TarefaAdapter
import android.util.Log // Import para Log
import com.agendafocopei.ui.dialog.FormularioTarefaFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat // Para formatar data do lembrete
import java.util.Date // Para formatar data do lembrete
import java.util.Locale // Para formatar data do lembrete

class TarefasActivity : AppCompatActivity(), FormularioTarefaFragment.FormularioTarefaListener {

    private lateinit var binding: ActivityTarefasBinding
    private lateinit var tarefaAdapter: TarefaAdapter
    private lateinit var tarefaDao: TarefaDao
    private lateinit var subtarefaDao: SubtarefaDao // Adicionado SubtarefaDao

    companion object {
        const val EXTRA_ABRIR_FORMULARIO_NOVA_TAREFA = "abrir_formulario_nova_tarefa"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTarefasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(applicationContext)
        tarefaDao = db.tarefaDao()
        subtarefaDao = db.subtarefaDao() // Inicializar SubtarefaDao

        setSupportActionBar(binding.toolbarTarefas)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeTarefasPendentes()

        binding.fabAdicionarTarefa.setOnClickListener {
            abrirFormularioTarefa(null)
        }

        if (intent.getBooleanExtra(EXTRA_ABRIR_FORMULARIO_NOVA_TAREFA, false)) {
            intent.removeExtra(EXTRA_ABRIR_FORMULARIO_NOVA_TAREFA)
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
        val formularioDialog = FormularioTarefaFragment.newInstance(tarefaId)
        formularioDialog.setListener(this)
        formularioDialog.show(supportFragmentManager, FormularioTarefaFragment.TAG)
    }

    // Implementação do FormularioTarefaListener atualizada
    override fun onTarefaSalva(tarefa: Tarefa, subtarefas: List<Subtarefa>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val tarefaIdSalva: Int
            if (tarefa.id == 0) { // Nova tarefa
                tarefaIdSalva = tarefaDao.inserir(tarefa).toInt()
            } else { // Tarefa existente (atualização)
                tarefaDao.atualizar(tarefa)
                tarefaIdSalva = tarefa.id
                // Deleta subtarefas antigas antes de reinserir as atualizadas
                subtarefaDao.deleteTodasPorTarefaId(tarefaIdSalva)
            }

            // Insere as subtarefas (novas ou atualizadas) com o ID correto da tarefa pai
            subtarefas.forEach { subtarefa ->
                // Se a subtarefa da lista já tem um ID (veio do banco), ela será atualizada pelo REPLACE.
                // Se for nova (id=0), será inserida.
                // A abordagem deleteTodasPorTarefaId + inserir todas simplifica, mas pode ser ineficiente para muitas subtarefas.
                // Uma abordagem mais otimizada rastrearia IDs para update/delete/insert individual.
                // Para esta implementação, vamos manter a simplicidade: deletar todas e reinserir.
                // No entanto, ao reinserir, queremos que o Room gere novos IDs para as subtarefas se elas não tinham um,
                // ou atualize se elas tinham. A estratégia de `subtarefa.copy(tarefaId = tarefaIdSalva, id = 0)`
                // força a criação de novas subtarefas sempre, o que pode não ser o ideal se quisermos manter os IDs das subtarefas.
                // Vamos assumir que a lista de subtarefas do fragmento já tem os IDs corretos se forem edições de subtarefas existentes,
                // ou id=0 se forem novas subtarefas adicionadas na UI.
                subtarefaDao.inserir(subtarefa.copy(tarefaId = tarefaIdSalva))
            }

            // Lógica do Lembrete (Placeholder)
            // Usar o objeto 'tarefa' que foi passado para onTarefaSalva,
            // pois ele contém os dados do formulário (lembreteConfigurado, lembreteDateTime).
            // O ID correto para Log/Toast é tarefaIdSalva.
            val tarefaParaLembrete = tarefa.copy(id = tarefaIdSalva)


            if (tarefaParaLembrete.lembreteConfigurado && tarefaParaLembrete.lembreteDateTime != null) {
                if (tarefaParaLembrete.lembreteDateTime!! > System.currentTimeMillis()) {
                    val dataFormatada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(tarefaParaLembrete.lembreteDateTime!!))

                    Log.d("TarefasActivity_Lembrete", "Lembrete AGENDADO para Tarefa ID: ${tarefaParaLembrete.id} ('${tarefaParaLembrete.descricao}') em $dataFormatada")
                    // É importante mostrar o Toast na UI Thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TarefasActivity, "Lembrete para '${tarefaParaLembrete.descricao}' configurado para $dataFormatada.", Toast.LENGTH_LONG).show()
                    }
                    // IMPLEMENTAÇÃO REAL (futuro) com AlarmManager...
                } else {
                    Log.w("TarefasActivity_Lembrete", "Lembrete para Tarefa ID: ${tarefaParaLembrete.id} ('${tarefaParaLembrete.descricao}') não agendado pois a hora do lembrete (${dataFormatadaParaLog(tarefaParaLembrete.lembreteDateTime!!)}) já passou.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TarefasActivity, "Hora do lembrete para '${tarefaParaLembrete.descricao}' já passou. Lembrete não agendado.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.d("TarefasActivity_Lembrete", "Lembrete NÃO configurado para Tarefa ID: ${tarefaParaLembrete.id} ('${tarefaParaLembrete.descricao}').")
                // IMPLEMENTAÇÃO REAL (futuro): Cancelar alarme se existia antes...
            }


            withContext(Dispatchers.Main) {
                // Toast de "Tarefa Salva!" já está aqui, pode ser redundante se o Toast do lembrete for mostrado.
                // Decidir qual Toast manter ou se ambos são ok. Por ora, manterei ambos.
                Toast.makeText(this@TarefasActivity, "Tarefa salva!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função helper para log
    private fun dataFormatadaParaLog(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
