package com.agendafocopei.ui.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.R
import com.agendafocopei.data.*
import com.agendafocopei.databinding.DialogFormularioTarefaBinding
import com.agendafocopei.ui.adapter.SubtarefaAdapter
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class FormularioTarefaFragment : DialogFragment(), ColorPickerDialogFragment.ColorPickerListener {

    private var _binding: DialogFormularioTarefaBinding? = null
    private val binding get() = _binding!!

    interface FormularioTarefaListener {
        // Passar a tarefa e a lista de subtarefas para a Activity salvar
        fun onTarefaSalva(tarefa: Tarefa, subtarefas: List<Subtarefa>)
    }

    private var listener: FormularioTarefaListener? = null
    private var tarefaIdParaEdicao: Int? = null
    private var tarefaParaEdicao: Tarefa? = null

    private lateinit var tarefaDao: TarefaDao
    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao
    private lateinit var subtarefaDao: SubtarefaDao

    private var listaDisciplinas: List<Disciplina> = emptyList()
    private var listaTurmas: List<Turma> = emptyList()
    private val listaSubtarefas = mutableListOf<Subtarefa>()
    private lateinit var subtarefaAdapter: SubtarefaAdapter

    private var prazoCalendar: Calendar? = null
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val storeDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val deltasLembreteMillis = listOf(
        -1L, 0L, 5 * 60 * 1000L, 10 * 60 * 1000L, 30 * 60 * 1000L,
        60 * 60 * 1000L, 2 * 60 * 60 * 1000L, 24 * 60 * 60 * 1000L
    )

    companion object {
        const val TAG = "FormularioTarefaDialog"
        private const val ARG_TAREFA_ID = "arg_tarefa_id"

        fun newInstance(tarefaId: Int? = null): FormularioTarefaFragment {
            val fragment = FormularioTarefaFragment()
            val args = Bundle()
            tarefaId?.let { args.putInt(ARG_TAREFA_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_TAREFA_ID)) {
                tarefaIdParaEdicao = it.getInt(ARG_TAREFA_ID)
            }
        }
        val db = AppDatabase.getDatabase(requireContext())
        tarefaDao = db.tarefaDao()
        disciplinaDao = db.disciplinaDao()
        turmaDao = db.turmaDao()
        subtarefaDao = db.subtarefaDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioTarefaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupDateTimePickers()
        setupSubtarefasRecyclerView()

        if (tarefaIdParaEdicao != null) {
            binding.textViewFormularioTarefaTitulo.text = "Editar Tarefa"
            carregarTarefaParaEdicao(tarefaIdParaEdicao!!)
        } else {
            binding.textViewFormularioTarefaTitulo.text = "Nova Tarefa"
            binding.spinnerPrioridadeTarefa.setSelection(1)
            atualizarEstadoSpinnerLembrete()
            subtarefaAdapter.submitList(emptyList())
        }

        binding.buttonSalvarTarefa.setOnClickListener { salvarTarefaESubtarefas() }
        binding.buttonCancelarTarefa.setOnClickListener { dismiss() }
        binding.buttonLimparPrazoTarefa.setOnClickListener {
            prazoCalendar = null
            binding.editTextPrazoDataTarefa.setText("")
            binding.editTextPrazoHoraTarefa.setText("")
            atualizarEstadoSpinnerLembrete()
        }
        binding.buttonAdicionarSubtarefa.setOnClickListener { adicionarNovaSubtarefa() }
    }

    private fun setupSubtarefasRecyclerView() {
        subtarefaAdapter = SubtarefaAdapter(
            onItemCheckedChange = { subtarefa, isChecked ->
                val index = listaSubtarefas.indexOfFirst { it.id == subtarefa.id && (it.id != 0 || it.descricaoSubtarefa == subtarefa.descricaoSubtarefa) }
                if (index != -1) {
                    listaSubtarefas[index] = listaSubtarefas[index].copy(concluida = isChecked)
                }
            },
            onItemDescriptionChange = { subtarefa, newDescription ->
                val index = listaSubtarefas.indexOfFirst { it.id == subtarefa.id && (it.id != 0 || it.descricaoSubtarefa == subtarefa.descricaoSubtarefa) }
                if (index != -1) {
                     // Apenas atualiza se o texto realmente mudou para evitar loops com TextWatcher
                    if (listaSubtarefas[index].descricaoSubtarefa != newDescription) {
                        listaSubtarefas[index] = listaSubtarefas[index].copy(descricaoSubtarefa = newDescription)
                    }
                }
            },
            onDeleteClickListener = { subtarefa ->
                val itemToRemove = listaSubtarefas.find { it.id == subtarefa.id && (it.id != 0 || it.descricaoSubtarefa == subtarefa.descricaoSubtarefa) }
                itemToRemove?.let {
                    listaSubtarefas.remove(it)
                    subtarefaAdapter.submitList(listaSubtarefas.toList())
                }
            }
        )
        binding.recyclerViewSubtarefas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subtarefaAdapter
        }
    }

    private fun adicionarNovaSubtarefa() {
        val descricao = binding.editTextNovaSubtarefa.text.toString().trim()
        if (descricao.isNotEmpty()) {
            val novaSubtarefa = Subtarefa(
                // id é 0 para novo, tarefaId será setado ao salvar a tarefa principal
                tarefaId = tarefaIdParaEdicao ?: 0, // Temporário se for nova tarefa
                descricaoSubtarefa = descricao,
                concluida = false,
                ordem = listaSubtarefas.size
            )
            listaSubtarefas.add(novaSubtarefa)
            subtarefaAdapter.submitList(listaSubtarefas.toList())
            binding.editTextNovaSubtarefa.text?.clear()
        } else {
            Toast.makeText(context, "Descrição da subtarefa não pode ser vazia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            requireContext(), R.array.prioridades_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPrioridadeTarefa.adapter = adapter
        }
        ArrayAdapter.createFromResource(
            requireContext(), R.array.opcoes_lembrete_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerLembreteTarefa.adapter = adapter
        }
        lifecycleScope.launch {
            listaDisciplinas = withContext(Dispatchers.IO) { disciplinaDao.buscarTodas().first() }
            val nomesDisciplinasComOpcao = listOf("Nenhuma") + listaDisciplinas.map { it.nome }
            val disciplinaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesDisciplinasComOpcao).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerDisciplinaTarefa.adapter = disciplinaAdapter

            listaTurmas = withContext(Dispatchers.IO) { turmaDao.buscarTodas().first() }
            val nomesTurmasComOpcao = listOf("Nenhuma") + listaTurmas.map { it.nome }
            val turmaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesTurmasComOpcao).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerTurmaTarefa.adapter = turmaAdapter

            if (tarefaIdParaEdicao != null && tarefaParaEdicao != null) {
                selecionarValoresSpinnerEdicao(tarefaParaEdicao!!)
            }
        }
    }

    private fun setupDateTimePickers() {
        binding.editTextPrazoDataTarefa.setOnClickListener {
            val cal = prazoCalendar ?: Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                if (prazoCalendar == null) prazoCalendar = Calendar.getInstance()
                prazoCalendar!!.clear(Calendar.HOUR_OF_DAY); prazoCalendar!!.clear(Calendar.MINUTE); prazoCalendar!!.clear(Calendar.SECOND); prazoCalendar!!.clear(Calendar.MILLISECOND)
                prazoCalendar!!.set(year, month, dayOfMonth)
                binding.editTextPrazoDataTarefa.setText(displayDateFormat.format(prazoCalendar!!.time))
                if(binding.editTextPrazoHoraTarefa.text.isNullOrEmpty()){
                    prazoCalendar!!.set(Calendar.HOUR_OF_DAY, 8); prazoCalendar!!.set(Calendar.MINUTE, 0)
                    binding.editTextPrazoHoraTarefa.setText(timeFormat.format(prazoCalendar!!.time))
                }
                atualizarEstadoSpinnerLembrete()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.editTextPrazoHoraTarefa.setOnClickListener {
            if (prazoCalendar == null || binding.editTextPrazoDataTarefa.text.isNullOrEmpty()) {
                 Toast.makeText(requireContext(), "Selecione uma data de prazo primeiro.", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
            }
            val cal = prazoCalendar!!
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                prazoCalendar!!.set(Calendar.HOUR_OF_DAY, hourOfDay); prazoCalendar!!.set(Calendar.MINUTE, minute)
                binding.editTextPrazoHoraTarefa.setText(timeFormat.format(prazoCalendar!!.time))
                atualizarEstadoSpinnerLembrete()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }
    }

    private fun atualizarEstadoSpinnerLembrete() {
        val prazoTotalmenteDefinido = prazoCalendar != null &&
                                    !binding.editTextPrazoDataTarefa.text.isNullOrEmpty() &&
                                    !binding.editTextPrazoHoraTarefa.text.isNullOrEmpty()
        binding.spinnerLembreteTarefa.isEnabled = prazoTotalmenteDefinido
        binding.textViewLabelLembrete.isEnabled = prazoTotalmenteDefinido
        if (!prazoTotalmenteDefinido) {
            binding.spinnerLembreteTarefa.setSelection(0)
        }
    }

    private fun carregarTarefaParaEdicao(id: Int) {
        lifecycleScope.launch {
            tarefaParaEdicao = withContext(Dispatchers.IO) { tarefaDao.buscarPorId(id) }
            tarefaParaEdicao?.let { tarefa ->
                binding.editTextDescricaoTarefa.setText(tarefa.descricao)
                prazoCalendar = null
                binding.editTextPrazoDataTarefa.setText("")
                binding.editTextPrazoHoraTarefa.setText("")
                if (tarefa.prazoData != null) {
                    try {
                        val date = storeDateFormat.parse(tarefa.prazoData!!)
                        prazoCalendar = Calendar.getInstance().apply { time = date!! }
                        binding.editTextPrazoDataTarefa.setText(displayDateFormat.format(prazoCalendar!!.time))
                        if (tarefa.prazoHora != null) {
                            val timeCal = Calendar.getInstance()
                            timeCal.time = timeFormat.parse(tarefa.prazoHora!!) ?: Date()
                            prazoCalendar!!.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                            prazoCalendar!!.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                            binding.editTextPrazoHoraTarefa.setText(tarefa.prazoHora)
                        }
                    } catch (e: ParseException) { prazoCalendar = null }
                }
                binding.spinnerPrioridadeTarefa.setSelection(tarefa.prioridade)

                if(listaDisciplinas.isNotEmpty() || listaTurmas.isNotEmpty()){
                    selecionarValoresSpinnerEdicao(tarefa)
                }

                if (tarefa.lembreteConfigurado && tarefa.lembreteDateTime != null && prazoCalendar != null) {
                    val diffMillis = prazoCalendar!!.timeInMillis - tarefa.lembreteDateTime!!
                    val indexNoSpinner = deltasLembreteMillis.indexOfFirst { it == diffMillis }
                    if (indexNoSpinner != -1 && indexNoSpinner < binding.spinnerLembreteTarefa.adapter.count) {
                        binding.spinnerLembreteTarefa.setSelection(indexNoSpinner)
                    } else { binding.spinnerLembreteTarefa.setSelection(0) }
                } else { binding.spinnerLembreteTarefa.setSelection(0) }
                atualizarEstadoSpinnerLembrete()

                // Carregar subtarefas
                subtarefaDao.buscarPorTarefaId(tarefa.id).collect { subtarefasDoBanco ->
                    listaSubtarefas.clear()
                    listaSubtarefas.addAll(subtarefasDoBanco)
                    subtarefaAdapter.submitList(listaSubtarefas.toList())
                }
            }
        }
    }

    private fun selecionarValoresSpinnerEdicao(tarefa: Tarefa){
        val discPos = tarefa.disciplinaId?.let { discId -> listaDisciplinas.indexOfFirst { it.id == discId } } ?: -1
        binding.spinnerDisciplinaTarefa.setSelection(if (discPos != -1) discPos + 1 else 0)
        val turmaPos = tarefa.turmaId?.let { turId -> listaTurmas.indexOfFirst { it.id == turId } } ?: -1
        binding.spinnerTurmaTarefa.setSelection(if (turmaPos != -1) turmaPos + 1 else 0)
    }

    private fun salvarTarefaESubtarefas() {
        val descricao = binding.editTextDescricaoTarefa.text.toString().trim()
        if (descricao.isEmpty()) {
            binding.editTextDescricaoTarefa.error = "Descrição é obrigatória"; return
        } else { binding.editTextDescricaoTarefa.error = null }

        val prazoDataStr: String? = if (prazoCalendar != null && !binding.editTextPrazoDataTarefa.text.isNullOrEmpty()) storeDateFormat.format(prazoCalendar!!.time) else null
        val prazoHoraStr: String? = if (prazoCalendar != null && prazoDataStr != null && !binding.editTextPrazoHoraTarefa.text.isNullOrEmpty()) timeFormat.format(prazoCalendar!!.time) else null

        if (prazoDataStr == null && prazoHoraStr != null) {
            Toast.makeText(requireContext(), "Selecione uma data para o prazo se uma hora foi definida.", Toast.LENGTH_SHORT).show(); return
        }
        if (prazoDataStr != null && prazoHoraStr == null) {
             Toast.makeText(requireContext(), "Defina uma hora para o prazo ou limpe a data.", Toast.LENGTH_SHORT).show(); return
        }

        val prioridade = binding.spinnerPrioridadeTarefa.selectedItemPosition
        val disciplinaIdSelecionada: Int? = if (binding.spinnerDisciplinaTarefa.selectedItemPosition > 0 && listaDisciplinas.isNotEmpty()) listaDisciplinas[binding.spinnerDisciplinaTarefa.selectedItemPosition - 1].id else null
        val turmaIdSelecionada: Int? = if (binding.spinnerTurmaTarefa.selectedItemPosition > 0 && listaTurmas.isNotEmpty()) listaTurmas[binding.spinnerTurmaTarefa.selectedItemPosition - 1].id else null

        var lembreteConfig = false
        var lembreteTs: Long? = null
        val lembreteSpinnerPos = binding.spinnerLembreteTarefa.selectedItemPosition
        if (prazoCalendar != null && prazoDataStr != null && prazoHoraStr != null && lembreteSpinnerPos > 0 && lembreteSpinnerPos < deltasLembreteMillis.size) {
            lembreteTs = prazoCalendar!!.timeInMillis - deltasLembreteMillis[lembreteSpinnerPos]
            lembreteConfig = true
        }

        val tarefaParaSalvar = Tarefa(
            id = tarefaIdParaEdicao ?: 0,
            descricao = descricao,
            prazoData = prazoDataStr,
            prazoHora = prazoHoraStr,
            prioridade = prioridade,
            disciplinaId = disciplinaIdSelecionada,
            turmaId = turmaIdSelecionada,
            concluida = tarefaParaEdicao?.concluida ?: false,
            dataCriacao = tarefaParaEdicao?.dataCriacao ?: System.currentTimeMillis(),
            dataConclusao = tarefaParaEdicao?.dataConclusao,
            lembreteConfigurado = lembreteConfig,
            lembreteDateTime = lembreteTs
        )

        // A Activity agora é responsável por salvar a tarefa principal e depois as subtarefas
        listener?.onTarefaSalva(tarefaParaSalvar, listaSubtarefas.toList()) // Passa a lista atual de subtarefas
        dismiss()
    }

    override fun onColorSelected(color: Int, tag: String?) { /* Não aplicável */ }

    fun setListener(listener: FormularioTarefaListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
