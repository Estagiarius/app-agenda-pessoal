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
import com.agendafocopei.data.EventoRecorrenteDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.FragmentDiaBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment // Import do BottomSheet
import com.agendafocopei.ui.model.ItemAgendaDashboard
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DiaFragment : Fragment() {

    private var _binding: FragmentDiaBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao
    private lateinit var agendaAdapter: AgendaHojeAdapter // Renomeado de agendaHojeAdapter para agendaAdapter para consistência
    private var dataSelecionada: Calendar = Calendar.getInstance()

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
        eventoRecorrenteDao = appDatabase.eventoRecorrenteDao()

        setupRecyclerView()
        setupDatePicker()

        carregarEventosDoDia() // Carrega para a data atual inicialmente
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaHojeAdapter { item ->
            val (id, type) = when (item) {
                is ItemAgendaDashboard.AulaItem -> Pair(item.aula.id, "aula")
                is ItemAgendaDashboard.EventoItem -> Pair(item.evento.id, "evento")
            }
            DetalhesEventoSheetFragment.newInstance(id, type)
                .show(childFragmentManager, DetalhesEventoSheetFragment.TAG) // Usar childFragmentManager
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
        // Formato: "Segunda, 10 de Junho de 2024"
        val dateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        var dataFormatada = dateFormat.format(dataSelecionada.time)
        // Capitaliza a primeira letra do dia da semana e do mês
        dataFormatada = dataFormatada.split(" ").map { palavra ->
            palavra.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        }.joinToString(" ")
        binding.textViewDataSelecionadaDia.text = dataFormatada
    }

    private fun carregarEventosDoDia() {
        val diaSemanaQuery = dataSelecionada.get(Calendar.DAY_OF_WEEK)

        viewLifecycleOwner.lifecycleScope.launch {
            horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaQuery)
                .combine(eventoRecorrenteDao.buscarPorDia(diaSemanaQuery)) { aulasDoDia, eventosDoDia ->
                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    eventosDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.EventoItem(it)) }

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
