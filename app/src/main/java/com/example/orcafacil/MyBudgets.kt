package com.example.orcafacilraf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.orcafacilraf.model.App
import com.example.orcafacilraf.model.Budget

class MyBudgets : AppCompatActivity() {

    private lateinit var rvBudgets: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var budgetsLiveData: LiveData<List<Budget>>
    private val budgets = mutableListOf<Budget>() // Lista mutável para o adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_budgets)

        // Força o modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inicializar o RecyclerView e o TextView
        rvBudgets = findViewById(R.id.rvBudgets)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvBudgets.layoutManager = LinearLayoutManager(this)

        // Configurar o adapter com a lista mutável
        budgetAdapter = BudgetAdapter(budgets)
        rvBudgets.adapter = budgetAdapter

        // Buscar todos os orçamentos do banco de dados
        fetchAllBudgets()
    }

    private fun fetchAllBudgets() {
        val app = application as App
        val dao = app.db.budgetDao()
        budgetsLiveData = dao.getAllBudgets()

        budgetsLiveData.observe(this) { budgetList ->
            Log.i("MyBudgets", "Budgets received: $budgetList")
            budgets.clear()
            budgets.addAll(budgetList ?: emptyList())
            budgetAdapter.notifyDataSetChanged()

            updateEmptyViewVisibility(budgets.isEmpty())
        }
    }

    // Função para atualizar a visibilidade do tvEmpty e rvBudgets
    fun updateEmptyViewVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            tvEmpty.text = "Nenhum orçamento encontrado"
            tvEmpty.visibility = View.VISIBLE
            rvBudgets.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvBudgets.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BudgetAdapter.EDIT_BUDGET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Recarregar os dados do banco de dados após a edição
            fetchAllBudgets()
        }
    }
}