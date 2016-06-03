package com.geowall;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.Query;
import com.geowall.domain.Message;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;


/**
 * @author sara
 * This class uses FirebaseListAdapter. It uses the <code>Mesage</code> class to encapsulate the
 * data for each individual chat message
 */
public class MessageListAdapter extends FirebaseListAdapter<Message> {

    // The mUsername for this client. Used to indicate which messages originated from this user
    private String mUsername;
    String url;
    private Context context;
    FirebaseStorage storage;
    ImageView imgView;
    Activity a;

    private static final String TAG = MessageListAdapter.class.getSimpleName();

    public MessageListAdapter(Context context, Query ref, Activity activity, int layout, String mUsername) {
        super(ref, Message.class, layout, activity);
        this.mUsername = mUsername;
        this.context = context;
        this.a = activity;
    }

    /**
     * Bind an instance of the <code>Message</code> class to the list view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a data change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Chat</code> instance that represents the current data to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param msg  An instance representing the current state of a chat message
     */
    @Override
    protected void populateView(View view, Message msg) {
        Log.i(TAG, "POPULATEVIEW");
        // map a Chat object to an entry in our listview

        storage = FirebaseStorage.getInstance();
        String author = msg.getUid();
        String time = msg.getTimestamp();
        TextView authorText = (TextView) view.findViewById(R.id.author);
        TextView timeText = (TextView) view.findViewById(R.id.timetxt);
        imgView = (ImageView) view.findViewById(R.id.imgView);
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicked");
            }
        });

        authorText.setText(author + " ");
        timeText.setText(time);
        // If the message was sent by this user, color it differently
        if (author != null && author.equals(mUsername)) {
            authorText.setTextColor(Color.RED);
            //TODO change this message layout to be like telegram's bubbles
        } else {
            authorText.setTextColor(Color.BLUE);

        }
        Log.i(TAG, "URLMSG : " + msg.getText());
        if (msg.getText().startsWith("https://firebasestorage.googleapis.com/v0/b/geowallapp.appspot.com/o/")) {
            //message is an image
            Log.i(TAG, "ENTERER_IFIMGVIEW : " + msg.getText());
            ((TextView) view.findViewById(R.id.message)).setText("");
            //fill imageView
            Picasso.with(context)
                    .load(msg.getText())
                    .placeholder(R.drawable.placeholder)
                    .into(imgView);
        } else {
            //message is just text
            ((TextView) view.findViewById(R.id.message)).setText(msg.getText());
        }
    }


}