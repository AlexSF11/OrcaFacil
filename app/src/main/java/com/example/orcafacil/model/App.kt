package com.example.orcafacil.model

import android.app.Application

class App : Application() {

    lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        // Inicializar o banco de dados com suporte à migração
        db = AppDatabase.getDatabase(this)
    }
}