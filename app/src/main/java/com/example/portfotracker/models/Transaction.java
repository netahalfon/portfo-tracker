package com.example.portfotracker.models;

public class Transaction {
    private int quantity;
    private double price;

    public Transaction() {
        // Required for Firebase
    }

    public Transaction(int quantity, double price) {
        this.quantity = quantity;
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}

