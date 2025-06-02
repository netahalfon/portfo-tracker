package com.example.portfotracker.services;

import android.util.Log;

import com.example.portfotracker.models.Stock;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class YahooFinanceService {

    private static final String BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart";

    private static Map<String,Stock> cacheStocks = new HashMap<>();
    private static final String[] TOP_50_SP500_STOCKS = {
            // Technology
            "MSFT",  // Microsoft
            "AAPL",  // Apple
            "NVDA",  // NVIDIA
            "AVGO",  // Broadcom
            "META",  // Meta Platforms
            "GOOGL", // Alphabet Class A
            "GOOG",  // Alphabet Class C
            "AMD",   // Advanced Micro Devices
            "ADBE",  // Adobe
            "CRM",   // Salesforce

            // Healthcare
            "LLY",   // Eli Lilly
            "UNH",   // UnitedHealth Group
            "JNJ",   // Johnson & Johnson
            "MRK",   // Merck
            "ABBV",  // AbbVie
            "PFE",   // Pfizer

            // Consumer
            "AMZN",  // Amazon
            "WMT",   // Walmart
            "PG",    // Procter & Gamble
            "COST",  // Costco
            "PEP",   // PepsiCo
            "KO",    // Coca-Cola
            "MCD",   // McDonald's
            "NKE",   // Nike

            // Financial
            "BRK-B", // Berkshire Hathaway
            "JPM",   // JPMorgan Chase
            "V",     // Visa
            "MA",    // Mastercard
            "BAC",   // Bank of America

            // Energy
            "XOM",   // ExxonMobil
            "CVX",   // Chevron

            // Industrial
            "CAT",   // Caterpillar
            "RTX",   // Raytheon Technologies
            "HON",   // Honeywell
            "DE",    // John Deere

            // Communications
            "NFLX",  // Netflix
            "CMCSA", // Comcast
            "T",     // AT&T
            "VZ",    // Verizon

            // Others
            "HD",    // Home Depot
            "ORCL",  // Oracle
            "TMO",   // Thermo Fisher Scientific
            "ACN",   // Accenture
            "DHR",   // Danaher
            "LIN",   // Linde
            "INTU",  // Intuit
            "TXN",   // Texas Instruments
            "IBM",   // IBM
            "SPGI"   // S&P Global
    };

    public static ArrayList<Stock> getAllStocks(String searchTerm) {

        ExecutorService executor = Executors.newFixedThreadPool(10);
        ArrayList<Future<Stock>> futures = new ArrayList<>();

        for (String symbol : TOP_50_SP500_STOCKS) {
            futures.add(executor.submit(() -> getStockData(symbol)));
        }

        ArrayList<Stock> stockList = new ArrayList<>();
        for (Future<Stock> future : futures) {
            try {
                Stock stock = future.get();
                cacheStocks.put(stock.getSymbol(),stock);
                if(searchTerm != null && !searchTerm.isEmpty() && !stock.getSymbol().toLowerCase().contains(searchTerm.toLowerCase()) && !stock.getName().toLowerCase().contains(searchTerm.toLowerCase())){
                    continue;
                }
                stockList.add(stock);
            } catch (Exception e) {
                Log.d("Fetch Error", "Error fetching stock data: " + e.getMessage());
            }
        }

        executor.shutdown();
        return stockList;
    }
    public static Stock getStockData(String symbol){
        try {
            URL url = new URL(BASE_URL + "/" + symbol);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setRequestProperty("User-Agent", "Mozilla/5.0");
            request.setRequestProperty("Accept", "application/json");
            request.setConnectTimeout(5000);
            request.setReadTimeout(5000);
            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));

            JsonObject close = root.getAsJsonObject()
                    .getAsJsonObject("chart")
                    .getAsJsonArray("result")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("indicators")
                    .getAsJsonArray("quote")
                    .get(0)
                    .getAsJsonObject();

            ArrayList<Float> closePrices = new ArrayList<>();
            JsonArray closeArray = close.getAsJsonArray("close");
            for(JsonElement price: closeArray){
                if (!price.isJsonNull()) {
                    closePrices.add(price.getAsFloat());
                } else {
                    closePrices.add(null);
                }
            }
            JsonObject meta = root.getAsJsonObject()
                    .getAsJsonObject("chart")
                    .getAsJsonArray("result")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("meta");

            String companyName = meta.get("longName").getAsString();
            double currentPrice = meta.get("regularMarketPrice").getAsDouble();
            double previousClose = meta.get("previousClose").getAsDouble();

            double priceChange = currentPrice - previousClose;
            double priceChangePercentage = (priceChange / previousClose) * 100;

            return new Stock(symbol, companyName, currentPrice, priceChange, priceChangePercentage,closePrices);

        } catch (IOException e) {
            Log.d("Fetch Error", "Error fetching stock data for symbol: " + symbol);
        }
        return null;
    }
    public static Map<String,Stock> getCacheStocks(){
        return cacheStocks;
    }
}
