package com.agendafocopei.ui.agenda

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
import com.agendafocopei.databinding.FragmentSemanaBinding
import com.agendafocopei.ui.adapter.SemanaDiasAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment
import com.agendafocopei.ui.model.ItemAgendaDashboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SemanaFragment : Fragment() {

    private var _binding: FragmentSemanaBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao // Usar EventoDao
    private lateinit var semanaDiasAdapter: SemanaDiasAdapter
    private var calendarAtualPrimeiroDiaDaSemana: Calendar = Calendar.getInstance()

    private val localePtBr = Locale("pt", "BR")
    private val shortDateFormat = SimpleDateFormat("dd/MM", localePtBr)
    private val queryDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSemanaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appDatabase = AppDatabase.getDatabase(requireContext())
        horarioAulaDao = appDatabase.horarioAulaDao()
        eventoDao = appDatabase.eventoDao() // Usar eventoDao()

        setupRecyclerView()
        setupNavigationButtons()

        calendarAtualPrimeiroDiaDaSemana.firstDayOfWeek = Calendar.MONDAY
        calendarAtualPrimeiroDiaDaSemana.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        carregarDadosDaSemana()
    }

    private fun setupRecyclerView() {
        semanaDiasAdapter = SemanaDiasAdapter { eventoCompacto ->
            DetalhesEventoSheetFragment.newInstance(eventoCompacto.idOriginal, eventoCompacto.tipo)
                .show(childFragmentManager, DetalhesEventoSheetFragment.TAG)
        }
        binding.recyclerViewSemana.apply {
            adapter = semanaDiasAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupNavigationButtons() {
        binding.buttonSemanaAnterior.setOnClickListener {
            calendarAtualPrimeiroDiaDaSemana.add(Calendar.WEEK_OF_YEAR, -1)
            carregarDadosDaSemana()
        }
        binding.buttonProximaSemana.setOnClickListener {
            calendarAtualPrimeiroDiaDaSemana.add(Calendar.WEEK_OF_YEAR, 1)
            carregarDadosDaSemana()
        }
    }

    private fun atualizarIntervaloSemanaTextView() {
        val inicioSemana = calendarAtualPrimeiroDiaDaSemana.clone() as Calendar
        val fimSemana = calendarAtualPrimeiroDiaDaSemana.clone() as Calendar
        fimSemana.add(Calendar.DAY_OF_YEAR, 6)
        binding.textViewIntervaloSemana.text =
            "${shortDateFormat.format(inicioSemana.time)} - ${shortDateFormat.format(fimSemana.time)}"
    }

    private fun carregarDadosDaSemana() {
        atualizarIntervaloSemanaTextView()

        viewLifecycleOwner.lifecycleScope.launch {
            val diasDaSemanaComEventos = mutableListOf<DiaDaSemanaComEventos>()

            val deferreds = (0..6).map { i ->
                val dataParaEsteDia = calendarAtualPrimeiroDiaDaSemana.clone() as Calendar
                dataParaEsteDia.add(Calendar.DAY_OF_YEAR, i)

                async(Dispatchers.IO) {
                    val diaSemanaQuery = dataParaEsteDia.get(Calendar.DAY_OF_WEEK)
                    val dataFormatadaQuery = queryDateFormat.format(dataParaEsteDia.time)

                    // Usar first() para obter a lista atual dos Flows
                    val aulasDoDia = horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaQuery).first()
                    val eventosDoDia = eventoDao.buscarEventosParaData(diaSemanaQuery, dataFormatadaQuery).first() // Query correta

                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    eventosDoDia.forEach { evento -> itensCombinados.add(ItemAgendaDashboard.EventoItem(evento)) } // EventoItem espera Evento
                    itensCombinados.sortBy { it.horaInicio }

                    val eventosCompactos = itensCombinados.mapNotNull { item ->
                        val tipoItem = if (item.idUnico.startsWith("aula_")) "aula" else if (item.idUnico.startsWith("evento_")) "evento" else null
                        val idOriginal = item.idUnico.substringAfter("_").toIntOrNull()

                        if (tipoItem != null && idOriginal != null) {
                            EventoSemanaCompacto(
                                idOriginal = idOriginal,
                                tipo = tipoItem,
                                idUnico = item.idUnico,
                                horario = item.horaInicio,
                                nome = item.nomePrincipal,
                                cor = item.cor
                            )
                        } else { null }
                    }

                    DiaDaSemanaComEventos(
                        data = dataParaEsteDia,
                        nomeDiaAbrev = SimpleDateFormat("EEE", localePtBr).format(dataParaEsteDia.time).uppercase(localePtBr),
                        dataFormatadaCurta = shortDateFormat.format(dataParaEsteDia.time),
                        eventos = eventosCompactos
                    )
                }
            }

            diasDaSemanaComEventos.addAll(deferreds.awaitAll())

            withContext(Dispatchers.Main) {
                 semanaDiasAdapter.submitList(diasDaSemanaComEventos)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
