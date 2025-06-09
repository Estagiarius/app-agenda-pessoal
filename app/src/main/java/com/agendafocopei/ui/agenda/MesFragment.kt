package com.agendafocopei.ui.agenda

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
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
import com.agendafocopei.data.EventoDao
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
import java.time.DayOfWeek as JavaTimeDayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class MesFragment : Fragment() {

    private var _binding: FragmentMesBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao
    private lateinit var agendaAdapter: AgendaHojeAdapter

    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val eventsMap = mutableMapOf<LocalDate, List<ItemAgendaDashboard>>()

    private val localePtBr = Locale("pt", "BR")
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", localePtBr)
    private val queryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()) // Padrão ISO para DB
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
        eventoDao = appDatabase.eventoDao()

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

    // Função para obter cor de atributo do tema
    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
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

        val colorOnPrimary = getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val colorPrimary = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val textColorPrimary = getThemeColor(android.R.attr.textColorPrimary)
        val textColorTertiary = getThemeColor(android.R.attr.textColorTertiary)


        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.textView.text = data.date.dayOfMonth.toString()
                container.textView.background = null // Reset background
                container.view.isClickable = false


                if (data.position == DayPosition.MonthDate) {
                    container.view.isClickable = true
                    when {
                        data.date == selectedDate -> {
                            container.textView.setTextColor(colorOnPrimary)
                            container.textView.setBackgroundResource(R.drawable.oval_indicator_background) // Usa ?attr/colorPrimary
                        }
                        data.date == today -> {
                            container.textView.setTextColor(colorPrimary)
                        }
                        else -> {
                            container.textView.setTextColor(textColorPrimary)
                        }
                    }
                    container.indicatorView.visibility = if (eventsMap[data.date].orEmpty().isNotEmpty()) View.VISIBLE else View.INVISIBLE
                } else {
                    container.textView.setTextColor(textColorTertiary) // Cor para dias fora do mês
                    container.indicatorView.visibility = View.INVISIBLE
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
        val daysOfWeek = daysOfWeek(firstDayOfWeek = JavaTimeDayOfWeek.MONDAY)
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

            var currentDateIterator = firstDayOfMonth
            while (currentDateIterator <= lastDayOfMonth) {
                val cal = Calendar.getInstance().apply {
                    clear()
                    set(currentDateIterator.year, currentDateIterator.monthValue - 1, currentDateIterator.dayOfMonth)
                }
                val diaSemanaQuery = cal.get(Calendar.DAY_OF_WEEK)
                val dataFormatadaQuery = queryDateFormatter.format(currentDateIterator)

                val aulasDoDia = withContext(Dispatchers.IO) { horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaQuery).first() }
                val eventosDoDia = withContext(Dispatchers.IO) { eventoDao.buscarEventosParaData(diaSemanaQuery, dataFormatadaQuery).first() }

                val itensDoDia = mutableListOf<ItemAgendaDashboard>()
                // Para aulas, verificamos se a data da aula (se existir) corresponde ao dia atual do loop
                // ou se é uma aula recorrente para o dia da semana.
                aulasDoDia.forEach { aulaDisplay ->
                     // HorarioAulaDisplay não tem dataAula diretamente, o HorarioAula original sim.
                     // A query buscarTodosParaDisplayPorDia já filtra pelo dia da semana.
                     // Se precisássemos filtrar aulas únicas por data específica aqui, precisaríamos da data no HorarioAulaDisplay.
                     // Por ora, todas as aulas retornadas para aquele dia da semana são adicionadas.
                    itensDoDia.add(ItemAgendaDashboard.AulaItem(aulaDisplay))
                }
                eventosDoDia.forEach { evento ->
                    // A query buscarEventosParaData já lida com dataEspecifica OU diaDaSemana.
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
            updateEventsListForSelectedDate()
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
            agendaAdapter.submitList(eventosDoDia.sortedBy { it.horaInicio })

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
