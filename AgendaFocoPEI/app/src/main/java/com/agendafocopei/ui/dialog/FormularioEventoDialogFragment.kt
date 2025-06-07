package com.agendafocopei.ui.dialog

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
import com.agendafocopei.data.EventoRecorrente
import com.agendafocopei.data.EventoRecorrenteDao
import com.agendafocopei.databinding.DialogFormularioEventoBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FormularioEventoDialogFragment : DialogFragment(), ColorPickerDialogFragment.ColorPickerListener {

    private var _binding: DialogFormularioEventoBinding? = null
    private val binding get() = _binding!!

    interface FormularioEventoListener {
        fun onEventoSalvo(evento: EventoRecorrente)
    }

    private var listener: FormularioEventoListener? = null
    private var eventoIdParaEdicao: Int? = null
    private var eventoParaEdicao: EventoRecorrente? = null

    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao
    private var corSelecionadaEvento: Int? = null

    private var horaInicioSelecionada: Calendar = Calendar.getInstance()
    private var horaFimSelecionada: Calendar = Calendar.getInstance()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

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
        eventoRecorrenteDao = AppDatabase.getDatabase(requireContext()).eventoRecorrenteDao()
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

        if (eventoIdParaEdicao != null) {
            binding.textViewFormularioEventoTitulo.text = "Editar Evento Recorrente"
            carregarEventoParaEdicao(eventoIdParaEdicao!!)
        } else {
            binding.textViewFormularioEventoTitulo.text = "Adicionar Evento Recorrente"
            horaInicioSelecionada.set(Calendar.MINUTE, 0) // Hora cheia
            horaFimSelecionada.set(Calendar.MINUTE, 0)
            horaFimSelecionada.add(Calendar.HOUR_OF_DAY, 1) // Default 1 hora
            atualizarTextoHora(binding.editTextHoraInicioEvento, horaInicioSelecionada)
            atualizarTextoHora(binding.editTextHoraFimEvento, horaFimSelecionada)
            updateColorPreview()
        }

        binding.buttonSalvarEvento.setOnClickListener { salvarEvento() }
        binding.buttonCancelarEvento.setOnClickListener { dismiss() }
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
            // O listener será setado por este fragmento implementar a interface e o dialog procurar por ele no childFragmentManager
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
            eventoParaEdicao = withContext(Dispatchers.IO) { eventoRecorrenteDao.buscarPorId(id) }
            eventoParaEdicao?.let { evento ->
                binding.editTextNomeEvento.setText(evento.nomeEvento)
                // Mapear diaDaSemana (Calendar.DAY_OF_WEEK) para índice do spinner (0-based)
                // Ex: se array é Seg-Dom, e Calendar.MONDAY = 2, então índice = dia - 2
                val spinnerPosition = evento.diaDaSemana - Calendar.MONDAY // Ajuste se seu array for diferente
                if (spinnerPosition >= 0 && spinnerPosition < binding.spinnerDiaSemanaEvento.adapter.count) {
                    binding.spinnerDiaSemanaEvento.setSelection(spinnerPosition)
                }

                try {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    horaInicioSelecionada.time = sdf.parse(evento.horaInicio) ?: Date()
                    horaFimSelecionada.time = sdf.parse(evento.horaFim) ?: Date()
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

    private fun getDiaDaSemanaSelecionado(): Int {
        // Mapeia a posição do spinner (0-based, Seg-Dom) para Calendar.DAY_OF_WEEK
        return binding.spinnerDiaSemanaEvento.selectedItemPosition + Calendar.MONDAY
    }

    private fun salvarEvento() {
        val nomeEvento = binding.editTextNomeEvento.text.toString().trim()
        val diaSemana = getDiaDaSemanaSelecionado()
        val horaInicioStr = binding.editTextHoraInicioEvento.text.toString()
        val horaFimStr = binding.editTextHoraFimEvento.text.toString()
        val salaLocal = binding.editTextSalaLocalEvento.text.toString().trim()
        val observacoes = binding.editTextObservacoesEvento.text.toString().trim()

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

        val eventoParaSalvar = EventoRecorrente(
            id = eventoIdParaEdicao ?: 0,
            nomeEvento = nomeEvento,
            diaDaSemana = diaSemana,
            horaInicio = horaInicioStr,
            horaFim = horaFimStr,
            salaLocal = salaLocal.ifEmpty { null },
            cor = corSelecionadaEvento,
            observacoes = observacoes.ifEmpty { null }
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
