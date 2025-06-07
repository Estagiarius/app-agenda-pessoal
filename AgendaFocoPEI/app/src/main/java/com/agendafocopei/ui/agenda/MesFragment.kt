package com.agendafocopei.ui.agenda

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.R
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.EventoRecorrenteDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.CalendarDayLayoutBinding
import com.agendafocopei.databinding.FragmentMesBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment // Import do BottomSheet
import com.agendafocopei.ui.model.ItemAgendaDashboard
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class MesFragment : Fragment() {

    private var _binding: FragmentMesBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao
    private lateinit var agendaAdapter: AgendaHojeAdapter

    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val eventsMap = mutableMapOf<LocalDate, List<ItemAgendaDashboard>>()

    private val localePtBr = Locale("pt", "BR")
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", localePtBr)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appDatabase = AppDatabase.getDatabase(requireContext())
        horarioAulaDao = appDatabase.horarioAulaDao()
        eventoRecorrenteDao = appDatabase.eventoRecorrenteDao()

        agendaAdapter = AgendaHojeAdapter { item ->
            val (id, type) = when (item) {
                is ItemAgendaDashboard.AulaItem -> Pair(item.aula.id, "aula")
                is ItemAgendaDashboard.EventoItem -> Pair(item.evento.id, "evento")
            }
            DetalhesEventoSheetFragment.newInstance(id, type)
                .show(childFragmentManager, DetalhesEventoSheetFragment.TAG)
        }
        binding.recyclerViewEventosMes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = agendaAdapter
        }

        setupCalendarView()
        setupNavigationButtons()

        // Carrega eventos para o mês atual inicialmente
        val currentMonth = YearMonth.now()
        binding.calendarViewMes.scrollToMonth(currentMonth)
        loadEventsForVisibleMonth(currentMonth) // Carrega para o mês visível
        updateEventsListForSelectedDate() // Para o estado inicial (nenhuma data selecionada)
    }

    private fun setupCalendarView() {
        val calendarView = binding.calendarViewMes

        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = CalendarDayLayoutBinding.bind(view).calendarDayText
            val indicatorView: View = CalendarDayLayoutBinding.bind(view).eventIndicatorView
            lateinit var day: CalendarDay // Para o onClickListener
            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) { // Apenas dias do mês atual são selecionáveis
                        if (selectedDate != day.date) {
                            val oldDate = selectedDate
                            selectedDate = day.date
                            calendarView.notifyDateChanged(day.date)
                            oldDate?.let { calendarView.notifyDateChanged(it) }
                            updateEventsListForSelectedDate()
                        }
                    }
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    container.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)) // Cor padrão
                    container.view.isClickable = true

                    if (data.date == selectedDate) {
                        container.textView.setBackgroundResource(R.drawable.oval_indicator_background) // Exemplo de destaque
                        container.textView.setTextColor(Color.WHITE)
                    } else {
                        container.textView.background = null
                        if (data.date == today) {
                             container.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                        }
                    }
                    container.indicatorView.visibility = if (eventsMap[data.date].orEmpty().isNotEmpty()) View.VISIBLE else View.INVISIBLE
                } else { // Dias de outros meses
                    container.textView.setTextColor(Color.LTGRAY)
                    container.indicatorView.visibility = View.INVISIBLE
                    container.view.isClickable = false
                }
            }
        }

        calendarView.monthScrollListener = { calendarMonth ->
            val firstDay = calendarMonth.weekDays.first().first().date
            val lastDay = calendarMonth.weekDays.last().last().date
            binding.textViewMesAnoCalendar.text = calendarMonth.yearMonth.format(monthTitleFormatter).replaceFirstChar { it.titlecase(localePtBr) }
            loadEventsForVisibleMonth(calendarMonth.yearMonth) // Recarregar para o novo mês visível
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY)
        calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        calendarView.scrollToMonth(currentMonth)
    }

    private fun setupNavigationButtons() {
        binding.buttonMesAnteriorCalendar.setOnClickListener {
            binding.calendarViewMes.findFirstVisibleMonth()?.let {
                binding.calendarViewMes.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }
        binding.buttonMesProximoCalendar.setOnClickListener {
            binding.calendarViewMes.findFirstVisibleMonth()?.let {
                binding.calendarViewMes.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }
    }

    private fun loadEventsForVisibleMonth(yearMonth: YearMonth) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Esta é uma simplificação. Idealmente, você faria queries ao DAO que filtram por mês.
            // Como nossos DAOs atuais filtram por dia da semana, vamos buscar todos e filtrar no app.
            // Para uma app real, otimizar as queries do DAO para buscar por intervalo de datas seria melhor.

            val todosHorariosFlow = horarioAulaDao.buscarTodosParaDisplay() // Todos os horários (aulas)
            val todosEventosFlow = eventoRecorrenteDao.buscarTodosOrdenados() // Todos os eventos recorrentes

            todosHorariosFlow.combine(todosEventosFlow) { todasAulas, todosEventos ->
                val newEventsMap = mutableMapOf<LocalDate, MutableList<ItemAgendaDashboard>>()

                // Processa aulas (HorarioAulaDisplay)
                todasAulas.forEach { aula ->
                    // Adiciona a aula para cada ocorrência no mês visível
                    addRecurringItemToMap(yearMonth, aula.diaDaSemana, ItemAgendaDashboard.AulaItem(aula), newEventsMap)
                }
                // Processa eventos recorrentes
                todosEventos.forEach { evento ->
                     addRecurringItemToMap(yearMonth, evento.diaDaSemana, ItemAgendaDashboard.EventoItem(evento), newEventsMap)
                }

                // Ordena os eventos de cada dia
                newEventsMap.forEach { (_, itemList) -> itemList.sortBy { it.horaInicio } }
                newEventsMap
            }.collect { collectedMap ->
                eventsMap.clear()
                eventsMap.putAll(collectedMap.mapValues { it.value.toList() }) // Converte para List imutável
                binding.calendarViewMes.notifyCalendarChanged() // Re-binda todos os dias visíveis
                updateEventsListForSelectedDate() // Atualiza a lista se uma data estiver selecionada
            }
        }
    }

    private fun addRecurringItemToMap(
        targetMonth: YearMonth,
        diaDaSemanaNoEvento: Int, // 1=Domingo, ..., 7=Sábado (Calendar.DAY_OF_WEEK)
        item: ItemAgendaDashboard,
        map: MutableMap<LocalDate, MutableList<ItemAgendaDashboard>>
    ) {
        val firstOfMonth = targetMonth.atDay(1)
        val lastOfMonth = targetMonth.atEndOfMonth()

        var currentDate = firstOfMonth
        while (currentDate <= lastOfMonth) {
            val calendarDayOfWeek = currentDate.dayOfWeek.value % 7 + 1 // Converte DayOfWeek (MON=1..SUN=7) para Calendar (SUN=1..SAT=7)
            if (calendarDayOfWeek == diaDaSemanaNoEvento) {
                map.getOrPut(currentDate) { mutableListOf() }.add(item)
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    private fun updateEventsListForSelectedDate() {
        val date = selectedDate
        if (date == null) {
            binding.textViewEventosDoDiaSelecionadoMes.visibility = View.GONE
            binding.recyclerViewEventosMes.visibility = View.GONE
            binding.textViewNenhumEventoMes.visibility = View.VISIBLE
            binding.textViewNenhumEventoMes.text = "Selecione um dia para ver os eventos."
        } else {
            val eventosDoDia = eventsMap[date].orEmpty()
            agendaAdapter.submitList(eventosDoDia)

            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", localePtBr)
            val dataFormatada = date.format(dateFormatter).replaceFirstChar { it.titlecase(localePtBr) }
            binding.textViewEventosDoDiaSelecionadoMes.text = "Eventos para: $dataFormatada"
            binding.textViewEventosDoDiaSelecionadoMes.visibility = View.VISIBLE

            if (eventosDoDia.isEmpty()) {
                binding.recyclerViewEventosMes.visibility = View.GONE
                binding.textViewNenhumEventoMes.visibility = View.VISIBLE
                binding.textViewNenhumEventoMes.text = "Nenhum evento para este dia."
            } else {
                binding.recyclerViewEventosMes.visibility = View.VISIBLE
                binding.textViewNenhumEventoMes.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
