package tv.tv9x9.player;

import java.io.File;
import java.util.Locale;

import tv.tv9x9.player.switchboard.LocalBinder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RelayActivity extends Activity
	{
	/* 0=unstarted, 2=waiting for service, 3=started */
	int started = 0;

	boolean mBound = false;
	switchboard mService;

	metadata config = null;
	boolean dongle_mode = false;
	
	final Handler in_main_thread = new Handler ();
	
	LayoutInflater inflater = null;

	String identity = "service";
	
	public void log (String text)
		{
		Log.i ("vtest", "[" + identity + "] " + text);
		}
	
	int pixels_2 = 2;
	int pixels_3 = 3;
	int pixels_5 = 5;	
	int pixels_6 = 6;
	int pixels_7 = 7;	
	int pixels_10 = 10;
	int pixels_15 = 15;	
	int pixels_20 = 20;
	int pixels_25 = 25;
	int pixels_30 = 30;
	int pixels_50 = 50;	
	int pixels_60 = 60;	
	int pixels_80 = 80;		
	int pixels_100 = 100;	
	int pixels_120 = 120;		
	int pixels_150 = 150;			
	int pixels_200 = 200;
	
	double screen_inches = 0L;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);

		device_parameters();		
		calculate_pixels();
		
		requestWindowFeature (Window.FEATURE_NO_TITLE);
		
		/* save this, to avoid passing context many layers deep */
		inflater = getLayoutInflater();
		
		if (mBound)
			{
			started = 3;
			log ("switchboard service ready, starting");
			ready();
			}
		else
			{
			log ("waiting for switchboard service");
			started = 2;
			}
		
		
		Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
			{
		    @Override
		    public void uncaughtException (Thread thread, Throwable throwable)
		    	{
		    	log ("Uncaught exception!");
		    	throwable.printStackTrace();
			    finish();
		    	}
			});				
		}
	
	public void calculate_pixels()
		{
		DisplayMetrics dm = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics (dm);

	    pixels_2   = (int) (2   * dm.density);
	    pixels_3   = (int) (3   * dm.density);
	    pixels_5   = (int) (5   * dm.density);	    
	    pixels_6   = (int) (6   * dm.density);
	    pixels_7   = (int) (7   * dm.density);	    
	    pixels_10  = (int) (10  * dm.density); 
	    pixels_15  = (int) (15  * dm.density); 	    
	    pixels_20  = (int) (20  * dm.density);
	    pixels_25  = (int) (25  * dm.density);	    
	    pixels_30  = (int) (30  * dm.density); 
	    pixels_50  = (int) (50  * dm.density); 	    
	    pixels_60  = (int) (60  * dm.density); 
	    pixels_80  = (int) (80  * dm.density); 	    
	    pixels_100 = (int) (100 * dm.density);	    
	    pixels_120 = (int) (120 * dm.density);	
	    pixels_150 = (int) (150 * dm.density);	 	    
	    pixels_200 = (int) (200 * dm.density);
		}

	public void device_parameters()
		{
		try
			{
			DisplayMetrics dm = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics (dm);
		    double x = Math.pow (dm.widthPixels / dm.xdpi, 2);
		    double y = Math.pow (dm.heightPixels / dm.ydpi, 2);
		    screen_inches = Math.sqrt (x+y);
			}
		catch (Exception ex)
			{
			/* nothing */
			}
		}
	
	@Override
	protected void onStart()
		{
		super.onStart();
		log ("onStart");
		Intent intent = new Intent (this, switchboard.class);
		bindService (intent, mConnection, Context.BIND_AUTO_CREATE);
		}
	
	@Override
	protected void onStop()
		{
		super.onStop();
		log ("onStop");
		if (mBound)
			{
			unbindService (mConnection);
			mBound = false;
			}
		}
	
	@Override
	protected void onDestroy()
		{
		log ("onDestroy");

		if (mService != null)
			{
			mService.relay_post ("RELEASE");
			mService.unset_callbacks (identity);
			}

		super.onDestroy();
		}
	
	@Override
	public void onResume()
		{
		super.onResume();		
		
		if (config != null)
			{
			if (config.fresh_signin)
				onRelayActivitySignin();
			else if (config.fresh_signup)
				onRelayActivitySignup();
			else if (config.fresh_signout)
				onRelayActivitySignout();
			else if (config.fresh_fb_signout)
				{
				log ("onResume: user has signed out from Facebook");
				signout_inner();
				}
			config.fresh_signup = config.fresh_signin = config.fresh_signout = config.fresh_fb_signout = false;
			}
		}

	private ServiceConnection mConnection = new ServiceConnection ()
		{
		@Override
		public void onServiceConnected (ComponentName className, IBinder service)
			{
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService ();
			mBound = true;
			if (started == 2)
				{
				log ("switchboard service connected");
				started = 3;
				ready();
				}
			}

		@Override
		public void onServiceDisconnected (ComponentName arg0)
			{
			mBound = false;
			log ("switchboard service disconnected");
			}
		};
			
	public void exit_stage_left()
		{
		finish();
		}
	
	public void alert (String message)
		{
		toast (message);
		}
		
	public void toast (String text)
		{
		Toast.makeText (this, text, Toast.LENGTH_LONG).show();
		}
	
	public void toast_by_resource (int id)
		{
		String text = getResources().getString (id);		
		toast (text);
		}

	public void toast_by_resource (int id, String arg1)
		{
		String text = getResources().getString (id);
		text = text.replaceAll ("%1", arg1);
		toast (text);
		}

	public void toast_by_resource (int id, String arg1, String arg2)
		{
		String text = getResources().getString (id);
		text = text.replaceAll ("%1", arg1);
		text = text.replaceAll ("%2", arg2);
		toast (text);
		}
	
	int relay_retries = 0;
	
	public void attach_relay()
		{
		if (mService.is_connected())
			{
			mService.set_callbacks (identity, relay_receive, relay_error);
			announce();
			}
		else
	    	in_main_thread.postDelayed (start_relay_task, relay_retries > 20 ? 30000 : 2000);
		}
		
	public void announce()
		{
		boolean googletv = false;
		if (getPackageManager().hasSystemFeature ("com.google.android.tv"))
			{
			googletv = true;
		    log ("Google TV detected");
			}
		mService.relay_post ("DEVICE " + (googletv ? "GoogleTV" : "AndroidTV"));
		String token = config.rendering_token != null ? config.rendering_token : config.usertoken;
		if (token != null && !token.equals (""))
			mService.relay_post ("RENDERER " + token);
		else
			mService.relay_post ("RENDERER nobody");
		mService.relay_post ("WHOAMI");
		// readout_volume();
		}
	
	final Callback relay_started = new Callback()
		{
		@Override
		public void run_string (String s)
			{
			log ("relay started");
			announce();
			}
		};
	
	final Runnable start_relay_task = new Runnable()
		{
		@Override
		public void run()
			{
			config = mService.get_metadata (identity);
			if (mService.is_connected())
				{
				log ("start_relay_task, already connected, carry on");
				mService.set_callbacks (identity, relay_receive, relay_error);
				}
			else
				mService.open_relay (identity, relay_started, relay_receive, relay_error);
			}
		};
			
	final Callback relay_error = new Callback()
		{
		@Override
		public void run_string (String s)
			{
			log ("relay error: " + s);
			log ("** RECONNECT **");
	    	relay_retries++;
			/* to prevent stack overflow, post this in the main thread, in case this was called from a thread */
	    	in_main_thread.postDelayed (start_relay_task, 500);
			}
		};

	final Callback relay_receive = new Callback()
		{
		@Override
		public void run_string (String s)
			{
			log ("relay received: " + s);
			String fields[] = s.split (" ");
			if (fields[0].equals ("BECOME"))
				{
				log ("RECEIVED: " + s);
				config.rendering_token = fields [1];
				if (fields.length >= 3)
					{
					mService.relay_post ("RENDERER " + config.rendering_token + " " + fields[2]);
					config.rendering_username = fields [2];
					}
				else
					mService.relay_post ("RENDERER " + config.rendering_token);				
				mService.relay_post ("REPORT READY");
				}
			else if (fields[0].equals ("OFF"))
				{
				log ("RECEIVED: " + s);
				config.television_turning_off = true;
				exit_stage_left();
				}			
			else if (fields[0].equals ("YOUARE"))
				{
				log ("RECEIVED: " + s);
				final String devname = fields[1];
				}
			}
		};
	
	protected void ready()
		{
		log ("ready? started=" + started);
		if (started == 3)
			{
			config = mService.get_metadata (identity);
			onRelayActivityReady();
			}
		}
	
	public void onRelayActivityReady()
		{
		/* override this */
		}
	
	@Override
	public void onConfigurationChanged (Configuration newConfig)
		{
		super.onConfigurationChanged (newConfig);
		log ("CONFIGURATION CHANGED!");
		}
	
	public void set_mask()
		{
		FrameLayout vMask = (FrameLayout) findViewById (R.id.mask);
		
		DisplayMetrics dm = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics (dm);
	    
	    FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vMask.getLayoutParams();
	    
	    layout.width = dm.widthPixels;
	    layout.height = dm.heightPixels;
	    
	    vMask.setLayoutParams (layout);
		}	
	
	public void signout()
		{
		signout_inner();
		}
	
	public void signout_inner()
		{
		config.usertoken = null;
		config.username = null;
		config.email = null;
		config.youtube_auth_token = null;
		
		File userf = new File (getFilesDir() + "/user@" + config.api_server);
		userf.delete();
		
		File namef = new File (getFilesDir() + "/name@" + config.api_server);
		namef.delete();		
		
		config.forget_programs_in_channel ("virtual:following");
		config.forget_subscriptions();
	
		String txt_signed_out = getResources().getString (R.string.signed_out_successfully);
		alert (txt_signed_out);
		
		config.fresh_signout = true;
		onRelayActivitySignout();
		}	
	
	public void onRelayActivitySignout()
		{
		/* overide this */
		}

	public void onRelayActivitySignin()
		{
		/* override this */
		}

	public void onRelayActivitySignup()
		{
		/* override this */
		}
	
	public void set_width_of_dialog (float percent)
		{
		LinearLayout vDialog = (LinearLayout) findViewById (R.id.dialog);
		if (vDialog != null)
			{
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vDialog.getLayoutParams();
			
			DisplayMetrics dm = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics (dm);
		    
		    layout.width = (int) ((float) dm.widthPixels * percent);
		    if (screen_inches < 5)
		    	layout.height = (int) ((float) dm.heightPixels * 0.8);
		    
		    vDialog.setLayoutParams (layout);
			}
		}	

	public void set_height_of_dialog (float percent)
		{
		LinearLayout vDialog = (LinearLayout) findViewById (R.id.dialog);
		if (vDialog != null)
			{
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vDialog.getLayoutParams();
			
			DisplayMetrics dm = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics (dm);
		    
	    	layout.height = (int) ((float) dm.heightPixels * percent);
		    
		    vDialog.setLayoutParams (layout);
			}
		}	

	public void share_episode (String channel_id, String episode_id)
		{
		if (config != null && channel_id != null)
			{
			Intent i = new Intent (Intent.ACTION_SEND);
			i.setType ("text/plain");
			i.putExtra (Intent.EXTRA_SUBJECT, "Shared to you from 9x9.tv");
			String mso = config.mso != null ? config.mso : "9x9";
			String server = config.api_server;
			if (mso.equals ("cts") && server.equals ("www.9x9.tv"))
				server = "cts.9x9.tv";
			String url =  "http://" + server + "/view?ch=" + channel_id + "&mso=" + mso;
			if (episode_id != null)
				 url = "http://" + server + "/view?ch=" + channel_id + "&ep=" + episode_id + "&mso=" + mso;
			i.putExtra (Intent.EXTRA_TEXT, url);
			startActivity (Intent.createChooser (i, "Share this 9x9.tv episode"));
			}
		}		
	public void query_following (final Callback callback)
		{
		if (config == null || config.usertoken == null)
			{
			if (callback != null)
				callback.run_string ("virtual:following");
			return;
			}
		
		final String query = "channelLineup?user=" + config.usertoken + "&setInfo=true";
		
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (in_main_thread, config, query)
			{
			public void success (String[] chlines)
				{
				try
					{
					long vc_end = System.currentTimeMillis();
					long elapsed = (vc_end - vc_start) / 1000L;
					log ("[" + query + "] lines received: " + chlines.length + ", elapsed: " + elapsed);
					config.parse_channel_info_with_setinfo (chlines);
					if (callback != null)
						callback.run_string ("virtual:following");
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				alert ("ERROR! " + errtext);
				}
			};
		}	
	
	public String device_type()
		{
		if (dongle_mode)
			return "dongle";
		else if (screen_inches < 5)
			return "phone";
		else
			return "tablet";
		}	
	
	public boolean is_phone()
		{
		return screen_inches < 6;
		}

	public boolean is_large_tablet()
		{
		return screen_inches > 8;
		}	
	
	public String get_language()
		{
		String lang = Locale.getDefault().getLanguage();
		if (lang.equals ("zh") || lang.equals ("tw"))
			lang = "cn";
		log ("get_language: " + lang);
		return lang;
		}	
	}