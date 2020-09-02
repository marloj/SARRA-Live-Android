package com.example.sarra

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.utterance.view.*

class UtteranceAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var utterances: MutableList<utterance> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return UtteranceHolder(LayoutInflater.from(parent.context).inflate(R.layout.utterance, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is UtteranceHolder -> {
                holder.bind(utterances.get(position))
            }
        }
    }

    override fun getItemCount(): Int {
        return utterances.size
    }

    fun updateLastNonFinal(txt: String, final: Boolean) {
        var last = utterances.lastOrNull()
        if(last == null || last.final) {
            last = utterance("...", false)
            utterances.add(last)
            this.notifyItemInserted(utterances.lastIndex)
        }

        last.text = txt
        last.final = final
        this.notifyItemChanged(utterances.lastIndex, last)
    }

    class UtteranceHolder constructor( itemView: View ) : RecyclerView.ViewHolder(itemView) {
        val text = itemView.text

        fun bind(utt: utterance) {
            text.setText(utt.text)
        }
    }

}