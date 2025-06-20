package com.example.portfotracker.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.portfotracker.R;
import com.example.portfotracker.databinding.ItemStockBinding;
import com.example.portfotracker.models.Stock;

import java.util.ArrayList;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder>{
    private Context context;

    private ArrayList<Stock> stockArrayList;
    private OnItemClickListener onItemClickListener;
    public StockAdapter(Context context, ArrayList<Stock> stockArrayList, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.stockArrayList = stockArrayList;
        this.onItemClickListener = onItemClickListener;
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
        holder.binding.stockFirstLetter.setText(String.valueOf(currentStock.getSymbol().charAt(0)));
        holder.binding.stockName.setText(currentStock.getName());
        holder.binding.stockPrice.setText(String.valueOf(currentStock.getCurrentPrice()));
        holder.binding.stockChange.setText(String.format("%.2f%%", currentStock.getPriceChangePercentage()));

        // check if the current change is positive
        boolean isPositive = currentStock.getPriceChangePercentage() > 0;
        int textColor = ContextCompat.getColor(context, isPositive ? R.color.accent_green : R.color.accent_red);
        holder.binding.stockChange.setTextColor(textColor);
        holder.binding.stockChange.setBackgroundResource(isPositive
                ? R.drawable.bg_stock_change_positive
                : R.drawable.bg_stock_change_negative);


        int arrowIcon = isPositive ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down;
        holder.binding.stockChange.setCompoundDrawablesWithIntrinsicBounds(arrowIcon, 0, 0, 0);
        holder.binding.itemView.setOnClickListener(v->{
            if(onItemClickListener != null){
                onItemClickListener.onItemClick(currentStock);
            }
        });
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
    public interface OnItemClickListener {
        void onItemClick(Stock stock);
    }
}
