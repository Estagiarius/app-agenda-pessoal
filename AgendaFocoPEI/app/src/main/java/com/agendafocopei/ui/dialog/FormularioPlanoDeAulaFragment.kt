package com.agendafocopei.ui.dialog

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView // Import para AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.data.*
import com.agendafocopei.databinding.DialogFormularioPlanoDeAulaBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class FormularioPlanoDeAulaFragment : DialogFragment() {

    private var _binding: DialogFormularioPlanoDeAulaBinding? = null
    private val binding get() = _binding!!

    interface FormularioPlanoListener {
        fun onPlanoSalvo(plano: PlanoDeAula)
    }

    private var listener: FormularioPlanoListener? = null
    private var planoIdParaEdicao: Int? = null
    private var horarioAulaIdPredefinido: Int? = null
    private var disciplinaIdPredefinida: Int? = null
    private var turmaIdPredefinida: Int? = null
    private var dataPredefinida: String? = null // YYYY-MM-DD

    private lateinit var planoDeAulaDao: PlanoDeAulaDao
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao
    private lateinit var templateDao: TemplatePlanoAulaDao // Agora usado

    private var listaDisciplinas: List<Disciplina> = emptyList()
    private var listaTurmas: List<Turma> = emptyList()
    private var templatesDisponiveis: List<TemplatePlanoAula> = emptyList() // Agora usado

    private var dataAulaSelecionada: Calendar? = null
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val storeDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var anexoUri: Uri? = null
    private var anexoNome: String? = null
    private var anexoTipo: String? = null

    private lateinit var anexoLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val TAG = "FormularioPlanoDeAulaDialog"
        private const val ARG_PLANO_ID = "arg_plano_id"
        private const val ARG_HORARIO_AULA_ID = "arg_horario_aula_id"
        private const val ARG_DISCIPLINA_ID_PREDEF = "arg_disciplina_id_predef"
        private const val ARG_TURMA_ID_PREDEF = "arg_turma_id_predef"
        private const val ARG_DATA_PREDEF = "arg_data_predef"


        fun newInstance(
            planoId: Int? = null,
            horarioAulaId: Int? = null,
            disciplinaIdPredefinida: Int? = null,
            turmaIdPredefinida: Int? = null,
            dataPredefinida: String? = null // YYYY-MM-DD
        ): FormularioPlanoDeAulaFragment {
            val fragment = FormularioPlanoDeAulaFragment()
            val args = Bundle()
            planoId?.let { args.putInt(ARG_PLANO_ID, it) }
            horarioAulaId?.let { args.putInt(ARG_HORARIO_AULA_ID, it) }
            disciplinaIdPredefinida?.let { args.putInt(ARG_DISCIPLINA_ID_PREDEF, it) }
            turmaIdPredefinida?.let { args.putInt(ARG_TURMA_ID_PREDEF, it) }
            dataPredefinida?.let { args.putString(ARG_DATA_PREDEF, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            planoIdParaEdicao = if (it.containsKey(ARG_PLANO_ID)) it.getInt(ARG_PLANO_ID) else null
            horarioAulaIdPredefinido = if (it.containsKey(ARG_HORARIO_AULA_ID)) it.getInt(ARG_HORARIO_AULA_ID) else null
            disciplinaIdPredefinida = if (it.containsKey(ARG_DISCIPLINA_ID_PREDEF)) it.getInt(ARG_DISCIPLINA_ID_PREDEF) else null
            turmaIdPredefinida = if (it.containsKey(ARG_TURMA_ID_PREDEF)) it.getInt(ARG_TURMA_ID_PREDEF) else null
            dataPredefinida = if (it.containsKey(ARG_DATA_PREDEF)) it.getString(ARG_DATA_PREDEF) else null
        }

        val db = AppDatabase.getDatabase(requireContext())
        planoDeAulaDao = db.planoDeAulaDao()
        disciplinaDao = db.disciplinaDao()
        turmaDao = db.turmaDao()
        templateDao = db.templatePlanoAulaDao() // Inicializa o DAO

        anexoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    anexoUri = uri
                    anexoTipo = requireContext().contentResolver.getType(uri)
                    // Obter nome do arquivo
                    val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) anexoNome = it.getString(nameIndex)
                        }
                    }
                    binding.textViewNomeAnexoPlano.text = anexoNome ?: uri.lastPathSegment ?: "Arquivo selecionado"
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioPlanoDeAulaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupDatePicker()
        setupAnexoButton()

        if (planoIdParaEdicao != null) {
            binding.textViewFormularioPlanoTitulo.text = "Editar Plano de Aula"
            carregarPlanoParaEdicao(planoIdParaEdicao!!)
        } else {
            binding.textViewFormularioPlanoTitulo.text = "Novo Plano de Aula"
            preencherComDadosPredefinidos()
        }

        binding.buttonSalvarPlano.setOnClickListener { salvarPlano() }
        binding.buttonCancelarPlano.setOnClickListener { dismiss() }
    }

    private fun preencherComDadosPredefinidos() {
        if (dataPredefinida != null) {
            try {
                val date = storeDateFormat.parse(dataPredefinida!!)
                dataAulaSelecionada = Calendar.getInstance().apply { time = date!! }
                binding.editTextDataAulaPlano.setText(displayDateFormat.format(dataAulaSelecionada!!.time))
            } catch (e: ParseException) { dataAulaSelecionada = null }
        }
        // A seleção dos spinners será feita quando eles forem populados, em setupSpinners
    }


    private fun setupSpinners() {
        lifecycleScope.launch {
            listaDisciplinas = withContext(Dispatchers.IO) { disciplinaDao.buscarTodas().first() }
            val nomesDisciplinas = listaDisciplinas.map { it.nome }
            val disciplinaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesDisciplinas).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerDisciplinaPlano.adapter = disciplinaAdapter
            disciplinaIdPredefinida?.let { predefId ->
                val pos = listaDisciplinas.indexOfFirst { it.id == predefId }
                if (pos != -1) binding.spinnerDisciplinaPlano.setSelection(pos)
            }

            // Popular Spinner de Turmas
            listaTurmas = withContext(Dispatchers.IO) { turmaDao.buscarTodas().first() }
            val nomesTurmasComOpcaoNenhuma = mutableListOf("Nenhuma (Geral)")
            nomesTurmasComOpcaoNenhuma.addAll(listaTurmas.map { it.nome })
            val turmaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesTurmasComOpcaoNenhuma).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerTurmaPlano.adapter = turmaAdapter
            turmaIdPredefinida?.let { predefId ->
                 // +1 por causa da opção "Nenhuma"
                val pos = listaTurmas.indexOfFirst { it.id == predefId }
                if (pos != -1) binding.spinnerTurmaPlano.setSelection(pos + 1) else binding.spinnerTurmaPlano.setSelection(0)
            } ?: binding.spinnerTurmaPlano.setSelection(0)

            // Popular Spinner de Templates
            templatesDisponiveis = withContext(Dispatchers.IO) { templateDao.buscarTodos().first() }
            val nomesTemplatesComOpcaoNenhuma = mutableListOf("Nenhum (Personalizado)")
            nomesTemplatesComOpcaoNenhuma.addAll(templatesDisponiveis.map { it.nomeTemplate })
            val templateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesTemplatesComOpcaoNenhuma).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerSelecionarTemplatePlano.adapter = templateAdapter
            binding.spinnerSelecionarTemplatePlano.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) { // Posição 0 é "Nenhum"
                        val templateSelecionado = templatesDisponiveis[position - 1]
                        preencherCamposComTemplate(templateSelecionado)
                    } else {
                        // Opcional: Limpar campos se "Nenhum" for selecionado após um template
                        // binding.editTextConteudoPlano.setText("") // Exemplo
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }


            // Se estiver editando, recarregar para garantir que os spinners são selecionados após serem populados
            // Isso inclui o spinner de template.
            if (planoIdParaEdicao != null && _binding != null) {
                 carregarPlanoParaEdicao(planoIdParaEdicao!!, true) // O true agora significa apenas selecionar spinners
            }
        }
    }

    private fun preencherCamposComTemplate(template: TemplatePlanoAula) {
        // Concatena os campos do template no campo de conteúdo principal.
        // Uma UI mais elaborada teria campos separados no formulário do plano.
        val sb = StringBuilder()
        template.campoHabilidades?.let { if(it.isNotBlank()) sb.append("Habilidades:\n").append(it).append("\n\n") }
        template.campoRecursos?.let { if(it.isNotBlank()) sb.append("Recursos:\n").append(it).append("\n\n") }
        template.campoMetodologia?.let { if(it.isNotBlank()) sb.append("Metodologia/Estratégias:\n").append(it).append("\n\n") }
        template.campoAvaliacao?.let { if(it.isNotBlank()) sb.append("Avaliação:\n").append(it).append("\n") }
        // template.outrosCampos // Poderia ser parseado se fosse JSON e adicionado

        binding.editTextConteudoPlano.setText(sb.toString().trim())
        Toast.makeText(context, "Conteúdo preenchido com template: ${template.nomeTemplate}", Toast.LENGTH_SHORT).show()
    }

    private fun setupDatePicker() {
        binding.editTextDataAulaPlano.setOnClickListener {
            val calendarToShow = dataAulaSelecionada ?: Calendar.getInstance()
            val year = calendarToShow.get(Calendar.YEAR)
            val month = calendarToShow.get(Calendar.MONTH)
            val day = calendarToShow.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                if(dataAulaSelecionada == null) dataAulaSelecionada = Calendar.getInstance()
                dataAulaSelecionada!!.set(selectedYear, selectedMonth, selectedDayOfMonth)
                binding.editTextDataAulaPlano.setText(displayDateFormat.format(dataAulaSelecionada!!.time))
            }, year, month, day).show()
        }
    }

    private fun setupAnexoButton() {
        binding.buttonAnexarArquivoPlano.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Todos os tipos de arquivo
            }
            anexoLauncher.launch(intent)
        }
    }

    private fun carregarPlanoParaEdicao(id: Int, apenasSelecionarSpinners: Boolean = false) {
        lifecycleScope.launch {
            val planoDisplay = withContext(Dispatchers.IO) { planoDeAulaDao.buscarDisplayPorId(id) }
            planoDisplay?.let { pd ->
                if (!apenasSelecionarSpinners && _binding != null) { // Evita repopular tudo se for só para spinners
                    binding.editTextTituloPlano.setText(pd.planoDeAula.tituloPlano)
                    if (pd.planoDeAula.dataAula != null) {
                        try {
                            val date = storeDateFormat.parse(pd.planoDeAula.dataAula!!)
                            dataAulaSelecionada = Calendar.getInstance().apply { time = date!! }
                            binding.editTextDataAulaPlano.setText(displayDateFormat.format(dataAulaSelecionada!!.time))
                        } catch (e: ParseException) { dataAulaSelecionada = null; binding.editTextDataAulaPlano.text = null }
                    } else {
                        dataAulaSelecionada = null; binding.editTextDataAulaPlano.text = null
                    }
                    binding.editTextConteudoPlano.setText(pd.planoDeAula.textoPlano)
                    anexoUri = pd.planoDeAula.caminhoAnexo?.let { Uri.parse(it) }
                    anexoTipo = pd.planoDeAula.tipoAnexo
                    // Nome do anexo não é armazenado diretamente, idealmente seria. Exibindo URI por enquanto.
                    binding.textViewNomeAnexoPlano.text = anexoUri?.lastPathSegment ?: "Nenhum anexo"
                    // Se o plano usou um template, pré-seleciona ele no spinner
                    pd.planoDeAula.templateUsadoId?.let { templateId ->
                        if (templatesDisponiveis.isNotEmpty()) {
                            val templatePos = templatesDisponiveis.indexOfFirst { it.id == templateId }
                            if (templatePos != -1) {
                                binding.spinnerSelecionarTemplatePlano.setSelection(templatePos + 1) // +1 pela opção "Nenhum"
                            }
                        }
                    }
                }

                // Selecionar spinners de Disciplina e Turma
                if (listaDisciplinas.isNotEmpty()) {
                    val discPos = listaDisciplinas.indexOfFirst { it.id == pd.planoDeAula.disciplinaId }
                    if (discPos != -1) binding.spinnerDisciplinaPlano.setSelection(discPos)
                }
                if (listaTurmas.isNotEmpty()) {
                    val turmaPos = listaTurmas.indexOfFirst { it.id == pd.planoDeAula.turmaId }
                    if (turmaPos != -1) binding.spinnerTurmaPlano.setSelection(turmaPos + 1) else binding.spinnerTurmaPlano.setSelection(0)
                } else {
                     binding.spinnerTurmaPlano.setSelection(0)
                }
            }
        }
    }

    private fun salvarPlano() {
        val titulo = binding.editTextTituloPlano.text.toString().trim()
        val conteudo = binding.editTextConteudoPlano.text.toString().trim()

        if (binding.spinnerDisciplinaPlano.selectedItemPosition < 0 || listaDisciplinas.isEmpty()) {
            Toast.makeText(context, "Selecione uma disciplina.", Toast.LENGTH_SHORT).show()
            return
        }
        val disciplinaSelecionada = listaDisciplinas[binding.spinnerDisciplinaPlano.selectedItemPosition]

        val turmaSelecionadaId: Int? = if (binding.spinnerTurmaPlano.selectedItemPosition > 0 && listaTurmas.isNotEmpty()) {
            listaTurmas[binding.spinnerTurmaPlano.selectedItemPosition - 1].id // -1 por causa da opção "Nenhuma"
        } else {
            null
        }

        val dataAulaFormatada: String? = dataAulaSelecionada?.let { storeDateFormat.format(it.time) }

        val templateSelecionadoId: Int? = if (binding.spinnerSelecionarTemplatePlano.selectedItemPosition > 0 && templatesDisponiveis.isNotEmpty()) {
            templatesDisponiveis[binding.spinnerSelecionarTemplatePlano.selectedItemPosition - 1].id
        } else {
            null
        }

        if (disciplinaSelecionada == null) {
            Toast.makeText(context, "Disciplina é obrigatória.", Toast.LENGTH_SHORT).show()
            return
        }

        val planoParaSalvar = PlanoDeAula(
            id = planoIdParaEdicao ?: 0,
            horarioAulaId = horarioAulaIdPredefinido,
            dataAula = dataAulaFormatada,
            disciplinaId = disciplinaSelecionada.id,
            turmaId = turmaSelecionadaId,
            tituloPlano = titulo.ifEmpty { null },
            textoPlano = conteudo.ifEmpty { null },
            caminhoAnexo = anexoUri?.toString(),
            tipoAnexo = anexoTipo,
            templateUsadoId = templateSelecionadoId
        )
        listener?.onPlanoSalvo(planoParaSalvar)
        dismiss()
    }

    fun setListener(listener: FormularioPlanoListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
