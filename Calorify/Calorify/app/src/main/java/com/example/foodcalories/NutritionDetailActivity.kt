package com.example.foodcalories

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodcalories.ai.NutritionAIService
import com.example.foodcalories.ai.NutritionInfo
import com.example.foodcalories.databinding.ActivityNutritionDetailBinding
import com.example.foodcalories.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class NutritionDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNutritionDetailBinding
    private val nutritionAIService = NutritionAIService()
    private lateinit var preferencesManager: PreferencesManager
    private var currentNutrition: NutritionInfo? = null
    private var foodName: String = "Unknown Food"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        // Get food name from intent
        foodName = intent.getStringExtra("food_name") ?: "Unknown Food"
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = foodName
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        
        // Set up calculate button
        binding.calculateButton.setOnClickListener {
            calculateRecommendation()
        }
        
        // Set up share button
        binding.shareButton.setOnClickListener {
            shareNutritionInfo()
        }
        
        
        // Load nutrition data
        loadNutritionData(foodName)
    }
    
    private fun loadNutritionData(foodName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentLayout.visibility = View.GONE
                
                val nutrition = withContext(Dispatchers.IO) {
                    nutritionAIService.getNutritionInfo(foodName)
                }
                
                currentNutrition = nutrition
                displayNutritionData(foodName, nutrition)
                
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.errorLayout.visibility = View.VISIBLE
                binding.errorText.text = "Error loading nutrition data: ${e.message}"
            }
        }
    }
    
    private fun displayNutritionData(foodName: String, nutrition: NutritionInfo) {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        
        binding.foodNameTitle.text = foodName
        
        // Set nutrition values with "per 100g" clearly stated
        binding.caloriesText.text = " Calories: ${nutrition.calories.toInt()} kcal per 100g"
        binding.proteinText.text = " Protein: ${nutrition.protein.toInt()}g per 100g"
        binding.carbsText.text = " Carbohydrates: ${nutrition.carbs.toInt()}g per 100g"
        binding.fatText.text = " Fat: ${nutrition.fat.toInt()}g per 100g"
        binding.fiberText.text = " Fiber: ${nutrition.fiber.toInt()}g per 100g"
        binding.sugarText.text = " Sugar: ${nutrition.sugar.toInt()}g per 100g"
        
        // Set description
        binding.descriptionText.text = "Nutrition information for $foodName per 100 grams"
        
        // Pre-fill with saved calorie goal if available
        if (preferencesManager.hasCalorieGoal()) {
            binding.calorieIntakeEditText.setText(preferencesManager.getCalorieGoal().toString())
        }
    }
    
    private fun calculateRecommendation() {
        val calorieIntakeText = binding.calorieIntakeEditText.text.toString()
        
        if (calorieIntakeText.isEmpty()) {
            Toast.makeText(this, "Please enter your daily calorie intake", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val calorieIntake = calorieIntakeText.toInt()
            currentNutrition?.let { nutrition ->
                if (nutrition.calories <= 0) {
                    Toast.makeText(this, "Cannot calculate recommendation for foods with 0 calories", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Calculate how many servings (100g each) would meet the calorie goal
                val servings = calorieIntake / nutrition.calories
                val grams = (servings * 100).roundToInt()
                
                // Show recommendation
                binding.recommendationCard.visibility = View.VISIBLE
                binding.recommendationText.text = getString(
                    R.string.recommendation_result,
                    servings,
                    grams
                )
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareNutritionInfo() {
        currentNutrition?.let { nutrition ->
            val shareText = buildString {
                append(" $foodName - Nutrition Information\n\n")
                append(" Per 100g:\n")
                append(" Calories: ${nutrition.calories.toInt()} kcal\n")
                append(" Protein: ${nutrition.protein.toInt()}g\n")
                append(" Carbohydrates: ${nutrition.carbs.toInt()}g\n")
                append(" Fat: ${nutrition.fat.toInt()}g\n")
                append(" Fiber: ${nutrition.fiber.toInt()}g\n")
                append(" Sugar: ${nutrition.sugar.toInt()}g\n\n")
                
                // Add recommendation if available
                val calorieIntakeText = binding.calorieIntakeEditText.text.toString()
                if (calorieIntakeText.isNotEmpty() && binding.recommendationCard.visibility == View.VISIBLE) {
                    val calorieIntake = calorieIntakeText.toIntOrNull() ?: 0
                    if (calorieIntake > 0 && nutrition.calories > 0) {
                        val servings = calorieIntake / nutrition.calories
                        val grams = (servings * 100).toInt()
                        append(" Recommendation for ${calorieIntake} kcal daily intake:\n")
                        append("   ${String.format("%.1f", servings)} servings (${grams}g)\n\n")
                    }
                }
                
                append("📱 Shared from CaloriFy App")
            }
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Nutrition Info: $foodName")
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Nutrition Info"))
        } ?: run {
            Toast.makeText(this, "No nutrition data available to share", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
