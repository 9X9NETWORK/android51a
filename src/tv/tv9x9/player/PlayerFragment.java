package tv.tv9x9.player;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;

public class PlayerFragment extends Fragment implements Player
	{
    private OnPlayerListener mCallback = null; 

    private String pending_url = null;
	private SurfaceView mPreview = null;
	private SurfaceHolder holder = null;
	private MediaPlayer mMediaPlayer = null;
	
	private int mVideoWidth;
	private int mVideoHeight;
	private boolean mIsVideoSizeKnown = false;
	private boolean mIsVideoReadyToBePlayed = false;
	
	private boolean paused = false;
	private long most_recent_offset = 0;
	
	private long video_time_counter = 0;
	
	/* for compatibility with VideoFragment. See the longer note there */
	private Handler handler = null;
	private Runnable startup_function = null;
	
	public void log (String text)
		{
		Log.i ("vtest", "[PlayerFragment] " + text);
		}
	
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
    	
    	/*
    	 *  This must be done in the start activity, since it will restart the app on failure!
    	 *  
		if (!LibsChecker.checkVitamioLibs (getActivity()))
			{
			log ("Vitamio library check failed!");
			return null;
			}
		*
		*/
    	
		View v = null;
		
		try 
			{
	        v = inflater.inflate (R.layout.player_fragment, container, false);
			}
		catch (Exception ex)
			{
			log ("GAH!");
			ex.printStackTrace();
			}
		
		log ("onCreateView 2");

		mPreview = (SurfaceView) v.findViewById (R.id.surface);
		setup_holder();
		
        return v;
    	}
    
    /*
    @Override
    public void onCreate(Bundle b)
    	{
    	log ("onCreate");
    	}
    */
    
    @Override
    public void onAttach (Activity activity)
    	{
    	log ("onAttach");
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnPlayerListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnPlayerListener");
        	}
        
        log ("onAttach 2");
    	}
    
	@Override
	public void onPause()
		{
		log ("onPause");
		super.onPause();
		release_media_player();
		do_cleanup();
		}
	
	@Override
	public void onResume()
		{
		log ("onResume");
		super.onResume();
		}
	
    @Override
    public void onDestroy()
    	{
    	log ("onDestroy");
    	super.onDestroy();
		release_media_player();
		do_cleanup();
    	}
    
    private void setup_holder()
    	{
    	log ("setup holder");
    	
    	holder = mPreview.getHolder();
    	
    	holder.addCallback (new SurfaceHolder.Callback()
    		{
			@Override
			public void surfaceChanged (SurfaceHolder holder, int format, int width, int height)
				{
				log ("surfaceChanged");
				}

			@Override
			public void surfaceCreated (SurfaceHolder holder)
				{
				log ("surfaceCreated");
				if (pending_url != null)
					{
					play_video (pending_url);
					pending_url = null;
					}
				}

			@Override
			public void surfaceDestroyed (SurfaceHolder holder)
				{
				log ("surfaceDestroyed");
				}
			});
    	
		holder.setFormat (PixelFormat.RGBA_8888);
    	}
    
    @Override
    public boolean is_ready()
    	{
    	return mMediaPlayer != null && mIsVideoReadyToBePlayed;
    	}
    
    @Override
    public void stop()
    	{
    	if (mMediaPlayer != null)
    		{
    		log ("stop");
    		try { mMediaPlayer.stop(); } catch (Exception ex) {};
        	release_media_player();
    		}
    	do_cleanup();
    	}

    @Override
    public void pause()
    	{
    	log ("[player pause]");
    	if (mMediaPlayer != null)
    		{
    		log ("pause");
    		try { mMediaPlayer.pause(); } catch (Exception ex) {};
    		paused = true;
    		}
    	}

    @Override
    public void play()
		{
		if (mMediaPlayer != null)
			{
    		log ("play");
			try { mMediaPlayer.start(); } catch (Exception ex) {};
			paused = false;
			}
		}
    
    @Override
    public boolean is_paused()
    	{
    	return paused;
    	}
    
	@Override
	public boolean is_playing()
		{
		boolean is_playing = true;
		if (mMediaPlayer != null)
			try { is_playing = mMediaPlayer.isPlaying(); } catch (Exception ex) {};
		return is_playing && !paused;
		}
	
    @Override
	public void seek (long offset)
		{
		if (!mCallback.is_chromecasted())
			if (mMediaPlayer != null)
				{
				log ("seek: " + offset);
				try { mMediaPlayer.seekTo (offset); } catch (Exception ex) {};
				}
		}
	
    @Override
	public long get_offset()
		{
		long offset = 0;
		if (mMediaPlayer != null)
			try { offset = most_recent_offset = mMediaPlayer.getCurrentPosition(); } catch (Exception ex) {};			
		return offset;
		}
	
    @Override
	public long get_duration()
		{
		long duration = 0;
		if (mMediaPlayer != null)
			try { duration = mMediaPlayer.getDuration(); } catch (Exception ex) {};
		return duration;
		}
    
	@Override
	public void set_startup_function (Handler h, Runnable r)
		{
		handler = h;
		startup_function = r;
		}
	
	@Override
	public void reset_time_played()
		{
		video_time_counter = System.currentTimeMillis();
		}
	
	@Override
	public void add_to_time_played()
		{
		long now = System.currentTimeMillis();
		mCallback.accumulate_episode_time (now - video_time_counter);
		video_time_counter = now;
		}
	
	@Override
	public long get_most_recent_offset()
		{
		return most_recent_offset;
		}	
	
	@Override
	public void set_manage_audio_focus (boolean focus)
		{
		/* not used by Vitamio */
		}
	
	@Override
	public void set_full_screen (boolean flag)
		{
		/* not used by Vitamio */
		}
	
	@Override
	public void set_listeners()
		{
		/* not used by Vitamio */
		}
	
    private void do_cleanup()
    	{
   		mVideoWidth = 0;
   		mVideoHeight = 0;
   		mIsVideoReadyToBePlayed = false;
   		mIsVideoSizeKnown = false;
    	}
    
    private void release_media_player()
    	{
		if (mMediaPlayer != null)
			{
			log ("release");
			mMediaPlayer.release();
			mMediaPlayer = null;
			}
    	}
    
	public void play_video (String url)
		{
		do_cleanup();
		
		log ("play video: " + url);
		
		mMediaPlayer = new MediaPlayer (getActivity());
		
		try
			{
			mMediaPlayer.setDataSource (url);
			mMediaPlayer.setDisplay (holder);
			mMediaPlayer.prepareAsync();
			mMediaPlayer.setOnBufferingUpdateListener (new MediaPlayer.OnBufferingUpdateListener()
				{
				@Override
				public void onBufferingUpdate (MediaPlayer mp, int percent)
					{
					log ("onBufferingUpdate");
					}
				});
			mMediaPlayer.setOnCompletionListener (new MediaPlayer.OnCompletionListener()
				{
				@Override
				public void onCompletion (MediaPlayer mp)
					{
					log ("onCompletion");
					}
				});
			mMediaPlayer.setOnPreparedListener (new MediaPlayer.OnPreparedListener()
				{
				@Override
				public void onPrepared (MediaPlayer mp)
					{
					log ("onPrepared");
					mIsVideoReadyToBePlayed = true;
					if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown)
						{
						startVideoPlayback();
						}
					}
				});
			mMediaPlayer.setOnVideoSizeChangedListener (new MediaPlayer.OnVideoSizeChangedListener()
				{
				@Override
				public void onVideoSizeChanged (MediaPlayer mp, int width, int height)
					{
					log ("onVideoSizeChanged");
					if (width == 0 || height == 0)
						{
						log ("invalid video width(" + width + ") or height(" + height + ")");
						return;
						}
					mIsVideoSizeKnown = true;
					mVideoWidth = width;
					mVideoHeight = height;
					if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown)
						{
						startVideoPlayback();
						}
					}
				});
		
			mMediaPlayer.setOnInfoListener (new OnInfoListener()
				{
                @Override
                public boolean onInfo (MediaPlayer mp, int what, int extra)
                	{
                	View vProgress = getView().findViewById (R.id.vitamio_spinner);
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                    	{
                        vProgress.setVisibility (View.VISIBLE);
                    	}
                    else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
                    	{
                        vProgress.setVisibility (View.GONE);
                    	}
                    return false;
                	}
				});
			
			// setVolumeControlStream (AudioManager.STREAM_MUSIC);
			} 
		catch (Exception ex)
			{
			log ("video error!");
			ex.printStackTrace();
			}
		}
	
	private void startVideoPlayback()
		{
		log ("startVideoPlayback");
		holder.setFixedSize (mVideoWidth, mVideoHeight);
		mMediaPlayer.start();
		}
	}