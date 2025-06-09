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
import java.util.concurrent.TimeUnit

class ConfigPomodoroDialogFragment : DialogFragment() {

    private var _binding: DialogConfigPomodoroBinding? = null
    private val binding get() = _binding!!

    interface ConfigPomodoroListener {
        fun onConfigSalva(focoMin: Int, curtaMin: Int, longaMin: Int, ciclos: Int, hiperfoco: Boolean, som: Boolean)
    }

    private var listener: ConfigPomodoroListener? = null
    private lateinit var prefs: SharedPreferences

    companion object {
        const val TAG = "ConfigPomodoroDialog"
        // SharedPreferences Keys (serão movidas/usadas pelo ViewModel também)
        const val PREFS_NAME = "pomodoro_prefs"
        const val KEY_FOCO_MIN = "foco_min"
        const val KEY_PAUSA_CURTA_MIN = "pausa_curta_min"
        const val KEY_PAUSA_LONGA_MIN = "pausa_longa_min"
        const val KEY_CICLOS_ATE_PAUSA_LONGA = "ciclos_pausa_longa"
        const val KEY_MODO_HIPERFOCO = "modo_hiperfoco" // Pausas mais curtas ainda
        const val KEY_ALARME_SONORO = "alarme_sonoro"


        fun newInstance(): ConfigPomodoroDialogFragment {
            return ConfigPomodoroDialogFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Tenta setar o listener a partir da Activity ou Fragment pai
        listener = parentFragment as? ConfigPomodoroListener ?: activity as? ConfigPomodoroListener
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogConfigPomodoroBinding.inflate(inflater, container, false)
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        binding.editTextTempoFocoConfig.setText(prefs.getInt(KEY_FOCO_MIN, 25).toString())
        binding.editTextPausaCurtaConfig.setText(prefs.getInt(KEY_PAUSA_CURTA_MIN, 5).toString())
        binding.editTextPausaLongaConfig.setText(prefs.getInt(KEY_PAUSA_LONGA_MIN, 15).toString())
        binding.editTextCiclosPausaLongaConfig.setText(prefs.getInt(KEY_CICLOS_ATE_PAUSA_LONGA, 4).toString())
        binding.switchModoHiperfocoConfig.isChecked = prefs.getBoolean(KEY_MODO_HIPERFOCO, false)
        binding.switchAlarmeSonoroConfig.isChecked = prefs.getBoolean(KEY_ALARME_SONORO, true)
    }

    private fun salvarEFechar() {
        val focoMin = binding.editTextTempoFocoConfig.text.toString().toIntOrNull() ?: 25
        val pausaCurtaMin = binding.editTextPausaCurtaConfig.text.toString().toIntOrNull() ?: 5
        val pausaLongaMin = binding.editTextPausaLongaConfig.text.toString().toIntOrNull() ?: 15
        val ciclos = binding.editTextCiclosPausaLongaConfig.text.toString().toIntOrNull() ?: 4
        val hiperfoco = binding.switchModoHiperfocoConfig.isChecked
        val som = binding.switchAlarmeSonoroConfig.isChecked

        // Validação simples
        if (focoMin <= 0 || pausaCurtaMin <= 0 || pausaLongaMin <= 0 || ciclos <= 0) {
            Toast.makeText(context, "Valores devem ser maiores que zero.", Toast.LENGTH_SHORT).show()
            return
        }

        // Salva nas SharedPreferences (ViewModel também fará isso ao ser inicializado ou modificado)
        prefs.edit().apply {
            putInt(KEY_FOCO_MIN, focoMin)
            putInt(KEY_PAUSA_CURTA_MIN, pausaCurtaMin)
            putInt(KEY_PAUSA_LONGA_MIN, pausaLongaMin)
            putInt(KEY_CICLOS_ATE_PAUSA_LONGA, ciclos)
            putBoolean(KEY_MODO_HIPERFOCO, hiperfoco)
            putBoolean(KEY_ALARME_SONORO, som)
            apply()
        }

        listener?.onConfigSalva(focoMin, pausaCurtaMin, pausaLongaMin, ciclos, hiperfoco, som)
        dismiss()
    }

    // Método para Activity setar o listener (se não usar a abordagem do onAttach)
    // fun setListener(listener: ConfigPomodoroListener) {
    //     this.listener = listener
    // }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
