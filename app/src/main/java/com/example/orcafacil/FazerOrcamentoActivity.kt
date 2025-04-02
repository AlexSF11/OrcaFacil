package com.example.orcafacil

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import androidx.core.content.ContextCompat

class FazerOrcamentoActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etValorTotal: EditText
    private lateinit var btnSalvar: Button
    private lateinit var pdfFile: File
    private val listaTarefas = mutableListOf<EditText>()
    private val listaTarefasValores = mutableListOf<Pair<EditText, EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fazer_orcamento)

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

        val layoutTarefas = findViewById<LinearLayout>(R.id.layout_tarefas)
        val btnAdicionarTarefa = findViewById<Button>(R.id.btnAdicionarTarefa)

        // Função para criar uma nova linha com campos e botão de remoção
        fun criarNovaLinha(): Pair<LinearLayout, Pair<EditText, EditText>> {
            val novaTarefaLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val novaTarefa = EditText(this).apply {
                hint = "Produto / Serviço"
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f
                )
            }

            novaTarefa.filters = arrayOf(InputFilter.AllCaps())

            val novoValorServico = EditText(this).apply {
                hint = "Valor"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
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

            // Botão de remoção pequeno
            val btnRemover = Button(this).apply {
                text = "X"
                layoutParams = LinearLayout.LayoutParams(
                    80, // Largura fixa pequena (ajuste se necessário)
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                // Corrige o erro usando setBackgroundTintList
                setBackgroundTintList(ContextCompat.getColorStateList(this@FazerOrcamentoActivity, android.R.color.darker_gray))
                setOnClickListener {
                    val parentLayout = novaTarefaLayout.parent as LinearLayout
                    val index = parentLayout.indexOfChild(novaTarefaLayout)
                    parentLayout.removeView(novaTarefaLayout) // Remove o layout da linha
                    listaTarefasValores.removeAt(index) // Remove o par da lista
                    atualizarValorTotal() // Atualiza o valor total
                }
            }

            // Adiciona os elementos ao layout horizontal
            novaTarefaLayout.addView(novaTarefa)
            novaTarefaLayout.addView(novoValorServico)
            novaTarefaLayout.addView(btnRemover)

            return Pair(novaTarefaLayout, Pair(novaTarefa, novoValorServico))
        }

        // Criar a primeira linha
        val (primeiraTarefaLayout, primeiraTarefaPair) = criarNovaLinha()
        layoutTarefas.addView(primeiraTarefaLayout)
        listaTarefasValores.add(primeiraTarefaPair)

        // Atualizar o botão "Adicionar +" para usar a função criarNovaLinha
        btnAdicionarTarefa.setOnClickListener {
            val (novaTarefaLayout, novaTarefaPair) = criarNovaLinha()
            layoutTarefas.addView(novaTarefaLayout)
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

                // Executar inserção no banco em uma thread separada
                Thread {
                    val app = application as App
                    val dao = app.db.budgetDao()
                    dao.insert(
                        Budget(
                            name = etName.text.toString(),
                            address = etAddress.text.toString(),
                            phone = etPhone.text.toString(),
                            description = descriptions,
                            unitPrice = unitPrices,
                            totalPrice = totalPrice
                        )
                    )
                }.start()

                Toast.makeText(this, "Orçamento salvo com sucesso!", Toast.LENGTH_LONG).show()

                val timestamp = System.currentTimeMillis()
                val filePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/orcamento_$timestamp.pdf"
                pdfFile = File(filePath)

                generatePDF(pdfFile)
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
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.drc_logo)
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
        val numeroOrcamento = "Nº245"

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
        canvas.drawText("DRC:", startX, footerStartY + 45f, paint)

        paint.color = Color.BLUE
        canvas.drawText("Rua Queiroz, 15 - Mata Fria", startX, footerStartY + 65f, paint)
        canvas.drawText("Telefone: 96218-7332", startX, footerStartY + 80f, paint)
        canvas.drawText("E-mail: naufreire13@gmail.com", startX, footerStartY + 95f, paint)

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
}