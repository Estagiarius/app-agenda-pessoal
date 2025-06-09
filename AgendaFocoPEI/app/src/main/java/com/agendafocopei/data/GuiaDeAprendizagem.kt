package com.agendafocopei.data

import androidx.room.*

@Entity(
    tableName = "guias_de_aprendizagem",
    foreignKeys = [
        ForeignKey(
            entity = Disciplina::class,
            parentColumns = ["id"],
            childColumns = ["disciplinaId"],
            onDelete = ForeignKey.CASCADE // Se a Disciplina for deletada, os Guias associados são deletados.
        )
    ],
    indices = [Index(value = ["disciplinaId"])]
)
data class GuiaDeAprendizagem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bimestre: String, // Ex: "1º Bimestre", "2º Bimestre", etc.
    val ano: Int,
    val disciplinaId: Int,
    @ColumnInfo(name = "caminho_anexo_guia") val caminhoAnexoGuia: String?, // URI como String
    @ColumnInfo(name = "tipo_anexo_guia") val tipoAnexoGuia: String?, // Ex: "application/pdf"
    @ColumnInfo(name = "titulo_guia") val tituloGuia: String? = null // Título opcional para o guia
)
