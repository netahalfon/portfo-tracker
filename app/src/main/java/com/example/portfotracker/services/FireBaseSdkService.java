package com.example.portfotracker.services;

import com.example.portfotracker.models.Stock;
import com.example.portfotracker.models.Transaction;
import com.example.portfotracker.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireBaseSdkService {

    private static final String USERS_PATH_STRING = "users";
    private static final String FAVORITES_PATH_STRING = "favorites";
    private static final String ACCOUNT_BALANCE_PATH_STRING = "accountBalance";
    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    public static Task<AuthResult> register(String name,String email, String password){

        TaskCompletionSource<AuthResult> taskSource = new TaskCompletionSource<>();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String userID = task.getResult().getUser().getUid();
                User logedInUser = new User(name, email,1000);
                mDatabase.child(USERS_PATH_STRING).child(userID).setValue(logedInUser);
                taskSource.setResult(task.getResult());
            }else{
                taskSource.setException(task.getException() != null
                        ? task.getException()
                        : new Exception("Unknown registration error"));
            }
        });

        return taskSource.getTask();
    }
    public static Task<AuthResult> login(String email, String password){

        TaskCompletionSource<AuthResult> taskSource = new TaskCompletionSource<>();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                taskSource.setResult(task.getResult());
            }else{
                taskSource.setException(task.getException() != null
                        ? task.getException()
                        : new Exception("Unknown registration error"));            }
        });

        return taskSource.getTask();
    }

    public static Task<String> getUserName(){
        TaskCompletionSource<String> taskSource = new TaskCompletionSource<>();

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = mDatabase.child(USERS_PATH_STRING).child(userId).child("name");
        userRef.get().addOnCompleteListener(task -> {
        if (task.isSuccessful() && task.getResult().exists()) {
            String name = task.getResult().getValue(String.class);
            taskSource.setResult(name);
        }else{
            taskSource.setException(task.getException() != null
                    ? task.getException()
                    : new Exception("Failed to update user data"));
        }
        });
        return taskSource.getTask();
    }

    public static ValueEventListener observeUserData(ValueEventListener valueEventListener){
        String userId = mAuth.getCurrentUser().getUid();
        return mDatabase.child(USERS_PATH_STRING)
                .child(userId)
                .addValueEventListener(valueEventListener);
    }
    public static void stopObserveUserData(ValueEventListener valueEventListener){
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child(USERS_PATH_STRING)
                .child(userId)
                .removeEventListener(valueEventListener);
    }

    public static Task<Void> buyStock(Stock stock, int quantity) {
        return processStockTransaction(stock, quantity, true);
    }

    public static Task<Void> sellStock(Stock stock, int quantity) {
        return processStockTransaction(stock, quantity, false);
    }

    private static Task<Void> processStockTransaction(Stock stock, int quantity, boolean isBuy) {

        TaskCompletionSource<Void> taskSource = new TaskCompletionSource<>();

        String userId = mAuth.getCurrentUser().getUid();
        String stockSymbol = stock.getSymbol();
        double estimatedPrice = stock.getCurrentPrice() * quantity;

        DatabaseReference userRef = mDatabase.child(USERS_PATH_STRING).child(userId);

        userRef.get().addOnCompleteListener(userFetchTask -> {
            if (!userFetchTask.isSuccessful() || !userFetchTask.getResult().exists()) {
                taskSource.setException(userFetchTask.getException() != null
                        ? userFetchTask.getException()
                        : new Exception("Failed to fetch user data"));
                return;
            }

            User currentUser = userFetchTask.getResult().getValue(User.class);
            if (currentUser == null) {
                taskSource.setException(new Exception("Failed to fetch user data"));
                return;
            }

            // Step 1: validate quantity if selling
            int ownedQuantity = currentUser.getTotalQuantityForStock(stockSymbol);
            if (!isBuy && ownedQuantity < quantity) {
                taskSource.setException(new Exception("Insufficient stock quantity"));
                return;
            }

            // Step 2: validate balance if buying
            double accountBalance = currentUser.getAccountBalance();
            if (isBuy && estimatedPrice > accountBalance) {
                taskSource.setException(new Exception("Insufficient credits"));
                return;
            }

            // Step 3: continue with transaction
            int signedQuantity = isBuy ? quantity : -quantity;
            double signedPrice = isBuy ? -estimatedPrice : estimatedPrice;
            Transaction transaction = new Transaction(signedQuantity, stock.getCurrentPrice());

            // Step 4: update balance
            double newBalance = accountBalance + signedPrice;
            currentUser.setAccountBalance(newBalance);

            // Step 5: update transaction list
            Map<String, List<Transaction>> userTransactions = currentUser.getTransactions();
            if (userTransactions == null) {
                userTransactions = new HashMap<>();
            }

            List<Transaction> stockTransactions = userTransactions.get(stockSymbol);
            if (stockTransactions == null) {
                stockTransactions = new ArrayList<>();
            }

            stockTransactions.add(transaction);
            userTransactions.put(stockSymbol, stockTransactions);
            currentUser.setTransactions(userTransactions); // update map in user

            // Step 6: push updated user to Firebase
            userRef.setValue(currentUser).addOnCompleteListener(setTask -> {
                if (setTask.isSuccessful()) {
                    taskSource.setResult(setTask.getResult());
                } else {
                    taskSource.setException(setTask.getException() != null
                            ? setTask.getException()
                            : new Exception("Failed to update user data"));
                }
            });
        });

        return taskSource.getTask();
    }

    public static Task<Void> setUserAccountBalance(double newBalance) {
        TaskCompletionSource<Void> taskSource = new TaskCompletionSource<>();

        String userId = mAuth.getCurrentUser().getUid();

        DatabaseReference balanceRef = mDatabase.child(USERS_PATH_STRING).child(userId).child(ACCOUNT_BALANCE_PATH_STRING);

        balanceRef.setValue(newBalance).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                taskSource.setResult(task.getResult());
            } else {
                taskSource.setException(task.getException() != null
                        ? task.getException()
                        : new Exception("Failed to update balance"));
            }
        });

        return taskSource.getTask();
    }

    public static Task<Boolean> toggleFavorite(String stockSymbol) {
        TaskCompletionSource<Boolean> taskSource = new TaskCompletionSource<>();
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = mDatabase.child(USERS_PATH_STRING).child(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                taskSource.setException(task.getException() != null
                        ? task.getException()
                        : new Exception("Failed to fetch user data"));
                return;
            }
            User user = task.getResult().getValue(User.class);
            if (user == null) {
                taskSource.setException(new Exception("Failed to fetch user data"));
                return;
            }

            List<String> favorites = user.getFavorites();

            if(favorites.contains(stockSymbol)){
                favorites.remove(stockSymbol);
            }else {
                favorites.add(stockSymbol);
            }

            userRef.child(FAVORITES_PATH_STRING).setValue(favorites).addOnCompleteListener(setTask -> {
                if (setTask.isSuccessful()) {
                    // If the update to the database succeeded,
                    // set the task result to whether the stock is currently a favorite (true = added to favorites, false = removed from favorites)
                    taskSource.setResult(favorites.contains(stockSymbol));
                }else{
                    taskSource.setException(setTask.getException() != null
                            ? setTask.getException()
                            : new Exception("Failed to update user data"));
                }
            });
        });
        return taskSource.getTask();
    }

    public static void signOut() {
        mAuth.signOut();
    }
}
