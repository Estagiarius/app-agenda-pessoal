package com.agendafocopei.ui.pomodoro

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer // Para o som
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.agendafocopei.R // Para R.raw e R.drawable
import com.agendafocopei.ui.activity.PomodoroActivity // Para o PendingIntent
import java.util.concurrent.TimeUnit

enum class PomodoroState { PRONTO, FOCO, PAUSA_CURTA, PAUSA_LONGA, PAUSADO }

class PomodoroViewModel : ViewModel() {

    companion object {
        const val PREFS_NAME = "pomodoro_prefs"
        const val KEY_FOCO_MIN = "foco_min"
        const val KEY_PAUSA_CURTA_MIN = "pausa_curta_min"
        const val KEY_PAUSA_LONGA_MIN = "pausa_longa_min"
        const val KEY_CICLOS_ATE_PAUSA_LONGA = "ciclos_pausa_longa"
        const val KEY_MODO_HIPERFOCO = "modo_hiperfoco"
        const val KEY_ALARME_SONORO = "alarme_sonoro"
        private const val NOTIFICATION_ID = 123098 // ID único para a notificação Pomodoro
        private const val POMODORO_CHANNEL_ID = "POMODORO_CHANNEL_ID" // Deve ser o mesmo da Activity
    }

    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null

    val duracaoFocoMillis = MutableLiveData(TimeUnit.MINUTES.toMillis(25))
    val duracaoPausaCurtaMillis = MutableLiveData(TimeUnit.MINUTES.toMillis(5))
    val duracaoPausaLongaMillis = MutableLiveData(TimeUnit.MINUTES.toMillis(15))
    val ciclosAtePausaLonga = MutableLiveData(4)
    val ativarModoHiperfoco = MutableLiveData(false)
    val ativarAlarmeSonoro = MutableLiveData(true)

    private val _tempoRestanteMillis = MutableLiveData<Long>()
    val tempoRestanteMillis: LiveData<Long> get() = _tempoRestanteMillis

    private val _estadoAtual = MutableLiveData<PomodoroState>(PomodoroState.PRONTO)
    val estadoAtual: LiveData<PomodoroState> get() = _estadoAtual

    private val _cicloFocoAtual = MutableLiveData<Int>(0)
    val cicloFocoAtual: LiveData<Int> get() = _cicloFocoAtual

    private val _tempoTotalCicloAtualMillis = MutableLiveData<Long>()
    val tempoTotalCicloAtualMillis: LiveData<Long> get() = _tempoTotalCicloAtualMillis

    private var countDownTimer: CountDownTimer? = null
    private var ciclosDeFocoCompletosNaSequencia: Int = 0
    private var millisRestantesAoPausar: Long = 0
    private var estadoAntesDePausar: PomodoroState = PomodoroState.PRONTO
    private var filtroInterrupcaoAnterior: Int? = null
    private var dndAtivadoPeloViewModel = false
    private var mediaPlayer: MediaPlayer? = null


    fun inicializarComPrefs(context: Context, sharedPreferences: SharedPreferences) {
        this.appContext = context.applicationContext
        prefs = sharedPreferences
        carregarConfiguracoes()
        resetarParaEstadoInicial(this.appContext!!)
    }

    private fun carregarConfiguracoes() {
        duracaoFocoMillis.value = TimeUnit.MINUTES.toMillis(prefs.getInt(KEY_FOCO_MIN, 25).toLong())
        duracaoPausaCurtaMillis.value = TimeUnit.MINUTES.toMillis(prefs.getInt(KEY_PAUSA_CURTA_MIN, 5).toLong())
        duracaoPausaLongaMillis.value = TimeUnit.MINUTES.toMillis(prefs.getInt(KEY_PAUSA_LONGA_MIN, 15).toLong())
        ciclosAtePausaLonga.value = prefs.getInt(KEY_CICLOS_ATE_PAUSA_LONGA, 4)
        ativarModoHiperfoco.value = prefs.getBoolean(KEY_MODO_HIPERFOCO, false)
        ativarAlarmeSonoro.value = prefs.getBoolean(KEY_ALARME_SONORO, true)
    }

