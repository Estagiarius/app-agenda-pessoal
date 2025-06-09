package com.agendafocopei.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.PlanoDeAula
import com.agendafocopei.data.PlanoDeAulaDao
import com.agendafocopei.databinding.ActivityPlanosDeAulaGeralBinding
import com.agendafocopei.ui.adapter.PlanoDeAulaAdapter
import com.agendafocopei.ui.dialog.FormularioPlanoDeAulaFragment
import com.agendafocopei.ui.model.PlanoDeAulaDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlanosDeAulaGeralActivity : AppCompatActivity(), FormularioPlanoDeAulaFragment.FormularioPlanoListener {

    private lateinit var binding: ActivityPlanosDeAulaGeralBinding
    private lateinit var planoDeAulaAdapter: PlanoDeAulaAdapter
    private lateinit var planoDeAulaDao: PlanoDeAulaDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanosDeAulaGeralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planoDeAulaDao = AppDatabase.getDatabase(applicationContext).planoDeAulaDao()

        setSupportActionBar(binding.toolbarPlanosDeAula)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Título já definido no XML

        setupRecyclerView()
        observePlanosDeAula()

        binding.fabAdicionarPlanoDeAula.setOnClickListener {
            FormularioPlanoDeAulaFragment.newInstance(
                planoId = null, // Novo plano
                horarioAulaId = null, // Não vinculado a um horário específico inicialmente
                disciplinaIdPredefinida = null, // Sem predefinição
                turmaIdPredefinida = null,
                dataPredefinida = null
            ).also {
                it.setListener(this)
                it.show(supportFragmentManager, FormularioPlanoDeAulaFragment.TAG)
            }
        }
    }

    private fun setupRecyclerView() {
        planoDeAulaAdapter = PlanoDeAulaAdapter(
            onEditClickListener = { planoDisplay ->
                FormularioPlanoDeAulaFragment.newInstance(
                    planoId = planoDisplay.planoDeAula.id,
                    // Outros IDs predefinidos podem ser passados se disponíveis/necessários
                    // horarioAulaId = planoDisplay.planoDeAula.horarioAulaId,
                    // disciplinaIdPredefinida = planoDisplay.planoDeAula.disciplinaId,
                    // turmaIdPredefinida = planoDisplay.planoDeAula.turmaId,
                    // dataPredefinida = planoDisplay.planoDeAula.dataAula
                ).also {
                    it.setListener(this)
                    it.show(supportFragmentManager, FormularioPlanoDeAulaFragment.TAG)
                }
            },
            onDeleteClickListener = { planoDisplay ->
                mostrarDialogoConfirmacaoDelete(planoDisplay.planoDeAula)
            },
            onItemClickListener = { planoDisplay ->
                // Lógica para visualizar detalhes do plano, se houver uma tela de detalhes.
                // Por enquanto, pode ser um Toast ou abrir o modo de edição.
                Toast.makeText(this, "Visualizar: ${planoDisplay.planoDeAula.tituloPlano ?: "Plano"}", Toast.LENGTH_SHORT).show()
                 FormularioPlanoDeAulaFragment.newInstance(planoDisplay.planoDeAula.id)
                     .also { it.setListener(this) }
                     .show(supportFragmentManager, FormularioPlanoDeAulaFragment.TAG)
            }
        )
        binding.recyclerViewPlanosDeAula.apply {
            adapter = planoDeAulaAdapter
            layoutManager = LinearLayoutManager(this@PlanosDeAulaGeralActivity)
        }
    }

    private fun observePlanosDeAula() {
        lifecycleScope.launch {
            planoDeAulaDao.buscarTodosParaDisplay().collect { listaPlanos ->
                planoDeAulaAdapter.submitList(listaPlanos)
                if (listaPlanos.isEmpty()) {
                    binding.textViewListaVaziaPlanos.visibility = View.VISIBLE
                    binding.recyclerViewPlanosDeAula.visibility = View.GONE
                } else {
                    binding.textViewListaVaziaPlanos.visibility = View.GONE
                    binding.recyclerViewPlanosDeAula.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacaoDelete(plano: PlanoDeAula) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o plano \"${plano.tituloPlano ?: "Plano sem título"}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deletarPlano(plano)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarPlano(plano: PlanoDeAula) {
        lifecycleScope.launch(Dispatchers.IO) {
            planoDeAulaDao.deletar(plano)
            // A UI será atualizada automaticamente pelo Flow
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PlanosDeAulaGeralActivity, "Plano excluído.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPlanoSalvo(plano: PlanoDeAula) {
        lifecycleScope.launch(Dispatchers.IO) {
            planoDeAulaDao.inserir(plano) // REPLACE strategy lida com insert/update
            withContext(Dispatchers.Main) {
                 Toast.makeText(this@PlanosDeAulaGeralActivity, "Plano salvo!", Toast.LENGTH_SHORT).show()
            }
            // A UI será atualizada automaticamente pelo Flow
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
