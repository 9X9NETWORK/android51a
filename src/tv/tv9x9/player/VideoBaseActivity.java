package tv.tv9x9.player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.tv9x9.player.main.menuitem;
import tv.tv9x9.player.switchboard.LocalBinder;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger.LogLevel;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.flurry.android.FlurryAgent;

import com.google.android.youtube.player.YouTubePlayer;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* black screen video: wfaeWDpiX84 */

public class VideoBaseActivity extends FragmentActivity implements YouTubePlayer.OnFullscreenListener, OnPlayerListener
	{
	/* 0=unstarted, 2=waiting for service, 3=started 4=started+initialized */
	int started = 0;

	boolean mBound = false;
	switchboard mService;

	metadata config = null;
	boolean dongle_mode = false;
	
	String black_video_id = "wfaeWDpiX84";
	
	String player_real_channel = null;

	int subepisodes_this_episode = 0;
	int current_subepisode = 0;

	String program_line[] = null;
	int current_episode_index = 1;
	
	String youtube_url = null;

	public boolean fullscreen = false;
	
	Timer progress_timer = null;
	Timer countdown_timer = null;
	
	final Handler in_main_thread = new Handler ();
	
	double screen_inches = 0L;
	int screen_width = 0;
	int screen_height = 0;
	float screen_density = 1f;
	
	boolean video_play_pending = false;
	
	boolean osd_visible_in_full_screen = true;
	
	String requested_episode = null;
	
	/* set of channels, for up/down flipping */
	String arena[] = null;
	
	LayoutInflater inflater = null;
	
	/* to prevent machine-gunning of videos as multiple onStopped is called when videos are unavailable */
	boolean video_has_started = false;
	
	/* to prevent starting videos while leaving activity, which causes a crash */
	boolean exit_in_progress = false;
	
	/* are we currently playing a titlecard, and not a video? */
	boolean playing_begin_titlecard = false;
	boolean playing_end_titlecard = false;
	
	/* time to force end of video, or -1 to play until natural ending */
	long video_cutoff_time = -1;
	
	/* time for next POI trigger */
	long video_next_trigger = -1;

	/* time to remove OSD for most recent POI */
	long video_release_trigger = -1;
	
	/* the index number of the POI (for debugging purposes) */
	int video_trigger_id = -1;
	
	/* with above. This is a hack to re-kick the video when trying to restart it upon return to the app. It seems that
	   sometimes after an app launches on low-memory devices, the YouTube API has problems */

	boolean pending_restart = false;
	
	String identity = "service";
	
	/* to detect if login has changed */
	String saved_usertoken = null;
	
	/* if a problem with a view, don't want to repeatedly start videos */
	boolean video_systemic_error = false;
	
	/* another player is being launched (probably POI) */
	boolean launch_in_progress = false;
	
	/* how many pixels before we decide user is doing a horizontal drag. see calculate_pixels() for adjusted value */
	int h_movement_threshhold = 125;

	/* how many pixels before we decide user is doing a vertical drag. see calculate_pixels() for adjusted value */
	int v_movement_threshhold = 125;
	
	public enum flipdir { FORWARD, REVERSE };
	
	flipdir flip_direction = flipdir.FORWARD;
	
	String cumulative_channel_id = null;
	String cumulative_episode_id = null;
	long cumulative_episode_time = 0;
	long cumulative_channel_time = 0;
	
	Player player = null;
	
	public void accumulate_episode_time (long t)
		{
		cumulative_episode_time += t;
		}
	
	/* dm.density shortcut */
	float density = 1f;

	static int pixels_2 = 2;
	static int pixels_3 = 3;	
	static int pixels_4 = 4;
	static int pixels_5 = 5;
	static int pixels_6 = 6;	
	static int pixels_7 = 7;	
	static int pixels_8 = 8;
	static int pixels_10 = 10;
	static int pixels_12 = 12;	
	static int pixels_15 = 15;	
	static int pixels_20 = 20;
	static int pixels_24 = 24;		
	static int pixels_25 = 25;	
	static int pixels_30 = 30;
	static int pixels_32 = 32;
	static int pixels_35 = 35;
	static int pixels_40 = 40;	
	static int pixels_50 = 50;		
	static int pixels_60 = 60;
	static int pixels_70 = 70;
	static int pixels_80 = 80;		
	static int pixels_100 = 100;		
	static int pixels_120 = 120;
	static int pixels_130 = 130;
	static int pixels_140 = 140;
	static int pixels_150 = 150;
	static int pixels_160 = 160;	
	static int pixels_200 = 200;
	static int pixels_300 = 300;
	
	public VideoFragment videoFragment = null;
	public PlayerFragment playerFragment = null;
	
	PlasterCast plasterCast = null;
	
	public void log (String text)
		{
		Log.i ("vtest", "[" + identity + "] " + text);
		}

	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);

		calculate_pixels();
		device_parameters();
		
		requestWindowFeature (Window.FEATURE_NO_TITLE);

		setContentView (R.layout.main);
		
		plasterCast = new PlasterCast (plasterListener);
		
		/* save this, to avoid passing context many layers deep */
		inflater = getLayoutInflater();
		
		FrameLayout vMain = (FrameLayout) findViewById (R.id.main);
		
		/* Lili wants to support two different layouts for playback page */
		// View video_layer = inflater.inflate (R.layout.video_layer_new, vMain, true);		
		
	    videoFragment = (VideoFragment) getSupportFragmentManager().findFragmentById (R.id.video_fragment_container);
	    videoFragment.set_context (this); /* naughty */
	    player = videoFragment;
	    
	    playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById (R.id.player_fragment_container);
	    
	    set_reasonable_player_size();
        
	    hide_player_fragment();
	    
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
		    	log (identity + ": Uncaught exception!");
		    	throwable.printStackTrace();
			    finish();
		    	}
			});
		}
	
	@Override
	protected void onStart()
		{
		super.onStart();
		log ("VideoActivity onStart");
		Intent intent = new Intent (this, switchboard.class);
		bindService (intent, mConnection, Context.BIND_AUTO_CREATE);
		EasyTracker.getInstance (this).activityStart (this);
		if (gcast_created)
			google_cast_start();
		else
			gcast_start_pending = true;
		cumulative_episode_time = 0;
		}
	
	@Override
	protected void onStop()
		{
		super.onStop();
		log ("VideoActivity onStop");
		if (mBound)
			{
			unbindService (mConnection);
			mBound = false;
			}
		google_cast_stop();
		FlurryAgent.onEndSession (this);
		EasyTracker.getInstance (this).activityStop (this);
		analytics ("onStop");
		}
	
	@Override
	public void onPause()
		{
		super.onPause();
		log ("onPause");
		gcast_pause();
		}
	
	@Override
	public void onResume()
		{
		super.onResume();
		log ("onResume");
		launch_in_progress = false;
		cumulative_episode_time = 0;
		gcast_resume();
		}
		
	@Override
	protected void onDestroy()
		{
		log ("VideoActivity onDestroy");
		
		if (progress_timer != null)
			progress_timer.cancel();
		
		if (countdown_timer != null)
			countdown_timer.cancel();
		
		google_cast_destroy();
		super.onDestroy();
		}

	@Override
	public Handler main_thread_handler()
		{
		return in_main_thread;
		}
	
	public metadata get_config()
		{
		return config;
		}
	
	private ServiceConnection mConnection = new ServiceConnection ()
		{
		@Override
		public void onServiceConnected (ComponentName className, IBinder service)
			{
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
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
	
	public boolean is_tablet()
		{
		return screen_inches >= 6;
		}
			
	public void calculate_pixels()
		{
		DisplayMetrics dm = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics (dm);

	    density = dm.density;
	   
	    pixels_2   = (int) (2   * dm.density); 
	    pixels_3   = (int) (3   * dm.density); 	    
	    pixels_4   = (int) (4   * dm.density);
	    pixels_5   = (int) (5   * dm.density);
	    pixels_6   = (int) (6   * dm.density); 	    
	    pixels_7   = (int) (7   * dm.density); 	    
	    pixels_8   = (int) (8   * dm.density); 	  	    
	    pixels_10  = (int) (10  * dm.density); 
	    pixels_12  = (int) (12  * dm.density); 	    
	    pixels_15  = (int) (15  * dm.density); 	    
	    pixels_20  = (int) (20  * dm.density); 
	    pixels_24  = (int) (24  * dm.density); 	    
	    pixels_25  = (int) (25  * dm.density); 	    
	    pixels_30  = (int) (30  * dm.density); 
	    pixels_32  = (int) (32  * dm.density); 	    
	    pixels_35  = (int) (35  * dm.density); 	    
	    pixels_40  = (int) (40  * dm.density); 	 	    
	    pixels_50  = (int) (50  * dm.density); 	    
	    pixels_60  = (int) (60  * dm.density);
	    pixels_70  = (int) (70  * dm.density);
	    pixels_80  = (int) (80  * dm.density); 	    
	    pixels_100 = (int) (100 * dm.density);
	    pixels_120 = (int) (120 * dm.density);
	    pixels_130 = (int) (130 * dm.density);
	    pixels_140 = (int) (140 * dm.density);	    
	    pixels_150 = (int) (150 * dm.density);	    
	    pixels_160 = (int) (160 * dm.density);	 	    
	    pixels_200 = (int) (200 * dm.density);
	    pixels_300 = (int) (300 * dm.density);	    
	    
	    h_movement_threshhold = pixels_120;
	    v_movement_threshhold = pixels_120;
		}
		
	public int actual_pixels (int dp)
		{
		return (int) (dp * density);
		}
	
	public int screen_width()
		{
		return screen_width;		
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
		    screen_width = dm.widthPixels < dm.heightPixels ? dm.widthPixels : dm.heightPixels;
		    screen_height = dm.widthPixels < dm.heightPixels ? dm.heightPixels : dm.widthPixels;
		    screen_density = dm.density;
			}
		catch (Exception ex)
			{
			/* nothing */
			}
		}
	
	public void set_reasonable_player_size()
		{
        View vPlayerFragment = findViewById (R.id.player_fragment_container);
        if (vPlayerFragment != null)
	        {
        	FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vPlayerFragment.getLayoutParams();
        	layout.height = (int) (screen_width / 1.77);
        	vPlayerFragment.setLayoutParams (layout);	
	        }
		}
	
	public void flurry_log (String event)
		{
		if (event != null)
			FlurryAgent.logEvent (event);
		}
	
	public void post_to_main_thread (Runnable r)
		{
		in_main_thread.post (r);
		}
	
	public void flurry_log (String event, String k1, String v1)
		{
		if (event != null)
			{
			Map <String, String> params = new HashMap <String, String>();		
	        params.put (k1, v1); 
	        FlurryAgent.logEvent (event, params);
			}
		}
	
	String most_recent_screen = null;
	String most_recent_screen_or_menu = null;
	
	Tracker get_tracker()
		{
		if (config != null)
			{
			GoogleAnalytics ga = GoogleAnalytics.getInstance (this);
			// LogLevel can be: VERBOSE | INFO | DEBUG | WARNING
			ga.getLogger().setLogLevel (LogLevel.VERBOSE);
			String tracking_id = config.google_analytics;
			if (tracking_id != null)
				return ga.getTracker (tracking_id);
			else
				return null;
			}
		else
			return null;
		}
	
	public void track_screen (String screen)
		{	
		if (screen != null)
			{
			if (most_recent_screen_or_menu != null && most_recent_screen_or_menu.equals (screen))
				return;
			
			most_recent_screen_or_menu = screen;
			if (!screen.equals ("menu"))
				most_recent_screen = screen;
			
			Tracker tr = get_tracker();
			if (tr != null)
				{
				log ("[analytics] track screen: " + screen);
				tr.set (Fields.SCREEN_NAME, screen);
				Map <String, String> m = MapBuilder.createAppView().build();
				tr.send (m);
				}
			else
				log ("track_screen(" + screen + "): not tracking");
			}
		}
	
	String most_recent_event;
	
	public void track_event (String category, String action, String label, long value)
		{
		track_event (category, action, label, value, "");
		}
	
	public void track_event (String category, String action, String label, long value, String extra)
		{
		String smashed_event = category + "." + action + "." + label + value + "." + extra;
		
		if (most_recent_event != null && most_recent_event.equals (smashed_event))
			{
			/* never send duplicate events */
			return;
			}
		
		most_recent_event = smashed_event;
		
		Map <String, String> m = MapBuilder.createEvent (category, action, label, value).build();
		Tracker tr = get_tracker();
		if (tr != null)
			{
			log ("[analytics] track event: " + smashed_event);
			tr.send (m);
			}
		else
			log ("track_event(" + action + "): not tracking");
		}
	
	public void track_current_screen()
		{
		if (most_recent_screen != null && !most_recent_screen.equals ("menu"))
			track_screen (most_recent_screen);
		}
	
	/* override this */
	public boolean video_is_minimized()
		{
		return false;
		}
			
	float downX = 0, downY = 0, max_delta_X = 0, max_delta_Y = 0;
	boolean x_movement_started = false;
	boolean y_movement_started = false;
	boolean started_inside_container = false;
	boolean big_thing_moving = false;
	
	public boolean dispatchTouchEvent (MotionEvent event)
		{
		super.dispatchTouchEvent (event);
		
		int action = event.getAction();
		int fragment_container = (player == playerFragment) ? R.id.player_fragment_container : R.id.video_fragment_container;
		
		View vContainer = findViewById (chromecasted ? R.id.chromecast_window : fragment_container);
				
		if (action == MotionEvent.ACTION_DOWN)
			{
			big_thing_moving = false;
			downX = event.getX();
			downY = event.getY();
			max_delta_X = 0;
			max_delta_Y = 0;
			log ("[dispatch] ACTION DOWN, x=" + event.getX() + ", y=" + event.getY());
			started_inside_container = video_visible_somewhere() && point_inside_view (event.getX(), event.getY(), vContainer);
			if (started_inside_container)
				onVideoActionDown();
			else if (inside_big_draggable_thing (event.getX(), event.getY()))
				{
				onBigThingDown();
				big_thing_moving = true;
				}
			onActionDown();
			}	
		else if (action == MotionEvent.ACTION_UP)
			{
			int deltaX = (int) (downX - event.getX());
			int deltaY = (int) (downY - event.getY());
			
			log ("[dispatch] ACTION UP, x=" + event.getX() + ", y=" + event.getY() + ", deltaX=" + deltaX + ", deltaY=" + deltaY + ", max_delta_X=" + max_delta_X);
			
			if (big_thing_moving)
				onBigThingUp (deltaX, deltaY);
			else if (point_inside_view (event.getX(), event.getY(), vContainer))
				{
				// if (Math.abs (deltaX) > pixels_80 || Math.abs (deltaY) > pixels_80)
				if (max_delta_X > pixels_40 || max_delta_Y > pixels_40)
					onVideoActionUp (deltaX, deltaY);
				else
					onVideoActionTapped();
				}
			onActionUp();
			}
		else if (action == MotionEvent.ACTION_MOVE)
			{

			log ("[dispatch] ACTION MOVE, x=" + event.getX() + ", y=" + event.getY());
			int deltaX = (int) (downX - event.getX());
			int deltaY = (int) (downY - event.getY());
			
			if (Math.abs (deltaX) > max_delta_X)
				max_delta_X = Math.abs (deltaX);
			
			if (Math.abs (deltaY) > max_delta_Y)
				max_delta_Y = Math.abs (deltaY);
			
			if (big_thing_moving)
				onBigThingMove (deltaX, deltaY);
			else if (video_is_minimized() && point_inside_view (event.getX(), event.getY(), vContainer))
				onVideoHorizontal (deltaX);
			
			/* this should be lightweight! onBigThingMove and onVideoHorizontal for heavier stuff */
			onActionMove (deltaX, deltaY);
			}
		
		return false;
		}
	
	public boolean  video_visible_somewhere()
		{
		return false;
		}
		
	public boolean inside_big_draggable_thing (float x, float y)
		{
		/* override this */
		return false;
		}
	
	public void onBigThingDown()
		{
		/* override this */
		}
	
	public void onBigThingMove (int deltaX, int deltaY)
		{	
		/* override this */
		}

	public void onBigThingUp (int deltaX, int deltaY)
		{
		/* override this */
		}
	
	public void onActionDown()
		{
		/* override this */		
		}
	
	public void onActionUp()
		{
		/* override this */		
		}
	
	public void onActionMove (int deltaX, int deltaY)
		{
		/* override this */		
		}	
	
	public boolean onVideoActionTapped()
		{
		/* override this */
		return false;
		}
	
	public boolean onVideoActionDown()
		{
		/* override this */
		return false;
		}
	
	public boolean onVideoActionUp (int deltaX, int deltaY)
		{
		/* override this */
		return false;
		}
	
	public boolean onVideoHorizontal (int deltaX)
		{
		/* override this */
		return false;
		}

	public boolean point_inside_view (float x, float y, View view)
		{
		if (view != null)
			{
		    int location[] = new int[2];
		    view.getLocationOnScreen (location);
		    
		    int viewX = location [0];
		    int viewY = location [1];
	
		    return ( (x >= viewX && x <= (viewX + view.getWidth()) ) && ( y >= viewY && y <= (viewY + view.getHeight()) ));
			}
		else
			return false;
	    }

	public void onVideoActivityFlingUp()
		{
		/* override this */		
		}
	
	public void onVideoActivityFlingDown()
		{
		/* override this */		
		}
	
	public void onVideoActivityFlingLeft()
		{
		/* override this */		
		}
	
	public void onVideoActivityFlingRight()
		{
		/* override this */		
		}

	public void onVideoActivityTouched()
		{
		/* override this */		
		}
	
	/* touch but not a video touch */
	public void onVideoActivityOutsideTouch (float x, float y)
		{
		/* override this */
		}
	
	public void onVideoHorizontalMovement (int deltaX)
		{
		/* override this */
		}

	public void onVideoVerticalMovement (int deltaY)
		{
		/* override this */
		}
	
	public void onVideoMovementDone (int deltaX, int deltaY)
		{
		/* override this */
		}
	
	public boolean screen_is_on()
		{
		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE);
		return pm.isScreenOn();
		}

	public void perform_fling_left()
		{
		/* clear these to prevent their completion routines from executing */
		clear_titlecard_ids();
		
		if (any_more_subepisodes())
			{
			log ("fling left: more subepisodes");
			next_episode();
			}
		else
			{
			if (any_more_episodes())
				{
				log ("fling left: play next episode");
				next_episode();
				}
			else
				toast_by_resource (R.string.at_last_episode);
			}
		}
	
	String actual_channel()
		{
		String ret = player_real_channel;
		if (ret.contains (":"))
			{
			String episode_id = program_line [current_episode_index - 1];
			ret = config.program_meta (episode_id, "real_channel");
			}
		return ret;
		}
	
	public void set_player_real_channel (String channel_id)
		{
		player_real_channel = channel_id;
		}
	
	/* for TV devices such as Google TV or dongle */
	public void skip_video (int direction)
		{
		if (player.is_ready())
			{
			try
				{
	        	long offset = player.get_offset();
	        	long duration = player.get_duration();
	        	long stride = duration / 5;
	        	long new_position = offset + stride * direction;
	        	if (new_position < 0) new_position = 0;
	        	if (new_position > duration) new_position = duration;
				player.seek (new_position);
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			}
		}
	
	public void exit_stage_left()
		{
		exit_in_progress = true;
		if (config != null)
			config.television_turning_off = true;
		finish();
		}
	
	public void alert (String text)
		{
		log ("ALERT: " + text);
		Toast.makeText (this, text, Toast.LENGTH_LONG).show();
		}
	
	public void toast (String text)
		{
		log ("TOAST: " + text);
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
	
	public void alert_then_exit (String text)
		{
		log ("ALERT: " + text);
		
		AlertDialog.Builder builder = new AlertDialog.Builder (VideoBaseActivity.this);
	
		builder.setMessage (text);
		builder.setNeutralButton ("OK", new DialogInterface.OnClickListener()
			{
			@Override
			public void onClick (DialogInterface arg0, int arg1)
				{
				exit_stage_left();
				}
			});
		
		builder.create().show();
		}
	
	public void onVideoActivityNoEpisodes()
		{
		toast_by_resource (R.string.no_episodes_in_channel);
		}
	
	public void onVideoActivityRejigger()
		{
		/* override this */
		}
	
	String chromecast_stream_status = "play";
	
	public void pause_video()
		{
		log ("++ pause video ++");
		if (chromecasted)
			{
			chromecast_stream_status = "pause";
			chromecast_send_simple ("pause");
			}
		else
			{
			player.pause();
			fixup_pause_or_play_button();
			}
		}

	public void video_play()
		{
		log ("++ play video ++");
		if (chromecasted)
			{
			chromecast_stream_status = "play";
			chromecast_send_simple ("resume");
			}
		else
			{
			player.play();
			fixup_pause_or_play_button();
			}
		}
	
	public void pause_or_play()
		{
		if (chromecasted)
			{
			if (chromecast_stream_status.equals ("play"))
				pause_video();
			else
				video_play();
			}
		else
			{
			if (player.is_paused())
				{
				log ("pause_or_play: play");
				player.play();
				}
			else
				{
				log ("pause_or_play: pause");
				player.pause();
				}
			fixup_pause_or_play_button();
			}
		}
	
	public void fixup_pause_or_play_button()
		{
		/* override this */
		}
		
	public void play_first (String channel_id)
		{
		player_real_channel = channel_id;
		video_play_pending = true;
		log ("play first load channel " + channel_id);
		load_channel_then (channel_id, false, play_inner, "1", null);
		}
	
	public void play_nth (String channel_id, long position)
		{
		player_real_channel = channel_id;
		video_play_pending = true;
		log ("play nth(" + position + ") load channel " + channel_id);
		load_channel_then (channel_id, false, play_inner, Long.toString (position), null);
		}	

	public void play_nth (String channel_id, long position, long start_msec)
		{
		player_real_channel = channel_id;
		video_play_pending = true;
		log ("play nth(" + position + ") load channel " + channel_id);
		load_channel_then (channel_id, false, play_inner, Long.toString (position), new Long (start_msec));
		}
	
	final Callback play_inner = new Callback()
		{
		@Override
		public void run_string_and_object (String arg1, Object arg2)
			{
			int position = Integer.parseInt (arg1);
			long start_msec = arg2 == null ? 0 : (Long) arg2;
			log ("play_inner");
			program_line = config.program_line_by_id (player_real_channel);
			try_to_play_episode (position, start_msec);
			String episode = program_line != null && program_line.length >= position ? program_line [position-1] : null;
			onVideoActivityRefreshMetadata (player_real_channel, episode);
			}
		};
		
	public void onVideoActivityRefreshMetadata (String channel_id, String episode_id)
		{
		/* override this */
		}
	
	public void play (String channel_id, String episode_id)
		{
		player_real_channel = channel_id;
		
		/* RETARDED */
		/* We want to call yt_player.stop() here. BUT GOOGLE GAVE US NO STOP API. How broken is that? */
		playerFragment.stop();
		
		video_play_pending = true;
		
		log ("play load channel " + channel_id);
		load_channel_then (channel_id, false, play_episode_inner, episode_id, null);
		}
	
	final Callback play_episode_inner = new Callback()
		{
		@Override
		public void run_string_and_object (String episode_id, Object arg)
			{
			log ("play_episode: " + episode_id);
			onVideoActivityRefreshMetadata (player_real_channel, episode_id);
			play_specified_episode (episode_id);
			}
		};
		
	public void load_channel_then (final String channel_id, final boolean allow_cache, final Callback callback, final String arg1, final Object arg2)
		{
		/* if we already have episodes, done */
		
		if (config.channel_loaded (channel_id))
			{
			log ("load channel " + channel_id + " then: has episodes");
			if (callback != null)
				callback.run_string_and_object (arg1, arg2);
			return;
			}
		
		/* if the channel is known but without episodes */
		
		String name = config.pool_meta (channel_id, "name");
		if (name != null && !name.equals (""))
			{
			log ("load channel " + channel_id + " then: known, but no programs (allow_cache: " + allow_cache + ")");
			ytchannel.fetch_and_parse_by_id_in_thread (VideoBaseActivity.this, config, channel_id, allow_cache, in_main_thread, new Runnable()
				{
				@Override
				public void run()
					{
					if (callback != null)
						{
						log ("native callback! arg1=" + arg1 + " arg2=" + arg2);
						callback.run_string_and_object (arg1, arg2);
						}
					}				
				});				
			return;
			}
		
		/* if the channel is not known */
		
		log ("load channel " + channel_id + " then: not yet known");		
		new playerAPI (in_main_thread, config, "channelLineup?channel=" + channel_id)
			{
			public void success (String[] chlines)
				{
				log ("load channel " + channel_id + ", lines received: " + chlines.length);
				config.parse_channel_info (chlines);
				ytchannel.fetch_and_parse_by_id_in_thread (VideoBaseActivity.this, config, channel_id, allow_cache, in_main_thread, callback, arg1, arg2);
				return;
				}
	
			public void failure (int code, String errtext)
				{
				log ("ERROR! " + errtext);
				}
			};
		}
	
	public void play_specified_episode (String episode_id)
		{
		log ("play specified episode: " + episode_id + " in channel: " + player_real_channel);
		
		program_line = config.program_line_by_id (player_real_channel);
		
		if (program_line != null)
			{
			for (int i = 0; i < program_line.length; i++)
				{
				log ("[ch " + player_real_channel + "] #" + i + ": " + program_line [i]);
				if (config.equal_episodes (program_line [i], episode_id))
					{
					final int index = i + 1;
					in_main_thread.post (new Runnable()
						{
						@Override
						public void run()
							{
							try_to_play_episode (index);
							}
						});
					return;
					}
				}
			}
		
		log ("unable to find episode \"" + episode_id + "\" in channel \"" + player_real_channel + "\"");

		if (config.is_youtube (player_real_channel))
			{
			/* fallback behavior -- create a fake episode that can be played, for YouTube channels or playlists only */
			log ("creating fake episode: " + episode_id);
			config.add_runt_episode (1, player_real_channel, episode_id);
			play_specified_episode (episode_id);
			}
		else
			toast_by_resource (R.string.episode_not_found);
		}	
		
	public void relay_post (String text)
		{
		if (mService != null)
			mService.relay_post (text);
		}
	
	public int readout_volume()
		{
		AudioManager au = (AudioManager) getSystemService (Context.AUDIO_SERVICE);
		int vol = au.getStreamVolume (AudioManager.STREAM_MUSIC);
		int max_vol = au.getStreamMaxVolume (AudioManager.STREAM_MUSIC);
		// mService.relay_post ("REPORT VOLUME " + vol + " " + max_vol);
		return vol;
		}
	
	public void set_volume (int volume)
		{
		AudioManager au = (AudioManager) getSystemService (Context.AUDIO_SERVICE);
		int vol = au.getStreamVolume (AudioManager.STREAM_MUSIC);
		au.setStreamVolume (AudioManager.STREAM_MUSIC, volume, 0);
		}
	
	public void ready()
		{
		log ("ready? started=" + started + " player is " + ((player.is_ready()) ? "activated" : "not activated yet"));
		if (started == 3)
			{
			config = mService.get_metadata (identity);
			
			if (config.mso == null)
				{
				/* the background service has been killed and restarted, since there is no MSO and we know that if it had been handed to us
				   properly from .start, we would be guaranteed a non-null MSO. Exit and have start re-start. */

				config.future_action = "reload-and-restart";
				finish();
				return;
				}
			
			if (config.flurry_id != null && !config.flurry_id.isEmpty())
				FlurryAgent.onStartSession (this, config.flurry_id);
			FlurryAgent.setLogEnabled (true);
			FlurryAgent.setLogLevel (Log.DEBUG);
			
			google_cast_create();
			if (player.is_ready())
				{
				started = 4;
				onVideoActivityReady();
				saved_usertoken = (config != null) ? config.usertoken : null;
				}
			else
				onVideoActivityAlmostReady();
			}
		}
	
	public void onVideoActivityAlmostReady()
		{
		/* override this */
		}
	
	public void onVideoActivityReady()
		{
		/* override this */
		}
	
	public void analytics (String why)
		{
		log ("++ analytics enter (" + cumulative_channel_id + ", " + cumulative_episode_id + ")");
		
		String episode_id = null;
		
		boolean channel_changed = player_real_channel != null && !player_real_channel.equals (cumulative_channel_id);
		
		if (program_line != null && program_line.length >= current_episode_index)
			episode_id = program_line [current_episode_index - 1];
		
		if (cumulative_episode_id != null)
			{
			if (channel_changed || (episode_id != null && !episode_id.equals (cumulative_episode_id)))
				{
				cumulative_channel_time += cumulative_episode_time;
				submit_episode_analytics (why);
				cumulative_episode_id = episode_id;
				}
			}
		else
			cumulative_episode_id = episode_id;
		
		if (cumulative_channel_id != null)
			{
			if (player_real_channel != null && !player_real_channel.equals (cumulative_channel_id))			
				submit_channel_analytics (why);
			}
		else
			cumulative_channel_id = player_real_channel;
		
		if (cumulative_episode_id == null)
			{
			log ("cumulative episode id is null!");
			if (program_line != null)
				log ("program_line.length: " + program_line.length);
			else
				log ("program_line is null");
			log ("current_episode_index: " + current_episode_index);			
			}
		
		log ("++ analytics exit (" + cumulative_channel_id + ", " + cumulative_episode_id + ")");
		
		player.reset_time_played();
		}
	
	public void restart_playing (long start_msec)
		{
		if (start_msec > 0)
			{
			/* something bad happened probably when returning to a video after a POI or something. The video
			   needs to be kickstarted, but at the remembered location */
			try_to_play_episode (current_episode_index, start_msec);
			/* sure hope that worked out */
			}
		else
			start_playing();
		}
	
	public void start_playing()
		{
		log ("start playing");
		
		program_line = config.program_line_by_id (player_real_channel);
		
		if (program_line == null)
			{
			log ("no episodes in: " + player_real_channel);
			onVideoActivityNoEpisodes();
			return;
			}
		
		setup_progress_bar();

		if (requested_episode != null && !requested_episode.equals (""))
			{
			log ("start_playing: play requested episode: " + player_real_channel + " " + requested_episode);
			play (player_real_channel, requested_episode);
			return;
			}
		else
			log ("no requested episode, episode index is: " + current_episode_index + " (of " + program_line.length + ")");
			
		if (config.programs_in_real_channel (player_real_channel) > 0)
			{
			log ("start_playing()");
			try_to_play_episode (current_episode_index);
			}
		else
			play_first (player_real_channel);
		}
	
	public boolean is_streamable_url (String url)
		{
		return url.contains (".m3u8") || url.contains (".mp4") || url.contains (".MP4");
		}
	
	public void try_to_play_episode (final int episode)
		{
		final String channel_id = player_real_channel;
		
		Runnable r = new Runnable()
			{
			@Override
			public void run()
				{
				log ("advertisement finished, now playing episode #" + episode);
				play_nth_episode_in_channel (channel_id, episode);				
				}			
			};
			
		if (!is_it_time_to_play_an_advertisement (r))
			try_to_play_episode_msec (episode, 0);
		}
	
	public void try_to_play_episode (final int episode, final long start_msec)
		{
		final String channel = player_real_channel;
		Runnable r = new Runnable()
			{
			@Override
			public void run()
				{
				log ("advertisement finished, now playing episode #" + episode);
				if (player_real_channel == null)
					{
					log ("[try to play episode] player real channel is null! setting to: " + channel);
					player_real_channel = channel;
					}
				try_to_play_episode_msec (episode, start_msec);
				}			
			};
		
		if (!is_it_time_to_play_an_advertisement (r))
			try_to_play_episode_msec (episode, start_msec);
		}
	
	public void play_nth_episode_in_channel (String channel_id, int position)
		{
		/* override this -- ugly layer violation */
		}
	
	public boolean is_it_time_to_play_an_advertisement (Runnable r)
		{
		if (!chromecasted)
			{
			if (config.total_play_count > 1 && (config.total_play_count == 3 || config.total_play_count % 9 == 0))
				{				
				if (config.last_played_advertisement_at != config.total_play_count)
					{
					config.last_played_advertisement_at = config.total_play_count;
					return advertise (r);
					}
				else
					log ("count is " + config.total_play_count + ", already played an advertisement");
				}
			else
				log ("count is " + config.total_play_count + ", not the right time to play an advertisement");
			}
		
		return false;
		}
	
	 public boolean advertise (final Runnable r)
		{
		/* override this */
		return false;
		}
	
	public void try_to_play_episode_msec (int episode, long start_msec)
		{
		if (start_msec > 0)
			log ("try to play episode: " + episode + ", start_msec: " + start_msec);
		else
			log ("try to play episode: " + episode);		
		
		video_has_started = false;
		playing_begin_titlecard = playing_end_titlecard = false;
		
		if (countdown_timer != null)
			{
			countdown_timer.cancel();
			countdown_timer = null;
			}
		
		/* always clear the OSD for POI */
		remove_poi_inner();
		
		if (program_line == null || program_line.length == 0)
			{
			onVideoActivityNoEpisodes();
			return;
			}
		
		log ("Programs in channel \"" + player_real_channel + "\": " + program_line.length);
		
		if (episode <= program_line.length)
			{
			current_episode_index = episode;
	
			onVideoActivityRejigger();
			
			/* probably onStopped() has not been called yet, but we want analytics now */			
			player.add_to_time_played();
			analytics ("play");
			
			subepisodes_this_episode = 0;
			current_subepisode = 0;
			
			String episode_id = program_line [episode - 1];
			onVideoActivityRefreshMetadata (player_real_channel, episode_id); // TODO: this here?
			
			chromecast (player_real_channel, episode_id, 0);

			if (chromecasted)				
				{
				update_metadata_inner();
				}
			else
				{
				String total_subepisodes = config.program_meta (episode_id, "total-subepisodes");
				if (total_subepisodes != null && !total_subepisodes.equals ("0"))
					{
					subepisodes_this_episode = Integer.parseInt (total_subepisodes);
					current_subepisode = 0;
					}
				
				if (subepisodes_this_episode == 0)
					{
					String url = config.best_url (episode_id);		
					log ("episode \"" + episode_id + "\" has no subepisodes, url is: " + url);
					if (url == null)
						config.dump_episode_details (episode_id);
					
					String nature = config.pool_meta (player_real_channel, "nature");
					if (nature != null && nature.equals ("13"))
						play_live_episode (url);
					else
						play_video_url (url, start_msec, -1);
					}
				else
					{
					/* ignore start_msec problem here, since subepisodes seem to be on their way out */
					log ("play first subepisode of: " + episode_id + " (of " + subepisodes_this_episode + ")");
					play_current_subepisode (0, start_msec);
					}
				
				put_dots_on_progress_bar (episode_id);
				}
			}
		}

	public void play_live_episode (final String url)
		{
		if (url.contains ("youtube.com"))
			{	
			String video_id = video_id_of (url);
			ytchannel.youtube_live_info (config, video_id, in_main_thread, new Callback()
				{
				public void run_string (String start_timestamp)
					{
					if (start_timestamp != null)
						{
						long start_time = Long.parseLong (start_timestamp);
						
						long now = System.currentTimeMillis();					
						long seconds = (start_time - now) / 1000;
						
						log ("start seconds delta: " + seconds);
						
						if (seconds > 0)
							countdown_to_live_broadcast (url, start_timestamp);
						else
							play_video_url (url, 0, -1);
						}
					else
						play_video_url (url, 0, -1);
					}
				});
			}
		else
			{
			/* no special processing for non-YouTube */
			play_video_url (url, 0, -1);
			}
		}
	
	public void countdown_to_live_broadcast (String url, String timestamp)
		{
		long start_time = Long.parseLong (timestamp);
		
		TextView vMessage = (TextView) findViewById (R.id.titlecardtext);
		vMessage.setText ("");
		vMessage.setTextColor (Color.rgb (0xFF, 0xFF, 0xFF));
		
		View vTitlecard = findViewById (R.id.titlecard);
		vTitlecard.setBackgroundColor (Color.rgb (0x00, 0x00, 0x00));
		
		vTitlecard.setVisibility (View.VISIBLE);	
		set_video_visibility (View.GONE);
		
		View vBacking = findViewById (R.id.backing_controls);
		vBacking.setVisibility (View.GONE);
		
		/* ugly hack */
		if (!video_is_minimized() && videoFragment != null && videoFragment.video_width > 0)
			{
			/* set the height of the wrapper, otherwise collapsing the video will also collapse the wrapper */
			int orientation = getRequestedOrientation();
	    	boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			SpecialFrameLayout yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);
			LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
			wrapper_layout.weight = landscape ? 1.0f : 0f;
			wrapper_layout.height = landscape ? 0 : videoFragment.video_height;
			wrapper_layout.width = landscape ? MATCH_PARENT : videoFragment.video_width;
			yt_wrapper.setLayoutParams (wrapper_layout);
			}		
		
		countdown_timer = new Timer();
    	countdown_timer.scheduleAtFixedRate (new CountdownTask (start_time), 1000, 1000);
		}
	
	class CountdownTask extends TimerTask
		{  
		long start_time = 0;
		
		CountdownTask (long start_time)
			{
			this.start_time = start_time;
			}
		
		public void run()
	       	{
			in_main_thread.post (new Runnable()
				{
				@Override
				public void run()
					{
					long now = System.currentTimeMillis();					
					long seconds = (start_time - now) / 1000;
					
					log ("start: " + (start_time/1000) + ", now: " + (now/1000) + ", seconds: " + seconds);
					
					TextView vMessage = (TextView) findViewById (R.id.titlecardtext);
					
					String cd = util.countdown_time (seconds);
					
					vMessage.setText ("Please stand by. " + cd);
					
					View vTitlecard = findViewById (R.id.titlecard);
					if (vTitlecard.getVisibility() != View.VISIBLE)
						{
						cancel();
						countdown_timer = null;
						}
					}
				});
	       	}	       	
  		}
	
	
	/* this should be false unless the dots return. I hope the dots don't return */
	boolean haz_dotz = false;
	
	public void put_dots_on_progress_bar (String episode_id)
		{
		erase_all_dots();
		
		if (episode_id == null)
			{
			if (program_line != null)
				episode_id = program_line [current_episode_index - 1];
			else
				return;
			}
		
		FrameLayout vProgress = (FrameLayout) findViewById (R.id.dottistan);
		if (haz_dotz && vProgress != null)
			{
			// if creating dots programmatically
			// remove_images_from_viewgroup (vProgress);
			
			if (subepisodes_this_episode > 0)
				{
				String ts = config.program_meta (episode_id, "total-subepisodes");
				int total_subepisodes = ts != null ? Integer.parseInt (ts) : 0;
				
				String td = config.program_meta (episode_id, "total-duration");
				int total_duration = td != null ? Integer.parseInt (td) : 0;
				
				int progress_bar_width = measure_width (R.id.progressbar, true);
				/* ignore the above, but I think it forces getWidth() to be accurate */
				progress_bar_width = vProgress.getWidth();
				
				log ("@@ episode: " + episode_id + ", total subepisodes: " + total_subepisodes + ", total duration: " + total_duration);
				log ("@@ progress bar width: " + progress_bar_width);
				
				/* setting the left margin is problematic if the "dot" views are created programmatically. It seems part
				   of the gravity is not accessible. So we have to have pre-built dots in the layout. Max 20. */

				for (int i = 1; i <= total_subepisodes; i++)
					{
					int offset = Integer.parseInt (config.program_meta (episode_id, "sub-" + i + "-offset"));
					int duration = Integer.parseInt (config.program_meta (episode_id, "sub-" + i + "-duration"));				
					int start = Integer.parseInt (config.program_meta (episode_id, "sub-" + i + "-start"));	
					int end = Integer.parseInt (config.program_meta (episode_id, "sub-" + i + "-end"));
					
			    	float pct = (float) offset / (float) total_duration;
			    	int desired = (int) (progress_bar_width * pct);
			    	
					log ("@@ sub " + i + ": offset=" + offset + " duration=" + duration + " pct=" + pct + " start=" + start + " end=" + end);
			    	
					if (progress_bar_width > 100)
						{
						// if creating the dots programmatically... read above note
				    	// ImageView dot = new ImageView (this);
				    	// dot.setImageResource (R.drawable.control_dot);
				    	// vProgress.addView (dot);	

						int id = getResources().getIdentifier ("dot" + i, "id", getPackageName());
						ImageView dot = (ImageView) vProgress.findViewById (id);
						
						if (dot != null)
							{
							dot.setVisibility (View.VISIBLE);							
					    	FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) dot.getLayoutParams();
					    	layout.gravity = Gravity.LEFT;
					    	layout.setMargins (desired, 0, 0, 0);
					    	dot.setLayoutParams (layout);
							}
						else
							log ("dot" + i + " (" + id + ") not found in layout!");
						}
					}
				}
			}
		}
	
	public void erase_all_dots()
		{
		FrameLayout vProgress = (FrameLayout) findViewById (R.id.dottistan);
		if (haz_dotz && vProgress != null)
			{
			for (int i = 1; i <= 40; i++)
				{
				int id = getResources().getIdentifier ("dot" + i, "id", getPackageName());
					if (id != 0)
					{
					View v = vProgress.findViewById (id);
					if (v != null)
						v.setVisibility (View.GONE);
					else
						log ("dot" + i + " (" + id + ") not found in layout!");
					}
				}
			}
		}
	
	public void drop_knob_in_subepisode (float percent)
		{
		if (program_line != null && subepisodes_this_episode > 0)
			{
			String episode_id = program_line [current_episode_index - 1];
			int total_duration = Integer.parseInt (config.program_meta (episode_id, "total-duration"));
			
			int new_offset = (int) (total_duration * percent);
			
			for (int i = subepisodes_this_episode; i > 0; i--)
				{
				int offset = Integer.parseInt (config.program_meta (episode_id, "sub-" + i + "-offset"));
				int duration = Integer.parseInt (config.program_meta (episode_id, "sub-" + i + "-duration"));
				
				if (new_offset >= offset)
					{
					float percent_into_subepisode = (float) (new_offset - offset) / (float) duration;
					log ("new offset: " + new_offset + ", offset: " + offset + ", percent into this subepisode: " + percent_into_subepisode);
					log ("current subepisode: " + current_subepisode + ", will drop into subepisode: " + i);
					current_subepisode = i;
					play_current_subepisode (percent_into_subepisode, 0);
					break;
					}
				}
			}
		}
	
	/* stole this */
	private void remove_images_from_viewgroup (ViewGroup v)
		{
	    boolean doBreak = false;
	    
	    while (!doBreak)
	    	{
	        int childCount = v.getChildCount();
	        int i;
	        for(i=0; i<childCount; i++)
	        	{
	            View currentChild = v.getChildAt(i);
	            if (currentChild instanceof ImageView)
	            	{
	                v.removeView(currentChild);
	                break;
	            	}
	        	}

	        if (i == childCount)
	        	{
	            doBreak = true;
	        	}
	    	}
		}
	
	/* this prevents the completion routines from executing */
	public void clear_titlecard_ids()
		{
		current_begin_titlecard_id = null;
		current_end_titlecard_id = null;
		}
	
	public void set_video_visibility (int visibility)
		{
		View videoContainer = findViewById (R.id.video_fragment_container);
		videoContainer.setVisibility (visibility);
		}
	
	public int get_video_visibility()
		{
		View videoContainer = findViewById (R.id.video_fragment_container);
		return videoContainer.getVisibility();
		}
	
	public void set_video_alpha (int alpha)
		{
		View videoContainer = findViewById (R.id.video_fragment_container);
		videoContainer.setAlpha (alpha);
		}
	
	public boolean any_more_episodes()
		{
		if (player_real_channel != null)
			{
			String program_line[] = config.program_line_by_id (player_real_channel);
			if (program_line != null)
				return current_episode_index + 1 <= program_line.length;
			}
		return false;
		}
	
	public boolean any_more_subepisodes()
		{
		if (current_subepisode > 0)
			{
			String episode_id = program_line [current_episode_index - 1];
			String url = config.program_meta (episode_id, "sub-" + (1 + current_subepisode) + "-url");
			return (url != null && !url.equals (""));
			}
		else
			return false;
		}
	
	/* starting at the beginning of the subepisode */
	public void play_current_subepisode()
		{
		play_current_subepisode (0f, 0);
		}
	
	/* save these to detect when we've moved on */
	String current_begin_titlecard_id = null, current_end_titlecard_id = null;
	
	/* if percent is nonzero, use that. Otherwise, if start_msec is nonzero, use that. Otherwise, use default */
	public void play_current_subepisode (final float percent, long start_msec)
		{
		video_has_started = false;
		
		/* always clear the OSD for POI */
		remove_poi_inner();
		
		update_metadata_inner();
		
		String episode_id = program_line [current_episode_index - 1];
		
		/* if percent is nonzero, this was from a drag-and-drop of the control bar knob. Thus don't play titlecard */
		
		if (!playing_end_titlecard && percent == 0f)
			{
			final String end_title_id = config.program_meta (episode_id, "sub-" + current_subepisode + "-end-title");
			if (end_title_id != null && !end_title_id.equals (""))
				{
				playing_end_titlecard = true;
				
				String message = config.titlecard_meta (end_title_id, "message");
				String duration = config.titlecard_meta (end_title_id, "duration");
				String bgimage = config.titlecard_meta (end_title_id, "bgimage");		
				String color = config.titlecard_meta (end_title_id, "color");
				String bgcolor = config.titlecard_meta (end_title_id, "bgcolor");	
				
				// found key/value in meta: subepisode => 1
				// found key/value in meta: message => titlecard with background color
				// found key/value in meta: type => end
				// found key/value in meta: duration => 7
				// found key/value in meta: style => normal
				// found key/value in meta: size => 20
				// found key/value in meta: color => #ffffff
				// found key/value in meta: effect => clip
				// found key/value in meta: align => center
				// found key/value in meta: bgcolor => #ff0000
				// found key/value in meta: bgimage => http://9x9ui.s3.amazonaws.com/war/v0/images/titlecard-default.png
				// found key/value in meta: weight => bold
						
				if (duration == null || duration.equals (""))
					duration = "7";
				
				TextView vMessage = (TextView) findViewById (R.id.titlecardtext);
				vMessage.setText (message);
								
				if (color != null && color.startsWith ("#"))
					vMessage.setTextColor (Color.parseColor (color));
				else
					vMessage.setTextColor (Color.rgb (0xFF, 0xFF, 0xFF));
				
				View vTitlecard = findViewById (R.id.titlecard);
				
				if (bgcolor != null && bgcolor.startsWith ("#"))
					vTitlecard.setBackgroundColor (Color.parseColor (bgcolor));
				else
					vTitlecard.setBackgroundColor (Color.rgb (0x00, 0x00, 0x00));
				
				vTitlecard.setVisibility (View.VISIBLE);

				set_titlecard_bg (bgimage, player_real_channel, end_title_id, "end");
			
				set_video_visibility (View.GONE);
				
				log ("PLAY TITLECARD " + end_title_id + ": " + message);
				
				current_end_titlecard_id = end_title_id;
				
				in_main_thread.postDelayed (new Runnable()
					{
					@Override
					public void run()
						{
						if (current_end_titlecard_id != null && end_title_id.equals (current_end_titlecard_id))
							after_end_titlecard_inner();
						else
							log ("after_end_titlecard: we have moved on");
						}
					}, Integer.parseInt (duration) * 1000);
				
				return;
				}
			else
				{
				log ("++ [" + episode_id + "] current subepisode " + current_subepisode + " -> " + (current_subepisode + 1));
				current_subepisode++;
				}
			}
		else
			playing_end_titlecard = false;
		
		String url = config.program_meta (episode_id, "sub-" + current_subepisode + "-url");
		final String begin_title_id = config.program_meta (episode_id, "sub-" + current_subepisode + "-begin-title");
		
		if (url != null && !url.equals (""))
			{
			log ("playing subepisode #" + current_subepisode);
			
			if (begin_title_id != null && !begin_title_id.equals ("") && percent == 0f)
				{
				playing_begin_titlecard = true;
				
				String message = config.titlecard_meta (begin_title_id, "message");
				String duration = config.titlecard_meta (begin_title_id, "duration");
				String bgimage = config.titlecard_meta (begin_title_id, "bgimage");
				String color = config.titlecard_meta (begin_title_id, "color");
				String bgcolor = config.titlecard_meta (begin_title_id, "bgcolor");	
				
				if (duration == null || duration.equals (""))
					duration = "7";
				
				TextView vMessage = (TextView) findViewById (R.id.titlecardtext);
				vMessage.setText (message);
				
				if (color != null && color.startsWith ("#"))
					vMessage.setTextColor (Color.parseColor (color));
				else
					vMessage.setTextColor (Color.rgb (0xFF, 0xFF, 0xFF));
				
				View vTitlecard = findViewById (R.id.titlecard);
								
				if (bgcolor != null && bgcolor.startsWith ("#"))
					vTitlecard.setBackgroundColor (Color.parseColor (bgcolor));
				else
					vTitlecard.setBackgroundColor (Color.rgb (0x00, 0x00, 0x00));
				
				vTitlecard.setVisibility (View.VISIBLE);

				set_titlecard_bg (bgimage, player_real_channel, begin_title_id, "begin");
				
				set_video_visibility (View.GONE);
				
				log ("PLAY TITLECARD " + begin_title_id + ": " + message);
				
				/* save this so we can detect if the user has moved on */
				current_begin_titlecard_id = begin_title_id;
				
				in_main_thread.postDelayed (new Runnable()
					{
					@Override
					public void run() 
						{
						if (current_begin_titlecard_id != null && begin_title_id.equals (current_begin_titlecard_id))
							after_begin_titlecard_inner (percent);	
						else
							log ("after_begin_titlecard: we have moved on");
						}
					}, Integer.parseInt (duration) * 1000);
				}
			else
				{
				playing_begin_titlecard = false;
				String t_start = config.program_meta (episode_id, "sub-" + current_subepisode + "-start");	
				String t_end = config.program_meta (episode_id, "sub-" + current_subepisode + "-end");	
				String t_duration = config.program_meta (episode_id, "sub-" + current_subepisode + "-duration");				
				
				if (percent > 0 || start_msec == 0)
					start_msec = (t_start != null && !t_start.equals("")) ? Integer.parseInt (t_start) * 1000 : 0;
				int end_msec = (t_end != null && !t_end.equals("")) ? Integer.parseInt (t_end) * 1000 : -1;
				int duration_msec = (t_duration != null && !t_duration.equals ("")) ? Integer.parseInt (t_duration) * 1000 : -1;
				
				// current_subepisode++;
				int extra_offset = (int) (duration_msec * percent);
				
				log ("start_msec: " + start_msec + ", duration_msec: " + duration_msec + ", percent: " + percent + ", extra offset: " + extra_offset);
				
				play_video_url (url, start_msec + extra_offset, end_msec);
				}
			}
		else
			{
			log ("no more subepisodes in " + episode_id);
			current_subepisode = 0;
			next_episode_with_rules();
			}
		}
	
	public void play_video_url (String url, long start_msec, long end_msec)
		{
		String current_episode = program_line [current_episode_index - 1];
		
		String type = config.program_meta (current_episode, "type");
		if (type == null || type.equals (""))
			type = config.program_meta (current_episode, "sub-" + current_subepisode + "-type");
		
		log ("--> play video url, episode=" + current_episode + " subepisode=" + current_subepisode + " type=" + type);
		if (type != null && type.equals ("5"))
			play_protected_url (url, start_msec, end_msec);
		else if (is_streamable_url (url))
			play_vitamio_url (url, start_msec, end_msec);
		else if (url.contains ("vimeo.com"))
			play_vimeo_url (url, start_msec, end_msec);
		else if (url.contains ("new.livestream.com"))
			play_livestream_url (url, start_msec, end_msec);
		else
			play_youtube_url (url, start_msec, end_msec);	
		}
	
	public void play_livestream_url (final String url, final long start_msec, final long end_msec)
		{
		final String current_episode = program_line [current_episode_index - 1];
		Livestream.fetch_livestream_url_in_thread (config, current_episode, in_main_thread, new Callback()
			{
			@Override
			public void run()
				{
				String new_url = config.best_url_or_first_subepisode (current_episode);
				if (!url.equals (new_url))
					play_video_url (new_url, start_msec, end_msec);
				else
					log ("play_livestream_url: couldn't resolve to a true Livestream URL");
				}
			});
		}
	
	public void play_vimeo_url (final String url, final long start_msec, final long end_msec)
		{
		final String current_episode = program_line [current_episode_index - 1];
		VimeoUrl.fetch_vimeo_url_in_thread (config, current_episode, in_main_thread, new Callback()
			{
			@Override
			public void run()
				{
				String new_url = config.best_url_or_first_subepisode (current_episode);
				if (!url.equals (new_url))
					play_video_url (new_url, start_msec, end_msec);
				else
					log ("play_vimeo_url: couldn't resolve to a true Vimeo URL");
				}
			});
		}	
	
	public void play_protected_url (final String url, final long start_msec, final long end_msec)
		{
		final String current_episode = program_line [current_episode_index - 1];
		ProtectedUrl.fetch_protected_url_in_thread (config, current_episode, in_main_thread, new Callback()
			{
			@Override
			public void run()
				{
				/* this will have changed from type 5 to type 0 */
				String new_url = config.best_url_or_first_subepisode (current_episode);
				play_video_url (new_url, start_msec, end_msec);
				}
			});
		}
	
	public void after_begin_titlecard_inner (float abt_percent)
		{
		/* if program_line becomes null, it means we have moved away */
		if (program_line != null)
			{
			log ("after begin titlecard: " + current_begin_titlecard_id);
			
			set_video_visibility (View.VISIBLE);
			//// set_video_alpha (255);
			
			String episode_id = program_line [current_episode_index - 1];			
			String url = config.program_meta (episode_id, "sub-" + current_subepisode + "-url");
			
			String t_start = config.program_meta (episode_id, "sub-" + current_subepisode + "-start");	
			String t_end = config.program_meta (episode_id, "sub-" + current_subepisode + "-end");	
			String t_duration = config.program_meta (episode_id, "sub-" + current_subepisode + "-duration");
			
			int start_msec = (t_start != null && !t_start.equals("")) ? Integer.parseInt (t_start) * 1000 : 0;
			int end_msec = (t_end != null && !t_end.equals("")) ? Integer.parseInt (t_end) * 1000 : -1;	
			int duration_msec = (t_duration != null && !t_duration.equals ("")) ? Integer.parseInt (t_duration) * 1000 : -1;
			
			playing_begin_titlecard = false;

			int extra_offset = (int) (duration_msec * abt_percent);
			log ("t_duration: " + t_duration + ", duration_msec: " + duration_msec + ", percent: " + abt_percent + ", extra offset: " + extra_offset);
			play_youtube_url (url, start_msec + extra_offset, end_msec);
			}
		}
		
	public void after_end_titlecard_inner()
		{
		log ("after end titlecard: " + current_end_titlecard_id);
		
		set_video_visibility (View.VISIBLE);
		//// set_video_alpha (255);
		
		String episode_id = program_line [current_episode_index - 1];
		log ("++ [" + episode_id + "] current subepisode " + current_subepisode + " -> " + (current_subepisode + 1));
		current_subepisode++;
		play_current_subepisode();
		}
	
	public void set_titlecard_bg (String bgimage, String channel, String titlecard_id, String begin_or_end)
		{
		if (bgimage == null)
			{
			log ("set_titlecard_bg: bgimage is null, ignoring background image");
			return;
			}
		
		boolean got_filled = false;

		ImageView vTitlecardBG = (ImageView) findViewById (R.id.titlecardbg);
		if (vTitlecardBG != null)
			{
			if (bgimage != null && bgimage.startsWith ("http"))
				{
				String filepath = "titlecards/" + channel + "/" + titlecard_id + "." + begin_or_end + ".png";
				String filename = getFilesDir() + "/" + config.api_server + "/" + filepath;
				File f = new File (filename);
				if (f.exists() && f.length() > 0)
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						got_filled = true;
						vTitlecardBG.setImageBitmap (bitmap);	
						log ("titlecard image set to: " + filename);						
						}
					}
				else
					log ("no titlecard image at: " + filename);
				}
			vTitlecardBG.setVisibility (got_filled ? View.VISIBLE : View.GONE);
			}
		else
			log ("set_titlecard_bg: vTitlecardBG is null");
		}
			
	final Runnable titlecard_image_update = new Runnable()
		{
		public void run()
			{
			String episode_id = program_line [current_episode_index - 1];
			
			String begin_or_end = null;
			
			if (playing_begin_titlecard)
				begin_or_end = "begin";
			else if (playing_end_titlecard)
				begin_or_end = "end";
			
			if (begin_or_end != null)
				{
				String title_id = config.program_meta (episode_id, "sub-" + current_subepisode + "-" + begin_or_end + "-title");
				String bgimage = config.titlecard_meta (title_id, "bgimage");
				set_titlecard_bg (bgimage, player_real_channel, title_id, "begin");
				}	
			}
		};
	
	/* this allows business rules to override next_episode() when called internally. For instance,
	   the Streaming Portal wants next_episode() to switch to the next channel */

	public void next_episode_with_rules()
		{
		next_episode();
		}
	
	public void next_episode()
		{
		if (program_line != null)
			{
			if (current_subepisode > 0)
				{
				play_current_subepisode();
				return;
				}

			String program_line[] = config.program_line_by_id (player_real_channel);			
			if (program_line == null)
				{
				log ("program line is null!");
				return;
				}
			
			if (current_episode_index + 1 <= program_line.length)
				{
				log ("next episode");
				try_to_play_episode (current_episode_index + 1);
				}
			else
				{
				log ("next episode: on last episode");
				onLastEpisode();
				}
			}
		else
			log ("next episode: no program line");
		}

	public void onLastEpisode()
		{
		/* override this */
		log ("last episode!");
		exit_stage_left();
		}
	
	public void previous_episode()
		{
		if (program_line != null)
			{
			if (current_episode_index - 1 > 0)
				{
				log ("previous episode");
				try_to_play_episode (current_episode_index - 1);
				}
			else
				toast_by_resource (R.string.at_first_episode);
			}
		else
			log ("previous episode: no program line");
		}

	public void play_youtube_url (String url, long start_msec, long end_msec)
		{
		if (url != null)
			{
			String video_id = video_id_of (url);
			if (start_msec > 0)
				log ("[playvideo] YouTube id: " + video_id + ", start msec: " + start_msec);
			else
				log ("[playvideo] YouTube id: " + video_id);
			set_poi_trigger (false);
			play_youtube (video_id, start_msec, end_msec);
			}
		}
	
	String video_id_of (String url)
		{
		String video_id = url;
		int voffset = url.indexOf ("?v=");
		if (voffset > 0)
			{
			try { video_id = url.substring (voffset + 3, voffset + 3 + 11); } catch (Exception ex) {};
			}
		return video_id;
		}
	
	void play_youtube (final String id, final long start_msec, final long end_msec)
		{
		final Runnable play_youtube_runnable = new Runnable()
			{
			public void run()
				{
				play_youtube_using_new_api (id, start_msec, end_msec);
				
				/* save these so that in case of interruption, we can restart */
				set_most_recent_restore_video_information (id, end_msec);
				
				in_main_thread.post (update_metadata);
				// submit_pdr();
				onVideoActivityPlayback();
				}
			};
		
		if (player == videoFragment)
			play_youtube_runnable.run();
		else
			switch_players (playerFragment, play_youtube_runnable);
		}

	void play_vitamio_url (String url, long start_msec, long end_msec)
		{
		if (url.contains (";"))
			url = url.replaceAll (";.*$", "");
		
		final String final_url = url;
		
		log ("play vitamio url: " + url);
		
		View vTitlecard = findViewById (R.id.titlecard);
		vTitlecard.setVisibility (View.GONE);
		
		in_main_thread.post (update_metadata);

		switch_to_player_fragment();
		in_main_thread.post (new Runnable()
			{
			@Override
			public void run()
				{
				playerFragment.stop();
				playerFragment.play_video (final_url);
				
				/* save these so that in case of interruption, we can restart */
				set_most_recent_restore_video_information (final_url, -1);
				}
			});
		}
	
	public String active_player()
		{
		return (player == playerFragment ? "player" : "video");
		}
	
	public void switch_players (final Player p, final Runnable callback)
		{
		final Runnable switch_players_runnable = new Runnable()
			{
			public void run()
				{
				player = p;
				if (player == playerFragment)
					{
					log ("switching to playerFragment");
					hide_video_fragment();
					show_player_fragment();
					}
				else
					{
					log ("switching to videoFragment");
					hide_player_fragment();
					show_video_fragment();
					}
				callback.run();
				}
			};
		
		if (Looper.myLooper() == Looper.getMainLooper())
			{
			/* already in main thread */
			switch_players_runnable.run();
			}
		else
			in_main_thread.post (switch_players_runnable);
		}
	
	public void onVideoActivityPlayback()
		{
		/* override this */
		}
	
	void play_youtube_using_new_api (String id, long start_msec, long end_msec)
		{
		if (id == null)
			{
			log ("play youtube with new API: tried to play a null video id");
			return;
			}
		
		if (start_msec != 0 || end_msec != 0)
			log ("play youtube with new API: " + id + ", start_msec: " + start_msec + ", end_msec: " + end_msec);
		else
			log ("play youtube with new API: " + id);
		
		video_play_pending = true;
		
		player.set_manage_audio_focus (false);
		
		View vTitlecard = findViewById (R.id.titlecard);
		vTitlecard.setVisibility (View.GONE);				
		
		switch_to_video_fragment();
		
		if (progress_timer == null)
			{
			progress_timer = new Timer();
        	progress_timer.scheduleAtFixedRate (new TickTask(), 1000, 1000);
			}
	
		player.set_listeners();
		

		log ("load video id: " + id + ", start msec: " + start_msec);
		
		video_cutoff_time = end_msec;
				
		if (start_msec > 0)
			videoFragment.load_video (id, start_msec);
		else
			videoFragment.load_video (id);
		}

	final Runnable pause_or_play_adjustment = new Runnable()
		{
		public void run()
			{
			onVideoActivityPauseOrPlay (true);
			}
		};
		
	public void onVideoActivityVideoStarted()
		{
		/* override this */
		}
	
	public void onVideoActivityPauseOrPlay (boolean paused)
		{
		/* override this */
		}
	
   class TickTask extends TimerTask
   		{  
        public void run()
        	{  
        	if (player != null)
	        	{
	            if (player.is_playing())
	            	{
	            	in_main_thread.post (update_progress_bar);
	            	long offset = 0;
	            	long duration = 0;
	            	try
		            	{
			        	offset = player.get_offset();
			        	duration = player.get_duration();
		            	}
	            	catch (Exception ex)
		            	{	
		            	}
	            	if (video_cutoff_time >= 0)
	            		{
	            		if (offset > 0 && offset > video_cutoff_time)
	            			{            			
	            			log ("++ VIDEO HAS REACHED CUTOFF TIME AT " + video_cutoff_time);
	            			video_cutoff_time = -1;
	            			in_main_thread.post (runnable_next_episode);
	            			}
	            		}
	            	if (video_next_trigger >= 0 && !dragging_my_knob)
	            		{
	            		/* do not POI trigger if the progress bar is being dragged, since it disappears the bar */
	            		if (offset > 0 && offset > video_next_trigger)
		            		{
		        			log ("++ VIDEO HAS REACHED POI TRIGGER AT " + video_next_trigger);
		        			in_main_thread.post (trigger_poi);
		            		}
	            		}
	            	if (video_release_trigger >= 0)
	            		{
	            		if (offset > 0 && offset > video_release_trigger)
		            		{
		        			log ("remove OSD for POI");
		        			in_main_thread.post (remove_poi);
		            		}
	            		}
	            	}
	        	}
        	}
   		}

	final Runnable runnable_next_episode = new Runnable()
		{
		public void run()
			{
			next_episode_with_rules();
			}
		};

	/*
	 *  CURRENT POI STATES (tentative):
	 * -1 = uninitialized
	 * -2 = there are no triggers
	 * -3 = trigger executed
	 */
		
	boolean haz_poi = true;
		
	final Runnable trigger_poi = new Runnable()
		{
		public void run()
			{
			// alert ("Trigger POI #" + video_trigger_id + "!");
			log  ("Trigger POI #" + video_trigger_id + "!");			
			video_next_trigger = -3;
			/* set_poi_trigger (true); <-- don't do this until poi removed */
			
			final View vPOI = findViewById (R.id.poi_h);
			if (haz_poi && vPOI != null)
				{
				TextView vText = (TextView) findViewById (R.id.poi_h_message);
				vText.setText (poi_message);
				
				Button vButton = (Button) findViewById (R.id.poi_h_button);
				vButton.setText (poi_button_1_text);
				
				vPOI.setVisibility (View.VISIBLE);
				onVideoActivityLayout();
				
				vButton.requestFocus();				
				vButton.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{		        	
			        	log ("POI CLICKED: " + poi_action_1_url);
						vPOI.setVisibility (View.GONE);
						remember_location();
			        	launch_player_url (poi_action_1_url);
			        	}
					});
				}
			}
		};

	final Runnable remove_poi = new Runnable()
		{
		public void run()
			{
			remove_poi_inner();
			}
		};
		
	public void remove_poi_inner()
		{
		final View vPOI = findViewById (R.id.poi_h);
		if (vPOI != null)
			vPOI.setVisibility (View.GONE);
		set_poi_trigger (true);
		}
	
	// A: {"message":"Testing","button":[{"text":"Tap me","actionUrl":"https://www.youtube.com/watch?v=iG4SBGwohMc&list=PLbpi6ZahtOH5Jymgxb1MgladAL0eMSpmp"}]}
	// B: {"message":"POI to Flipr EP: iPhone Snowboard Video 2","button":[{"text":"Press","actionUrl":"http://yourapp2.flipr.tv/view/p34985/e908312"}]}"
	// C: {"message":"POI to Flipr CH: Product Reviews & Tutorials","button":[{"text":"Press","actionUrl":"http://yourapp2.flipr.tv/view/p34984"}]}
	
	String poi_message = null;		
	String poi_button_1_text = null;
	String poi_action_1_url = null;
	int poi_duration_1 = 10;
	
	public void parse_poi_json (String data)
		{
		JSONObject json;		
		try {
			json = new JSONObject (data);
			}
		catch (JSONException e)
			{
			e.printStackTrace();
			return;
			}
		
		String j_message = null;
		try
			{
			j_message = json.getString ("message");
			}
		catch (Exception ex)
			{
			log ("JSON: no \"message\"");
			return;
			}
		
		JSONArray j_buttons = null;
		try 
			{
			j_buttons = json.getJSONArray ("button");
			}
		catch (JSONException e)
			{
			log ("JSON: no \"button\"");
			return;
			}
		
		JSONObject j_first_button = null;		
		try
			{
			j_first_button = j_buttons.getJSONObject (0);
			}
		catch (JSONException e)
			{
			log ("JSON: no buttons");
			return;
			}
		
		String j_button_text = null;
		try
			{
			j_button_text = j_first_button.getString ("text");
			}
		catch (JSONException e)
			{
			log ("JSON: no first button text");
			return;
			}
		
		String j_button_url = null;
		try
			{
			j_button_url = j_first_button.getString ("actionUrl");
			}
		catch (JSONException e)
			{
			log ("JSON: no first button action");
			return;
			}	
		
		poi_message = j_message;	
		poi_button_1_text = j_button_text;
		poi_action_1_url = j_button_url;		
		}
	
	public void reset_poi()
		{
		poi_message = null;		
		poi_button_1_text = null;
		poi_action_1_url = null;
		video_next_trigger = -1;
		}
	
	public void set_poi_trigger (boolean use_current_offset)
		{
		reset_poi();
		
		/* there presently aren't any POIs, and this function may not be stable */
		if (!haz_poi)
			return;
		
		if (program_line == null || program_line.length == 0)
			{
			log ("set poi trigger: no programs");
			return;
			}
		
		if (current_subepisode > 0)
			{
			/* POI exists only for subepisodes */
			
			int nth = -1;
			
			if (program_line == null || current_episode_index >= program_line.length)
				{
				log ("program_line is out of date, resetting");
				program_line = config.program_line_by_id (player_real_channel);
				}
			
			if (program_line == null || current_episode_index >= program_line.length)
				{
				log ("program line can't reconcile with current_episode_index, which is " + current_episode_index + " -- will not set POIs");
				return;
				}
			
			String episode_id = program_line [current_episode_index - 1];
			
			long offset = 0;
			if (use_current_offset)
				{
				offset = player.get_offset();
				/* determine first POI beyond this point */
				for (nth = 1; /* nothing */; nth++)
					{
					String poi_start = config.program_meta (episode_id, "sub-" + current_subepisode + "-poi-" + nth + "-start");
					if (poi_start == null)
						{
						nth = -1;
						break;
						}
					int poi_start_millis = Integer.parseInt (poi_start) * 1000;
					if (poi_start_millis > offset)
						break;
					}
				}
			else
				nth = 1;
						
			if (nth <= 0)
				{
				/* no valid POIs after this offset */
				return;
				}
	    	
	    	String poi_start = config.program_meta (episode_id, "sub-" + current_subepisode + "-poi-" + nth + "-start");
	    	String poi_end = config.program_meta (episode_id, "sub-" + current_subepisode + "-poi-" + nth + "-end");
	    	String poi_json  = config.program_meta (episode_id, "sub-" + current_subepisode + "-poi-" + nth + "-data");
	    	
			log ("CHECK POI? episode: " + episode_id + " subepisode: " + current_subepisode + " poi start? " + poi_start);
			
	    	if (poi_start != null && poi_json != null)
	    		{
	    		video_next_trigger = Integer.parseInt (poi_start) * 1000;
	    		video_trigger_id = nth;
	    		video_release_trigger = Integer.parseInt (poi_end) * 1000;
	    		parse_poi_json (poi_json);
	    		log ("POI TRIGGER set to: " + video_next_trigger);
	    		}
	    	else
	    		video_next_trigger = video_release_trigger = -2;
			}
		}
	
	public class VideoLocation
		{
		String restore_video_id = null;
		long restore_video_position = -1;
		long restore_video_end_msec = -1;
		String restore_video_channel = null;
		String restore_arena[] = null;
		int restore_video_current_episode_index = -1;
		int restore_video_current_subepisode = -1;
		int restore_video_visibility = -1;
		}
	
	Stack <VideoLocation> videoStack = new Stack <VideoLocation> ();
	
	public void forget_video_location()
		{
		videoStack = new Stack <VideoLocation> ();
		poi_instruction_visibility (false);
		}
	
	public boolean any_remembered_locations()
		{
		return videoStack != null && videoStack.size() > 0;
		}
			
	public void remember_location()
		{
		if (!chromecasted)
			{
			if (videoStack == null)
				forget_video_location();
			
			VideoLocation vloc = new VideoLocation();
			
			vloc.restore_video_position = player.get_most_recent_offset();
			vloc.restore_video_current_episode_index = current_episode_index;
			vloc.restore_video_current_subepisode = current_subepisode;
			vloc.restore_video_channel = player_real_channel;
			vloc.restore_arena = arena;
			log ("VIDEO remember location: " + vloc.restore_video_position);
			vloc.restore_video_visibility = get_video_visibility();
			
			videoStack.push (vloc);
			
			pause_video();
			set_video_visibility (View.INVISIBLE);
			
			poi_instruction_visibility (true);
			}
		}

	public void poi_instruction_visibility (boolean visible)
		{
		View vInstruction = findViewById (R.id.portrait_poi_return_instruction);
		if (vInstruction != null)
			vInstruction.setVisibility (visible ? View.VISIBLE : View.GONE);
		}
	
	public void set_most_recent_restore_video_information (String video_id, long end_msec)
		{
		if (any_remembered_locations())
			{
			/* this is particularly nasty */
			VideoLocation vloc = videoStack.peek();
			vloc.restore_video_id = video_id;
			if (end_msec > 0)
				vloc.restore_video_end_msec = end_msec;
			}
		}
	
	public void restore_location()
		{
		if (any_remembered_locations())
			{
			pending_restart = true;
			/* note: leave pending_restart as true! */
			
			VideoLocation vloc = videoStack.pop();
			
			log ("VIDEO restore location: " + vloc.restore_video_position);
			log ("VIDEO restore video_id: " + vloc.restore_video_id);
			log ("VIDEO restore visibility == View.VISIBLE: " + (vloc.restore_video_visibility == View.VISIBLE));		
			log ("VIDEO restore able to play video: " + able_to_play_video());
			log ("VIDEO restore left in stack: " + videoStack.size());
			
			current_episode_index = vloc.restore_video_current_episode_index;
			current_subepisode = vloc.restore_video_current_subepisode;
			player_real_channel = vloc.restore_video_channel;
			arena = vloc.restore_arena;
			
			set_video_visibility (vloc.restore_video_visibility);
			if (vloc.restore_video_id != null && able_to_play_video() && vloc.restore_video_visibility == View.VISIBLE)
				play_video_url (vloc.restore_video_id, vloc.restore_video_position, vloc.restore_video_end_msec);
			
			if (videoStack.size() == 0)
				poi_instruction_visibility (false);
			}
		else
			{
			log ("restore_location: nothing in video stack!");
			poi_instruction_visibility (false);
			}
		}
	
	public void launch_player_url (String url)
		{
		// http://beagle.9x9.tv/playback?ch=9754&ep=15952

		// A: {"message":"Testing","button":[{"text":"Tap me","actionUrl":"https://www.youtube.com/watch?v=iG4SBGwohMc&list=PLbpi6ZahtOH5Jymgxb1MgladAL0eMSpmp"}]}
		// B: {"message":"POI to Flipr EP: iPhone Snowboard Video 2","button":[{"text":"Press","actionUrl":"http://yourapp2.flipr.tv/view/p34985/e908312"}]}"
		// C: {"message":"POI to Flipr CH: Product Reviews & Tutorials","button":[{"text":"Press","actionUrl":"http://yourapp2.flipr.tv/view/p34984"}]}
		
		if (url.contains ("flipr.tv"))
			{
			String channel = null, episode = null;
			
			Pattern pattern1 = Pattern.compile ("/view/p(\\d+)/([a-z0-9]*)$");			
			Matcher m1 = pattern1.matcher (url);
			if (m1.find())
				{
				channel = m1.group (1);
				episode = m1.group (2);
				}
	
			if (episode == null)
				{
				Pattern pattern2 = Pattern.compile ("/view/p(\\d+)$");			
				Matcher m2 = pattern2.matcher (url);
				if (m2.find())
					{
					channel = m2.group (1);
					}
				}
			
			log ("launch player URL: ch=|" + channel + "| episode=|" + episode + "|");
			launch_player_with_episode (channel, episode);
			}
		else
			{
			/* launch a browser */
    		remember_location();
        	Intent wIntent = new Intent (Intent.ACTION_VIEW, Uri.parse (url));
        	try
        		{
	        	startActivity (wIntent);
        		}
        	catch (Exception ex)
        		{
        		ex.printStackTrace();
        		}
			}
		}
	
	public void launch_player_with_episode (final String channel_id, final String episode_id)
		{
		if (launch_in_progress)
			{
			log ("launch is in progress, won't launch player");
			return;
			}
		
		if (channel_id != null && !channel_id.equals(""))
			{
			launch_in_progress = true;
			
			if (channel_id.contains (":"))
				launch_player (channel_id, episode_id, new String[] { channel_id });
			else
				{
				final Callback launch_episode_inner = new Callback()
					{
					@Override
					public void run_string (String channel_id)
						{
						launch_player (channel_id, episode_id, new String[] { channel_id });
						}
					};
					
				ytchannel.full_channel_fetch (in_main_thread, launch_episode_inner, config, channel_id);
				}
			}
		}

	public void launch_player (String channel_id, String episode_id, String channels[])
		{
		/* override this */
		}
	
	final Runnable update_progress_bar = new Runnable()
		{
		public void run()
			{
			if (!chromecasted)
				{
				long offset = 0;
				long duration = 0;
				
				float pct = 0f;
				
				boolean is_playing = false;
				
	            if (player != null && player.is_playing())
		            {
	            	is_playing = true;
	            	
		        	offset = player.get_offset();
		        	duration = player.get_duration();
		        	
		        	pct = (float) offset / (float) duration;
		            }
	            
				onVideoActivityProgress (is_playing, offset, duration, pct);
				}
			}
		};

	public void onVideoActivityProgress (boolean is_playing, long offset, long duration, float pct)
		{
		/* override this */
		}
	
	/* here is a default updater for the progress bar */
		
	void videoActivityUpdateProgressBar (long offset, long duration)
		{
		int desired = 0;
		
		FrameLayout progress_bar = (FrameLayout) findViewById (R.id.progressbar);
		LinearLayout played = (LinearLayout) findViewById (R.id.played);
	
		if (progress_bar == null)
			return;
		
	    if (duration > 0 && program_line != null)
	        {
	    	String episode_id = program_line [current_episode_index - 1];
	    	
	    	float pct = (float) offset / (float) duration;
	
	    	log ("Tick! episode: " + episode_id + " offset: " + offset + ", duration: " + duration + ", pct: " + pct);
	    	
			config.last_visited_episode_id_for_seek = episode_id;
			config.last_visited_seek = offset;

			/* these two values are adjusted for subepisodes */
			long adjusted_offset = offset;
			long total_duration = duration;
			
			if (current_subepisode > 0)
				{	
				String total_duration_string = config.program_meta (episode_id, "total-duration");
				if (total_duration_string == null)
					{
					/* missing important subprogram data! */
					log ("episode " + episode_id + " has subepisodes but no total-duration!");
					return;
					}
				total_duration = 1000 * Integer.parseInt (total_duration_string);
				
				int s_offset = 1000 * Integer.parseInt (config.program_meta (episode_id, "sub-" + current_subepisode + "-offset"));
				int s_duration = 1000 * Integer.parseInt (config.program_meta (episode_id, "sub-" + current_subepisode + "-duration"));
				
				int s_pre = 0;
				if (config.program_meta (episode_id, "sub-" + current_subepisode + "-pre") != null)
					s_pre = 1000 * Integer.parseInt (config.program_meta (episode_id, "sub-" + current_subepisode + "-pre"));

				int s_start = 0;
				String string_start = config.program_meta (episode_id, "sub-" + current_subepisode + "-start");
				if (string_start != null && !string_start.equals (""))
					s_start = 1000 * Integer.parseInt (string_start);
				
				if (s_start > 0)
					{
					/* pct is different if there the subepisode is cropped */
			    	pct = (float) (offset - s_start) / (float) s_duration;
					}
				
				adjusted_offset = s_offset + s_pre + (int) (pct * s_duration);
				float adjusted_percent = (float) adjusted_offset / (float) total_duration;
				
				int width = measure_width (R.id.progressbar);

				log ("[#" + current_subepisode + "] off: " + offset 
						+ ", s_start: " + s_start + ", s_dur: " + s_duration + ", s_pct: " + pct + ", adjusted offset: " + adjusted_offset 
						+ ", total duration: " + total_duration + ", adjusted percent: " + adjusted_percent);
				
				desired = (int) (progress_bar.getWidth() * adjusted_percent);		
				}
			else
				{
		    	desired = (int) (progress_bar.getWidth() * pct);
				}
			
			TextView vPosition = (TextView) findViewById (R.id.position);
			if (vPosition != null)
				vPosition.setText (util.seconds_to_string (adjusted_offset / 1000));
			
			TextView vDuration = (TextView) findViewById (R.id.duration);	
			if (vDuration != null)
				vDuration.setText (util.seconds_to_string (total_duration / 1000));	
			
		    /* shortening "played" leaves an ugly artifact in the progress bar */
		    boolean invalidate_after = false;
		    
		    if (played != null)
			    {
		    	// log ("* * * * * * WIDTH " + desired);
			    invalidate_after = (played.getWidth() < desired);
			    FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) played.getLayoutParams();
			    layout.width = desired;
				played.setLayoutParams (layout);
			    }
		    
			if (!dragging_my_knob)
				{
				/* position only if user isn't moving it */
				set_knob_to (desired);
				}
			
			if (invalidate_after)
			  invalidate_progress_bar();			
	        }
		}
	
	boolean dragging_my_knob = false;
	
	public void setup_progress_bar()
		{
		final FrameLayout progress_container = (FrameLayout) findViewById (R.id.progresscontainer);
		if (progress_container != null)
			{
			progress_container.setOnTouchListener (new OnTouchListener()
				{
			    @Override
			    public boolean onTouch (View v, MotionEvent event)
			    	{		    	
					int action = event.getAction();
		
				    float x = event.getX();
				    float y = event.getY();
					
				    /* make sure this display isn't removed */
					onKnobDragging();
					
				    if (action == MotionEvent.ACTION_UP)
				    	{
				    	float percent = x / progress_container.getWidth();
			    		Log.i ("vtest", "CONTROL BAR ACTION UP , x=" + x + ", y=" + y + ", percent=" + percent);
			    		if (subepisodes_this_episode == 0)
				    		{
			    			if (!chromecasted)
				    			{
					    		try
					    			{
					    			if (player.is_playing())
						    			{
						    			long duration = player.get_duration();
						    			long seekpos = (long) (duration * percent);
						    			player.seek (seekpos);
						    			}
					    			}
					    		catch (Exception ex)
					    			{
					    			ex.printStackTrace();
					    			}
				    			}
			    			else
			    				chromecast_seek_percent (percent);
				    		}
				    	else
				    		drop_knob_in_subepisode (percent);
			    		dragging_my_knob = false;
				    	}
				    else if (action == MotionEvent.ACTION_DOWN)
				    	{
						Log.i ("vtest", "CONTROL BAR ACTION DOWN , x=" + x + ", y=" + y);
						dragging_my_knob = true;
				    	}
				    else
				    	{
				    	/* too noisy for log */
				    	Log.i ("vtest", "CONTROL BAR event=" + action + ", x=" + x + ", y=" + y);
				    	}		
					
				    set_knob_to ((int) x);
		
				    return true;
			    	}
			    });
			}
		}

	/* mainly this is to keep a timer to prevent the display from being removed during a drag */
	public void onKnobDragging()
		{
		/* override this */
		}
	
	void set_knob_to (int xposition)
		{
		// log ("set knob to: " + xposition); // noisy
		ImageView knob = (ImageView) findViewById (R.id.knob);
		if (knob != null)
			{
		    FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) knob.getLayoutParams();
			layout.leftMargin = xposition;
			knob.setLayoutParams (layout);
			}
		put_dots_on_progress_bar (null);	
		invalidate_progress_bar();
		}
	
	public void reset_progress_bar()
		{
		log ("reset progress bar");
		TextView vPosition = (TextView) findViewById (R.id.position);
		if (vPosition != null)
			vPosition.setText ("");
		TextView vDuration = (TextView) findViewById (R.id.duration);
		if (vDuration != null)
			vDuration.setText ("");
		
		set_knob_to (0);
		
		set_knob_visibility (false);
		
		LinearLayout vPlayed = (LinearLayout) findViewById (R.id.played);
		if (vPlayed != null)
			{
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vPlayed.getLayoutParams();
			layout.width = 0;
			vPlayed.setLayoutParams (layout);
			}
		
		invalidate_progress_bar();
		}
	
	public void invalidate_progress_bar()
		{
		View progress_bar = findViewById (R.id.progressbar);
		if (progress_bar != null)
			progress_bar.invalidate();
		}
	
	public void set_knob_visibility (boolean visible)
		{
		ImageView knob = (ImageView) findViewById (R.id.knob);
		if (knob != null)
			knob.setVisibility (visible ? View.VISIBLE : View.INVISIBLE);
		}
	
	public void onVideoActivityLayout()
  		{
		/* override this */
  		}

	@Override
	public void onFullscreen (boolean isFullscreen)
  		{
		fullscreen = isFullscreen;
		log (isFullscreen ? "FULLSCREEN!" : "HALFSCREEN!");
		onVideoActivityLayout();
  		}

	@Override
	public void onConfigurationChanged (Configuration newConfig)
		{
		super.onConfigurationChanged (newConfig);
		log ("CONFIGURATION CHANGED!");
		onVideoActivityLayout();
		}
	
	final Runnable update_metadata = new Runnable()
		{
		public void run()
			{
			update_metadata_inner();
			}
		};
	
	public void update_metadata_inner()
		{
		String episode_id = program_line [current_episode_index - 1];
		String episode = config.program_meta (episode_id, "name");
		
		String channel = null;
		if (player_real_channel.contains (":"))
			{
			String actual_channel_id = config.program_meta (episode_id, "real_channel");
			channel = config.pool_meta (actual_channel_id, "name");
			}
		else
			channel = config.pool_meta (player_real_channel, "name");
		}
	
	public void update_metadata_mini (String episode_id)
		{	
		/* override this */
		/* only the episode ID has changed */
		}
	
	final Runnable go_halfscreen = new Runnable()
		{
		public void run()
			{			
			log ("go halfscreen");
			update_metadata_inner();
			try
				{
				fullscreen = false;
				player.set_full_screen (false);
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			}
		};
		
	final Runnable go_fullscreen = new Runnable()
		{
		public void run()
			{			
			go_fullscreen_inner();
			}
		};

	public void go_fullscreen_inner()
		{
		log ("go fullscreen");
		try
			{
			fullscreen = true;
			player.set_full_screen (true);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
	public void signout()
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
		
		toast_by_resource (R.string.signed_out_successfully);
		
		onVideoActivitySignout();
		}	
		
	public void onVideoActivitySignout()
		{
		/* override this */
		}
	
	public void onVideoActivitySignin()
		{
		/* override this */
		}
	
	public void onVideoActivitySignup()
		{
		/* override this */
		}
	
	/* "most_recent_track" will be submitted to the tracker when unpausing a video */
	String most_recent_track = null;
	
	public void submit_track_eof()
		{
		if (most_recent_track != null)
			{
			log ("track eof");
			/* this will only be submitted if the tracking has already occurred -- should stop races */
			String preamble = "/" + config.mso + ".9x9.tv";
			//// config.tracker.trackPageView (preamble + "/" + "eof");
			// config.tracker.trackPageView ("eof");
			// config.tracker.dispatch();
			}
		else
			log ("track eof: was not tracking");
		}
			
	public void submit_episode_analytics (String why)
		{
		if (!cumulative_channel_id.equals ("?UNDEFINED"))
			{
			int duration = (int) (cumulative_episode_time / 1000);
			
			log ("submitting episode analytics (" + cumulative_channel_id + ", " 
					+ cumulative_episode_id + "), duration: " + duration + ", reason: " + why);
	
			String channel_name = config.pool_meta (cumulative_channel_id, "name");
			String episode_name = config.program_meta (cumulative_episode_id, "name");			
			
			/* must share the external version of the episode. Some virtual channels have an altered internal format */
			String outside_episode_id = config.outside_episode_id (cumulative_episode_id);
			
			if (duration >= 12)
				{
				track_event ("p" + cumulative_channel_id + "/" + outside_episode_id, "epWatched", channel_name + "/" + episode_name, duration);
				/* this is the advertising counter */
				config.total_play_count++;
				}
			}
		cumulative_episode_id = null;
		cumulative_episode_time = 0;
		}
	
	public void submit_channel_analytics (String why)
		{
		if (!cumulative_channel_id.equals ("?UNDEFINED"))
			{
			int duration = (int) (cumulative_channel_time / 1000);
			
			log ("submitting channel analytics (" + cumulative_channel_id + "), duration: " + duration + ", reason: " + why);
			
			String channel_name = config.pool_meta (cumulative_channel_id, "name");		
			
			if (duration >= 6)
				track_event ("p" + cumulative_channel_id, "pgWatched", channel_name, duration);
			}
		cumulative_channel_id = player_real_channel;
		cumulative_channel_time = 0;
		}
	
	public void submit_pdr()
		{
		if (config != null && program_line != null)
			{
			log ("=================> submit pdr (queuing)");
			
			/* only if we are on the same channel, send this. also send &program= if we can... */
			
			String actual_channel = player_real_channel;
			final String episode = program_line [current_episode_index - 1];
			if (player_real_channel.contains (":"))
				actual_channel = config.program_meta (episode, "real_channel");
			final String channel = actual_channel;
			
			most_recent_track = null;
			
			in_main_thread.postDelayed (new Runnable()
				{
				@Override
				public void run()
					{	
					log ("============================= pdr?");
					if (player_real_channel == null || program_line == null || current_episode_index - 1 >= program_line.length)
						{
						log ("pdr: we have moved away (metadata gone)");
						return;
						}
					
					String current_channel = player_real_channel;
					String current_episode = program_line [current_episode_index - 1];
					
					if (current_channel.contains (":"))
						current_channel = config.program_meta (current_episode, "real_channel");
					
					if (current_channel != null && current_channel.equals (channel) 
							&& current_episode != null && current_episode.equals (episode))
						{
						if (current_episode.contains (":"))
							{
							String fields[] = current_episode.split (":");
							current_episode = fields[1];
							}					
						
						if (config.usertoken != null)
							{
							log ("submitting PDR for " + config.usertoken + " (watched " + current_channel + ")");
							pdr_post_via_http (current_channel, episode);
							}	
						
						/* add to local personal history, if needed */
						onVideoWatched (current_channel, current_episode);
						}
					else
						log ("pdr: have moved away from " + channel + " (now on " + current_channel + "), no PDR submitted");
					}			
				}
			,6000);
			}
		}
		
	public void pdr_post_via_http (final String channel_id, final String episode_id)
		{
		Thread t = new Thread ()
			{
			@Override
			public void run()
				{
				String pdr = null;
				if (episode_id != null)
					pdr = ("0" + "\t" + "w" + "\t" + channel_id + "\t" + episode_id);
				else
					pdr = ("0" + "\t" + "w" + "\t" + channel_id);
				
			    HttpClient httpclient = new DefaultHttpClient();
			    HttpPost httppost = new HttpPost ("http://" + config.api_server + "/playerAPI/pdr");

				ResponseHandler <String> response_handler = new BasicResponseHandler ();
				
			    try
			    	{
			        List <NameValuePair> kv = new ArrayList <NameValuePair> (3);
			        kv.add (new BasicNameValuePair ("mso", config.mso));
			        kv.add (new BasicNameValuePair ("user", config.usertoken));
			        kv.add (new BasicNameValuePair ("pdr", pdr));
			        httppost.setEntity (new UrlEncodedFormEntity (kv));

			        String response = httpclient.execute (httppost, response_handler);
			        
			        Log.i ("vtest", "pdr response: " + response);
			    	}
			    catch (Exception ex)
			    	{
			    	}
				}
			};

		t.start();
		}
	
	public void onVideoWatched (String channel_id, String episode_id)
		{
		/* add to local personal history, but only if we are a Guest */
		if (config != null && config.usertoken == null)
			{
			log ("onVideoWatched channel: " + channel_id);
			// config.local_personal_history_set.add (channel_id);
			}
		}

	public void share()
		{
		if (config != null)
			{
			String channel_id = actual_channel();
			if (channel_id != null)
				{
				if (program_line != null)
					{
					/* must share the external version of the episode id. Some virtual channels have an altered format */
					String outside_episode_id = config.outside_episode_id (program_line [current_episode_index - 1]);
					share_episode (channel_id, outside_episode_id);
					}
				else
					share_episode (channel_id, null);
				}
			}
		}
	
	public void share_episode (String channel_id, String episode_id)
		{
		if (config != null && channel_id != null)
			{
			remember_location();
			Intent i = new Intent (Intent.ACTION_SEND);
			i.setType ("text/plain");
			i.putExtra (Intent.EXTRA_SUBJECT, "Shared to you from 9x9.tv");
			String mso = config.mso != null ? config.mso : "9x9";
			String server = config.api_server;
			if (config.api_server.equals ("api.flipr.tv"))
				server = mso + ".flipr.tv";
			String url = "http://" + server + "/view/p" + channel_id;
			String eprefix = config.is_youtube (channel_id) ? "yt" : "";
			if (episode_id != null)
				 url = url + "/" + eprefix + episode_id;
			i.putExtra (Intent.EXTRA_TEXT, url);
			startActivity (Intent.createChooser (i, "Share this 9x9.tv episode"));
			track_event ("share", "share", "share", 0, channel_id);
			}
		}	
	
	public String get_language()
		{
		String lang = Locale.getDefault().getLanguage();
		if (lang.equals ("zh") || lang.equals ("tw"))
			lang = "cn";
		log ("get_language: " + lang);
		return lang;
		}
	
	public String device_type()
		{
		if (dongle_mode)
			return "dongle";
		else if (is_phone())
			return "phone";
		else
			return "tablet";
		}
		
	public boolean dongle_mode()
		{
		return dongle_mode;
		}
	
	public void make_osd_visible()
		{
		if (!osd_visible_in_full_screen)
			toggle_osd();
		else
			remove_osd_after_a_while();
		}	
	
	public void toggle_osd()
		{
		log ("toggle OSD");
		osd_visible_in_full_screen = !osd_visible_in_full_screen;
		onVideoActivityLayout();
		if (osd_visible_in_full_screen)
			{
			put_dots_on_progress_bar (null);
			setup_progress_bar();
			}
		remove_osd_after_a_while();
		redraw_control_bar_in_thread();
		}	

	public void redraw_control_bar_in_thread()
		{
		in_main_thread.post (redraw_control_bar);
		}
	
	final Runnable redraw_control_bar = new Runnable()
		{
		public void run()
			{
			View vProgressContainer = findViewById (R.id.progresscontainer);
			if (vProgressContainer != null)
				vProgressContainer.invalidate();
			}
		};
		
	public void toggle_osd_and_pause()
		{
		log ("TOGGLE OSD AND PAUSE");
		toggle_osd();
		pause_or_play();
		}
	
	/* wrapper function only to clarify the intent it is called with */
	public void reset_osd_display_time()
		{
		remove_osd_after_a_while();
		}
	
	long keep_osd_up_for = 7000;
	long osd_displayed_at = 0;
	
	public void remove_osd_after_a_while()
		{
		osd_displayed_at = System.currentTimeMillis();
		if (osd_visible_in_full_screen)				
			{
			in_main_thread.postDelayed (new Runnable()
				{
				@Override
				public void run()
					{
					if (osd_visible_in_full_screen)
						{
						long now = System.currentTimeMillis();
						if (now - osd_displayed_at > keep_osd_up_for - 1000)
							toggle_osd();
						}
					}
				}, keep_osd_up_for);
			}
		}	
	
	public int measure_width (int id)
		{
		return measure_width (id, false);
		}
	
	public int measure_width (int id, boolean filled)
		{
		int width = -1;
		View v = findViewById (id);
		if (v != null)
			{
			int max = 0;
			int how = MeasureSpec.UNSPECIFIED;
			if (filled)
				{	 
				max = ViewGroup.LayoutParams.FILL_PARENT;
				how = MeasureSpec.EXACTLY;
				}
			v.measure ((MeasureSpec.makeMeasureSpec (max, how)), (MeasureSpec.makeMeasureSpec (max, how)));
			width = v.getMeasuredWidth();
			log ("measured width: " + width + " other width: " + v.getWidth());
			}
		return width;
		}	
	
	public boolean userIsSignedIn()
		{
		return (config != null && config.usertoken != null);
		}
	
	public boolean is_phone()
		{
		return !dongle_mode && (screen_inches < 6);
		}
	
	public boolean is_large_tablet()
		{
		return !dongle_mode && (screen_inches > 8);
		}

	public boolean able_to_play_video()
		{
		/* override this */
		return true;
		}
	
	
	
	
	
	
	/* GoogleCast Section */
	
	// SessionListener gcast_session_listener = null;
	
    // private ApplicationSession gcast_application_session = null;
    // private CastMessageStream gcast_message_stream = null;

    // private CastContext gcast_context = null;
    private MediaRouter gcast_media_router = null;
    private MediaRouteSelector gcast_media_route_selector = null;
    private MediaRouter.Callback gcast_media_router_callback = null;
    //=CHROMECAST private CastDevice gcast_selected_device = null;
    // private FlingDevice gcast_selected_device = null;
    //=CHROMECAST private Cast.Listener gcast_listener;
    //=MATCHSTICK private Fling.Listener gcast_listener;
    // private boolean gcast_application_started;
    // private boolean gcast_waiting_for_reconnect;
    /* private FireflyApiClient gcast_api_client; /* OLD FIREFLY */
    //=CHROMECAST private GoogleApiClient gcast_api_client; /* CHROMECAST */
    // FlingManager gcast_api_client;
    
    /* private GoogleApiClient gcast_api_client; */
    //=CHROMECAST private ConnectionCallbacks gcast_connection_callbacks;
    //==MATCHSTICKprivate FlingManager.ConnectionCallbacks gcast_connection_callbacks;
    
    public boolean gcast_created = false;
    public boolean gcast_start_pending = false;
    
	public void google_cast_create()
		{
        log ("CCX create");
        
        gcast_media_router = MediaRouter.getInstance (getApplicationContext());
        
        MediaRouteButton gcast_media_route_button = (MediaRouteButton) findViewById (R.id.media_route_button);  
        MediaRouteButton gcast_media_route_button_main = (MediaRouteButton) findViewById (R.id.media_route_button_main);   
        
        try
        	{
        	gcast_media_route_selector = new MediaRouteSelector.Builder().addControlCategory
        			(plasterCast.categoryForCast (config.chromecast_app_name)).build();
        	}
        catch (Exception ex)
        	{
        	alert ("There is an error with Chromecast");
        	log ("CCX stack trace:");
        	ex.printStackTrace();
        	if (gcast_media_route_button != null)
    			((ViewManager) gcast_media_route_button.getParent()).removeView (gcast_media_route_button);
        	if (gcast_media_route_button_main != null)
    			((ViewManager) gcast_media_route_button_main.getParent()).removeView (gcast_media_route_button_main);        	
        	return;
        	}
        
        gcast_media_router_callback = new MyMediaRouterCallback();
        
        if (gcast_media_route_button != null)
        	{
        	log ("CCX setting selector for playback MediaRouteButton");
        	gcast_media_route_button.setRouteSelector (gcast_media_route_selector);
        	}
        
        if (gcast_media_route_button_main != null)
        	{
        	log ("CCX setting selector for main MediaRouteButton");
        	gcast_media_route_button_main.setRouteSelector (gcast_media_route_selector);
        	}
        
        gcast_created = true;        
        if (gcast_start_pending)
        	{
        	gcast_start_pending = false;
        	google_cast_start();
        	}
		}
	
	public void google_cast_start()
		{
		log ("CCX start");
		if (gcast_media_route_selector != null && gcast_media_router_callback != null)
			gcast_media_router.addCallback (gcast_media_route_selector, gcast_media_router_callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
		}
	
	public void google_cast_stop()
		{
		}
	
	public void gcast_pause()
		{
		if (gcast_media_router != null && gcast_media_router_callback != null)
			{
			log ("CCX removing media router callback");
			gcast_media_router.removeCallback (gcast_media_router_callback);
			}
		}
	
	public void gcast_resume()
		{
		if (gcast_media_router != null && gcast_media_route_selector != null && gcast_media_router_callback != null)
			{
			log ("CCX adding media router callback");
			gcast_media_router.addCallback (gcast_media_route_selector, gcast_media_router_callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
			}
		}		

	public void google_cast_destroy()
		{
        teardown();
		}
	
    private class MyMediaRouterCallback extends MediaRouter.Callback
    	{
        @Override
        public void onRouteSelected (MediaRouter router, RouteInfo info)
        	{
            log ("CCX onRouteSelected");
            plasterCast.selectDevice (router, info);
            gcast_launch_receiver();
        	}
        
        @Override
        public void onRouteUnselected (MediaRouter router, RouteInfo info)
        	{
            log ("CCX onRouteUnselected: info=" + info);
            track_event ("uncast", "uncast", "uncast", 0);
            teardown();
            plasterCast.selectDevice (null, null);
        	}
    	}

    /* P L A S T E R C A S T */
    PlasterCast.PlasterListener plasterListener = new PlasterCast.PlasterListener()
		{
    	@Override
        public void onApplicationDisconnected (int errorCode)
        	{
            log ("CCX application has stopped");
            teardown();
        	}
        
    	@Override
        public void onVolumeChanged()
        	{
        	log ("CCX volume changed");
        	}
        
    	@Override
		public String getNamespace()
			{
            return "urn:x-cast:tv.9x9.cast";
			}
		
    	@Override
    	public void onConnected (Bundle connectionHint)
    		{
    		gcast_connected();
    		}
    	
    	@Override
    	public void onApplicationCouldNotLaunch (int statusCode)
    		{    		
    		alert ("Chromecast could not launch! code " + statusCode);
    		}
    	
    	@Override
        public void onMessageReceived (String namespace, String message)
	    	{
	        log ("CCX Chromecast Raw onMessageReceived: " + message);
	        if (message.startsWith ("{"))
	            {
	            try
	            	{
					JSONObject json = new JSONObject (message);
					chromecast_message_received  (json);
	            	}
	            catch (JSONException ex)
	            	{
					ex.printStackTrace();
	            	}
	        	}
	        else
	        	log ("CCX does not seem to be a JSON message: " + message);
	    	}
		
    	@Override
    	public void onConnectionSuspended (int cause)
    		{
    	    log ("onConnectionSuspended");
    		}
    	
    	@Override
		public void onConnectionFailed()
			{
            log ("onConnectionFailed");	
            teardown();
			}
		
    	@Override
		public void onTeardown()
			{
	        pending_seek = false;
	        pending_seek_count = 0;
	         
	        chromecast_volume = 100;
	         
	        chromecasted = false;
	         
	        View vChromecastWindow = findViewById (R.id.chromecast_window);
	        vChromecastWindow.setVisibility (View.GONE);
	         
	        if (player == videoFragment)
	        	show_video_fragment();
	        else if (player == playerFragment)
	        	show_player_fragment();
	         
	        if (player_real_channel != null && program_line != null && current_episode_index <= program_line.length)
	        	{
	        	log ("**** resuming episode, position: " + chromecast_position);
	        	play_nth (player_real_channel, current_episode_index, chromecast_position);
	        	}  
			}
		};
    
    private void gcast_launch_receiver()
    	{
        try
        	{
            plasterCast.createClient (this, plasterListener);
        	}
        catch (Exception ex)
        	{
            log ("CCX failed gcast_launch_receiver");
            ex.printStackTrace();
        	}
    	}

    private void teardown()
    	{
    	log ("CCX teardown");	
    	if (plasterCast != null)
    		plasterCast.teardown();      
    	}
    
    public void chromecast_volume_up()
    	{
    	if (chromecasted)
    		{
    		chromecast_volume += 10;
    		
    		if (chromecast_volume > 100)
    			chromecast_volume = 100;
    		
    		chromecast_send_volume (chromecast_volume);
    		}
    	}
    
    public void chromecast_volume_down()
    	{
    	if (chromecasted)
    		{
    		chromecast_volume -= 10;
    		
    		if (chromecast_volume < 0)
    			chromecast_volume = 0;
    		
    		chromecast_send_volume (chromecast_volume);
    		}
    	}

    private void gcast_connected()
    	{
    	log ("CCX connected!");
    	
        /* don't let device sleep */
        getWindow().addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        long offset = player_real_channel == null ? 0 : player.get_offset();
        
        hide_video_fragment();
        hide_player_fragment();
        
        View vChromecastWindow = findViewById (R.id.chromecast_window);
        vChromecastWindow.setVisibility	(View.VISIBLE);
        
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vChromecastWindow.getLayoutParams();
        layout.height = (int) (screen_width / 1.77);
        vChromecastWindow.setLayoutParams (layout);
        
        player.pause();
        
        sendMessage ("hello hello!");
        
        // gcast_message_stream.join ("MyName");
        chromecast_current_episode (offset);
        // gcast_message_stream.play_video_id (videoFragment.current_video_id());
        
        chromecasted = true;
        chromecast_volume = 100;
        
        track_event ("cast", "cast", "cast", 0);
    	}

    public void hide_player_fragment()
    	{
    	hide_fragment (playerFragment);
    	}
    
    public void hide_video_fragment()
    	{
    	hide_fragment (videoFragment);
    	}
    
    public void hide_fragment (Fragment f)
    	{
    	try
	    	{
	    	if (f != null)
		    	{
	    		if (!f.isHidden())
		    		{
	    			log ("hide fragment");
			        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
			        ft.hide (f);  
			        ft.commit();
		    		}
		    	}
	    	}
    	catch (Exception ex)
    		{
    		ex.printStackTrace();
    		}
    	}

    public void show_video_fragment()
    	{
    	show_fragment (videoFragment);
    	}
    
    public void show_player_fragment()
    	{
    	show_fragment (playerFragment);
    	}
    
    public void show_fragment (Fragment f)
    	{
		try
			{
	    	if (f != null)
		    	{
				if (f.isHidden())
		    		{
					log ("show fragment");
			        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
			        ft.show (f);  
			        ft.commit();
		    		}
		    	}
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
    	}
	    
    public void switch_to_video_fragment()
    	{
    	log ("switch to video fragment");
		try
			{
	    	if (videoFragment != null)
		    	{
		        View vVideoContainer = findViewById (R.id.video_fragment_container);
		        if (vVideoContainer != null)
		        	{
		        	SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) vVideoContainer.getLayoutParams();
		        	layout.width = MATCH_PARENT;
		        	vVideoContainer.setLayoutParams (layout);
		        	}		
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.show (videoFragment);
		        ft.hide (playerFragment);
		        ft.commit();
		        // set_video_alpha (255);
		        View vVideo = videoFragment.getView();
		        if (vVideo != null)
		        	{
		        	FrameLayout f = (FrameLayout) vVideo;
		        	int count = f.getChildCount();
		        	log ("***** TYPE OF VIDEO VIEW: " + vVideo.getClass().getName() + "; children=" + count);
		        	View v = f.getChildAt (0);
		        	log ("***** CHILD IS TYPE: " + v.getClass().getName());
		        	// v.setVisibility (View.VISIBLE);
		        	}
		        View vPlayerSurface = findViewById (R.id.player_fragment_container).findViewById (R.id.surface);
		        if (vPlayerSurface != null)		        	
		        	vPlayerSurface.setVisibility (View.GONE);

		        player = videoFragment;
		    	}
	    	}
		catch (IllegalStateException ex)
			{
			/* This is probably a bug in android. Not sure how to handle it! see: */
			/* http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa */
	    	ex.printStackTrace();
			}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
    	}
    
    
	public void switch_to_player_fragment()
		{
    	log ("switch to player fragment");
		try
			{
	    	if (playerFragment != null)
		    	{
		        View vVideoContainer = findViewById (R.id.video_fragment_container);
		        if (vVideoContainer != null)
		        	{
		        	SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) vVideoContainer.getLayoutParams();
		        	layout.width = 0;
		        	vVideoContainer.setLayoutParams (layout);
		        	}		        
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.show (playerFragment);
		        ft.hide (videoFragment);
		        ft.commit();
		        // set_video_alpha (0);
		        View vPlayerFragment = findViewById (R.id.player_fragment_container);
		        if (vPlayerFragment != null)
			        {
		        	// FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vPlayerFragment.getLayoutParams();
		        	// layout.height = (int) (screen_width / 1.77);
		        	// vPlayerFragment.setLayoutParams (layout);	
			        View vPlayerSurface = vPlayerFragment.findViewById (R.id.surface);
			        if (vPlayerSurface != null)
			        	{
			        	vPlayerSurface.setVisibility (View.VISIBLE);
			        	}		        	
			        }
		        View vVideo = videoFragment.getView();
		        if (vVideo != null)
		        	{
		        	FrameLayout f = (FrameLayout) vVideo;
		        	int count = f.getChildCount();
		        	log ("***** TYPE OF VIDEO VIEW: " + vVideo.getClass().getName() + "; children=" + count);
		        	View v = f.getChildAt (0);
		        	log ("***** CHILD IS TYPE: " + v.getClass().getName());
		        	// v.setVisibility (View.GONE);
		        	}
		        player = playerFragment;
		    	}
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
		}

    public void hide_both_fragments()
		{
    	log ("hide both fragments");
		try
			{
	    	if (videoFragment != null)
		    	{
		        View vVideoContainer = findViewById (R.id.video_fragment_container);
		        if (vVideoContainer != null)
		        	{
		        	log ("set video_fragment_container width to 0");
		        	SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) vVideoContainer.getLayoutParams();
		        	layout.width = 0;
		        	vVideoContainer.setLayoutParams (layout);
		        	}		
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.hide (videoFragment);
		        ft.hide (playerFragment);
		        ft.commit();

		        View vPlayerSurface = findViewById (R.id.player_fragment_container).findViewById (R.id.surface);
		        if (vPlayerSurface != null)		        	
		        	vPlayerSurface.setVisibility (View.GONE);
		        
		        player = null;
		    	}
	    	}
		catch (IllegalStateException ex)
			{
			/* This is probably a bug in android. Not sure how to handle it! see: */
			/* http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa */
	    	ex.printStackTrace();
			}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
		}

    private void sendMessage (String message)
    	{
    	log ("SEND MESSAGE: " + message);
    	plasterCast.sendMessage (message);
    	}            

    boolean chromecasted = false;
             
    public boolean is_chromecasted()
    	{
    	return chromecasted;
    	}
    
    public void chromecast (String channel_id, String episode_id, long offset)
    	{
    	JSONObject json = assemble_play_command_json (channel_id, episode_id, offset, arena);
    	pending_seek = false;
    	try { sendMessage (json.toString()); } catch (Exception ex) { ex.printStackTrace(); }
    	setup_progress_bar();
    	}
    
    boolean pending_seek = false;
    int pending_seek_count = 0;
    
    public void chromecast_seek_percent (float percent)
    	{
    	if (chromecast_is_playing && chromecast_duration > 0 && percent >= 0)
	    	{
	    	try
	    		{
	    		pending_seek = true;
	    		pending_seek_count = chromecast_seek_count;
	    		
				JSONObject payload = new JSONObject();
			    JSONObject data = new JSONObject();
			    
			    int pos = (int) (chromecast_duration * percent);
				data.put ("time", pos);
				
		    	payload.put ("data", data);
		    	payload.put ("type", "seek");
		    	
		    	sendMessage (payload.toString());
	    		}
	    	catch (Exception ex)
	    		{
	    		ex.printStackTrace();
	    		}
	    	}
    	else
    		{
    		log ("CCX: chromecast not playing, won't seek");
    		log ("chromecast duration: " + chromecast_duration);
    		log ("percent: " + percent);
    		log ("chromecast is playing: " + chromecast_is_playing);
    		}
    	}
    		
    public void chromecast_current_episode (long starting)
    	{
    	if (program_line != null && program_line.length > 0)
	    	{
	    	if (current_episode_index <= program_line.length)
	    		{
	    		String episode_id = program_line [current_episode_index - 1];
	    		chromecast (player_real_channel, episode_id, starting);
	    		}
	    	}
    	/* if the attempt fails, try to resume at this point */
    	chromecast_position = starting;
    	}
    
    public boolean chromecast_is_playing = false;
    
    public String current_chromecast_episode = null;
    public String current_chromecast_channel = null;
    
    public long chromecast_position;
    public long chromecast_duration;
    
    public int chromecast_seek_count = 0;
    
    public int chromecast_volume = 100;
    
    public void chromecast_message_received (JSONObject message)
    	{
        try {
            log ("onMessageReceived: " + message);
            if (message.has ("event"))
            	{
                String event = message.getString ("event");
                if (event.equals ("progress"))
                	{
                	JSONObject data = message.getJSONObject ("data");
                	String state = data.getString ("state");
                	if (state.equals ("playing"))
                		onChromecastPausePlay (true);
                	else if (state.equals ("paused"))
                		onChromecastPausePlay (false);
                	
                	try
	            		{
	            		chromecast_seek_count = data.getInt ("seekCount");
	            		log ("seekCount: " + chromecast_seek_count);
	            		}
	            	catch (Exception ex)
	            		{                		
	            		}
                	
                	String position = data.getString ("position");
                	position = position.replaceAll ("\\.\\d+$", "");
                	String duration = data.getString ("duration");
                	duration = duration.replaceAll ("\\.\\d+$", "");
                	int position_int = position.equals ("null") ? 0 : Integer.parseInt (position);
                	int duration_int = duration.equals ("null") ? 0 : Integer.parseInt (duration);
                	String episode = data.getString ("episode");
                	String channel = data.getString ("channel");
                	onChromecastPosition (channel, episode, position_int, duration_int);
                	
                	if (player != null)
                		{
                		/* sometimes the player has audio in the background. Send extra pauses every so often */
                		if (player == playerFragment)
                			player.pause();
                		}
                	
                	if (current_chromecast_episode == null || !current_chromecast_episode.equals (episode))
                		{
                		current_chromecast_episode = episode;
                		onChromecastEpisodeChanged (channel, episode);
                		}                
                	}
                else if (event.equals ("senderConnected"))
                	{
                	log ("Sender connected!");
                	}
                }
        	}
        catch (Exception ex)
        	{
        	ex.printStackTrace();
        	}
    	}
    
    public void onChromecastPausePlay (boolean is_playing)    
    	{    	
		chromecast_is_playing = is_playing;
		ImageView vPausePlay = (ImageView) findViewById (R.id.pause_or_play);
		if (vPausePlay != null)
			vPausePlay.setImageResource (is_playing ? R.drawable.pause_tablet : R.drawable.play_tablet);	
    	}
    
    public void onChromecastPosition (String channel, String episode, int position, int duration)    
    	{    	
		chromecast_position = position;
		chromecast_duration = duration;
		float pct = (float) position / (float) duration;
		
		log ("pending_seek: " + pending_seek + ", pending_seek_count: " + pending_seek_count + ", chromecast_seek_count: " + chromecast_seek_count);
		
		if (chromecast_seek_count > pending_seek_count)
			pending_seek = false;
		
		if (!pending_seek)
			onVideoActivityProgress (chromecast_is_playing, position, duration, pct);
		
		set_knob_visibility (true);
		
		/* this will only hide it if it is somehow not already hidden */
		hide_video_fragment();
		hide_player_fragment();
    	}
    
    public void onChromecastEpisodeChanged (String channel, String episode)
    	{    	
    	log ("CCX: chromecast episode changed to: " + episode);
    	pending_seek = false;
    	update_metadata_mini (episode);
    	}
    
    public void chromecast_send_simple (String command)
    	{
    	JSONObject payload = new JSONObject();
    	try { payload.put ("type", command); } catch (Exception ex) { ex.printStackTrace(); }
    	try { sendMessage (payload.toString()); } catch (Exception ex) { ex.printStackTrace(); }
    	}
   
    public void chromecast_send_volume (int level)
		{    	
		JSONObject payload = new JSONObject();
	    JSONObject data = new JSONObject();
	    
	    try { data.put ("level", level); } catch (JSONException ex) { ex.printStackTrace(); }	    
		try { payload.put ("type", "setVolume"); } catch (Exception ex) { ex.printStackTrace(); }
		try { payload.put ("data", data); } catch (Exception ex) { ex.printStackTrace(); }
		
		try { sendMessage (payload.toString()); } catch (Exception ex) { ex.printStackTrace(); }
		}
    
    public JSONObject assemble_play_command_json (String channel_id, String episode_id, long offset, String arena[])
	    {
		JSONObject payload = new JSONObject();
	    JSONObject data = new JSONObject();
	    JSONArray channel_arena = new JSONArray();
	    JSONArray episode_arena = new JSONArray();
	    JSONObject mso = new JSONObject();
	    try
	    	{
	        if (arena != null)
	        	{
		        for (String arena_channel_id: arena)
		        	{
		        	if (arena_channel_id != null && !channel_id.equals (""))
	    	        	{
	    	        	String name = config.pool_meta (arena_channel_id, "name");
	    	        	String desc = config.pool_meta (arena_channel_id, "desc");
	    	        	String thumb = config.pool_meta (arena_channel_id, "thumb");
	    	        	String nature = config.pool_meta (arena_channel_id, "nature");
	    	        	String extra = config.pool_meta (arena_channel_id, "extra");
	    	        	
	    	        	JSONObject channel_structure = new JSONObject();
	    	        	
	    	        	channel_structure.put ("youtubeId", extra);
	    	        	channel_structure.put ("contentType", nature);
	    	        	int program_count = config.programs_in_real_channel (arena_channel_id);
	    	        	if (program_count > 0)
	    	        		channel_structure.put ("programCount", program_count);		    	        	
	    	        	channel_structure.put ("thumb", thumb);		    	        	
	    	        	channel_structure.put ("description", desc);
	    	        	channel_structure.put ("name", name);
	    	        	channel_structure.put ("id", arena_channel_id);
	    	        	
	    	        	channel_arena.put (channel_structure);
	    	        	}
		        	}
		        data.put ("channelArena", channel_arena);
		        
		        String episodes[] = config.program_line_by_id (channel_id);
		        for (String arena_episode_id: episodes)
		        	{
		        	JSONObject episode_structure = new JSONObject();
		        	
		        	String name = config.program_meta (arena_episode_id, "name");
		        	String timestamp = config.program_meta (arena_episode_id, "timestamp");
		        	
		        	if (timestamp == null)
		        		timestamp = "";
		        	
		        	String url = config.best_url_or_first_subepisode (arena_episode_id);			        	
		        	
		        	if (url == null)
		        		{
		        		log ("episode \"" + arena_episode_id + "\" has no url! (channel " + channel_id + ")");		
		        		config.dump_episode_details (arena_episode_id);
		        		}
		        	
		        	episode_structure.put ("id", arena_episode_id);
		        	episode_structure.put ("name", name);
		        	episode_structure.put ("published", timestamp);
		        	
		    	    JSONArray videos = new JSONArray();		    	    
		    	    JSONObject video_structure = new JSONObject();		    	    
		    	    if (!url.contains ("youtube.com"))
		    	    	{
		    			if (url.contains (";"))
		    				url = url.replaceAll (";.*$", "");
		    	    	video_structure.put ("url", url);
		    	    	}
		    	    else
			    	    video_structure.put ("id", video_id_of (url));
		    	    videos.put (video_structure);
		    	    episode_structure.put ("videos", videos);		    	    
		        	episode_arena.put (episode_structure);
		        	}
		        
		        data.put ("episodeArena", episode_arena);
	        	}
	        else
	        	Log.i ("vtest", "json: no channel arena");
	        
	        mso.put ("name", config.mso);
	        mso.put ("title", config.mso_title != null ? config.mso_title : config.app_name);
	        mso.put ("supported-region", config.supported_region);
	        mso.put ("region",  config.region);
	        mso.put ("preferredLangCode",  config.mso_preferred_lang_code);
	        	        
			data.put ("channelId", channel_id);
			data.put ("episodeId", episode_id);
			data.put ("startSeconds", offset);
			
			data.put ("mso", mso);
	    	payload.put ("data", data);
	    	payload.put ("type", "play");	        
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
	    
	    return payload;
		}
 	}