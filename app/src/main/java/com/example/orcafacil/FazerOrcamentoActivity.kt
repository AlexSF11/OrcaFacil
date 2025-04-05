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
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
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
import android.graphics.PorterDuff
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat

class FazerOrcamentoActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etValorTotal: EditText
    private lateinit var btnSalvar: Button
    private lateinit var layoutTarefas: LinearLayout
    private lateinit var pdfFile: File
    private val listaTarefas = mutableListOf<EditText>()
    private val listaTarefasValores = mutableListOf<Pair<EditText, EditText>>()
    private lateinit var numeroOrcamento: String // Variável para armazenar o número do orçamento

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fazer_orcamento)

        // Força o modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Obter o último número do orçamento do banco de dados
        Thread {
            val app = application as App
            val dao = app.db.budgetDao()
            val lastBudget = dao.getLastBudget()
            runOnUiThread {
                // Se não houver orçamentos, começar com "Nº1"
                numeroOrcamento = if (lastBudget == null) {
                    "Nº1"
                } else {
                    // Extrair o número do último orçamento e incrementar
                    val lastNumber = lastBudget.numeroOrcamento.replace("Nº", "").toIntOrNull() ?: 0
                    "Nº${lastNumber + 1}"
                }
            }
        }.start()

        etName = findViewById(R.id.et_name)
        etPhone = findViewById(R.id.et_phone)
        etAddress = findViewById(R.id.et_address)

        fun aplicarMascaraMonetaria(editText: EditText) {
            editText.addTextChangedListener(object : TextWatcher {
                private var isUpdating = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(editable: Editable?) {
                    if (isUpdating || editable.isNullOrEmpty()) return

                    isUpdating = true

                    val str = editable.toString()
                    val cleanString = str.replace(Regex("[^0-9]"), "") // Remove tudo que não for número

                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble() / 100
                        val formatted = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed)

                        // Atualiza o texto mantendo a posição correta do cursor
                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                    }

                    isUpdating = false
                }
            })
        }

        etValorTotal = findViewById(R.id.et_valor_total)
        aplicarMascaraMonetaria(etValorTotal)

        // Campos em caixa alta
        etName.filters = arrayOf(InputFilter.AllCaps())
        etAddress.filters = arrayOf(InputFilter.AllCaps())

        layoutTarefas = findViewById(R.id.layout_tarefas)
        val btnAdicionarTarefa = findViewById<Button>(R.id.btnAdicionarTarefa)

        // Função para criar uma nova linha com campos e botão de remoção
        fun criarNovaLinha(focusOnNewItem: Boolean = true): Pair<LinearLayout, Pair<EditText, EditText>> {
            // Container principal para a linha (vertical)
            val novaTarefaLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 16, 0, 16) // Aumenta o padding vertical para maior espaçamento entre linhas
            }

            // Campo de descrição (Produto / Serviço)
            val novaTarefa = EditText(this).apply {
                hint = "Produto / Serviço"
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                background = resources.getDrawable(android.R.drawable.edit_text, null)
                setPadding(12, 12, 12, 12)
                setTextColor(ResourcesCompat.getColor(resources, R.color.edittext_text, theme))
                setHintTextColor(ResourcesCompat.getColor(resources, R.color.edittext_hint, theme))
                filters = arrayOf(InputFilter.AllCaps())
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

            // Campo de valor
            val novoValorServico = EditText(this).apply {
                hint = "Valor Unitário"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    3f // Peso maior para ocupar mais espaço
                )
                background = resources.getDrawable(android.R.drawable.edit_text, null)
                setPadding(12, 12, 12, 12)
                setTextColor(ResourcesCompat.getColor(resources, R.color.edittext_text, theme))
                setHintTextColor(ResourcesCompat.getColor(resources, R.color.edittext_hint, theme))
                aplicarMascaraMonetaria(this)
            }

            // Adiciona listener para atualizar o valor total
            novoValorServico.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    atualizarValorTotal()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Botão de remoção
            val btnRemover = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    40.dpToPx(), // Largura fixa para o ícone
                    40.dpToPx(), // Altura fixa para o ícone
                    0f
                ).apply {
                    setMargins(8, 0, 0, 0) // Espaço à esquerda do botão
                }
                setBackgroundResource(R.drawable.ripple_effect) // Fundo com ripple
                // Define o ícone usando setCompoundDrawablesWithIntrinsicBounds
                val iconDrawable = ContextCompat.getDrawable(this@FazerOrcamentoActivity, R.drawable.trash)
                iconDrawable?.setColorFilter(ContextCompat.getColor(this@FazerOrcamentoActivity, android.R.color.white), PorterDuff.Mode.SRC_IN)
                setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null) // Ícone à esquerda (ou central, já que não há texto)
                contentDescription = "Remover item" // Para acessibilidade
                setPadding(8.dpToPx(), 14.dpToPx(), 8.dpToPx(), 14.dpToPx()) // Padding interno para o ícone
                setOnClickListener {
                    val parentLayout = novaTarefaLayout.parent as LinearLayout
                    val index = parentLayout.indexOfChild(novaTarefaLayout)
                    parentLayout.removeView(novaTarefaLayout) // Remove o layout da linha
                    listaTarefasValores.removeAt(index) // Remove o par da lista
                    atualizarValorTotal() // Atualiza o valor total
                }
            }

            // Adiciona o campo de valor e o botão de remoção ao subcontainer
            valorERemoverLayout.addView(novoValorServico)
            valorERemoverLayout.addView(btnRemover)

            // Adiciona os elementos ao container principal
            novaTarefaLayout.addView(novaTarefa)
            novaTarefaLayout.addView(valorERemoverLayout)

            // Adiciona um separador visual (uma linha)
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    setMargins(0, 16, 0, 0) // Aumenta a margem superior do separador
                }
                setBackgroundColor(ContextCompat.getColor(this@FazerOrcamentoActivity, android.R.color.darker_gray))
            }
            novaTarefaLayout.addView(divider)

            // Adiciona o container ao layoutTarefas
            layoutTarefas.addView(novaTarefaLayout)

            // Rolar para o novo item adicionado e focar apenas se focusOnNewItem for true
            if (focusOnNewItem) {
                layoutTarefas.post {
                    val scrollView = layoutTarefas.parent.parent as ScrollView
                    scrollView.smoothScrollTo(0, novaTarefaLayout.bottom )
                }

                // Focar no campo de descrição do novo item
                novaTarefa.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(novaTarefa, InputMethodManager.SHOW_IMPLICIT)
            }

            return Pair(novaTarefaLayout, Pair(novaTarefa, novoValorServico))
        }

        // Criar a primeira linha para o usuário preencher, sem foco inicial
        val (primeiraTarefaLayout, primeiraTarefaPair) = criarNovaLinha(focusOnNewItem = false)
        listaTarefasValores.add(primeiraTarefaPair)

        // Garantir que o ScrollView comece no topo
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView.post {
            scrollView.scrollTo(0, 0)
        }

        // Atualizar o botão "Adicionar +" para usar a função criarNovaLinha com foco
        btnAdicionarTarefa.setOnClickListener {
            val (novaTarefaLayout, novaTarefaPair) = criarNovaLinha(focusOnNewItem = true)
            listaTarefasValores.add(novaTarefaPair)
        }

        btnSalvar = findViewById(R.id.btn_salvar)
        btnSalvar.setOnClickListener {
            if (validarFormulario()) {
                val descriptions = mutableListOf<String>()
                val unitPrices = mutableListOf<Double>()

                val format = NumberFormat.getInstance(Locale("pt", "BR"))

                for ((tarefa, valor) in listaTarefasValores) {
                    val desc = tarefa.text.toString().trim()
                    val price = valor.text.toString().replace("R$", "").trim()

                    if (desc.isNotEmpty()) descriptions.add(desc)
                    try {
                        unitPrices.add(format.parse(price)?.toDouble() ?: 0.0)
                    } catch (e: Exception) {
                        unitPrices.add(0.0)
                    }
                }

                val totalPrice = try {
                    format.parse(etValorTotal.text.toString().replace("R$", "").trim())?.toDouble() ?: 0.0
                } catch (e: Exception) {
                    e.printStackTrace()
                    0.0
                }

                // Gerar o PDF e obter o caminho
                val timestamp = System.currentTimeMillis()
                val filePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/orcamento_$timestamp.pdf"
                pdfFile = File(filePath)
                generatePDF(pdfFile)

                // Criar o objeto Budget com o caminho do PDF e o número do orçamento
                val budget = Budget(
                    name = etName.text.toString(),
                    phone = etPhone.text.toString(),
                    address = etAddress.text.toString(),
                    description = descriptions,
                    unitPrice = unitPrices,
                    totalPrice = totalPrice,
                    pdfPath = pdfFile.absolutePath,
                    numeroOrcamento = numeroOrcamento // Salvar o número do orçamento
                )

                // Executar inserção no banco em uma thread separada
                Thread {
                    val app = application as App
                    val dao = app.db.budgetDao()
                    dao.insert(budget) // Inserir o orçamento
                    Log.d("FazerOrcamento", "Orçamento salvo com número: $numeroOrcamento")
                }.start()

                Toast.makeText(this, "Orçamento salvo com sucesso!", Toast.LENGTH_LONG).show()
                Log.d("DEBUG_PDF", "Arquivo salvo em: ${pdfFile.absolutePath}")
                Toast.makeText(this, "PDF gerado com sucesso!", Toast.LENGTH_LONG).show()
                openPDF(pdfFile)
            }
        }
    }

    private fun atualizarValorTotal() {
        var total = 0.0

        val format = NumberFormat.getInstance(Locale("pt", "BR"))

        for ((_, valor) in listaTarefasValores) {
            val valorTexto = valor.text.toString()
                .replace("R$", "") // Remove o símbolo da moeda
                .trim()

            try {
                val numero = format.parse(valorTexto)?.toDouble() ?: 0.0
                total += numero
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val moedaFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        etValorTotal.setText(moedaFormat.format(total))
    }

    private fun generatePDF(file: File) {
        Log.d("DEBUG_PDF", "Iniciando a geração do PDF...")
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.raf_logo)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 100, false)
            canvas.drawBitmap(scaledBitmap, (pageInfo.pageWidth - 500) / 2f, 20f, paint)
        } catch (e: Exception) {
            Log.e("DEBUG_PDF", "Erro ao carregar o logotipo: ${e.message}")
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
                val textWidth = paint.measureText(it)
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
        val textWidthOrcamento = paint.measureText(numeroOrcamento)
        val textWidthData = paint.measureText(dataAtual)

        // Linha Superior que fecha a parte de cima da tabela
        canvas.drawLine(tableLeftX, yText + -20f, tableRightX, yText + -20f, paint)

        // Ajustando a posição
        canvas.drawText(dataAtual, xRightAlign - textWidthData, 170f, paint)  // Data primeiro, mais acima

        val yNome = yTitle + 50f  // Posição Y do "Nome"
        paint.color = Color.RED
        canvas.drawText(numeroOrcamento, xRightAlign - textWidthOrcamento, yNome, paint)
        paint.color = Color.BLACK

        paint.textSize = 14f
        drawRow("CLIENTE: ", etName.text.toString())
        drawRow("ENDEREÇO: ", etAddress.text.toString())
        drawRow("TELEFONE: ", etPhone.text.toString())
        drawRow("COND.PGTO: ", "50% no inicio da obra, 25% ao decorrer da obra e 25% no final.")

        paint.textSize = 16f
        drawRow("DESCRIÇÃO", "", "PREÇO")

        paint.textSize = 12f
        for ((tarefa, valor) in listaTarefasValores) {
            drawRowItems(tarefa.text.toString(), "", valor.text.toString())
        }
        paint.isFakeBoldText = true

        paint.textSize = 12f

        canvas.drawLine(tableInner, yText - 20, tableInner, yText + 10f, paint)
        paint.isFakeBoldText = true
        drawRow("VALOR TOTAL", "", etValorTotal.text.toString().ifEmpty { "0.00" })
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

        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            Log.d("DEBUG_PDF", "PDF salvo com sucesso!")
        } catch (e: IOException) {
            Log.e("DEBUG_PDF", "Erro ao salvar o PDF: ${e.message}")
            e.printStackTrace()
        }

        pdfDocument.close()
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

    private fun validarFormulario(): Boolean {
        if (etName.text.toString().trim().isEmpty()) {
            etName.error = "Preencha o nome!"
            return false
        }
        return true
    }

    // Função auxiliar para converter dp para pixels
    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}