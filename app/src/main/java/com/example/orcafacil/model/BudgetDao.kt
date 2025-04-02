package com.example.orcafacil.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BudgetDao {
    @Insert
    fun insert(budget: Budget)

    @Query("SELECT * FROM Budget WHERE name = :name")
    fun getRegisterByName(name: String): LiveData<List<Budget>>

    @Query("SELECT * FROM Budget")
    fun getAllBudgets(): LiveData<List<Budget>>

    @Update
    fun update(budget: Budget) // Novo método para atualizar um orçamento
}