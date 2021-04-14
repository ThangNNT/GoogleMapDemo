package com.nnt.mapapidemo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.nnt.mapapidemo.databinding.DialogStoreInfoBinding

class StoreInfoDialog: DialogFragment() {

    private lateinit var binding: DialogStoreInfoBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogStoreInfoBinding.inflate(inflater)
       return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.store = arguments?.get(STORE) as Store
    }


    companion object {
        private const val STORE = "STORE"
        val TAG = StoreInfoDialog::class.java.simpleName
        fun newInstance(store: Store): StoreInfoDialog{
            return StoreInfoDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(STORE, store)
                }
            }
        }
    }
}