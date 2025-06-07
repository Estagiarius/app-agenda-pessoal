package com.agendafocopei.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.EventoRecorrenteDao
import com.agendafocopei.data.EventoRecorrente // Import da entidade
import com.agendafocopei.databinding.ActivityGerenciarEventosBinding
import com.agendafocopei.ui.adapter.EventoRecorrenteAdapter
import com.agendafocopei.ui.dialog.FormularioEventoDialogFragment // Import do Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GerenciarEventosActivity : AppCompatActivity(), FormularioEventoDialogFragment.FormularioEventoListener {

    private lateinit var binding: ActivityGerenciarEventosBinding
    private lateinit var eventoAdapter: EventoRecorrenteAdapter
    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarEventosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventoRecorrenteDao = AppDatabase.getDatabase(applicationContext).eventoRecorrenteDao()

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbarEventos)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // O título é definido no layout

        setupRecyclerView()
        observeEventos()

        binding.fabAdicionarEvento.setOnClickListener {
            val dialog = FormularioEventoDialogFragment.newInstance(null) // null para novo evento
            dialog.setListener(this)
            dialog.show(supportFragmentManager, FormularioEventoDialogFragment.TAG)
        }
    }

    private fun setupRecyclerView() {
        eventoAdapter = EventoRecorrenteAdapter(
            onEditClickListener = { evento ->
                val dialog = FormularioEventoDialogFragment.newInstance(evento.id)
                dialog.setListener(this)
                dialog.show(supportFragmentManager, FormularioEventoDialogFragment.TAG)
            },
            onDeleteClickListener = { evento ->
                mostrarDialogoConfirmacaoDelete(evento)
            }
        )
        binding.recyclerViewEventos.apply {
            adapter = eventoAdapter
            layoutManager = LinearLayoutManager(this@GerenciarEventosActivity)
        }
    }

    private fun observeEventos() {
        lifecycleScope.launch {
            eventoRecorrenteDao.buscarTodosOrdenados().collect { listaEventos ->
                eventoAdapter.submitList(listaEventos)
                if (listaEventos.isEmpty()) {
                    binding.textViewListaVaziaEventos.visibility = View.VISIBLE
                    binding.recyclerViewEventos.visibility = View.GONE
                } else {
                    binding.textViewListaVaziaEventos.visibility = View.GONE
                    binding.recyclerViewEventos.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacaoDelete(evento: com.agendafocopei.data.EventoRecorrente) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o evento \"${evento.nomeEvento}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deletarEvento(evento.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarEvento(eventoId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            eventoRecorrenteDao.deleteById(eventoId)
            // A UI será atualizada automaticamente pelo Flow em observeEventos()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Implementação do FormularioEventoListener
    override fun onEventoSalvo(evento: EventoRecorrente) {
        lifecycleScope.launch(Dispatchers.IO) {
            eventoRecorrenteDao.inserir(evento) // OnConflictStrategy.REPLACE trata inserção e atualização
            // A UI deve atualizar automaticamente via Flow em observeEventos()
            runOnUiThread {
                Toast.makeText(this@GerenciarEventosActivity, "Evento salvo!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
