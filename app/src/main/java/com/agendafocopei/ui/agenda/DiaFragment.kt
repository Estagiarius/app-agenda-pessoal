package com.agendafocopei.ui.agenda

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.EventoDao // Usar EventoDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.FragmentDiaBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment
import com.agendafocopei.ui.model.ItemAgendaDashboard
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DiaFragment : Fragment() {

    private var _binding: FragmentDiaBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao // Usar EventoDao
    private lateinit var agendaAdapter: AgendaHojeAdapter
    private var dataSelecionada: Calendar = Calendar.getInstance()

    private val displayDateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
    private val queryDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appDatabase = AppDatabase.getDatabase(requireContext())
        horarioAulaDao = appDatabase.horarioAulaDao()
        eventoDao = appDatabase.eventoDao() // Usar eventoDao()

        setupRecyclerView()
        setupDatePicker()

        carregarEventosDoDia()
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaHojeAdapter { item ->
            val (id, type) = when (item) {
                is ItemAgendaDashboard.AulaItem -> Pair(item.aula.id, "aula")
                is ItemAgendaDashboard.EventoItem -> Pair(item.evento.id, "evento")
            }
            DetalhesEventoSheetFragment.newInstance(id, type)
                .show(childFragmentManager, DetalhesEventoSheetFragment.TAG)
        }
        binding.recyclerViewEventosDia.apply {
            adapter = agendaAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupDatePicker() {
        atualizarTextViewData()
        binding.buttonSelecionarDataDia.setOnClickListener {
            val year = dataSelecionada.get(Calendar.YEAR)
            val month = dataSelecionada.get(Calendar.MONTH)
            val day = dataSelecionada.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                dataSelecionada.set(selectedYear, selectedMonth, selectedDayOfMonth)
                atualizarTextViewData()
                carregarEventosDoDia()
            }, year, month, day)
            datePickerDialog.show()
        }
    }

    private fun atualizarTextViewData() {
        var dataFormatada = displayDateFormat.format(dataSelecionada.time)
        dataFormatada = dataFormatada.split(" ").map { palavra ->
            palavra.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        }.joinToString(" ")
        binding.textViewDataSelecionadaDia.text = dataFormatada
    }

    private fun carregarEventosDoDia() {
        val diaSemanaQuery = dataSelecionada.get(Calendar.DAY_OF_WEEK)
        val dataFormatadaQuery = queryDateFormat.format(dataSelecionada.time)

        viewLifecycleOwner.lifecycleScope.launch {
            horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaQuery)
                .combine(eventoDao.buscarEventosParaData(diaSemanaQuery, dataFormatadaQuery)) { aulasDoDia, eventosDoDia -> // Usar query correta
                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    // EventoItem agora espera Evento
                    eventosDoDia.forEach { evento -> itensCombinados.add(ItemAgendaDashboard.EventoItem(evento)) }

                    itensCombinados.sortBy { it.horaInicio }
                    itensCombinados
                }.collect { itensAgrupadosEOrdenados ->
                    agendaAdapter.submitList(itensAgrupadosEOrdenados)
                    binding.textViewNenhumEventoDia.visibility = if (itensAgrupadosEOrdenados.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewEventosDia.visibility = if (itensAgrupadosEOrdenados.isEmpty()) View.GONE else View.VISIBLE
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
