package com.agendafocopei.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.data.Anotacao
import com.agendafocopei.data.AnotacaoDao
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Turma
import com.agendafocopei.data.TurmaDao
import com.agendafocopei.databinding.DialogFormularioAnotacaoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FormularioAnotacaoFragment : DialogFragment(), ColorPickerDialogFragment.ColorPickerListener {

    private var _binding: DialogFormularioAnotacaoBinding? = null
    private val binding get() = _binding!!

    interface FormularioAnotacaoListener {
        fun onAnotacaoSalva(anotacao: Anotacao)
    }

    private var listener: FormularioAnotacaoListener? = null
    private var anotacaoIdParaEdicao: Int? = null
    private var anotacaoParaEdicao: Anotacao? = null

    private lateinit var anotacaoDao: AnotacaoDao
    private lateinit var turmaDao: TurmaDao

    private var listaTurmas: List<Turma> = emptyList()
    private var corSelecionadaAnotacao: Int? = null

    companion object {
        const val TAG = "FormularioAnotacaoDialog"
        private const val ARG_ANOTACAO_ID = "arg_anotacao_id"
        private const val COLOR_PICKER_REQUEST_TAG = "anotacao_color_picker"

        fun newInstance(anotacaoId: Int? = null): FormularioAnotacaoFragment {
            val fragment = FormularioAnotacaoFragment()
            val args = Bundle()
            anotacaoId?.let { args.putInt(ARG_ANOTACAO_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_ANOTACAO_ID)) {
                anotacaoIdParaEdicao = it.getInt(ARG_ANOTACAO_ID)
            }
        }
        val db = AppDatabase.getDatabase(requireContext())
        anotacaoDao = db.anotacaoDao()
        turmaDao = db.turmaDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioAnotacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTurmaSpinner()
        setupColorPicker()
        setupGravarVozButton()

        if (anotacaoIdParaEdicao != null) {
            binding.textViewFormularioAnotacaoTitulo.text = "Editar Anotação"
            carregarAnotacaoParaEdicao(anotacaoIdParaEdicao!!)
        } else {
            binding.textViewFormularioAnotacaoTitulo.text = "Nova Anotação"
            updateColorPreview() // Para cor inicial transparente
        }

        binding.buttonSalvarAnotacao.setOnClickListener { salvarAnotacao() }
        binding.buttonCancelarAnotacao.setOnClickListener { dismiss() }
    }

    private fun setupTurmaSpinner() {
        lifecycleScope.launch {
            listaTurmas = withContext(Dispatchers.IO) { turmaDao.buscarTodas().first() }
            val nomesTurmasComOpcao = listOf("Nenhuma (Geral/Pessoal)") + listaTurmas.map { it.nome }
            val turmaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesTurmasComOpcao).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerTurmaAnotacao.adapter = turmaAdapter
            binding.spinnerTurmaAnotacao.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Mostrar/ocultar campo de aluno se "Nenhuma" for selecionada ou não.
                    // Aqui, a lógica é que o campo Aluno é sempre visível se uma turma é selecionada,
                    // mas poderia ser oculto se "Nenhuma" for selecionada.
                    // Por enquanto, mantendo simples: campo Aluno é sempre editável.
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Se estiver editando, selecionar valor após carregar
            if (anotacaoIdParaEdicao != null && anotacaoParaEdicao != null) {
                selecionarValoresSpinnerEdicao(anotacaoParaEdicao!!)
            }
        }
    }

    private fun setupColorPicker() {
        binding.buttonEscolherCorAnotacao.setOnClickListener {
            val dialog = ColorPickerDialogFragment.newInstance(corSelecionadaAnotacao, COLOR_PICKER_REQUEST_TAG)
            dialog.show(childFragmentManager, ColorPickerDialogFragment.TAG)
        }
        binding.viewPreviewCorAnotacao.setOnClickListener { // Também abre o picker
             binding.buttonEscolherCorAnotacao.performClick()
        }
    }

    private fun setupGravarVozButton() {
        binding.buttonGravarVozAnotacao.setOnClickListener {
            Toast.makeText(context, "Funcionalidade de Gravar Voz em breve!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateColorPreview() {
        binding.viewPreviewCorAnotacao.setBackgroundColor(corSelecionadaAnotacao ?: Color.TRANSPARENT)
    }

    private fun carregarAnotacaoParaEdicao(id: Int) {
        lifecycleScope.launch {
            anotacaoParaEdicao = withContext(Dispatchers.IO) { anotacaoDao.buscarPorId(id) }
            anotacaoParaEdicao?.let { anotacao ->
                binding.editTextConteudoAnotacao.setText(anotacao.conteudo)
                binding.editTextTagsAnotacao.setText(anotacao.tagsString)
                corSelecionadaAnotacao = anotacao.cor
                updateColorPreview()
                binding.editTextAlunoAnotacao.setText(anotacao.alunoNome)

                if(listaTurmas.isNotEmpty()){ // Garante que o spinner já foi populado
                     selecionarValoresSpinnerEdicao(anotacao)
                }
            }
        }
    }

    private fun selecionarValoresSpinnerEdicao(anotacao: Anotacao) {
        val turmaPos = anotacao.turmaId?.let { turId -> listaTurmas.indexOfFirst { it.id == turId } } ?: -1
        binding.spinnerTurmaAnotacao.setSelection(if (turmaPos != -1) turmaPos + 1 else 0) // +1 por "Nenhuma"
    }

    private fun salvarAnotacao() {
        val conteudo = binding.editTextConteudoAnotacao.text.toString().trim()
        if (conteudo.isEmpty()) {
            binding.editTextConteudoAnotacao.error = "Conteúdo é obrigatório"
            return
        } else {
            binding.editTextConteudoAnotacao.error = null
        }

        val tags = binding.editTextTagsAnotacao.text.toString().trim()
        val alunoNome = binding.editTextAlunoAnotacao.text.toString().trim()

        val turmaIdSelecionada: Int? = if (binding.spinnerTurmaAnotacao.selectedItemPosition > 0 && listaTurmas.isNotEmpty()) {
            listaTurmas[binding.spinnerTurmaAnotacao.selectedItemPosition - 1].id
        } else null


        val anotacaoParaSalvar = Anotacao(
            id = anotacaoIdParaEdicao ?: 0,
            conteudo = conteudo,
            dataCriacao = anotacaoParaEdicao?.dataCriacao ?: System.currentTimeMillis(),
            dataModificacao = System.currentTimeMillis(),
            cor = corSelecionadaAnotacao,
            turmaId = turmaIdSelecionada,
            alunoNome = alunoNome.ifEmpty { null },
            tagsString = tags.ifEmpty { null }
        )

        listener?.onAnotacaoSalva(anotacaoParaSalvar)
        dismiss()
    }

    override fun onColorSelected(color: Int, tag: String?) {
        if (tag == COLOR_PICKER_REQUEST_TAG) {
            corSelecionadaAnotacao = color
            updateColorPreview()
        }
    }

    fun setListener(listener: FormularioAnotacaoListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
