package com.example.portfotracker.fragments;

import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.portfotracker.R;
import com.example.portfotracker.adapters.StockAdapter;
import com.example.portfotracker.databinding.FragmentProfileBinding;
import com.example.portfotracker.models.Stock;
import com.example.portfotracker.models.Transaction;
import com.example.portfotracker.models.User;
import com.example.portfotracker.services.FireBaseSdkService;
import com.example.portfotracker.services.YahooFinanceService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private StockAdapter stockAdapter;
    private ArrayList<Stock> stockArrayList = new ArrayList<>();

    private  ValueEventListener valueEventListener;

    public ProfileFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        binding.stocksRecycler.setItemAnimator(new DefaultItemAnimator());
        binding.stocksRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        stockAdapter = new StockAdapter(getContext(),stockArrayList, stock -> {
            Bundle bundle = new Bundle();
            bundle.putString(ChartFragment.STOCK_SYMBOL, stock.getSymbol());
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_profileFragment_to_chartFragment, bundle);
        });
        binding.stocksRecycler.setAdapter(stockAdapter);

        binding.btnDeposit.setOnClickListener(v -> showDepositWithdrawDialog(true));
        binding.btnWithdraw.setOnClickListener(v -> showDepositWithdrawDialog(false));

        observeUserData();
        setTotalPaid();
        setCurrentValue();
        //TODO: Load stocks from Firebase and combine them with the data from Yahoo Finance
        //TODO: insert the combination into stockArrayList
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

    private void showDepositWithdrawDialog(boolean isDeposit) {
        //Create a dialog-optimized view
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_deposit_withdraw, null);

        //Finding components in the dialog view
        TextView tvCurrentBalance = dialogView.findViewById(R.id.tv_current_balance);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextView tvNewBalance = dialogView.findViewById(R.id.tv_new_balance);
        TextView tvError = dialogView.findViewById(R.id.tv_error);

        //View current balance
        String balanceStr = binding.tvBalance.getText().toString().replace("$", "");
        double currentBalance = Double.parseDouble(balanceStr);
        tvCurrentBalance.setText("Current balance: $" + String.format("%.2f", currentBalance));




        //Building the dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isDeposit ? "Deposit" : "Withdraw")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialogInterface, which) -> {
                    String entered = etAmount.getText().toString();
                    double amount = Double.parseDouble(entered);
                    double newBalance = isDeposit ? currentBalance + amount : currentBalance - amount;
                    FireBaseSdkService.setUserAccountBalance(newBalance);
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Showing the dialog
        dialog.show();
        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        // Calculating and updating the future balance while writing
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                double amount = 0;
                boolean valid = false;
                tvError.setText(" ");

                try {
                    amount = Double.parseDouble(s.toString());
                    double newBalance = isDeposit ? currentBalance + amount : currentBalance - amount;

                    if (newBalance<0) {
                        tvError.setText("Cannot withdraw more than your balance");
                    } else if (amount == 0) {
                        tvError.setText("Add a valid amount");
                    }else{
                        valid = true;
                    }
                    tvNewBalance.setText("New balance: $" + String.format("%.2f", newBalance));
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


    private void setTotalPaid() {
        // TODO: Calculate and load total paid for stocks from Firebase in the future
        if (binding != null) {
            binding.tvTotalPaid.setText("$900.00");
        }
    }

    private void setCurrentValue() {
        // TODO: Calculate and load current stocks value from Firebase in the future
        if (binding != null) {
            binding.tvCurrentValue.setText("$1020.00");
        }
    }

    private void observeUserData(){
        valueEventListener = FireBaseSdkService.observeUserData(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if(user == null) return;
                binding.tvBalance.setText(String.format("$%.2f", user.getAccountBalance()));
                binding.tvUsername.setText(user.getName());
                Map<String, List<Transaction>> transactions = user.getTransactions();
                stockArrayList.clear();
                double totalPaidForStocks = 0;
                double currentStocksValue = 0;
                for (String stockSymbol : transactions.keySet()) {
                    if(user.getTotalQuantityForStock(stockSymbol) == 0)continue;
                    Stock stock = YahooFinanceService.getCacheStocks().get(stockSymbol);
                    stockArrayList.add(stock);
                    for (Transaction transaction : transactions.get(stockSymbol)){
                        totalPaidForStocks += transaction.getPrice() * transaction.getQuantity();
                        currentStocksValue += stock.getCurrentPrice() * transaction.getQuantity();
                    }
                }
                binding.tvTotalPaid.setText(String.format("$%.2f",totalPaidForStocks));
                binding.tvCurrentValue.setText(String.format("$%.2f",currentStocksValue));
                binding.tvChangeFromCost.setText(String.format("$%.2f",currentStocksValue - totalPaidForStocks));
                binding.tvPortfolioValue.setText(String.format("$%.2f",currentStocksValue + user.getAccountBalance()));
                stockAdapter.updateStockList(stockArrayList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }
}