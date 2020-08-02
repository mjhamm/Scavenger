package com.app.scavenger;

public class GroceryListItem {

    private String mGroceryItemName;

    public GroceryListItem(String groceryItemName) {
        mGroceryItemName = groceryItemName;
    }

    public String getGroceryItemName() {
        return mGroceryItemName;
    }

    public void setGroceryItemName(String mGroceryItemName) {
        this.mGroceryItemName = mGroceryItemName;
    }
}
