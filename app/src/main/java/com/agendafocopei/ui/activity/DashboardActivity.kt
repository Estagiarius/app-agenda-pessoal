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
import com.agendafocopei.data.Evento
import com.agendafocopei.data.EventoDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.data.TarefaDao
import com.agendafocopei.databinding.ActivityDashboardBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.adapter.TarefaAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment
import com.agendafocopei.ui.model.HorarioAulaDisplay
import com.agendafocopei.ui.model.ItemAgendaDashboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao
    private lateinit var tarefaDao: TarefaDao
    private lateinit var agendaHojeAdapter: AgendaHojeAdapter
    private lateinit var tarefaUrgenteAdapter: TarefaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        horarioAulaDao = database.horarioAulaDao()
        eventoDao = database.eventoDao()
        tarefaDao = database.tarefaDao()

        setSupportActionBar(binding.toolbarDashboard)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val calendario = Calendar.getInstance()
        val formatoDataDisplay = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        var dataFormatada = formatoDataDisplay.format(calendario.time)
        dataFormatada = dataFormatada.split(" ").joinToString(" ") { palavra ->
            palavra.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        }
        binding.textViewDataAtualDashboard.text = "Hoje, ${dataFormatada.substringAfter(", ")}"

        setupRecyclerViews()

        binding.buttonNovaTarefaDashboard.setOnClickListener {
            val intent = Intent(this, TarefasActivity::class.java)
            intent.putExtra(TarefasActivity.EXTRA_ABRIR_FORMULARIO_NOVA_TAREFA, true)
            startActivity(intent)
        }
        binding.buttonAnotacaoRapidaDashboard.setOnClickListener {
            // Atualizado para abrir AnotacoesActivity com o extra
            val intent = Intent(this, AnotacoesActivity::class.java)
            intent.putExtra(AnotacoesActivity.EXTRA_ABRIR_FORMULARIO_NOVA_ANOTACAO, true)
            startActivity(intent)
        }
        binding.buttonFocoDashboard.setOnClickListener {
            val intent = Intent(this, PomodoroActivity::class.java)
            startActivity(intent)
        }

        binding.buttonVerAgendaCompleta.setOnClickListener {
            val intent = Intent(this, AgendaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerViews() {
        tarefaUrgenteAdapter = TarefaAdapter(
            onTarefaCheckChanged = { tarefaDisplay, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val tarefaAtualizada = tarefaDisplay.tarefa.copy(
                        concluida = isChecked,
                        dataConclusao = if (isChecked) System.currentTimeMillis() else null
                    )
                    tarefaDao.atualizar(tarefaAtualizada)
                }
            },
            onEditClickListener = { tarefaDisplay ->
                val intent = Intent(this, TarefasActivity::class.java)
                Toast.makeText(this, "Editar Tarefa ID: ${tarefaDisplay.tarefa.id} (abrirá lista de tarefas)", Toast.LENGTH_SHORT).show()
                startActivity(intent)
            }
        )
        binding.recyclerViewTarefasUrgentesDashboard.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTarefasUrgentesDashboard.adapter = tarefaUrgenteAdapter

        agendaHojeAdapter = AgendaHojeAdapter { item ->
            val (id, type) = when (item) {
                is ItemAgendaDashboard.AulaItem -> Pair(item.aula.id, "aula")
                is ItemAgendaDashboard.EventoItem -> Pair(item.evento.id, "evento")
            }
            DetalhesEventoSheetFragment.newInstance(id, type)
                .show(supportFragmentManager, DetalhesEventoSheetFragment.TAG)
        }
        binding.recyclerViewAgendaHojeDashboard.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAgendaHojeDashboard.adapter = agendaHojeAdapter
    }

    override fun onResume() {
        super.onResume()
        carregarProximoCompromisso()
        carregarAgendaDoDia()
        carregarTarefasUrgentes()
    }

    private fun carregarProximoCompromisso() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val diaSemanaAtual = calendar.get(Calendar.DAY_OF_WEEK)
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val horaAtualStr = formatoHora.format(calendar.time)
            val dataAtualStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val proximaAula = horarioAulaDao.buscarProximoHorarioAulaDoDia(diaSemanaAtual, horaAtualStr)
            val proximoEvento = eventoDao.buscarProximoEventoParaData(diaSemanaAtual, dataAtualStr, horaAtualStr)
            atualizarUIProximoCompromisso(proximaAula, proximoEvento)
        }
    }

    private fun atualizarUIProximoCompromisso(proximaAula: HorarioAulaDisplay?, proximoEvento: Evento?) {
        val cardProximoCompromisso = binding.cardViewProximoCompromisso
        val nenhumCompromisso = proximaAula == null && proximoEvento == null

        binding.textViewNenhumCompromissoHoje.visibility = if (nenhumCompromisso) View.VISIBLE else View.GONE
        binding.textViewHorarioProximoCompromisso.visibility = if (nenhumCompromisso) View.GONE else View.VISIBLE
        binding.textViewNomeProximoCompromisso.visibility = if (nenhumCompromisso) View.GONE else View.VISIBLE
        binding.textViewSalaLocalProximoCompromisso.visibility = if (nenhumCompromisso) View.GONE else View.VISIBLE
        binding.textViewTurmaProximoCompromisso.visibility = View.GONE

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
        } else { return }

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
            val dataAtualStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaAtual)
                .combine(eventoDao.buscarEventosParaData(diaSemanaAtual, dataAtualStr)) { aulasDoDia, eventosDoDia ->
                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    eventosDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.EventoItem(it)) }
                    itensCombinados.sortBy { item -> item.horaInicio }
                    itensCombinados
                }.collect { itensAgrupadosEOrdenados ->
                    agendaHojeAdapter.submitList(itensAgrupadosEOrdenados)
                    binding.textViewPlaceholderAgenda.visibility = if (itensAgrupadosEOrdenados.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewAgendaHojeDashboard.visibility = if (itensAgrupadosEOrdenados.isEmpty()) View.GONE else View.VISIBLE
                }
        }
    }

    private fun carregarTarefasUrgentes() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val formatoQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val hojeStr = formatoQuery.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val amanhaStr = formatoQuery.format(calendar.time)

            tarefaDao.buscarUrgentesParaDisplay(hojeStr, amanhaStr).collect { listaTarefasUrgentes ->
                tarefaUrgenteAdapter.submitList(listaTarefasUrgentes)
                if (listaTarefasUrgentes.isEmpty()) {
                    binding.textViewPlaceholderTarefas.text = "Nenhuma tarefa urgente para hoje ou amanhã."
                    binding.textViewPlaceholderTarefas.visibility = View.VISIBLE
                    binding.recyclerViewTarefasUrgentesDashboard.visibility = View.GONE
                } else {
                    binding.textViewPlaceholderTarefas.visibility = View.GONE
                    binding.recyclerViewTarefasUrgentesDashboard.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
