package tv.tv9x9.player;

import android.os.Handler;

public interface Player
	{
    public void play();
    public void seek (long offset);
    public void pause();
    public boolean is_paused();
	public boolean is_playing();
    public long get_offset();
	public long get_duration();
    public long get_most_recent_offset();
    public void stop();
    public boolean is_ready();
    public void reset_time_played();
    public void add_to_time_played();
    public void set_manage_audio_focus (boolean focus);
    public void set_full_screen (boolean flag);
	public void set_listeners();
	public void set_startup_function (Handler h, Runnable r);
	}