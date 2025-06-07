package com.agendafocopei.ui.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase // Import para o DB
import com.agendafocopei.data.HorarioAula // Import para a entidade
import com.agendafocopei.data.HorarioAulaDao // Import para o DAO
import com.agendafocopei.databinding.ActivityGerenciarHorarioBinding
import com.agendafocopei.ui.adapter.HorarioAdapter
import com.agendafocopei.ui.dialog.FormularioHorarioDialogFragment // Import do Dialog
import kotlinx.coroutines.Dispatchers // Import para Coroutines
import kotlinx.coroutines.launch // Import para Coroutines
import androidx.lifecycle.lifecycleScope // Import para lifecycleScope
import android.view.View // Import para View
import androidx.appcompat.app.AlertDialog // Import para AlertDialog


class GerenciarHorarioActivity : AppCompatActivity(), FormularioHorarioDialogFragment.FormularioHorarioListener {

    private lateinit var binding: ActivityGerenciarHorarioBinding
    private lateinit var horarioAdapter: HorarioAdapter
    private lateinit var horarioAulaDao: HorarioAulaDao // DAO para salvar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenciarHorarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        horarioAulaDao = AppDatabase.getDatabase(applicationContext).horarioAulaDao()

        setSupportActionBar(binding.toolbarHorario)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()

        binding.fabAdicionarHorario.setOnClickListener {
            val dialog = FormularioHorarioDialogFragment.newInstance(null) // null para novo horário
            dialog.setListener(this) // Define esta Activity como listener
            dialog.show(supportFragmentManager, FormularioHorarioDialogFragment.TAG)
        }

        // Por enquanto, a lista estará vazia.
        observeHorarios() // Chamar o método para observar os horários
    }

    private fun setupRecyclerView() {
        horarioAdapter = HorarioAdapter(
            onEditClickListener = { horarioDisplay ->
                val dialog = FormularioHorarioDialogFragment.newInstance(horarioDisplay.id)
                dialog.setListener(this)
                dialog.show(supportFragmentManager, FormularioHorarioDialogFragment.TAG)
            },
            onDeleteClickListener = { horarioDisplay ->
                mostrarDialogoConfirmacaoDelete(horarioDisplay)
            }
        )
        binding.recyclerViewHorarios.apply {
            adapter = horarioAdapter
            layoutManager = LinearLayoutManager(this@GerenciarHorarioActivity)
        }
    }

    // Implementação do FormularioHorarioListener
    override fun onHorarioSalvo(horarioAula: HorarioAula) {
        lifecycleScope.launch(Dispatchers.IO) {
            horarioAulaDao.inserir(horarioAula) // Inserir ou atualizar (devido ao OnConflictStrategy.REPLACE)
            // A UI deve se atualizar automaticamente se estivermos coletando um Flow do DAO.
            // Caso contrário, precisaria chamar um método para recarregar os horários.
            // Ex: loadHorarios()
            runOnUiThread{ Toast.makeText(this@GerenciarHorarioActivity, "Horário salvo!", Toast.LENGTH_SHORT).show()}
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // quando o HorarioAulaDao for usado para buscar e transformar os dados.

    private fun observeHorarios() {
        lifecycleScope.launch {
            horarioAulaDao.buscarTodosParaDisplay().collect { listaHorariosDisplay ->
                horarioAdapter.submitList(listaHorariosDisplay)
                if (listaHorariosDisplay.isEmpty()) {
                    binding.textViewListaVaziaHorarios.visibility = View.VISIBLE
                    binding.recyclerViewHorarios.visibility = View.GONE
                } else {
                    binding.textViewListaVaziaHorarios.visibility = View.GONE
                    binding.recyclerViewHorarios.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacaoDelete(horarioParaDeletar: com.agendafocopei.ui.model.HorarioAulaDisplay) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir este horário (${horarioParaDeletar.nomeDisciplina} - ${horarioParaDeletar.getDiaDaSemanaFormatado()})?")
            .setPositiveButton("Excluir") { _, _ ->
                deletarHorario(horarioParaDeletar.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarHorario(horarioId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            horarioAulaDao.deleteById(horarioId)
            // A UI será atualizada automaticamente pelo Flow em observeHorarios()
            // runOnUiThread{ Toast.makeText(this@GerenciarHorarioActivity, "Horário deletado!", Toast.LENGTH_SHORT).show()} // Opcional
        }
    }
}
