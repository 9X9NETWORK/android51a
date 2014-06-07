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

public final class VideoFragment extends YouTubePlayerSupportFragment implements OnInitializedListener, Player
	{
	boolean paused = false;

	private YouTubePlayer player = null;
	private String videoId = null;

	final String devkey = "AI39si5HrNx2gxiCnGFlICK4Bz0YPYzGDBdJHfZQnf-fClL2i7H_A6Fxz6arDBriAMmnUayBoxs963QLxfo-5dLCO9PCX-DTrA";
	
	private VideoBaseActivity ctx = null;
	private OnPlayerListener mCallback = null;
	
	private int most_recent_offset = 0;
	
	/* loadVideo will sometimes ignore a start offset! if so, issue a seekTo() */
	private long start_time_workaround = 0;
	
	/* send a play command as soon as possible. This is because it's now necessary to re-initialize the YouTube api when
	   onPaused is called. Before Google made that change, it just worked and this nastiness was not required */
	private Handler handler = null;
	private Runnable startup_function = null;
	
	private boolean restart_in_progress = false;
	int number_of_inits = 0;
	
	private void log (String text)
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
		restart_in_progress = true;
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
		reset_time_played();
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
		this.mCallback = (OnPlayerListener) ctx;
		}
	
	@Override
	public boolean is_ready()
		{
		return player != null;
		}
	
	@Override
	public boolean is_paused()
		{
		return paused;
		}
	
	public void setVideoId (String videoId)
		{
		log ("setVideoId: " + videoId);
		if (videoId != null && !videoId.equals (this.videoId))
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
	
	@Override
	public void pause()
		{
		log ("[video pause]");
		if (player != null)
			{
			log ("pause");
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

	@Override
	public void play()
		{
		if (!mCallback.is_chromecasted())
			{
			if (player != null)
				{
				try
					{		
					log ("play");
					player.play();
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			}
		}
	
	public void load_video (String id, long start_msec)
		{
		if (restart_in_progress)
			{
			log ("restart is in progress, won't load video!");
			}
		else if (!mCallback.is_chromecasted())
			{
			videoId = id;
			if (player != null)
				{
				log ("load video " + id + ", start: " + start_msec);
				start_time_workaround = start_msec;
				try
					{
					player.loadVideo (id, (int) start_msec);
					}
				catch (IllegalStateException ex)
					{
					restart();
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			}
		}

	public void load_video (String id)
		{
		if (restart_in_progress)
			{
			log ("restart is in progress, won't load video!");
			}
		else if (!mCallback.is_chromecasted())
			{
			videoId = id;
			if (player != null)
				{
				log ("load video " + id + ", in its entirety");
				start_time_workaround = 0;
				try
					{
					player.loadVideo (id);
					}
				catch (IllegalStateException ex)
					{
					restart();
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			}
		}

	@Override
	public void set_manage_audio_focus (boolean focus)
		{
		if (player != null)
			{
			try
				{
				player.setManageAudioFocus (false);
				}
			catch (IllegalStateException ex)
				{
				restart();
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			}
		}

	public void restart()
		{
		/* this is the first call to the YouTube player, so handle this re-init here */
		if (!restart_in_progress)
			{
			if (number_of_inits++ < 16)
				{
				/* will mess up our context */
				log ("Youtube is probably released, re-initializing");
				restart_in_progress = true;
				
				initialize (devkey, this);
				}
			else
				log ("Youtube init is " + number_of_inits + ", probably in some awful feedback loop, won't try to init");
			}
		else
			log ("YouTube state error! but a re-initialization is in progress");
		}
	
	@Override
	public long get_offset()
		{
		int offset = 0;
		if (player != null)
			try { offset = most_recent_offset = player.getCurrentTimeMillis(); } catch (Exception ex) {};			
		return offset;
		}

	@Override
	public long get_duration()
		{
		int duration = 0;
		if (player != null)
			try { duration = player.getDurationMillis(); } catch (Exception ex) {};
		return duration;
		}
	
	@Override
	public void seek (long offset)
		{
		if (!mCallback.is_chromecasted())
			if (player != null)
				try { player.seekToMillis ((int) offset); } catch (Exception ex) {};
		}
	
	@Override
	public void stop()
		{
		/* astonishingly, this is lacking in the YouTube API */
		}
	
	@Override
	public boolean is_playing()
		{
		boolean is_playing = true;
		if (player != null)
			try { is_playing = player.isPlaying(); } catch (Exception ex) {};
		return is_playing;
		}
	
	@Override
	public void set_full_screen (boolean flag)
		{
		if (player != null)
			try { player.setFullscreen (flag); } catch (Exception ex) {};
		}
	
	@Override
	public void set_listeners()
		{
		set_playback_event_listener();
		set_state_change_listener();
		}
			
	long video_time_counter = 0L;
	
	@Override
	public void add_to_time_played()
		{
		long now = System.currentTimeMillis();
		mCallback.accumulate_episode_time (now - video_time_counter);
		video_time_counter = now;	
		}
	
	@Override
	public void reset_time_played()
		{
		video_time_counter = System.currentTimeMillis();
		}
	
	@Override
	public long get_most_recent_offset()
		{
		return most_recent_offset;
		}	
	
	@Override
	public void set_startup_function (Handler h, Runnable r)
		{
		handler = h;
		startup_function = r;
		}
	
	public void set_playback_event_listener()
		{
		if (player == null)
			{
			log ("set_playback_event_listener: player is null!");
			return;
			}
		
		try
			{
			player.setPlaybackEventListener (new YouTubePlayer.PlaybackEventListener()
				{				
				@Override
				public void onBuffering (boolean isBuffering)
					{
					log ("video event: onBuffering=" + isBuffering);
					mCallback.set_video_visibility (View.VISIBLE);
					mCallback.set_video_alpha (255);
					mCallback.relay_post ("REPORT BUFFERING");
					}
		
				@Override
				public void onPaused()
					{
					if (player != null)
						try { most_recent_offset = player.getCurrentTimeMillis(); } catch (Exception ex) {};
						
					log ("video event: onPaused");
					paused = true;
					
					mCallback.main_thread_handler().post (new Runnable()
						{
						public void run()
							{
							mCallback.onVideoActivityPauseOrPlay (true);
							mCallback.redraw_control_bar_in_thread();
							}
						});
					// in_main_thread.post (go_halfscreen);
					mCallback.relay_post ("REPORT PAUSED");
					mCallback.submit_track_eof();
					
					add_to_time_played();					
					}
		
				@Override
				public void onPlaying()
					{
					log ("video event: onPlaying");
					paused = false;
					
					mCallback.set_video_visibility (View.VISIBLE);
					mCallback.set_video_alpha (255);
					ImageView knob = (ImageView) mCallback.findViewById (R.id.knob);
					if (knob != null)
						knob.setVisibility (View.VISIBLE);
					if (false && ctx.video_has_started)
						{
						/* presume this is resuming from a pause */
			        	long offset = player.getCurrentTimeMillis();
			        	if (offset > 0)
			        		mCallback.main_thread_handler().post (ctx.go_fullscreen);
						}
					ctx.video_has_started = true;
					ctx.video_play_pending = false;
					ctx.pending_restart = false;
					mCallback.relay_post ("REPORT PLAYING");
					mCallback.readout_volume();
					mCallback.main_thread_handler().post (new Runnable()
						{
						public void run()
							{
							mCallback.onVideoActivityPauseOrPlay (false);
							}
						});
					
					reset_time_played();
					}
		
				@Override
				public void onSeekTo (int newPositionMillis)
					{
					log ("video event: onSeekTo: " + newPositionMillis);
					mCallback.invalidate_progress_bar();
					}
		
				@Override
				public void onStopped()
					{
					log ("video event: onStopped");
					
					most_recent_offset = 0;
					add_to_time_played();	
					
					log ("pending_restart is: " + ctx.pending_restart);
					mCallback.reset_progress_bar();
					// video_cutoff_time = -1;
					// video_next_trigger = video_release_trigger = -1;
					mCallback.set_video_alpha (0);
					mCallback.submit_track_eof();
					if (mCallback.is_chromecasted())
						{
						log ("onStopped: chromecast is active, won't move to next episode");
						return;
						}
					else if (!mCallback.active_player().equals ("video"))
						{
						log ("onStopped: other player is active, won't move to next episode");
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
					else if (!mCallback.able_to_play_video())
						{	
						log ("onStopped: incorrect mode to play another video");
						return;
						}						
					else if (ctx.pending_restart)
						{
						log ("onStopped: pending_restart, re-kicking the video");
						ctx.pending_restart = false;
						mCallback.start_playing();
						return;
						}					
					else if (ctx.video_has_started)
						{
						if (mCallback.screen_is_on())
							mCallback.next_episode_with_rules();
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
		catch (IllegalStateException ex)
			{
			restart();
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
	public void set_state_change_listener()
		{	
		if (player == null)
			{
			log ("set_state_change_listener: player is null!");
			return;
			}
		try
			{
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
					String rString = reason.toString();
					log ("[video state] onError: " + rString);
	
					most_recent_offset = 0;
					
					/* maybe this error is in the middle of a video */
					add_to_time_played();
					
					/* not displaying errors is idiotic -- requested by PMs */
					if (1 == 2)
						{
						if (rString.equals ("USER_DECLINED_RESTRICTED_CONTENT"))
							mCallback.alert ("Restricted content -- probably age restriction");
						else if (rString.equals ("INTERNAL_ERROR"))
							mCallback.alert ("Internal error -- probably this video is private");
						else if (rString.equals ("UNAUTHORIZED_OVERLAY"))
							ctx.video_systemic_error = true;
						else if (rString.equals ("PLAYER_VIEW_TOO_SMALL"))
							ctx.video_systemic_error = true;		
						else if (rString.equals ("UNKNOWN"))
							/* API bug? -- do nothing */;
						else
							mCallback.alert (rString);
						}
					else
						{
						if (rString.equals ("UNEXPECTED_SERVICE_DISCONNECTION"))
							{
							ctx.video_systemic_error = true;
							mCallback.alert ("Problems with YouTube!");
                            restart();
                            return;
							}
						else if (rString.equals ("UNAUTHORIZED_OVERLAY"))
							ctx.video_systemic_error = true;
						else if (rString.equals ("PLAYER_VIEW_TOO_SMALL"))
							ctx.video_systemic_error = true;
						}
					
					/* this behavior requested by PMs is idiotic */
					if (!ctx.exit_in_progress && !ctx.playing_begin_titlecard 
							&& !ctx.playing_end_titlecard && mCallback.screen_is_on() && mCallback.able_to_play_video())
						{
						/* add extra time to minimize catastrophe caused by requested, idiotic behavior */
						mCallback.main_thread_handler().postDelayed (new Runnable()
							{	
							@Override
							public void run()
								{									
								if (mCallback.screen_is_on() && mCallback.able_to_play_video() && mCallback.active_player().equals ("video"))
									{
									log ("video onError; playing next episode");
									mCallback.next_episode();
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
					mCallback.set_video_visibility (View.VISIBLE);
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
					mCallback.set_poi_trigger (false);
					mCallback.set_video_visibility (View.VISIBLE);
					mCallback.onVideoActivityVideoStarted();
					mCallback.setup_progress_bar();
					reset_time_played();
					
					/* during restart, start_msec will be ignored by loadVideo, though in simple test programs this is not the case.
					   perform a seek if the video started close to zero and not where we thought. Also, restart videos entirely if the
					   start_msec was near the beginning anyway */

					long offset = get_offset();
					boolean perform_extra_seek = start_time_workaround > 6000 && offset < 4000;
					start_time_workaround = 0;
					if (perform_extra_seek)
						{
						log ("extra start time seek (workaround for YouTube bug): " + start_time_workaround + " (we are at offset: " + offset + ")");
						seek (start_time_workaround);
						}
					}
				});
			}
		catch (IllegalStateException ex)
			{
			restart();
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
		restart_in_progress = false;
		this.player = player;

		log ("YouTube initialized successfully");
		
		SpecialFrameLayout yt_wrapper = (SpecialFrameLayout) mCallback.findViewById (R.id.ytwrapper2);
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
			mCallback.ready();		
		
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
		mCallback.onVideoActivityLayout();
		}
	}