package com.example.portfotracker.auth.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.portfotracker.R;
import com.example.portfotracker.activities.HomeActivity;
import com.example.portfotracker.databinding.FragmentLoginBinding;
import com.example.portfotracker.services.FireBaseSdkService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    public LoginFragment() {}

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        binding.registerLink.setOnClickListener(v-> Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment));

        binding.loginButton.setOnClickListener(v->handleLogin());


        return binding.getRoot();
    }

    private void handleLogin() {
        String email = binding.emailInput.getText().toString();
        String password = binding.passwordInput.getText().toString();

        if(email == null || password == null || email.isEmpty() || password.isEmpty()){
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FireBaseSdkService.login(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    hideKeyboardFrom(binding.getRoot());
                    Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireActivity(), HomeActivity.class);
                    startActivity(intent);
                }else {
                    Toast.makeText(getContext(), "Login Failed", Toast.LENGTH_SHORT).show();
                }
            });
    }
    public static void hideKeyboardFrom( View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}