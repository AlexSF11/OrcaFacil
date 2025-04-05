package com.example.orcafacilraf.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BudgetDao {
    @Insert
    fun insert(budget: Budget)

    @Query("SELECT * FROM Budget")
    fun getAllBudgets(): LiveData<List<Budget>>

    @Update
    fun update(budget: Budget)

    @Delete
    fun delete(budget: Budget)

    @Query("SELECT * FROM Budget ORDER BY id DESC LIMIT 1")
    fun getLastBudget(): Budget? // Novo método para buscar o último orçamento
}