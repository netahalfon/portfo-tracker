package com.example.portfotracker.fragments;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.portfotracker.R;
import com.example.portfotracker.databinding.FragmentChartBinding;
import com.example.portfotracker.models.Stock;
import com.example.portfotracker.models.User;
import com.example.portfotracker.services.FireBaseSdkService;
import com.example.portfotracker.services.YahooFinanceService;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class ChartFragment extends Fragment {

    public static final String STOCK_SYMBOL = "STOCK_SYMBOL";

    private  ValueEventListener valueEventListener;

    private FragmentChartBinding binding;

    private String stockSymbol;
    private Stock stock;

    public ChartFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            stockSymbol = getArguments().getString(STOCK_SYMBOL);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChartBinding.inflate(inflater, container, false);
        new Thread(() -> {
            stock = YahooFinanceService.getStockData(stockSymbol);
            getActivity().runOnUiThread(this::initViews);
        }).start();

        binding.btnBuy.setOnClickListener(v -> showBuySellDialog(true));
        binding.btnSell.setOnClickListener(v -> showBuySellDialog(false));
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());
        return binding.getRoot();
    }

private void toggleFavorite() {
    FireBaseSdkService.toggleFavorite(stock.getSymbol()).addOnCompleteListener(task -> {
        if(!task.isSuccessful()){
            Toast.makeText(getContext(),task.getException().getMessage(),Toast.LENGTH_LONG).show();
            return;
        }
    });
}

