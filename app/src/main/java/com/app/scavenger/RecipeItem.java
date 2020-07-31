package com.app.scavenger;

import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

class RecipeItem {

    //Empty Constructor
    RecipeItem() {}

    private String itemId;

    @Expose
    @SerializedName("dietLabels")
    private ArrayList<String> mRecipeAttributes;

    @Expose
    @SerializedName("ingredients")
    private ArrayList<String> mIngredients;

    @Expose
    @SerializedName("image")
    private String mImageUrl;

    @Expose
    @SerializedName("label")
    private String mRecipeName;

    @Expose
    @SerializedName("source")
    private String mSourceName;

    @Expose
    @SerializedName("CHOCDF")
    private int mCarbs;

    @Expose
    @SerializedName("FAT")
    private int mFat;

    @Expose
    @SerializedName("PROCNT")
    private int mProtein;

    @Expose
    @SerializedName("url")
    private String mRecipeURL;

    @Expose
    @SerializedName("yield")
    private int mServings;

    @Expose
    @SerializedName("calories")
    private int mCalories;

    private boolean clicked;
    private boolean liked;

    @Exclude
    String getItemId() {
        return itemId;
    }

    void setItemId(String itemId) {
        this.itemId = itemId;
    }

    void setmImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }

    void setmRecipeName(String mRecipeName) {
        this.mRecipeName = mRecipeName;
    }

    void setmSourceName(String mSourceName) {
        this.mSourceName = mSourceName;
    }

    String getmImageUrl() {
        return mImageUrl;
    }

    String getmRecipeName() {
        return mRecipeName;
    }

    String getmSourceName() {
        return mSourceName;
    }

    boolean isClicked() {
        return clicked;
    }

    void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    boolean isLiked() {
        return liked;
    }

    void setLiked(boolean liked) {
        this.liked = liked;
    }

    ArrayList<String> getmRecipeAttributes() {
        return mRecipeAttributes;
    }

    void setmRecipeAttributes(ArrayList<String> mRecipeAttributes) { this.mRecipeAttributes = mRecipeAttributes; }

    ArrayList<String> getmIngredients() {
        return mIngredients;
    }

    void setmIngredients(ArrayList<String> mIngredients) {
        this.mIngredients = mIngredients;
    }

    int getmCarbs() {
        return mCarbs;
    }

    void setmCarbs(int mCarbs) {
        this.mCarbs = mCarbs;
    }

    int getmFat() {
        return mFat;
    }

    void setmFat(int mFat) {
        this.mFat = mFat;
    }

    int getmProtein() {
        return mProtein;
    }

    void setmProtein(int mProtein) {
        this.mProtein = mProtein;
    }

    String getmRecipeURL() {
        return mRecipeURL;
    }

    void setmRecipeURL(String mRecipeURL) {
        this.mRecipeURL = mRecipeURL;
    }

    int getmServings() {
        return mServings;
    }

    void setmServings(int mServings) {
        this.mServings = mServings;
    }

    int getmCalories() {
        return mCalories;
    }

    void setmCalories(int mCalories) {
        this.mCalories = mCalories;
    }
}
