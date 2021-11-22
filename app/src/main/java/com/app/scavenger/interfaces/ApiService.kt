package com.app.scavenger.interfaces

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

internal interface ApiService {
    // getting random recipes
    @GET("random?")
    fun getRandomRecipeData(
        @Query("apiKey") apiKey: String?,
        @Query("number") toIngr: Int
    ): Call<String?>?

    // getting recipes based on recipe search
    @GET("complexSearch?")
    fun getRecipeData(
        @Query("apiKey") apiKey: String?,
        @Query("query") ingredients: String?,
        @Query("addRecipeInformation") addInfo: Boolean,
        @Query("instructionsRequired") instrRequired: Boolean,
        @Query("offset") fromIngr: Int,
        @Query("number") toIngr: Int,
        @Query("limitLicense") limit: Boolean
    ): Call<String?>?

    // getting recipes based on ingredients
    @GET("complexSearch?")
    fun getRecipeDataIngr(
        @Query("apiKey") apiKey: String?,
        @Query("query") ingredients: String?,
        @Query("includeIngredients") includeIngr: String?,
        @Query("addRecipeInformation") addInfo: Boolean,
        @Query("instructionsRequired") instrRequired: Boolean,
        @Query("offset") fromIngr: Int,
        @Query("number") toIngr: Int,
        @Query("limitLicense") limit: Boolean
    ): Call<String?>?
}