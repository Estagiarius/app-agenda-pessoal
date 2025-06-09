package com.agendafocopei.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.agendafocopei.data.AppDatabase
import com.agendafocopei.data.TemplatePlanoAula
import com.agendafocopei.data.TemplatePlanoAulaDao
import com.agendafocopei.databinding.DialogFormularioTemplatePlanoAulaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FormularioTemplatePlanoAulaFragment : DialogFragment() {

    private var _binding: DialogFormularioTemplatePlanoAulaBinding? = null
    private val binding get() = _binding!!

    interface FormularioTemplateListener {
        fun onTemplateSalvo(template: TemplatePlanoAula)
    }

    private var listener: FormularioTemplateListener? = null
    private var templateIdParaEdicao: Int? = null

    private lateinit var templateDao: TemplatePlanoAulaDao

    companion object {
        const val TAG = "FormularioTemplateDialog"
        private const val ARG_TEMPLATE_ID = "arg_template_id"

        fun newInstance(templateId: Int? = null): FormularioTemplatePlanoAulaFragment {
            val fragment = FormularioTemplatePlanoAulaFragment()
            val args = Bundle()
            templateId?.let { args.putInt(ARG_TEMPLATE_ID, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_TEMPLATE_ID)) {
                templateIdParaEdicao = it.getInt(ARG_TEMPLATE_ID)
            }
        }
        templateDao = AppDatabase.getDatabase(requireContext()).templatePlanoAulaDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFormularioTemplatePlanoAulaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (templateIdParaEdicao != null) {
            binding.textViewFormularioTemplateTitulo.text = "Editar Template"
            carregarTemplateParaEdicao(templateIdParaEdicao!!)
        } else {
            binding.textViewFormularioTemplateTitulo.text = "Novo Template de Plano de Aula"
        }

        binding.buttonSalvarTemplate.setOnClickListener { salvarTemplate() }
        binding.buttonCancelarTemplate.setOnClickListener { dismiss() }
    }

    private fun carregarTemplateParaEdicao(id: Int) {
        lifecycleScope.launch {
            val template = withContext(Dispatchers.IO) { templateDao.buscarPorId(id) }
            template?.let {
                binding.editTextNomeTemplateForm.setText(it.nomeTemplate)
                binding.editTextCampoHabilidades.setText(it.campoHabilidades)
                binding.editTextCampoRecursos.setText(it.campoRecursos)
                binding.editTextCampoMetodologia.setText(it.campoMetodologia)
                binding.editTextCampoAvaliacao.setText(it.campoAvaliacao)
                // Lidar com outros_campos se houver UI para ele
            }
        }
    }

    private fun salvarTemplate() {
        val nomeTemplate = binding.editTextNomeTemplateForm.text.toString().trim()
        if (nomeTemplate.isEmpty()) {
            binding.editTextNomeTemplateForm.error = "Nome do template é obrigatório."
            return
        } else {
            binding.editTextNomeTemplateForm.error = null
        }

        val template = TemplatePlanoAula(
            id = templateIdParaEdicao ?: 0,
            nomeTemplate = nomeTemplate,
            campoHabilidades = binding.editTextCampoHabilidades.text.toString().trim().ifEmpty { null },
            campoRecursos = binding.editTextCampoRecursos.text.toString().trim().ifEmpty { null },
            campoMetodologia = binding.editTextCampoMetodologia.text.toString().trim().ifEmpty { null },
            campoAvaliacao = binding.editTextCampoAvaliacao.text.toString().trim().ifEmpty { null },
            outrosCampos = null // A ser implementado se houver UI
        )
        listener?.onTemplateSalvo(template)
        dismiss()
    }

    fun setListener(listener: FormularioTemplateListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
