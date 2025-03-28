package com.example.orcafacil.model

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface BudgetDao {
    @Insert
    fun insert(budget: Budget)
}