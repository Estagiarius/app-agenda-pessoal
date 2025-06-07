package com.agendafocopei

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MainActivityTest {

    private lateinit var mainActivity: MainActivity

    @Before
    fun setUp() {
        // Instanciamos a MainActivity diretamente para testes unitários locais.
        // Nota: Componentes Android como Context, SharedPreferences, UI não estarão
        // funcionais aqui a menos que mockados. Estamos focando na lógica pura.
        mainActivity = MainActivity()
        // Limpar a lista de disciplinas antes de cada teste, pois a instância da Activity
        // e sua lista 'disciplinas' são reutilizadas entre os testes no mesmo objeto de teste.
        mainActivity.disciplinas.clear()
    }

    @Test
    fun `adicionarNovaDisciplina_listaAumenta_eContemDisciplina`() {
        val nomeDisciplina = "Cálculo I"
        val resultado = mainActivity.adicionarDisciplinaLogica(nomeDisciplina)

        assertTrue("A adição de uma nova disciplina deve retornar true", resultado)
        assertEquals("A lista deve conter 1 disciplina após adicionar uma nova", 1, mainActivity.disciplinas.size)
        assertTrue("A lista de disciplinas deve conter a disciplina adicionada", mainActivity.disciplinas.contains(nomeDisciplina))
    }

    @Test
    fun `adicionarDisciplinaDuplicada_listaNaoAumenta_retornaFalse`() {
        val nomeDisciplina = "Física I"
        // Adiciona a primeira vez
        mainActivity.adicionarDisciplinaLogica(nomeDisciplina)
        val tamanhoOriginal = mainActivity.disciplinas.size

        // Tenta adicionar a mesma disciplina novamente
        val resultado = mainActivity.adicionarDisciplinaLogica(nomeDisciplina)

        assertFalse("A adição de uma disciplina duplicada deve retornar false", resultado)
        assertEquals("O tamanho da lista não deve mudar ao tentar adicionar uma duplicata", tamanhoOriginal, mainActivity.disciplinas.size)
    }

    @Test
    fun `adicionarMultiplasDisciplinas_listaAumentaCorretamente`() {
        mainActivity.adicionarDisciplinaLogica("Algoritmos")
        mainActivity.adicionarDisciplinaLogica("Estrutura de Dados")
        mainActivity.adicionarDisciplinaLogica("Probabilidade")

        assertEquals("A lista deve conter 3 disciplinas", 3, mainActivity.disciplinas.size)
        assertTrue(mainActivity.disciplinas.contains("Algoritmos"))
        assertTrue(mainActivity.disciplinas.contains("Estrutura de Dados"))
        assertTrue(mainActivity.disciplinas.contains("Probabilidade"))
    }

    @Test
    fun `adicionarDisciplinaComEspacosNoInicioFim_logicaDeveTratarOuNao`() {
        // A lógica atual em adicionarDisciplinaLogica não faz trim().
        // O trim() é feito no listener do botão antes de chamar adicionarDisciplinaLogica.
        // Este teste verifica o comportamento da função adicionarDisciplinaLogica como está.
        val nomeComEspacos = "  História Moderna  "
        val nomeSemEspacos = "História Moderna"

        val resultadoComEspacos = mainActivity.adicionarDisciplinaLogica(nomeComEspacos)
        assertTrue("Adicionar com espaços (se for a primeira vez) deve ser bem sucedido", resultadoComEspacos)
        assertEquals("Lista deve ter 1 item", 1, mainActivity.disciplinas.size)
        assertTrue("Lista deve conter o nome com espaços", mainActivity.disciplinas.contains(nomeComEspacos))

        // Se tentarmos adicionar a versão sem espaços, deve ser considerada diferente
        val resultadoSemEspacos = mainActivity.adicionarDisciplinaLogica(nomeSemEspacos)
        assertTrue("Adicionar a versão sem espaços (diferente da com espaços) deve ser bem sucedido", resultadoSemEspacos)
        assertEquals("Lista deve ter 2 itens", 2, mainActivity.disciplinas.size)
        assertTrue("Lista deve conter a versão sem espaços", mainActivity.disciplinas.contains(nomeSemEspacos))
    }


    @Test
    fun `adicionarDisciplinaVazia_listaNaoAumenta_retornaFalse`() {
        val nomeDisciplina = ""
        val resultado = mainActivity.adicionarDisciplinaLogica(nomeDisciplina)

        assertFalse("A adição de uma disciplina vazia deve retornar false", resultado)
        assertEquals("O tamanho da lista não deve mudar ao tentar adicionar uma disciplina vazia", 0, mainActivity.disciplinas.size)
    }

    @Test
    fun `adicionarDisciplinaComApenasEspacos_listaNaoAumenta_retornaFalse`() {
        // Similar ao teste de string vazia, pois o trim() é feito antes no listener.
        // A função adicionarDisciplinaLogica recebe a string como ela é.
        // Se o listener enviar "   ", adicionarDisciplinaLogica tentará adicionar "   ".
        // Se o listener enviar "" (após trim), o teste acima (adicionarDisciplinaVazia) cobre.
        // Este teste verifica o comportamento de adicionarDisciplinaLogica com "   ".
        val nomeDisciplina = "   "
        val resultado = mainActivity.adicionarDisciplinaLogica(nomeDisciplina)

        // Se a lógica de adicionarDisciplinaLogica NÃO fizer trim, "   " é um nome válido.
        // No nosso caso, adicionarDisciplinaLogica não faz trim, então deve adicionar.
        assertTrue("Adicionar disciplina com apenas espaços deve retornar true (pois não está vazia)", resultado)
        assertEquals("O tamanho da lista deve aumentar se '   ' for considerado nome válido", 1, mainActivity.disciplinas.size)
        assertTrue(mainActivity.disciplinas.contains("   "))

        // Se o requisito fosse que adicionarDisciplinaLogica rejeitasse "   ", o teste seria:
        // assertFalse("Adicionar disciplina com apenas espaços deve retornar false", resultado)
        // assertEquals("O tamanho da lista não deve mudar", 0, mainActivity.disciplinas.size)
    }
}