    private fun salvarConfiguracoes() {
        if (!this::prefs.isInitialized) return
        prefs.edit().apply {
            putInt(KEY_FOCO_MIN, TimeUnit.MILLISECONDS.toMinutes(duracaoFocoMillis.value ?: TimeUnit.MINUTES.toMillis(25)).toInt())
            putInt(KEY_PAUSA_CURTA_MIN, TimeUnit.MILLISECONDS.toMinutes(duracaoPausaCurtaMillis.value ?: TimeUnit.MINUTES.toMillis(5)).toInt())
            putInt(KEY_PAUSA_LONGA_MIN, TimeUnit.MILLISECONDS.toMinutes(duracaoPausaLongaMillis.value ?: TimeUnit.MINUTES.toMillis(15)).toInt())
            putInt(KEY_CICLOS_ATE_PAUSA_LONGA, ciclosAtePausaLonga.value ?: 4)
            putBoolean(KEY_MODO_HIPERFOCO, ativarModoHiperfoco.value ?: false)
            putBoolean(KEY_ALARME_SONORO, ativarAlarmeSonoro.value ?: true)
            apply()
        }
    }

    fun setConfiguracoes(focoMin: Int, curtaMin: Int, longaMin: Int, ciclos: Int, hiperfoco: Boolean, som: Boolean) {
        if (estadoAtual.value != PomodoroState.PRONTO && estadoAtual.value != PomodoroState.PAUSADO) return

        duracaoFocoMillis.value = TimeUnit.MINUTES.toMillis(focoMin.toLong())
        duracaoPausaCurtaMillis.value = TimeUnit.MINUTES.toMillis(pausaCurtaMin.toLong())
        duracaoPausaLongaMillis.value = TimeUnit.MINUTES.toMillis(longaMin.toLong())
        ciclosAtePausaLonga.value = ciclos
        ativarModoHiperfoco.value = hiperfoco
        ativarAlarmeSonoro.value = som

        salvarConfiguracoes()
        appContext?.let { resetarParaEstadoInicial(it) }
    }

    private fun resetarParaEstadoInicial(context: Context) {
        countDownTimer?.cancel(); countDownTimer = null
        ciclosDeFocoCompletosNaSequencia = 0
        _cicloFocoAtual.value = 0
        _estadoAtual.value = PomodoroState.PRONTO
        _tempoRestanteMillis.value = duracaoFocoMillis.value ?: TimeUnit.MINUTES.toMillis(25)
        _tempoTotalCicloAtualMillis.value = duracaoFocoMillis.value ?: TimeUnit.MINUTES.toMillis(25)
        millisRestantesAoPausar = 0
        estadoAntesDePausar = PomodoroState.PRONTO
        restaurarModoNaoPerturbe(context)
        cancelarNotificacao(context)
    }

