package com.example.portfotracker.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.portfotracker.R;
import com.example.portfotracker.adapters.StockAdapter;
import com.example.portfotracker.databinding.FragmentFavoriteBinding;
import com.example.portfotracker.models.Stock;
import com.example.portfotracker.models.User;
import com.example.portfotracker.services.FireBaseSdkService;
import com.example.portfotracker.services.YahooFinanceService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoriteFragment extends Fragment {

    private FragmentFavoriteBinding binding;
    private StockAdapter stockAdapter;
    private ArrayList<Stock> stockArrayList = new ArrayList<>();

    private  ValueEventListener valueEventListener;

    public FavoriteFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false);
        binding.stocksRecycler.setItemAnimator(new DefaultItemAnimator());
        binding.stocksRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        stockAdapter = new StockAdapter(getContext(),stockArrayList, stock -> {
            Bundle bundle = new Bundle();
            bundle.putString(ChartFragment.STOCK_SYMBOL, stock.getSymbol());
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_favoriteFragment_to_chartFragment, bundle);
        });
        binding.stocksRecycler.setAdapter(stockAdapter);

        observeUserData();

        stockAdapter.updateStockList(stockArrayList);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(valueEventListener != null){
            FireBaseSdkService.stopObserveUserData(valueEventListener);
        }
        binding = null;
    }



    private void observeUserData(){
        valueEventListener = FireBaseSdkService.observeUserData(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if(user == null) return;
                List<String> favorites = user.getFavorites();
                stockArrayList.clear();

                Map<String, Stock> cacheStocks = YahooFinanceService.getCacheStocks();
                for (String favoriteSymbol : favorites) {
                    Stock stock = cacheStocks.get(favoriteSymbol);
                    if (stock != null) {
                        stockArrayList.add(stock);
                    }
                }
                stockAdapter.updateStockList(stockArrayList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }
}