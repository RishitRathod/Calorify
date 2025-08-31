package com.example.foodcalories

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodcalories.adapter.FoodAdapter
import com.example.foodcalories.data.FoodInfo
import com.example.foodcalories.databinding.ActivityMainBinding
import com.example.foodcalories.databinding.DialogCalorieGoalBinding
import com.example.foodcalories.repository.FoodRepository
import com.example.foodcalories.util.PreferencesManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var foodAdapter: FoodAdapter
    private val foodRepository = FoodRepository()
    private lateinit var preferencesManager: PreferencesManager
    private var calorieGoal: Int = 0
    
    // Camera activity launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val detectedItem = result.data?.getStringExtra("detected_item")
            val confidence = result.data?.getFloatExtra("confidence", 0f) ?: 0f
            
            if (!detectedItem.isNullOrEmpty()) {
                // Search for the detected item
                binding.searchEditText.setText(detectedItem)
                searchFoods(detectedItem)
                Toast.makeText(this, "Detected: $detectedItem (${(confidence * 100).toInt()}% confidence)", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupRecyclerView()
        setupSearchBar()
        setupCameraButton()
        setupAboutButton()
        
        // Set default calorie goal without showing dialog
        if (preferencesManager.hasCalorieGoal()) {
            calorieGoal = preferencesManager.getCalorieGoal()
        } else {
            // Set default calorie goal (2000 calories for average adult)
            calorieGoal = 2000
            preferencesManager.saveCalorieGoal(calorieGoal)
        }
        foodAdapter.setCalorieGoal(calorieGoal)
    }
    
    private fun setupRecyclerView() {
        foodAdapter = FoodAdapter(emptyList()) { food ->
            // Launch NutritionDetailActivity instead of showing dialog
            val intent = Intent(this, NutritionDetailActivity::class.java).apply {
                putExtra("food_name", food.name)
            }
            startActivity(intent)
        }
        
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = foodAdapter
        }
    }
    
    private fun setupSearchBar() {
        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                searchFoods(query)
            }
            true
        }
        
        // Add text watcher to handle placeholder behavior
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Clear hint when text is entered
                if (!s.isNullOrEmpty()) {
                    binding.searchEditText.hint = ""
                } else {
                    // Restore hint when text is cleared
                    binding.searchEditText.hint = getString(R.string.search_hint)
                }
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    
    private fun setupCameraButton() {
        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent)
        }
    }
    
    private fun setupAboutButton() {
        binding.aboutButton.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun searchFoods(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                showLoading(true)
                showError(false)
                
                val foods = withContext(Dispatchers.IO) {
                    foodRepository.searchFoods(query)
                }
                
                showLoading(false)
                
                if (foods.isEmpty()) {
                    showNoResults()
                } else {
                    showResults(foods)
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Search error", e)
                showLoading(false)
                showError(true)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.resultsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showError(show: Boolean) {
        binding.errorLayout.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showNoResults() {
        binding.resultsRecyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorText.text = getString(R.string.no_results)
    }
    
    private fun showResults(foods: List<FoodInfo>) {
        binding.resultsRecyclerView.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        foodAdapter.updateFoods(foods)
    }
    
    private fun showCalorieGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_calorie_goal, null)
        val calorieGoalEditText = dialogView.findViewById<TextInputEditText>(R.id.calorieGoalEditText)
        
        // Add text watcher to handle placeholder behavior
        calorieGoalEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Clear hint when text is entered
                if (!s.isNullOrEmpty()) {
                    calorieGoalEditText.hint = ""
                } else {
                    // Restore hint when text is cleared
                    calorieGoalEditText.hint = getString(R.string.calorie_goal_hint)
                }
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // Set button click listeners
        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val goalText = calorieGoalEditText.text.toString()
            if (goalText.isNotEmpty()) {
                try {
                    calorieGoal = goalText.toInt()
                    preferencesManager.saveCalorieGoal(calorieGoal)
                    foodAdapter.setCalorieGoal(calorieGoal)
                    // Force refresh the adapter to show recommendations
                    val currentFoods = foodAdapter.getCurrentFoods()
                    if (currentFoods.isNotEmpty()) {
                        foodAdapter.updateFoods(currentFoods)
                    }
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter your calorie goal", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            // Set a default value if user cancels
            calorieGoal = 200 // Default average adult calorie intake
            preferencesManager.saveCalorieGoal(calorieGoal)
            foodAdapter.setCalorieGoal(calorieGoal)
            // Force refresh the adapter to show recommendations
            val currentFoods = foodAdapter.getCurrentFoods()
            if (currentFoods.isNotEmpty()) {
                foodAdapter.updateFoods(currentFoods)
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
