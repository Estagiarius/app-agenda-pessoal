package com.agendafocopei.data

import androidx.room.*

@Entity(tableName = "templates_plano_aula")
data class TemplatePlanoAula(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nome_template") val nomeTemplate: String,
    @ColumnInfo(name = "campo_habilidades", typeAffinity = ColumnInfo.TEXT) val campoHabilidades: String?,
    @ColumnInfo(name = "campo_recursos", typeAffinity = ColumnInfo.TEXT) val campoRecursos: String?,
    @ColumnInfo(name = "campo_metodologia", typeAffinity = ColumnInfo.TEXT) val campoMetodologia: String?,
    @ColumnInfo(name = "campo_avaliacao", typeAffinity = ColumnInfo.TEXT) val campoAvaliacao: String?,
    @ColumnInfo(name = "outros_campos", typeAffinity = ColumnInfo.TEXT) val outrosCampos: String? // JSON String para campos customizados
)
