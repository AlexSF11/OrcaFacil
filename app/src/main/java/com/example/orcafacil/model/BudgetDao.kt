package com.example.orcafacil.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BudgetDao {
    @Insert
    fun insert(budget: Budget)

    @Query("SELECT * FROM BUDGET")
    fun getAllBudgets(): LiveData<List<Budget>> // Método para buscar todos os orçamentos
}