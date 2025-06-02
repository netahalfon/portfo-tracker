package com.example.portfotracker.auth.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.portfotracker.R;
import com.example.portfotracker.activities.HomeActivity;
import com.example.portfotracker.activities.MainActivity;
import com.example.portfotracker.databinding.FragmentLoginBinding;
import com.example.portfotracker.databinding.FragmentRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth mAuth;

    public RegisterFragment() {}


    public static RegisterFragment newInstance() {
        return new RegisterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        binding.registerButton.setOnClickListener(v -> handleRegister());
        binding.loginLink.setOnClickListener(v-> Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment));

        return binding.getRoot();
    }

    private void handleRegister() {
        String name = binding.nameInput.getText().toString();
        String email = binding.emailInput.getText().toString();
        String password = binding.passwordInput.getText().toString();
        String confirmPassword = binding.confirmPasswordInput.getText().toString();
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task ->  {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getContext(), "Register Successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getContext(), HomeActivity.class);
                        startActivity(intent);
                    }else{
                        Toast.makeText(getContext(), "Registration Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}