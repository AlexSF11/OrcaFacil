package com.example.orcafacil

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.orcafacil.model.App
import com.example.orcafacil.model.Budget
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rvMain: RecyclerView
    private lateinit var rvRecentBudgets: RecyclerView
    private lateinit var budgetAdapter: BudgetAdapter
    private val recentBudgets = mutableListOf<Budget>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar o RecyclerView para os botões "Fazer Orçamento" e "Meus Orçamentos"
        rvMain = findViewById(R.id.rv_main)
        val mainItems = mutableListOf<MainItem>()
        mainItems.add(
            MainItem(
                id = 1,
                drawableId = R.drawable.new_budget,
                textStringId = R.string.new_budget
            )
        )
        mainItems.add(
            MainItem(
                id = 2,
                drawableId = R.drawable.my_budgets,
                textStringId = R.string.my_budgets
            )
        )

        val mainAdapter = MainAdapter(mainItems) { id ->
            when (id) {
                1 -> {
                    val intent = Intent(this@MainActivity, FazerOrcamentoActivity::class.java)
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(this@MainActivity, MyBudgets::class.java)
                    startActivity(intent)
                }
            }
            Log.i("Teste", "clicou $id!!")
        }

        rvMain.adapter = mainAdapter
        rvMain.layoutManager = GridLayoutManager(this, 2)

        // Configurar o RecyclerView para os últimos orçamentos
        rvRecentBudgets = findViewById(R.id.rvRecentBudgets)
        budgetAdapter = BudgetAdapter(recentBudgets)
        rvRecentBudgets.adapter = budgetAdapter
        rvRecentBudgets.layoutManager = LinearLayoutManager(this)

        // Carregar os 4 últimos orçamentos
        loadRecentBudgets()
    }

    private fun loadRecentBudgets() {
        val app = application as App
        val dao = app.db.budgetDao()
        val budgetsLiveData = dao.getAllBudgets()

        budgetsLiveData.observe(this) { budgets ->
            // Ordena por data de criação (descendente) e pega os 4 primeiros
            val recent = budgets?.sortedByDescending { it.createdDate }?.take(4) ?: emptyList()

            recentBudgets.clear()
            recentBudgets.addAll(recent)
            budgetAdapter.notifyDataSetChanged()

            // Mostrar/esconder o RecyclerView com base nos dados
            rvRecentBudgets.visibility = if (recentBudgets.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar os orçamentos ao voltar para a MainActivity
        loadRecentBudgets()
    }

    private inner class MainAdapter(
        private val mainItems: List<MainItem>,
        private val onItemClickListener: (Int) -> Unit,
    ) : RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
            val view = layoutInflater.inflate(R.layout.main_item, parent, false)
            return MainViewHolder(view)
        }

        override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
            val itemCurrent = mainItems[position]
            holder.bind(itemCurrent)
        }

        override fun getItemCount(): Int {
            return mainItems.size
        }

        private inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: MainItem) {
                val img: ImageView = itemView.findViewById(R.id.item_img_icon)
                val name: TextView = itemView.findViewById(R.id.item_text_name)
                val container: LinearLayout = itemView.findViewById(R.id.item_container_new_budget)

                img.setImageResource(item.drawableId)
                name.setText(item.textStringId)
                container.setOnClickListener {
                    onItemClickListener.invoke(item.id)
                }
            }
        }
    }
}

