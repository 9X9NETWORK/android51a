package tv.tv9x9.player;

import android.os.Handler;
import android.view.View;

public interface OnPlayerListener
	{
	public Handler main_thread_handler();
	public boolean is_chromecasted();
	public boolean able_to_play_video();
	public boolean screen_is_on();
	public void next_episode();
	public void next_episode_with_rules();
	public void setup_progress_bar();
	public void reset_progress_bar();
	public void redraw_control_bar_in_thread();
	public void invalidate_progress_bar();
	public void submit_track_eof();
	public void set_video_alpha (int alpha);
	public void set_video_visibility (int visibility);
	public void relay_post (String text);
	public int readout_volume();
	public void alert (String text);
	public void set_poi_trigger (boolean use_current_offset);
	public void start_playing();
	public void restart_playing (long start_msec);
	public void ready();
	public void accumulate_episode_time (long t);
	public View findViewById (int id);
	public String active_player();
	public void post_to_main_thread (Runnable r);
	
	public void onVideoActivityVideoStarted();
	public void onVideoActivityLayout();
	public void onVideoActivityPauseOrPlay (boolean paused);
	
	public boolean any_remembered_locations();
	}   