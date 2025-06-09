package com.agendafocopei.ui.bottomsheet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.Evento // Usar Evento
import com.agendafocopei.data.EventoDao // Usar EventoDao
import com.agendafocopei.data.HorarioAulaDao
import com.agendafocopei.databinding.BottomSheetDetalhesEventoBinding
import com.agendafocopei.ui.model.HorarioAulaDisplay
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class DetalhesEventoSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDetalhesEventoBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAulaDao: HorarioAulaDao
    private lateinit var eventoDao: EventoDao
    private lateinit var planoDeAulaDao: PlanoDeAulaDao // Novo DAO

    private var itemId: Int = -1 // ID da Aula ou Evento
    private lateinit var itemType: String
    private var aulaAtualDisplay: HorarioAulaDisplay? = null // Para armazenar dados da aula

    companion object {
        const val TAG = "DetalhesEventoSheet"
        private const val ARG_ITEM_ID = "item_id"
        private const val ARG_ITEM_TYPE = "item_type" // "aula" ou "evento"

        fun newInstance(itemId: Int, itemType: String): DetalhesEventoSheetFragment {
            val args = Bundle()
            args.putInt(ARG_ITEM_ID, itemId)
            args.putString(ARG_ITEM_TYPE, itemType)
            val fragment = DetalhesEventoSheetFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            itemId = it.getInt(ARG_ITEM_ID, -1)
            itemType = it.getString(ARG_ITEM_TYPE, "")
        }
        val database = AppDatabase.getDatabase(requireContext())
        horarioAulaDao = database.horarioAulaDao()
        eventoDao = database.eventoDao()
        planoDeAulaDao = database.planoDeAulaDao() // Inicializar novo DAO
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDetalhesEventoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (itemId == -1 || itemType.isEmpty()) {
            Toast.makeText(context, "Erro ao carregar detalhes.", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        loadDetails()
    }

    private fun getDiaDaSemanaFormatado(diaDaSemana: Int, dataEspecifica: String? = null): String {
        val localePtBr = Locale("pt", "BR")
        if (dataEspecifica != null) {
            try {
                val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdfInput.parse(dataEspecifica)
                date?.let {
                    // Formato: "Seg, 10/06/2024"
                    val sdfOutput = SimpleDateFormat("EEE, dd/MM/yyyy", localePtBr)
                    return sdfOutput.format(it).split(",").joinToString(", ") { part ->
                        part.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(localePtBr) else it.toString() }
                    }
                }
            } catch (e: Exception) { /* fallback */ }
        }
        val weekdays = DateFormatSymbols(localePtBr).weekdays
        return if (diaDaSemana in 1..7) weekdays[diaDaSemana] else "Recorrente"
    }


    private fun loadDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (itemType == "aula") {
                val aulaDisplay = withContext(Dispatchers.IO) { horarioAulaDao.buscarDisplayPorId(itemId) }
                aulaDisplay?.let { popularUiComAula(it) } ?:แจ้งเตือนและปิด()
            } else if (itemType == "evento") {
                val evento = withContext(Dispatchers.IO) { eventoDao.buscarPorId(itemId) } // Usar EventoDao
                evento?.let { popularUiComEvento(it) } ?:แจ้งเตือนและปิด()
            } else {
                แจ้งเตือนและปิด()
            }
        }
    }

    private fun แจ้งเตือนและปิด() {
        Toast.makeText(context, "Item não encontrado.", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun popularUiComAula(aula: HorarioAulaDisplay) {
        aulaAtualDisplay = aula // Armazena para uso no listener do botão de plano
        binding.viewCorDetalhesEvento.setBackgroundColor(aula.corDisciplina ?: Color.TRANSPARENT)
        binding.textViewNomeDetalhesEvento.text = aula.nomeDisciplina
        binding.textViewHorarioDetalhesEvento.text =
            "${aula.horaInicio} - ${aula.horaFim} (${getDiaDaSemanaFormatado(aula.diaDaSemana)})"
        binding.textViewTipoDetalhesEvento.text = "Tipo: Aula"

        binding.textViewTurmaDetalhesEvento.text = "Turma: ${aula.nomeTurma}"
        binding.textViewTurmaDetalhesEvento.visibility = View.VISIBLE

        if (aula.salaAula.isNullOrEmpty()) {
            binding.textViewSalaLocalDetalhesEvento.visibility = View.GONE
        } else {
            binding.textViewSalaLocalDetalhesEvento.text = "Sala: ${aula.salaAula}"
            binding.textViewSalaLocalDetalhesEvento.visibility = View.VISIBLE
        }

        binding.textViewObservacoesDetalhesEvento.visibility = View.GONE

        // Lógica do botão Plano de Aula
        binding.buttonPlanoDeAulaDetalhes.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val planoExistente = withContext(Dispatchers.IO) {
                planoDeAulaDao.buscarUmPorHorarioAulaId(aula.id) // aula.id é o HorarioAula.id
            }
            if (planoExistente != null) {
                binding.buttonPlanoDeAulaDetalhes.text = "Editar Plano de Aula"
                binding.buttonPlanoDeAulaDetalhes.setOnClickListener {
                    FormularioPlanoDeAulaFragment.newInstance(
                        planoId = planoExistente.id,
                        horarioAulaId = aula.id, // Passa o ID do HorarioAula
                        disciplinaIdPredefinida = aula.disciplinaId,
                        turmaIdPredefinida = aula.turmaId,
                        dataPredefinida = null // Data pode vir do plano ou do horário se necessário
                    ).also { it.setListener(parentFragment as? FormularioPlanoDeAulaFragment.FormularioPlanoListener ?: activity as? FormularioPlanoDeAulaFragment.FormularioPlanoListener) }
                     .show(parentFragmentManager, FormularioPlanoDeAulaFragment.TAG)
                    dismiss() // Fecha o bottom sheet
                }
            } else {
                binding.buttonPlanoDeAulaDetalhes.text = "Adicionar Plano de Aula"
                binding.buttonPlanoDeAulaDetalhes.setOnClickListener {
                    // Formatar data da aula se disponível (aula.diaDaSemana e horaInicio podem não ser suficientes para data exata)
                    // Idealmente, o HorarioAulaDisplay teria a data exata se fosse de um dia específico
                    // Por simplicidade, não passaremos data aqui, o usuário selecionará no formulário do plano.
                    FormularioPlanoDeAulaFragment.newInstance(
                        planoId = null,
                        horarioAulaId = aula.id,
                        disciplinaIdPredefinida = aula.disciplinaId,
                        turmaIdPredefinida = aula.turmaId,
                        dataPredefinida = null // Usuário define no formulário do plano
                    ).also { it.setListener(parentFragment as? FormularioPlanoDeAulaFragment.FormularioPlanoListener ?: activity as? FormularioPlanoDeAulaFragment.FormularioPlanoListener) }
                     .show(parentFragmentManager, FormularioPlanoDeAulaFragment.TAG)
                    dismiss() // Fecha o bottom sheet
                }
            }
        }
    }

    private fun popularUiComEvento(evento: Evento) {
        binding.viewCorDetalhesEvento.setBackgroundColor(evento.cor ?: Color.TRANSPARENT)
        binding.textViewNomeDetalhesEvento.text = evento.nomeEvento
        binding.textViewHorarioDetalhesEvento.text =
            "${evento.horaInicio} - ${evento.horaFim} (${getDiaDaSemanaFormatado(evento.diaDaSemana, evento.dataEspecifica)})"
        binding.textViewTipoDetalhesEvento.text = if(evento.dataEspecifica != null) "Tipo: Evento Único" else "Tipo: Evento Recorrente"

        binding.textViewTurmaDetalhesEvento.visibility = View.GONE

        if (evento.salaLocal.isNullOrEmpty()) {
            binding.textViewSalaLocalDetalhesEvento.visibility = View.GONE
        } else {
            binding.textViewSalaLocalDetalhesEvento.text = "Local: ${evento.salaLocal}"
            binding.textViewSalaLocalDetalhesEvento.visibility = View.VISIBLE
        }

        if (evento.observacoes.isNullOrEmpty()) {
            binding.textViewObservacoesDetalhesEvento.visibility = View.GONE
        } else {
            binding.textViewObservacoesDetalhesEvento.text = "Obs: ${evento.observacoes}"
            binding.textViewObservacoesDetalhesEvento.visibility = View.VISIBLE
        }
        binding.buttonPlanoDeAulaDetalhes.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
