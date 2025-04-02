package com.example.orcafacil

import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.orcafacil.model.App
import com.example.orcafacil.model.Budget
import kotlin.concurrent.thread

class EditBudgetActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var llDescriptionContainer: LinearLayout
    private lateinit var llUnitPriceContainer: LinearLayout
    private lateinit var etTotalPrice: EditText
    private lateinit var btnSave: Button
    private lateinit var btnAddDescription: Button

    private val descriptionEditTexts = mutableListOf<EditText>()
    private val unitPriceEditTexts = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_budget)

        // Inicializar os campos
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        llDescriptionContainer = findViewById(R.id.llDescriptionContainer)
        llUnitPriceContainer = findViewById(R.id.llUnitPriceContainer)
        etTotalPrice = findViewById(R.id.etTotalPrice)
        btnSave = findViewById(R.id.btnSave)
        btnAddDescription = findViewById(R.id.btnAddDescription)

        // Receber o objeto Budget do Intent
        val budget: Budget? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("budget", Budget::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("budget")
        }

        if (budget == null) {
            Toast.makeText(this, "Erro ao carregar o orçamento", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Log para depuração
        Log.d("EditBudgetActivity", "Budget recebido: $budget")

        // Preencher os campos com os dados atuais do orçamento
        etName.setText(budget.name)
        etPhone.setText(budget.phone)
        etAddress.setText(budget.address)
        etTotalPrice.setText(budget.totalPrice.toString())

        // Adicionar EditText dinamicamente para a descrição
        budget.description.forEach { description ->
            addDescriptionAndUnitPriceFields(description, budget.unitPrice.getOrNull(budget.description.indexOf(description))?.toString() ?: "")
        }

        // Configurar o botão de adicionar (agora único)
        btnAddDescription.setOnClickListener {
            addDescriptionAndUnitPriceFields("", "")
        }

        // Configurar o botão de salvar
        btnSave.setOnClickListener {
            // Adicionar animação de escala
            btnSave.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnSave.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            // Obter os novos valores dos campos
            val newName = etName.text.toString()
            val newPhone = etPhone.text.toString()
            val newAddress = etAddress.text.toString()
            val newDescription = descriptionEditTexts.map { it.text.toString().trim() }
            val newUnitPrice = unitPriceEditTexts.map { it.text.toString().trim().toDoubleOrNull() ?: 0.0 }
            val newTotalPriceText = etTotalPrice.text.toString()

            // Validações
            if (newName.isBlank()) {
                etName.error = "O nome é obrigatório"
                return@setOnClickListener
            }
            if (newPhone.isBlank()) {
                etPhone.error = "O telefone é obrigatório"
                return@setOnClickListener
            }
            if (newAddress.isBlank()) {
                etAddress.error = "O endereço é obrigatório"
                return@setOnClickListener
            }
            if (newDescription.isEmpty() || newDescription.any { it.isBlank() }) {
                Toast.makeText(this, "Todos os campos de descrição são obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newUnitPrice.isEmpty() || newUnitPrice.any { it == 0.0 }) {
                Toast.makeText(this, "Todos os preços unitários devem ser válidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newTotalPriceText.isBlank()) {
                etTotalPrice.error = "O preço total é obrigatório"
                return@setOnClickListener
            }

            // Converter o preço total
            val newTotalPrice = newTotalPriceText.toDoubleOrNull() ?: 0.0
            if (newTotalPrice == 0.0) {
                etTotalPrice.error = "O preço total deve ser um valor válido"
                return@setOnClickListener
            }

            // Criar um novo objeto Budget com os dados atualizados
            val updatedBudget = Budget(
                id = budget.id,
                name = newName,
                phone = newPhone,
                address = newAddress,
                description = newDescription,
                unitPrice = newUnitPrice,
                totalPrice = newTotalPrice,
                createdDate = budget.createdDate
            )

            // Atualizar o orçamento no banco de dados
            thread {
                val app = application as App
                val dao = app.db.budgetDao()
                dao.update(updatedBudget)

                runOnUiThread {
                    Toast.makeText(this, "Orçamento atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    // Função para adicionar campos de descrição e preço unitário com botão de remoção
    private fun addDescriptionAndUnitPriceFields(initialDescription: String, initialUnitPrice: String) {
        // Container para a descrição
        val descriptionContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val descriptionEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setText(initialDescription)
            background = resources.getDrawable(android.R.drawable.edit_text, null)
            setPadding(12, 12, 12, 12)
            hint = "Digite a descrição"
        }

        val descriptionRemoveButton = Button(this).apply {
            text = "X"
            layoutParams = LinearLayout.LayoutParams(
                80, // Largura fixa pequena (ajuste se necessário)
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 0)
            }
            setBackgroundTintList(ContextCompat.getColorStateList(this@EditBudgetActivity, android.R.color.darker_gray))
            setOnClickListener {
                llDescriptionContainer.removeView(descriptionContainer)
                descriptionEditTexts.remove(descriptionEditText)
            }
        }

        descriptionContainer.addView(descriptionEditText)
        descriptionContainer.addView(descriptionRemoveButton)
        descriptionEditTexts.add(descriptionEditText)
        llDescriptionContainer.addView(descriptionContainer)

        // Container para o preço unitário
        val unitPriceContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val unitPriceEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setText(initialUnitPrice)
            background = resources.getDrawable(android.R.drawable.edit_text, null)
            setPadding(12, 12, 12, 12)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Digite o preço"
        }

        val unitPriceRemoveButton = Button(this).apply {
            text = "X"
            layoutParams = LinearLayout.LayoutParams(
                80, // Largura fixa pequena (ajuste se necessário)
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 0)
            }
            setBackgroundTintList(ContextCompat.getColorStateList(this@EditBudgetActivity, android.R.color.darker_gray))
            setOnClickListener {
                llUnitPriceContainer.removeView(unitPriceContainer)
                unitPriceEditTexts.remove(unitPriceEditText)
            }
        }

        unitPriceContainer.addView(unitPriceEditText)
        unitPriceContainer.addView(unitPriceRemoveButton)
        unitPriceEditTexts.add(unitPriceEditText)
        llUnitPriceContainer.addView(unitPriceContainer)
    }
}