package tv.tv9x9.player;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;

public final class VideoFragment extends YouTubePlayerSupportFragment implements OnInitializedListener
	{
	boolean paused = false;

	private YouTubePlayer player = null;
	private String videoId;

	final String devkey = "AI39si5HrNx2gxiCnGFlICK4Bz0YPYzGDBdJHfZQnf-fClL2i7H_A6Fxz6arDBriAMmnUayBoxs963QLxfo-5dLCO9PCX-DTrA";
	
	private VideoBaseActivity ctx = null;
	
	private int most_recent_offset = 0;
	
	/* send a play command as soon as possible. This is because it's now necessary to re-initialize the YouTube api when
	   onPaused is called. Before Google made that change, it just worked and this nastiness was not required */
	private Handler handler = null;
	private Runnable startup_function = null;
	
	public void log (String text)
		{
		Log.i ("vtest", "[videoFragment] " + text);
		}
	
	public static VideoFragment newInstance()
		{
		return new VideoFragment();
		}

	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		log ("onCreate");			
		initialize (devkey, this);
		reset_time_played();
		}

	@Override
	public void onDestroy()
		{
		if (player != null)
			{
			try { player.release(); } catch (Exception ex) { ex.printStackTrace(); };
			log ("YouTube released");
			}
		log ("onDestroy");    		
		super.onDestroy();
		}

	@Override
	public void onStart()
		{
		log ("onStart"); 
		super.onStart();
		}
	
	@Override
	public void onResume()
		{
		log ("onResume");
		initialize (devkey, this);
		super.onResume();
		}

	@Override
	public void onPause()
		{
		log ("onPause");
		if (player != null)
			{
			try { player.release(); player = null; } catch (Exception ex) { ex.printStackTrace(); };
			log ("YouTube released");
			}
		super.onPause();
		}

	@Override
	public void onAttach (Activity activity)
		{
		log ("onAttach"); 
		super.onAttach (activity);
		}
	
	@Override
	public void onDetach()
		{
		log ("onDetach"); 
		super.onDetach();
		}
	
	public void set_context (VideoBaseActivity ctx)
		{
		this.ctx = ctx;
		}
	
	public boolean ready()
		{
		return player != null;
		}
	
	public boolean is_paused()
		{
		return paused;
		}
	
	public void setVideoId (String videoId)
		{
		log ("setVideoId: " + videoId);
		if (videoId != null && !videoId.equals(this.videoId))
			{
			this.videoId = videoId;
			if (player != null)
				{
				try
					{
					player.cueVideo (videoId);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			}
		}

	public String current_video_id()
		{
		return videoId;
		}
	
	public void pause()
		{
		if (player != null)
			{
			try
				{
				player.pause();
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			}
		}

	public void play()
		{
		if (!ctx.chromecasted)
			{
			if (player != null)
				{
				try
					{										
					player.play();
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			}
		}
	
	public void load_video (String id, int start_msec)
		{
		if (!ctx.chromecasted)
			{
			videoId = id;
			if (player != null)
				{
				log ("load video " + id + ", start: " + start_msec);
				try { player.loadVideo (id, start_msec); } catch (Exception ex) { ex.printStackTrace(); }
				}
			}
		}

	public void load_video (String id)
		{
		if (!ctx.chromecasted)
			{
			videoId = id;
			if (player != null)
				{
				log ("load video " + id + ", in its entirety");
				try { player.loadVideo (id); } catch (Exception ex) { ex.printStackTrace(); }
				}
			}
		}

	public void set_manage_audio_focus (boolean focus)
		{
		if (player != null)
			try { player.setManageAudioFocus (false); } catch (Exception ex) { ex.printStackTrace(); }
		}

	public int get_offset()
		{
		int offset = 0;
		if (player != null)
			try { offset = most_recent_offset = player.getCurrentTimeMillis(); } catch (Exception ex) {};			
		return offset;
		}

	public int get_duration()
		{
		int duration = 0;
		if (player != null)
			try { duration = player.getDurationMillis(); } catch (Exception ex) {};
		return duration;
		}
	
	public void seek (int offset)
		{
		if (!ctx.chromecasted)
			if (player != null)
				try { player.seekToMillis (offset); } catch (Exception ex) {};
		}
	
	public boolean is_playing()
		{
		boolean is_playing = true;
		if (player != null)
			try { is_playing = player.isPlaying(); } catch (Exception ex) {};
		return is_playing;
		}
	
	public void set_full_screen (boolean flag)
		{
		if (player != null)
			try { player.setFullscreen (flag); } catch (Exception ex) {};
		}
	
	public void set_listeners()
		{
		set_playback_event_listener();
		set_state_change_listener();
		}
			
	long video_time_counter = 0L;
	
	public void add_to_time_played()
		{
		long now = System.currentTimeMillis();
		ctx.cumulative_episode_time += (now - video_time_counter);
		video_time_counter = now;	
		}
	
	public void reset_time_played()
		{
		video_time_counter = System.currentTimeMillis();
		}
	
	public int get_most_recent_offset()
		{
		return most_recent_offset;
		}	
	
	public void set_startup_function (Handler h, Runnable r)
		{
		handler = h;
		startup_function = r;
		}
	
	public void set_playback_event_listener()
		{
		try
			{
			if (player == null)
				{
				log ("set_playback_event_listener: player is null!");
				return;
				}
			player.setPlaybackEventListener (new YouTubePlayer.PlaybackEventListener()
				{				
				@Override
				public void onBuffering (boolean isBuffering)
					{
					log ("video event: onBuffering=" + isBuffering);
					ctx.set_video_visibility (View.VISIBLE);
					ctx.set_video_alpha (255);
					ctx.mService.relay_post ("REPORT BUFFERING");
					}
		
				@Override
				public void onPaused()
					{
					if (player != null)
						try { most_recent_offset = player.getCurrentTimeMillis(); } catch (Exception ex) {};
						
					log ("video event: onPaused");
					paused = true;
					
					ctx.in_main_thread.post (new Runnable()
						{
						public void run()
							{
							ctx.onVideoActivityPauseOrPlay (true);
							ctx.in_main_thread.post (ctx.redraw_control_bar);
							}
						});
					// in_main_thread.post (go_halfscreen);
					ctx.mService.relay_post ("REPORT PAUSED");
					ctx.submit_track_eof();
					
					add_to_time_played();					
					}
		
				@Override
				public void onPlaying()
					{
					log ("video event: onPlaying");
					paused = false;
					
					ctx.set_video_visibility (View.VISIBLE);
					ctx.set_video_alpha (255);
					// ImageView knob = (ImageView) findViewById (R.id.knob);
					ImageView knob = (ImageView) ctx.findViewById (R.id.knob);
					if (knob != null)
						knob.setVisibility (View.VISIBLE);
					if (false && ctx.video_has_started)
						{
						/* presume this is resuming from a pause */
			        	long offset = player.getCurrentTimeMillis();
			        	if (offset > 0)
			        		ctx.in_main_thread.post (ctx.go_fullscreen);
						}
					ctx.video_has_started = true;
					ctx.video_play_pending = false;
					ctx.pending_restart = false;
					ctx.mService.relay_post ("REPORT PLAYING");
					ctx.readout_volume();
					ctx.in_main_thread.post (new Runnable()
						{
						public void run()
							{
							ctx.onVideoActivityPauseOrPlay (false);
							}
						});
					// ctx.submit_track_unpause();
					
					reset_time_played();
					}
		
				@Override
				public void onSeekTo (int newPositionMillis)
					{
					log ("video event: onSeekTo: " + newPositionMillis);
					ctx.invalidate_progress_bar();
					}
		
				@Override
				public void onStopped()
					{
					log ("video event: onStopped");
					
					most_recent_offset = 0;
					add_to_time_played();	
					
					log ("pending_restart is: " + ctx.pending_restart);
					ctx.reset_progress_bar();
					// video_cutoff_time = -1;
					// video_next_trigger = video_release_trigger = -1;
					ctx.set_video_alpha (0);
					ctx.submit_track_eof();
					if (ctx.chromecasted)
						{
						log ("onStopped: chromecast is active, won't move to next episode");
						return;
						}
					else if (ctx.video_systemic_error)
						{
						/* don't want to start videos -- problem with a layout or view */
						ctx.video_systemic_error = false;
						log ("onStopped: systemic error, won't move to next episode");
						return;
						}
					else if (ctx.exit_in_progress)
						{
						log ("onStopped: exit in progress, won't move to next episode");
						return;
						}
					else if (ctx.restore_video_location)
						{
						log ("onstopped: restore_video_location is set, won't move to next episode");
						return;
						}
					// in_main_thread.post (go_fullscreen);
					// in_main_thread.post (remove_dialog_tuner);
					// set_screen_from_thread (video_play_pending ? screentype.BEACHBALL : screentype.SPLASH);
					else if (ctx.playing_begin_titlecard)
						{
						log ("onStopped: playing begin titlecard, won't move to next episode");
						return;
						}
					else if (ctx.playing_end_titlecard)
						{
						log ("onStopped: playing end titlecard, won't move to next episode");
						return;
						}
					else if (!ctx.able_to_play_video())
						{	
						log ("onStopped: incorrect mode to play another video");
						return;
						}						
					else if (ctx.pending_restart)
						{
						log ("onStopped: pending_restart, re-kicking the video");
						ctx.pending_restart = false;
						ctx.start_playing();
						return;
						}					
					else if (ctx.video_has_started)
						{
						if (ctx.screen_is_on())
							ctx.next_episode_with_rules();
						else
							log ("onStopped: screen is off, won't move to next episode");
						}
					else
						{
						log ("onStopped: video was not started, won't move to next episode");
						return;
						}
					}
				});
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
	public void set_state_change_listener()
		{	
		try
			{
			if (player == null)
				{
				log ("set_state_change_listener: player is null!");
				return;
				}
			player.setPlayerStateChangeListener (new YouTubePlayer.PlayerStateChangeListener()
				{
				@Override
				public void onAdStarted()
					{
					log ("[video state] onAdStarted");
					}
		
				@Override
				public void onError (ErrorReason reason)
					{
					log ("[video state] onError: " + reason.toString());
	
					most_recent_offset = 0;
					
					/* maybe this error is in the middle of a video */
					add_to_time_played();
					
					/* not displaying errors is idiotic -- requested by PMs */
					if (1 == 2)
						{
						if (reason.toString().equals ("USER_DECLINED_RESTRICTED_CONTENT"))
							ctx.alert ("Restricted content -- probably age restriction");
						else if (reason.toString().equals ("INTERNAL_ERROR"))
							ctx.alert ("Internal error -- probably this video is private");
						else if (reason.toString().equals ("UNAUTHORIZED_OVERLAY"))
							ctx.video_systemic_error = true;
						else if (reason.toString().equals ("PLAYER_VIEW_TOO_SMALL"))
							ctx.video_systemic_error = true;		
						else if (reason.toString().equals ("UNKNOWN"))
							/* API bug? -- do nothing */;
						else
							ctx.alert (reason.toString());
						}
					else
						{
						if (reason.toString().equals ("UNAUTHORIZED_OVERLAY"))
							ctx.video_systemic_error = true;
						else if (reason.toString().equals ("PLAYER_VIEW_TOO_SMALL"))
							ctx.video_systemic_error = true;
						}
					
					/* this behavior requested by PMs is idiotic */
					if (!ctx.exit_in_progress && !ctx.playing_begin_titlecard 
							&& !ctx.playing_end_titlecard && ctx.screen_is_on() && ctx.able_to_play_video())
						{
						/* add extra time to minimize catastrophe caused by requested, idiotic behavior */
						ctx.in_main_thread.postDelayed (new Runnable()
							{	
							@Override
							public void run()
								{
								if (ctx.screen_is_on() && ctx.able_to_play_video())
									{
									log ("video onError; playing next episode");
									ctx.next_episode();
									}
								}
							}, 200);
						}
					}
		
				@Override
				public void onLoaded (String arg0)
					{		
					log ("[video state] onLoaded");
					}
		
				@Override
				public void onLoading()
					{
					log ("[video state] onLoading");
					ctx.set_video_visibility (View.VISIBLE);
					}
		
				@Override
				public void onVideoEnded()
					{
					log ("[video state] onVideoEnded");
					most_recent_offset = 0;
					add_to_time_played();
					}
		
				@Override
				public void onVideoStarted()
					{
					log ("[video state] onVideoStarted");
					most_recent_offset = 0;
					ctx.set_poi_trigger (false);
					ctx.set_video_visibility (View.VISIBLE);
					ctx.onVideoActivityVideoStarted (player);
					ctx.setup_progress_bar();
					reset_time_played();
					}
				});
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}

	int video_height = 0, video_width = 0;
	
	@Override
	public void onInitializationSuccess (Provider provider, YouTubePlayer player, boolean was_restored)
		{
		this.player = player;
		
		log ("YouTube initialized successfully");
		
		SpecialFrameLayout yt_wrapper = (SpecialFrameLayout) ctx.findViewById (R.id.ytwrapper2);
		yt_wrapper.setOnVideoResize (new Callback()
			{
			@Override
			public void run_two_integers (int newX, int newY)
				{
				/* This is no longer necessary to the player. But keeping it in, since knowing newX and newY helps when diagnosing problems */
				log ("AHA! newX=" + newX + ", newY=" + newY);
				if (newX > 100 && newY > 100)
					{
					log ("recording new video size: width=" + newX + " height=" + newY);
					video_width = newX;
					video_height = newY;
					}
				}
			});
				
		try
			{
			player.addFullscreenControlFlag (YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		/* these three lines force the navigation bar (present on tablets and at least one dongle), to remain locked on */
		// int flags = player.getFullscreenControlFlags();
		// flags &= ~player.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI;
		// player.setFullscreenControlFlags (flags);
		
		try
			{
			player.setPlayerStyle (YouTubePlayer.PlayerStyle.CHROMELESS);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		try
			{
			player.setOnFullscreenListener ((VideoBaseActivity) getActivity());
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		if (!was_restored && videoId != null)
			{
			try
				{
				log ("cue video: " + videoId);
				player.cueVideo (videoId);
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			}
			
		if (!was_restored)
			ctx.ready();		
		
		if (startup_function != null)
			{
			log ("executing startup function");
			handler.post (startup_function);
			startup_function = null;
			}
		}

	@Override
	public void onInitializationFailure (Provider provider, YouTubeInitializationResult result)
		{
		player = null;
		}
	
	@Override
	public void onConfigurationChanged (Configuration newConfig)
		{
		super.onConfigurationChanged (newConfig);
		log ("CONFIGURATION CHANGED IN VIDEO FRAGMENT!");
		ctx.onVideoActivityLayout();
		}
	}