package tv.tv9x9.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.os.Handler;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmSetup
	{
	/* Google Cloud Messaging */
	
	Context ctx = null;
	metadata config = null;
	Handler in_main_thread = null;
	
    GoogleCloudMessaging gcm = null;
    
    String EXTRA_MESSAGE = "message";
    String PROPERTY_REG_ID = "registration_id";
    String PROPERTY_APP_VERSION = "appVersion";
    
    /* this number is arbitrarily 9000, but must be unique to the calling activity! */
    int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    
    GcmSetup (Context ctx, metadata config, Handler in_main_thread)
    	{
    	this.ctx = ctx;
    	this.config = config;
    	this.in_main_thread = in_main_thread;
    	}
    
	public void log (String text)
		{
		Log.i ("vtest", "[gcm] " + text);
		}
	
	public void gcm_register()
		{		
		if (config != null && config.gcm_sender_id != null)
			{
	        gcm = GoogleCloudMessaging.getInstance (ctx);
	        String regid = get_gcm_registration_id (ctx.getApplicationContext());

	        if (regid.isEmpty())
	        	register_in_background();
			}
		}
		
	public String get_gcm_registration_id (Context context)
		{
	    SharedPreferences prefs = get_gcm_preferences (context);
	    
	    String registration_id = prefs.getString (PROPERTY_REG_ID, "");
	    if (registration_id.isEmpty())
	    	{
	        log ("gcm: registration not found");
	        return "";
	    	}
	    
	    int registered_version = prefs.getInt (PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int current_version = get_app_version (context);
	    if (current_version >= 0 && registered_version != current_version)
	    	{
	        log ("gcm: App version changed");
	        return "";
	    	}
	    
	    log ("gcm: obtained saved GCM id: " + registration_id);
	    return registration_id;
		}
	
	private SharedPreferences get_gcm_preferences (Context context)
		{
	    return ctx.getSharedPreferences (main.class.getSimpleName(), Context.MODE_PRIVATE);
		}
	
	private void store_registration_id (Context context, String regId)
		{
	    final SharedPreferences prefs = get_gcm_preferences (context);
	    int appVersion = get_app_version (context);
	    log ("gcm: saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString (PROPERTY_REG_ID, regId);
	    editor.putInt (PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
		}
	
	public int get_app_version (Context context)
		{
		try
			{
	        PackageInfo packageInfo = context.getPackageManager().getPackageInfo (context.getPackageName(), 0);
	        return packageInfo.versionCode;
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			return -1;
			}
		}
	
	public void register_in_background()
		{
        try
        	{
        	final Context context = ctx.getApplicationContext();
                
    		Thread t = new Thread()
				{
				public void run()
					{
					log ("in a thread");
					
					gcm = GoogleCloudMessaging.getInstance (context);
					
		            log ("trying to register with GCM sender id: " + config.gcm_sender_id);                     
		            String regid = null;
					try
						{
						regid = gcm.register (config.gcm_sender_id);
						}
					catch (Exception ex)
						{
						ex.printStackTrace();
						return;
						}
		            log ("gcm: device registered, registration ID=" + regid);
		            store_registration_id (ctx.getApplicationContext(), regid);
		            
					// http://api.flipr.tv/playerAPI/deviceRegister?mso=crashcourse&type=gcm&token=xxxxxxxx
		    		new playerAPI (in_main_thread, config, "deviceRegister?type=gcm&token=" + regid)
						{
						public void success (String[] lines)
							{
							log ("gcm: successfully registered GCM on 9x9 server");
							}
						public void failure (int code, String errtext)
							{
							log ("gcm: failure registering GCM: " + errtext);
							}
						};
					}
				};
			
			t.start();
        	}
        catch (Exception ex)
        	{
        	ex.printStackTrace();
        	}
		}
	}