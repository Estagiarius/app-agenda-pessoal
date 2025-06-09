package com.agendafocopei.ui.pomodoro

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class PomodoroViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context // Mock Context para SharedPreferences e DND (embora DND não testado aqui)

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var viewModel: PomodoroViewModel

    // Captors para verificar interações com LiveData
    @Captor
    private lateinit var longCaptor: ArgumentCaptor<Long>

    @Captor
    private lateinit var stateCaptor: ArgumentCaptor<PomodoroState>

    @Captor
    private lateinit var intCaptor: ArgumentCaptor<Int>

    @Before
    fun setUp() {
        // Configuração padrão do mock SharedPreferences
        `when`(mockContext.getSharedPreferences(PomodoroViewModel.PREFS_NAME, Context.MODE_PRIVATE)).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)

        // Configurar valores padrão para carregamento inicial
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_FOCO_MIN, 25)).thenReturn(25)
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_PAUSA_CURTA_MIN, 5)).thenReturn(5)
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_PAUSA_LONGA_MIN, 15)).thenReturn(15)
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_CICLOS_ATE_PAUSA_LONGA, 4)).thenReturn(4)
        `when`(mockSharedPreferences.getBoolean(PomodoroViewModel.KEY_MODO_HIPERFOCO, false)).thenReturn(false)
        `when`(mockSharedPreferences.getBoolean(PomodoroViewModel.KEY_ALARME_SONORO, true)).thenReturn(true)

        viewModel = PomodoroViewModel()
        viewModel.inicializarComPrefs(mockContext, mockSharedPreferences) // Inicializa com mocks
    }

    @Test
    fun `estadoInicial_deveSerProntoETempoDeFocoPadrao`() {
        assertEquals(PomodoroState.PRONTO, viewModel.estadoAtual.value)
        assertEquals(TimeUnit.MINUTES.toMillis(25), viewModel.tempoRestanteMillis.value)
        assertEquals(0, viewModel.cicloFocoAtual.value) // 0 indica pronto para o primeiro ciclo
        assertEquals(TimeUnit.MINUTES.toMillis(25), viewModel.tempoTotalCicloAtualMillis.value)
    }

    @Test
    fun `setConfiguracoes_atualizaDimensoesCorretamenteEChamaReset`() {
        viewModel.setConfiguracoes(focoMin = 30, curtaMin = 6, longaMin = 20, ciclos = 3, hiperfoco = true, som = false)
        // setConfiguracoes chama resetarParaEstadoInicial, que atualiza os LiveData
        assertEquals(PomodoroState.PRONTO, viewModel.estadoAtual.value)
        assertEquals(TimeUnit.MINUTES.toMillis(30), viewModel.tempoRestanteMillis.value)
        assertEquals(TimeUnit.MINUTES.toMillis(30), viewModel.duracaoFocoMillis.value) // Verifica se a config foi atualizada
        assertEquals(true, viewModel.ativarModoHiperfoco.value)
        assertEquals(false, viewModel.ativarAlarmeSonoro.value)
    }

    @Test
    fun `carregarConfiguracoes_carregaValoresDoSharedPreferencesCorretamente`() {
        // Simula diferentes valores nas prefs antes de inicializar
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_FOCO_MIN, 25)).thenReturn(20)
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_PAUSA_CURTA_MIN, 5)).thenReturn(4)
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_PAUSA_LONGA_MIN, 15)).thenReturn(10)
        `when`(mockSharedPreferences.getInt(PomodoroViewModel.KEY_CICLOS_ATE_PAUSA_LONGA, 4)).thenReturn(2)
        `when`(mockSharedPreferences.getBoolean(PomodoroViewModel.KEY_MODO_HIPERFOCO, false)).thenReturn(true)
        `when`(mockSharedPreferences.getBoolean(PomodoroViewModel.KEY_ALARME_SONORO, true)).thenReturn(false)

        viewModel.inicializarComPrefs(mockContext, mockSharedPreferences) // Re-inicializa para carregar novos mocks

        assertEquals(TimeUnit.MINUTES.toMillis(20), viewModel.duracaoFocoMillis.value)
        assertEquals(TimeUnit.MINUTES.toMillis(4), viewModel.duracaoPausaCurtaMillis.value)
        assertEquals(TimeUnit.MINUTES.toMillis(10), viewModel.duracaoPausaLongaMillis.value)
        assertEquals(2, viewModel.ciclosAtePausaLonga.value)
        assertEquals(true, viewModel.ativarModoHiperfoco.value)
        assertEquals(false, viewModel.ativarAlarmeSonoro.value)
    }

    @Test
    fun `salvarConfiguracoes_salvaValoresCorretosNoSharedPreferences`() {
        viewModel.setConfiguracoes(focoMin = 50, curtaMin = 10, longaMin = 25, ciclos = 2, hiperfoco = true, som = false)

        verify(mockEditor).putInt(PomodoroViewModel.KEY_FOCO_MIN, 50)
        verify(mockEditor).putInt(PomodoroViewModel.KEY_PAUSA_CURTA_MIN, 10)
        verify(mockEditor).putInt(PomodoroViewModel.KEY_PAUSA_LONGA_MIN, 25)
        verify(mockEditor).putInt(PomodoroViewModel.KEY_CICLOS_ATE_PAUSA_LONGA, 2)
        verify(mockEditor).putBoolean(PomodoroViewModel.KEY_MODO_HIPERFOCO, true)
        verify(mockEditor).putBoolean(PomodoroViewModel.KEY_ALARME_SONORO, false)
        verify(mockEditor, times(2)).apply() // Uma vez em setConfiguracoes (para bools) e outra em salvarConfiguracoes (para ints)
    }


    @Test
    fun `iniciarPausar_quandoPronto_vaiParaFoco`() {
        viewModel.iniciarPausar(mockContext)
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
        assertEquals(1, viewModel.cicloFocoAtual.value)
        // Testar se o timer iniciou é mais complexo, mas podemos verificar o tempo inicial
        assertEquals(viewModel.duracaoFocoMillis.value, viewModel.tempoRestanteMillis.value)
    }

    @Test
    fun `iniciarPausar_quandoFoco_vaiParaPausado`() {
        viewModel.iniciarPausar(mockContext) // Para FOCO
        // Simular passagem de tempo (não trivial sem Robolectric ou TestCoroutineDispatcher)
        // Para este teste, vamos assumir que o timer estava rodando e o tempo diminuiu um pouco
        // viewModel._tempoRestanteMillis.value = viewModel.duracaoFocoMillis.value!! - 1000 // Simulação manual para teste

        viewModel.iniciarPausar(mockContext) // Para PAUSADO
        assertEquals(PomodoroState.PAUSADO, viewModel.estadoAtual.value)
        // assertTrue(viewModel.millisRestantesAoPausar > 0) // Não podemos garantir sem controle do timer
    }

    @Test
    fun `iniciarPausar_quandoPausadoEmFoco_retomaParaFoco`() {
        viewModel.iniciarPausar(mockContext) // PRONTO -> FOCO
        // Simular que o timer rodou um pouco antes de pausar
        // viewModel._tempoRestanteMillis.value = (viewModel.duracaoFocoMillis.value ?: 0) - 5000L
        // viewModel.millisRestantesAoPausar = viewModel._tempoRestanteMillis.value ?: 0L
        // viewModel.estadoAntesDePausar = PomodoroState.FOCO
        // viewModel._estadoAtual.value = PomodoroState.PAUSADO
        // A forma acima é manipulação interna, o ideal é chamar o método que pausa:
        viewModel.iniciarPausar(mockContext) // FOCO -> PAUSADO (estadoAntesDePausar = FOCO)

        viewModel.iniciarPausar(mockContext) // PAUSADO -> FOCO (retoma)
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
    }

    // Testes para avancarParaProximoEstado são mais complexos pois dependem do onFinish do timer.
    // Vamos testar a lógica de pularParaProximoCiclo que chama avancarParaProximoEstado diretamente.

    @Test
    fun `pularParaProximoCiclo_deFocoAposMenosDeNCiclos_vaiParaPausaCurta`() {
        viewModel.setConfiguracoes(25, 5, 15, 4, false, true) // 4 ciclos para longa
        viewModel.iniciarPausar(mockContext) // Inicia FOCO (ciclo 1)
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
        assertEquals(1, viewModel.cicloFocoAtual.value)

        viewModel.pularParaProximoCiclo(mockContext) // Simula fim do FOCO 1
        assertEquals(PomodoroState.PAUSA_CURTA, viewModel.estadoAtual.value)
        assertEquals(TimeUnit.MINUTES.toMillis(5), viewModel.tempoRestanteMillis.value)
        // cicloFocoAtual ainda é 1, pois a pausa pertence ao ciclo de foco
    }

    @Test
    fun `pularParaProximoCiclo_dePausaCurta_vaiParaFocoEIncrementaCiclo`() {
        viewModel.setConfiguracoes(25, 5, 15, 4, false, true)
        viewModel.iniciarPausar(mockContext) // FOCO 1
        viewModel.pularParaProximoCiclo(mockContext) // PAUSA_CURTA 1
        assertEquals(PomodoroState.PAUSA_CURTA, viewModel.estadoAtual.value)
        assertEquals(1, viewModel.cicloFocoAtual.value)


        viewModel.pularParaProximoCiclo(mockContext) // Simula fim da PAUSA_CURTA 1
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
        assertEquals(2, viewModel.cicloFocoAtual.value) // Próximo ciclo de foco
        assertEquals(TimeUnit.MINUTES.toMillis(25), viewModel.tempoRestanteMillis.value)
    }

    @Test
    fun `pularParaProximoCiclo_deFocoAposNCiclos_vaiParaPausaLonga`() {
        viewModel.setConfiguracoes(1, 1, 3, 2, false, true) // Foco=1, Curta=1, Longa=3, Ciclos=2

        // Ciclo 1
        viewModel.iniciarPausar(mockContext) // Foco 1
        viewModel.pularParaProximoCiclo(mockContext) // Pausa Curta 1

        // Ciclo 2
        viewModel.pularParaProximoCiclo(mockContext) // Foco 2
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
        assertEquals(2, viewModel.cicloFocoAtual.value)

        viewModel.pularParaProximoCiclo(mockContext) // Fim do Foco 2 (atingiu _ciclosAtePausaLonga)
        assertEquals(PomodoroState.PAUSA_LONGA, viewModel.estadoAtual.value)
        assertEquals(TimeUnit.MINUTES.toMillis(3), viewModel.tempoRestanteMillis.value)
        // cicloFocoAtual ainda é 2, pois a pausa longa pertence a este ciclo "maior"
    }

    @Test
    fun `pularParaProximoCiclo_dePausaLonga_vaiParaFocoEResetaCicloParaUm`() {
        viewModel.setConfiguracoes(1, 1, 3, 2, false, true)
        // Leva até a pausa longa
        viewModel.iniciarPausar(mockContext) // F1
        viewModel.pularParaProximoCiclo(mockContext) // PC1
        viewModel.pularParaProximoCiclo(mockContext) // F2
        viewModel.pularParaProximoCiclo(mockContext) // PL (após F2)
        assertEquals(PomodoroState.PAUSA_LONGA, viewModel.estadoAtual.value)
        assertEquals(2, viewModel.cicloFocoAtual.value)


        viewModel.pularParaProximoCiclo(mockContext) // Fim da Pausa Longa
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
        assertEquals(1, viewModel.cicloFocoAtual.value) // Ciclo de foco resetado para 1
        assertEquals(TimeUnit.MINUTES.toMillis(1), viewModel.tempoRestanteMillis.value)
    }


    @Test
    fun `resetarCicloAtualOuPomodoro_quandoEmFoco_reiniciaTempoDeFoco`() {
        viewModel.iniciarPausar(mockContext) // Para FOCO
        // Simular passagem de tempo (não testaremos o tick exato aqui)
        // viewModel._tempoRestanteMillis.value = (viewModel.duracaoFocoMillis.value ?: 0) - 10000L

        viewModel.resetarCicloAtualOuPomodoro(mockContext)
        assertEquals(PomodoroState.FOCO, viewModel.estadoAtual.value)
        assertEquals(viewModel.duracaoFocoMillis.value, viewModel.tempoRestanteMillis.value)
        assertEquals(1, viewModel.cicloFocoAtual.value) // Mantém o ciclo atual
    }

    @Test
    fun `resetarCicloAtualOuPomodoro_quandoPronto_permanecePronto`() {
        viewModel.resetarCicloAtualOuPomodoro(mockContext)
        assertEquals(PomodoroState.PRONTO, viewModel.estadoAtual.value)
        assertEquals(viewModel.duracaoFocoMillis.value, viewModel.tempoRestanteMillis.value)
        assertEquals(0, viewModel.cicloFocoAtual.value)
    }

    @Test
    fun `resetarCicloAtualOuPomodoro_quandoPausado_vaiParaPronto`() {
        viewModel.iniciarPausar(mockContext) // FOCO
        viewModel.iniciarPausar(mockContext) // PAUSADO
        assertEquals(PomodoroState.PAUSADO, viewModel.estadoAtual.value)

        viewModel.resetarCicloAtualOuPomodoro(mockContext)
        assertEquals(PomodoroState.PRONTO, viewModel.estadoAtual.value) // Comportamento atual: reset total se pausado
        assertEquals(viewModel.duracaoFocoMillis.value, viewModel.tempoRestanteMillis.value)
        assertEquals(0, viewModel.cicloFocoAtual.value)
    }

}
