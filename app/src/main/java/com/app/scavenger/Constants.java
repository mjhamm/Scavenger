package com.app.scavenger;

public class Constants {

//    Edamam Key Information
//    public static final String appId = "bd790cc2";
//    public static final String appKey = "56fdf6a5593ad5199e8040a29b9fbfd6";

    public static final String apiKey = "1e479374e15a4bf2981d5a78098381b1";

    // Common String Information
    public static final String firebaseUser = "Users";
    public static final String firebaseLikes = "Likes";
    public static final String firebaseComments = "Comments";
    //public static final String firebaseGrocery = "GroceryList";
    //public static final String firebaseIngrListId = "VCHP0Xze7IyETG4NT6at";
    public static final String firebaseRecipeReports = "RecipeReports";
    public static final String firebaseCommentReports = "CommentReports";
    public static final String firebaseReports = "reports";
    public static final String firebaseTime = "Timestamp";

    public static final String scavengerTermsURL = "https://www.thescavengerapp.com/terms-and-conditions";
    public static final String scavengerPrivacyURL = "https://www.thescavengerapp.com/privacy-policy";
    public static final String scavengerHelpURL = "https://www.thescavengerapp.com/help";
    public static final String scavengerBaseURL = "https://www.thescavengerapp.com/";

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
    public static final String nutritionInformation = "Scavenger uses your search criteria to look throughout the Internet in order to bring you " +
            "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
            "the View Recipe button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information, " +
            "Number of Servings as well as the Steps for the recipe.";

    // Firebase Constants --------------------------------------------------------------

    public static final String ITEM_ID = "itemId";
    public static final String ITEM_NAME = "name";
    public static final String ITEM_SOURCE = "source";
    public static final String ITEM_IMAGE = "image";
    public static final String ITEM_URL = "url";
    public static final String COMMENT_NAME = "Comment Name";
    public static final String COMMENT_DETAIL = "Comment Text";
    public static final String COMMENT_USERID = "User Id";
}
