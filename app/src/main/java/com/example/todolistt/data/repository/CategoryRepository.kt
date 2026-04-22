package com.example.todolistt.data.repository

import com.example.todolistt.data.local.Category
import com.example.todolistt.data.local.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun delete(name: String) {
        categoryDao.deleteCategoryByName(name)
    }
}
