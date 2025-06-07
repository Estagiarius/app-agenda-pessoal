package com.agendafocopei.ui.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.R
import com.agendafocopei.data.*
import com.agendafocopei.databinding.DialogFormularioHorarioBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FormularioHorarioDialogFragment : DialogFragment() {

    private var _binding: DialogFormularioHorarioBinding? = null
    private val binding get() = _binding!!

    interface FormularioHorarioListener {
        fun onHorarioSalvo(horarioAula: HorarioAula)
    }

    private var listener: FormularioHorarioListener? = null
    private var horarioIdParaEdicao: Int? = null

    private lateinit var disciplinaDao: DisciplinaDao
    private lateinit var turmaDao: TurmaDao
    private lateinit var horarioAulaDao: HorarioAulaDao

    private var listaDisciplinas: List<Disciplina> = emptyList()
    private var listaTurmas: List<Turma> = emptyList()

    private var horaInicioSelecionada: Calendar = Calendar.getInstance()
    private var horaFimSelecionada: Calendar = Calendar.getInstance()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    companion object {
        const val TAG = "FormularioHorarioDialog"
        private const val ARG_HORARIO_ID = "arg_horario_id"

        fun newInstance(horarioId: Int? = null): FormularioHorarioDialogFragment {
            val fragment = FormularioHorarioDialogFragment()
            val args = Bundle()
            horarioId?.let { args.putInt(ARG_HORARIO_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_HORARIO_ID)) {
                horarioIdParaEdicao = it.getInt(ARG_HORARIO_ID)
            }
        }
        val appDatabase = AppDatabase.getDatabase(requireContext())
        disciplinaDao = appDatabase.disciplinaDao()
        turmaDao = appDatabase.turmaDao()
        horarioAulaDao = appDatabase.horarioAulaDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioHorarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupTimePickers()

        if (horarioIdParaEdicao != null) {
            binding.textViewFormularioHorarioTitulo.text = "Editar Horário"
            carregarHorarioParaEdicao(horarioIdParaEdicao!!)
        } else {
            binding.textViewFormularioHorarioTitulo.text = "Adicionar Horário"
            // Configurar horaInicio e horaFim para valores padrão (ex: próxima hora cheia)
            horaInicioSelecionada.set(Calendar.MINUTE, 0)
            horaFimSelecionada.set(Calendar.MINUTE, 0)
            horaFimSelecionada.add(Calendar.HOUR_OF_DAY, 1) // Default 1 hora de duração
            atualizarTextoHora(binding.editTextHoraInicio, horaInicioSelecionada)
            atualizarTextoHora(binding.editTextHoraFim, horaFimSelecionada)
        }

        binding.buttonSalvarHorario.setOnClickListener { salvarHorario() }
        binding.buttonCancelarHorario.setOnClickListener { dismiss() }
    }

    private fun setupSpinners() {
        // Dia da Semana
        val diasSemana = resources.getStringArray(R.array.dias_semana_array) // Necessário criar este array em strings.xml
        val diaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, diasSemana).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerDiaSemana.adapter = diaAdapter

        // Disciplinas e Turmas (carregados de forma assíncrona)
        lifecycleScope.launch {
            listaDisciplinas = withContext(Dispatchers.IO) { disciplinaDao.buscarTodas().first() }
            val nomesDisciplinas = listaDisciplinas.map { it.nome }
            val disciplinaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesDisciplinas).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerDisciplinaHorario.adapter = disciplinaAdapter

            listaTurmas = withContext(Dispatchers.IO) { turmaDao.buscarTodas().first() }
            val nomesTurmas = listaTurmas.map { it.nome }
            val turmaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesTurmas).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerTurmaHorario.adapter = turmaAdapter

            // Se estiver editando, define as seleções após os adapters serem populados
            horarioIdParaEdicao?.let { carregarHorarioParaEdicao(it) }
        }
    }

    private fun setupTimePickers() {
        binding.editTextHoraInicio.setOnClickListener { mostrarTimePicker(it as TextInputEditText, horaInicioSelecionada) }
        binding.editTextHoraFim.setOnClickListener { mostrarTimePicker(it as TextInputEditText, horaFimSelecionada) }
    }

    private fun mostrarTimePicker(editText: TextInputEditText, calendar: Calendar) {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            atualizarTextoHora(editText, calendar)
        }, currentHour, currentMinute, true).show() // true para formato 24h
    }

    private fun atualizarTextoHora(editText: TextInputEditText, calendar: Calendar) {
        editText.setText(timeFormatter.format(calendar.time))
    }

    private fun carregarHorarioParaEdicao(id: Int) {
        lifecycleScope.launch {
            val horario = withContext(Dispatchers.IO) { horarioAulaDao.buscarPorId(id) }
            if (horario != null) {
                // Dia da semana (ajustar índice se o array for 0-based e diaDaSemana 1-based)
                // Supondo que o array de strings é 0=Seg, 1=Ter... e diaDaSemana é 2=Seg, 3=Ter...
                // Precisa de um mapeamento mais robusto aqui se os valores não baterem diretamente com o índice.
                // Para Calendar.DAY_OF_WEEK: Domingo=1, Segunda=2... Sábado=7
                // Se o array for Seg-Sex/Sab:
                val diasNoSpinner = resources.getStringArray(R.array.dias_semana_array).size
                if(horario.diaDaSemana >= Calendar.MONDAY && horario.diaDaSemana <= Calendar.MONDAY + diasNoSpinner -1) {
                     binding.spinnerDiaSemana.setSelection(horario.diaDaSemana - Calendar.MONDAY)
                }


                try {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    horaInicioSelecionada.time = sdf.parse(horario.horaInicio) ?: Date()
                    horaFimSelecionada.time = sdf.parse(horario.horaFim) ?: Date()
                } catch (e: Exception) { /* Tratar erro de parse */ }
                atualizarTextoHora(binding.editTextHoraInicio, horaInicioSelecionada)
                atualizarTextoHora(binding.editTextHoraFim, horaFimSelecionada)

                val disciplinaIndex = listaDisciplinas.indexOfFirst { it.id == horario.disciplinaId }
                if (disciplinaIndex != -1) binding.spinnerDisciplinaHorario.setSelection(disciplinaIndex)

                val turmaIndex = listaTurmas.indexOfFirst { it.id == horario.turmaId }
                if (turmaIndex != -1) binding.spinnerTurmaHorario.setSelection(turmaIndex)

                binding.editTextSalaAula.setText(horario.salaAula)
            }
        }
    }

    private fun getDiaDaSemanaSelecionado(): Int {
        // Supondo que o array dias_semana_array em strings.xml comece com "Segunda-feira" (índice 0)
        // E Calendar.DAY_OF_WEEK: Segunda=2, Terça=3, ..., Sábado=7, Domingo=1
        val posicaoSelecionada = binding.spinnerDiaSemana.selectedItemPosition
        // Mapeia a posição do spinner para o valor Calendar.DAY_OF_WEEK
        // Ex: Se o spinner é ["Segunda", "Terça", ...]:
        // Seg (pos 0) -> Calendar.MONDAY (2)
        // Ter (pos 1) -> Calendar.TUESDAY (3)
        return Calendar.MONDAY + posicaoSelecionada // Ajuste conforme a ordem do seu array
    }


    private fun salvarHorario() {
        val diaSemana = getDiaDaSemanaSelecionado()
        val horaInicioStr = binding.editTextHoraInicio.text.toString()
        val horaFimStr = binding.editTextHoraFim.text.toString()
        val disciplinaSelecionada = if (binding.spinnerDisciplinaHorario.selectedItemPosition >= 0 && listaDisciplinas.isNotEmpty()) {
            listaDisciplinas[binding.spinnerDisciplinaHorario.selectedItemPosition]
        } else null
        val turmaSelecionada = if (binding.spinnerTurmaHorario.selectedItemPosition >= 0 && listaTurmas.isNotEmpty()) {
            listaTurmas[binding.spinnerTurmaHorario.selectedItemPosition]
        } else null
        val salaAula = binding.editTextSalaAula.text.toString().trim()

        if (horaInicioStr.isEmpty() || horaFimStr.isEmpty()) {
            Toast.makeText(context, "Horas de início e fim são obrigatórias.", Toast.LENGTH_SHORT).show()
            return
        }
        if (disciplinaSelecionada == null) {
            Toast.makeText(context, "Selecione uma disciplina.", Toast.LENGTH_SHORT).show()
            return
        }
        if (turmaSelecionada == null) {
            Toast.makeText(context, "Selecione uma turma.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validação de hora (fim > início)
        if(horaInicioSelecionada.after(horaFimSelecionada) || horaInicioSelecionada.equals(horaFimSelecionada)){
            Toast.makeText(context, "Hora de fim deve ser após a hora de início.", Toast.LENGTH_SHORT).show()
            return
        }

        val horarioParaSalvar = HorarioAula(
            id = horarioIdParaEdicao ?: 0, // Se for edição, usa o ID existente, senão 0 para novo
            diaDaSemana = diaSemana,
            horaInicio = horaInicioStr,
            horaFim = horaFimStr,
            disciplinaId = disciplinaSelecionada.id,
            turmaId = turmaSelecionada.id,
            salaAula = salaAula.ifEmpty { null }
        )

        listener?.onHorarioSalvo(horarioParaSalvar)
        dismiss()
    }

    fun setListener(listener: FormularioHorarioListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
