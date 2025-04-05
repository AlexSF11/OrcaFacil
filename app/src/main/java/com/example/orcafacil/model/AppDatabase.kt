package com.example.orcafacilraf.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Budget::class], version = 3) // Incrementar a versão de 2 para 3
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        // Migração de versão 1 para 2 (já existente)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar a nova coluna pdfPath à tabela Budget
                database.execSQL("ALTER TABLE Budget ADD COLUMN pdfPath TEXT")
            }
        }

        // Nova migração de versão 2 para 3 para adicionar o campo numeroOrcamento
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar a nova coluna numeroOrcamento à tabela Budget
                database.execSQL("ALTER TABLE Budget ADD COLUMN numeroOrcamento TEXT NOT NULL DEFAULT 'Nº1'")
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
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Adicionar ambas as migrações
                        .build()
                }
                INSTANCE as AppDatabase
            } else {
                INSTANCE as AppDatabase
            }
        }
    }
}