package com.agendafocopei.data

import androidx.room.*

@Entity(
    tableName = "anotacoes",
    foreignKeys = [
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
            onDelete = ForeignKey.SET_NULL // Se a Turma for deletada, turmaId se torna NULL
        )
    ],
    indices = [Index(value = ["turmaId"])]
)
data class Anotacao(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) var conteudo: String,
    @ColumnInfo(name = "data_criacao", defaultValue = "0L") // DefaultValue para Room
    val dataCriacao: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "data_modificacao", defaultValue = "0L") // DefaultValue para Room
    var dataModificacao: Long = System.currentTimeMillis(),
    var cor: Int? = null, // ARGB Int color
    var turmaId: Int?,
    @ColumnInfo(name = "aluno_nome") var alunoNome: String? = null,
    @ColumnInfo(name = "tags_string") var tagsString: String? = null // Ex: "#ideia #importante #reuniao"
)
