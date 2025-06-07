package com.agendafocopei.ui.agenda

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.R
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.EventoDao // Usar EventoDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.CalendarDayLayoutBinding
import com.agendafocopei.databinding.FragmentMesBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment
import com.agendafocopei.ui.model.ItemAgendaDashboard
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek as JavaTimeDayOfWeek // Alias para evitar conflito com Calendar.DAY_OF_WEEK
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class MesFragment : Fragment() {

    private var _binding: FragmentMesBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao // Usar EventoDao
    private lateinit var agendaAdapter: AgendaHojeAdapter

    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val eventsMap = mutableMapOf<LocalDate, List<ItemAgendaDashboard>>()

    private val localePtBr = Locale("pt", "BR")
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", localePtBr)
    private val queryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", localePtBr)
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", localePtBr)


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
        eventoDao = appDatabase.eventoDao() // Usar eventoDao()

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

        val currentMonth = YearMonth.now()
        binding.calendarViewMes.scrollToMonth(currentMonth)
        // loadEventsForVisibleMonth é chamado pelo monthScrollListener na configuração inicial
    }

    private fun setupCalendarView() {
        val calendarView = binding.calendarViewMes

        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = CalendarDayLayoutBinding.bind(view).calendarDayText
            val indicatorView: View = CalendarDayLayoutBinding.bind(view).eventIndicatorView
            lateinit var day: CalendarDay
            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
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
                    container.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    container.view.isClickable = true
                    container.textView.background = null // Reset background

                    if (data.date == selectedDate) {
                        container.textView.setBackgroundResource(R.drawable.oval_indicator_background)
                        container.textView.setTextColor(Color.WHITE)
                    } else if (data.date == today) {
                         container.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                    }
                    container.indicatorView.visibility = if (eventsMap[data.date].orEmpty().isNotEmpty()) View.VISIBLE else View.INVISIBLE
                } else {
                    container.textView.setTextColor(Color.LTGRAY)
                    container.indicatorView.visibility = View.INVISIBLE
                    container.view.isClickable = false
                }
            }
        }

        calendarView.monthScrollListener = { calendarMonth ->
            binding.textViewMesAnoCalendar.text = calendarMonth.yearMonth.format(monthTitleFormatter)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localePtBr) else it.toString() }
            loadEventsForVisibleMonth(calendarMonth.yearMonth)
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val daysOfWeek = daysOfWeek(firstDayOfWeek = JavaTimeDayOfWeek.MONDAY) // Use o alias
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
            val firstDayOfMonth = yearMonth.atDay(1)
            val lastDayOfMonth = yearMonth.atEndOfMonth()
            val tempEventsMap = mutableMapOf<LocalDate, MutableList<ItemAgendaDashboard>>()

            // Itera por cada dia do mês para carregar eventos
            var currentDateIterator = firstDayOfMonth
            while (currentDateIterator <= lastDayOfMonth) {
                val cal = Calendar.getInstance().apply {
                    clear()
                    set(currentDateIterator.year, currentDateIterator.monthValue - 1, currentDateIterator.dayOfMonth)
                }
                val diaSemanaQuery = cal.get(Calendar.DAY_OF_WEEK)
                val dataFormatadaQuery = queryDateFormatter.format(currentDateIterator)

                // Coleta os flows para o dia atual
                val aulasDoDia = withContext(Dispatchers.IO) { horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaQuery).first() }
                val eventosDoDia = withContext(Dispatchers.IO) { eventoDao.buscarEventosParaData(diaSemanaQuery, dataFormatadaQuery).first() }

                val itensDoDia = mutableListOf<ItemAgendaDashboard>()
                aulasDoDia.forEach { aula ->
                    // Adicionar apenas se a aula for recorrente ou na data específica
                    // (buscarTodosParaDisplayPorDia já considera apenas dia da semana, então está ok para aulas recorrentes)
                     itensDoDia.add(ItemAgendaDashboard.AulaItem(aula))
                }
                eventosDoDia.forEach { evento -> // Evento já considera data específica ou dia da semana
                    itensDoDia.add(ItemAgendaDashboard.EventoItem(evento))
                }

                if (itensDoDia.isNotEmpty()) {
                    itensDoDia.sortBy { it.horaInicio }
                    tempEventsMap[currentDateIterator] = itensDoDia
                }
                currentDateIterator = currentDateIterator.plusDays(1)
            }

            eventsMap.clear()
            eventsMap.putAll(tempEventsMap)
            binding.calendarViewMes.notifyCalendarChanged()
            updateEventsListForSelectedDate() // Atualiza a lista para a data selecionada, se houver
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
            agendaAdapter.submitList(eventosDoDia.sortedBy { it.horaInicio }) // Garante ordenação final

            var dataFormatada = date.format(displayDateFormatter)
            dataFormatada = dataFormatada.split(" ").map {palavra ->
                 palavra.replaceFirstChar { if(it.isLowerCase()) it.titlecase(localePtBr) else it.toString() }
            }.joinToString(" ")
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
