package com.example.portfotracker.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.portfotracker.databinding.ItemStockBinding;
import com.example.portfotracker.models.Stock;

import java.util.ArrayList;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder>{

    private ArrayList<Stock> stockArrayList;

    public StockAdapter(ArrayList<Stock> stock_ArrayList) {
        stockArrayList = stock_ArrayList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateStockList(ArrayList<Stock> stock_ArrayList){
        stockArrayList = stock_ArrayList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStockBinding binding = ItemStockBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StockViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Stock currentStock = stockArrayList.get(position);
        holder.binding.stockSymbol.setText(currentStock.getSymbol());
        holder.binding.stockName.setText(currentStock.getName());
        holder.binding.stockPrice.setText(String.valueOf(currentStock.getCurrentPrice()));
        holder.binding.stockChange.setText(String.format("%.2f%%", currentStock.getPriceChangePercentage()));
    }

    @Override
    public int getItemCount() {
        return stockArrayList.size();
    }


    public static class StockViewHolder extends RecyclerView.ViewHolder{

        private ItemStockBinding binding;
        public StockViewHolder(@NonNull ItemStockBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }
}
