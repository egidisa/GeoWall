package com.geowall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.geowall.domain.UserInfo;
import com.geowall.services.FirebaseManager;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    protected EditText emailEditText;
    protected EditText passwordEditText;
    protected Button loginButton;
    protected TextView signUpTextView;

    String email;
    String password;
    String nickname;
    SharedPreferences.Editor editor;
    ArrayList<UserInfo> mUserList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUserList = new ArrayList<UserInfo>();

        signUpTextView = (TextView)findViewById(R.id.signUpText);
        emailEditText = (EditText)findViewById(R.id.emailField);
        passwordEditText = (EditText)findViewById(R.id.passwordField);
        loginButton = (Button)findViewById(R.id.loginButton);

        //firebase reference
        final Firebase ref = FirebaseManager.getInstance().getRef();

        //opens new activity for sign up
        signUpTextView.setOnClickListener(new TextView.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        //retrieve user list
        Firebase tempRef = new Firebase(Constants.FIREBASE_URL).child("users");
        tempRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ds) {
                for(DataSnapshot snap : ds.getChildren()) {
                    UserInfo u = snap.getValue(UserInfo.class);
                    mUserList.add(u);
                }
            }
            @Override
            public void onCancelled(FirebaseError e) { }
        });

        //SharedPreferences for saving login credentials
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

        email = emailEditText.getText().toString();
        password = passwordEditText.getText().toString();
        emailEditText.setText(pref.getString("KEY_USERNAME", null));
        passwordEditText.setText(pref.getString("KEY_PASSWORD",null));


        //checks login credentials and opens new activity on login success
        loginButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.i(TAG, "BEGIN: loginButton.onClick()");
                email = emailEditText.getText().toString();
                password = passwordEditText.getText().toString();

                email.trim();
                password.trim();

                if (email.isEmpty()||password.isEmpty()){
                    //default alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setMessage(R.string.login_error_message)
                            .setTitle(R.string.login_error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    final String emailAddress = email;

                    ref.authWithPassword(email, password, new Firebase.AuthResultHandler(){

                        @Override
                        public void onAuthenticated(AuthData authData){

                            //auth successful
                            for(UserInfo u : mUserList){
                                Log.i(TAG,"NICKNAME"+u.getNickname());
                                if (u.getEmail().compareTo(email)==0 ){
                                    nickname = u.getNickname();
                                    editor.putString("KEY_NICKNAME", nickname);
                                }
                            }

                            editor.putString("KEY_NICKNAME", nickname);
                            editor.putString("KEY_USERNAME", email);
                            editor.putString("KEY_PASSWORD", password);
                            Log.i(TAG,"ONCREATE:"+nickname);

                            editor.commit(); // commit changes

                            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError){
                            //auth failed
                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                            builder.setMessage(firebaseError.getMessage())
                                    .setTitle(R.string.login_error_title)
                                    .setPositiveButton(android.R.string.ok, null);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                }
                Log.i(TAG, "END: loginButton.onClick()");
            }
        });
    }

    /**
     * When activity stops, saves the credential in shared preferences
     */
    @Override
    protected void onStop() {
        super.onStop();
        editor.putString("KEY_USERNAME", email);
        editor.putString("KEY_PASSWORD", password);
        editor.putString("KEY_NICKNAME", nickname);
        Log.i(TAG,"ONSTOP:"+nickname);

    }


}
