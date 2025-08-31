package com.example.foodcalories.data

import com.google.gson.annotations.SerializedName

data class FoodSearchResponse(
    @SerializedName("foods")
    val foods: List<FoodItem>?,
    @SerializedName("totalHits")
    val totalHits: Int?,
    @SerializedName("currentPage")
    val currentPage: Int?,
    @SerializedName("totalPages")
    val totalPages: Int?,
    @SerializedName("pageSize")
    val pageSize: Int?
)

data class FoodItem(
    @SerializedName("fdcId")
    val fdcId: Int?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("dataType")
    val dataType: String?,
    @SerializedName("publicationDate")
    val publicationDate: String?,
    @SerializedName("foodNutrients")
    val foodNutrients: List<FoodNutrient>?,
    @SerializedName("brandOwner")
    val brandOwner: String?,
    @SerializedName("brandName")
    val brandName: String?,
    @SerializedName("ingredients")
    val ingredients: String?
)

data class FoodNutrient(
    @SerializedName("number")
    val number: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("unitName")
    val unitName: String?,
    @SerializedName("derivationCode")
    val derivationCode: String?,
    @SerializedName("derivationDescription")
    val derivationDescription: String?
)

data class FoodInfo(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val description: String,
    val brand: String? = null,
    val fdcId: Int? = null
)
