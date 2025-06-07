package com.agendafocopei.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turmas")
data class Turma(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Default para autoGenerate funcionar bem com data class

    @ColumnInfo(name = "nome_turma")
    val nome: String,

    @ColumnInfo(name = "cor", defaultValue = "NULL") // defaultValue para a migração
    val cor: Int? = null
)
