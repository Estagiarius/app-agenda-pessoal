package com.agendafocopei.data

import androidx.room.*

@Entity(
    tableName = "itens_checklist_guia",
    foreignKeys = [
        ForeignKey(
            entity = GuiaDeAprendizagem::class,
            parentColumns = ["id"],
            childColumns = ["guiaAprendizagemId"],
            onDelete = ForeignKey.CASCADE // Se o Guia for deletado, seus itens de checklist são deletados.
        )
    ],
    indices = [Index(value = ["guiaAprendizagemId"])]
)
data class ItemChecklistGuia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val guiaAprendizagemId: Int,
    @ColumnInfo(name = "descricao_item") val descricaoItem: String,
    var concluido: Boolean = false
)
