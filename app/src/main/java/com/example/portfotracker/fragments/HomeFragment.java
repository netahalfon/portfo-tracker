package com.example.portfotracker.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.portfotracker.adapters.StockAdapter;
import com.example.portfotracker.databinding.FragmentHomeBinding;
import com.example.portfotracker.models.Stock;
import com.example.portfotracker.services.YahooFinanceService;

import java.util.ArrayList;

import javax.annotation.Nullable;

public class HomeFragment extends Fragment {

    private static final String STOCK_LIST_KEY = "stock_list";
    private FragmentHomeBinding binding;
    private StockAdapter stockAdapter;
    private ArrayList<Stock> stockArrayList = new ArrayList<>();

    private String lastSubmitQuery = "";

    public HomeFragment() {}

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.stocksRecycler.setItemAnimator(new DefaultItemAnimator());
        binding.stocksRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        stockAdapter = new StockAdapter(stockArrayList);
        binding.stocksRecycler.setAdapter(stockAdapter);

        binding.searchEditText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadStockData(query);
                lastSubmitQuery = query;
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText.isEmpty()){
                    loadStockData("");
                }
                return false;
            }
        });
        if(lastSubmitQuery.isEmpty() && stockArrayList.isEmpty()){
            loadStockData("");
        }
        return binding.getRoot();
    }

    private void loadStockData(String searchTerm) {
        new Thread(() -> {
            try {
                stockArrayList = YahooFinanceService.getAllStocks(searchTerm); // קריאה ברקע
                requireActivity().runOnUiThread(() -> {
                    stockAdapter.updateStockList(stockArrayList);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}