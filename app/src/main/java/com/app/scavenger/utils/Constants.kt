package com.app.scavenger.utils

class Constants {

    companion object {
        const val apiKey = "1e479374e15a4bf2981d5a78098381b1"
        const val firebaseUser = "Users"
        const val firebaseLikes = "Likes"
        const val firebaseComments = "Comments"
        const val firebaseRecipeReports = "RecipeReports"
        const val firebaseCommentReports = "CommentReports"
        const val firebaseReports = "reports"
        const val firebaseTime = "Timestamp"

        const val scavengerTermsURL = "https://www.thescavengerapp.com/terms-and-conditions"
        const val scavengerPrivacyURL = "https://www.thescavengerapp.com/privacy-policy"
        const val scavengerHelpURL = "https://www.thescavengerapp.com/help"
        const val scavengerBaseURL = "https://www.thescavengerapp.com/"

        const val noInternetTitle = "No Internet connection found"
        const val noInternetMessage = "You don't have an Internet connection. Please reconnect and try again."

        const val accountNotFound = "Account not found"
        const val accountNotFoundMessage = "The email that you have entered does not belong to an account. If this issue persists, please contact support at support@theScavengerApp.com"
        const val resetPassTitle = "Reset password email sent"
        const val resetPassMessage = "You will receive reset instructions in 2-5 minutes."

        const val nutritionInformationTitle = "Some information about our data"
        const val nutritionInformation = "Scavenger uses your search criteria to look throughout the Internet in order to bring you " +
                "the best information we can find. However, sometimes this information may not be 100% accurate. Using " +
                "the View Instructions or View Recipe Website button to see the recipe on the actual website will give you the most accurate data. This includes Nutrition Information, " +
                "Number of Servings as well as the Steps for the recipe."

        const val ITEM_ID = "itemId"
        const val ITEM_NAME = "name"
        const val ITEM_SOURCE = "source"
        const val ITEM_IMAGE = "image"
        const val ITEM_URL = "url"
        const val COMMENT_NAME = "Comment Name"
        const val COMMENT_DETAIL = "Comment Text"
        const val COMMENT_USERID = "User Id"
        const val COMMENTS = "comments"
    }
}