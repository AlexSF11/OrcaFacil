package com.example.orcafacil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.orcafacil.model.App
import com.example.orcafacil.model.Budget

class MyBudgets : AppCompatActivity() {

    private lateinit var rvBudgets: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var budgetAdapter: BudgetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_budgets)

        // Inicializar o RecyclerView e o TextView
        rvBudgets = findViewById(R.id.rvBudgets)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvBudgets.layoutManager = LinearLayoutManager(this)

        // Buscar todos os orçamentos do banco de dados
        fetchAllBudgets()
    }

    private fun fetchAllBudgets() {
        val app = application as App
        val dao = app.db.budgetDao()
        val budgetsLiveData: LiveData<List<Budget>> = dao.getAllBudgets()

        budgetsLiveData.observe(this) { budgets ->
            Log.i("MyBudgets", "Budgets received: $budgets")
            budgetAdapter = BudgetAdapter(budgets ?: emptyList())
            rvBudgets.adapter = budgetAdapter
            rvBudgets.adapter?.notifyDataSetChanged()

            if (budgets.isNullOrEmpty()) {
                tvEmpty.text = "Nenhum orçamento encontrado"
                tvEmpty.visibility = View.VISIBLE
                rvBudgets.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvBudgets.visibility = View.VISIBLE
            }
        }
    }
}