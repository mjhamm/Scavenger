package com.app.scavenger;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment {

    private RecyclerView mFavoriteRecyclerView;
    private ArrayList<RecipeItem> favoriteItems;
    private FavoriteAdapter adapter;
    private Context mContext;
    private SearchView mFavoriteSearch;
    private TextView favorite_message;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        favorite_message = view.findViewById(R.id.favorite_message);
        mFavoriteSearch = view.findViewById(R.id.favorites_searchView);
        mFavoriteSearch.setMaxWidth(Integer.MAX_VALUE);

        favorite_message.setText("You Don't Have Any Favorites :(");

        return view;
    }
}
