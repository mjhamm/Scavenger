package com.app.scavenger;

import java.util.ArrayList;

class RecipeItem {

    private String mImageUrl;
    private String mRecipeName;
    private String mSourceName;
    private boolean clicked;
    private boolean favorited;

    //Constructor
    public RecipeItem(String imageUrl, String recipeName, String sourceName, boolean clicked, boolean favorited) {
        mImageUrl = imageUrl;
        mRecipeName = recipeName;
        mSourceName = sourceName;
        this.clicked = clicked;
        this.favorited = favorited;
    }

    public static int lastRecipeId = 0;

    public static ArrayList<RecipeItem> createContactsList(int numContacts) {
        ArrayList<RecipeItem> contacts = new ArrayList<>();

        for (int i = 1; i <= numContacts; i++) {
            contacts.add(new RecipeItem("Image", "Test Recipe "
                    + ++lastRecipeId, "Test Recipe Source " + lastRecipeId, false, false));
        }

        return contacts;
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
}
