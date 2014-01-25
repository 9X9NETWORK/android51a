package tv.tv9x9.player;

import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import tv.tv9x9.player.switchboard.LocalBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class start extends Activity
	{
	/* 0=unstarted, 2=waiting for service, 3=started */
	int started = 0;

	boolean mBound = false;
	switchboard mService;

	private metadata config = null;

	GoogleAnalyticsTracker tracker = null;
	
	boolean tablet = false;
	
	double screen_inches = 0;
	
	/* if started with a flipr://... */
	Uri URI_for_launch = null;
	
	String identity = "start";
	
	public void log (String text)
		{
		Log.i ("vtest", "[" + identity + "] " + text);
		}
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);

		requestWindowFeature (Window.FEATURE_NO_TITLE);
		device_parameters();

		setContentView (R.layout.splashlogo);
		
		customize_splash_screen();		
		adjust_layout();
		
		if (mBound)
			{
			started = 3;
			Log.i ("vtest", "switchboard service ready, starting");
			ready();
			}
		else
			{
			Log.i ("vtest", "waiting for switchboard service");
			started = 2;
			}
		
		Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
			{
		    @Override
		    public void uncaughtException (Thread thread, Throwable throwable)
		    	{
		    	Log.i ("vtest", "Uncaught exception!");
		    	throwable.printStackTrace();
			    finish();
		    	}
			});
		
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.setDebug (true);
		
		// android.intent.action.VIEW
		
		Intent intent = getIntent();
		String action = intent.getAction();
	    
	    Uri uri = intent.getData();
	    
	    if (action.equals (Intent.ACTION_VIEW) && uri != null)
	    	{
	    	if (uri.getScheme().contains ("flipr") || uri.getPath().contains ("redirect"))
		    	{
		    	URI_for_launch = uri;
		    	log ("URL host: " + uri.getHost());
		    	log ("URL path: " + uri.getPath());
		    	log ("URL query: " + uri.getQuery());
		    	}
	    	}
		}
	
	@Override
	protected void onStart()
		{
		super.onStart();
		Intent intent = new Intent (this, switchboard.class);
		bindService (intent, mConnection, Context.BIND_AUTO_CREATE);
		FlurryAgent.onStartSession (this, "648QZK9W54HCPMBHGZ4M");
		}

	@Override
	protected void onStop ()
		{
		super.onStop();
		FlurryAgent.onEndSession (this);
		}

	@Override
	protected void onDestroy ()
		{
		super.onDestroy();
		if (mBound)
			{
			mService.relay_post ("QUIT");
			mService.close_relay();
			unbindService (mConnection);
			mBound = false;
			}
		}
	
	@Override
	protected void onResume()
		{
		super.onResume();
		Log.i ("vtest", "[start] onResume");
		
		if (config != null && config.television_turning_off)
			{
			Log.i ("vtest", "television turning off -- exit");
			finish();
			return;
			}
		
		if (mService != null)
			mService.set_callbacks ("start", relay_receive, relay_error);
		
		if (config != null && config.future_action != null)
			{
			String future_action = config.future_action;
			config.future_action = null;
			
			Log.i ("vtest", "EXECUTING FUTURE ACTION: " + future_action);
						
			if (future_action.equals ("tv"))
				launch_tv();
			else if (future_action.equals ("tvportal"))
				launch_tvportal();			
			else if (future_action.equals ("tablet"))
				launch_tablet();
			else if (future_action.equals ("portal") || future_action.equals ("portola"))
				launch_portal();
			else if (future_action.equals ("restart"))
				launch();
			}
		
		try
			{
			// com.facebook.Settings.publishInstallAsync (getBaseContext(), "110847978946712");
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
	private ServiceConnection mConnection = new ServiceConnection()
		{
		@Override
		public void onServiceConnected (ComponentName className, IBinder service)
			{
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService ();
			mBound = true;
			if (started == 2)
				{
				started = 3;
				ready();
				}
			}

		@Override
		public void onServiceDisconnected (ComponentName arg0)
			{
			mBound = false;
			}
		};
		
	@Override
	public void onConfigurationChanged (Configuration newconfig)
		{
	    super.onConfigurationChanged (newconfig);
		}
	
	@Override
	public boolean onCreateOptionsMenu (Menu menu)
		{
		// menu.add ("Settings").setIcon (android.R.drawable.ic_menu_preferences).setIntent (new Intent (this, settings.class));
		// menu.add ("Become a TV").setIntent (new Intent (this, tvgrid.class));
		// menu.add ("Grid").setIntent (new Intent (this, tvgrid.class));
		// menu.add ("dev2");
		// menu.add ("beagle");
		// menu.add ("www");
		// menu.add ("stage");
		// menu.add ("Shear").setIntent (new Intent (this, shear.class));
		return true;
		}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
		{
		Log.i ("vtest", "menu item: " + item.getItemId () + " title: " + item.getTitle ());

		if (item.getTitle().equals ("Remote"))
			{
			String app_name = "com.soylent.rc";
			Intent i = this.getPackageManager().getLaunchIntentForPackage (app_name);
			if (i != null)
				this.startActivity (i);
			else
				alert ("application not available: " + app_name);
			return true;
			}
		
		return super.onOptionsItemSelected (item);
		}
		
	public void device_parameters()
		{
		try
			{
			Log.i ("vtest", "MODEL: " + android.os.Build.MODEL);
			DisplayMetrics dm = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics (dm);
		    double x = Math.pow (dm.widthPixels / dm.xdpi, 2);
		    double y = Math.pow (dm.heightPixels / dm.ydpi, 2);
		    screen_inches = Math.sqrt (x+y);
		    Log.i ("vtest", "screen size in pixels: width=" + dm.widthPixels + " height=" + dm.heightPixels);
		    Log.i ("vtest", "screen size in inches : " + screen_inches);
		    
		    tablet = ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE);
			}
		catch (Exception ex)
			{
			/* nothing */
			}
		}

	public void launch_tv()
		{
		// Log.i ("vtest", "becoming a television!");
		// launch_inner (tvgrid.class);
		}
	
	public void launch_tablet()
		{
		// launch_inner (home.class);
		}

	public void launch_portal()
		{
		}
	
	public void launch_portola()
		{
		}
	
	public void launch_tvportal()
		{
		}

	public void launch_home()
		{
		launch_inner (main.class);
		}
	
	public void launch_inner (Class <?> c)
		{
		Intent wIntent = new Intent (this, c);
		if (URI_for_launch != null)
			{
	    	String url = URI_for_launch.getScheme() + "://"
	    		+ URI_for_launch.getHost() + URI_for_launch.getPath() + "?" + URI_for_launch.getQuery();
	    	log ("LAUNCH URL: " + url);
	    	wIntent.putExtra ("tv.9x9.url", url);
			}
		startActivity (wIntent);
		}
	
	public void read_config_file()
		{
		String config_data = futil.read_file (this, "config");
		if (!config_data.startsWith ("ERROR:"))
			{
			String lines[] = config_data.split ("\n");
			for (String line: lines)
				{
				String fields[] = line.split ("\t");
				if (fields.length >= 2)
					{
					if (fields[0].equals ("api-server"))
						config.api_server = fields[1];
					if (fields[0].equals ("region"))
						config.region = fields[1];
					// if (fields[0].equals ("supported-region"))
					//		config.supported_region = fields[1];
					}
				}
			}
		}
	
	public void customize_splash_screen()
		{
		FrameLayout vSplash = (FrameLayout) findViewById (R.id.splash);		
		ImageView vLogo = (ImageView) findViewById (R.id.logo);
		TextView vSlogan = (TextView) findViewById (R.id.slogan);
		
		String splash_bg = getResources().getString (R.string.splash_background);
		String splash_fg = getResources().getString (R.string.splash_foreground);		
		String splash_slogan = getResources().getString (R.string.splash_slogan);	

		if (splash_bg != null && splash_fg != null)
			{
			int bg_id = getResources().getIdentifier (splash_bg, "drawable", getPackageName());
			int fg_id = getResources().getIdentifier (splash_fg, "drawable", getPackageName());		
			
			if (bg_id != 0 && fg_id != 0)
				{
				vSplash.setBackgroundResource (bg_id);
				vLogo.setImageResource (fg_id);
				}
			}
		
		if (splash_slogan != null && !splash_slogan.equals (""))
			{
			vSlogan.setText (splash_slogan);
			vSlogan.setVisibility (View.VISIBLE);
			}
		else
			vSlogan.setVisibility (View.GONE);
		}
	
	public void adjust_layout()
		{
		DisplayMetrics dm = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics (dm);
	    
		ImageView vLogo = (ImageView) findViewById (R.id.logo);
		LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vLogo.getLayoutParams();
		
		layout.height = (int) (dm.heightPixels * 0.4);

		vLogo.setLayoutParams (layout);
		
		if (screen_inches < 5)
			{
			TextView vSlogan = (TextView) findViewById (R.id.slogan);
			if (vSlogan != null)
				vSlogan.setTextSize (TypedValue.COMPLEX_UNIT_SP, 24);
			}
		}
	
	long start_time = System.currentTimeMillis();
	
	public void ready()
		{
		config = mService.get_metadata ("start");
		
		if (URI_for_launch != null && URI_for_launch.getHost() != null)
			{
			if (URI_for_launch.getHost().equals ("beagle.9x9.tv"))
				{
				config.api_server = "beagle.9x9.tv";
				log ("API server set to: " + config.api_server);
				}
			}
		
		config.usertoken = null;
		config.username = null;
		
		config.tracker = tracker;
		
		config.white_label = getResources().getString (R.string.white_label);
		config.mso = getResources().getString (R.string.mso);
		config.region = getResources().getString (R.string.default_region);		
		config.app_name = getResources().getString (R.string.app_name);
		
		if (config.white_label.contains ("portal"))
			{
			/* unless overridden by configuration file */
			config.api_server = "www.9x9.tv";
			}
		
		Log.i ("vtest", "white label: " + config.white_label);
		Log.i ("vtest", "mso: " + config.mso);
		
		read_config_file();
		
		if (config.tracker != null)
			{
			Log.i ("vtest", "starting Google Analytics");
			// config.tracker.startNewSession ("UA-37212670-1", 30, this);
			// config.tracker.startNewSession ("UA-37212670-1", this);
			// config.tracker.startNewSession ("UA-21595932-3", this); web
			// config.tracker.startNewSession ("UA-37214861-1", this); iOS
			// config.tracker.startNewSession ("UA-37212670-2", this);
			config.tracker.startNewSession ("UA-21595932-1", this);
			
			config.tracker.setCustomVar (1,  "x",  "y", 2);
			config.tracker.trackPageView ("/" + config.api_server + "/start");
			config.tracker.dispatch();
			}

		/* if the brandInfo takes over 3 seconds, display a spinner */
		in_main_thread.postDelayed (new Runnable()
			{
			public void run()
				{
				View vProgress = findViewById (R.id.progress);
				vProgress.setVisibility (View.VISIBLE);
				}
			}, 3300);
			
		new playerAPI (in_main_thread, config, "brandInfo")
			{
			public void success (String[] chlines)
				{
				process_brandinfo (chlines);
				delayed_launch();
				}

			public void failure (int code, String errtext)
				{				
				Log.i ("vtest", "brandInfo error: " + errtext);
				delayed_launch();
				}
			};
		}

	public void delayed_launch()
		{
		long end_time = System.currentTimeMillis();
		
		if (end_time - start_time < 3000)
			{
			long delay = 3000 - (end_time - start_time);
			Log.i ("vtest", "delayed launch: " + delay);
			in_main_thread.postDelayed (new Runnable()
				{
				public void run()
					{
					Log.i ("vtest", "delayed launch!");
					launch();
					in_main_thread.postDelayed (new Runnable()
						{
						public void run()
							{
							thumbnail.purge_old_thumbnails (getBaseContext(), config);
							}
						}, 10000);
					}
				}, delay);
			}
		else
			launch();
		}
	
	public void process_brandinfo (String lines[])
		{
		for (String line: lines)
			{
			Log.i ("vtest", "brandInfo: " + line);
			String fields[] = line.split ("\t");
			if (fields.length >= 2)
				{
				if (fields[0].equals ("supported-region"))
					config.supported_region = fields[1];
				if (fields[0].equals ("title"))
					config.mso_title = fields[1];
				if (fields[0].equals ("preferredLangCode"))
					config.mso_preferred_lang_code = fields[1];				
				if (fields[0].equals ("video"))
					config.tutorial_video = fields[1];
				}
			}
		}
	
	public void launch()
		{
		launch_home();
		}
	
	public void OLD_launch()
		{
		if (util.is_a_tv (getBaseContext()))
			launch_tvportal();
		else if (config.white_label.equals ("tvportal") || config.white_label.endsWith ("-portal"))
			launch_tvportal();
		else if (config.white_label.equals ("original-style"))
			launch_tablet();
		else
			launch_portola();
		}
	
	final static Handler in_main_thread = new Handler();

	public void alert (String text)
		{
		Log.i ("vtest", "[start] ALERT: " + text);
		
		AlertDialog.Builder builder = new AlertDialog.Builder (start.this);

		builder.setMessage (text);
		builder.setNeutralButton ("OK", null);

		builder.create().show();
		}

	public void toast (String text)
		{
	 	Toast.makeText (getBaseContext (), text, Toast.LENGTH_SHORT).show();
		}
	
	/* to decrease number of attempts to connect, if the relay isn't there */
	int relay_retries = 0;
	
	public void start_relay()
		{
		if (mService.is_connected())
			{
			Log.i ("vtest", "start: attach to relay");
			mService.set_callbacks ("start", relay_receive, relay_error);
			}
		else
			{
			Log.i ("vtest", "start: start relay");
			/* to prevent stack overflow, post this in the main thread, in case this was called from a thread */
			in_main_thread.postDelayed (start_relay_task, relay_retries > 20 ? 30000 : 2000);
			}
		}
	
	final Runnable start_relay_task = new Runnable()
		{
		@Override
		public void run()
			{
			Log.i ("vtest", "[start] start_relay_task");
			config = mService.get_metadata ("start");
			if (mService.is_connected())
				Log.i ("vtest", "[start] start_relay_task -- am connected, carry on");
			else
				mService.open_relay ("start", relay_started, relay_receive, relay_error);
			}
		};
		
	final Callback relay_started = new Callback()
		{
		@Override
		public void run_string (String s)
			{
			Log.i ("rc", "start: relay started");
			mService.relay_post ("DEVICE Android");
			mService.relay_post ("WHO");
			}
		};
	
	final Callback relay_error = new Callback()
		{
		@Override
		public void run_string (String s)
			{
			Log.i ("vtest", "start: relay error: " + s);
			Log.i ("vtest", "** start: RECONNECT **");
			relay_retries++;
			start_relay();
			}
		};
	
	final Callback relay_receive = new Callback()
		{
		@Override
		public void run_string (String s)
			{
			Log.i ("vtest", "start: relay received: " + s);
			}
		};
	}