package com.agendafocopei

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CadastroTurmaActivityTest {

    private lateinit var cadastroTurmaActivity: CadastroTurmaActivity

    @Before
    fun setUp() {
        // Instancia direta para teste unitário local.
        // Não teremos UI ou Contexto Android real aqui.
        cadastroTurmaActivity = CadastroTurmaActivity()
    }

    @Test
    fun `isNomeTurmaValido_nomeCorreto_retornaTrue`() {
        assertTrue(cadastroTurmaActivity.isNomeTurmaValido("Turma A"))
    }

    @Test
    fun `isNomeTurmaValido_nomeComEspacosNoInicioFim_retornaTrue`() {
        assertTrue(cadastroTurmaActivity.isNomeTurmaValido("  Turma B  "))
    }

    @Test
    fun `isNomeTurmaValido_nomeNulo_retornaFalse`() {
        assertFalse(cadastroTurmaActivity.isNomeTurmaValido(null))
    }

    @Test
    fun `isNomeTurmaValido_nomeVazio_retornaFalse`() {
        assertFalse(cadastroTurmaActivity.isNomeTurmaValido(""))
    }

    @Test
    fun `isNomeTurmaValido_nomeComApenasEspacos_retornaFalse`() {
        assertFalse(cadastroTurmaActivity.isNomeTurmaValido("   "))
    }
}
