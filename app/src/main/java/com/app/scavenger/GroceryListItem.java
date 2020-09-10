package com.app.scavenger;

public class GroceryListItem {

    private String mGroceryItemName;
    private boolean mTapped;
    private boolean showSelectItems;
    private boolean mSelected;

    public GroceryListItem() {}

    public String getGroceryItemName() {
        return mGroceryItemName;
    }

    public void setGroceryItemName(String mGroceryItemName) {
        this.mGroceryItemName = mGroceryItemName;
    }

    public boolean getGroceryItemTapped() {
        return mTapped;
    }

    public void setmGroceryItemTapped(boolean tapped) {
        this.mTapped = tapped;
    }

    public boolean getmGroceryItemSelected() {
        return mSelected;
    }

    public void setmGroceryItemSelected(boolean selected) {
        this.mSelected = selected;
    }

    public boolean isShowSelectItems() { return showSelectItems; }

    public void setShowSelectItems(boolean showSelectItems) { this.showSelectItems = showSelectItems; }
}
