package com.example.foodcalories.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodcalories.R
import com.example.foodcalories.data.FoodInfo

class FoodAdapter(
    private var foods: List<FoodInfo> = emptyList(),
    private val onItemClick: (FoodInfo) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {
    
    private var calorieGoal: Int = 0

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameText: TextView = itemView.findViewById(R.id.foodNameText)
        val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        val recommendationText: TextView = itemView.findViewById(R.id.recommendationText)
        val servingSizeText: TextView = itemView.findViewById(R.id.servingSizeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        holder.foodNameText.text = food.name
        holder.descriptionText.text = food.description
        
        // Show recommendation if calorie goal is set
        if (calorieGoal > 0 && food.calories > 0) {
            val recommendedServings = calculateRecommendedServings(food.calories)
            holder.recommendationText.text = holder.itemView.context.getString(
                R.string.recommended_amount, recommendedServings)
            holder.recommendationText.visibility = View.VISIBLE
            holder.servingSizeText.visibility = View.VISIBLE
        } else {
            holder.recommendationText.visibility = View.GONE
            holder.servingSizeText.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(food)
        }
    }
    
    private fun calculateRecommendedServings(caloriesPer100g: Double): Int {
        if (calorieGoal <= 0 || caloriesPer100g <= 0) return 0
        
        // Calculate how many 100g servings would meet the daily calorie goal
        // We'll recommend approximately 1/3 of daily calories from this food
        val targetCalories = calorieGoal / 3.0
        val servings = (targetCalories / caloriesPer100g).toInt()
        
        // Return at least 1 serving if possible
        return maxOf(1, servings)
    }

    override fun getItemCount(): Int = foods.size

    fun updateFoods(newFoods: List<FoodInfo>) {
        foods = newFoods
        notifyDataSetChanged()
    }
    
    fun getCurrentFoods(): List<FoodInfo> {
        return foods
    }
    
    fun setCalorieGoal(goal: Int) {
        calorieGoal = goal
        notifyDataSetChanged()
    }
}
