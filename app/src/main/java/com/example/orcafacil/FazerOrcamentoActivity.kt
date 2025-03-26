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
import android.text.Html
import android.text.InputFilter
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        etValorTotal = findViewById(R.id.et_valor_total)


        etName.filters = arrayOf(InputFilter.AllCaps())
        etAddress.filters = arrayOf(InputFilter.AllCaps())

        //val valoresUnitarios = 150.75
        //etValorTotal.hint = "Ex: R$ %2.f".format(valoresUnitarios)
        //etValorTotal.hint = "100,00"


        val layoutTarefas = findViewById<LinearLayout>(R.id.layout_tarefas)
        val btnAdicionarTarefa = findViewById<Button>(R.id.btnAdicionarTarefa)

        // Criando um layout horizontal para tarefa e valor
        val tarefaLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Criando o campo de tarefa inicial
        val tarefaInicial = EditText(this).apply {
            hint = "Produto / Serviço"
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f
            )
        }

        tarefaInicial.filters = arrayOf(InputFilter.AllCaps())






        // Criando o campo de valor do serviço
        val valorServico = EditText(this).apply {
            hint = "Valor"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )

        }



        listaTarefasValores.add(Pair(tarefaInicial, valorServico))

        valorServico.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                atualizarValorTotal()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })




        // Adicionando os dois campos ao layout horizontal
        tarefaLayout.addView(tarefaInicial)
        tarefaLayout.addView(valorServico)

        // Adicionando o layout ao layout principal
        layoutTarefas.addView(tarefaLayout)

        // Adiciona à lista de tarefas
        //listaTarefas.add(tarefaInicial)


        //layoutTarefas.addView(tarefaInicial) // Adiciona o campo ao layout
        listaTarefas.add(tarefaInicial) // Adiciona à lista de tarefas

// Lista que armazena pares (Tarefa, Valor)


        btnAdicionarTarefa.setOnClickListener {
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
            }


            // 🔹 Adiciona o listener para atualizar o valor total ao digitar
            novoValorServico.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    atualizarValorTotal() // Chama a função para recalcular a soma
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })


            novaTarefaLayout.addView(novaTarefa)
            novaTarefaLayout.addView(novoValorServico)
            layoutTarefas.addView(novaTarefaLayout)

            listaTarefasValores.add(Pair(novaTarefa, novoValorServico))


        }





        btnSalvar = findViewById(R.id.btn_salvar)
        btnSalvar.setOnClickListener {
            if (validarFormulario()) {
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


        for ((_, valor) in listaTarefasValores) {
            val valorTexto = valor.text.toString().replace(",", ".") // Substitui vírgula por ponto para evitar erro
            total += valorTexto.toDoubleOrNull() ?: 0.0
        }

        // Atualiza o valor total corretamente
        etValorTotal.setText("R$ %.2f".format(total)) // Alterado para setText() ao invés de hint
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

        fun drawRow(label: String, content: String, rightContent: String? = null) {
            canvas.drawText("$label $content", tableLeftX + 5f, yText, paint)
            rightContent?.let {
                val textWidth = paint.measureText(it)
                canvas.drawText(it, tableRightX - textWidth - 5f, yText, paint)
            }
            // Linhas laterais
            canvas.drawLine(tableLeftX, yText - 20f, tableLeftX, yText + 10f, paint)
            canvas.drawLine(tableRightX, yText - 20f, tableRightX, yText + 10f, paint)
            // Linha inferior da linha atual
            canvas.drawLine(tableLeftX, yText + 10f, tableRightX, yText + 10f, paint)
            yText += spacing
        }


        val dataAtual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val numeroOrcamento = "Nº245"


        paint.isFakeBoldText = true
        val xRightAlign = pageInfo.pageWidth - 50f  // Margem direita
        val textWidthOrcamento = paint.measureText(numeroOrcamento)
        val textWidthData = paint.measureText(dataAtual)

//        Linha Superior que fecha a parte de cima da tabela
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
        drawRow("COND.PGTO: ","50% no inicio da obra, 25% ao decorrer da obra e 25% no final.")

        paint.textSize = 18f
        drawRow("DESCRIÇÃO", "", "VALOR")

        paint.textSize = 16f


        for ((tarefa, valor) in listaTarefasValores) {
            drawRow(tarefa.text.toString(), "", valor.text.toString())
            //canvas.drawText(tarefa.text.toString(), valor.text.toString())
        }
        paint.isFakeBoldText = true



        paint.textSize = 18f
        drawRow("VALOR TOTAL", "", etValorTotal.text.toString().ifEmpty { "0.00" })
        paint.isFakeBoldText = false



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
