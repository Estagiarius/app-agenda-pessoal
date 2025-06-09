package com.agendafocopei.ui.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.*
import com.agendafocopei.databinding.ActivityDetalhesGuiaBinding
import com.agendafocopei.ui.adapter.ItemChecklistGuiaAdapter
import com.agendafocopei.ui.model.GuiaDeAprendizagemDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalhesGuiaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalhesGuiaBinding
    private lateinit var itemChecklistAdapter: ItemChecklistGuiaAdapter
    private lateinit var guiaDao: GuiaDeAprendizagemDao
    private lateinit var itemChecklistDao: ItemChecklistGuiaDao
    private var guiaId: Int = -1
    private var guiaAtual: GuiaDeAprendizagemDisplay? = null

    companion object {
        const val EXTRA_GUIA_ID = "extra_guia_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalhesGuiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guiaId = intent.getIntExtra(EXTRA_GUIA_ID, -1)
        if (guiaId == -1) {
            Toast.makeText(this, "ID do Guia inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val db = AppDatabase.getDatabase(applicationContext)
        guiaDao = db.guiaDeAprendizagemDao()
        itemChecklistDao = db.itemChecklistGuiaDao()

        setSupportActionBar(binding.toolbarDetalhesGuia)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadGuiaDetalhes()
        observeChecklistItens()

        binding.buttonAdicionarItemChecklist.setOnClickListener {
            adicionarNovoItemChecklist()
        }

        binding.buttonVerDocumentoAnexadoGuia.setOnClickListener {
            abrirDocumentoAnexado()
        }
    }

    private fun setupRecyclerView() {
        itemChecklistAdapter = ItemChecklistGuiaAdapter(
            onItemCheckChanged = { item, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    itemChecklistDao.atualizar(item.copy(concluido = isChecked))
                }
            },
            onDeleteItemClickListener = { item ->
                mostrarDialogoConfirmacaoDeleteChecklistItem(item)
            }
        )
        binding.recyclerViewItensChecklist.apply {
            adapter = itemChecklistAdapter
            layoutManager = LinearLayoutManager(this@DetalhesGuiaActivity)
        }
    }

    private fun loadGuiaDetalhes() {
        lifecycleScope.launch {
            guiaAtual = withContext(Dispatchers.IO) { guiaDao.buscarDisplayPorId(guiaId) }
            guiaAtual?.let { display ->
                val guia = display.guiaDeAprendizagem
                val tituloGerado = "Guia: ${display.nomeDisciplina ?: ""} - ${guia.bimestre} ${guia.ano}"
                binding.textViewTituloDetalhesGuia.text = guia.tituloGuia ?: tituloGerado
                binding.textViewDisciplinaDetalhesGuia.text = "Disciplina: ${display.nomeDisciplina ?: "N/A"}"
                binding.textViewBimestreAnoDetalhesGuia.text = "${guia.bimestre} - ${guia.ano}"

                if (guia.caminhoAnexoGuia != null) {
                    binding.buttonVerDocumentoAnexadoGuia.visibility = View.VISIBLE
                } else {
                    binding.buttonVerDocumentoAnexadoGuia.visibility = View.GONE
                }
            } ?: run {
                Toast.makeText(this@DetalhesGuiaActivity, "Guia não encontrado.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun observeChecklistItens() {
        lifecycleScope.launch {
            itemChecklistDao.buscarPorGuiaId(guiaId).collectLatest { itens ->
                itemChecklistAdapter.submitList(itens)
                binding.textViewChecklistVazio.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewItensChecklist.visibility = if (itens.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun adicionarNovoItemChecklist() {
        val descricao = binding.editTextNovoItemChecklist.text.toString().trim()
        if (descricao.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                itemChecklistDao.inserir(
                    ItemChecklistGuia(
                        guiaAprendizagemId = guiaId,
                        descricaoItem = descricao,
                        concluido = false
                    )
                )
            }
            binding.editTextNovoItemChecklist.text?.clear()
        } else {
            Toast.makeText(this, "Descrição do item não pode ser vazia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoConfirmacaoDeleteChecklistItem(item: ItemChecklistGuia) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o item \"${item.descricaoItem}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deletarChecklistItem(item)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarChecklistItem(item: ItemChecklistGuia) {
        lifecycleScope.launch(Dispatchers.IO) {
            itemChecklistDao.deletar(item)
        }
    }

    private fun abrirDocumentoAnexado() {
        guiaAtual?.guiaDeAprendizagem?.caminhoAnexoGuia?.let { uriString ->
            val uri = Uri.parse(uriString)
            val mimeType = guiaAtual?.guiaDeAprendizagem?.tipoAnexoGuia

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: "*/*") // Se tipo MIME não conhecido, usa genérico
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Necessário para URIs de conteúdo
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Nenhum aplicativo encontrado para abrir este tipo de arquivo.", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(this, "Nenhum documento anexado.", Toast.LENGTH_SHORT).show()
    }


    override fun onSupportNavigateUp(): Boolean {
        // Informa a activity anterior que pode ter havido mudanças (ex: progresso do checklist)
        setResult(Activity.RESULT_OK)
        finish()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }
}
