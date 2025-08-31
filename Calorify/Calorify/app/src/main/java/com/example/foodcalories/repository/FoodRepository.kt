package com.example.foodcalories.repository

import android.util.Log
import com.example.foodcalories.data.FoodInfo
import com.example.foodcalories.data.FoodItem
import com.example.foodcalories.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoodRepository {
    
    private val apiService = ApiClient.createApiService()
    
    suspend fun searchFoods(query: String): List<FoodInfo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchFoods(query = query)
            Log.d("FoodRepository", "API Response: ${response.foods?.size} foods found")
            
            // Log the complete response structure for debugging
            response.foods?.take(1)?.forEach { food ->
                Log.d("FoodRepository", "=== FOOD ITEM DEBUG ===")
                Log.d("FoodRepository", "Name: ${food.description}")
                Log.d("FoodRepository", "FDC ID: ${food.fdcId}")
                Log.d("FoodRepository", "Data Type: ${food.dataType}")
                Log.d("FoodRepository", "Brand: ${food.brandName}")
                Log.d("FoodRepository", "Nutrients count: ${food.foodNutrients?.size}")
                
                food.foodNutrients?.forEachIndexed { index, nutrient ->
                    Log.d("FoodRepository", "Nutrient $index: ${nutrient.name} (${nutrient.number}) = ${nutrient.amount} ${nutrient.unitName}")
                }
                Log.d("FoodRepository", "=== END DEBUG ===")
            }
            
            response.foods?.mapNotNull { foodItem ->
                convertToFoodInfo(foodItem)
            }?.let { allFoods ->
                // Filter to show only the main fruit/vegetable, not variations
                filterMainFoods(allFoods, query)
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("FoodRepository", "API Error", e)
            throw e
        }
    }
    
    private fun filterMainFoods(foods: List<FoodInfo>, query: String): List<FoodInfo> {
        val normalizedQuery = query.lowercase().trim()
        
        // Define main food categories and their variations to filter out
        val mainFoods = mapOf(
            "apple" to listOf("apple", "apples"),
            "banana" to listOf("banana", "bananas"),
            "orange" to listOf("orange", "oranges"),
            "tomato" to listOf("tomato", "tomatoes"),
            "carrot" to listOf("carrot", "carrots"),
            "broccoli" to listOf("broccoli"),
            "cucumber" to listOf("cucumber", "cucumbers"),
            "potato" to listOf("potato", "potatoes"),
            "onion" to listOf("onion", "onions"),
            "lettuce" to listOf("lettuce"),
            "spinach" to listOf("spinach"),
            "strawberry" to listOf("strawberry", "strawberries"),
            "grape" to listOf("grape", "grapes"),
            "mango" to listOf("mango", "mangos"),
            "pineapple" to listOf("pineapple", "pineapples"),
            "peach" to listOf("peach", "peaches"),
            "pear" to listOf("pear", "pears"),
            "watermelon" to listOf("watermelon"),
            "corn" to listOf("corn", "sweet corn"),
            "rice" to listOf("rice"),
            "bread" to listOf("bread"),
            "pasta" to listOf("pasta"),
            "chicken" to listOf("chicken"),
            "beef" to listOf("beef"),
            "fish" to listOf("fish"),
            "milk" to listOf("milk"),
            "cheese" to listOf("cheese"),
            "yogurt" to listOf("yogurt"),
            "almond" to listOf("almond", "almonds"),
            "peanut" to listOf("peanut", "peanuts"),
            "walnut" to listOf("walnut", "walnuts"),
            "olive oil" to listOf("olive oil"),
            "corn oil" to listOf("corn oil"),
            "vegetable oil" to listOf("vegetable oil")
        )
        
        // Find the main food category for the query
        val mainFoodCategory = mainFoods.entries.find { (key, variations) ->
            variations.any { variation -> normalizedQuery.contains(variation) }
        }?.key
        
        if (mainFoodCategory != null) {
            // Filter foods to show only the main food, not variations
            return foods.filter { food ->
                val foodName = food.name.lowercase()
                val isMainFood = mainFoods[mainFoodCategory]?.any { variation ->
                    foodName.contains(variation, ignoreCase = true)
                } ?: false
                
                // Exclude processed forms, brands, and variations
                val excludeKeywords = listOf(
                    "fuji", "gala", "red delicious", "granny smith", // Apple varieties
                    "canned", "frozen", "dried", "juice", "sauce", "paste",
                    "organic", "conventional", "fresh", "raw", "cooked",
                    "with skin", "without skin", "peeled", "unpeeled",
                    "sliced", "chopped", "diced", "whole", "pieces",
                    "baby", "large", "small", "medium", "extra large",
                    "brand", "company", "inc", "llc", "corp"
                )
                
                val shouldExclude = excludeKeywords.any { keyword ->
                    foodName.contains(keyword, ignoreCase = true)
                }
                
                isMainFood && !shouldExclude
            }.take(3) // Limit to top 3 results
        }
        
        // If no main food category found, return filtered results
        return foods.filter { food ->
            val foodName = food.name.lowercase()
            val excludeKeywords = listOf(
                "canned", "frozen", "dried", "juice", "sauce", "paste",
                "organic", "conventional", "fresh", "raw", "cooked",
                "with skin", "without skin", "peeled", "unpeeled",
                "sliced", "chopped", "diced", "whole", "pieces",
                "baby", "large", "small", "medium", "extra large",
                "brand", "company", "inc", "llc", "corp"
            )
            
            !excludeKeywords.any { keyword ->
                foodName.contains(keyword, ignoreCase = true)
            }
        }.take(3)
    }
    
    private fun convertToFoodInfo(foodItem: FoodItem): FoodInfo? {
        val name = foodItem.description ?: return null
        val nutrients = foodItem.foodNutrients ?: emptyList()
        
        Log.d("FoodRepository", "Processing food: $name")
        Log.d("FoodRepository", "Nutrients count: ${nutrients.size}")
        
        // Log all nutrients for debugging
        nutrients.forEach { nutrient ->
            Log.d("FoodRepository", "Nutrient: ${nutrient.name} (${nutrient.number}) = ${nutrient.amount} ${nutrient.unitName}")
        }
        
        // Try multiple approaches to find nutrition data
        val calories = findCalories(nutrients)
        val protein = findProtein(nutrients)
        val carbs = findCarbs(nutrients)
        val fat = findFat(nutrients)
        val fiber = findFiber(nutrients)
        val sugar = findSugar(nutrients)
        
        Log.d("FoodRepository", "Extracted values - Calories: $calories, Protein: $protein, Carbs: $carbs, Fat: $fat, Fiber: $fiber, Sugar: $sugar")
        
        val description = buildDescription(foodItem)
        val brand = foodItem.brandName ?: foodItem.brandOwner
        
        return FoodInfo(
            name = name,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            description = description,
            brand = brand,
            fdcId = foodItem.fdcId
        )
    }
    
    private fun findCalories(nutrients: List<com.example.foodcalories.data.FoodNutrient>): Double {
        val calorieKeywords = listOf("Energy", "Calories", "ENERC_KCAL", "ENERC")
        val calorieCodes = listOf("208", "957")
        
        return findNutrient(nutrients, calorieKeywords, calorieCodes)
    }
    
    private fun findProtein(nutrients: List<com.example.foodcalories.data.FoodNutrient>): Double {
        val proteinKeywords = listOf("Protein", "PROCNT", "PROT")
        val proteinCodes = listOf("203")
        
        return findNutrient(nutrients, proteinKeywords, proteinCodes)
    }
    
    private fun findCarbs(nutrients: List<com.example.foodcalories.data.FoodNutrient>): Double {
        val carbKeywords = listOf("Carbohydrate, by difference", "Total carbohydrate", "CHOCDF", "Carbohydrate")
        val carbCodes = listOf("205")
        
        return findNutrient(nutrients, carbKeywords, carbCodes)
    }
    
    private fun findFat(nutrients: List<com.example.foodcalories.data.FoodNutrient>): Double {
        val fatKeywords = listOf("Total lipid (fat)", "Fat", "FAT", "Total fat")
        val fatCodes = listOf("204")
        
        return findNutrient(nutrients, fatKeywords, fatCodes)
    }
    
    private fun findFiber(nutrients: List<com.example.foodcalories.data.FoodNutrient>): Double {
        val fiberKeywords = listOf("Fiber, total dietary", "Dietary fiber", "FIBTG", "Fiber")
        val fiberCodes = listOf("291")
        
        return findNutrient(nutrients, fiberKeywords, fiberCodes)
    }
    
    private fun findSugar(nutrients: List<com.example.foodcalories.data.FoodNutrient>): Double {
        val sugarKeywords = listOf("Sugars, total including NLEA", "Sugars, Total", "SUGAR", "Total sugars")
        val sugarCodes = listOf("269")
        
        return findNutrient(nutrients, sugarKeywords, sugarCodes)
    }
    
    private fun findNutrient(nutrients: List<com.example.foodcalories.data.FoodNutrient>, keywords: List<String>, codes: List<String>): Double {
        // First try to find by name/keywords
        val foundByName = nutrients.find { nutrient ->
            keywords.any { keyword -> 
                nutrient.name?.contains(keyword, ignoreCase = true) == true 
            }
        }
        
        if (foundByName != null) {
            Log.d("FoodRepository", "Found by name: ${foundByName.name} = ${foundByName.amount}")
            return foundByName.amount ?: 0.0
        }
        
        // Then try to find by nutrient code
        val foundByCode = nutrients.find { nutrient ->
            codes.contains(nutrient.number)
        }
        
        if (foundByCode != null) {
            Log.d("FoodRepository", "Found by code: ${foundByCode.name} (${foundByCode.number}) = ${foundByCode.amount}")
            return foundByCode.amount ?: 0.0
        }
        
        Log.d("FoodRepository", "Not found for keywords: $keywords, codes: $codes")
        return 0.0
    }
    
    private fun buildDescription(foodItem: FoodItem): String {
        val parts = mutableListOf<String>()
        
        if (foodItem.brandName != null) {
            parts.add("Brand: ${foodItem.brandName}")
        }
        
        // Removed the dataType display as requested
        
        if (foodItem.ingredients != null && foodItem.ingredients.isNotBlank()) {
            parts.add("Ingredients: ${foodItem.ingredients}")
        }
        
        return parts.joinToString(" • ")
    }
}
