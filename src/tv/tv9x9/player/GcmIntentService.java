package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService
	{
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService()
    	{
        super("GcmIntentService");
    	}

    public void log (String text)
    	{
    	Log.i ("vtest", "gcm: " + text);
    	}
    
    @Override
    protected void onHandleIntent(Intent intent)
    	{
    	log ("GcmIntentService onHandleIntent");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parafmeter must be the intent you received
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
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType))
            	{
            	// send error
                sendNotification("error", extras);
            	}
            else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType))
            	{
            	// deleted messages on server
                sendNotification("deleted", extras);
                // If it's a regular GCM message, do some work.
            	}
            else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType))
            	{
                // This loop represents the service doing some work.
                for (int i=0; i<5; i++)
                	{
                    log ("Working... " + (i+1) + "/5 @ " + SystemClock.elapsedRealtime());
                    try
                    	{
                        Thread.sleep(5000);
                    	}
                    catch (InterruptedException e)
                    	{
                    	}
                	}
                log ("Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                sendNotification("received", extras);
                log ("Received: " + extras.toString());
            	}
            else
                log ("message type was: " + messageType + ", ignoring GCM");
        	}
        else
        	log ("extras was empty, ignoring GCM");
        
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent (intent);
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
    		log ("ignoring " + why);
    		return;
    		}
    	String msg = bundle.toString();
    	String text = bundle.getString ("message");
    	text = util.decodeURIComponent (text);
    	
    	String app_name = getResources().getString (R.string.app_name);
    	
    	String mso = null;
    	String channel_id = null;
    	String episode_id = null;
        String title = null;
        
    	String content = bundle.getString ("content");
    	log ("content=" + content);
    	if (content != null)
    		{
    		String fields[] = content.split (":");
    		if (fields.length >= 1)
    			mso = fields[0];
    		if (fields.length >= 2)
    			channel_id = fields[1];
    		if (fields.length >= 3)
    			{
    			episode_id = fields[2];
    			episode_id = episode_id.replaceAll ("^yt", "");
    			}
    		if (fields.length >= 4)
    			{
    			title = util.decodeURIComponent (fields[3]);
    			if (title.equals (""))
    				title = null;
    			}
    		}
    
    	boolean notifications_enabled = true;
    	boolean notify_with_sound = false;
    	boolean notify_with_vibrate = false;
    	
    	File configfile = new File (getFilesDir(), "config.notifications");
    	try
    		{
    	    FileReader reader = new FileReader (configfile);
    	    BufferedReader br = new BufferedReader (reader);
    	    String line = br.readLine();    
    	    while (line != null)
    	    	{
    	    	while (line != null)
    	    		{
    	    		String fields[] = line.split ("\t");
    	    		if (fields.length >= 2)
    	    			{
    	    			log ("config k: " + fields[0] + " v: " + fields[1]);
    					if (fields[0].equals ("notifications"))
    						notifications_enabled = fields[1].equals ("on");
    					if (fields[0].equals ("notify-with-sound"))
    						notify_with_sound = fields[1].equals ("on");
    					if (fields[0].equals ("notify-with-vibrate"))
    						notify_with_vibrate = fields[1].equals ("on");	
    	    			}
    	    		line = br.readLine();    	    		
    	    		}
    	    	}
    	    reader.close();
    		}
    	catch (IOException ex)
    		{
    	    ex.printStackTrace();
    		}
    	   
    	if (notifications_enabled)
	    	{
	        mNotificationManager = (NotificationManager) getSystemService (Context.NOTIFICATION_SERVICE);
	
	        Intent start_intent = new Intent (this, start.class);
	        start_intent.setAction ("tv.tv9x9.player.notify");
	        
	        if (mso != null)
	        	start_intent.putExtra ("mso", mso);
	        
	        if (channel_id != null)
	        	start_intent.putExtra ("channel", channel_id);
	        
	        if (episode_id != null)
	        	start_intent.putExtra ("episode", episode_id);
	     
	        if (text != null)
	        	start_intent.putExtra ("message", text);
	        
	        log ("start_intent mso=" + mso + " channel=" + channel_id + " episode=" + episode_id);
	        
	        PendingIntent pi = PendingIntent.getActivity (this, 0, start_intent, PendingIntent.FLAG_CANCEL_CURRENT);
	        
	        String icon_name = getResources().getString (R.string.app_icon);	
	        String fields[] = icon_name.split ("/");
	        icon_name = fields [fields.length - 1];
	        icon_name = icon_name.replaceAll ("\\.[a-z]+$", "");        
			int icon_id = getResources().getIdentifier (icon_name, "drawable", getPackageName());
	        log ("icon=" + icon_name + " id=" + icon_id);
	        log ("payload=" + msg);
	        log ("text=" + text);
	        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder (this)
	        .setContentTitle (title != null ? title : app_name)
	        .setStyle (new NotificationCompat.BigTextStyle()
	        .bigText (text))
	        .setContentText (text)
	        .setSmallIcon (icon_id)
	        .setAutoCancel (true);
	
	        mBuilder.setContentIntent (pi);
	        
	        mNotificationManager.cancelAll();
	        mNotificationManager.notify (NOTIFICATION_ID, mBuilder.build());
	        
	        if (notify_with_sound)
	        	{
	        	log ("notify with sound");
	        	play_sound ("oven.mp3");
	        	}
	        
	        if (notify_with_vibrate)
	        	{	
	        	log ("notify with vibrate");
                Vibrator v = (Vibrator) getSystemService (Context.VIBRATOR_SERVICE);
                v.vibrate (500);
	        	}
	    	}
    	else
    		log ("notifications are NOT enabled!");
    	    	
        File dir = new File (getFilesDir(), "notifications");
        try
        	{
        	if (!dir.exists())
        		dir.mkdir();
        	}
        catch (Exception e)
			{
        	log ("Error creating directory: " + dir);
        	return;
			}
        
        String note = "";
        if (mso != null)
        	note += "mso" + "\t" + mso + "\n";
        if (channel_id != null)
        	note += "channel" + "\t" + channel_id + "\n";
        if (episode_id != null)
        	note += "episode" + "\t" + episode_id + "\n";
        if (title != null)
        	note += "title" + "\t" + title + "\n";
        if (text != null)
        	note += "text" + "\t" + text + "\n";

        log ("NOTE: " + note);
        String filename = "" + new Date().getTime();
        
        try
	        {
	        File notefile = new File (getFilesDir() + "/notifications", filename);
	        log ("writing to: " + notefile);
	        BufferedWriter output = new BufferedWriter (new FileWriter (notefile));
	        output.write (note);
	        output.close();
	        }
        catch (IOException ex)
        	{
        	ex.printStackTrace();
        	}
    	}
    
    MediaPlayer mp = null;
    
	public void play_sound (String asset_filename)
		{
		if (mp != null)
			{
			mp.release();
			}
		
		mp = new MediaPlayer();
		
		AssetFileDescriptor descriptor = null;
		try
			{
			descriptor = getAssets().openFd (asset_filename);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			};
	
		if (descriptor == null)
			{
			mp.release();
			return;
			}
		
		try
			{
			mp.setDataSource (descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mp.release();
			return;
			}
	
		try 
			{
			mp.setAudioStreamType (AudioManager.STREAM_NOTIFICATION);
			mp.prepare();
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mp.release();
			}
		
		try
			{
		    descriptor.close();	
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mp.release();
			return;
			}
	    
	    mp.start();
	    mp.setOnCompletionListener (new OnCompletionListener()
	    	{
	        @Override
	        public void onCompletion(MediaPlayer mp)
	        	{
	            mp.release();
	            log ("sound notification completed");	        	}
	    	});
		}        
}