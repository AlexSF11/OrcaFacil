package com.example.orcafacil

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.util.Log
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.orcafacil.model.App
import com.example.orcafacil.model.Budget
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import android.graphics.PorterDuff
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat

class EditBudgetActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var llItemsContainer: LinearLayout
    private lateinit var etTotalPrice: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDeleteBudget: Button
    private lateinit var btnAddDescription: Button

    private val descriptionEditTexts = mutableListOf<EditText>()
    private val unitPriceEditTexts = mutableListOf<EditText>()
    private lateinit var budget: Budget // Armazenar o orçamento recebido

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_budget)

        // Força o modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inicializar os campos
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        llItemsContainer = findViewById(R.id.llItemsContainer)
        etTotalPrice = findViewById(R.id.etTotalPrice)
        btnSave = findViewById(R.id.btnSave)
        btnDeleteBudget = findViewById(R.id.btnDeleteBudget)
        btnAddDescription = findViewById(R.id.btnAddDescription)

        // Aplicar máscara monetária ao etTotalPrice
        aplicarMascaraMonetaria(etTotalPrice)

        // Receber o objeto Budget do Intent
        budget = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("budget", Budget::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("budget")
        } ?: run {
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
        etTotalPrice.setText(NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(budget.totalPrice))

        // Adicionar itens dinamicamente, sem foco inicial
        val minSize = minOf(budget.description.size, budget.unitPrice.size)
        for (i in 0 until minSize) {
            val description = budget.description.getOrNull(i) ?: ""
            val unitPrice = budget.unitPrice.getOrNull(i)?.toString() ?: ""
            addDescriptionAndUnitPriceFields(description, unitPrice, focusOnNewItem = false)
        }

        // Garantir que o ScrollView comece no topo
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView.post {
            scrollView.scrollTo(0, 0)
        }

        // Configurar o botão de adicionar
        btnAddDescription.setOnClickListener {
            addDescriptionAndUnitPriceFields("", "", focusOnNewItem = true)
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
            val newUnitPrice = unitPriceEditTexts.map {
                it.text.toString().replace("R$", "").replace(".", "").replace(",", "").trim().toDoubleOrNull()?.div(100) ?: 0.0
            }
            val newTotalPriceText = etTotalPrice.text.toString().replace("R$", "").replace(".", "").replace(",", "").trim()
            val newTotalPrice = newTotalPriceText.toDoubleOrNull()?.div(100) ?: 0.0

            // Validações
            if (newName.isBlank()) {
                etName.error = "O nome é obrigatório"
                return@setOnClickListener
            }

            // Determinar o caminho do PDF
            val pdfPath = if (budget.pdfPath != null && File(budget.pdfPath).exists()) {
                // Se já existe um pdfPath e o arquivo existe, usar o mesmo caminho para sobrescrever
                budget.pdfPath
            } else {
                // Caso contrário, criar um novo caminho com o padrão Orcamento_${budget.id}.pdf
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/Orcamento_${budget.id}.pdf"
            }

            // Criar um novo objeto Budget com os dados atualizados, incluindo o pdfPath e o numeroOrcamento
            val updatedBudget = Budget(
                id = budget.id,
                name = newName,
                phone = newPhone,
                address = newAddress,
                description = newDescription,
                unitPrice = newUnitPrice,
                totalPrice = newTotalPrice,
                createdDate = budget.createdDate,
                pdfPath = pdfPath, // Manter ou atualizar o pdfPath
                numeroOrcamento = budget.numeroOrcamento // Preservar o numeroOrcamento original
            )

            // Atualizar o orçamento no banco de dados
            thread {
                try {
                    val app = application as App
                    val dao = app.db.budgetDao()
                    dao.update(updatedBudget)

                    runOnUiThread {
                        Toast.makeText(this, "Orçamento atualizado com sucesso!", Toast.LENGTH_SHORT).show()

                        // Gerar e abrir o PDF após salvar
                        try {
                            val pdfFile = File(pdfPath)
                            Log.d("EditBudgetActivity", "Caminho do PDF: ${pdfFile.absolutePath}")
                            generatePDF(pdfFile)
                            Toast.makeText(this, "PDF gerado com sucesso!", Toast.LENGTH_LONG).show()
                            openPDF(pdfFile)
                        } catch (e: Exception) {
                            Log.e("EditBudgetActivity", "Erro ao gerar ou abrir o PDF: ${e.message}", e)
                            Toast.makeText(this, "Erro ao gerar/abrir o PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                        setResult(RESULT_OK)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("EditBudgetActivity", "Erro ao salvar o orçamento: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this, "Erro ao salvar o orçamento: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Configurar o botão de remover
        btnDeleteBudget.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmar Remoção")
                .setMessage("Tem certeza que deseja remover este orçamento?")
                .setPositiveButton("Sim") { _, _ ->
                    thread {
                        try {
                            val app = application as App
                            val dao = app.db.budgetDao()
                            dao.delete(budget)

                            // Remover o PDF associado, se existir
                            budget.pdfPath?.let { path ->
                                val pdfFile = File(path)
                                if (pdfFile.exists()) {
                                    pdfFile.delete()
                                }
                            }

                            runOnUiThread {
                                Toast.makeText(this, "Orçamento removido com sucesso!", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                                finish()
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(this, "Erro ao remover o orçamento: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun generatePDF(file: File) {
        Log.d("DEBUG_PDF", "Iniciando a geração do PDF...")
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        try {
            // Tentar carregar e desenhar o logotipo
            try {
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.raf_logo)
                if (bitmap == null) {
                    Log.e("DEBUG_PDF", "Logotipo não encontrado: R.drawable.drc_logo")
                } else {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 100, false)
                    canvas.drawBitmap(scaledBitmap, (pageInfo.pageWidth - 500) / 2f, 20f, paint)
                }
            } catch (e: Exception) {
                Log.e("DEBUG_PDF", "Erro ao carregar o logotipo: ${e.message}", e)
            }

            paint.textSize = 22f
            paint.isFakeBoldText = true
            val textWidth = paint.measureText("ORÇAMENTO")
            val xTitle = (pageInfo.pageWidth - textWidth) / 2
            val yTitle = 150f
            canvas.drawText("ORÇAMENTO", xTitle, yTitle, paint)

            paint.textSize = 16f
            paint.isFakeBoldText = false
            val spacing = 30f
            var yText = yTitle + 50f
            val tableLeftX = 45f
            val tableRightX = pageInfo.pageWidth - 45f
            val tableInner = pageInfo.pageWidth - 135f

            fun drawRow(label: String, content: String, rightContent: String? = null) {
                // Descrição
                canvas.drawText("$label $content", tableLeftX + 5f, yText + 2f, paint)
                rightContent?.let {
                    // Preço
                    canvas.drawText(it, tableLeftX + 420f, yText + 2f, paint)
                }

                // Linhas laterais
                canvas.drawLine(tableLeftX, yText - 20f, tableLeftX, yText + 10f, paint)
                canvas.drawLine(tableRightX, yText - 20f, tableRightX, yText + 10f, paint)

                // Linha inferior da linha atual
                canvas.drawLine(tableLeftX, yText + 10f, tableRightX, yText + 10f, paint)

                yText += spacing
            }

            fun drawRowItems(label: String, content: String, rightContent: String? = null) {
                val maxTextWidth = tableInner - tableLeftX - 10f // Limite da largura do texto

                // Configuração para quebrar linha caso o texto seja grande
                val textPaint = TextPaint().apply {
                    textSize = paint.textSize
                    typeface = paint.typeface
                    color = paint.color
                }

                val staticLayout = StaticLayout.Builder.obtain(label, 0, label.length, textPaint, maxTextWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(5f, 1f)
                    .build()

                // Obtém altura real do texto para evitar sobreposição
                val rowHeight = staticLayout.height - 10f

                // Desenha o texto na coluna "DESCRIÇÃO"
                canvas.save()
                canvas.translate(tableLeftX + 5f, yText - 20f)
                staticLayout.draw(canvas)
                canvas.restore()

                // Desenha o valor na coluna "VALOR", garantindo alinhamento à direita
                rightContent?.let {
                    val textWidth = paint.measureText(it)
                    canvas.drawText(it, tableLeftX + 420f, yText - 6f, paint)
                }

                // Desenha as bordas das células
                canvas.drawLine(tableLeftX, yText - 20, tableLeftX, yText + rowHeight, paint)
                canvas.drawLine(tableRightX, yText - 20, tableRightX, yText + rowHeight, paint)
                canvas.drawLine(tableInner, yText - 20, tableInner, yText + rowHeight, paint)
                canvas.drawLine(tableLeftX, yText + rowHeight - 10, tableRightX, yText + rowHeight - 10, paint)

                yText += rowHeight + 10f // Ajusta altura para evitar sobreposição
            }

            val dataAtual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            paint.isFakeBoldText = true
            val xRightAlign = pageInfo.pageWidth - 50f  // Margem direita
            val textWidthOrcamento = paint.measureText(budget.numeroOrcamento)
            val textWidthData = paint.measureText(dataAtual)

            // Linha Superior que fecha a parte de cima da tabela
            canvas.drawLine(tableLeftX, yText + -20f, tableRightX, yText + -20f, paint)

            // Ajustando a posição
            canvas.drawText(dataAtual, xRightAlign - textWidthData, 170f, paint)  // Data primeiro, mais acima

            val yNome = yTitle + 50f  // Posição Y do "Nome"
            paint.color = Color.RED
            canvas.drawText(budget.numeroOrcamento, xRightAlign - textWidthOrcamento, yNome, paint)
            paint.color = Color.BLACK

            paint.textSize = 14f
            drawRow("CLIENTE: ", etName.text.toString())
            drawRow("ENDEREÇO: ", etAddress.text.toString())
            drawRow("TELEFONE: ", etPhone.text.toString())
            drawRow("COND.PGTO: ", "50% no inicio da obra, 25% ao decorrer da obra e 25% no final.")

            paint.textSize = 16f
            drawRow("DESCRIÇÃO", "", "PREÇO")

            paint.textSize = 12f
            // Iterar sobre as listas de EditText para obter os valores
            for (i in descriptionEditTexts.indices) {
                val description = descriptionEditTexts[i].text.toString()
                val unitPrice = unitPriceEditTexts[i].text.toString()
                drawRowItems(description, "", unitPrice)
            }
            paint.isFakeBoldText = true

            paint.textSize = 12f

            canvas.drawLine(tableInner, yText - 20, tableInner, yText + 10f, paint)
            paint.isFakeBoldText = true
            drawRow("VALOR TOTAL", "", etTotalPrice.text.toString().ifEmpty { "0.00" })
            paint.isFakeBoldText = false

            val startX = 50f

            // Adicionando rodapé com a nova mensagem
            val footerStartY = yText
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("**Estou ciente com referido orçamento e quanto aos ítens contido nele.qualquer serviço adicional será cobrado a parte.\n", startX, footerStartY, paint)

            // Adicionando informações do cliente no rodapé
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Linha de Assinatura
            canvas.drawLine(50f, yText + 27f, tableRightX, yText + 27f, paint)
            canvas.drawText("CLIENTE:", startX, footerStartY + 25f, paint)

            // Linha de Assinatura
            canvas.drawLine(50f, yText + 47f, tableRightX, yText + 47f, paint)
            canvas.drawText("KAIQUE:", startX, footerStartY + 45f, paint)

            paint.color = Color.BLUE
            canvas.drawText("Rua Queiroz, 15 - Mata Fria", startX, footerStartY + 65f, paint)
            canvas.drawText("Telefone: (11)97988-2751", startX, footerStartY + 80f, paint)
            canvas.drawText("E-mail: kaiquefreire9536@gmail.com", startX, footerStartY + 95f, paint)

            pdfDocument.finishPage(page)

            // Salvar o PDF
            try {
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                fos.close()
                Log.d("DEBUG_PDF", "PDF salvo com sucesso em: ${file.absolutePath}")
            } catch (e: IOException) {
                Log.e("DEBUG_PDF", "Erro ao salvar o PDF: ${e.message}", e)
                throw e // Propaga o erro para ser capturado no bloco externo
            }
        } catch (e: Exception) {
            Log.e("DEBUG_PDF", "Erro ao gerar o PDF: ${e.message}", e)
            throw e // Propaga o erro para ser capturado no bloco externo
        } finally {
            pdfDocument.close()
        }
    }

    private fun openPDF(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        val chooser = Intent.createChooser(intent, "Abrir PDF com")
        try {
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum aplicativo encontrado para abrir PDF", Toast.LENGTH_LONG).show()
        }
    }

    // Função para aplicar máscara monetária
    private fun aplicarMascaraMonetaria(editText: EditText): TextWatcher {
        val watcher = object : TextWatcher {
            private var isUpdating = false
            private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (isUpdating || editable.isNullOrEmpty()) return

                isUpdating = true

                val str = editable.toString().replace(Regex("[^0-9]"), "") // Remove tudo que não for número
                val cleanValue = if (str.isNotEmpty()) str.toDouble() / 100 else 0.0
                val formatted = currencyFormat.format(cleanValue)

                editText.removeTextChangedListener(this) // Remove temporariamente o listener
                editText.setText(formatted)
                editText.setSelection(formatted.length) // Mantém o cursor no final
                editText.addTextChangedListener(this) // Re-adiciona o listener

                isUpdating = false
            }
        }
        editText.addTextChangedListener(watcher)
        editText.setTag(watcher) // Armazena o watcher como tag para controle posterior
        return watcher
    }

    // Função para adicionar campos de descrição e preço unitário
    private fun addDescriptionAndUnitPriceFields(initialDescription: String, initialUnitPrice: String, focusOnNewItem: Boolean = true) {
        // Container principal para o item (vertical)
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16) // Aumenta o padding vertical para maior espaçamento entre linhas
        }

        // Campo de descrição
        val descriptionEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setText(initialDescription)
            background = resources.getDrawable(android.R.drawable.edit_text, null)
            setPadding(12, 12, 12, 12)
            setTextColor(ResourcesCompat.getColor(resources, R.color.edittext_text, theme))
            setHintTextColor(ResourcesCompat.getColor(resources, R.color.edittext_hint, theme))
            hint = "Digite a descrição"
            filters = arrayOf(InputFilter.AllCaps()) // Adiciona filtro de caixa alta
            isSingleLine = false // Permite múltiplas linhas
            minLines = 1 // Garante que o campo tenha pelo menos 1 linha
            maxLines = 5 // Limita a 5 linhas (ajuste conforme necessário)
        }

        // Subcontainer para o campo Valor e o botão de remoção (horizontal)
        val valorERemoverLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0) // Espaço entre a descrição e os campos abaixo
            }
        }

        // Campo de preço unitário com máscara monetária
        val unitPriceEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                3f // Peso maior para ocupar mais espaço
            )
            setText(if (initialUnitPrice.isNotEmpty()) {
                try {
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(initialUnitPrice.toDouble())
                } catch (e: NumberFormatException) {
                    Log.e("InitialUnitPrice", "Erro na conversão inicial: ${e.message}")
                    ""
                }
            } else "")
            background = resources.getDrawable(android.R.drawable.edit_text, null)
            setPadding(12, 12, 12, 12)
            setTextColor(ResourcesCompat.getColor(resources, R.color.edittext_text, theme))
            setHintTextColor(ResourcesCompat.getColor(resources, R.color.edittext_hint, theme))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Valor Unitário"
            aplicarMascaraMonetaria(this) // Aplica máscara monetária
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    atualizarValorTotal() // Atualiza o total ao mudar o valor
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Botão de remoção
        val removeButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                40.dpToPx(), // Largura fixa para o ícone
                40.dpToPx(), // Altura fixa para o ícone
                0f // Peso 0, já que estamos usando tamanho fixo
            ).apply {
                setMargins(8, 0, 0, 0) // Espaço à esquerda do botão
            }
            setBackgroundResource(R.drawable.ripple_effect) // Fundo com ripple
            // Define o ícone usando setCompoundDrawablesWithIntrinsicBounds
            val iconDrawable = ContextCompat.getDrawable(this@EditBudgetActivity, R.drawable.trash)
            iconDrawable?.setColorFilter(ContextCompat.getColor(this@EditBudgetActivity, android.R.color.white), PorterDuff.Mode.SRC_IN)
            setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null) // Ícone à esquerda (ou central, já que não há texto)
            contentDescription = "Remover item" // Para acessibilidade
            setPadding(8.dpToPx(), 14.dpToPx(), 8.dpToPx(), 14.dpToPx()) // Padding interno para o ícone
            setOnClickListener {
                try {
                    // Encontrar o índice correto usando as listas em vez de indexOfChild
                    val index = descriptionEditTexts.indexOf(descriptionEditText)
                    if (index >= 0) { // Verifica se o elemento ainda está na lista
                        llItemsContainer.removeView(container)
                        descriptionEditTexts.removeAt(index)
                        unitPriceEditTexts.removeAt(index)
                        atualizarValorTotal() // Atualiza o total ao remover
                    }
                } catch (e: Exception) {
                    Log.e("RemoveButton", "Erro ao remover item: ${e.message}")
                    Toast.makeText(this@EditBudgetActivity, "Erro ao remover item", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Adiciona o campo de valor e o botão de remoção ao subcontainer
        valorERemoverLayout.addView(unitPriceEditText)
        valorERemoverLayout.addView(removeButton)

        // Adiciona os elementos ao container principal
        container.addView(descriptionEditText)
        container.addView(valorERemoverLayout)

        // Adiciona um separador visual (uma linha)
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, 16, 0, 0) // Aumenta a margem superior do separador
            }
            setBackgroundColor(ContextCompat.getColor(this@EditBudgetActivity, android.R.color.darker_gray))
        }
        container.addView(divider)

        // Adiciona o container ao llItemsContainer
        descriptionEditTexts.add(descriptionEditText)
        unitPriceEditTexts.add(unitPriceEditText)
        llItemsContainer.addView(container)

        // Rolar para o novo item adicionado e focar apenas se focusOnNewItem for true
        if (focusOnNewItem) {
            llItemsContainer.post {
                val scrollView = llItemsContainer.parent.parent as ScrollView
                scrollView.smoothScrollTo(0, container.bottom )
            }

            // Focar no campo de descrição do novo item
            descriptionEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(descriptionEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    // Função para atualizar o valor total somando os unitPrices
    private fun atualizarValorTotal() {
        var total = 0.0

        for (unitPriceEditText in unitPriceEditTexts) {
            val valorTexto = unitPriceEditText.text.toString()
                .replace("R$", "")
                .replace(".", "")
                .replace(",", "")
                .trim()
            total += if (valorTexto.isNotEmpty()) {
                valorTexto.toDoubleOrNull()?.div(100) ?: 0.0
            } else {
                0.0
            }
        }

        val moedaFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val totalWatcher = etTotalPrice.getTag() as? TextWatcher
        totalWatcher?.let { etTotalPrice.removeTextChangedListener(it) } // Remove listener temporariamente
        etTotalPrice.setText(moedaFormat.format(total))
        totalWatcher?.let { etTotalPrice.addTextChangedListener(it) } // Re-adiciona o listener
    }

    // Função auxiliar para converter dp para pixels
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}