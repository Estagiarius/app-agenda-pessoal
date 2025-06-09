package com.agendafocopei.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.Anotacao
import com.agendafocopei.data.AnotacaoDao
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.TurmaDao
import com.agendafocopei.databinding.ActivityAnotacoesBinding
import com.agendafocopei.ui.adapter.AnotacaoAdapter
import com.agendafocopei.ui.dialog.FormularioAnotacaoFragment // Import do DialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnotacoesActivity : AppCompatActivity(), FormularioAnotacaoFragment.FormularioAnotacaoListener {

    private lateinit var binding: ActivityAnotacoesBinding
    private lateinit var anotacaoAdapter: AnotacaoAdapter
    private lateinit var anotacaoDao: AnotacaoDao
    private lateinit var turmaDao: TurmaDao

    companion object {
        const val EXTRA_ABRIR_FORMULARIO_NOVA_ANOTACAO = "abrir_formulario_nova_anotacao"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnotacoesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(applicationContext)
        anotacaoDao = db.anotacaoDao()
        turmaDao = db.turmaDao()

        setSupportActionBar(binding.toolbarAnotacoes)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeAnotacoes()

        binding.fabAdicionarAnotacao.setOnClickListener {
            abrirFormularioAnotacao(null)
        }

        if (intent.getBooleanExtra(EXTRA_ABRIR_FORMULARIO_NOVA_ANOTACAO, false)) {
            intent.removeExtra(EXTRA_ABRIR_FORMULARIO_NOVA_ANOTACAO)
            abrirFormularioAnotacao(null)
        }
    }

    private fun setupRecyclerView() {
        anotacaoAdapter = AnotacaoAdapter(
            turmaDao = turmaDao,
            onItemClickListener = { anotacao ->
                abrirFormularioAnotacao(anotacao.id)
            },
            onEditClickListener = { anotacao ->
                abrirFormularioAnotacao(anotacao.id)
            },
            onDeleteClickListener = { anotacao ->
                mostrarDialogoConfirmacaoDelete(anotacao)
            }
        )
        binding.recyclerViewAnotacoes.apply {
            adapter = anotacaoAdapter
            layoutManager = LinearLayoutManager(this@AnotacoesActivity)
        }
    }

    private fun observeAnotacoes() {
        lifecycleScope.launch {
            anotacaoDao.buscarTodas().collect { listaAnotacoes ->
                anotacaoAdapter.submitList(listaAnotacoes)
                binding.textViewListaVaziaAnotacoes.visibility = if (listaAnotacoes.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewAnotacoes.visibility = if (listaAnotacoes.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun abrirFormularioAnotacao(anotacaoId: Int?) {
        val formularioDialog = FormularioAnotacaoFragment.newInstance(anotacaoId)
        formularioDialog.setListener(this)
        formularioDialog.show(supportFragmentManager, FormularioAnotacaoFragment.TAG)
    }

    // Implementação do FormularioAnotacaoListener
    override fun onAnotacaoSalva(anotacao: Anotacao) {
        lifecycleScope.launch(Dispatchers.IO) {
            anotacaoDao.inserir(anotacao) // OnConflict REPLACE
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AnotacoesActivity, "Anotação salva!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoConfirmacaoDelete(anotacao: Anotacao) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir esta anotação?")
            .setPositiveButton("Excluir") { _, _ ->
                deletarAnotacao(anotacao)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarAnotacao(anotacao: Anotacao) {
        lifecycleScope.launch(Dispatchers.IO) {
            anotacaoDao.deletar(anotacao)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AnotacoesActivity, "Anotação excluída.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
