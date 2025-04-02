package com.example.orcafacil

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.orcafacil.model.Budget

class BudgetAdapter(private val budgets: List<Budget>) :
    RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBudgetName: TextView = itemView.findViewById(R.id.tvBudgetName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgets[position]
        holder.tvBudgetName.text = budget.name

        // Adicionar listener de clique para abrir a tela de edição
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditBudgetActivity::class.java)
            intent.putExtra("budget", budget)
            (context as MyBudgets).startActivityForResult(intent, EDIT_BUDGET_REQUEST_CODE)
        }
    }

    override fun getItemCount(): Int = budgets.size

    companion object {
        const val EDIT_BUDGET_REQUEST_CODE = 100
    }
}