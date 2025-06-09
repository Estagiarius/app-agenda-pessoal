package com.agendafocopei.ui.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat // Import para compatibilidade
import androidx.lifecycle.Observer
import com.agendafocopei.R // Necessário para R.drawable
import com.agendafocopei.databinding.ActivityPomodoroBinding
import com.agendafocopei.ui.pomodoro.ConfigPomodoroDialogFragment
import com.agendafocopei.ui.pomodoro.PomodoroState
import com.agendafocopei.ui.pomodoro.PomodoroViewModel
import java.util.concurrent.TimeUnit

class PomodoroActivity : AppCompatActivity(), ConfigPomodoroDialogFragment.ConfigPomodoroListener {

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels()
    private lateinit var notificationManager: NotificationManager // Para DND
    private lateinit var notificationManagerCompat: NotificationManagerCompat // Para notificações

    private val POMODORO_CHANNEL_ID = "POMODORO_CHANNEL_ID"


    private val requestDndPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Permissão Não Perturbe concedida!", Toast.LENGTH_SHORT).show()
            if (viewModel.estadoAtual.value == PomodoroState.PRONTO && viewModel.ativarModoHiperfoco.value == true) {
                 viewModel.iniciarPausar(applicationContext)
            }
        } else {
            Toast.makeText(this, "Permissão Não Perturbe não concedida. Modo Hiperfoco pode não funcionar como esperado.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManagerCompat = NotificationManagerCompat.from(this)


        createNotificationChannel() // Criar canal de notificação

        val prefs = getSharedPreferences(PomodoroViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        viewModel.inicializarComPrefs(applicationContext, prefs)

        setSupportActionBar(binding.toolbarPomodoro)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupObservers()
        setupClickListeners()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Temporizador Pomodoro" // Nome visível para o usuário
            val descriptionText = "Notificações persistentes para o temporizador Pomodoro"
            val importance = NotificationManager.IMPORTANCE_LOW // Para não fazer som/vibrar a cada update
            val channel = NotificationChannel(POMODORO_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null) // Som da notificação será tocado manualmente pelo ViewModel/MediaPlayer
            }
            // Registrar o canal com o sistema
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }


    private fun setupObservers() {
        viewModel.tempoRestanteMillis.observe(this, Observer { millis ->
            val minutos = TimeUnit.MILLISECONDS.toMinutes(millis)
            val segundos = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            val tempoFormatado = String.format("%02d:%02d", minutos, segundos)
            binding.textViewTempoPomodoro.text = tempoFormatado

            val totalMillis = viewModel.tempoTotalCicloAtualMillis.value ?: millis
            if (totalMillis > 0) {
                val progresso = ((totalMillis - millis).toFloat() / totalMillis * 100).toInt()
                binding.progressBarPomodoro.progress = progresso
            } else {
                binding.progressBarPomodoro.progress = 0
            }
            // Atualizar notificação com o tempo
            if (viewModel.isTimerEffectivelyRunning()) { // Só atualiza se estiver rodando
                 viewModel.mostrarOuAtualizarNotificacao(applicationContext, tempoFormatado)
            }
        })

        viewModel.estadoAtual.observe(this, Observer { estado ->
            val tempoFormatadoAtual = binding.textViewTempoPomodoro.text.toString() // Pega o tempo já formatado
            when (estado) {
                PomodoroState.PRONTO -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pronto"
                    binding.buttonIniciarPausarPomodoro.text = "Iniciar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = false
                    binding.buttonPularCicloPomodoro.isEnabled = false
                    binding.progressBarPomodoro.progress = 0
                    viewModel.cancelarNotificacao(applicationContext)
                }
                PomodoroState.FOCO -> {
                    binding.textViewEstadoCicloPomodoro.text = "Foco"
                    binding.buttonIniciarPausarPomodoro.text = "Pausar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                    viewModel.mostrarOuAtualizarNotificacao(applicationContext, tempoFormatadoAtual)
                }
                PomodoroState.PAUSA_CURTA -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pausa Curta"
                    binding.buttonIniciarPausarPomodoro.text = "Pausar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                    viewModel.mostrarOuAtualizarNotificacao(applicationContext, tempoFormatadoAtual)
                }
                PomodoroState.PAUSA_LONGA -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pausa Longa"
                    binding.buttonIniciarPausarPomodoro.text = "Pausar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                    viewModel.mostrarOuAtualizarNotificacao(applicationContext, tempoFormatadoAtual)
                }
                PomodoroState.PAUSADO -> {
                    binding.textViewEstadoCicloPomodoro.text = "Pausado"
                    binding.buttonIniciarPausarPomodoro.text = "Retomar"
                    binding.buttonIniciarPausarPomodoro.isEnabled = true
                    binding.buttonResetarPomodoro.isEnabled = true
                    binding.buttonPularCicloPomodoro.isEnabled = true
                    // Atualiza notificação para refletir estado pausado, mas com tempo atual
                    viewModel.mostrarOuAtualizarNotificacao(applicationContext, tempoFormatadoAtual)
                }
                null -> {}
            }
        })

        viewModel.cicloFocoAtual.observe(this, Observer { ciclo ->
            val totalCiclosParaPausaLonga = viewModel.ciclosAtePausaLonga.value ?: 4
            if (ciclo > 0 && viewModel.estadoAtual.value != PomodoroState.PRONTO) {
                binding.textViewNumeroCicloPomodoro.text = "Ciclo: $ciclo/$totalCiclosParaPausaLonga"
                binding.textViewNumeroCicloPomodoro.visibility = View.VISIBLE
            } else {
                 binding.textViewNumeroCicloPomodoro.text = "Ciclo: -/-"
            }
        })

        viewModel.ativarModoHiperfoco.observe(this) { hiperfocoAtivado ->
            if (hiperfocoAtivado && (viewModel.estadoAtual.value == PomodoroState.PRONTO || viewModel.estadoAtual.value == PomodoroState.FOCO)) {
                verificarESolicitarPermissaoDnd()
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonIniciarPausarPomodoro.setOnClickListener {
            if (viewModel.estadoAtual.value == PomodoroState.PRONTO && viewModel.ativarModoHiperfoco.value == true) {
                if (!notificationManager.isNotificationPolicyAccessGranted) {
                    verificarESolicitarPermissaoDnd()
                } else {
                    viewModel.iniciarPausar(applicationContext)
                }
            } else {
                viewModel.iniciarPausar(applicationContext)
            }
        }
        binding.buttonResetarPomodoro.setOnClickListener {
            viewModel.resetarCicloAtualOuPomodoro(applicationContext)
        }
        binding.buttonPularCicloPomodoro.setOnClickListener {
            viewModel.pularParaProximoCiclo(applicationContext)
        }
        binding.buttonConfigPomodoro.setOnClickListener {
            ConfigPomodoroDialogFragment.newInstance().show(supportFragmentManager, ConfigPomodoroDialogFragment.TAG)
        }
    }

    private fun verificarESolicitarPermissaoDnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("Para o Modo Hiperfoco funcionar, o app precisa de permissão para gerenciar o Modo Não Perturbe. Deseja ir para as configurações conceder?")
                .setPositiveButton("Sim") { _, _ ->
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    requestDndPermissionLauncher.launch(intent)
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    override fun onConfigSalva(focoMin: Int, curtaMin: Int, longaMin: Int, ciclos: Int, hiperfoco: Boolean, som: Boolean) {
        viewModel.setConfiguracoes(focoMin, curtaMin, longaMin, ciclos, hiperfoco, som)
        Toast.makeText(this, "Configurações salvas e aplicadas.", Toast.LENGTH_SHORT).show()
        if (hiperfoco) {
            verificarESolicitarPermissaoDnd()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Se o timer não for para rodar em background (ViewModel sobrevive à Activity)
        // e a activity está realmente sendo destruída (não por mudança de config),
        // pode ser um local para cancelar a notificação se o ViewModel não for limpo.
        // Mas o onCleared do ViewModel é geralmente mais seguro.
        // if (isFinishing) {
        //     viewModel.cancelarNotificacao(applicationContext)
        // }
    }
}
