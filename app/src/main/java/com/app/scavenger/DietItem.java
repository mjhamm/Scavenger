package com.app.scavenger;

class DietItem {

    DietItem(String itemName) {
        this.itemName = itemName;
        this.checked = false;
    }

    private final String itemName;
    private boolean checked;

    public String getItemName() {
        return itemName;
    }

// --Commented out by Inspection START (11/10/2020 10:22 AM):
//    public void setItemName(String itemName) {
//        this.itemName = itemName;
//    }
// --Commented out by Inspection STOP (11/10/2020 10:22 AM)

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
