package com.app.scavenger;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private final List<CommentItem> mCommentArray;
    private final LayoutInflater mInflater;
    private final String recipeName, recipeSource;
    private final int recipeId;
    private final FirebaseAuth mAuth;
    private final Context mContext;
    private String reportReason;
    private final ConnectionDetector con;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // data is passed into the constructor
    CommentsAdapter(Context context, List<CommentItem> commentArray, int recipeId, String recipeName, String recipeSource) {
        this.mInflater = LayoutInflater.from(context);
        this.mCommentArray = commentArray;
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.recipeSource = recipeSource;
        this.mContext = context;
        con = new ConnectionDetector(mContext);
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public CommentsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_comment, parent, false);
        return new CommentsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentsAdapter.ViewHolder holder, int position) {
        CommentItem item = mCommentArray.get(position);
        holder.mName.setText(item.getName());
        holder.mDetail.setText(item.getDetail());
    }

    @Override
    public int getItemCount() {
        return mCommentArray == null ? 0 : mCommentArray.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mDetail, mName;
        final ImageButton mMoreButton;

        ViewHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.comment_name);
            mDetail = itemView.findViewById(R.id.comment_detail);
            mMoreButton = itemView.findViewById(R.id.comment_more);

            mMoreButton.setOnClickListener(v -> {
                CommentItem commentItem = mCommentArray.get(getAdapterPosition());

                // create popup to show report option
                PopupMenu popupMenu = new PopupMenu(mContext, mMoreButton);
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.comment_report) {
                        reportComment(commentItem.getName(), commentItem.getDetail());
                        return true;
                    }
                    return false;
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.comment_report_menu, popupMenu.getMenu());
                popupMenu.show();
            });
        }

        private void reportComment(String name, String detail) {
            reportReason = "Spam";
            final CharSequence[] listItems = {"Spam","Inappropriate"};
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle("Why are you reporting this?")
                    .setSingleChoiceItems(listItems, 0, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                reportReason = "Spam";
                                break;
                            case 1:
                                reportReason = "Inappropriate";
                                break;
                        }
                    })
                    .setPositiveButton("Report",(dialog, which) -> {
                        if (!con.connectedToInternet()) {
                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(Constants.noInternetTitle)
                                    .setMessage(Constants.noInternetMessage)
                                    .setPositiveButton("OK", (dialog1, which1) -> dialog1.dismiss())
                                    .create()
                                    .show();
                        } else {
                            sendCommentReport(recipeId, name, detail, recipeName, recipeSource);
                        }
                    })
                    .setNegativeButton("Cancel", ((dialog, which) -> dialog.cancel()))
                    .create()
                    .show();
        }

        // Send report to Server under reports with Phone information
        private void sendCommentReport(int recipeId, String name, String detail, String recipeName, String recipeSource) {
            String userId;

            if (mAuth.getCurrentUser() != null) {
                userId = mAuth.getCurrentUser().getUid();
            } else {
                userId = "Not Signed In";
                name = "N/A";
            }

            // Date information
            Calendar calendar = Calendar.getInstance();
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int year = calendar.get(Calendar.YEAR);
            calendar.clear();
            calendar.set(year, month, day);
            Date now = new Date();
            Timestamp timestamp = new Timestamp(now);

            HashMap<String, Object> reportCommentInfo = new HashMap<>();

            CollectionReference reportingReference = db.collection(Constants.firebaseCommentReports).document(String.valueOf(recipeId)).collection(Constants.firebaseReports);

            reportCommentInfo.put("Report", reportReason);
            reportCommentInfo.put("Timestamp", timestamp);
            reportCommentInfo.put("User Id", userId);
            reportCommentInfo.put("Recipe Name", recipeName);
            reportCommentInfo.put("Recipe Source", recipeSource);
            reportCommentInfo.put("name",name);
            reportCommentInfo.put("detail", detail);

            reportingReference.document().set(reportCommentInfo)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("COMMENTSADAPTER","Report saved to Firebase");
                        //toastMessage("Thank you for your report");
                    })
                    .addOnFailureListener(e -> {
                        //toastMessage("Error sending report");
                        Log.d("COMMENTSADAPTER", e.toString());
                    });
        }
    }

}
