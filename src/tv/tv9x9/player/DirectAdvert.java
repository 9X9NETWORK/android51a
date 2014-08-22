package tv.tv9x9.player;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tv.tv9x9.player.Social.PingTask;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.google.analytics.tracking.android.Logger.LogLevel;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;

public class DirectAdvert extends RelayActivity 
		implements OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback
	{
	String advert_url = null;
	String defer_url = null;
	String advert_id = null;
	String advert_name = null;
	
	long duration = -1;
	Timer update_timer = null;
	
	private boolean mIsVideoSizeKnown = false;
	private int mVideoWidth = 0;
	private int mVideoHeight = 0;
	private MediaPlayer mMediaPlayer = null;
	private SurfaceView mPreview = null;
	private SurfaceHolder holder = null;
	private boolean mIsVideoReadyToBePlayed = false;
	private boolean mSurfaceCreated = false;
	
	int milliseconds_remaining = 6500;
	boolean countdown_started = false;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		
		identity = "DirectAdvert";
		
		log ("onCreate");
		
		if (!LibsChecker.checkVitamioLibs (this))
			return;
		
		requestWindowFeature (Window.FEATURE_NO_TITLE);
		
		setContentView (R.layout.direct_ad_layer);
		
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		
		View ad_layer = findViewById (R.id.direct_ad_layer);
		ad_layer.setVisibility (View.VISIBLE);
				
		adjust_layout (landscape);
		int height = (int) ((float) screen_width / 1.77);
		set_skip_phase (1);
				
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();	
		if (extras != null)
			{
			advert_url = extras.getString ("tv.9x9.advert");
			advert_id = extras.getString ("tv.9x9.advert_id");
			advert_name = extras.getString ("tv.9x9.advert_name");
			}
		
		// advert_url = "http://download.wavetlan.com/SVV/Media/HTTP/H264/Other_Media/H264_test5_voice_mp4_480x360.mp4";
		// advert_url = "http://download.wavetlan.com/SVV/Media/HTTP/MP4/ConvertedFiles/Media-Convert/Unsupported/dw11222.mp4";
		
		if (is_phone())
			{
			TextView vText1a = (TextView) findViewById (R.id.skip_ad_button_phase_1_text_1);
			vText1a.setTextSize (TypedValue.COMPLEX_UNIT_SP, 12);			
			TextView vText1b = (TextView) findViewById (R.id.skip_ad_button_phase_1_text_2);
			vText1b.setTextSize (TypedValue.COMPLEX_UNIT_SP, 12);	    
			TextView vText1c = (TextView) findViewById (R.id.skip_ad_button_phase_1_text_3);
			vText1c.setTextSize (TypedValue.COMPLEX_UNIT_SP, 12);		
			TextView vCountdown = (TextView) findViewById (R.id.skip_ad_button_countdown);
			vCountdown.setTextSize (TypedValue.COMPLEX_UNIT_SP, 12);				
			TextView vText2 = (TextView) findViewById (R.id.skip_ad_button_phase_2_text);
			vText2.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);	
			}
		
		TextView vCountdown = (TextView) findViewById (R.id.skip_ad_button_countdown);
		vCountdown.setText ("6");
		}	
	
	@Override
	protected void onDestroy()
		{
		log ("onDestroy");
		super.onDestroy();
		if (update_timer != null)
			try { update_timer.cancel(); } catch (Exception ex) {};
		release_player();
		cleanup();
		}
		
	@Override
	protected void onPause()
		{
		log ("onPause");
		super.onPause();
		release_player();
		cleanup();
		}	
	
	@Override
	protected void onStop()
		{
		log ("onStop");
		super.onStop();	
		}

	@Override
	public void onConfigurationChanged (Configuration newConfig)
		{
		super.onConfigurationChanged (newConfig);
		boolean landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
		adjust_layout (landscape);
		}
	
	public void adjust_layout (boolean landscape)
		{		
		log ("adjust layout: " + (landscape ? "landscape" : "portrait"));
		
		int true_width = 0;
		int true_height = 0;
		if (landscape)
			{
			true_width = screen_width > screen_height ? screen_width : screen_height;
			true_height = screen_width > screen_height ? screen_height : screen_width;
			}
		else
			{
			true_width = screen_width > screen_height ? screen_height : screen_width;
			true_height = screen_width > screen_height ? screen_width : screen_height;
			}
			
		int height = (int) ((float) screen_width / 1.77);

		mPreview = (SurfaceView) findViewById (R.id.video_ad_surface);
		// mPreview.setVisibility (View.VISIBLE);
		holder = mPreview.getHolder();
		holder.addCallback (this);
		holder.setFormat (PixelFormat.RGBA_8888);
		// holder.setFixedSize (screen_width, height);
		// holder.setFixedSize (true_width, true_height);
		
		View vContainer = findViewById (R.id.video_ad_container);
		FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vContainer.getLayoutParams();
		layout.height = height;
		layout.width = screen_width;
		vContainer.setLayoutParams (layout);
		
		View vSkip = findViewById (R.id.skip_ad_button);
		FrameLayout.LayoutParams skip_layout = (FrameLayout.LayoutParams) vSkip.getLayoutParams();
		if (landscape)
			skip_layout.topMargin = height - pixels_60 - pixels_50;
		else
			skip_layout.topMargin = height + pixels_60 + pixels_10;

		vSkip.setLayoutParams (skip_layout);
		}
	
	@Override
	public void onRelayActivityReady()
		{
		log ("READY");
		
		View vContainer = findViewById (R.id.video_ad_container);
		vContainer.setVisibility (View.VISIBLE);
		
		if (advert_url != null && mSurfaceCreated)
			play_advert (advert_url);
		else
			defer_url = advert_url;
		}
	
	public void play_advert (String url)
		{
		log ("play advert: " + url);
		
		mMediaPlayer = new MediaPlayer (this);
		try 
			{
			mMediaPlayer.setDataSource (url);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			return;
			}
		
		mMediaPlayer.setDisplay (holder);
		
		try 
			{
			mMediaPlayer.prepare();
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			return;
			}
		
		mMediaPlayer.setOnBufferingUpdateListener (this);
		mMediaPlayer.setOnCompletionListener (this);
		mMediaPlayer.setOnPreparedListener (this);
		mMediaPlayer.setOnVideoSizeChangedListener (this);
		
		setVolumeControlStream (AudioManager.STREAM_MUSIC);
		}

	public void cleanup()
		{
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
		defer_url = null;
		}
	
	public void release_player()
		{
		if (mMediaPlayer != null)
			{
			mMediaPlayer.release();
			mMediaPlayer = null;
			}
		}
	
	public void start_video()
		{
		log ("start video");
		// holder.setFixedSize (mVideoWidth, mVideoHeight);
		
		int height = (int) ((float) screen_width / 1.77);
		holder.setFixedSize (screen_width, height);
		
		// View vContainer = findViewById (R.id.video_ad_container);
		// vContainer.setVisibility (View.VISIBLE);
		
		mMediaPlayer.start();
		
		if (!countdown_started)
			{
			countdown_started = true;
			post_countdown();
			update_timer = new Timer();
			update_timer.scheduleAtFixedRate (new UpdateTask(), 500, 500);
			}
		
		duration = mMediaPlayer.getDuration();
		}
	
	public void post_countdown()
		{
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				milliseconds_remaining -= 500;
				log ("milliseconds remaining: " + milliseconds_remaining);
				
				TextView vCountdown = (TextView) findViewById (R.id.skip_ad_button_countdown);
				vCountdown.setText (Integer.toString (milliseconds_remaining / 1000));
				
				if (milliseconds_remaining > 500)
					post_countdown();
				else
					set_skip_phase (2);
				}			
			}, 1000);
		}
	
	@Override
	public void surfaceChanged (SurfaceHolder holder, int format, int width, int height)
		{
		log ("surfaceChanged");
		}

	@Override
	public void surfaceCreated (SurfaceHolder holder)
		{
		log ("surfaceCreated");
		mSurfaceCreated = true;
		if (defer_url != null)
			{
			log ("playing deferred url: " + defer_url);
			play_advert (defer_url);
			}
		}

	@Override
	public void surfaceDestroyed (SurfaceHolder holder)
		{
		log ("surfaceDestroyed");		
		}

	@Override
	public void onVideoSizeChanged (MediaPlayer mp, int width, int height)
		{
		log ("onVideoSizeChanged: width=" + width + ", height=" + height);
		if (width > 0 && height > 0)
			{
			mIsVideoSizeKnown = true;
			mVideoWidth = width;
			mVideoHeight = height;
			if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown)
				start_video();
			}
		}

	@Override
	public void onPrepared (MediaPlayer mp)
		{
		log ("onPrepared");
		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown)
			start_video();
		}

	@Override
	public void onCompletion (MediaPlayer mp)
		{
		log ("onCompletion");
		update_timer.cancel();
		track_event (advert_id, "completed", advert_name, 0);
		send_impression_track();
		finish();
		}

	@Override
	public void onBufferingUpdate (MediaPlayer mp, int percent)
		{
		log ("onBufferingUpdate: " + percent);
		}
	
	int current_phase = -1;
	
	public void set_skip_phase (int phase)
		{
		current_phase = phase;
		
		View vOne = findViewById (R.id.skip_ad_button_phase_1);
		vOne.setVisibility (phase == 1 ? View.VISIBLE : View.INVISIBLE);
		
		View vTwo = findViewById (R.id.skip_ad_button_phase_2);
		vTwo.setVisibility (phase == 2 ? View.VISIBLE : View.INVISIBLE);
		
		if (phase == 2)
			{
			vTwo.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	send_impression_track();
		        	finish();
		        	}
				});
			}
		}
	
	public void send_impression_track()
		{		
		if (current_phase == 2)
			{
			track_event (advert_id, "impression", advert_name, duration);
			}
		}
	
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
	
	public void track_event (String category, String action, String label, long value)
		{
		track_event (category, action, label, value, "");
		}
	
	public void track_event (String category, String action, String label, long value, String extra)
		{
		String smashed_event = category + "." + action + "." + label + value + "." + extra;
		
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
	
	boolean q0 = false, q1 = false, q2 = false, q3 = false;
	
	class UpdateTask extends TimerTask
		{  
		public void run()
	   		{
			if (mMediaPlayer == null)
				{
				cancel();
				return;
				}
			long new_duration = 0;
			try { new_duration = mMediaPlayer.getDuration(); } catch (Exception ex) {};
			if (new_duration > 0)
				duration = new_duration;
			long offset = 0;
			try { offset = mMediaPlayer.getCurrentPosition(); } catch (Exception ex) {};
			
			float percent = (float) offset / (float) duration;
			
			log ("tick offset=" + offset + " duration=" + duration + " percent=" + percent);
			if (!q0 && percent >= 0.0)
				{
				q0 = true;
				// track_event (advert_id, "started", advert_name, 0);
				}			
			if (!q1 && percent >= 0.25)
				{
				q1 = true;
				track_event (advert_id, "firstQuartile", advert_name, 0);
				}
			if (!q2 && percent >= 0.50)
				{
				q2 = true;
				track_event (advert_id, "midPoint", advert_name, 0);
				}
			if (!q3 && percent >= 0.75)
				{
				q3 = true;
				track_event (advert_id, "thirdQuartile", advert_name, 0);
				cancel();
				}
	   		}
		}
	}