package com.example.orcafacil

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.orcafacil.model.App
import com.example.orcafacil.model.Budget
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class BudgetAdapter(
    private val budgets: MutableList<Budget> // Alterado para MutableList para permitir remoção
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBudgetName: TextView = itemView.findViewById(R.id.tvBudgetName)
        val tvBudgetCreatedDate: TextView = itemView.findViewById(R.id.tvBudgetCreatedDate)
        val tvBudgetTotalPrice: TextView = itemView.findViewById(R.id.tvBudgetTotalPrice)
        val btnPrintPdf: Button = itemView.findViewById(R.id.btnPrintPdf)
        val btnDeleteBudget: Button = itemView.findViewById(R.id.btnDeleteBudget)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

        // Configurar o botão de Imprimir PDF
        holder.btnPrintPdf.setOnClickListener {
            budget.pdfPath?.let { path ->
                val pdfFile = File(path)
                if (pdfFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        holder.itemView.context,
                        "${holder.itemView.context.packageName}.provider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val chooser = Intent.createChooser(intent, "Imprimir PDF")
                    try {
                        holder.itemView.context.startActivity(chooser)
                    } catch (e: Exception) {
                        Toast.makeText(
                            holder.itemView.context,
                            "Nenhum aplicativo de impressão encontrado",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "Arquivo PDF não encontrado!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } ?: Toast.makeText(
                holder.itemView.context,
                "Nenhum PDF associado a este orçamento!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Configurar o botão de Remover com confirmação
        holder.btnDeleteBudget.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Confirmar Remoção")
                .setMessage("Tem certeza que deseja remover este orçamento?")
                .setPositiveButton("Sim") { _, _ ->
                    thread {
                        try {
                            val app = holder.itemView.context.applicationContext as App
                            val dao = app.db.budgetDao()
                            dao.delete(budget)

                            // Remover o PDF associado, se existir
                            budget.pdfPath?.let { path ->
                                val pdfFile = File(path)
                                if (pdfFile.exists()) {
                                    pdfFile.delete()
                                }
                            }

                            // Atualizar a lista na UI
                            (holder.itemView.context as? MyBudgets)?.runOnUiThread {
                                budgets.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, budgets.size)

                                // Atualizar visibilidade do tvEmpty e rvBudgets
                                if (budgets.isEmpty()) {
                                    (holder.itemView.context as MyBudgets).updateEmptyViewVisibility(true)
                                }

                                Toast.makeText(
                                    holder.itemView.context,
                                    "Orçamento removido com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } ?: run {
                                // Caso esteja na MainActivity, apenas atualizar a lista
                                (holder.itemView.context as? MainActivity)?.runOnUiThread {
                                    budgets.removeAt(position)
                                    notifyItemRemoved(position)
                                    notifyItemRangeChanged(position, budgets.size)

                                    // Mostrar/esconder o RecyclerView com base nos dados
                                    val rvRecentBudgets = (holder.itemView.context as MainActivity).findViewById<RecyclerView>(R.id.rvRecentBudgets)
                                    rvRecentBudgets.visibility = if (budgets.isEmpty()) View.GONE else View.VISIBLE

                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Orçamento removido com sucesso!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            (holder.itemView.context as? MyBudgets)?.runOnUiThread {
                                Toast.makeText(
                                    holder.itemView.context,
                                    "Erro ao remover o orçamento: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } ?: (holder.itemView.context as? MainActivity)?.runOnUiThread {
                                Toast.makeText(
                                    holder.itemView.context,
                                    "Erro ao remover o orçamento: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }

        // Adicionar listener de clique para abrir a tela de edição
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditBudgetActivity::class.java)
            intent.putExtra("budget", budget)
            if (context is MyBudgets) {
                context.startActivityForResult(intent, EDIT_BUDGET_REQUEST_CODE)
            } else {
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = budgets.size

    companion object {
        const val EDIT_BUDGET_REQUEST_CODE = 100
    }
}