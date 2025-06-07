package com.agendafocopei.ui.dialog

import android.app.DatePickerDialog
import android.app.Dialog
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
import com.agendafocopei.R
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Evento // Atualizado para Evento
import com.agendafocopei.data.EventoDao // Atualizado para EventoDao
import com.agendafocopei.databinding.DialogFormularioEventoBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class FormularioEventoDialogFragment : DialogFragment(), ColorPickerDialogFragment.ColorPickerListener {

    private var _binding: DialogFormularioEventoBinding? = null
    private val binding get() = _binding!!

    interface FormularioEventoListener {
        fun onEventoSalvo(evento: Evento) // Atualizado para Evento
    }

    private var listener: FormularioEventoListener? = null
    private var eventoIdParaEdicao: Int? = null
    private var eventoParaEdicao: Evento? = null // Atualizado para Evento

    private lateinit var eventoDao: EventoDao // Atualizado para EventoDao
    private var corSelecionadaEvento: Int? = null
    private var dataEspecificaSelecionada: Calendar? = null // Novo para data específica

    private var horaInicioSelecionada: Calendar = Calendar.getInstance()
    private var horaFimSelecionada: Calendar = Calendar.getInstance()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatterDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateFormatterStore = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    companion object {
        const val TAG = "FormularioEventoDialog"
        private const val ARG_EVENTO_ID = "arg_evento_id"
        private const val COLOR_PICKER_REQUEST_TAG = "evento_color_picker"

        fun newInstance(eventoId: Int? = null): FormularioEventoDialogFragment {
            val fragment = FormularioEventoDialogFragment()
            val args = Bundle()
            eventoId?.let { args.putInt(ARG_EVENTO_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_EVENTO_ID)) {
                eventoIdParaEdicao = it.getInt(ARG_EVENTO_ID)
            }
        }
        eventoDao = AppDatabase.getDatabase(requireContext()).eventoDao() // Atualizado para eventoDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioEventoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDiaSemanaSpinner()
        setupTimePickers()
        setupColorPickerButton()
        setupCheckBoxDiaUnico() // Novo
        setupDatePickerDataEspecifica() // Novo

        if (eventoIdParaEdicao != null) {
            binding.textViewFormularioEventoTitulo.text = "Editar Evento"
            carregarEventoParaEdicao(eventoIdParaEdicao!!)
        } else {
            binding.textViewFormularioEventoTitulo.text = "Adicionar Evento"
            // Valores padrão para novo evento
            binding.checkBoxEventoDiaUnico.isChecked = false // Default para recorrente
            controlarVisibilidadeCamposData(false)
            horaInicioSelecionada.set(Calendar.MINUTE, 0)
            horaFimSelecionada.set(Calendar.MINUTE, 0)
            horaFimSelecionada.add(Calendar.HOUR_OF_DAY, 1)
            atualizarTextoHora(binding.editTextHoraInicioEvento, horaInicioSelecionada)
            atualizarTextoHora(binding.editTextHoraFimEvento, horaFimSelecionada)
            updateColorPreview()
        }

        binding.buttonSalvarEvento.setOnClickListener { salvarEvento() }
        binding.buttonCancelarEvento.setOnClickListener { dismiss() }
    }

    private fun controlarVisibilidadeCamposData(isDiaUnico: Boolean) {
        binding.layoutDataEspecificaEvento.visibility = if (isDiaUnico) View.VISIBLE else View.GONE
        binding.layoutDiaSemanaEvento.visibility = if (isDiaUnico) View.GONE else View.VISIBLE
    }

    private fun setupCheckBoxDiaUnico() {
        binding.checkBoxEventoDiaUnico.setOnCheckedChangeListener { _, isChecked ->
            controlarVisibilidadeCamposData(isChecked)
            if (!isChecked) {
                dataEspecificaSelecionada = null
                binding.editTextDataEspecificaEvento.text = null
            } else {
                if (dataEspecificaSelecionada == null) { // Abrir DatePicker se não houver data e marcar dia único
                    abrirDatePickerParaDataEspecifica()
                }
            }
        }
    }

    private fun setupDatePickerDataEspecifica() {
        binding.editTextDataEspecificaEvento.setOnClickListener {
            abrirDatePickerParaDataEspecifica()
        }
    }

    private fun abrirDatePickerParaDataEspecifica() {
        val calendarToShow = dataEspecificaSelecionada ?: Calendar.getInstance()
        val year = calendarToShow.get(Calendar.YEAR)
        val month = calendarToShow.get(Calendar.MONTH)
        val day = calendarToShow.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            if (dataEspecificaSelecionada == null) dataEspecificaSelecionada = Calendar.getInstance()
            dataEspecificaSelecionada!!.set(selectedYear, selectedMonth, selectedDayOfMonth)
            binding.editTextDataEspecificaEvento.setText(dateFormatterDisplay.format(dataEspecificaSelecionada!!.time))
        }, year, month, day).show()
    }


    private fun setupDiaSemanaSpinner() {
        val diasSemana = resources.getStringArray(R.array.dias_semana_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, diasSemana).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerDiaSemanaEvento.adapter = adapter
    }

    private fun setupTimePickers() {
        binding.editTextHoraInicioEvento.setOnClickListener { mostrarTimePicker(it as TextInputEditText, horaInicioSelecionada) }
        binding.editTextHoraFimEvento.setOnClickListener { mostrarTimePicker(it as TextInputEditText, horaFimSelecionada) }
    }

    private fun setupColorPickerButton() {
        binding.buttonEscolherCorEvento.setOnClickListener {
            val dialog = ColorPickerDialogFragment.newInstance(corSelecionadaEvento, COLOR_PICKER_REQUEST_TAG)
            dialog.show(childFragmentManager, ColorPickerDialogFragment.TAG)
        }
    }

    private fun mostrarTimePicker(editText: TextInputEditText, calendar: Calendar) {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            atualizarTextoHora(editText, calendar)
        }, currentHour, currentMinute, true).show()
    }

    private fun atualizarTextoHora(editText: TextInputEditText, calendar: Calendar) {
        editText.setText(timeFormatter.format(calendar.time))
    }

    private fun updateColorPreview() {
        binding.viewPreviewCorSelecionadaEvento.setBackgroundColor(corSelecionadaEvento ?: Color.TRANSPARENT)
    }

    private fun carregarEventoParaEdicao(id: Int) {
        lifecycleScope.launch {
            eventoParaEdicao = withContext(Dispatchers.IO) { eventoDao.buscarPorId(id) }
            eventoParaEdicao?.let { evento ->
                binding.editTextNomeEvento.setText(evento.nomeEvento)

                if (evento.dataEspecifica != null) {
                    binding.checkBoxEventoDiaUnico.isChecked = true
                    controlarVisibilidadeCamposData(true)
                    try {
                        val date = dateFormatterStore.parse(evento.dataEspecifica!!)
                        dataEspecificaSelecionada = Calendar.getInstance().apply { time = date!! }
                        binding.editTextDataEspecificaEvento.setText(dateFormatterDisplay.format(dataEspecificaSelecionada!!.time))
                    } catch (e: ParseException) {
                        Toast.makeText(context, "Erro ao carregar data.", Toast.LENGTH_SHORT).show()
                        dataEspecificaSelecionada = null // Reset
                        binding.editTextDataEspecificaEvento.text = null
                        binding.checkBoxEventoDiaUnico.isChecked = false // Volta para recorrente
                        controlarVisibilidadeCamposData(false)
                    }
                } else {
                    binding.checkBoxEventoDiaUnico.isChecked = false
                    controlarVisibilidadeCamposData(false)
                    // Ajuste para mapear Calendar.DAY_OF_WEEK (Dom=1..Sab=7) para índice do spinner (Seg=0..Dom=6)
                    val spinnerPosition = when (evento.diaDaSemana) {
                        Calendar.SUNDAY -> 6 // Último item no array "Seg-Dom"
                        else -> evento.diaDaSemana - Calendar.MONDAY // Seg=0, Ter=1...
                    }
                    if (spinnerPosition >= 0 && spinnerPosition < binding.spinnerDiaSemanaEvento.adapter.count) {
                        binding.spinnerDiaSemanaEvento.setSelection(spinnerPosition)
                    }
                }

                try {
                    horaInicioSelecionada.time = timeFormatter.parse(evento.horaInicio) ?: Date()
                    horaFimSelecionada.time = timeFormatter.parse(evento.horaFim) ?: Date()
                } catch (e: Exception) { /* Tratar erro */ }
                atualizarTextoHora(binding.editTextHoraInicioEvento, horaInicioSelecionada)
                atualizarTextoHora(binding.editTextHoraFimEvento, horaFimSelecionada)

                binding.editTextSalaLocalEvento.setText(evento.salaLocal)
                binding.editTextObservacoesEvento.setText(evento.observacoes)
                corSelecionadaEvento = evento.cor
                updateColorPreview()
            }
        }
    }

    private fun getDiaDaSemanaSelecionadoDoSpinner(): Int {
        // Mapeia a posição do spinner (Seg=0..Dom=6) para Calendar.DAY_OF_WEEK (Dom=1..Sab=7)
        val selectedPosition = binding.spinnerDiaSemanaEvento.selectedItemPosition
        return when (selectedPosition) {
            6 -> Calendar.SUNDAY // Domingo
            else -> selectedPosition + Calendar.MONDAY // Segunda a Sábado
        }
    }

    private fun salvarEvento() {
        val nomeEvento = binding.editTextNomeEvento.text.toString().trim()
        val horaInicioStr = binding.editTextHoraInicioEvento.text.toString()
        val horaFimStr = binding.editTextHoraFimEvento.text.toString()
        val salaLocal = binding.editTextSalaLocalEvento.text.toString().trim()
        val observacoes = binding.editTextObservacoesEvento.text.toString().trim()

        val isDiaUnico = binding.checkBoxEventoDiaUnico.isChecked
        var diaSemanaParaSalvar: Int
        var dataEspecificaParaSalvar: String? = null

        if (nomeEvento.isEmpty()) {
            binding.editTextNomeEvento.error = "Nome do evento é obrigatório"
            return
        } else {
            binding.editTextNomeEvento.error = null
        }
        if (horaInicioStr.isEmpty() || horaFimStr.isEmpty()) {
            Toast.makeText(context, "Horas de início e fim são obrigatórias.", Toast.LENGTH_SHORT).show()
            return
        }
        if (horaInicioSelecionada.after(horaFimSelecionada) || horaInicioSelecionada.equals(horaFimSelecionada)) {
            Toast.makeText(context, "Hora de fim deve ser após a hora de início.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isDiaUnico) {
            if (dataEspecificaSelecionada == null) {
                Toast.makeText(context, "Selecione uma data para o evento de dia único.", Toast.LENGTH_SHORT).show()
                binding.editTextDataEspecificaEvento.requestFocus() // Ou similar
                return
            }
            dataEspecificaParaSalvar = dateFormatterStore.format(dataEspecificaSelecionada!!.time)
            diaSemanaParaSalvar = dataEspecificaSelecionada!!.get(Calendar.DAY_OF_WEEK)
        } else {
            diaSemanaParaSalvar = getDiaDaSemanaSelecionadoDoSpinner()
        }

        val eventoParaSalvar = Evento(
            id = eventoIdParaEdicao ?: 0,
            nomeEvento = nomeEvento,
            diaDaSemana = diaSemanaParaSalvar,
            horaInicio = horaInicioStr,
            horaFim = horaFimStr,
            salaLocal = salaLocal.ifEmpty { null },
            cor = corSelecionadaEvento,
            observacoes = observacoes.ifEmpty { null },
            dataEspecifica = dataEspecificaParaSalvar
        )

        listener?.onEventoSalvo(eventoParaSalvar)
        dismiss()
    }

    override fun onColorSelected(color: Int, tag: String?) {
        if (tag == COLOR_PICKER_REQUEST_TAG) {
            corSelecionadaEvento = color
            updateColorPreview()
        }
    }

    fun setListener(listener: FormularioEventoListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
