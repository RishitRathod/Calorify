package com.example.foodcalories.network

import android.util.Log
import com.example.foodcalories.data.FoodSearchResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.GET
import retrofit2.http.Query

interface USDAApiService {
    
    @GET("foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String = "lozK1aEb8uCSMRgyLqcaWSgSn4nlrgPhM3xhCdgO",
        @Query("query") query: String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy",
        @Query("pageSize") pageSize: Int = 25,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("sortBy") sortBy: String = "dataType.keyword",
        @Query("sortOrder") sortOrder: String = "asc",
        @Query("format") format: String = "full"
    ): FoodSearchResponse
}

object ApiClient {
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"
    
    fun createApiService(): USDAApiService {
        // Create logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("USDA_API", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Create OkHttpClient with logging
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        
        return retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(USDAApiService::class.java)
    }
}
