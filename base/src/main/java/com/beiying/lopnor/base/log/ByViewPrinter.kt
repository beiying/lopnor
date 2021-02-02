package com.beiying.lopnor.base.log

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beiying.lopnor.base.R

class ByViewPrinter(activity: Activity): ByLogPrinter {
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: LogAdapter
    lateinit var viewPrinterProvider: ByViewPrinterProvider

    init {
        val rootView: FrameLayout = activity.findViewById(android.R.id.content)
        recyclerView = RecyclerView(activity)
        adapter = LogAdapter(LayoutInflater.from(activity))
        val layoutManager: LinearLayoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        viewPrinterProvider = ByViewPrinterProvider(rootView, recyclerView)

    }
    override fun print(config: ByLogConfig, level: Int, tag: String, printString: String) {
        adapter.addItem(ByLogModel(System.currentTimeMillis(), level, tag, printString))
        recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
    }

    class LogAdapter(var layoutInflater: LayoutInflater): RecyclerView.Adapter<LogViewHolder>() {
        private var logs: ArrayList<ByLogModel> = ArrayList()

        fun addItem(log: ByLogModel) {
            logs.add(log)
            notifyItemInserted(logs.size - 1)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            var itemView = layoutInflater.inflate(R.layout.log_item, parent, false)
            return LogViewHolder(itemView)
        }

        override fun getItemCount(): Int {
            return logs.size
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val logModel: ByLogModel = logs[position]

            val color: Int = getHighlightColor(logModel.level)
            holder.tagView.setTextColor(color)
            holder.messageView.setTextColor(color)

            holder.tagView.text = logModel.getFlattend()
            holder.messageView.text = logModel.log
        }

        private fun getHighlightColor(level: Int): Int {
            when (level) {
                ByLogType.A -> return 0xFFFF00
                ByLogType.D -> return 0xFFFFFF
                ByLogType.I -> return 0x6A8759
                ByLogType.W -> return 0xBBB539
                ByLogType.E -> return 0xFF6B68
                ByLogType.V -> return 0xBBBBBB
            }
            return 0xFFFF00
        }

    }

    class LogViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var tagView: TextView = view.findViewById(R.id.tag)
        var messageView: TextView = view.findViewById(R.id.message)
    }

}