package com.agendafocopei.ui.pomodoro

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.agendafocopei.databinding.DialogConfigPomodoroBinding
// Importar as chaves do ViewModel para consistência
// import com.agendafocopei.ui.pomodoro.PomodoroViewModel.Companion.PREFS_NAME // etc.

class ConfigPomodoroDialogFragment : DialogFragment() {

    private var _binding: DialogConfigPomodoroBinding? = null
    private val binding get() = _binding!!

    interface ConfigPomodoroListener {
        fun onConfigSalva(focoMin: Int, curtaMin: Int, longaMin: Int, ciclos: Int, hiperfoco: Boolean, som: Boolean)
    }

    private var listener: ConfigPomodoroListener? = null
    private lateinit var prefs: SharedPreferences

    // Usar as mesmas chaves definidas no PomodoroViewModel
    companion object {
        const val TAG = "ConfigPomodoroDialog"
        // SharedPreferences Keys (replicadas aqui para uso, idealmente de um local comum ou do ViewModel)
        private const val PREFS_NAME_INTERNAL = "pomodoro_prefs" // Evitar conflito com companion do ViewModel se importado diretamente
        private const val KEY_FOCO_MIN_INTERNAL = "foco_min"
        private const val KEY_PAUSA_CURTA_MIN_INTERNAL = "pausa_curta_min"
        private const val KEY_PAUSA_LONGA_MIN_INTERNAL = "pausa_longa_min"
        private const val KEY_CICLOS_ATE_PAUSA_LONGA_INTERNAL = "ciclos_pausa_longa"
        private const val KEY_MODO_HIPERFOCO_INTERNAL = "modo_hiperfoco"
        private const val KEY_ALARME_SONORO_INTERNAL = "alarme_sonoro"

        fun newInstance(): ConfigPomodoroDialogFragment {
            return ConfigPomodoroDialogFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? ConfigPomodoroListener ?: activity as? ConfigPomodoroListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogConfigPomodoroBinding.inflate(inflater, container, false)
        prefs = requireActivity().getSharedPreferences(PREFS_NAME_INTERNAL, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle("Configurações do Pomodoro")
        carregarConfiguracoes()

        binding.buttonSalvarConfigPomodoro.setOnClickListener {
            salvarEFechar()
        }
        binding.buttonCancelarConfigPomodoro.setOnClickListener {
            dismiss()
        }
    }

    private fun carregarConfiguracoes() {
        binding.editTextTempoFocoConfig.setText(prefs.getInt(KEY_FOCO_MIN_INTERNAL, 25).toString())
        binding.editTextPausaCurtaConfig.setText(prefs.getInt(KEY_PAUSA_CURTA_MIN_INTERNAL, 5).toString())
        binding.editTextPausaLongaConfig.setText(prefs.getInt(KEY_PAUSA_LONGA_MIN_INTERNAL, 15).toString())
        binding.editTextCiclosPausaLongaConfig.setText(prefs.getInt(KEY_CICLOS_ATE_PAUSA_LONGA_INTERNAL, 4).toString())
        binding.switchModoHiperfocoConfig.isChecked = prefs.getBoolean(KEY_MODO_HIPERFOCO_INTERNAL, false)
        binding.switchAlarmeSonoroConfig.isChecked = prefs.getBoolean(KEY_ALARME_SONORO_INTERNAL, true)
    }

    private fun salvarEFechar() {
        val focoMin = binding.editTextTempoFocoConfig.text.toString().toIntOrNull() ?: 25
        val pausaCurtaMin = binding.editTextPausaCurtaConfig.text.toString().toIntOrNull() ?: 5
        val pausaLongaMin = binding.editTextPausaLongaConfig.text.toString().toIntOrNull() ?: 15
        val ciclos = binding.editTextCiclosPausaLongaConfig.text.toString().toIntOrNull() ?: 4
        val hiperfoco = binding.switchModoHiperfocoConfig.isChecked
        val som = binding.switchAlarmeSonoroConfig.isChecked

        if (focoMin <= 0 || pausaCurtaMin < 0 || pausaLongaMin < 0 || ciclos <= 0) { // Pausas podem ser 0 para hiperfoco extremo
            Toast.makeText(context, "Valores de tempo e ciclo devem ser positivos.", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit().apply {
            putInt(KEY_FOCO_MIN_INTERNAL, focoMin)
            putInt(KEY_PAUSA_CURTA_MIN_INTERNAL, pausaCurtaMin)
            putInt(KEY_PAUSA_LONGA_MIN_INTERNAL, pausaLongaMin)
            putInt(KEY_CICLOS_ATE_PAUSA_LONGA_INTERNAL, ciclos)
            putBoolean(KEY_MODO_HIPERFOCO_INTERNAL, hiperfoco)
            putBoolean(KEY_ALARME_SONORO_INTERNAL, som)
            apply()
        }

        listener?.onConfigSalva(focoMin, pausaCurtaMin, pausaLongaMin, ciclos, hiperfoco, som)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
