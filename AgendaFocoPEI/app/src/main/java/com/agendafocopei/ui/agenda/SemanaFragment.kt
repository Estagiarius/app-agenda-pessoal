package com.agendafocopei.ui.agenda

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
import com.agendafocopei.databinding.FragmentSemanaBinding
import com.agendafocopei.ui.adapter.EventosDiaSemanaAdapter // Para o adapter aninhado
import com.agendafocopei.ui.adapter.SemanaDiasAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment // Import do BottomSheet
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
import java.util.concurrent.TimeUnit

class SemanaFragment : Fragment() {

    private var _binding: FragmentSemanaBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao
    private lateinit var semanaDiasAdapter: SemanaDiasAdapter
    private var calendarAtualPrimeiroDiaDaSemana: Calendar = Calendar.getInstance()

    private val localePtBr = Locale("pt", "BR")
    private val shortDateFormat = SimpleDateFormat("dd/MM", localePtBr)
    private val fullDateFormat = SimpleDateFormat("dd MMM yyyy", localePtBr)


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
        eventoRecorrenteDao = appDatabase.eventoRecorrenteDao()

        setupRecyclerView()
        setupNavigationButtons()

        // Define o primeiro dia da semana como Segunda-feira
        calendarAtualPrimeiroDiaDaSemana.firstDayOfWeek = Calendar.MONDAY
        // Leva para a segunda-feira da semana atual
        calendarAtualPrimeiroDiaDaSemana.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        carregarDadosDaSemana()
    }

    private fun setupRecyclerView() {
        // O SemanaDiasAdapter agora precisa de um callback para passar para o EventosDiaSemanaAdapter
        semanaDiasAdapter = SemanaDiasAdapter { eventoCompacto ->
            // eventoCompacto já tem idOriginal e tipo
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
        binding.textViewIntervaloSemana.text = "${shortDateFormat.format(inicioSemana.time)} - ${shortDateFormat.format(fimSemana.time)}"
    }

    private fun carregarDadosDaSemana() {
        atualizarIntervaloSemanaTextView()

        viewLifecycleOwner.lifecycleScope.launch {
            val diasDaSemanaComEventos = mutableListOf<DiaDaSemanaComEventos>()
            val diaAtualIteracao = calendarAtualPrimeiroDiaDaSemana.clone() as Calendar

            val deferreds = (0..6).map { i ->
                val dataParaEsteDia = diaAtualIteracao.clone() as Calendar
                dataParaEsteDia.add(Calendar.DAY_OF_YEAR, i)

                async(Dispatchers.IO) { // async para buscar dados de cada dia em paralelo (opcional)
                    val diaSemanaQuery = dataParaEsteDia.get(Calendar.DAY_OF_WEEK)

                    val aulasDoDia = horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaQuery).first()
                    val eventosDoDia = eventoRecorrenteDao.buscarPorDia(diaSemanaQuery).first()

                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    eventosDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.EventoItem(it)) }
                    itensCombinados.sortBy { it.horaInicio }

                    val eventosCompactos = itensCombinados.mapNotNull { item ->
                        // Certifique-se de que o idUnico realmente contém o prefixo esperado
                        val tipoItem = if (item.idUnico.startsWith("aula_")) "aula" else if (item.idUnico.startsWith("evento_")) "evento" else null
                        val idOriginal = item.idUnico.substringAfter("_").toIntOrNull()

                        if (tipoItem != null && idOriginal != null) {
                            EventoSemanaCompacto(
                                idOriginal = idOriginal,
                                tipo = tipoItem,
                                idUnico = item.idUnico, // Mantém o idUnico para DiffUtil do EventosDiaSemanaAdapter
                                horario = item.horaInicio,
                                nome = item.nomePrincipal,
                                cor = item.cor
                            )
                        } else {
                            null // Ignora itens com idUnico malformado
                        }
                    }

                    DiaDaSemanaComEventos(
                        data = dataParaEsteDia,
                        nomeDiaAbrev = SimpleDateFormat("EEE", localePtBr).format(dataParaEsteDia.time).uppercase(localePtBr),
                        dataFormatadaCurta = shortDateFormat.format(dataParaEsteDia.time),
                        eventos = eventosCompactos
                    )
                }
            }

            // Espera todas as buscas assíncronas e coleta os resultados
            diasDaSemanaComEventos.addAll(deferreds.awaitAll())

            withContext(Dispatchers.Main) { // Atualiza a UI na thread principal
                 semanaDiasAdapter.submitList(diasDaSemanaComEventos)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
