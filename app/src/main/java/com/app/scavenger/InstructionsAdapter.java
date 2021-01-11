package com.app.scavenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class InstructionsAdapter extends RecyclerView.Adapter<InstructionsAdapter.ViewHolder> {

    private final List<String> mData;
    private final LayoutInflater mInflater;

    // data is passed into the constructor
    InstructionsAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @NonNull
    @Override
    public InstructionsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_instructions_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.stepNumberText.setText(String.format(Locale.getDefault(), "STEP\n %d", (position + 1)));
        holder.instructionsText.setText(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView stepNumberText;
        final TextView instructionsText;

        ViewHolder(View itemView) {
            super(itemView);
            instructionsText = itemView.findViewById(R.id.instructions_textView);
            stepNumberText = itemView.findViewById(R.id.step_number_textView);
        }
    }
}

