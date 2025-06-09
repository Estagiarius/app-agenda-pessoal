package com.agendafocopei.ui.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels // Para by viewModels()
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.agendafocopei.databinding.ActivityPomodoroBinding
import com.agendafocopei.ui.pomodoro.ConfigPomodoroDialogFragment
import com.agendafocopei.ui.pomodoro.PomodoroState
import com.agendafocopei.ui.pomodoro.PomodoroViewModel
import java.util.concurrent.TimeUnit

class PomodoroActivity : AppCompatActivity(), ConfigPomodoroDialogFragment.ConfigPomodoroListener {

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa ViewModel com SharedPreferences
        val prefs = getSharedPreferences(PomodoroViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        viewModel.inicializarComPrefs(prefs)

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbarPomodoro)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // O título já está definido no XML

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.tempoRestanteMillis.observe(this, Observer { millis ->
            val minutos = TimeUnit.MILLISECONDS.toMinutes(millis)
            val segundos = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            binding.textViewTempoPomodoro.text = String.format("%02d:%02d", minutos, segundos)

            // Atualizar ProgressBar
            val totalMillis = viewModel.tempoTotalCicloAtualMillis.value ?: millis // Evita divisão por zero
            if (totalMillis > 0) {
                val progresso = ((totalMillis - millis).toFloat() / totalMillis * 100).toInt()
                binding.progressBarPomodoro.progress = progresso
            } else {
                binding.progressBarPomodoro.progress = 0
            }
        })

        viewModel.estadoAtual.observe(this, Observer { estado ->
            when (estado) {
                PomodoroState.PRONTO -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pronto"
                    binding.buttonIniciarPausarPomodoro.text = "Iniciar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = false
                    binding.buttonPularCicloPomodoro.isEnabled = false
                    binding.progressBarPomodoro.progress = 0
                }
                PomodoroState.FOCO -> {
                    binding.textViewEstadoCicloPomodoro.text = "Foco"
                    binding.buttonIniciarPausarPomodoro.text = "Pausar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                }
                PomodoroState.PAUSA_CURTA -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pausa Curta"
                    binding.buttonIniciarPausarPomodoro.text = "Pausar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                }
                PomodoroState.PAUSA_LONGA -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pausa Longa"
                    binding.buttonIniciarPausarPomodoro.text = "Pausar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                }
                PomodoroState.PAUSADO -> {
                    // Mantém o texto do estado anterior à pausa, ou um genérico "Pausado"
                    // O ViewModel precisaria expor o estadoAntesDePausar para isso.
                    // Por simplicidade, vamos usar "Pausado" e o texto do botão indica a ação.
                    binding.textViewEstadoCicloPomodoro.text = "Pausado"
                    binding.buttonIniciarPausarPomodoro.text = "Retomar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                }
                null -> {} // Não deve acontecer
            }
        })

        viewModel.cicloFocoAtual.observe(this, Observer { ciclo ->
             // Assume que _ciclosAtePausaLonga é acessível ou você tem um LiveData para ele
             // Para simplificar, vou usar um valor fixo aqui, mas idealmente viria do ViewModel
            val totalCiclosParaPausaLonga = prefs.getInt(PomodoroViewModel.KEY_CICLOS_ATE_PAUSA_LONGA, 4)
            if (ciclo > 0) {
                binding.textViewNumeroCicloPomodoro.text = "Ciclo: $ciclo/$totalCiclosParaPausaLonga"
                binding.textViewNumeroCicloPomodoro.visibility = View.VISIBLE
            } else {
                binding.textViewNumeroCicloPomodoro.text = "Ciclo: -/-"
                // Ou ocultar: binding.textViewNumeroCicloPomodoro.visibility = View.INVISIBLE
            }
        })
    }

    private fun setupClickListeners() {
        binding.buttonIniciarPausarPomodoro.setOnClickListener {
            viewModel.iniciarPausar()
        }
        binding.buttonResetarPomodoro.setOnClickListener {
            viewModel.resetarCicloAtualOuPomodoro()
        }
        binding.buttonPularCicloPomodoro.setOnClickListener {
            viewModel.pularParaProximoCiclo()
        }
        binding.buttonConfigPomodoro.setOnClickListener {
            ConfigPomodoroDialogFragment.newInstance().show(supportFragmentManager, ConfigPomodoroDialogFragment.TAG)
        }
    }

    // Implementação do ConfigPomodoroListener
    override fun onConfigSalva(focoMin: Int, curtaMin: Int, longaMin: Int, ciclos: Int, hiperfoco: Boolean, som: Boolean) {
        viewModel.setConfiguracoes(focoMin, curtaMin, longaMin, ciclos, hiperfoco, som)
        Toast.makeText(this, "Configurações salvas e aplicadas.", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
