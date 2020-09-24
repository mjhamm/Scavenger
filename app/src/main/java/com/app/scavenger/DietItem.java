package com.app.scavenger;

class DietItem {

    DietItem(String itemName, boolean checked) {
        this.itemName = itemName;
        this.checked = checked;
    }

    private String itemName;
    private boolean checked;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