private void initViews(){

        binding.companyName.setText(stock.getName());
        binding.stockSymbol.setText(stock.getSymbol());
        binding.currentPrice.setText(String.format("$%.2f",stock.getCurrentPrice()));

        configureChartAppearance();
        configureChartData();

        observeUserData();
    }
    private void configureChartAppearance(){
        binding.lineChart.setDrawGridBackground(false); // No grid background
        binding.lineChart.getDescription().setEnabled(false); // No description text
        binding.lineChart.setDrawBorders(false); // No border around chart
        binding.lineChart.setTouchEnabled(true); // Enable touch interactions
        binding.lineChart.setDragEnabled(true); // Enable drag
        binding.lineChart.setScaleEnabled(true); // Enable zoom

        binding.lineChart.setHighlightPerDragEnabled(true);
        binding.lineChart.setHighlightPerTapEnabled(true);

        // Hide right Y-axis
        binding.lineChart.getAxisRight().setEnabled(false);

        // Customize left Y-axis
        YAxis leftAxis = binding.lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true); // Light grid lines
        leftAxis.setGridColor(getResources().getColor(R.color.light_gray)); // Light gray grid lines
        leftAxis.setTextColor(getResources().getColor(R.color.black)); // Text color

        // Customize X-axis
        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Position X-axis at bottom
        xAxis.setDrawGridLines(false); // No grid lines
        xAxis.setAxisMinimum(16.5f);
        float maxTime = 16.5f + (stock.getStockPriceList().size() / 60f);
        xAxis.setAxisMaximum(maxTime);
        xAxis.setTextColor(getResources().getColor(R.color.black)); // Text color
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hours = (int) value;
                int minutes = (int) ((value - hours) * 60);
                return String.format("%02d:%02d", hours, minutes);
            }
        });

        // Disable legend
        binding.lineChart.getLegend().setEnabled(false);
        
        
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(valueEventListener != null){
            FireBaseSdkService.stopObserveUserData(valueEventListener);
        }
        binding = null;
    }

    private void configureChartData(){
        double priceChange = stock.getPriceChangePercentage();
        List<Entry> entries = new ArrayList<>();
        ArrayList<Float> stockPriceList = stock.getStockPriceList();
        for (int i = 0; i < stockPriceList.size(); i++) {
            if (stockPriceList.get(i) == null) continue;
            float timeInHours = convertToDecimalHours(i);
            entries.add(new Entry(timeInHours, stockPriceList.get(i)));
        }
        LineDataSet lineDataSet = new LineDataSet(entries, "Stocks");
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);

        lineDataSet.setHighlightEnabled(true);
        lineDataSet.setDrawHorizontalHighlightIndicator(true);
        lineDataSet.setDrawVerticalHighlightIndicator(true);
        lineDataSet.setHighLightColor(getResources().getColor(R.color.black));

        lineDataSet.setDrawHighlightIndicators(true);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("$%.2f", value);
            }
        });
        // Set color based on price change
        int colorResId = (priceChange >= 0) ?
                R.color.positive_green :  // Use green for positive change
                R.color.negative_red;     // Use red for negative change

        lineDataSet.setColor(getResources().getColor(colorResId));
        lineDataSet.setDrawFilled(true);

        // Set gradient drawable based on price change
        int gradientResId = (priceChange >= 0) ?
                R.drawable.fade_green :   // Use green gradient for positive change
                R.drawable.fade_red;      // Use red gradient for negative change

        lineDataSet.setFillDrawable(getResources().getDrawable(gradientResId));
        lineDataSet.setDrawValues(false);

        LineData lineData = new LineData(lineDataSet);
        binding.lineChart.setData(lineData);
        binding.lineChart.invalidate();
    }

    private float convertToDecimalHours(int index) {
        float baseHour = 16.5f;
        float minutesAfterStart = index;
        return baseHour + (minutesAfterStart / 60f);
    }

    private void showBuySellDialog(boolean isBuy) {
        //Create a dialog-optimized view
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_buy_sell, null);

        //Finding components in the dialog view
        TextView tvCurrentQuantity = dialogView.findViewById(R.id.tv_current_quantity);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextView tvNewQuantity = dialogView.findViewById(R.id.tv_new_quantity);
        TextView tvError = dialogView.findViewById(R.id.tv_error);

        //View current quantity
        String quantityStr = binding.tvQuantity.getText().toString().replace("$", "");
        int currentQuantity = Integer.parseInt(quantityStr);
        tvCurrentQuantity.setText("Current quantity: " + currentQuantity);




        //Building the dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isBuy ? "Buy" : "Sell")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialogInterface, which) -> {
                    String amountStr = etAmount.getText().toString();
                    int amount = Integer.parseInt(amountStr);
                    Task<Void>task = isBuy ?
                            FireBaseSdkService.buyStock(stock,amount) :
                            FireBaseSdkService.sellStock(stock,amount);
                    task.addOnCompleteListener(res -> {
                       if(res.isSuccessful()) {
                           Toast.makeText(getContext(), isBuy ? "Buy Complete":"Sell Complete",Toast.LENGTH_LONG).show();
                           return;
                       }
                       Toast.makeText(getContext(),res.getException().getMessage(),Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Showing the dialog
        dialog.show();
        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        // Calculating and updating the future quantity while writing
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int amount = 0;
                boolean valid = false;
                tvError.setText(" ");

                try {
                    amount = Integer.parseInt(s.toString());
                    int newQuantity = isBuy ? currentQuantity + amount : currentQuantity - amount;

                    if (newQuantity<0) {
                        tvError.setText("Cannot sell more than your quantity");
                        tvError.setVisibility(View.VISIBLE);
                    } else if (amount == 0) {
                        tvError.setText("Add a valid amount");
                        tvError.setVisibility(View.VISIBLE);
                    }else{
                        valid = true;
                    }
                    tvNewQuantity.setText("New quantity: " + newQuantity);
                } catch (Exception ignore) {}
                if (positiveButton != null) {
                    positiveButton.setEnabled(valid);
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        positiveButton.setEnabled(false); // כפתור Submit מושבת בהתחלה

    }

    private void observeUserData(){
        valueEventListener = FireBaseSdkService.observeUserData(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if(user == null) return;
                int newQuantity = user.getTotalQuantityForStock(stockSymbol);
                binding.tvQuantity.setText(String.valueOf(newQuantity));

                List<String> favorites = user.getFavorites();
                boolean isFavorite = favorites != null && favorites.contains(stockSymbol);
                updateFavoriteIcon(isFavorite);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.star);
        } else {
            binding.btnFavorite.setImageResource(R.drawable.empty_star);
        }
    }

}