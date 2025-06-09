package com.agendafocopei.ui.dialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Disciplina
import com.agendafocopei.data.DisciplinaDao
import com.agendafocopei.data.GuiaDeAprendizagem
import com.agendafocopei.data.GuiaDeAprendizagemDao
import com.agendafocopei.databinding.DialogFormularioGuiaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FormularioGuiaFragment : DialogFragment() {

    private var _binding: DialogFormularioGuiaBinding? = null
    private val binding get() = _binding!!

    interface FormularioGuiaListener {
        fun onGuiaSalvo(guia: GuiaDeAprendizagem)
    }

    private var listener: FormularioGuiaListener? = null
    private var guiaIdParaEdicao: Int? = null

    private lateinit var guiaDao: GuiaDeAprendizagemDao
    private lateinit var disciplinaDao: DisciplinaDao

    private var listaDisciplinas: List<Disciplina> = emptyList()

    private var anexoUri: Uri? = null
    private var anexoNome: String? = null
    private var anexoTipo: String? = null

    private lateinit var anexoLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val TAG = "FormularioGuiaDialog"
        private const val ARG_GUIA_ID = "arg_guia_id"

        fun newInstance(guiaId: Int? = null): FormularioGuiaFragment {
            val fragment = FormularioGuiaFragment()
            val args = Bundle()
            guiaId?.let { args.putInt(ARG_GUIA_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_GUIA_ID)) {
                guiaIdParaEdicao = it.getInt(ARG_GUIA_ID)
            }
        }

        val db = AppDatabase.getDatabase(requireContext())
        guiaDao = db.guiaDeAprendizagemDao()
        disciplinaDao = db.disciplinaDao()

        anexoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    anexoUri = uri
                    anexoTipo = requireContext().contentResolver.getType(uri)
                    val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) anexoNome = it.getString(nameIndex)
                        }
                    }
                    binding.textViewNomeAnexoGuia.text = anexoNome ?: uri.lastPathSegment ?: "Arquivo selecionado"
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioGuiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupAnexoButton()

        if (guiaIdParaEdicao != null) {
            binding.textViewFormularioGuiaTitulo.text = "Editar Guia de Aprendizagem"
            carregarGuiaParaEdicao(guiaIdParaEdicao!!)
        } else {
            binding.textViewFormularioGuiaTitulo.text = "Novo Guia de Aprendizagem"
            // Preencher ano atual por padrão
            binding.editTextAnoGuia.setText(Calendar.getInstance().get(Calendar.YEAR).toString())
        }

        binding.buttonSalvarGuia.setOnClickListener { salvarGuia() }
        binding.buttonCancelarGuia.setOnClickListener { dismiss() }
    }

    private fun setupSpinners() {
        lifecycleScope.launch {
            listaDisciplinas = withContext(Dispatchers.IO) { disciplinaDao.buscarTodas().first() }
            val nomesDisciplinas = listaDisciplinas.map { it.nome }
            val disciplinaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesDisciplinas).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerDisciplinaGuia.adapter = disciplinaAdapter

            // Se estiver editando, recarregar para garantir que o spinner é selecionado após ser populado
            if (guiaIdParaEdicao != null && _binding != null) {
                 carregarGuiaParaEdicao(guiaIdParaEdicao!!, true)
            }
        }
    }

    private fun setupAnexoButton() {
        binding.buttonAnexarDocumentoGuia.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Todos os tipos
            }
            anexoLauncher.launch(intent)
        }
    }

    private fun carregarGuiaParaEdicao(id: Int, apenasSelecionarSpinners: Boolean = false) {
        lifecycleScope.launch {
            val guia = withContext(Dispatchers.IO) { guiaDao.buscarPorId(id) }
            guia?.let {
                if (!apenasSelecionarSpinners && _binding != null) {
                    binding.editTextTituloGuiaForm.setText(it.tituloGuia)
                    binding.editTextBimestreGuia.setText(it.bimestre)
                    binding.editTextAnoGuia.setText(it.ano.toString())
                    anexoUri = it.caminhoAnexoGuia?.let { Uri.parse(it) }
                    anexoTipo = it.tipoAnexoGuia
                    binding.textViewNomeAnexoGuia.text = anexoUri?.lastPathSegment ?: "Nenhum anexo" // Simplificado
                }

                if (listaDisciplinas.isNotEmpty()) {
                    val discPos = listaDisciplinas.indexOfFirst { d -> d.id == it.disciplinaId }
                    if (discPos != -1) binding.spinnerDisciplinaGuia.setSelection(discPos)
                }
            }
        }
    }

    private fun salvarGuia() {
        val titulo = binding.editTextTituloGuiaForm.text.toString().trim()
        val bimestre = binding.editTextBimestreGuia.text.toString().trim()
        val anoStr = binding.editTextAnoGuia.text.toString().trim()

        if (binding.spinnerDisciplinaGuia.selectedItemPosition < 0 || listaDisciplinas.isEmpty()) {
            Toast.makeText(context, "Selecione uma disciplina.", Toast.LENGTH_SHORT).show()
            return
        }
        val disciplinaSelecionada = listaDisciplinas[binding.spinnerDisciplinaGuia.selectedItemPosition]

        if (bimestre.isEmpty()) {
            binding.editTextBimestreGuia.error = "Bimestre é obrigatório."
            return
        } else {
            binding.editTextBimestreGuia.error = null
        }

        val ano = anoStr.toIntOrNull()
        if (ano == null || ano < 2000 || ano > 2100) {
            binding.editTextAnoGuia.error = "Ano inválido."
            return
        } else {
            binding.editTextAnoGuia.error = null
        }

        val guiaParaSalvar = GuiaDeAprendizagem(
            id = guiaIdParaEdicao ?: 0,
            tituloGuia = titulo.ifEmpty { null },
            disciplinaId = disciplinaSelecionada.id,
            bimestre = bimestre,
            ano = ano,
            caminhoAnexoGuia = anexoUri?.toString(),
            tipoAnexoGuia = anexoTipo
        )
        listener?.onGuiaSalvo(guiaParaSalvar)
        dismiss()
    }

    fun setListener(listener: FormularioGuiaListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
