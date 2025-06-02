package com.example.portfotracker.models;

import java.util.ArrayList;

public class Stock {
    private String symbol;
    private String name;
    private double currentPrice;
    private double priceChangePercentage;
    private double priceChange;
    private ArrayList<Float> stockPriceList;

    public Stock(String symbol, String name, double currentPrice, double priceChange, double priceChangePercentage, ArrayList<Float> stockPriceList) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.priceChangePercentage = priceChangePercentage;
        this.stockPriceList = stockPriceList;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getPriceChangePercentage() {
        return priceChangePercentage;
    }

    public void setPriceChangePercentage(double priceChangePercentage) {
        this.priceChangePercentage = priceChangePercentage;
    }
    public double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }

    public ArrayList<Float> getStockPriceList() {
        return stockPriceList;
    }

    public void setStockPriceList(ArrayList<Float> stockPriceList) {
        this.stockPriceList = stockPriceList;
    }
}
