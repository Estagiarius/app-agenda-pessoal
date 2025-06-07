package com.agendafocopei.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.agendafocopei.databinding.DialogColorPickerBinding
import com.agendafocopei.ui.adapter.ColorGridAdapter
import com.agendafocopei.utils.ColorUtils

class ColorPickerDialogFragment : DialogFragment() {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    interface ColorPickerListener {
        fun onColorSelected(color: Int, tag: String?)
    }

    private var listener: ColorPickerListener? = null
    private var preselectedColor: Int? = null

    companion object {
        const val TAG = "ColorPickerDialog"
        private const val ARG_PRESELECTED_COLOR = "arg_preselected_color"
        private const val ARG_REQUEST_TAG = "arg_request_tag" // Para identificar quem chamou

        fun newInstance(preselectedColor: Int? = null, requestTag: String? = null): ColorPickerDialogFragment {
            val fragment = ColorPickerDialogFragment()
            val args = Bundle()
            preselectedColor?.let { args.putInt(ARG_PRESELECTED_COLOR, it) }
            requestTag?.let { args.putString(ARG_REQUEST_TAG, it) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_PRESELECTED_COLOR)) {
                preselectedColor = it.getInt(ARG_PRESELECTED_COLOR)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogColorPickerBinding.inflate(inflater, container, false)
        dialog?.setTitle("Escolha uma Cor") // Opcional, já que temos um TextView no layout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val colorAdapter = ColorGridAdapter(ColorUtils.PREDEFINED_COLORS, preselectedColor)
        colorAdapter.onColorSelected = { selectedColor ->
            val requestTag = arguments?.getString(ARG_REQUEST_TAG)
            // Tenta notificar o listener através da Activity pai que implementa a interface
            if (parentFragment is ColorPickerListener) {
                 (parentFragment as ColorPickerListener).onColorSelected(selectedColor, requestTag)
            } else if (activity is ColorPickerListener) {
                 (activity as ColorPickerListener).onColorSelected(selectedColor, requestTag)
            } else {
                 this.listener?.onColorSelected(selectedColor, requestTag)
            }
            dismiss()
        }

        binding.recyclerViewColors.apply {
            layoutManager = GridLayoutManager(context, 5) // 5 colunas, ajuste conforme necessário
            adapter = colorAdapter
        }
    }

    // Método para setar listener programaticamente (alternativa a Activity implementar interface diretamente)
    fun setListener(listener: ColorPickerListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
