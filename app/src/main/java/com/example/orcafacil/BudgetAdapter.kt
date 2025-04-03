package com.example.orcafacil

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.orcafacil.model.Budget
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class BudgetAdapter(private val budgets: List<Budget>) :
    RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBudgetName: TextView = itemView.findViewById(R.id.tvBudgetName)
        val tvBudgetCreatedDate: TextView = itemView.findViewById(R.id.tvBudgetCreatedDate)
        val tvBudgetTotalPrice: TextView = itemView.findViewById(R.id.tvBudgetTotalPrice)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgets[position]
        holder.tvBudgetName.text = budget.name
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvBudgetCreatedDate.text = dateFormat.format(budget.createdDate)
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        holder.tvBudgetTotalPrice.text = currencyFormat.format(budget.totalPrice)


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