package tv.tv9x9.player;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

public class metadata
	{	
	public String status = "";
	
	public String white_label = "default";
	public String mso = null;
	public String app_name = null;
	public String mso_title = null;
	public String mso_preferred_lang_code = null;
	
	public String api_server = "api.flipr.tv";
	// public String api_server = "beagle.9x9.tv";
	public String relay_server = "relay-dev.9x9.tv";
	public int relay_port = 909;
	public String google_analytics = null;
	public String facebook_app_id = null;
	public String flurry_id = null;
	public String gcm_sender_id = null;
	
	/* chromecast: 5ecf7ff9-2144-46ce-acc9-6d606831e2dc_1 */
	public String chromecast_app_name = null;
	
	public boolean shake_and_discover_feature = false;
	
	public String region = "en";
	public String supported_region = null;
	
	public String about_us_url = null;
	
	/* dongle mode only */
	public String guidemode = "3x3";
	
	/* transition between personalities -- tv, tablet, or portal */
	public String future_action = null;
	
	public String renderer = null;
	public String rendering_token = null;
	public String rendering_username = null;
	
	public boolean television_turning_off = false;

	public boolean grid_update_required = false;
	
	public String force_mode = null;
	
	public String usertoken = null;
	public String username = null;
	public String email = null;
	
	public boolean sign_status_changed = false;	
	public boolean fresh_signup = false, fresh_signin = false, fresh_signout = false, fresh_fb_signout = false;
	
	public String youtube_auth_token = null;
	public String youtube_account_name = null;
	public boolean youtube_9x9_mirror_created_this_session = false;
	
	public String default_stack = null;
	
	public String controlling = null;
	
	public boolean notifications_enabled = false;
	public boolean notify_with_sound = false;
	public boolean notify_with_vibrate = false;	

	public boolean notify_with_sound_default = false;
	public boolean notify_with_vibrate_default = false;
	
	public Runnable interrupt_with_notification = null;
	
	/* always, once, never */
	public String signup_nag = "never";
	
	/* the portal API may be called from start activity, and consumed by the main activity */
	public String portal_api_cache[] = null;
	
	/* title captions in the 9x9 grid, one above each 3x3 set */
	String[] set_titles = null;
	
	Hashtable  <Integer, Hashtable <String, String>>   channelgrid;
	Hashtable  <String,  Hashtable <String, String>>   channel_pool;
	Hashtable  <String,  Hashtable <String, String>>   programgrid;
	Hashtable  <String,  Hashtable <String, String>>   channels_by_youtube;
	Hashtable  <String,  Hashtable <String, String>>   titlecards;
	Hashtable  <String,  Hashtable <String, String>>   special_tags;
	
	Hashtable <String, Hashtable <String, String>> sets;
	
	/* episode id -> Integer.toString() -> Comment */
	Hashtable  <String,  Hashtable <String, Comment>>  comments;
	
	/* kept here because activities come and go */
	Hashtable < String, String[] > query_cache = new Hashtable < String, String[] > ();
		
	String last_visited_channel = null;
	String last_visited_episode = null;
	String last_visited_stack = null;
	int last_visited_stack_icon = 0;
	
	/* last_visited_seek might be updated in the past, so we need to remember which episode it's attached to */
	String last_visited_episode_id_for_seek = null;
	long last_visited_seek = 0;
	
	String grid_virtual_channel = null;
	
	/* for portal -- this is temporary */
	public String piles[] = null;
	public String pile_names[] = null;
	public String pile_episode_thumbs[] = null;
	public String pile_channel_thumbs[] = null;
	
	/* if the player changes subscription lineup (used by tv mode) */
	boolean subscriptions_altered = false;
	
	/* if the player unsubscribed, extra procecssing is needed */
	boolean pending_unsubscribe = false;
	
	Lock program_lock = new ReentrantLock();
	Lock channel_lock = new ReentrantLock();
	
	public static class Comment
		{
		String title;
		String text;
		String author;
		String date;
		String userid;
		String thumbnail;
		}
	
	public metadata()
		{
		Log.i ("vtest", "metadata");
		init_pools();
		}

	public void init_pools()
		{
		Log.i ("vtest", "[metadata] init pools");
		
		channel_pool = new Hashtable <String,  Hashtable <String, String>> ();
		init_subscription_pool();
		
		programgrid  = new Hashtable <String,  Hashtable <String, String>> ();
		channels_by_youtube = new Hashtable <String,  Hashtable <String, String>> ();	
		titlecards = new Hashtable <String,  Hashtable <String, String>> ();
		sets = new Hashtable <String, Hashtable <String, String>> ();
		comments = new Hashtable <String,  Hashtable <String, Comment>> ();
		special_tags = new Hashtable <String,  Hashtable <String, String>> ();
		
		init_query_cache();
		
		set_titles = new String [10];
		for (int i = 0; i < set_titles.length; i++)
			set_titles [i] = "Untitled";
		}
	
	public void init_subscription_pool()
		{
		channelgrid  = new Hashtable <Integer, Hashtable <String, String>> ();
		}
	
	public void init_query_cache()
		{
		query_cache = new Hashtable < String, String[] > ();
		}
	
	public String get_set_title (int index)
		{
		/* set index runs 1 .. 9 */
		return (index >= 1 && index <= 9) ? set_titles [index] : null;		
		}

	public void set_set_title (int index, String title)
		{
		/* set index runs 1 .. 9 */
		if  (index >= 1 && index <= 9)
			{
			set_titles [index] = title;
			Log.i ("vtest", "set title " + index + " now: " + title);
			}
		}
	
	public int first_channel ()
		{
		for (int i = 1; i <= 9; i++)
			for (int j = 1; j <= 9; j++)
				{
				if (occupied (i * 10 + j))
					return i * 10 + j;
				}
		return -1;
		}

	public int first_position_of (String channel_id)
		{
		for (int i = 1; i <= 9; i++)
			for (int j = 1; j <= 9; j++)
				{
				if (occupied (i * 10 + j))
					{
					if (channel_meta (i * 10 + j, "id").equals (channel_id))
						return i * 10 + j;
					}
				}
		return -1;
		}

	public int first_empty_position()
		{
		return first_empty_position_setwise();		
		}
	
	public int first_empty_position_linear()
		{
		for (int i = 1; i <= 9; i++)
			for (int j = 1; j <= 9; j++)
				{
				if (!occupied (i * 10 + j))
					{
					Log.i ("vtest", "first empty position: " + (i * 10 + j));
					return i * 10 + j;
					}
				}
		return -1;
		}
	
	public int first_empty_position_setwise()
		{
		int top_lefts[] = { 11, 14, 17, 41, 44, 47, 71, 74, 77 };
		
		for (int set = 0; set < 9; set++)
			{
			for (int dy = 0; dy <= 2; dy++)
				for (int dx = 0; dx <= 2; dx++)
					{
					int possible = top_lefts [set] + 10 * dy + dx;
					if (!occupied (possible))
						return possible;
					}
			}
		
		return -1;
		}
	
	/* position -1 to remove from grid */
	
	public void place_in_channel_grid (String channel_id, int position, boolean add_flag)
		{
		Hashtable <String, String> meta = null;
		try
			{
			channel_lock.lock();
			meta = channel_pool.get (channel_id);
			}
		finally
			{
			channel_lock.unlock();
			}
		
		/* adding to grid */
		if (add_flag && meta != null && !occupied (position))
			{
			try
				{
				channel_lock.lock();
				channelgrid.put (position, meta);
				Log.i ("vtest", "added channel " + channel_id + " to grid at position " + position);
				}
			finally
				{
				channel_lock.unlock();
				}
			}
	
		/* remove from grid. this could use a bit more safety checking */
		else if (!add_flag && meta != null && occupied (position))			
			{
			try
				{
				channel_lock.lock();
				channelgrid.remove (position);
				Log.i ("vtest", "removed channel " + channel_id + " from grid at position " + position);
				}
			finally
				{
				channel_lock.unlock();
				}
			}
		}
	
	public void move_channel (int from, int to)
		{
		Hashtable <String, String> meta = null;
		try
			{
			channel_lock.lock();
			if (!occupied (to))
				{
				meta = channelgrid.get (from);
				channelgrid.remove (from);
				channelgrid.put (to, meta);
				}
			}
		finally
			{
			channel_lock.unlock();
			}
		}
	
	public void copy_channel (String from, String to)
		{
		Log.i ("vtest", "copy channel: " + from + " -> " + to);
		Hashtable <String, String> meta = null;
		
		try
			{
			channel_lock.lock();
			meta = channel_pool.get (from);
			meta.put ("id", to);
			channel_pool.put (to, meta);
			}
		finally
			{
			channel_lock.unlock();
			}	
		
		/* now, update programs. Can't make a copy though, since the episode id remains unchanged */
		
		try
			{
			program_lock.lock();
			for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
				{
				String this_channel = program_meta_nolock (entry.getKey(), "channel");
				if (this_channel != null && this_channel.equals (from))
					{
					meta = entry.getValue();
					meta.put ("channel", to);
					}
				}
			}
		finally
			{
			program_lock.unlock();
			}
		}
	
	Hashtable <String, String> deep_copy_of_meta (Hashtable <String, String> original)
		{
		Hashtable <String, String> copy = new Hashtable <String, String> ();
		for (Entry <String, String> entry: original.entrySet())
			{
			copy.put (entry.getKey(), entry.getValue());
			}
		return copy;
		}
	
	public String[] subscribed_channels()
		{
		int count = 0;
		String channel_line[] = null;

		try
			{
			channel_lock.lock();
			
			for (int i = 1; i <= 9; i++)
				for (int j = 1; j <= 9; j++)
					{
					int position = i * 10 + j;
					if (occupied (position))
						count++;
					}
	
			if (count == 0)
				return null;
			
			channel_line = new String [1 + count];
			
			int rcount = 0;
			
			for (int i = 1; i <= 9; i++)
				for (int j = 1; j <= 9; j++)
					{
					int position = i * 10 + j;
					if (occupied (position))
						{
						String channel_id = channel_meta (position, "id");
						channel_line [++rcount] = channel_id;
						}
					}
			}
		finally
			{
			channel_lock.unlock();
			}
		
		return channel_line;
		}
	
	public void dump_subscriptions()
		{
		for (int i = 1; i <= 9; i++)
			for (int j = 1; j <= 9; j++)
				{
				int position = i * 10 + j;
				if (occupied (position))
					{
					String channel_id = channel_meta (position, "id");
					Log.i ("vtest", "+++ SUBSCRIPTION: grid=" + position +  ", server=" + client_to_server (position) + ", channel=" + channel_id);
					}
				}
		}
	
	/* add locking if going to re-use */
	public String OBSOLETE_first_program_in (int channel)
		{
		String real_channel = channel_meta (channel, "id");

		if (real_channel != null)
			for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
				{
				// entry.getKey() and entry.getValue()
				String this_channel = program_meta_nolock (entry.getKey (), "channel");
				if (this_channel != null && real_channel.equals (this_channel))
					return entry.getKey ();
				}

		return null;
		}

	public Comparator <String> sort_by_date = new Comparator <String>()
		{
		@Override
		public int compare (String a, String b)
			{
			Long a_date = 0L;
			Long b_date = 0L;
			
			try
				{
				String a_string = program_meta (a, "timestamp");
				String b_string = program_meta (b, "timestamp");
				a_date = a_string == null ? 0 : Long.parseLong (a_string);
				b_date = b_string == null ? 0 : Long.parseLong (b_string);
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "DATE COMPARE FAILED: a=" + program_meta (a, "timestamp") + ", b=" + program_meta (b, "timestamp"));
				ex.printStackTrace();
				}
			if (a_date > b_date)
				return -1;
			else if (a_date < b_date)
				return 1;
			else
				return 0;
			}
		};
		
	public Comparator <String> sort_by_position = new Comparator <String>()
		{
		@Override
		public int compare (String a, String b)
			{
			int a_pos = 0;
			int b_pos = 0;
			
			try
				{
				String a_string = program_meta (a, "sort");
				String b_string = program_meta (b, "sort");
				a_pos = a_string == null ? 0 : Integer.parseInt (a_string);
				b_pos = b_string == null ? 0 : Integer.parseInt (b_string);
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "DATE COMPARE FAILED: a=" + program_meta (a, "timestamp") + ", b=" + program_meta (b, "timestamp"));
				ex.printStackTrace();
				}
			if (a_pos > b_pos)
				return 1;
			else if (a_pos < b_pos)
				return -1;
			else
				return 0;
			}
		};
				
	public String[] program_line (int channel)
		{
		int count = programs_in_channel (channel);

		if (count > 0)
			{
			String results[] = new String [count];
			
			String real_channel = channel_meta (channel, "id");
			
			int n = 0;

			if (real_channel != null)
				{
				try
					{
					program_lock.lock();
					for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
						{
						// entry.getKey() and entry.getValue()
						String this_channel = program_meta_nolock (entry.getKey(), "channel");
						if (this_channel != null && real_channel.equals (this_channel))
							results[n++] = entry.getKey ();
						}
					}
				finally
					{
					program_lock.unlock();
					}

				if (is_youtube (real_channel))
					Arrays.sort (results, sort_by_date);
				else
					Arrays.sort (results, sort_by_position);			

				return results;
				}
			}

		return null;
		}

	public String[] program_line_by_id (String real_channel)
		{
		if (real_channel != null && programgrid != null)
			{
			int count = programs_in_real_channel (real_channel);
	
			if (count > 0)
				{
				String results[] = new String [count];
	
				int n = 0;
	
				try
					{
					program_lock.lock();
					for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
						{
						// entry.getKey() and entry.getValue()
						String this_channel = program_meta_nolock (entry.getKey(), "channel");
						if (this_channel != null && real_channel.equals (this_channel) && n < count)
							results[n++] = entry.getKey();
						}
					}
				finally
					{
					program_lock.unlock();
					}

				String nature = pool_meta (real_channel, "nature");
							
				if (nature != null && (nature.equals ("3") || nature.equals ("5")))
					{
					/* type 4, YouTube playlist, formerly used date, now uses position */
					Arrays.sort (results, sort_by_date);
					}
				else
					Arrays.sort (results, sort_by_position);		

				if (results.length >= 1)
					{
					String first_episode_id = results [0];
					String first_episode_thumb = program_meta (first_episode_id, "thumb");
					if (first_episode_thumb != null && !first_episode_thumb.equals (""))
						set_channel_meta_by_id (real_channel, "episode_thumb", first_episode_thumb);
					}
				return results;
				}
			}
		
		return null;
		}

	public String generate_ponderosa (String username)
		{
		long crawldate = System.currentTimeMillis() / 1000L;
		
		Stack <String> episodes = new Stack <String> ();
		
		String channel_id = youtube_username_to_channel_id (username);
		if (channel_id != null)
			{
			try
				{
				program_lock.lock();
				for (Entry <String, Hashtable <String, String>> entry: programgrid.entrySet())
					{
					Hashtable <String, String> meta = null;

					meta = programgrid.get (entry.getKey());
					if (meta != null)
						{
						String this_channel = meta.get ("real_channel");
						// Log.i ("vtest", "[ponderosa " + username + "=" + channel_id + "] " + entry.getKey() + ", channel: " + this_channel);
						if (this_channel != null && channel_id.equals (this_channel))
							{
							String url = meta.get ("url1");
							if (url == null)
								continue;
							String video_id = null;							
							int voffset = url.indexOf ("?v=");
							if (voffset > 0)
								{
								video_id = url.substring (voffset + 3, voffset + 3 + 11);
								}
							String name = util.encodeURIComponent (meta.get ("name"));
							String timestamp = meta.get ("timestamp");
							int duration = util.string_to_seconds (meta.get ("duration"));
							String thumb = meta.get ("thumb");
							String desc = util.encodeURIComponent (meta.get ("desc"));
							
							String line = channel_id + "\t" + username + "\t" + crawldate + "\t" + video_id + "\t" + name + "\t" +
							                 timestamp + "\t" + duration + "\t" + thumb + "\t" + desc;
							episodes.push (line);
							}
						}
					}
				}
			finally
				{
				program_lock.unlock();
				}
			}
		
		String result = "";
		while (!episodes.empty())
			{
			if (result.equals (""))
				result += episodes.pop();
			else
				result += "\n" + episodes.pop();
			}
		
		return result;
		}
	// print >>out, id + '\t' + ch ['youtube'] + '\t' + crawldate + '\t' + e_video_id + '\t' + e_name + '\t' + e_time 
	// + '\t' + e_duration + '\t' + e_thumb + '\t' + e_desc
	// 4027	instyleuk	1358530737	gnY-y1QQMT0	Eva Longoria-Parkers's InStyle Shoot	1216205564	92
	// http://i.ytimg.com/vi/gnY-y1QQMT0/mqdefault.jpg	long-encoded-description
	
	public void forget_programs_in_channel (String real_channel)
		{
		if (real_channel != null)
			{
			Stack <String> to_be_deleted = new Stack <String> ();
			try
				{
				program_lock.lock();
				for (Entry <String, Hashtable <String, String>> entry: programgrid.entrySet())
					{
					String this_channel = program_meta_nolock (entry.getKey(), "channel");
					if (this_channel != null && real_channel.equals (this_channel))
						{
						to_be_deleted.push (entry.getKey());
						}
					}
				while (!to_be_deleted.empty())
					programgrid.remove (to_be_deleted.pop());
				}
			finally
				{
				program_lock.unlock();
				}
			}
		}
	
	public void forget_subscriptions()
		{	
		try
			{
			channel_lock.lock();
			for (Entry <String, Hashtable <String, String>> entry : channel_pool.entrySet ())
				{
				Hashtable <String, String> v = entry.getValue();
				String subscribed = v.get ("subscribed");
				if (subscribed != null && subscribed.equals ("1"))
					v.put (subscribed, "0");
				}			
			}
		finally
			{
			channel_lock.unlock();	
			}
		
		init_subscription_pool();
		}
	
	public int programs_in_channel (int channel)
		{
		int count = 0;
		String real_channel = channel_meta (channel, "id");

		if (real_channel != null)
			{
			try
				{
				program_lock.lock();
				for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
					{
					// entry.getKey() and entry.getValue()
					String this_channel = program_meta_nolock (entry.getKey (), "channel");
					if (this_channel != null && real_channel.equals (this_channel))
						count++;
					}
				}
			finally
				{
				program_lock.unlock();
				}
			}

		return count;
		}
	
	public int programs_in_real_channel (String real_channel)
		{
		int count = 0;
		
		if (real_channel != null)
			{
			try
				{
				program_lock.lock();
				for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
					{
					// entry.getKey() and entry.getValue()
					String this_channel = program_meta_nolock (entry.getKey (), "channel");
					if (this_channel != null && real_channel.equals (this_channel))
						count++;
					}
				}
			finally
				{
				program_lock.unlock();	
				}
			}
	
		return count;
		}

	public int display_channel_count (String channel_id)
		{
		int true_count = programs_in_real_channel (channel_id);
		int server_count = 0;
		
		String server_count_string = pool_meta (channel_id, "server-count");
		if (server_count_string != null && !server_count_string.equals (""))
			server_count = Integer.parseInt (server_count_string);
		
		int count = server_count > true_count ? server_count : true_count;
		if (count > 200)
			count = 200;
		
		return count;
		}
	
	public int highest_sort (String real_channel)
		{
		int highest = 0;
		
		if (real_channel != null)
			{
			try
				{
				program_lock.lock();
				for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
					{
					// entry.getKey() and entry.getValue()
					String sort_string = program_meta_nolock (entry.getKey (), "sort");
					int sort_int = Integer.parseInt (sort_string);
					if (sort_int > highest)
						highest = sort_int;
					}
				}
			finally
				{
				program_lock.unlock();	
				}
			}
	
		return highest;
		}
	
	public Stack <String> channels_with_status (String status)
		{
		Stack <String> matches = new Stack <String> ();
		
		try
			{
			channel_lock.lock();
			for (Entry <String, Hashtable <String, String>> entry : channel_pool.entrySet ())
				{
				String id = entry.getKey();
				Hashtable <String, String> channel = entry.getValue();
				
				String this_status = channel.get ("status");
				
				if (this_status.equals (status))
					matches.push (id);
				}
			}
		finally
			{
			channel_lock.unlock();
			}
		
		return matches;
		}
	
	/* set this field for every channel */
	
	public void set_all_channel_meta (String field, String value)
		{
		try
			{
			channel_lock.lock();
			for (Entry <String, Hashtable <String, String>> entry : channel_pool.entrySet ())
				{
				Hashtable <String, String> channel = entry.getValue();				
				channel.put (field, value);
				}
			}
		finally
			{
			channel_lock.unlock();
			}
		}
	
	public long get_most_appropriate_timestamp (String channel_id)
		{
		String episode_id = null;
		boolean use_channel_meta = true;
		
		if (is_youtube (channel_id))
			{
			String program_line[] = program_line_by_id (channel_id);
			if (program_line != null && program_line.length > 0)
				{
				episode_id = program_line [0];
				use_channel_meta = false;
				}
			}
		
		String ts;
		if (use_channel_meta)
			ts = pool_meta (channel_id, "timestamp");
		else
			ts = program_meta (episode_id, "timestamp");
		
		return Long.parseLong (ts) / 1000;
		}
	
	public boolean is_youtube (String channel)
		{
		if (channel != null)
			{
			String nature = pool_meta (channel, "nature");
			if (nature != null)
				return nature.equals ("3") || nature.equals ("4");
			}
		return false;
		}
	
	public boolean is_subscribed (String channel)
		{
		if (channel.startsWith ("="))
			{
			String username = channel.replaceAll ("^=", "");
			String new_channel = youtube_username_to_channel_id (username);
			if (new_channel != null)
				channel = new_channel;
			}
		
		if (usertoken == null)
			{
			return false;
			}
		else if (youtube_auth_token != null)
			{
			String sub = pool_meta (channel, "subscribed");
			return sub != null && sub.equals ("1");
			}
		else
			return first_position_of (channel) > 0;
		}
	
	public String best_url (String program)
		{
		String  u1 = program_meta (program, "url1");
		if (u1 != null && !u1.equals(""))
			return u1;
		String  u2 = program_meta (program, "url2");
		if (u2 != null && !u2.equals(""))
			return u2;
		String  u3 = program_meta (program, "url3");
		if (u3 != null && !u3.equals(""))
			return u1;
		return null;
		}

	public String parse_setinfo (String virtual_channel_id, String[] lines)
		{
		int section = 0;
		String set_owner_name = null;
		
		String shortname = virtual_channel_id;
		if (shortname.contains (":"))
			{
			String fields[] = virtual_channel_id.split (":");
			shortname = fields [fields.length - 1];
			}

		int scount = 0;
		int pcount = 0;
		
		for (String s: lines)
			{
			if (s.equals ("--"))
				section++;
			else if (section == 1)
				{
				String fields[] = s.split ("\t");
				if (fields.length >= 2)
					{
					if (fields[0].equals ("name"))
						set_owner_name = fields[1];
					}
				}
			else if (section == 2)
				{
				add_channel_to_set (virtual_channel_id, s, ++scount);
				}
			else if (section == 3)
				{
				/* comment this out because Taipei is complaining that throwing these extra episodes into the pool is confusing them */
				// parse_program_info_32_line (virtual_channel_id, pcount++, s);
				}
			}
		
		return set_owner_name;
		}
	
	public void parse_virtual (String virtual_channel_id, String[] lines)
		{
		int count = 0;
		int section = 0;
		
		boolean forgot_old_episodes = false;
		
		String shortname = virtual_channel_id;
		if (shortname.contains (":"))
			{
			String fields[] = virtual_channel_id.split (":");
			shortname = fields [fields.length - 1];
			}
		
		for (String s: lines)
			{
			if (s.equals ("--"))
				section++;
			else if (section == 0)
				parse_virtual_channel_line (++count, virtual_channel_id, s);
			else if (section == 1)
				{
				if (!forgot_old_episodes && !virtual_channel_id.equals ("virtual:following"))
					{
					/* do this only if there ARE episodes in this parse */
					forgot_old_episodes = true;
					forget_programs_in_channel (virtual_channel_id);
					}
				parse_virtual_program_line (++count, virtual_channel_id, shortname, s);
				}
			}
		}

	public void parse_virtual_program_line (int count, String virtual_channel_id, String shortname, String s)
		{
		String fields[] = s.split ("\t");
		
		if (fields.length == 7)
			{
			/* sanity checks */
			if (fields[1].length() != 11)
				{
				Log.i ("vtest", "ignoring virtualChannel line. This is not a video id: " + fields[1]);
				return;
				}
			if (!fields[5].startsWith ("http"))
				{
				Log.i ("vtest", "ignoring virtualChannel line. This is not a URL: " + fields[5]);
				return;
				}	

			// Log.i ("vtest", "id: " + video_id + " url: " + video_url + " title: " + title + " thumb: " + thumb);
			
			if (fields[5].contains ("|"))
				{
				Log.i ("vtest", "virtualChannel :: seems to have sub-episodes: " + s);
				return;
				}
			
			String episode_id = shortname + ":" + fields[1];
			String video_url = "http://www.youtube.com/watch?v=" + fields[1];
			
			Log.i ("vtest", "VIRTUAL " + virtual_channel_id + ", EPISODE ENTRY :: " + episode_id);
			
			Hashtable <String, String> program = new Hashtable <String, String> ();

			program.put ("sort", Integer.toString (++count));
			program.put ("id", episode_id);
			program.put ("channel", virtual_channel_id);
			program.put ("real_channel", fields[0]);
			program.put ("name", fields[2]);
			program.put ("duration", fields[4]);
			program.put ("desc", (fields.length > 6) ? util.decodeURIComponent (fields[6]) : "");
			program.put ("thumb", (fields.length > 5) ? fields[5] : "");
			program.put ("url1", video_url);
			program.put ("url2", "");
			program.put ("url3", "");
			program.put ("url4", "");
			program.put ("timestamp", fields[3]);

			add_programs_from_youtube (program);
			}
		else
			Log.i ("vtest", "virtualChannel :: not enough fields: " + s);
		}

	public String parse_virtual_channel_line (int count, String virtual_channel_id, String s)
		{
		String[] fields = s.split ("\t");

		/*
		0 - grid location (for subscriptions only, otherwise a 0)
		1 - channel id
		2 - channel name
		3 - channel desc
		4 - channel thumb (JUST the channel thumb, no 3.2 style episode thumbnails)
		5 - channel nature (3=YouTube, 4=YouTube Playlist, 5=Facebook, etc.)
		6 - channel extra (this field actually contains the YouTube user id) 
		      ps. does not output youtube user id for youtube playlist.
		7 - episode count
		8 - channel type
		9 - channel status
		*/
		
		String channel_id = fields [1];
		Log.i ("vtest", "VIRTUAL CHANNEL LINE :: channel=" + channel_id + " name=" + fields[2]);
		

		boolean channel_is_new = false;
		
		Hashtable <String, String> channel = channel_hash_by_id (fields[1]);
		if (channel == null)
			{
			channel = new Hashtable <String, String>();
			// Log.i ("vtest", "channel " + channel_id + " is new");
			channel_is_new = true;
			}
		else
			{
			// Log.i ("vtest", "channel " + channel_id + " name was: " + pool_meta (channel_id, "name"));
			}
		
		String name = fields [2];
		String desc = fields [3];
		String thumb = fields [4];
		
		if (name.equals ("") && !channel_is_new)
			name = pool_meta (channel_id, "name");
		if (name == null)
			name = fields [6];
		
		if (desc.equals ("") && !channel_is_new)
			desc = pool_meta (channel_id, "desc");
		if (desc == null)
			desc = "";
		
		if (thumb.equals ("") && !channel_is_new)
			thumb = pool_meta (channel_id, "thumb");
		if (thumb == null)
			thumb = "";

		// Log.i ("vtest", "name is NOW set to: " + name);
		
		channel.put ("id", channel_id);
		channel.put ("name", name);
		channel.put ("desc", desc);
		channel.put ("thumb", thumb);
		channel.put ("nature", fields[5]);
		channel.put ("extra", fields[6]);
		channel.put ("count", fields[7]);		
		channel.put ("type", fields[8]);
		channel.put ("status", fields[9]);
		channel.put ("timestamp", fields[10]);
		
		/* have not fetched from youtube */
		channel.put ("fetched", "0");
		/* thumbnail has not been downloaded */
		channel.put ("thdown", "0");

		if (virtual_channel_id != null)
			channel.put ("#" + virtual_channel_id, Integer.toString (count));

		if (virtual_channel_id != null & virtual_channel_id.equals ("virtual:following"))
			channel.put ("subscribed", "1");
		
		if (!fields[0].equals ("0"))
			{
			try
				{
				channel_lock.lock();
				Log.i ("vtest", "+++ POSITION: " + server_to_client (Integer.parseInt (fields[0])) + " IS " + channel_id);
				channelgrid.put (server_to_client (Integer.parseInt (fields[0])), channel);
				}
			finally
				{
				channel_lock.unlock();
				}
			}
		
		try
			{
			channel_lock.lock();
			channel_pool.put (fields[1], channel);
			if (fields [5].equals ("3") || fields [5].equals ("4"))
				{
				// Log.i ("vtest", "YouTube channel, extra=" + fields [6]);
				channels_by_youtube.put (fields [6].toLowerCase(), channel);
				}		
			}
		finally
			{
			channel_lock.unlock();
			}
		
		return channel_id;
		}
	
	public void parse_channel_info (String[] lines)
		{
		for (String s: lines)
			parse_channel_info_line (null, s, 0);
		}
	
	public void parse_channel_info_with_setinfo (String[] lines)
		{
		int section = 0;
		for (String s: lines)
			{
			if (s.equals ("--"))
				{
				section++;
				continue;
				}
			if (section == 0)
				{
				String fields[] = s.split ("\t");
				set_set_title (Integer.parseInt (fields[0]), fields[2]);
				}
			else if (section == 1)
				parse_channel_info_line (null, s, 0);
			}
		}
	
	public String parse_channel_info_line (String line)
		{
		return parse_channel_info_line (null, line, 0);
		}
	
	public String parse_channel_info_line (String virtual_channel, String line, int count)
		{
		String[] fields = line.split ("\t");

		if (fields.length < 16)
			{
			Log.i ("vtest", "Not enough fields! has " + fields.length + ", need at least 16");
			return null;
			}
		
		Hashtable <String, String> channel = new Hashtable <String, String>();
		
		String thumb = fields[4];
		String episode_thumb = null;
		
		if (thumb.contains ("|"))
			{
			String thumbs[] = thumb.split ("\\|");
			Log.i ("vtest", "thumb " + thumb + " -> " + thumbs[0]);
			thumb = thumbs[0];
			if (thumbs.length > 1)
				episode_thumb = thumbs[1];
			}
		
		final String channel_id = fields [1];
		
		if (pool_meta (channel_id, "id") != null)
			{
			Log.i ("vtest", "parse_channel_info_line: channel " + channel_id + " already known");
			if (!fields[0].equals ("0"))
				{
				Hashtable <String, String> meta = channel_hash_by_id (channel_id);
				try
					{
					channel_lock.lock();
					channelgrid.put (server_to_client (Integer.parseInt (fields[0])), meta);
					}
				finally
					{
					channel_lock.unlock();
					}
				}
			return channel_id;
			}
		
		Log.i ("vtest", "parse channel info line: channel " + channel_id + " count: " + fields[5]);
		
		channel.put ("id", channel_id);
		channel.put ("name", fields[2]);
		channel.put ("desc", fields[3]);
		channel.put ("thumb", thumb);
		channel.put ("count", fields[5]);
		channel.put ("server-count", fields[5]);
		channel.put ("type", fields[6]);
		channel.put ("status", fields[7]);
		channel.put ("nature", fields[8]);
		channel.put ("extra", fields[9]);
		channel.put ("timestamp", fields[10]);
		
		if (virtual_channel != null)
			{
			channel.put ("#" + virtual_channel, Integer.toString (count));
			Log.i ("vtest", "parse channel line: virtual #" + virtual_channel + " <= " + channel_id);
			}
		
		if (episode_thumb != null)
			{
			/* v3.2+ only */
			channel.put ("episode_thumb", episode_thumb);
			Log.i ("vtest", "episode thumb " + channel_id + ": " + episode_thumb);
			}
		
		/* have not fetched from youtube */
		channel.put ("fetched", "0");
		/* thumbnail has not been downloaded */
		channel.put ("thdown", "0");
		
		if (!fields[0].equals ("0"))
			{
			try
				{
				channel_lock.lock();
				channelgrid.put (server_to_client (Integer.parseInt (fields[0])), channel);
				}
			finally
				{
				channel_lock.unlock();
				}
			}
		else
			Log.i ("vtest", "offgrid channel: " + channel_id + "=" + fields[2]);
		
		try
			{
			channel_lock.lock();
			channel_pool.put (channel_id, channel);
			if (fields[8].equals ("3") || fields[8].equals ("4"))
				channels_by_youtube.put (fields[9], channel);
			}
		finally
			{
			channel_lock.unlock();
			}
		
		return channel_id;
		}

	public void add_channel (int position, String id, String name, String desc, 
			String thumb, String count, String type, String status, String nature, String extra)
		{
		Hashtable <String, String> channel = new Hashtable <String, String> ();

		Log.i ("vtest", "add channel: " + id);
		
		if (thumb.contains ("|"))
			{
			String thumbs[] = thumb.split ("\\|");
			// Log.i ("vtest", "thumb " + thumb + " -> " + thumbs[0]); // noisy
			thumb = thumbs[0];
			}
		
		channel.put ("id", id);
		channel.put ("name", name);
		channel.put ("desc", desc);
		channel.put ("thumb", thumb);
		channel.put ("count", count);
		channel.put ("type", type);
		channel.put ("status", status);
		channel.put ("nature", nature);
		channel.put ("extra", extra);
		channel.put ("timestamp", "0");
		
		/* have not fetched from youtube */
		channel.put ("fetched", "0");
		/* thumbnail has not been downloaded */
		channel.put ("thdown", "0");

		try
			{
			channel_lock.lock();
			if (position > 0)
				channelgrid.put (position, channel);
			channel_pool.put (id, channel);
			if (nature.equals ("3") || nature.equals ("4"))
				channels_by_youtube.put (extra, channel);
			}
		finally
			{
			channel_lock.unlock();
			}
		}

	public void add_temporary_channel (int position, String id)
		{
		Hashtable <String, String> channel = new Hashtable <String, String> ();

		Log.i ("vtest", "add temporary channel: " + id);
		
		channel.put ("id", id);
		channel.put ("name", "");
		channel.put ("desc", "");
		channel.put ("thumb", "");
		channel.put ("count", "");
		channel.put ("type", "temporary");
		channel.put ("status", "");
		channel.put ("nature", "");
		channel.put ("extra", "");
		channel.put ("timestamp", "0");		
		channel.put ("fetched", "0");
		/* lie about the thumbnail being downloaded */
		channel.put ("thdown", "1");

		try
			{
			channel_lock.lock();
			channelgrid.put (position, channel);
			}
		finally
			{
			channel_lock.unlock();
			}
		}
	
	public void add_runt_episode (int sort, String channel_id, String episode_id)
		{
		String url = "http://www.youtube.com/watch?v=" + episode_id;
		add_runt_episode (sort, channel_id, episode_id, url);		
		}
	
	public void add_runt_episode (int sort, String channel_id, String episode_id, String url)
		{
		Hashtable <String, String> program = new Hashtable <String, String> ();		
		
		program.put ("sort", Integer.toString (sort));
		program.put ("channel", channel_id);
		program.put ("name", episode_id);
		program.put ("desc", "");
		program.put ("thumb", "");
		program.put ("url1", url);
		program.put ("url2", "");
		program.put ("url3", "");
		program.put ("url4", "");
		program.put ("timestamp", "0");
		
		try
			{
			program_lock.lock();
			programgrid.put (episode_id, program);	
			}
		finally
			{
			program_lock.unlock();
			}
		}

	/*
	 * programgrid [fields [1]] = { 'channel': fields[0],
	 * 'url1': 'fp:' + fields[8], 'url2': 'fp:' + fields[9], 'url3': 'fp:' +
	 * fields[10], 'url4': 'fp:' + fields[11], 'name': fields[2], 'desc': fields
	 * [3], 'type': fields[4], 'thumb': fields[6], 'snapshot': fields[7],
	 * 'timestamp': fields[12], 'duration': fields[5] };
	 */

	public void parse_program_info (String[] lines)
		{
		int count = 0;
		
		for (String s: lines)
			{
			count++;
			
			String[] fields = s.split ("\t");
			
			/* filter out 9x9 audio programs */
			if (fields[4].equals ("3"))
				{
				Log.i ("vtest", "audio program ignored: " + fields[1]);
				continue;
				}
			
			Hashtable <String, String> program = new Hashtable <String, String> ();

			String thumb = fields[6];
			if (thumb.contains ("|"))
				{
				String thumbs[] = thumb.split ("\\|");				
				Log.i ("vtest", "thumb " + thumb + " -> " + thumbs[0]);
				thumb = thumbs[0];
				}
			
			program.put ("sort", Integer.toString (count));
			program.put ("channel", fields[0]);
			program.put ("name", fields[2]);
			program.put ("desc", fields[3]);
			program.put ("thumb", thumb);
			program.put ("url1", fields[8]);
			program.put ("url2", fields[9]);
			program.put ("url3", fields[10]);
			program.put ("url4", fields[11]);
			program.put ("timestamp", fields[12]);

			try
				{
				program_lock.lock();
				programgrid.put (fields[1], program);	
				}
			finally
				{
				program_lock.unlock();
				}
			}
		}

	/* 3.2 style programs with subepisodes */
	
	public void parse_program_info_32 (String[] lines)
		{
		int count = 0;
		for (String s: lines)
			parse_program_info_32_line (count++, s);
		}
	
	public void parse_program_info_32_line (int count, String line)
		{
		parse_program_info_32_line (null, count, line);
		}
	
	public void parse_program_info_32_line (String virtual_channel, int count, String line)
		{	
		// Log.i ("vtest", "PINFO32: " + line); // noisy
		
		String[] fields = line.split ("\t");
		
		/* bad way to skip the preamble information */
		if (fields.length < 5)
			return;
		
		/* filter out 9x9 audio programs */
		if (fields[4].equals ("3"))
			{
			Log.i ("vtest", "audio program ignored: " + fields[1]);
			return;
			}
		
		Hashtable <String, String> program = new Hashtable <String, String> ();

		String episode_id = fields [1];
		String name = fields [2];
		String duration = fields [5];
		String thumb = fields [6];
		String url = fields [8];
		
		String submeta = "";
		if (fields.length >= 15)
			submeta = util.decodeURIComponent (fields [14]);
		
		if (name.contains ("|"))
			{
			String names[] = name.split ("\\|");
			String durations[] = duration.split ("\\|");
			String urls[] = url.split ("\\|");
			String thumbs[] = thumb.split ("\\|");
			
			name = names.length > 0 ? names [0] : "";	
			url = "";
			thumb = (thumbs.length > 0) ? thumbs [0] : "";
			
			program.put ("total-subepisodes", Integer.toString (Math.min (names.length, urls.length) - 1));
			program.put ("total-duration", durations [0]);
			
			/* must parse the submeta first, because to calculate offsets we need durations from it */
			
			if (!submeta.equals (""))
				{
				String submetas[] = submeta.split ("\n--");
				for (String meta: submetas)
					{
					Hashtable <String, String> kvs = new Hashtable <String, String> ();
					
					for (String metaline: meta.split ("\n"))
						{
						if (metaline.contains (":"))
							{
							String ab[] = metaline.split (":\\s+");
							if (ab.length >= 2)
								kvs.put (ab[0], ab[1]);
							// Log.i ("vtest", "found key/value in meta: " + ab[0] + " => " + ab[1]); // noisy
							}
						}
					String kv_type = kvs.get ("type");
					String kv_sub = kvs.get ("subepisode");
					String kv_duration = kvs.get ("duration");				
					
					if (kv_sub != null && !kv_sub.equals (""))
						{
						if (kv_type.contains ("begin"))
							{
							String title_id = "begin-" + episode_id + "-" + kv_sub;
							/* consider mutex here */
							titlecards.put (title_id, kvs);
							program.put ("sub-" + kv_sub + "-begin-title", title_id);
							program.put ("sub-" + kv_sub + "-pre", kv_duration);
							// Log.i ("vtest", "added titlecard: " + title_id); // noisy
							}
						if (kv_type.contains ("end"))
							{
							String title_id = "end-" + episode_id + "-" + kv_sub;
							/* consider mutex here */
							titlecards.put (title_id, kvs);
							program.put ("sub-" + kv_sub + "-end-title", title_id);
							program.put ("sub-" + kv_sub + "-post", kv_duration);
							// Log.i ("vtest", "added titlecard: " + title_id); // noisy
							}
						}
					}
				}

			int present_offset = 0;
			
			for (int i = 1; i < names.length && i < urls.length; i++)
				{
				program.put ("sub-" + i + "-name", names.length > i ? names [i] : "");
				program.put ("sub-" + i + "-url", urls.length > i ? urls [i] : "");
				program.put ("sub-" + i + "-thumb", thumbs.length > i ? thumbs [i] : "");
				program.put ("sub-" + i + "-duration", durations.length > i ? durations [i] : "");
				program.put ("sub-" + i + "-offset", Integer.toString (present_offset));
				
				present_offset += Integer.parseInt (durations [i]);
				
				/* add in the length of title cards, if any are present */
				if (program.get ("sub-" + i + "-pre") != null)
					present_offset += Integer.parseInt (program.get ("sub-" + i + "-pre"));
				if (program.get ("sub-" + i + "-post") != null)
					present_offset += Integer.parseInt (program.get ("sub-" + i + "-post"));
				
				if (urls [i].contains (";"))
					{
					String semicolons[] = urls[i].split (";");
					program.put ("sub-" + i + "-start", semicolons.length > 1 ? semicolons [1] : "");
					program.put ("sub-" + i + "-end", semicolons.length > 2 ? semicolons [2] : "");
					}
				// Log.i ("vtest", episode_id + " SUB-EPISODE " + i + " ADDED (" + urls[i] + ")"); // noisy
				}
			}
		else
			program.put ("total-subepisodes", "0");
		
		String channel = fields[0];
		if (channel.contains (":"))
			{
			String ch_fields[] = channel.split (":");
			if (ch_fields.length > 1)
				{
				channel = ch_fields[0];
				program.put ("real_channel",  ch_fields[1]);
				}
			}
		
		program.put ("sort", Integer.toString (count));
		program.put ("channel", virtual_channel == null ? channel : virtual_channel);
		program.put ("name", name);
		program.put ("desc", fields[3]);
		program.put ("thumb", thumb);
		program.put ("url1", url);
		program.put ("url2", fields[9]);
		program.put ("url3", fields[10]);
		program.put ("url4", fields[11]);
		program.put ("timestamp", fields[12]);
		
		if (fields.length >= 16 && !fields[15].equals (""))
			{
			String pois[] = fields[15].split ("\\|");
			for (String poi: pois)
				{
				String pfields[] = poi.split (";");
				if (pfields.length == 5)
					{
					String poi_sub = pfields[0];
					int poi_nth;
					for (poi_nth = 1; /* nothing */; poi_nth++)
						{
						String dummy = program.get ("sub-" + poi_sub + "-poi-" + poi_nth + "-start");
						if (dummy == null)
							break;
						}
					String poi_json = util.decodeURIComponent (pfields[4]);
					Log.i ("vtest", "POI JSON: " + poi_json);
					program.put ("sub-" + poi_sub + "-poi-" + poi_nth + "-start", pfields[1]);
					program.put ("sub-" + poi_sub + "-poi-" + poi_nth + "-end", pfields[2]);		
					program.put ("sub-" + poi_sub + "-poi-" + poi_nth + "-type", pfields[3]);							
					program.put ("sub-" + poi_sub + "-poi-" + poi_nth + "-data", poi_json);
					Log.i ("vtest", "poi on this episode (sub episode " + poi_sub + ", #" + poi_nth + ") at: " + pfields[1] + ", ending: " + pfields[2]);
					}
				}
			}
		
		try
			{
			program_lock.lock();
			programgrid.put (episode_id, program);	
			}
		finally
			{
			program_lock.unlock();
			}
		}
	
	public void add_programs_from_youtube (Hashtable <String, String> program)
		{
		String id = program.get ("id");
		try
			{
			program_lock.lock();
			programgrid.put (id, program);
			}
		finally
			{
			program_lock.unlock();
			}
		}

	public boolean OLD_occupied (int channel)
		{
		return (channelgrid.containsKey (channel));
		}

	public boolean occupied (int channel)
		{
		Hashtable <String, String> meta = channelgrid.get (channel);
		return meta != null;
		}
		
	public Hashtable <String, String> channel_hash_by_id (String channel_id)
		{
		Hashtable <String, String> meta = null;
		try
			{
			channel_lock.lock();
			meta = channel_pool.get (channel_id);
			}
		finally
			{
			channel_lock.unlock();
			}
		return meta;
		}
	
	public String pool_meta (String channel, String field)
		{
		if (channel == null)
			{
			Log.i ("vtest", "pool_meta: was given a null channel");
			return null;
			}
		Hashtable <String, String> meta = channel_hash_by_id (channel);
		if (meta == null)
			{
			// Log.i ("vtest", "pool_meta: Channel \"" + channel + "\" not found!"); // noisy
			return null;
			}
		String ret = null;
		try
			{
			channel_lock.lock();
			ret = meta.get (field);
			}
		finally
			{
			channel_lock.unlock();
			}
		return ret;
		}
	
	public String channel_meta (int channel, String field)
		{
		if (occupied (channel))
			{
			String ret = null;
			Hashtable <String, String> meta = null;
			try
				{
				channel_lock.lock();
				meta = channelgrid.get (channel);
				if (meta != null)
					{
					// Log.i ("vtest", "(meta " + channel + ") field " + field);
					ret = meta.get (field);
					}
				}
			finally
				{
				channel_lock.unlock();
				}
			return ret;
			}
		else
			return null;
		}

	public void OBSOLETE_set_channel_meta (int channel, String field, String value)
		{
		if (occupied (channel))
			{
			Hashtable <String, String> meta = channelgrid.get (channel);
			meta.put (field, value);
			}
		}

	public void set_channel_meta_by_id (String channel_id, String field, String value)
		{
		Hashtable <String, String> meta = channel_hash_by_id (channel_id);
		if (meta != null)
			{
			try
				{
				channel_lock.lock();
				meta.put (field, value);
				}
			finally
				{
				channel_lock.unlock();
				}
			// Log.i ("vtest", "set meta by id, channel=" + channel_id + ", field=" + field + ", value=" + value);
			}
		}

	public void set_program_meta (String episode_id, String field, String value)
		{
		try
			{
			program_lock.lock();
			Hashtable <String, String> program = programgrid.get (episode_id);
			if (program != null)
				{	
				program.put (field,  value);
				programgrid.put (episode_id, program);
				}
			}
		finally
			{
			program_lock.unlock();
			}
		}
	
	public boolean OBSOLETE_is_youtube_channel (int channel)
		{
		if (!occupied (channel))
			{
			Log.i ("vtest", "Youtube? " + channel + " NOT OCCUPIED");
			return false;
			}
		String s = channel_meta (channel, "thumb");
		Log.i ("vtest", "Youtube? " + channel + " " + s);
		return s != null && s.contains ("ytimg.com");
		}

	public String program_meta (String episode, String field)
		{
		if (episode != null && field != null)
			{
			String ret = null;
			Hashtable <String, String> meta = null;
			try
				{
				program_lock.lock();
				meta = programgrid.get (episode);
				if (meta != null)
					ret = meta.get (field);
				}
			finally
				{
				program_lock.unlock();
				}
			
			return ret;
			}
		else
			return null;
		}

	public String program_meta_nolock (String episode, String field)
		{
		if (episode != null && field != null)
			{
			String ret = null;
			Hashtable <String, String> meta = null;

			meta = programgrid.get (episode);
			ret = meta.get (field);
		
			return ret;
			}
		else
			return null;
		}
	
	public String titlecard_meta (String title_id, String field)
		{
		if (title_id != null && field != null)
			{
			String ret = null;
			Hashtable <String, String> meta = null;
			try
				{
				program_lock.lock();
				meta = titlecards.get (title_id);
				if (meta != null)
					ret = meta.get (field);
				}
			finally
				{
				program_lock.unlock();
				}
			
			return ret;
			}
		else
			return null;
		}
	
	public int server_to_client (int grid)
		{
		int column = grid % 9;
		if (column == 0)
			column = 9;
		int row = 1 + ((grid - 1) / 9);
		return row * 10 + column;
		}

	public int client_to_server (int position)
		{
		if (position >= 11 && position <= 99)
			{
			int row = position / 10;
			int column = position % 10;
			return (row - 1) * 9 + column;
			}
		else
			return -1;
		}
	
	public String episode_in_cache (String episode)
		{
		String ext = "png";
		String url = program_meta (episode, "thumb");
		
		if (url != null && url.equals (""))
			{
			String[] fields = url.split ("\\.");
			ext = fields [fields.length - 1];
			}
		
		String channel = program_meta (episode, "channel");
		if (channel == null)
			{
			Log.i ("vtest", "episode_in_cache: channel for episode \"" + episode + "\" is null");
			return null;
			}
		
		String actual_channel = channel;
		if (channel.contains (":"))
			actual_channel = program_meta (episode, "real_channel");
		
		String physical_episode = episode;
		if (physical_episode.contains (":"))
			{
			String[] fields = physical_episode.split (":");
			if (fields.length == 2)
				physical_episode = fields[1];
			}
		return api_server + "/ethumbs/" + actual_channel + "/" + physical_episode + "." + ext;
		}

	public String episode_in_cache (String channel, String episode)
		{
		String url = program_meta (episode, "thumb");
		String ext = "png";
		if (url != null)
			{
			String[] fields = url.split ("\\.");
			// ext = fields [fields.length - 1];
			}
		
		String physical_episode = episode;
		if (physical_episode.contains (":"))
			{
			String[] fields = physical_episode.split (":");
			if (fields.length == 2)
				physical_episode = fields[1];
			}
		
		/* have to assume png, or we have to pass a context and start poking around file directories */
		return api_server + "/ethumbs/" + channel + "/" + physical_episode + "." + ext;
		}
	
	public String youtube_username_to_channel_id (String username)
		{
		Hashtable <String, String> meta = null;
		try
			{
			channel_lock.lock();
			meta = channels_by_youtube.get (username.toLowerCase());
			}
		finally
			{
			channel_lock.unlock();
			}

		String ret = null;
		if (meta != null)
			{
			try
				{
				channel_lock.lock();
				ret = meta.get ("id");
				}
			finally
				{
				channel_lock.unlock();
				}
			}
		
		return ret;
		}

	public int number_of_channels_in_set (final String virtual_channel_id)
		{
		int count = 0;
		
		if (virtual_channel_id != null)
			{
			try
				{
				channel_lock.lock();
				Hashtable <String, String> set = sets.get (virtual_channel_id);
				if (set != null)
					{
					count = 0;
					for (Entry <String, String> entry : set.entrySet ())
						{
						String channel_id = entry.getValue();
						if (channel_pool.get (channel_id) != null)
							count++;
						}
					}
				}
			finally
				{
				channel_lock.unlock();	
				}
			}
		return count;
		}
	
	public String[] list_of_channels_in_set (final String virtual_channel_id)
		{
		int count = 0;
		String channel_ids[] = null;
		
		if (virtual_channel_id != null)
			{
			try
				{
				channel_lock.lock();
				Hashtable <String, String> set = sets.get (virtual_channel_id);
				if (set != null)
					{
					count = 0;
					for (Entry <String, String> entry : set.entrySet ())
						count++;
					channel_ids = new String [count + 1];
					count = 0;
					for (Entry <String, String> entry : set.entrySet ())
						{
						String channel_id = entry.getValue();
						channel_ids [++count] = channel_id;
						}
					}
				}
			finally
				{
				channel_lock.unlock();	
				}
			}		
				
		if (channel_ids == null)
			return new String[] {};
		
		Log.i ("vtest", "set \"" + virtual_channel_id + "\" has " + count + " channels");
		
		Arrays.sort (channel_ids, new Comparator <String>()
			{
			@Override
			public int compare (String a, String b)
				{
				int a_position = 0;
				int b_position = 0;
				
				try
					{
					if (a != null)
						{
						// Log.i ("vtest", "++ (a)VIRTUAL: " + virtual_channel_id + ", a=" + a);
						String sort = pool_meta (a, "#" + virtual_channel_id);
						// Log.i ("vtest", "__(a)sort: " + sort);						
						}
					if (b != null)
						{
						// Log.i ("vtest", "++ (b)VIRTUAL: " + virtual_channel_id + ", b=" + b);
						String sort = pool_meta (b, "#" + virtual_channel_id);
						// Log.i ("vtest", "__(b)sort: " + sort);						
						}					
					a_position = a == null ? 0 : Integer.parseInt (pool_meta (a, "#" + virtual_channel_id));
					b_position = b == null ? 0 : Integer.parseInt (pool_meta (b, "#" + virtual_channel_id));
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				
				// Log.i ("vtest", "compare (" + a + "," + b + ")" + " = " + "(" + a_position + "," + b_position + ")");
				
				if (a_position > b_position)
					return 1;
				else if (a_position < b_position)
					return -1;
				else
					return 0;
				}
			});
		
		for (String channel: channel_ids)
			{
			String m0 = pool_meta (channel, "name");
			String m1 = pool_meta (channel, "#" + virtual_channel_id);
			Log.i ("vtest", "sorted " + m1 + " (" + m0 + ")");
			}
		return channel_ids;
		}
	
	public void add_channel_to_set (String virtual_channel_id, String line, int sort)
		{
		String channel_id = parse_channel_info_line (line);
		
		/* ugly way to store the order of channels in this set */
		set_channel_meta_by_id (channel_id, "#" + virtual_channel_id, Integer.toString (sort));

		try
			{
			channel_lock.lock();
			Hashtable <String, String> set = sets.get (virtual_channel_id);
			if (set == null)
				{
				set = new Hashtable <String, String> ();
				sets.put (virtual_channel_id, set);
				}
			set.put (channel_id, channel_id);
			}
		finally
			{
			channel_lock.unlock();	
			}
		}
	
	public int channels_in_virtual_channel (String virtual_channel_id)
		{
		int count = 0;
		
		if (virtual_channel_id != null)
			{
			try
				{
				channel_lock.lock();
				for (Entry <String, Hashtable <String, String>> entry : channel_pool.entrySet ())
					{
					Hashtable <String, String> v = entry.getValue();
					String position = v.get ("#" + virtual_channel_id);
					if (position != null)
						count++;
					}
				}
			finally
				{
				channel_lock.unlock();	
				}
			}
	
		return count;
		}
	
	public String[] list_of_channels_in_virtual (final String virtual_channel_id)
		{
		int count = channels_in_virtual_channel (virtual_channel_id);

		if (count > 0)
			{
			String results[] = new String [count];

			int n = 0;

			if (virtual_channel_id != null)
				{
				try
					{
					channel_lock.lock();
					for (Entry <String, Hashtable <String, String>> entry : channel_pool.entrySet ())
						{
						String k = entry.getKey();
						Hashtable <String, String> v = entry.getValue();
						String position = v.get ("#" + virtual_channel_id);
						if (position != null)
							{
							results [n++] = k;
							}
						}
					}
				finally
					{
					channel_lock.unlock();
					}
				Arrays.sort (results, new Comparator <String>()
						{
						@Override
						public int compare (String a, String b)
							{
							int a_position = 0;
							int b_position = 0;
							
							try
								{
								a_position = Integer.parseInt (pool_meta (a, "#" + virtual_channel_id));
								b_position = Integer.parseInt (pool_meta (b, "#" + virtual_channel_id));
								}
							catch (Exception ex)
								{
								ex.printStackTrace();
								}
							if (a_position > b_position)
								return -1;
							else if (a_position < b_position)
								return 1;
							else
								return 0;
							}
						});
				return results;
				}
			}
		return null;
		}
	
	String most_recent_episode_id (String channel_id)
		{
		String program_line[] = program_line_by_id (channel_id);
			
		if (program_line == null)
			{
			try
				{
				program_lock.lock();
				/* check scattered virtual channels, try to find something */
				for (Entry <String, Hashtable <String, String>> entry : programgrid.entrySet ())
					{
					String this_channel = program_meta_nolock (entry.getKey(), "channel");
					if (this_channel != null && this_channel.equals (channel_id))
						return entry.getKey();
					String real_channel = program_meta_nolock (entry.getKey(), "real_channel");
					if (real_channel != null && real_channel.equals (channel_id))
						return entry.getKey();
					}
				}
			finally
				{
				program_lock.unlock();
				}
			Log.i ("vtest", "no recent episodes in: " + channel_id);
			return "";
			}
		
		Log.i ("vtest", "most recent episode in " + channel_id + ": " + program_line[0]);
		return program_line[0];
		}
	
	String episode_with_typical_thumbnail (String channel_id)
		{
		String ret = pool_meta (channel_id, "most_recent_thumb_id");
		if (ret == null)
			{
			ret = most_recent_episode_id (channel_id);
			/* TODO: save this in most_recent_thumb_id */
			return (ret == null) ? "" : ret;
			}
		else
			return ret;
		}
	
	public void set_comment (String episode_id, String number, Comment comment)
		{
		try
			{
			program_lock.lock();
			Hashtable <String, Comment> comments_for_episode = comments.get (episode_id);
			if (comments_for_episode == null)
				{
				comments_for_episode = new Hashtable <String, Comment> ();
				comments.put (episode_id, comments_for_episode);
				}
			comments_for_episode.put (number,  comment);
			
			String max_comment = program_meta_nolock (episode_id, "maxcomment");
			if (max_comment == null || max_comment.equals ("") || Integer.parseInt (max_comment) < Integer.parseInt (number))
				{
				Hashtable <String, String> meta = programgrid.get (episode_id);
				meta.put ("maxcomment", number);
				}
			}
		finally
			{
			program_lock.unlock();
			}
		}
	
	public Comment get_comment (String episode_id, String number)
		{
		try
			{
			program_lock.lock();
			Hashtable <String, Comment> comments_for_episode = comments.get (episode_id);
			if (comments_for_episode != null)
				{
				return comments_for_episode.get (number);
				}
			else
				return null;
			}
		finally
			{
			program_lock.unlock();
			}
		}
	
	public void dump_episode_details (String episode_id)
		{
		try
			{
			program_lock.lock();
			Hashtable <String, String> program = programgrid.get (episode_id);
			if (program != null)
				{
				for (Entry <String, String> entry : program.entrySet())
					{
					String key = entry.getKey();
					String value = entry.getValue();
					Log.i ("vtest", "program " + episode_id + " :: " + key + "=" + value);
					}
				}
			}
		finally
			{
			program_lock.unlock();
			}
		}
	
	public void set_special_tag (String channel_id, String situation, String specific, String tag)
		{
		try
			{
			channel_lock.lock();
			Hashtable <String, String> taglist = special_tags.get (channel_id);
			if (taglist == null)
				taglist = new Hashtable <String, String> ();
			taglist.put (situation + ":" + specific, tag);
			special_tags.put (channel_id, taglist);
			}
		finally
			{
			channel_lock.unlock();
			}
		}
	
	public String get_special_tag (String channel_id, String situation, String specific)
		{
		String tag = null;
		try
			{
			channel_lock.lock();
			Hashtable <String, String> taglist = special_tags.get (channel_id);
			if (taglist == null)
				taglist = new Hashtable <String, String> ();
			tag = taglist.get (situation + ":" + specific);
			}
		finally
			{
			channel_lock.unlock();
			}
		return tag;
		}
	}