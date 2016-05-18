package com.geowall;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.firebase.client.Query;
import com.geowall.FirebaseListAdapter;
import com.geowall.R;
import com.geowall.domain.Message;

import org.w3c.dom.Text;


/**
 * @author greg
 * @since 6/21/13
 *
 * This class is an example of how to use FirebaseListAdapter. It uses the <code>Chat</code> class to encapsulate the
 * data for each individual chat message
 */
public class MessageListAdapter extends FirebaseListAdapter<Message> {

    // The mUsername for this client. We use this to indicate which messages originated from this user
    private String mUsername;
    private String timeStamp;

    public MessageListAdapter(Query ref, Activity activity, int layout, String mUsername) {
        super(ref, Message.class, layout, activity);
        this.mUsername = mUsername;
    }

    /**
     * Bind an instance of the <code>Chat</code> class to our view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a data change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Chat</code> instance that represents the current data to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param msg An instance representing the current state of a chat message
     */
    @Override
    protected void populateView(View view, Message msg) {
        // Map a Chat object to an entry in our listview

        String author = msg.getUid();
        String time = msg.getTimestamp();
        TextView authorText = (TextView) view.findViewById(R.id.author);
        TextView timeText = (TextView) view.findViewById(R.id.timetxt);
        authorText.setText(author+" ");
        timeText.setText(time);
        // If the message was sent by this user, color it differently
        if (author != null && author.equals(mUsername)) {
            authorText.setTextColor(Color.RED);
            //TODO change this message layout, not just color
        } else {
            authorText.setTextColor(Color.BLUE);

        }
        ((TextView) view.findViewById(R.id.message)).setText(msg.getText());
    }
}