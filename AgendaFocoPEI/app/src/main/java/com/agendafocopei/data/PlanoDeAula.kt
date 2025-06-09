package com.agendafocopei.data

import androidx.room.*

@Entity(
    tableName = "planos_de_aula",
    foreignKeys = [
        ForeignKey(
            entity = HorarioAula::class,
            parentColumns = ["id"],
            childColumns = ["horarioAulaId"],
            onDelete = ForeignKey.SET_NULL // Se HorarioAula for deletado, seta horarioAulaId para NULL
        ),
        ForeignKey(
            entity = Disciplina::class,
            parentColumns = ["id"],
            childColumns = ["disciplinaId"],
            onDelete = ForeignKey.CASCADE // Se Disciplina for deletada, o PlanoDeAula associado é deletado
        ),
        ForeignKey(
            entity = Turma::class,
            parentColumns = ["id"],
            childColumns = ["turmaId"],
            onDelete = ForeignKey.SET_NULL // Se Turma for deletada, seta turmaId para NULL
        )
        // ForeignKey para TemplatePlanoAula será adicionada depois se templateUsadoId for usado
    ],
    indices = [
        Index(value = ["horarioAulaId"]),
        Index(value = ["disciplinaId"]),
        Index(value = ["turmaId"]),
        Index(value = ["template_usado_id"]) // Adicionar índice mesmo que a FK seja futura
    ]
)
data class PlanoDeAula(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val horarioAulaId: Int?, // Pode ser nulo se o plano não estiver vinculado a um horário específico
    @ColumnInfo(name = "data_aula") val dataAula: String?, // YYYY-MM-DD, pode ser nulo se vinculado a um horário recorrente
    val disciplinaId: Int,
    val turmaId: Int?, // Pode ser nulo
    @ColumnInfo(name = "titulo_plano") val tituloPlano: String?,
    @ColumnInfo(name = "texto_plano", typeAffinity = ColumnInfo.TEXT) val textoPlano: String?,
    @ColumnInfo(name = "caminho_anexo") val caminhoAnexo: String?, // URI como String
    @ColumnInfo(name = "tipo_anexo") val tipoAnexo: String?, // Ex: "image/jpeg", "application/pdf"
    @ColumnInfo(name = "template_usado_id") val templateUsadoId: Int? = null
)
