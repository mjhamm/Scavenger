package com.app.scavenger;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;

import com.google.firebase.firestore.Exclude;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

class RecipeItem implements Parcelable {

    protected RecipeItem(Parcel in) {
        itemType = in.readInt();
        itemId = in.readInt();
        itemRating = in.readFloat();
        mIngredients = in.createStringArrayList();
        mImageUrl = in.readString();
        mRecipeName = in.readString();
        mSourceName = in.readString();
        mRecipeURL = in.readString();
        liked = in.readByte() != 0;
    }

    public static final Creator<RecipeItem> CREATOR = new Creator<RecipeItem>() {
        @Override
        public RecipeItem createFromParcel(Parcel in) {
            return new RecipeItem(in);
        }

        @Override
        public RecipeItem[] newArray(int size) {
            return new RecipeItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(itemType);
        dest.writeInt(itemId);
        dest.writeFloat(itemRating);
        dest.writeStringList(mIngredients);
        dest.writeString(mImageUrl);
        dest.writeString(mRecipeName);
        dest.writeString(mSourceName);
        dest.writeString(mRecipeURL);
        dest.writeByte((byte) (liked ? 1 : 0));
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_HEADER, TYPE_ITEM})

    public @interface ItemType {}

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    //Empty Constructor
    RecipeItem(@ItemType int itemType) {
        this.itemType = itemType;
    }

    private final int itemType;

    // testing
    //private String itemId;
    @Expose
    @SerializedName("id")
    private int itemId;

    private float itemRating;
    //private int itemRating;

    /*@Expose
    @SerializedName("uri")
    private String mRecipeUri;

    @Expose
    @SerializedName("dietLabels")
    private ArrayList<String> mRecipeAttributes;*/

    @Expose
    @SerializedName("ingredients")
    private ArrayList<String> mIngredients;

    @Expose
    @SerializedName("image")
    private String mImageUrl;

    @Expose
//    @SerializedName("label")
    @SerializedName("title")
    private String mRecipeName;

    @Expose
    @SerializedName("sourceName")
//    @SerializedName("source")
    private String mSourceName;

    /*@Expose
    @SerializedName("CHOCDF")
    private int mCarbs;

    @Expose
    @SerializedName("FAT")
    private int mFat;

    @Expose
    @SerializedName("PROCNT")
    private int mProtein;*/

    @Expose
//    @SerializedName("url")
    @SerializedName("sourceUrl")
    private String mRecipeURL;

    /*@Expose
    @SerializedName("yield")
    private int mServings;

    @Expose
    @SerializedName("calories")
    private int mCalories;*/

    private boolean liked;

    @Exclude
    int getItemId() {
        return itemId;
    }

    void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemType() { return itemType; }

    float getItemRating() {
        return itemRating;
    }

    void setItemRating(float itemRating) {
        this.itemRating = itemRating;
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

    boolean isLiked() {
        return liked;
    }

    void setLiked(boolean liked) {
        this.liked = liked;
    }

    ArrayList<String> getmIngredients() {
        return mIngredients;
    }

    String getmRecipeURL() {
        return mRecipeURL;
    }

    void setmRecipeURL(String mRecipeURL) {
        this.mRecipeURL = mRecipeURL;
    }

    // testing
    /*@Exclude
    String getItemId() {
        return itemId;
    }

    void setItemId(String itemId) {
        this.itemId = itemId;
    }

    // testing
    int getItemRating() {
        return itemRating;
    }

    void setItemRating(int itemRating) {
        this.itemRating = itemRating;
    }

    void setItemUri(String mRecipeUri) {
        this.mRecipeUri = mRecipeUri;
    }

    String getItemUri() {
        return mRecipeUri;
    }

    ArrayList<String> getmRecipeAttributes() {
        return mRecipeAttributes;
    }

    void setmRecipeAttributes(ArrayList<String> mRecipeAttributes) {
        this.mRecipeAttributes = mRecipeAttributes;
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
    }*/
}
