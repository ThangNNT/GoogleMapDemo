package com.nnt.mapapidemo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nnt.mapapidemo.databinding.ItemStoreBinding

class StoreAdapter(private val stores: List<Store>, private val onItemClick: (store: Store)->Unit): RecyclerView.Adapter<StoreAdapter.ViewHolder>() {
    private var filterStores: List<Store> = emptyList()

    init {
        filterStores = stores
    }
    class ViewHolder(val binding: ItemStoreBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemStoreBinding.inflate(layoutInflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val store = filterStores[position]
        binding.store = store
        binding.root.setOnClickListener {
            onItemClick(store)
        }
    }

    override fun getItemCount(): Int {
        return if(filterStores.size>=MAX_SHOWING_STORE) MAX_SHOWING_STORE else filterStores.size
    }

    fun filter(keyword: String): Boolean{
         if(keyword.isEmpty()){
             filterStores = emptyList()
        } else {
             filterStores = stores.filter { it.name.removeAccent().contains(keyword.removeAccent(), true) }
        }
        notifyDataSetChanged()
        return filterStores.isNotEmpty()
    }

    companion object {
        private const val MAX_SHOWING_STORE = 5
    }
}