package com.example.foodcalories.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NutritionAIService {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    
    // Free Hugging Face API token (you can get your own at https://huggingface.co/settings/tokens)
    private val HF_TOKEN = "hf_demo" // This is a demo token, you can get a free one
    
    suspend fun getNutritionInfo(foodName: String): NutritionInfo = withContext(Dispatchers.IO) {
        try {
            // First try the local nutrition database
            val localInfo = getLocalNutritionInfo(foodName)
            if (localInfo.calories > 0) {
                Log.d("NutritionAI", "Found nutrition info in local database for: $foodName")
                return@withContext localInfo
            }
            
            // If not found locally, try AI analysis
            Log.d("NutritionAI", "Trying AI analysis for: $foodName")
            val aiInfo = getAINutritionInfo(foodName)
            if (aiInfo.calories > 0) {
                return@withContext aiInfo
            }
            
            // Fallback to estimated values
            Log.d("NutritionAI", "Using fallback nutrition info for: $foodName")
            getEstimatedNutritionInfo(foodName)
            
        } catch (e: Exception) {
            Log.e("NutritionAI", "Error getting nutrition info", e)
            getEstimatedNutritionInfo(foodName)
        }
    }
    
    private fun getLocalNutritionInfo(foodName: String): NutritionInfo {
        val normalizedName = foodName.lowercase().trim()
        
        // Comprehensive nutrition database
        val nutritionDatabase = mapOf(
            // Fruits
            "apple" to NutritionInfo(52.0, 0.3, 14.0, 0.2, 2.4, 10.4),
            "banana" to NutritionInfo(89.0, 1.1, 23.0, 0.3, 2.6, 12.2),
            "orange" to NutritionInfo(47.0, 0.9, 12.0, 0.1, 2.4, 9.4),
            "strawberry" to NutritionInfo(33.0, 0.7, 8.0, 0.3, 2.0, 4.9),
            "grape" to NutritionInfo(69.0, 0.7, 18.0, 0.2, 0.9, 16.0),
            "mango" to NutritionInfo(60.0, 0.8, 15.0, 0.4, 1.6, 13.7),
            "pineapple" to NutritionInfo(50.0, 0.5, 13.0, 0.1, 1.4, 9.9),
            "watermelon" to NutritionInfo(30.0, 0.6, 8.0, 0.2, 0.4, 6.2),
            "peach" to NutritionInfo(39.0, 0.9, 10.0, 0.3, 1.5, 8.4),
            "pear" to NutritionInfo(57.0, 0.4, 15.0, 0.1, 3.1, 9.8),
            
            // Vegetables
            "broccoli" to NutritionInfo(34.0, 2.8, 7.0, 0.4, 2.6, 1.5),
            "carrot" to NutritionInfo(41.0, 0.9, 10.0, 0.2, 2.8, 4.7),
            "tomato" to NutritionInfo(18.0, 0.9, 3.9, 0.2, 1.2, 2.6),
            "cucumber" to NutritionInfo(16.0, 0.7, 3.6, 0.1, 0.5, 1.7),
            "lettuce" to NutritionInfo(15.0, 1.4, 2.9, 0.1, 1.3, 0.8),
            "spinach" to NutritionInfo(23.0, 2.9, 3.6, 0.4, 2.2, 0.4),
            "onion" to NutritionInfo(40.0, 1.1, 9.0, 0.1, 1.7, 4.7),
            "potato" to NutritionInfo(77.0, 2.0, 17.0, 0.1, 2.2, 0.8),
            "sweet potato" to NutritionInfo(86.0, 1.6, 20.0, 0.1, 3.0, 4.2),
            "bell pepper" to NutritionInfo(31.0, 1.0, 7.0, 0.3, 2.1, 4.2),
            
            // Grains
            "rice" to NutritionInfo(130.0, 2.7, 28.0, 0.3, 0.4, 0.1),
            "bread" to NutritionInfo(265.0, 9.0, 49.0, 3.2, 2.7, 5.0),
            "pasta" to NutritionInfo(131.0, 5.0, 25.0, 1.1, 1.8, 0.6),
            "oatmeal" to NutritionInfo(68.0, 2.4, 12.0, 1.4, 1.7, 0.3),
            
            // Proteins
            "chicken" to NutritionInfo(165.0, 31.0, 0.0, 3.6, 0.0, 0.0),
            "beef" to NutritionInfo(250.0, 26.0, 0.0, 15.0, 0.0, 0.0),
            "fish" to NutritionInfo(100.0, 20.0, 0.0, 2.0, 0.0, 0.0),
            "egg" to NutritionInfo(155.0, 13.0, 1.1, 11.0, 0.0, 1.1),
            
            // Dairy
            "milk" to NutritionInfo(42.0, 3.4, 5.0, 1.0, 0.0, 5.0),
            "cheese" to NutritionInfo(113.0, 7.0, 0.4, 9.0, 0.0, 0.4),
            "yogurt" to NutritionInfo(59.0, 10.0, 3.6, 0.4, 0.0, 3.2),
            
            // Nuts and Seeds
            "almond" to NutritionInfo(579.0, 21.0, 22.0, 50.0, 12.5, 4.8),
            "peanut" to NutritionInfo(567.0, 26.0, 16.0, 49.0, 8.5, 4.7),
            "walnut" to NutritionInfo(654.0, 15.0, 14.0, 65.0, 6.7, 2.6),
            
            // Oils
            "olive oil" to NutritionInfo(884.0, 0.0, 0.0, 100.0, 0.0, 0.0),
            "corn oil" to NutritionInfo(884.0, 0.0, 0.0, 100.0, 0.0, 0.0),
            "vegetable oil" to NutritionInfo(884.0, 0.0, 0.0, 100.0, 0.0, 0.0)
        )
        
        // Try exact match first
        nutritionDatabase[normalizedName]?.let { return it }
        
        // Try partial matches
        nutritionDatabase.entries.find { (key, _) ->
            normalizedName.contains(key) || key.contains(normalizedName)
        }?.value?.let { return it }
        
        return NutritionInfo(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
    
    private suspend fun getAINutritionInfo(foodName: String): NutritionInfo {
        try {
            // Use a simple text analysis approach since Hugging Face API requires authentication
            // This simulates AI analysis based on food categories
            val category = categorizeFood(foodName)
            return getNutritionByCategory(category, foodName)
        } catch (e: Exception) {
            Log.e("NutritionAI", "AI analysis failed", e)
            return NutritionInfo(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }
    
    private fun categorizeFood(foodName: String): String {
        val name = foodName.lowercase()
        return when {
            name.contains("fruit") || name.contains("apple") || name.contains("banana") || 
            name.contains("orange") || name.contains("berry") || name.contains("grape") ||
            name.contains("mango") || name.contains("pineapple") || name.contains("peach") ||
            name.contains("pear") || name.contains("watermelon") -> "fruit"
            
            name.contains("vegetable") || name.contains("broccoli") || name.contains("carrot") ||
            name.contains("tomato") || name.contains("cucumber") || name.contains("lettuce") ||
            name.contains("spinach") || name.contains("onion") || name.contains("potato") ||
            name.contains("pepper") -> "vegetable"
            
            name.contains("meat") || name.contains("chicken") || name.contains("beef") ||
            name.contains("pork") || name.contains("lamb") || name.contains("fish") ||
            name.contains("seafood") -> "protein"
            
            name.contains("grain") || name.contains("rice") || name.contains("bread") ||
            name.contains("pasta") || name.contains("wheat") || name.contains("oat") ||
            name.contains("corn") -> "grain"
            
            name.contains("dairy") || name.contains("milk") || name.contains("cheese") ||
            name.contains("yogurt") || name.contains("cream") -> "dairy"
            
            name.contains("nut") || name.contains("seed") || name.contains("almond") ||
            name.contains("peanut") || name.contains("walnut") -> "nuts"
            
            name.contains("oil") || name.contains("fat") -> "oil"
            
            else -> "unknown"
        }
    }
    
    private fun getNutritionByCategory(category: String, foodName: String): NutritionInfo {
        return when (category) {
            "fruit" -> NutritionInfo(60.0, 0.8, 15.0, 0.3, 2.5, 12.0)
            "vegetable" -> NutritionInfo(25.0, 2.0, 5.0, 0.2, 2.0, 2.5)
            "protein" -> NutritionInfo(200.0, 25.0, 0.0, 10.0, 0.0, 0.0)
            "grain" -> NutritionInfo(120.0, 4.0, 25.0, 1.0, 2.0, 1.0)
            "dairy" -> NutritionInfo(80.0, 5.0, 6.0, 4.0, 0.0, 4.0)
            "nuts" -> NutritionInfo(600.0, 20.0, 20.0, 55.0, 8.0, 4.0)
            "oil" -> NutritionInfo(884.0, 0.0, 0.0, 100.0, 0.0, 0.0)
            else -> NutritionInfo(100.0, 5.0, 15.0, 3.0, 2.0, 5.0)
        }
    }
    
    private fun getEstimatedNutritionInfo(foodName: String): NutritionInfo {
        // Provide reasonable estimates for unknown foods
        return NutritionInfo(100.0, 5.0, 15.0, 3.0, 2.0, 5.0)
    }
}

data class NutritionInfo(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double
)
