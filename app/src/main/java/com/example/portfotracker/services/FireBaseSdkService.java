package com.example.portfotracker.services;

import com.example.portfotracker.models.Stock;
import com.example.portfotracker.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FireBaseSdkService {

    private static final String USERS_PATH_STRING = "users";
    private static final String TRANSACTIONS_PATH_STRING = "transactions";
    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    private static User currentUser;
    public static Task<AuthResult> register(String name,String email, String password){

        TaskCompletionSource<AuthResult> taskSource = new TaskCompletionSource<>();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String userID = task.getResult().getUser().getUid();
                currentUser = new User(name, email,1000);
                mDatabase.child(USERS_PATH_STRING).child(userID).setValue(currentUser);
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
                String userID = task.getResult().getUser().getUid();
                // Fetch user data from Firebase Realtime Database
                mDatabase.child(USERS_PATH_STRING).child(userID).get().addOnCompleteListener(dataTask -> {
                    if (dataTask.isSuccessful() && dataTask.getResult().exists()) {
                        String name = dataTask.getResult().child("name").getValue(String.class);
                        Double balance = dataTask.getResult().child("balance").getValue(Double.class);
                        currentUser = new User(name, email,balance != null ? balance : 0);
                        taskSource.setResult(task.getResult());
                    }else{
                        taskSource.setException(dataTask.getException() != null
                                ? dataTask.getException()
                                : new Exception("Unknown registration error"));
                    }
                });
            }else{
                taskSource.setException(task.getException() != null
                        ? task.getException()
                        : new Exception("Unknown registration error"));            }
        });


        return taskSource.getTask();
    }

    public static User getCurrentUser(){
        return currentUser;
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
        DatabaseReference userRef = mDatabase.child(USERS_PATH_STRING).child(userId);

        userRef.get().addOnCompleteListener(userFetchTask -> {
            if (!userFetchTask.isSuccessful() || !userFetchTask.getResult().exists()) {
                taskSource.setException(userFetchTask.getException() != null
                        ? userFetchTask.getException()
                        : new Exception("Failed to fetch user data"));
                return;
            }

            DataSnapshot data = userFetchTask.getResult();
            Double balance = data.child("balance").getValue(Double.class);
            double estimatedPrice = stock.getCurrentPrice() * quantity;

            if (balance == null) {
                taskSource.setException(new Exception("Failed to load balance"));
                return;
            }

            // Step 1: validate quantity if selling
            getTotalStockQuantity(stockSymbol).addOnCompleteListener(getTotalStockTask -> {
                if (!getTotalStockTask.isSuccessful()) {
                    taskSource.setException(getTotalStockTask.getException() != null
                            ? getTotalStockTask.getException() :
                            new Exception("Failed to fetch stock quantity"));
                    return;
                }

                int ownedQuantity = getTotalStockTask.getResult();
                if (!isBuy && ownedQuantity < quantity) {
                    taskSource.setException(new Exception("Insufficient stock quantity"));
                    return;
                }

                // Step 2: validate balance if buying
                if (isBuy && estimatedPrice > balance) {
                    taskSource.setException(new Exception("Insufficient credits"));
                    return;
                }

                // Step 3: continue with transaction
                int signedQuantity = isBuy ? quantity : -quantity;
                double signedPrice = isBuy ? -estimatedPrice : estimatedPrice;
                double newBalance = balance + signedPrice;

                Map<String, Object> transaction = new HashMap<>();
                transaction.put("quantity", signedQuantity);
                transaction.put("price", stock.getCurrentPrice());

                userRef.child(TRANSACTIONS_PATH_STRING).child(stockSymbol)
                        .push()
                        .setValue(transaction)
                        .addOnCompleteListener(transactionTask -> {
                            if (!transactionTask.isSuccessful()) {
                                taskSource.setException(transactionTask.getException() != null
                                        ? transactionTask.getException() :
                                        new Exception("Failed to add transaction"));
                                return;
                            }

                            userRef.child("balance").setValue(newBalance)
                                    .addOnCompleteListener(balanceUpdateTask -> {
                                        if (balanceUpdateTask.isSuccessful()) {
                                            currentUser.setAccountBalance(newBalance);
                                            taskSource.setResult(balanceUpdateTask.getResult());
                                            return;
                                        }
                                        taskSource.setException(balanceUpdateTask.getException() != null
                                        ? balanceUpdateTask.getException() :
                                        new Exception("Failed to update balance"));
                                    });
                        });
            });
        });

        return taskSource.getTask();
    }


    public static Task<Integer> getTotalStockQuantity(String stockSymbol) {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference stockRef = mDatabase.child(USERS_PATH_STRING)
                .child(userId)
                .child(TRANSACTIONS_PATH_STRING)
                .child(stockSymbol);

        TaskCompletionSource<Integer> taskSource = new TaskCompletionSource<>();

        stockRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                taskSource.setException(new Exception("Failed to fetch transactions"));
                return;
            }

            int totalQuantity = 0;
            for (DataSnapshot snapshot : task.getResult().getChildren()) {
                Integer q = snapshot.child("quantity").getValue(Integer.class);
                if (q != null) {
                    totalQuantity += q;
                }
            }

            taskSource.setResult(totalQuantity);
        });

        return taskSource.getTask();
    }
}
