package com.app.scavenger;

public class Constants {

    // Edamam Key Information
    public static final String appId = "bd790cc2";
    public static final String appKey = "56fdf6a5593ad5199e8040a29b9fbfd6";

    // Common String Information
    public static final String firebaseUser = "Users";
    public static final String firebaseLikes = "Likes";
    public static final String firebaseRecipeReports = "RecipeReports";
    public static final String firebaseReports = "reports";
    public static final String firebaseTime = "Timestamp";

    // Internet Error Messages
    public static final String noInternetTitle = "No Internet connection found";
    public static final String noInternetMessage = "You don't have an Internet connection. Please reconnect and try again.";

    // Account Information ------------------------------------------------------------

    // Account Messages
    public static final String accountNotFound = "Account not found";
    public static final String accountNotFoundMessage = "The email that you have entered does not belong to an account. If this issue persists, please contact support at support@theScavengerApp.com";
    public static final String resetPassTitle = "Reset password email sent";
    public static final String resetPassMessage = "You will receive reset instructions in 2-5 minutes.";

    // Card Information ---------------------------------------------------------------

    // Nutrition Data
    public static final String nutritionInformationTitle = "Some information about our data";
    public static final String nutritionInformation = "Scavenger uses Edamam Search and your search criteria to look throughout the Internet in order to bring you " +
            "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
            "the View Recipe button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information " +
            "as well as the number of servings the amount of ingredients can make.";

    // Firebase Constants --------------------------------------------------------------

    public static final String ITEM_ID = "itemId";
    public static final String ITEM_NAME = "name";
    public static final String ITEM_SOURCE = "source";
    public static final String ITEM_IMAGE = "image";
    public static final String ITEM_URL = "url";
    public static final String ITEM_YIELD = "servings";
    public static final String ITEM_CAL = "calories";
    public static final String ITEM_CARB = "carbs";
    public static final String ITEM_FAT = "fat";
    public static final String ITEM_PROTEIN = "protein";
    public static final String ITEM_ATT = "attributes";
    public static final String ITEM_INGR = "ingredients";
}
