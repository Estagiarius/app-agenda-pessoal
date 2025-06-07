package com.agendafocopei.ui.activity

import android.os.Bundle
import android.widget.Toast
import android.graphics.Color // Para cor padrão
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Para corrotinas
import androidx.recyclerview.widget.LinearLayoutManager
import com.agendafocopei.data.AppDatabase // Para DAOs
import com.agendafocopei.data.EventoRecorrente // Para tipo de dado
import com.agendafocopei.data.EventoRecorrenteDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.ActivityDashboardBinding
import com.agendafocopei.ui.adapter.AgendaHojeAdapter
import com.agendafocopei.ui.bottomsheet.DetalhesEventoSheetFragment // Import do BottomSheet
import com.agendafocopei.ui.model.HorarioAulaDisplay
import com.agendafocopei.ui.model.ItemAgendaDashboard
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoRecorrenteDao: EventoRecorrenteDao
    private lateinit var agendaHojeAdapter: AgendaHojeAdapter // Adapter para agenda do dia

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbarDashboard)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // O título já está definido no XML como "Dashboard"

        // Formatar e exibir a data atual
        val calendario = Calendar.getInstance()
        // Exemplo: "Hoje, 10 de Junho"
        val formatoData = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        var dataFormatada = formatoData.format(calendario.time)
        // Capitalizar o dia da semana e o mês
        dataFormatada = dataFormatada.split(" ").joinToString(" ") { palavra ->
            palavra.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        }
        binding.textViewDataAtualDashboard.text = "Hoje, ${dataFormatada.substringAfter(", ")}"


        // Configurar RecyclerViews (sem adapters reais por enquanto)
        binding.recyclerViewTarefasUrgentesDashboard.layoutManager = LinearLayoutManager(this)
        // binding.recyclerViewTarefasUrgentesDashboard.adapter = null

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


        // Configurar OnClickListeners para botões de Ações Rápidas
        binding.buttonNovaTarefaDashboard.setOnClickListener {
            Toast.makeText(this, "Funcionalidade 'Nova Tarefa' em breve!", Toast.LENGTH_SHORT).show()
            // Log.d("DashboardActivity", "Botão Nova Tarefa clicado - funcionalidade futura.")
        }
        binding.buttonAnotacaoRapidaDashboard.setOnClickListener {
            Toast.makeText(this, "Funcionalidade 'Anotação Rápida' em breve!", Toast.LENGTH_SHORT).show()
            // Log.d("DashboardActivity", "Botão Anotação Rápida clicado - funcionalidade futura.")
        }
        binding.buttonFocoDashboard.setOnClickListener {
            Toast.makeText(this, "Funcionalidade 'Foco (Pomodoro)' em breve!", Toast.LENGTH_SHORT).show()
            // Log.d("DashboardActivity", "Botão Foco clicado - funcionalidade futura.")
        }

        // Lógica para carregar dados ...
        // updateProximoCompromissoPlaceholder() // Será substituído por carregarProximoCompromisso
    }

    override fun onResume() {
        super.onResume()
        carregarProximoCompromisso()
        carregarAgendaDoDia() // Carregar agenda do dia
    }

    private fun carregarProximoCompromisso() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val diaSemanaAtual = calendar.get(Calendar.DAY_OF_WEEK)
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val horaAtualStr = formatoHora.format(calendar.time)

            val proximaAula = horarioAulaDao.buscarProximoHorarioAulaDoDia(diaSemanaAtual, horaAtualStr)
            val proximoEvento = eventoRecorrenteDao.buscarProximoEventoDoDia(diaSemanaAtual, horaAtualStr)

            atualizarUIProximoCompromisso(proximaAula, proximoEvento)
        }
    }

    private fun atualizarUIProximoCompromisso(proximaAula: HorarioAulaDisplay?, proximoEvento: EventoRecorrente?) {
        val cardProximoCompromisso = binding.cardViewProximoCompromisso // Usando ViewBinding

        val nenhumCompromisso = proximaAula == null && proximoEvento == null
        binding.textViewNenhumCompromissoHoje.visibility = if (nenhumCompromisso) View.VISIBLE else View.GONE

        if (nenhumCompromisso) {
            binding.textViewHorarioProximoCompromisso.visibility = View.GONE
            binding.textViewNomeProximoCompromisso.visibility = View.GONE
            binding.textViewTurmaProximoCompromisso.visibility = View.GONE
            binding.textViewSalaLocalProximoCompromisso.visibility = View.GONE
            cardProximoCompromisso.setCardBackgroundColor(Color.TRANSPARENT) // Ou cor padrão
            return
        }

        binding.textViewHorarioProximoCompromisso.visibility = View.VISIBLE
        binding.textViewNomeProximoCompromisso.visibility = View.VISIBLE
        binding.textViewSalaLocalProximoCompromisso.visibility = View.VISIBLE

        var compromissoNome: String
        var compromissoHorario: String
        var compromissoSalaLocal: String?
        var compromissoCor: Int? = null
        var nomeTurma: String? = null

        // Decide qual é o próximo
        val proximoEhAula = when {
            proximaAula != null && proximoEvento == null -> true
            proximaAula == null && proximoEvento != null -> false
            proximaAula != null && proximoEvento != null -> {
                // Ambos existem, compara horaInicio
                proximaAula.horaInicio <= proximoEvento.horaInicio
            }
            else -> false // Nunca deve acontecer se nenhumCompromisso é false
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
            nomeTurma = null // Eventos não têm turma diretamente associada desta forma
        } else {
             // Fallback, embora não devesse ser atingido se nenhumCompromisso é false
            binding.textViewNenhumCompromissoHoje.visibility = View.VISIBLE
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
        } else {
            binding.textViewTurmaProximoCompromisso.visibility = View.GONE
        }

        cardProximoCompromisso.setCardBackgroundColor(compromissoCor ?: Color.parseColor("#E0E0E0"))
    }

    private fun carregarAgendaDoDia() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val diaSemanaAtual = calendar.get(Calendar.DAY_OF_WEEK)

            // Coletar os dois flows e combiná-los
            horarioAulaDao.buscarTodosParaDisplayPorDia(diaSemanaAtual)
                .combine(eventoRecorrenteDao.buscarPorDia(diaSemanaAtual)) { aulasDoDia, eventosDoDia ->
                    val itensCombinados = mutableListOf<ItemAgendaDashboard>()
                    aulasDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.AulaItem(it)) }
                    eventosDoDia.forEach { itensCombinados.add(ItemAgendaDashboard.EventoItem(it)) }

                    // Ordenar pela hora de início
                    itensCombinados.sortBy { item -> item.horaInicio }
                    itensCombinados
                }.collect { itensAgrupadosEOrdenados ->
                    agendaHojeAdapter.submitList(itensAgrupadosEOrdenados)
                    // Lidar com visibilidade da mensagem de lista vazia
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
