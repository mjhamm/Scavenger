package com.app.scavenger;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

import static android.view.View.GONE;

public class SearchFragment extends Fragment {

    private RecyclerView mSearchRecyclerView;
    private ArrayList<RecipeItem> recipeItems;
    private SearchAdapter adapter;
    private Context mContext;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        SearchView mSearchView = view.findViewById(R.id.search_searchView);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        //recipeItems = new ArrayList<>();
        recipeItems = RecipeItem.createContactsList(20);

        mSearchRecyclerView = view.findViewById(R.id.search_recyclerView);
        mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mSearchRecyclerView.setHasFixedSize(true);

        adapter = new SearchAdapter(mContext,recipeItems);
        mSearchRecyclerView.setAdapter(adapter);

        return view;
    }
}
