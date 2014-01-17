package tv.tv9x9.player;

import java.io.File;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Message9x9Manager extends BroadcastReceiver
{
	private final String TAG = "Message9x9Manager";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();

		String display_action = action.replace("com.edgecore.launcher.tv9x9.message9x9.",  "");
		Log.i ("vtest'", "BROADCAST ACTION RECEIVED: " + display_action);
		
		notify_ui (context, display_action);
		
		if(action.equals(Message9x9.MESSAGE_INFO))
		{
			if(intent.hasExtra(Message9x9.EXTRA_DATA_9x9APK))
			{
				// 9x9 apk has newer version

				String version = intent.getStringExtra(Message9x9.EXTRA_DATA_VERSION);

				// tell system to upgrade 9x9 apk
				// upgrade9x9Apk(context, version);
				
				ack (context); /* <-- this ack seems unnecessary */
				request_upgrade_from_user (context, version, Message9x9.EXTRA_DATA_9x9APK);
			}
			else if(intent.hasExtra(Message9x9.EXTRA_DATA_SYSTEM))
			{
				// system image has a newer version

				String version = intent.getStringExtra(Message9x9.EXTRA_DATA_VERSION);

				// tell system to upgrade system image
				// upgradeSystemImage(context, version);
				
				ack (context); /* <-- this ack seems unnecessary */
				request_upgrade_from_user (context, version, Message9x9.EXTRA_DATA_SYSTEM);
			}
			else if(intent.hasExtra(Message9x9.EXTRA_DATA_SYS_SUSPEND))
			{
				// system is going to suspend

				doSave9x9Data (context);

				// tell system that 9x9 is ready to suspend

				readyToSuspend(context);
			}
			else if(intent.hasExtra(Message9x9.EXTRA_DATA_SYS_RESUME))
			{
				// system already wakeup

				doRestore9x9Data(); // FIXME: unimplemented, 9x9 should add real code in this function
			}
			else if(intent.hasExtra(Message9x9.EXTRA_DATA_CLEAR_DONE))
			{
				// system already clear cache data
			}
			else if(intent.hasExtra(Message9x9.EXTRA_DATA_SET_DONE))
			{
				// system network or screen setting is done
			}
		}
		else
		{
			// Received unexpected intent
		}
	}

	public void doSave9x9Data (Context context)
		{
		Log.i ("vtest", "making checkpoint live");
		Context ctx = context.getApplicationContext();
		File checkpoint = new File (ctx.getFilesDir() + "/checkpoint");
		File live_checkpoint = new File (ctx.getFilesDir() + "/checkpoint.live");
		checkpoint.renameTo (live_checkpoint);
		}
	
	public void doRestore9x9Data()
		{
		}
	
	public void readyToSuspend(Context context)
		{
		Intent intent = new Intent (Message9x9.MESSAGE_INFO);
		intent.putExtra (Message9x9.EXTRA_DATA_APK_READY, 0);
		context.sendBroadcast (intent);
		}

	public void ack (Context context)
		{
		Intent intent = new Intent (Message9x9.MESSAGE_INFO);
		intent.putExtra (Message9x9.EXTRA_DATA_APK_READY, 0);
		context.sendBroadcast (intent);
		}
	
	public void upgrade9x9Apk(Context context, String version)
	{
		Intent intent = new Intent(Message9x9.MESSAGE_UPGRADE);
		intent.putExtra(Message9x9.EXTRA_DATA_9x9APK, version);

		context.sendBroadcast(intent);
	}

	public void upgradeSystemImage(Context context, String version)
		{
		Intent intent = new Intent(Message9x9.MESSAGE_UPGRADE);
		intent.putExtra(Message9x9.EXTRA_DATA_SYSTEM, version);
		context.sendBroadcast(intent);
		}

	public void clearSystemCache(Context context)
	{
		Intent intent = new Intent(Message9x9.MESSAGE_CLEAR);
		intent.putExtra(Message9x9.EXTRA_DATA_CLEAR_CACHE, 0);

		context.sendBroadcast(intent);
	}

	public void setupSystemNetwork(Context context)
	{
		Intent intent = new Intent(Message9x9.MESSAGE_SET);
		intent.putExtra(Message9x9.EXTRA_DATA_SET_NET, 0);

		context.sendBroadcast(intent);
	}

	public void setupSystemScreen(Context context)
	{
		Intent intent = new Intent(Message9x9.MESSAGE_SET);
		intent.putExtra(Message9x9.EXTRA_DATA_SET_SCREEN, 0);

		context.sendBroadcast(intent);
	}

	public void switchTo9x9Home(Context context)
	{
		Intent intent = new Intent(Message9x9.MESSAGE_SWITCH);
		intent.putExtra(Message9x9.EXTRA_DATA_SWITCH_HOME, 0);

		context.sendBroadcast(intent);
	}
	
	public void notify_ui (Context c, String s)
		{
		/*
		Intent wIntent = new Intent (c, popupalert.class);
		wIntent.putExtra ("tv.9x9.action", "RC MESSAGE: " + s);
		wIntent.addFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity (wIntent);
		*/
		}
	
	public void request_upgrade_from_user (Context c, String version, String action)
		{
		/*
		Intent wIntent = new Intent (c, upgrade.class);
		wIntent.putExtra ("tv.9x9.action", action);
		wIntent.putExtra ("tv.9x9.version", version);
		wIntent.addFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity (wIntent);
		*/
		}
}