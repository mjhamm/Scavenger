package com.app.scavenger;

import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

class RecipeItem {

    //Empty Constructor
    public RecipeItem() {}

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
    @SerializedName("uri")
    private String mUniqueURI;

    @Expose
    @SerializedName("yield")
    private int mServings;

    @Expose
    @SerializedName("calories")
    private int mCalories;

    private boolean clicked;
    private boolean favorited;

    @Exclude
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setmImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }

    public void setmRecipeName(String mRecipeName) {
        this.mRecipeName = mRecipeName;
    }

    public void setmSourceName(String mSourceName) {
        this.mSourceName = mSourceName;
    }

    public String getmImageUrl() {
        return mImageUrl;
    }

    public String getmRecipeName() {
        return mRecipeName;
    }

    public String getmSourceName() {
        return mSourceName;
    }

    boolean isClicked() {
        return clicked;
    }

    void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    boolean isFavorited() {
        return favorited;
    }

    void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public ArrayList<String> getmRecipeAttributes() {
        return mRecipeAttributes;
    }

    public void setmRecipeAttributes(ArrayList<String> mRecipeAttributes) {
        this.mRecipeAttributes = mRecipeAttributes;
    }

    public ArrayList<String> getmIngredients() {
        return mIngredients;
    }

    public void setmIngredients(ArrayList<String> mIngredients) {
        this.mIngredients = mIngredients;
    }

    public int getmCarbs() {
        return mCarbs;
    }

    public void setmCarbs(int mCarbs) {
        this.mCarbs = mCarbs;
    }

    public int getmFat() {
        return mFat;
    }

    public void setmFat(int mFat) {
        this.mFat = mFat;
    }

    public int getmProtein() {
        return mProtein;
    }

    public void setmProtein(int mProtein) {
        this.mProtein = mProtein;
    }

    public String getmRecipeURL() {
        return mRecipeURL;
    }

    public void setmRecipeURL(String mRecipeURL) {
        this.mRecipeURL = mRecipeURL;
    }

    public String getmUniqueURI() {
        return mUniqueURI;
    }

    public void setmUniqueURI(String mUniqueURI) {
        this.mUniqueURI = mUniqueURI;
    }

    public int getmServings() {
        return mServings;
    }

    public void setmServings(int mServings) {
        this.mServings = mServings;
    }

    public int getmCalories() {
        return mCalories;
    }

    public void setmCalories(int mCalories) {
        this.mCalories = mCalories;
    }
}
