package com.example.orcafacil.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Budget::class], version = 2) // Incrementar a versão de 1 para 2
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        // Definir a migração de versão 1 para 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar a nova coluna pdfPath à tabela Budget
                database.execSQL("ALTER TABLE Budget ADD COLUMN pdfPath TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "orca_facil"
                    )
                        .addMigrations(MIGRATION_1_2) // Adicionar a migração
                        .build()
                }
                INSTANCE as AppDatabase
            } else {
                INSTANCE as AppDatabase
            }
        }
    }
}