    private fun iniciarTimer(duracaoMillis: Long, estadoAlvo: PomodoroState, context: Context) {
        if (estadoAlvo == PomodoroState.FOCO && (ativarModoHiperfoco.value == true)) {
            ativarModoNaoPerturbe(context)
        } else {
            restaurarModoNaoPerturbe(context)
        }

        _tempoRestanteMillis.value = duracaoMillis
        _tempoTotalCicloAtualMillis.value = duracaoMillis
        _estadoAtual.value = estadoAlvo

        countDownTimer?.cancel() // Garante que qualquer timer anterior seja cancelado
        countDownTimer = object : CountDownTimer(duracaoMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _tempoRestanteMillis.value = millisUntilFinished
                // Notificação é atualizada pela Activity observando _tempoRestanteMillis
            }
            override fun onFinish() {
                if (ativarAlarmeSonoro.value == true) {
                    tocarSomAlarme(context)
                }
                avancarParaProximoEstado(context)
            }
        }.start()
        // Atualizar notificação para o novo estado/tempo
        val tempoFormatado = formatarMillisParaString(_tempoRestanteMillis.value ?: duracaoMillis)
        mostrarOuAtualizarNotificacao(context, tempoFormatado)
    }

    private fun tocarSomAlarme(context: Context) {
        // Placeholder para som, pois não posso adicionar R.raw.pomodoro_alarm
        Log.d("PomodoroViewModel", "Alarme sonoro TOCARIA AGORA para estado: ${_estadoAtual.value}")
        Toast.makeText(context, "Fim do ciclo: ${_estadoAtual.value}", Toast.LENGTH_SHORT).show()
        // try {
        //     mediaPlayer?.release() // Libera player anterior se houver
        //     mediaPlayer = MediaPlayer.create(context, R.raw.pomodoro_alarm) // Substituir com som real
        //     mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
        //     mediaPlayer?.start()
        // } catch (e: Exception) {
        //     Log.e("PomodoroViewModel", "Erro ao tocar som do alarme", e)
        // }
    }


    private fun avancarParaProximoEstado(context: Context) {
        val ciclosAteLonga = ciclosAtePausaLonga.value ?: 4
        val duracaoFoco = duracaoFocoMillis.value ?: TimeUnit.MINUTES.toMillis(25)
        val duracaoCurta = duracaoPausaCurtaMillis.value ?: TimeUnit.MINUTES.toMillis(5)
        val duracaoLonga = duracaoPausaLongaMillis.value ?: TimeUnit.MINUTES.toMillis(15)

        when (_estadoAtual.value) {
            PomodoroState.FOCO -> {
                restaurarModoNaoPerturbe(context)
                ciclosDeFocoCompletosNaSequencia++
                if (ciclosDeFocoCompletosNaSequencia >= ciclosAteLonga) {
                    iniciarTimer(duracaoLonga, PomodoroState.PAUSA_LONGA, context)
                    ciclosDeFocoCompletosNaSequencia = 0
                } else {
                    iniciarTimer(duracaoCurta, PomodoroState.PAUSA_CURTA, context)
                }
            }
            PomodoroState.PAUSA_CURTA, PomodoroState.PAUSA_LONGA -> {
                val cicloAtual = _cicloFocoAtual.value ?: 0
                _cicloFocoAtual.value = cicloAtual + 1
                if (_estadoAtual.value == PomodoroState.PAUSA_LONGA) {
                     _cicloFocoAtual.value = 1
                     ciclosDeFocoCompletosNaSequencia = 0
                }
                iniciarTimer(duracaoFoco, PomodoroState.FOCO, context)
            }
            else -> { resetarParaEstadoInicial(context) }
        }
    }

    fun iniciarPausar(context: Context) {
        when (_estadoAtual.value) {
            PomodoroState.PRONTO -> {
                _cicloFocoAtual.value = 1
                ciclosDeFocoCompletosNaSequencia = 0
                iniciarTimer(duracaoFocoMillis.value ?: TimeUnit.MINUTES.toMillis(25), PomodoroState.FOCO, context)
            }
            PomodoroState.FOCO, PomodoroState.PAUSA_CURTA, PomodoroState.PAUSA_LONGA -> {
                estadoAntesDePausar = _estadoAtual.value!!
                millisRestantesAoPausar = _tempoRestanteMillis.value ?: 0
                countDownTimer?.cancel()
                _estadoAtual.value = PomodoroState.PAUSADO
                // Atualiza notificação para "Pausado"
                val tempoFormatado = formatarMillisParaString(millisRestantesAoPausar)
                mostrarOuAtualizarNotificacao(context, tempoFormatado)
            }
            PomodoroState.PAUSADO -> {
                if (millisRestantesAoPausar > 0) {
                    iniciarTimer(millisRestantesAoPausar, estadoAntesDePausar, context)
                } else {
                     _estadoAtual.value = estadoAntesDePausar
                    avancarParaProximoEstado(context)
                }
                millisRestantesAoPausar = 0
            }
            null -> resetarParaEstadoInicial(context)
        }
    }

    fun resetarCicloAtualOuPomodoro(context: Context) {
        countDownTimer?.cancel()
        if (_estadoAtual.value == PomodoroState.PRONTO) {
            cancelarNotificacao(context) // Garante que notificação seja removida se estava pronto
            return
        }

        val estadoParaResetar = if (_estadoAtual.value == PomodoroState.PAUSADO) estadoAntesDePausar else _estadoAtual.value

        when (estadoParaResetar) {
            PomodoroState.FOCO -> {
                restaurarModoNaoPerturbe(context) // Garante restauração do DND
                iniciarTimer(duracaoFocoMillis.value ?: TimeUnit.MINUTES.toMillis(25), PomodoroState.FOCO, context)
            }
            PomodoroState.PAUSA_CURTA -> iniciarTimer(duracaoPausaCurtaMillis.value ?: TimeUnit.MINUTES.toMillis(5), PomodoroState.PAUSA_CURTA, context)
            PomodoroState.PAUSA_LONGA -> iniciarTimer(duracaoPausaLongaMillis.value ?: TimeUnit.MINUTES.toMillis(15), PomodoroState.PAUSA_LONGA, context)
            else -> resetarParaEstadoInicial(context) // Inclui PRONTO e PAUSADO (que leva a PRONTO)
        }
    }

    fun pularParaProximoCiclo(context: Context) {
        if (_estadoAtual.value == PomodoroState.PRONTO) {
            iniciarPausar(context)
        } else {
            countDownTimer?.cancel()
            if (_estadoAtual.value == PomodoroState.FOCO || (_estadoAtual.value == PomodoroState.PAUSADO && estadoAntesDePausar == PomodoroState.FOCO)) {
                 restaurarModoNaoPerturbe(context)
            }
            avancarParaProximoEstado(context)
        }
    }

    private fun ativarModoNaoPerturbe(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            if (filtroInterrupcaoAnterior == null) {
                filtroInterrupcaoAnterior = notificationManager.currentInterruptionFilter
            }
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            dndAtivadoPeloViewModel = true
            Log.d("PomodoroViewModel", "Modo Não Perturbe ATIVADO (filtro anterior: $filtroInterrupcaoAnterior).")
        } else { Log.w("PomodoroViewModel", "Permissão para Modo Não Perturbe não concedida ou SDK < M.") }
    }

    private fun restaurarModoNaoPerturbe(context: Context) {
        if (dndAtivadoPeloViewModel && filtroInterrupcaoAnterior != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(filtroInterrupcaoAnterior!!)
                Log.d("PomodoroViewModel", "Modo Não Perturbe RESTAURADO para $filtroInterrupcaoAnterior.")
            } else { Log.w("PomodoroViewModel", "Não foi possível restaurar DND - permissão não concedida ou SDK < M.") }
            filtroInterrupcaoAnterior = null
            dndAtivadoPeloViewModel = false
        }
    }

    fun mostrarOuAtualizarNotificacao(context: Context, tempoFormatado: String) {
        if (!isTimerEffectivelyRunning() && _estadoAtual.value != PomodoroState.PAUSADO) {
             cancelarNotificacao(context) // Cancela se não estiver rodando e não estiver explicitamente pausado
             return
        }

        val intent = Intent(context, PomodoroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Ou FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlag)

        val estadoStr = _estadoAtual.value?.name?.lowercase(Locale.getDefault())?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "Pomodoro"

        val builder = NotificationCompat.Builder(context, POMODORO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Substituir por um ícone de timer/foco real
            .setContentTitle("Pomodoro: $estadoStr")
            .setContentText("Tempo: $tempoFormatado")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Torna persistente enquanto o timer está ativo ou pausado
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            // TODO: Adicionar ações de Pausar/Resetar na notificação posteriormente

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelarNotificacao(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        Log.d("PomodoroViewModel", "Notificação Pomodoro cancelada.")
    }

    fun isTimerEffectivelyRunning(): Boolean = countDownTimer != null &&
                                           (_estadoAtual.value != PomodoroState.PAUSADO &&
                                            _estadoAtual.value != PomodoroState.PRONTO)


    private fun formatarMillisParaString(millis: Long): String {
        val minutos = TimeUnit.MILLISECONDS.toMinutes(millis)
        val segundos = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutos, segundos)
    }


    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        appContext?.let {
            restaurarModoNaoPerturbe(it)
            cancelarNotificacao(it)
        }
        mediaPlayer?.release() // Libera o mediaPlayer
        mediaPlayer = null
    }
}
