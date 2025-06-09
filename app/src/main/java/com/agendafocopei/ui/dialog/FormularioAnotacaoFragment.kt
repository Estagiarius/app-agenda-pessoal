package com.agendafocopei.ui.dialog

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.R
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
import java.text.ParseException
import java.text.SimpleDateFormat
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

    private lateinit var requestAudioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>


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

        // Inicializar launchers
        requestAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                iniciarReconhecimentoDeVoz()
            } else {
                Toast.makeText(requireContext(), "Permissão de áudio negada.", Toast.LENGTH_SHORT).show()
            }
        }

        speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!speechResults.isNullOrEmpty()) {
                    val textoReconhecido = speechResults[0]
                    val textoAtual = binding.editTextConteudoAnotacao.text.toString()
                    binding.editTextConteudoAnotacao.setText(if (textoAtual.isNotEmpty()) "$textoAtual $textoReconhecido" else textoReconhecido)
                }
            } else {
                 Toast.makeText(requireContext(), "Falha no reconhecimento de voz ou cancelado.", Toast.LENGTH_SHORT).show()
            }
        }
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
            updateColorPreview()
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
        binding.viewPreviewCorAnotacao.setOnClickListener {
             binding.buttonEscolherCorAnotacao.performClick()
        }
    }

    private fun setupGravarVozButton() {
        binding.buttonGravarVozAnotacao.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    iniciarReconhecimentoDeVoz()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                    // Explicar ao usuário e pedir novamente (ou direcionar para configs)
                    // Por simplicidade, pedindo direto novamente ou informando.
                    Toast.makeText(requireContext(), "Permissão de áudio é necessária para esta funcionalidade.", Toast.LENGTH_LONG).show()
                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) // Tenta pedir de novo
                }
                else -> {
                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    private fun iniciarReconhecimentoDeVoz() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Reconhecimento de voz não disponível neste dispositivo.", Toast.LENGTH_LONG).show()
            return
        }
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale agora para sua anotação...")
            // putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR") // Opcional
        }
        try {
            speechRecognizerLauncher.launch(speechIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "App de reconhecimento de voz não encontrado. Considere instalar o app Google.", Toast.LENGTH_LONG).show()
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

                if(listaTurmas.isNotEmpty()){
                     selecionarValoresSpinnerEdicao(anotacao)
                }
            }
        }
    }

    private fun selecionarValoresSpinnerEdicao(anotacao: Anotacao) {
        val turmaPos = anotacao.turmaId?.let { turId -> listaTurmas.indexOfFirst { it.id == turId } } ?: -1
        binding.spinnerTurmaAnotacao.setSelection(if (turmaPos != -1) turmaPos + 1 else 0)
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
