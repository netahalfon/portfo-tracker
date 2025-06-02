package com.example.portfotracker.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String name;
    private String email;
    private double accountBalance;

    private Map<String, List<Transaction>> transactions;

    public User(){
        // Required for Firebase
    }
    public User(String name, String email, double accountBalance) {
        this.name = name;
        this.email = email;
        this.accountBalance = accountBalance;
        this.transactions = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public Map<String, List<Transaction>> getTransactions() {
        if(transactions == null) return new HashMap<>();
        return transactions;
    }

    public void setTransactions(Map<String, List<Transaction>> transactions) {
        this.transactions = transactions;
    }

    public int getTotalQuantityForStock(String stockSymbol) {
        if(transactions == null) return  0;
        List<Transaction> transactionsForStock = transactions.get(stockSymbol);
        if (transactionsForStock == null) {
            return 0;
        }
        return transactionsForStock.stream()
                .mapToInt(Transaction::getQuantity)
                .sum();
    }
}
