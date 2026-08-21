package com.example.calendarnotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R

enum class SearchResultType { EVENT, NOTE, PERSON }

data class SearchResultItem(
    val type: SearchResultType,
    val id: Long,
    val title: String,
    val subtitle: String,
    val typeLabel: String
)

class SearchResultAdapter(
    private val onClick: (SearchResultItem) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private var items: List<SearchResultItem> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvSearchType)
        val tvTitle: TextView = view.findViewById(R.id.tvSearchTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSearchSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvType.text = item.typeLabel
        holder.tvTitle.text = item.title
        if (item.subtitle.isNotBlank()) {
            holder.tvSubtitle.visibility = View.VISIBLE
            holder.tvSubtitle.text = item.subtitle
        } else {
            holder.tvSubtitle.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun submit(results: List<SearchResultItem>) {
        items = results
        notifyDataSetChanged()
    }
}
