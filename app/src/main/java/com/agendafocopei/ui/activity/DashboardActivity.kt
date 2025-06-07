package com.agendafocopei.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.R
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Evento // ATUALIZADO
import com.agendafocopei.data.EventoDao // ATUALIZADO
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.ActivityDashboardBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.model.HorarioAulaDisplay
import com.agendafocopei.ui.model.ItemAgendaDashboard
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao // ATUALIZADO
    private lateinit var agendaHojeAdapter: AgendaHojeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização dos DAOs
        val database = AppDatabase.getDatabase(applicationContext)
        horarioAulaDao = database.horarioAulaDao()
        eventoDao = database.eventoDao() // ATUALIZADO

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbarDashboard)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Formatar e exibir a data atual
        val calendario = Calendar.getInstance()
        val formatoData = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        var dataFormatada = formatoData.format(calendario.time)
        dataFormatada = dataFormatada.split(" ").joinToString(" ") { palavra ->
            palavra.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        }
        binding.textViewDataAtualDashboard.text = "Hoje, ${dataFormatada.substringAfter(", ")}"

        // Configurar RecyclerViews
        binding.recyclerViewTarefasUrgentesDashboard.layoutManager = LinearLayoutManager(this)
        // binding.recyclerViewTarefasUrgentesDashboard.adapter = null // Adapter de tarefas virá depois

        agendaHojeAdapter = AgendaHojeAdapter()
        binding.recyclerViewAgendaHojeDashboard.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAgendaHojeDashboard.adapter = agendaHojeAdapter

        // Configurar OnClickListeners para botões de Ações Rápidas
        binding.buttonNovaTarefaDashboard.setOnClickListener {
            Toast.makeText(this, "Funcionalidade 'Nova Tarefa' em breve!", Toast.LENGTH_SHORT).show()
        }
        binding.buttonAnotacaoRapidaDashboard.setOnClickListener {
            Toast.makeText(this, "Funcionalidade 'Anotação Rápida' em breve!", Toast.LENGTH_SHORT).show()
        }
        binding.buttonFocoDashboard.setOnClickListener {
            Toast.makeText(this, "Funcionalidade 'Foco (Pomodoro)' em breve!", Toast.LENGTH_SHORT).show()
        }

        // Botão para ver Agenda Completa
        binding.buttonVerAgendaCompleta.setOnClickListener {
            val intent = Intent(this, AgendaActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        carregarProximoCompromisso()
        carregarAgendaDoDia()
    }

    private fun carregarProximoCompromisso() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val diaSemanaAtual = calendar.get(Calendar.DAY_OF_WEEK)
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val horaAtualStr = formatoHora.format(calendar.time)
            val dataAtualStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time) // Data para nova query

            val proximaAula = horarioAulaDao.buscarProximoHorarioAulaDoDia(diaSemanaAtual, horaAtualStr)
            val proximoEvento = eventoDao.buscarProximoEventoParaData(diaSemanaAtual, dataAtualStr, horaAtualStr) // Query atualizada

            atualizarUIProximoCompromisso(proximaAula, proximoEvento)
        }
    }

    private fun atualizarUIProximoCompromisso(proximaAula: HorarioAulaDisplay?, proximoEvento: Evento?) { // Tipo atualizado
        val cardProximoCompromisso = binding.cardViewProximoCompromisso

        val nenhumCompromisso = proximaAula == null && proximoEvento == null
        binding.textViewNenhumCompromissoHoje.visibility = if (nenhumCompromisso) View.VISIBLE else View.GONE

        binding.textViewHorarioProximoCompromisso.visibility = if (nenhumCompromisso) View.GONE else View.VISIBLE
        binding.textViewNomeProximoCompromisso.visibility = if (nenhumCompromisso) View.GONE else View.VISIBLE
        binding.textViewSalaLocalProximoCompromisso.visibility = if (nenhumCompromisso) View.GONE else View.VISIBLE
        binding.textViewTurmaProximoCompromisso.visibility = View.GONE // Resetar antes de decidir

        if (nenhumCompromisso) {
            cardProximoCompromisso.setCardBackgroundColor(Color.TRANSPARENT)
            return
        }

        var compromissoNome: String
        var compromissoHorario: String
        var compromissoSalaLocal: String?
        var compromissoCor: Int? = null
        var nomeTurma: String? = null

        val proximoEhAula = when {
            proximaAula != null && proximoEvento == null -> true
            proximaAula == null && proximoEvento != null -> false
            proximaAula != null && proximoEvento != null -> proximaAula.horaInicio <= proximoEvento.horaInicio
            else -> false
        }

        if (proximoEhAula && proximaAula != null) {
            compromissoNome = proximaAula.nomeDisciplina
            compromissoHorario = "${proximaAula.horaInicio} - ${proximaAula.horaFim}"
            compromissoSalaLocal = proximaAula.salaAula
            compromissoCor = proximaAula.corDisciplina
            nomeTurma = proximaAula.nomeTurma
        } else if (proximoEvento != null) {
            compromissoNome = proximoEvento.nomeEvento
            compromissoHorario = "${proximoEvento.horaInicio} - ${proximoEvento.horaFim}"
            compromissoSalaLocal = proximoEvento.salaLocal
            compromissoCor = proximoEvento.cor
        } else {
            // Should not happen if !nenhumCompromisso
            return
        }

        binding.textViewNomeProximoCompromisso.text = compromissoNome
        binding.textViewHorarioProximoCompromisso.text = compromissoHorario

        if (compromissoSalaLocal.isNullOrEmpty()) {
            binding.textViewSalaLocalProximoCompromisso.visibility = View.GONE
        } else {
            binding.textViewSalaLocalProximoCompromisso.visibility = View.VISIBLE
            binding.textViewSalaLocalProximoCompromisso.text = compromissoSalaLocal
        }

        if (nomeTurma != null) {
            binding.textViewTurmaProximoCompromisso.text = nomeTurma
            binding.textViewTurmaProximoCompromisso.visibility = View.VISIBLE
        }

        cardProximoCompromisso.setCardBackgroundColor(compromissoCor ?: Color.parseColor("#E0E0E0"))
    }

    private fun carregarAgendaDoDia() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val diaSemanaAtual = calendar.get(Calendar.DAY_OF_WEEK)
            val dataAtualStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time) // Data para nova query

            horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaAtual)
                .combine(eventoDao.buscarEventosParaData(diaSemanaAtual, dataAtualStr)) { aulasDoDia, eventosDoDia -> // Query atualizada
                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    eventosDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.EventoItem(it)) } // EventoItem agora recebe Evento
                    itensCombinados.sortBy { item -> item.horaInicio }
                    itensCombinados
                }.collect { itensAgrupadosEOrdenados ->
                    agendaHojeAdapter.submitList(itensAgrupadosEOrdenados)
                    binding.textViewPlaceholderAgenda.visibility = if (itensAgrupadosEOrdenados.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewAgendaHojeDashboard.visibility = if (itensAgrupadosEOrdenados.isEmpty()) View.GONE else View.VISIBLE
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
