package com.geowall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.geowall.domain.Message;
import com.geowall.services.FirebaseManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author sara
 *
 * This activity shows the contents of a wall. It also handles the update of the image taken from
 * the camera.
 */
public class WallActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;

    private static final String TAG = WallActivity.class.getSimpleName();
    private ValueEventListener mConnectedListener;
    private MessageListAdapter mMessageListAdapter;
    ListView listView;
    String mUsername;
    String timeStamp;
    Firebase mRef;
    EditText inputText;
    FirebaseStorage storage;
    String mCurrentPhotoPath;
    FirebaseManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wall);
        setTitle(getIntent().getStringExtra("EXTRA_WALL_NAME"));
        Log.i(TAG,"NAME_RECEIVED"+getIntent().getStringExtra("EXTRA_WALL_NAME"));

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        mUsername = pref.getString("KEY_NICKNAME", null);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        String wallId = getIntent().getStringExtra("EXTRA_WALL_KEY");
        storage = FirebaseStorage.getInstance();
        fm = FirebaseManager.getInstance();
        mRef = fm.getRef().child("walls").child(wallId).child("messages");
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        findViewById(R.id.imgButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG,"buttonpressed");
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //check if app available for taking pictures
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }else{
                    //no app available
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            sendPhoto(photo,name);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        listView = (ListView)findViewById(android.R.id.list);
        mMessageListAdapter = new MessageListAdapter(getApplicationContext(), mRef.limit(50), this, R.layout.wall_message, mUsername);
        listView.setAdapter(mMessageListAdapter);
        mMessageListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Log.i(TAG,"DataChanged, listcnt: "+listView.getCount());

                listView.setSelection(listView.getCount());
                listView.smoothScrollToPosition(listView.getCount());

            }
        });

        // Indication of connection status
        mConnectedListener = mRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Log.i(TAG,"onStart():onDataChange():Connected to Firebase");
                    //Toast.makeText(WallActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG,"onStart():onDataChange():Disconnected to Firebase");
                    //Toast.makeText(WallActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    /**
     * On activity stop, remove eventlistener on connect and clean leastview
     */
    @Override
    public void onStop() {
        super.onStop();
        mRef.removeEventListener(mConnectedListener);
        mMessageListAdapter.cleanup();
    }

    /**
     * Send a text to the firebase db. An unique identifier is created on the basis of datetime
     * and firebase will manage possible collisions on files updated     *
     *
    */
    private void sendMessage() {
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if (!input.equals("")) {
            Message msg = new Message();
            msg.setUid(mUsername);
            msg.setContent(input);
            msg.setId("");
            timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
            msg.setTimestamp( timeStamp);

            // Create a new, auto-generated child of that chat location, and save msg
            mRef.push().setValue(msg);
            inputText.setText("");
            listView.setSelection(listView.getCount());
            listView.smoothScrollToPosition(listView.getCount());
        }
    }

    /**
     *
     * */
    private void sendPhoto(Bitmap pic, String name) {
        inputText = (EditText) findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pic.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] data = baos.toByteArray();

        StorageReference storageRef = storage.getReferenceFromUrl("gs://geowallapp.appspot.com/").child("images/"+name);
        UploadTask uploadTask = storageRef.putBytes(data);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                inputText.setText("Upload is " + progress + "% done");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Handle unsuccessful uploads
                Log.i(TAG,"ERROR_UPLOAD"+e);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.i(TAG,"UPLOAD_SUCCESS");
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Message msg = new Message();
                msg.setUid(mUsername);
                msg.setContent(downloadUrl.toString()); //TODO url img
                msg.setId("");
                timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
                msg.setTimestamp( timeStamp);
                // Create a new, auto-generated child of that chat location, and save our chat data there
                mRef.push().setValue(msg);
                inputText.setText("");
                listView.setSelection(listView.getCount());
                listView.smoothScrollToPosition(listView.getCount());
            }
        });



    }

}
