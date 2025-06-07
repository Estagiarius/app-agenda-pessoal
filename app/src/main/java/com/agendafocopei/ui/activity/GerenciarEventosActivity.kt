package com.agendafocopei.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Evento
import com.agendafocopei.data.EventoDao
import com.agendafocopei.databinding.ActivityGerenciarEventosBinding
import com.agendafocopei.ui.adapter.EventoAdapter // ATUALIZADO AQUI
import com.agendafocopei.ui.dialog.FormularioEventoDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GerenciarEventosActivity : AppCompatActivity(), FormularioEventoDialogFragment.FormularioEventoListener {

    private lateinit var binding: ActivityGerenciarEventosBinding
    private lateinit var eventoAdapter: EventoAdapter // ATUALIZADO AQUI
    private lateinit var eventoDao: EventoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarEventosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Corrigido para usar eventoDao()
        eventoDao = AppDatabase.getDatabase(applicationContext).eventoDao()

        setSupportActionBar(binding.toolbarEventos)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // O título é definido no layout como "Eventos Recorrentes", pode ser ajustado se necessário

        setupRecyclerView()
        observeEventos()

        binding.fabAdicionarEvento.setOnClickListener {
            val dialog = FormularioEventoDialogFragment.newInstance(null) // Para novo evento
            dialog.setListener(this)
            dialog.show(supportFragmentManager, FormularioEventoDialogFragment.TAG)
        }
    }

    private fun setupRecyclerView() {
        eventoAdapter = EventoAdapter( // ATUALIZADO AQUI
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
            // buscarTodosOrdenados() agora retorna Flow<List<Evento>>
            eventoDao.buscarTodosOrdenados().collect { listaEventos ->
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

    private fun mostrarDialogoConfirmacaoDelete(evento: Evento) { // Atualizado para Evento
        AlertDialog.Builder(this)
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
            eventoDao.deleteById(eventoId)
            // A UI será atualizada automaticamente pelo Flow em observeEventos()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Implementação do FormularioEventoListener
    override fun onEventoSalvo(evento: Evento) { // Atualizado para Evento
        lifecycleScope.launch(Dispatchers.IO) {
            eventoDao.inserir(evento) // OnConflictStrategy.REPLACE trata inserção e atualização
            runOnUiThread {
                Toast.makeText(this@GerenciarEventosActivity, "Evento salvo!", Toast.LENGTH_SHORT).show()
            }
            // A UI deve atualizar automaticamente via Flow em observeEventos()
        }
    }
}
