package com.example.portfotracker.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.portfotracker.R;
import com.example.portfotracker.databinding.ActivityHomeBinding; // זהו ה־import של הביינדינג
import com.example.portfotracker.services.FireBaseSdkService;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, -10);
            return insets;
        });


        FireBaseSdkService.getUserName().addOnSuccessListener(name -> {
            if(name != null){
                binding.avatarText.setText(name.toUpperCase().charAt(0) + "");
            }
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(binding.navHostFragment.getId());
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
                return NavigationUI.onNavDestinationSelected(item, navController);
            });
        }

        binding.logOutBtn.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FireBaseSdkService.signOut();
                        finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

    }
}
