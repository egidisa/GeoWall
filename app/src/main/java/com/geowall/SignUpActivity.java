package com.geowall;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.geowall.domain.UserInfo;
import com.geowall.services.FirebaseManager;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = SignUpActivity.class.getSimpleName();
    protected EditText passwordEditText;
    protected EditText emailEditText;
    protected EditText nicknameEditText;
    protected Button signUpButton;
    String email;
    String password;
    String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        passwordEditText = (EditText)findViewById(R.id.passwordField);
        emailEditText = (EditText)findViewById(R.id.emailField);
        nicknameEditText = (EditText)findViewById(R.id.nicknameField);
        signUpButton = (Button)findViewById(R.id.signupButton);

        final Firebase ref = FirebaseManager.getInstance().getRef();

        //When registered, save credentials in preferences
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        final SharedPreferences.Editor editor = pref.edit();

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "BEGIN: signUpButton.onClick()");
                password = passwordEditText.getText().toString();
                email = emailEditText.getText().toString();
                nickname = nicknameEditText.getText().toString();

                password = password.trim();
                email = email.trim();
                nickname = nickname.trim();

                //Nickname may consist of only letters for now
                if (password.isEmpty() || email.isEmpty()|| nickname.isEmpty() || !nickname.matches("[a-zA-Z]+")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                    builder.setMessage(R.string.signup_error_message)
                            .setTitle(R.string.signup_error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {

                    // signup
                    ref.createUser(email, password, new Firebase.ResultHandler() {
                        @Override
                        public void onSuccess() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                            builder.setMessage(R.string.signup_success)
                                    .setPositiveButton(R.string.login_button_label, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                            Firebase userRef = ref.child("users");
                                            UserInfo newUser = new UserInfo();
                                            newUser.setEmail(email);
                                            newUser.setNickname(nickname);
                                            userRef.push().setValue(newUser);

                                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            editor.putString("KEY_USERNAME", email);
                                            editor.putString("KEY_PASSWORD", password);
                                            editor.putString("KEY_NICKNAME", nickname);
                                            editor.commit(); // commit changes
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }

                        @Override
                        public void onError(FirebaseError firebaseError) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                            builder.setMessage(firebaseError.getMessage())
                                    .setTitle(R.string.signup_error_title)
                                    .setPositiveButton(android.R.string.ok, null);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                }
            }
        });

        Log.i(TAG, "END: signUpButton.onClick()");
    }
}