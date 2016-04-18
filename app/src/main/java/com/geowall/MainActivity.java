package com.geowall;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class MainActivity extends AppCompatActivity {

    private Firebase mRef;
    private String mUserId;
    private String itemsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRef = new Firebase(Constants.FIREBASE_URL);
        if (mRef.getAuth()==null){
            loadLoginView();
        }
        try {
            mUserId = mRef.getAuth().getUid();
        } catch (Exception e) {
            loadLoginView();
        }
        itemsUrl = Constants.FIREBASE_URL+"/users/"+ mUserId + "/items";

        //set up ListView
        final ListView listView = (ListView)findViewById(R.id.listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,android.R.id.text1);
        listView.setAdapter(adapter);

        // Add items via the Button and EditText at the bottom of the view.
        final EditText text = (EditText) findViewById(R.id.todoText);
        final Button button = (Button) findViewById(R.id.addButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Firebase(itemsUrl)
                        .push()
                        .child("title")
                        .setValue(text.getText().toString());
            }//qui gli si passa una stringa, ma si possono passare anche oggetti complessi
        });

        // Use Firebase to populate the list.
        new Firebase(itemsUrl)
                .addChildEventListener(new ChildEventListener() {
                    //retrieve the data, called anytime new data is added to the firebase ref
                    //listner receives snapshot of data, null if empty
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        adapter.add((String) dataSnapshot.child("title").getValue());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        adapter.remove((String) dataSnapshot.child("title").getValue());
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            mRef.unauth();
            loadLoginView();
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadLoginView(){
        Intent intent = new Intent(this, LoginActivity.class);
        //ogni task contiene più attività. Settare questi flag serve a iniziare
        //un nuovo task, clear task serve per "pulire" ogni task già esistente
        //che verrebbe associato con l'attività da resettare prima che l'attività parta.
        //L'attività diventa la nuova root di un task che altrimenti sarebbe vuoto, tutte
        //le vecchie attività vengono terminate.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);     //pulisce stack delle activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);   //previene che l'user premendo back torni alla main activity
        startActivity(intent);
    }

}
