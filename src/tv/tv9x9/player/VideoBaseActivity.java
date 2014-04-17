package tv.tv9x9.player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.tv9x9.player.switchboard.LocalBinder;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger.LogLevel;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.flurry.android.FlurryAgent;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;

/*
import com.google.cast.ApplicationChannel;
import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.google.cast.SessionError;
*/

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.GooglePlayServicesClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
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

public class VideoBaseActivity extends FragmentActivity implements YouTubePlayer.OnFullscreenListener
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
	int video_cutoff_time = -1;
	
	/* time for next POI trigger */
	int video_next_trigger = -1;

	/* time to remove OSD for most recent POI */
	int video_release_trigger = -1;
	
	/* the index number of the POI (for debugging purposes) */
	int video_trigger_id = -1;
	
	/* after launching POI, remember where we are */
	boolean restore_video_location = false;
	
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
	
	/* dm.density shortcut */
	float density = 1f;

	static int pixels_2 = 2;
	static int pixels_3 = 3;	
	static int pixels_5 = 5;
	static int pixels_6 = 6;	
	static int pixels_7 = 7;	
	static int pixels_10 = 10;
	static int pixels_12 = 12;	
	static int pixels_15 = 15;	
	static int pixels_20 = 20;
	static int pixels_25 = 25;	
	static int pixels_30 = 30;
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
	
	public VideoFragment videoFragment = null;

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
		
		/* save this, to avoid passing context many layers deep */
		inflater = getLayoutInflater();
		
		FrameLayout vMain = (FrameLayout) findViewById (R.id.main);
		
		/* Lili wants to support two different layouts for playback page */
		View video_layer = inflater.inflate (R.layout.video_layer_new, vMain, true);		
		
	    videoFragment = (VideoFragment) getSupportFragmentManager().findFragmentById (R.id.video_fragment_container);
	    videoFragment.set_context (this);
	    
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
		/*
		if (config != null)
			{			
			if (restore_video_location)
				restore_location();
			}
		*/
		launch_in_progress = false;
		gcast_resume();
		}
		
	@Override
	protected void onDestroy()
		{
		log ("VideoActivity onDestroy");
		
		if (progress_timer != null)
			progress_timer.cancel();
		
		google_cast_destroy();
		super.onDestroy();
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
	    pixels_5   = (int) (5   * dm.density);
	    pixels_6   = (int) (6   * dm.density); 	    
	    pixels_7   = (int) (7   * dm.density); 	    
	    pixels_10  = (int) (10  * dm.density); 
	    pixels_12  = (int) (12  * dm.density); 	    
	    pixels_15  = (int) (15  * dm.density); 	    
	    pixels_20  = (int) (20  * dm.density); 
	    pixels_25  = (int) (25  * dm.density); 	    
	    pixels_30  = (int) (30  * dm.density); 
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
		    screen_width = dm.widthPixels;
		    screen_height = dm.heightPixels;
		    screen_density = dm.density;
			}
		catch (Exception ex)
			{
			/* nothing */
			}
		}
	
	public void flurry_log (String event)
		{
		if (event != null)
			FlurryAgent.logEvent (event);
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
			// VERBOSE | INFO | DEBUG | WARNING
			ga.getLogger().setLogLevel(LogLevel.VERBOSE);
			// String tracking_id = getString (R.string.ga_trackingId);
			// String tracking_id = "UA-21595932-1";
			String tracking_id = config.google_analytics;
			// Tracker tr = EasyTracker.getInstance (this);
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
			return;		
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
			
	float downX = 0, downY = 0;
	boolean x_movement_started = false;
	boolean y_movement_started = false;
	boolean started_inside_container = false;
	boolean big_thing_moving = false;
	
	public boolean dispatchTouchEvent (MotionEvent event)
		{
		super.dispatchTouchEvent (event);
		
		int action = event.getAction();
		View vContainer = findViewById (chromecasted ? R.id.chromecast_window : R.id.video_fragment_container);
				
		if (action == MotionEvent.ACTION_DOWN)
			{
			big_thing_moving = false;
			// SpecialFrameLayout yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);
			downX = event.getX();
			downY = event.getY();
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
			log ("[dispatch] ACTION UP, x=" + event.getX() + ", y=" + event.getY());

			int deltaX = (int) (downX - event.getX());
			int deltaY = (int) (downY - event.getY());
			
			if (big_thing_moving)
				onBigThingUp (deltaX, deltaY);
			else if (point_inside_view (event.getX(), event.getY(), vContainer))
				{
				if (Math.abs (deltaX) > pixels_80 || Math.abs (deltaY) > pixels_80)
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

			if (big_thing_moving)
				onBigThingMove (deltaX, deltaY);
			else if (video_is_minimized() && point_inside_view (event.getX(), event.getY(), vContainer))
				onVideoHorizontal (deltaX);
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
	
	public void skip_video (int direction)
		{
		if (videoFragment.ready())
			{
			try
				{
	        	int offset = videoFragment.get_offset();
	        	int duration = videoFragment.get_duration();
	        	int stride = duration / 5;
	        	int new_position = offset + stride * direction;
	        	if (new_position < 0) new_position = 0;
	        	if (new_position > duration) new_position = duration;
				videoFragment.seek (new_position);
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
			videoFragment.pause();
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
			videoFragment.play();
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
			if (videoFragment.is_playing())
				{
				log ("pause_or_play: pause");
				videoFragment.pause();
				}
			else
				{
				log ("pause_or_play: play");
				videoFragment.play();
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
		load_channel_then (channel_id, play_inner, "1", null);
		}
	
	public void play_nth (String channel_id, int position)
		{
		player_real_channel = channel_id;
		video_play_pending = true;
		load_channel_then (channel_id, play_inner, Integer.toString (position), null);
		}	

	public void play_nth (String channel_id, int position, int start_msec)
		{
		player_real_channel = channel_id;
		video_play_pending = true;
		load_channel_then (channel_id, play_inner, Integer.toString (position), new Integer (start_msec));
		}
	
	final Callback play_inner = new Callback()
		{
		@Override
		public void run_string_and_object (String arg1, Object arg2)
			{
			int position = Integer.parseInt (arg1);
			int start_msec = arg2 == null ? 0 : (Integer) arg2;
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
		/* We want to call yt_player.stop() here. BUT THERE IS NO STOP API */
		video_play_pending = true;
		load_channel_then (channel_id, play_episode_inner, episode_id, null);
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
		
	public void load_channel_then (final String channel_id, final Callback callback, final String arg1, final Object arg2)
		{
		/* if we already have episodes, done */
		
		if (config.programs_in_real_channel (channel_id) > 0)
			{
			log ("load channel " + channel_id + " then: has episodes");
			if (callback != null)
				callback.run_string_and_object (arg1, arg2);
			return;
			}
		
		/* if the channel is known but without episodes */
		
		String name = config.pool_meta(channel_id, "name");
		if (name != null && !name.equals (""))
			{
			log ("load channel " + channel_id + " then: known, but no programs");
			ytchannel.fetch_and_parse_by_id_in_thread (config, channel_id, in_main_thread, new Runnable()
				{
				@Override
				public void run()
					{
					if (callback != null)
						callback.run_string_and_object (arg1, arg2);
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
				ytchannel.fetch_and_parse_by_id_in_thread (config, channel_id, in_main_thread, callback, arg1, arg2);
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
				if (program_line [i].equals (episode_id))
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
			config.add_runt_episode (player_real_channel, episode_id);
			play_specified_episode (episode_id);
			}
		else
			toast_by_resource (R.string.episode_not_found);
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
	
	protected void ready()
		{
		log ("ready? started=" + started + " player is " + ((videoFragment.ready()) ? "activated" : "not activated yet"));
		if (started == 3)
			{
			config = mService.get_metadata (identity);
			
			if (config.flurry_id != null && !config.flurry_id.isEmpty())
				FlurryAgent.onStartSession (this, config.flurry_id);
			FlurryAgent.setLogEnabled (true);
			FlurryAgent.setLogLevel (Log.DEBUG);
			
			google_cast_create();
			if (videoFragment.ready())
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
		
		videoFragment.reset_time_played();
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
	
	public void try_to_play_episode (int episode)
		{
		try_to_play_episode (episode, 0);
		}
	
	public void try_to_play_episode (int episode, int start_msec)
		{
		log ("try to play episode: " + episode);		
		
		video_has_started = false;
		playing_begin_titlecard = playing_end_titlecard = false;
		
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
			videoFragment.add_to_time_played();
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
					play_youtube_url (url, start_msec, -1);
					}
				else
					{
					/* ignore start_msec problem here, since subepisodes seem to be on their way out */
					log ("play first subepisode of: " + episode_id + " (of " + subepisodes_this_episode + ")");
					play_current_subepisode();
					}
				
				put_dots_on_progress_bar (episode_id);
				}
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
					play_current_subepisode (percent_into_subepisode);
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
		play_current_subepisode (0f);
		}
	
	/* save these to detect when we've moved on */
	String current_begin_titlecard_id = null, current_end_titlecard_id = null;
	
	public void play_current_subepisode (final float percent)
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
				
				int start_msec = (t_start != null && !t_start.equals("")) ? Integer.parseInt (t_start) * 1000 : 0;
				int end_msec = (t_end != null && !t_end.equals("")) ? Integer.parseInt (t_end) * 1000 : -1;
				int duration_msec = (t_duration != null && !t_duration.equals ("")) ? Integer.parseInt (t_duration) * 1000 : -1;
				
				// current_subepisode++;
				int extra_offset = (int) (duration_msec * percent);
				
				log ("start_msec: " + start_msec + ", duration_msec: " + duration_msec + ", percent: " + percent + ", extra offset: " + extra_offset);
				play_youtube_url (url, start_msec + extra_offset, end_msec);
				}
			}
		else
			{
			log ("no more subepisodes in " + episode_id);
			current_subepisode = 0;
			next_episode_with_rules();
			}
		}
	
	public void after_begin_titlecard_inner (float abt_percent)
		{
		/* if program_line becomes null, it means we have moved away */
		if (program_line != null)
			{
			log ("after begin titlecard: " + current_begin_titlecard_id);
			
			set_video_visibility (View.VISIBLE);
			set_video_alpha (255);
			
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
		set_video_alpha (255);
		
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

	public void play_youtube_url (String url, int start_msec, int end_msec)
		{
		if (url != null && !url.equals (""))
			{
			if (!url.contains ("youtube"))
				{
				log ("playvideo url: " + url);				
				}
			else if (url.contains ("youtube"))
				{
				String video_id = video_id_of (url);
				log ("[playvideo] YouTube id: " + video_id);
				set_poi_trigger (false);
				play_youtube (video_id, start_msec, end_msec);
				}
			}
		}
	
	String video_id_of (String url)
		{
		String video_id = null;
		int voffset = url.indexOf ("?v=");
		if (voffset > 0)
			{
			try { video_id = url.substring (voffset + 3, voffset + 3 + 11); } catch (Exception ex) {};
			}
		return video_id;
		}
	
	void play_youtube (String id, int start_msec, int end_msec)
		{
		play_youtube_using_new_api (id, start_msec, end_msec);
		
		/* save these so that in case of interruption, we can restart */
		restore_video_id = id;
		restore_video_end_msec = end_msec;
		
		in_main_thread.post (update_metadata);
		// submit_pdr();
		onVideoActivityPlayback();
		}

	public void onVideoActivityPlayback()
		{
		/* override this */
		}
	
	void play_youtube_using_new_api (String id, int start_msec, int end_msec)
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
		
		try
			{
			videoFragment.set_manage_audio_focus (false);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		View vTitlecard = findViewById (R.id.titlecard);
		vTitlecard.setVisibility (View.GONE);				
		
		if (progress_timer == null)
			{
			progress_timer = new Timer();
        	progress_timer.scheduleAtFixedRate (new TickTask(), 1000, 1000);
			}
	
		videoFragment.set_listeners();
		

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
		
	public void onVideoActivityVideoStarted (YouTubePlayer player)
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
        	if (videoFragment != null)
	        	{
	            if (videoFragment.is_playing())
	            	{
	            	in_main_thread.post (update_progress_bar);
	            	int offset = 0;
	            	int duration = 0;
	            	try
		            	{
			        	offset = videoFragment.get_offset();
			        	duration = videoFragment.get_duration();
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
		
	boolean haz_poi = false;
		
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
			        	// alert ("POI CLICKED: " + poi_action_1_url);
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
	
	// {"message": "更多壹傳媒內幕,盡在媒體停看聽","button": [{ "text": "了解更多", "actionUrl": "http://www.9x9.tv/view?ch=1380&ep=6789]" }]}
		
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
		
		if (program_line == null || program_line.length == 0)
			{
			log ("set poi trigger: no programs");
			return;
			}
		
		if (current_subepisode > 0)
			{
			/* POI exists only for subepisodes */
			
			int nth = -1;
			String episode_id = program_line [current_episode_index - 1];
			
			int offset = 0;
			if (use_current_offset)
				{
				offset = videoFragment.get_offset();
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
	
	String restore_video_id = null;
	int restore_video_position = -1;
	int restore_video_end_msec = -1;
	int restore_video_current_episode_index = -1;
	int restore_video_current_subepisode = -1;
	int restore_video_visibility = -1;
	
	public void remember_location()
		{
		if (!chromecasted)
			{
			restore_video_location = true;
			restore_video_position = videoFragment.get_most_recent_offset();
			restore_video_current_episode_index = current_episode_index;
			restore_video_current_subepisode = current_subepisode;
			log ("VIDEO remember location: " + restore_video_position);
			pause_video();
		
			restore_video_visibility = get_video_visibility();
			set_video_visibility (View.INVISIBLE);
			}
		}
		
	public void restore_location()
		{
		restore_video_location = false;
		pending_restart = true;
		/* note: leave pending_restart as true! */
		log ("VIDEO restore location: " + restore_video_position);
		log ("VIDEO restore video_id: " + restore_video_id);
		log ("VIDEO restore visibility == View.VISIBLE: " + (restore_video_visibility == View.VISIBLE));		
		log ("VIDEO restore able to play video: " + able_to_play_video());
		
		current_episode_index = restore_video_current_episode_index;
		current_subepisode = restore_video_current_subepisode;
		
		set_video_visibility (restore_video_visibility);
		if (restore_video_id != null && able_to_play_video() && restore_video_visibility == View.VISIBLE)
			play_youtube (restore_video_id, restore_video_position, restore_video_end_msec);
		}
	
	public void launch_player_url (String url)
		{
		// http://beagle.9x9.tv/playback?ch=9754&ep=15952

		String channel = null, episode = null;
		
		Pattern chPattern = Pattern.compile ("ch=(\\d+)");
		Matcher chMatcher = chPattern.matcher (url);
		if (chMatcher.find())
			channel = chMatcher.group (1);

		Pattern epPattern = Pattern.compile ("ep=([\\w\\d]+)");
		Matcher epMatcher = epPattern.matcher (url);
		if (epMatcher.find())
			episode = epMatcher.group (1);
		
		log ("launch player URL: ch=|" + channel + "| episode=|" + episode + "|");
		launch_player_with_episode (channel, episode);
		}

	String poi_episode_id = null;
	
	public void launch_player_with_episode (String channel_id, String episode_id)
		{
		if (channel_id != null && !channel_id.equals("") && !launch_in_progress)
			{
			launch_in_progress = true; 
			if (channel_id.contains (":"))
				launch_tv_specific_episode (channel_id, episode_id);
			else
				{
				poi_episode_id = episode_id; /* HACK */
				ytchannel.full_channel_fetch (in_main_thread, launch_episode_inner, config, channel_id);
				}
			}
		}
	
	final Callback launch_episode_inner = new Callback()
		{
		@Override
		public void run_string (String channel_id)
			{
			launch_tv_specific_episode (channel_id, poi_episode_id);
			}
		};

	public void launch_tv_specific_episode (String channel_id, String episode_id)
		{
		/*
		Intent wIntent = new Intent (VideoBaseActivity.this, tv4.class);
		wIntent.putExtra ("tv.9x9.channel", channel_id);
		if (episode_id != null)
			wIntent.putExtra ("tv.9x9.episode", episode_id);
		wIntent.putExtra ("tv.9x9.single-channel", true);
		if (episode_id != null)
			wIntent.putExtra ("tv.9x9.single-episode", true);			
		startActivity (wIntent);
		*/
		}	
	
	final Runnable update_progress_bar = new Runnable()
		{
		public void run()
			{
			if (!chromecasted)
				{
				int offset = 0;
				int duration = 0;
				
				float pct = 0f;
				
				boolean is_playing = false;
				
	            if (videoFragment.is_playing())
		            {
	            	is_playing = true;
	            	
		        	offset = videoFragment.get_offset();
		        	duration = videoFragment.get_duration();
		        	
		        	pct = (float) offset / (float) duration;
		            }
	            
				onVideoActivityProgress (is_playing, offset, duration, pct);
				}
			}
		};

	public void onVideoActivityProgress (boolean is_playing, int offset, int duration, float pct)
		{
		/* override this */
		}
	
	/* here is a default updater for the progress bar */
		
	void videoActivityUpdateProgressBar (int offset, int duration)
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
			int adjusted_offset = offset;
			int total_duration = duration;
			
			if (current_subepisode > 0)
				{
				total_duration = 1000 * Integer.parseInt (config.program_meta (episode_id, "total-duration"));
				
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
					    			if (videoFragment.is_playing())
						    			{
						    			int duration = videoFragment.get_duration();
						    			int seekpos = (int) (duration * percent);
						    			videoFragment.seek (seekpos);
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
	
	void invalidate_progress_bar()
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
				videoFragment.set_full_screen (false);
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
			videoFragment.set_full_screen (true);
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
	
		String txt_signed_out = getResources().getString (R.string.signed_out_successfully);
		alert (txt_signed_out);
		
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
		int duration = (int) (cumulative_episode_time / 1000);
		
		log ("submitting episode analytics (" + cumulative_channel_id + ", " 
				+ cumulative_episode_id + "), duration: " + duration + ", reason: " + why);

		String channel_name = config.pool_meta (cumulative_channel_id, "name");
		String episode_name = config.program_meta (cumulative_episode_id, "name");			
		
		if (duration >= 6)
			track_event ("p" + cumulative_channel_id + "/" + cumulative_episode_id, "epWatched", channel_name + "/" + episode_name, duration);	
		
		cumulative_episode_id = null;
		cumulative_episode_time = 0;
		}
	
	public void submit_channel_analytics (String why)
		{
		int duration = (int) (cumulative_channel_time / 1000);
		
		log ("submitting channel analytics (" + cumulative_channel_id + "), duration: " + duration + ", reason: " + why);
		
		String channel_name = config.pool_meta (cumulative_channel_id, "name");		
		
		if (duration >= 6)
			track_event ("p" + cumulative_channel_id, "pgWatched", channel_name, duration);
		
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
						
						String nature = config.pool_meta (current_channel, "nature");
						String channel_name = config.pool_meta (current_channel, "name");
						String episode_name = config.program_meta(current_episode, "name");							
						
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
			config.local_personal_history_set.add (channel_id);
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
					String episode_id = program_line [current_episode_index - 1];
					share_episode (channel_id, episode_id);
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
    private CastDevice gcast_selected_device = null;
    private Cast.Listener gcast_listener;
    private boolean gcast_application_started;
    private boolean gcast_waiting_for_reconnect;
    private Cast9x9Channel gcast_channel;
    private GoogleApiClient gcast_api_client;
    private ConnectionCallbacks gcast_connection_callbacks;
    private ConnectionFailedListener gcast_connection_failed_listener;
    
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
        			(CastMediaControlIntent.categoryForCast (config.chromecast_app_name)).build();
        	}
        catch (Exception ex)
        	{
        	alert ("There is an error with Chromecast");
        	ex.printStackTrace();
        	if (gcast_media_route_button != null)
    			((ViewManager) gcast_media_route_button.getParent()).removeView (gcast_media_route_button);
        	if (gcast_media_route_button_main != null)
    			((ViewManager) gcast_media_route_button_main.getParent()).removeView (gcast_media_route_button_main);        	
        	return;
        	}
        
        gcast_media_router_callback = new MyMediaRouterCallback();
        
        if (gcast_media_route_button != null)
        	gcast_media_route_button.setRouteSelector (gcast_media_route_selector);
     
        if (gcast_media_route_button_main != null)
        	gcast_media_route_button_main.setRouteSelector (gcast_media_route_selector);
        
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
            gcast_selected_device = CastDevice.getFromBundle (info.getExtras());
            gcast_launch_receiver();
        	}
        
        @Override
        public void onRouteUnselected (MediaRouter router, RouteInfo info)
        	{
            log ("CCX onRouteUnselected: info=" + info);
            teardown();
            gcast_selected_device = null;
        	}
    	}

    private void gcast_launch_receiver()
    	{
        try
        	{
            gcast_listener = new Cast.Listener()
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
            	};
            	
            // Connect to Google Play services
            gcast_connection_callbacks = new ConnectionCallbacks();
            gcast_connection_failed_listener = new ConnectionFailedListener();
            Cast.CastOptions.Builder api_options_builder = Cast.CastOptions.builder (gcast_selected_device, gcast_listener);
            gcast_api_client = new GoogleApiClient.Builder (this)
                    .addApi (Cast.API, api_options_builder.build())
                    .addConnectionCallbacks (gcast_connection_callbacks)
                    .addOnConnectionFailedListener (gcast_connection_failed_listener)
                    .build();

            gcast_api_client.connect();
        	}
        catch (Exception ex)
        	{
            log ("CCX failed gcast_launch_receiver");
            ex.printStackTrace();
        	}
    	}

    class Cast9x9Channel implements MessageReceivedCallback
    	{
        public String getNamespace()
        	{
            return "urn:x-cast:tv.9x9.cast";
        	}

        @Override
        public void onMessageReceived (CastDevice castDevice, String namespace, String message)
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
            	log ("CCX does not seem to be a JSON message");
        	}
    	}

    private void teardown()
    	{
        if (gcast_api_client != null)
        	{
            if (gcast_application_started)
            	{
                try
                	{
                    Cast.CastApi.stopApplication (gcast_api_client);
                    if (gcast_channel != null)
                    	{
                        Cast.CastApi.removeMessageReceivedCallbacks (gcast_api_client, gcast_channel.getNamespace());
                        gcast_channel = null;
                    	}
                	}
                catch (Exception ex)
                	{
                    log ("Exception while removing channel");
                    ex.printStackTrace();
                	}
                gcast_application_started = false;
            	}
            
            try
	            {
	            if (gcast_api_client.isConnected())
	                gcast_api_client.disconnect();
	            }
            catch (Exception ex)
	            {
	            ex.printStackTrace();
	            }
            
            gcast_api_client = null;
            
            track_event ("uncast", "uncast", "uncast", 0);
        	}       
        
        gcast_selected_device = null;
        gcast_waiting_for_reconnect = false;
        
        pending_seek = false;
        pending_seek_count = 0;
        
        chromecast_volume = 100;
        
        chromecasted = false;
        
        View vChromecastWindow = findViewById (R.id.chromecast_window);
        vChromecastWindow.setVisibility	(View.GONE);
        
        show_video_fragment();
        
        if (player_real_channel != null && program_line != null && current_episode_index <= program_line.length)
        	{
        	log ("**** resuming episode, position: " + chromecast_position);
        	play_nth (player_real_channel, current_episode_index, chromecast_position);
        	}        
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
    
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener
    	{
        @Override
        public void onConnectionFailed (ConnectionResult result)
        	{
            log ("onConnectionFailed");	
            teardown();
        	}
    	}
    

    
    
    
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks
    	{
    	@Override
    	public void onConnected (Bundle connectionHint)
    		{
    		log ("CCX onConnected");

    		if (gcast_api_client == null)
    			{
    			log ("CCX: we were disconnected, gcast_api_client is null");
    			return;
    			}

    		if (!gcast_api_client.isConnected())
    			{
    			log ("CCX: gcast_api_client says it is not connected");
    			return;
    			}
    		
    		try
    			{
		        if (gcast_waiting_for_reconnect)
		        	{
		        	gcast_waiting_for_reconnect = false;
		
		            if ((connectionHint != null) && connectionHint.getBoolean (Cast.EXTRA_APP_NO_LONGER_RUNNING))
		            	{
		            	log ("CCX receiver app is no longer running");
		                teardown();
		            	}
		            else
		            	{
		                // Re-create the custom message channel
		                try
		                	{
		                    Cast.CastApi.setMessageReceivedCallbacks (gcast_api_client, gcast_channel.getNamespace(), gcast_channel);
		                    log ("CCX setMessageReceivedCallbacks successful");
		                	}
		                catch (IOException ex)
		                	{
		                    log ("CCX exception while creating channel");
		                    ex.printStackTrace();
		                	}
		            	}
		        	}
		        else
			        	{
			            // Launch the receiver app
			            Cast.CastApi
			                    .launchApplication(gcast_api_client, config.chromecast_app_name, false)
			                    .setResultCallback(
			                            new ResultCallback<Cast.ApplicationConnectionResult>() {
			                                @Override
			                                public void onResult(
			                                        ApplicationConnectionResult result) {
			                                    Status status = result.getStatus();
			                                    log ("CCX ApplicationConnectionResultCallback.onResult: statusCode " + status.getStatusCode());
			                                    if (status.isSuccess())
			                                    	{
			                                        ApplicationMetadata applicationMetadata = result
			                                                .getApplicationMetadata();
			                                        String sessionId = result
			                                                .getSessionId();
			                                        String applicationStatus = result
			                                                .getApplicationStatus();
			                                        boolean wasLaunched = result
			                                                .getWasLaunched();
			                                        log ("CCX application name: "
			                                                        + applicationMetadata
			                                                                .getName()
			                                                        + ", status: "
			                                                        + applicationStatus
			                                                        + ", sessionId: "
			                                                        + sessionId
			                                                        + ", wasLaunched: "
			                                                        + wasLaunched);
			                                        gcast_application_started = true;
			
			                                        // Create the custom message
			                                        // channel
			                                        gcast_channel = new Cast9x9Channel();
			                                        try
			                                        	{
			                                            Cast.CastApi
			                                                    .setMessageReceivedCallbacks(
			                                                            gcast_api_client,
			                                                            gcast_channel
			                                                                    .getNamespace(),
			                                                            gcast_channel);
			                                        	}
			                                        catch (IOException ex)
			                                        	{
			                                            log ("CCX exception while creating channel");
			                                            ex.printStackTrace();
			                                        	}
			
			                                        gcast_connected();
			                                    } else {
			                                        log ("CCX application could not launch");
			                                        alert ("Chromecast could not launch! code " + status.getStatusCode());
			                                        teardown();
			                                    }
			                                }
			                            });
			        }
			    } 
			    catch (Exception ex)
			    	{
			        log ("CCX failed to launch application");
			        ex.printStackTrace();
			    	}
    		}

	@Override
	public void onConnectionSuspended(int cause)
		{
	    log ("CCX onConnectionSuspended");
	    gcast_waiting_for_reconnect = true;
		}
    }

    
    private void gcast_connected()
    	{
    	log ("CCX connected!");
    	
        /* don't let device sleep */
        getWindow().addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        int offset = player_real_channel == null ? 0 : videoFragment.get_offset();
        
        hide_video_fragment();
        
        View vChromecastWindow = findViewById (R.id.chromecast_window);
        vChromecastWindow.setVisibility	(View.VISIBLE);
        
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vChromecastWindow.getLayoutParams();
        layout.height = (int) (screen_width / 1.77);
        vChromecastWindow.setLayoutParams (layout);
        
        /* pause_video(); */
        videoFragment.pause();
        
        sendMessage ("hello hello!");
        
        // gcast_message_stream.join ("MyName");
        chromecast_current_episode (offset);
        // gcast_message_stream.play_video_id (videoFragment.current_video_id());
        
        chromecasted = true;
        chromecast_volume = 100;
        
        track_event ("cast", "cast", "cast", 0);
    	}

    public void hide_video_fragment()
    	{
    	try
	    	{
	    	if (videoFragment != null)
		    	{
	    		if (!videoFragment.isHidden())
		    		{
			        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
			        ft.hide (videoFragment);  
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
		try
			{
	    	if (videoFragment != null)
		    	{
				if (videoFragment.isHidden())
		    		{
			        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
			        ft.show (videoFragment);  
			        ft.commit();
		    		}
		    	}
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
    	}
    
    public void attach_video_fragment()
    	{
		log ("videoFragment attach");
	    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
        ft.attach (videoFragment);  
        ft.commit();
    	}
    
    public void detach_video_fragment()
		{
		log ("videoFragment detach");
	    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
	    ft.detach (videoFragment);  
	    ft.commit();
		}
	    
    private void sendMessage (String message)
    	{
    	log ("SEND MESSAGE: " + message);
        if (gcast_api_client != null && gcast_channel != null)
        	{
            try
            	{
                Cast.CastApi.sendMessage (gcast_api_client, gcast_channel.getNamespace(), message)
                        .setResultCallback (new ResultCallback<Status>()
                        		{
                            	@Override
                            	public void onResult(Status result)
                            		{
                            		if (!result.isSuccess())
	                                    log ("Sending message failed");                            }
                        });
            	} 
            catch (Exception ex)
            	{
                log ("Exception while sending message");
                ex.printStackTrace();
            	}
        	}
        else
        	{
        	log ("CCX: not sent");
        	if (gcast_api_client == null)
        		log ("CCX: gcast_api_client is null");
        	if (gcast_channel == null)
        		log ("CCX: gcast_channel is null");
        	}
    	}            

    boolean chromecasted = false;
             
    public void chromecast (String channel_id, String episode_id, int offset)
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
    		
    public void chromecast_current_episode (int starting)
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
    
    public int chromecast_position;
    public int chromecast_duration;
    
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
    
    public JSONObject assemble_play_command_json (String channel_id, String episode_id, int offset, String arena[])
	    {
		JSONObject payload = new JSONObject();
	    JSONObject data = new JSONObject();
	    JSONArray channel_arena = new JSONArray();
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
	        // Log.i ("vtest", "JSON: " + payload.toString());    	        
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
	    
	    return payload;
		}
 	}