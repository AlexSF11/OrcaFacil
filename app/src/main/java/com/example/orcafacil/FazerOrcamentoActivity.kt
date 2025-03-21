package com.example.orcafacil

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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

class FazerOrcamentoActivity : AppCompatActivity() {
    private lateinit var etNome: EditText
    private lateinit var etCelular: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCpfCnpj: EditText
    private lateinit var etEndereco: EditText
    private lateinit var etRelatorio: EditText
    private lateinit var btnSalvar: Button
    private lateinit var pdfFile: File
    private val listaTarefas = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fazer_orcamento)

        etNome = findViewById(R.id.et_nome)
        etCelular = findViewById(R.id.et_celular)
        etEmail = findViewById(R.id.et_email)
        etCpfCnpj = findViewById(R.id.et_cpf_cnpj)
        etEndereco = findViewById(R.id.et_endereco)
        //etRelatorio = findViewById(R.id.et_relatorio)

        val layoutTarefas = findViewById<LinearLayout>(R.id.layout_tarefas)
        val btnAdicionarTarefa = findViewById<Button>(R.id.btnAdicionarTarefa)

        // Criando um campo de tarefa inicial
        val tarefaInicial = EditText(this)
        tarefaInicial.hint = "Digite a tarefa"
        tarefaInicial.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        layoutTarefas.addView(tarefaInicial) // Adiciona o campo ao layout
        listaTarefas.add(tarefaInicial) // Adiciona à lista de tarefas

        btnAdicionarTarefa.setOnClickListener {
            val novaTarefa = EditText(this)
            novaTarefa.hint = "Digite a tarefa"
            novaTarefa.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutTarefas.addView(novaTarefa)
            listaTarefas.add(novaTarefa)
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


    private fun generatePDF(file: File) {
        Log.d("DEBUG_PDF", "Iniciando a geração do PDF...")
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Adicionando o logotipo
        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.drc_logo)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 100, false)
            canvas.drawBitmap(scaledBitmap, (pageInfo.pageWidth - 500) / 2f, 20f, paint)
        } catch (e: Exception) {
            Log.e("DEBUG_PDF", "Erro ao carregar o logotipo: ${e.message}")
        }

        // Título "ORÇAMENTO"
        paint.textSize = 22f
        paint.isFakeBoldText = true
        val textWidth = paint.measureText("ORÇAMENTO")
        val xTitle = (pageInfo.pageWidth - textWidth) / 2
        val yTitle = 150f
        canvas.drawText("ORÇAMENTO", xTitle, yTitle, paint)

        // Adicionando informações do cliente
        paint.textSize = 16f
        paint.isFakeBoldText = false
        val spacing = 30f
        var yText = yTitle + 50f
        val lineStartX = 50f
        val lineEndX = pageInfo.pageWidth - 50f

        fun drawLine() {
            canvas.drawLine(lineStartX, yText + 10f, lineEndX, yText + 10f, paint)
        }

        drawLine(); canvas.drawText("Nome: ${etNome.text}", 50f, yText, paint); yText += spacing
        drawLine(); canvas.drawText("Celular: ${etCelular.text}", 50f, yText, paint); yText += spacing
        drawLine(); canvas.drawText("Email: ${etEmail.text}", 50f, yText, paint); yText += spacing
        drawLine(); canvas.drawText("CPF/CNPJ: ${etCpfCnpj.text}", 50f, yText, paint); yText += spacing
        drawLine(); canvas.drawText("Endereço: ${etEndereco.text}", 50f, yText, paint); yText += spacing
        //drawLine(); canvas.drawText("Relatório: ${etRelatorio.text}", 50f, yText, paint); yText += spacing

        // Agora, adicionamos as tarefas LOGO APÓS as informações do cliente
        canvas.drawText("Tarefas:", 50f, yText, paint)
        yText += spacing

        for (tarefa in listaTarefas) {
            val textoTarefa = "✔ ${tarefa.text.toString()}"
            canvas.drawText(textoTarefa, 50f, yText, paint)
            yText += spacing
        }

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
        if (etNome.text.toString().trim().isEmpty()) {
            etNome.error = "Preencha o nome!"
            return false
        }
        if (etCelular.text.toString().trim().isEmpty()) {
            etCelular.error = "Preencha o celular!"
            return false
        }
        if (etEmail.text.toString().trim().isEmpty()) {
            etEmail.error = "Preencha o email!"
            return false
        }
        if (etCpfCnpj.text.toString().trim().isEmpty()) {
            etCpfCnpj.error = "Preencha o CPF/CNPJ!"
            return false
        }
        if (etEndereco.text.toString().trim().isEmpty()) {
            etEndereco.error = "Preencha o endereço!"
            return false
        }
//        if (etRelatorio.text.toString().trim().isEmpty()) {
//            etRelatorio.error = "Digite o relatório!"
//            return false
//        }
        return true
    }
}
