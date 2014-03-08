package tv.tv9x9.player;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    
    public String TAG = "vtest";

    @Override
    protected void onHandleIntent(Intent intent) {
    	Log.i ("vtest", "gcm: GcmIntentService onHandleIntent");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty())
        	{  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            	// send error
                sendNotification("error", extras);
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
            	// deleted messages on server
                sendNotification("deleted", extras);
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                for (int i=0; i<5; i++) {
                    Log.i(TAG, "gcm: Working... " + (i+1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.i(TAG, "gcm: Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                sendNotification("received", extras);
                Log.i(TAG, "Received: " + extras.toString());
            }
        else
        	Log.i (TAG, "gcm: extras was empty, ignoring GCM");
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }


    // I/vtest   (28656): gcm: payload=Received: Bundle[{ts=1394159260568, from=892665728999, content=ddtv:28718:yt6yBESVVU_ec, 
    // message=SPLAT, android.support.content.wakelockid=1, collapse_key=do_not_collapse}]
    
    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification (String why, Bundle bundle)
    	{
    	if (!why.equals ("received"))
    		{
    		Log.i (TAG, "gcm: ignoring " + why);
    		return;
    		}
    	String msg = bundle.toString();
    	String text = bundle.getString ("message");
    	String app_name = getResources().getString (R.string.app_name);
    	
    	String mso = null;
    	String channel_id = null;
    	String episode_id = null;
    	
    	String content = bundle.getString ("content");
    	if (content != null)
    		{
    		String fields[] = content.split (":");
    		if (fields.length >= 1)
    			mso = fields[0];
    		if (fields.length >= 2)
    			channel_id = fields[1];
    		if (fields.length >= 3)
    			episode_id = fields[2];
    		}
    	
        mNotificationManager = (NotificationManager) getSystemService (Context.NOTIFICATION_SERVICE);

        Intent start_intent = new Intent (this, start.class);
        start_intent.setAction ("tv.tv9x9.player.notify");
        
        if (mso != null)
        	start_intent.putExtra ("mso", mso);
        
        if (channel_id != null)
        	start_intent.putExtra ("channel", channel_id);
        
        if (episode_id != null)
        	start_intent.putExtra ("episode", episode_id);
        
        PendingIntent pi = PendingIntent.getActivity (this, 0, start_intent, 0);
        
        String icon_name = getResources().getString (R.string.app_icon);	
        String fields[] = icon_name.split ("/");
        icon_name = fields [fields.length - 1];
        icon_name = icon_name.replaceAll ("\\.[a-z]+$", "");        
		int icon_id = getResources().getIdentifier (icon_name, "drawable", getPackageName());
        Log.i ("vtest", "gcm: icon=" + icon_name + " id=" + icon_id);
        Log.i ("vtest", "gcm: payload=" + msg);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder (this)
        .setContentTitle (app_name)
        .setStyle (new NotificationCompat.BigTextStyle()
        .bigText (text))
        .setContentText (text)
        .setSmallIcon (icon_id);

        mBuilder.setContentIntent (pi);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    	}
}