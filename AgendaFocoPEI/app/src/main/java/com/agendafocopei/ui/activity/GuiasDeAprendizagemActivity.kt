package com.agendafocopei.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.GuiaDeAprendizagem
import com.agendafocopei.data.GuiaDeAprendizagemDao
import com.agendafocopei.databinding.ActivityGuiasDeAprendizagemBinding
import com.agendafocopei.ui.adapter.GuiaDeAprendizagemAdapter
import com.agendafocopei.ui.dialog.FormularioGuiaFragment
import com.agendafocopei.ui.model.GuiaDeAprendizagemDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GuiasDeAprendizagemActivity : AppCompatActivity(), FormularioGuiaFragment.FormularioGuiaListener {

    private lateinit var binding: ActivityGuiasDeAprendizagemBinding
    private lateinit var guiaAdapter: GuiaDeAprendizagemAdapter
    private lateinit var guiaDao: GuiaDeAprendizagemDao

    companion object {
        const val REQUEST_CODE_DETALHES_GUIA = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuiasDeAprendizagemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guiaDao = AppDatabase.getDatabase(applicationContext).guiaDeAprendizagemDao()

        setSupportActionBar(binding.toolbarGuias)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        observeGuias()

        binding.fabAdicionarGuia.setOnClickListener {
            FormularioGuiaFragment.newInstance(null).also {
                it.setListener(this)
                it.show(supportFragmentManager, FormularioGuiaFragment.TAG)
            }
        }
    }

    private fun setupRecyclerView() {
        guiaAdapter = GuiaDeAprendizagemAdapter(
            onDetalhesClickListener = { guiaDisplay ->
                val intent = Intent(this, DetalhesGuiaActivity::class.java)
                intent.putExtra(DetalhesGuiaActivity.EXTRA_GUIA_ID, guiaDisplay.guiaDeAprendizagem.id)
                startActivityForResult(intent, REQUEST_CODE_DETALHES_GUIA) // Se precisar atualizar algo ao voltar
            },
            onEditClickListener = { guiaDisplay ->
                FormularioGuiaFragment.newInstance(guiaDisplay.guiaDeAprendizagem.id).also {
                    it.setListener(this)
                    it.show(supportFragmentManager, FormularioGuiaFragment.TAG)
                }
            },
            onDeleteClickListener = { guiaDisplay ->
                mostrarDialogoConfirmacaoDelete(guiaDisplay.guiaDeAprendizagem)
            }
        )
        binding.recyclerViewGuias.apply {
            adapter = guiaAdapter
            layoutManager = LinearLayoutManager(this@GuiasDeAprendizagemActivity)
        }
    }

    private fun observeGuias() {
        lifecycleScope.launch {
            guiaDao.buscarTodosParaDisplay().collect { listaGuias ->
                guiaAdapter.submitList(listaGuias)
                if (listaGuias.isEmpty()) {
                    binding.textViewListaVaziaGuias.visibility = View.VISIBLE
                    binding.recyclerViewGuias.visibility = View.GONE
                } else {
                    binding.textViewListaVaziaGuias.visibility = View.GONE
                    binding.recyclerViewGuias.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun mostrarDialogoConfirmacaoDelete(guia: GuiaDeAprendizagem) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir o guia \"${guia.tituloGuia ?: "${guia.bimestre} ${guia.ano}"}\"? Isso também excluirá todos os itens do checklist associados.")
            .setPositiveButton("Excluir") { _, _ ->
                deletarGuia(guia)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarGuia(guia: GuiaDeAprendizagem) {
        lifecycleScope.launch(Dispatchers.IO) {
            guiaDao.deletar(guia) // CASCADE deve cuidar dos itens do checklist
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GuiasDeAprendizagemActivity, "Guia excluído.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onGuiaSalvo(guia: GuiaDeAprendizagem) {
        lifecycleScope.launch(Dispatchers.IO) {
            guiaDao.inserir(guia)
            withContext(Dispatchers.Main) {
                 Toast.makeText(this@GuiasDeAprendizagemActivity, "Guia salvo!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Se precisar atualizar a lista após DetalhesGuiaActivity (ex: se checklist progresso mudou)
    // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    //     super.onActivityResult(requestCode, resultCode, data)
    //     if (requestCode == REQUEST_CODE_DETALHES_GUIA && resultCode == Activity.RESULT_OK) {
    //         // Potencialmente recarregar ou notificar adapter se houver mudanças visíveis na lista
    //     }
    // }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
