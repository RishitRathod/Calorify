package com.example.foodcalories.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    
    fun saveCalorieGoal(calorieGoal: Int) {
        sharedPreferences.edit().putInt(KEY_CALORIE_GOAL, calorieGoal).apply()
    }
    
    fun getCalorieGoal(): Int {
        return sharedPreferences.getInt(KEY_CALORIE_GOAL, 0)
    }
    
    fun hasCalorieGoal(): Boolean {
        return sharedPreferences.contains(KEY_CALORIE_GOAL)
    }
    
    companion object {
        private const val PREFERENCES_NAME = "food_calories_preferences"
        private const val KEY_CALORIE_GOAL = "calorie_goal"
    }
}