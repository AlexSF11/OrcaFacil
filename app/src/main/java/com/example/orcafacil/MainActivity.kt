package com.example.orcafacil
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color


class MainActivity : AppCompatActivity() {



    private lateinit var rvMain: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val testIntent = Intent(this, FazerOrcamentoActivity::class.java)
//        startActivity(testIntent)

        val mainItems = mutableListOf<MainItem>()
        mainItems.add(
            MainItem(
                id = 1,
                textStringId = R.string.new_budget
//                color = Color.GREEN
            )
        )

        mainItems.add(
            MainItem(
                id = 2,
                textStringId = R.string.my_budgets
//                color = Color.GRAY
            )
        )




        val adapter = MainAdapter(mainItems) { id ->
            when (id) {
                1 -> {
                    val intent = Intent(this@MainActivity, FazerOrcamentoActivity::class.java)
                    startActivity(intent)
                }
                2 -> {
                    // Abrir outra activity
                }
            }
            Log.i("Teste", "clicou $id!!")
        }



        rvMain = findViewById(R.id.rv_main)
        rvMain.adapter = adapter
        //Comportamento de como os itens serão exibidos
        rvMain.layoutManager = GridLayoutManager(this, 2)


    }

    private inner class MainAdapter(
        private val mainItems: List<MainItem>,
        private val onItemClickListener: (Int) -> Unit,
    ) : RecyclerView.Adapter<MainAdapter.MainViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
            val view = layoutInflater.inflate(R.layout.main_item, parent, false)
            return MainViewHolder(view)
        }

        // Dispara toda vez que tem rolagem na tela
        override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
            val itemCurrent = mainItems[position]
            holder.bind(itemCurrent)
        }

        override fun getItemCount(): Int {
            return mainItems.size
        }




        // Classe da célula
        private inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: MainItem) {
                val name: TextView = itemView.findViewById(R.id.item_text_name)
                val container: LinearLayout = itemView.findViewById(R.id.item_container_new_budget)

                name.setText(item.textStringId)
//                container.setBackgroundColor(item.color)

               container.setOnClickListener {
                   onItemClickListener.invoke(item.id)
               }


            }
        }

    }

}