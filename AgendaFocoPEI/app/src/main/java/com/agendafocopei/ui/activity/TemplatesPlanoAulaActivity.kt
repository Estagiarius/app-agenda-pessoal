package com.agendafocopei.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.TemplatePlanoAula
import com.agendafocopei.data.TemplatePlanoAulaDao
import com.agendafocopei.databinding.ActivityTemplatesPlanoAulaBinding
import com.agendafocopei.ui.adapter.TemplatePlanoAulaAdapter
import com.agendafocopei.ui.dialog.FormularioTemplatePlanoAulaFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TemplatesPlanoAulaActivity : AppCompatActivity(), FormularioTemplatePlanoAulaFragment.FormularioTemplateListener {

    private lateinit var binding: ActivityTemplatesPlanoAulaBinding
    private lateinit var templateAdapter: TemplatePlanoAulaAdapter
    private lateinit var templateDao: TemplatePlanoAulaDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplatesPlanoAulaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        templateDao = AppDatabase.getDatabase(applicationContext).templatePlanoAulaDao()

        setSupportActionBar(binding.toolbarTemplates)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeTemplates()

        binding.fabAdicionarTemplate.setOnClickListener {
            FormularioTemplatePlanoAulaFragment.newInstance(null).also {
                it.setListener(this)
                it.show(supportFragmentManager, FormularioTemplatePlanoAulaFragment.TAG)
            }
        }
    }

    private fun setupRecyclerView() {
        templateAdapter = TemplatePlanoAulaAdapter(
            onEditClickListener = { template ->
                FormularioTemplatePlanoAulaFragment.newInstance(template.id).also {
                    it.setListener(this)
                    it.show(supportFragmentManager, FormularioTemplatePlanoAulaFragment.TAG)
                }
            },
            onDeleteClickListener = { template ->
                mostrarDialogoConfirmacaoDelete(template)
            }
        )
        binding.recyclerViewTemplatesPlanoAula.apply {
            adapter = templateAdapter
            layoutManager = LinearLayoutManager(this@TemplatesPlanoAulaActivity)
        }
    }

    private fun observeTemplates() {
        lifecycleScope.launch {
            templateDao.buscarTodos().collect { listaTemplates ->
                templateAdapter.submitList(listaTemplates)
                if (listaTemplates.isEmpty()) {
                    binding.textViewListaVaziaTemplates.visibility = View.VISIBLE
                    binding.recyclerViewTemplatesPlanoAula.visibility = View.GONE
                } else {
                    binding.textViewListaVaziaTemplates.visibility = View.GONE
                    binding.recyclerViewTemplatesPlanoAula.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacaoDelete(template: TemplatePlanoAula) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o template \"${template.nomeTemplate}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deletarTemplate(template)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarTemplate(template: TemplatePlanoAula) {
        lifecycleScope.launch(Dispatchers.IO) {
            templateDao.deletar(template)
            // A UI será atualizada automaticamente pelo Flow
            withContext(Dispatchers.Main) {
                Toast.makeText(this@TemplatesPlanoAulaActivity, "Template excluído.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onTemplateSalvo(template: TemplatePlanoAula) {
        lifecycleScope.launch(Dispatchers.IO) {
            templateDao.inserir(template) // REPLACE strategy lida com insert/update
             withContext(Dispatchers.Main) {
                Toast.makeText(this@TemplatesPlanoAulaActivity, "Template salvo!", Toast.LENGTH_SHORT).show()
            }
            // A UI será atualizada automaticamente pelo Flow
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
