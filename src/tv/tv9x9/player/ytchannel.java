package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.tv9x9.player.metadata.Comment;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

public class ytchannel
	{
	public interface Notifier
		{
		public void success (String[] lines);

		public void failure (int code, String errtext);
		}

	public static String fetch_youtube (String nature, String username, int start_index)
		{
		Log.i ("vtest", "Requesting YouTube channel: " + username);

		// nature 3: format=5&orderby=published
		// nature 4: format=5&orderby=position
		
		String url = null;
		if (nature.equals ("3"))
			url = "http://gdata.youtube.com/feeds/api/users/" + username
					+ "/uploads?v=2&alt=json&start-index=" + start_index + "&max-results=50&prettyprint=true";
		else if (nature.equals ("4"))
			url = "http://gdata.youtube.com/feeds/api/playlists/" + username
					+ "?v=2&alt=json&start-index=" + start_index + "&max-results=50&prettyprint=true";
		else if (nature.equals ("s"))
			url = "http://gdata.youtube.com/feeds/api/channels?q=" + username
					+ "&v=2&alt=json&start-index=" + start_index + "&max-results=50&prettyprint=true";
		else
			{
			Log.i ("vtest", "unknown channel nature: " + nature);
			return null;
			}

		return fetch_url (url);
		}

	public static String fetch_youtube_stats (String username)
		{
		Log.i ("vtest", "Requesting YouTube stats: " + username);
		String url = "http://gdata.youtube.com/feeds/api/users/" + username + "?alt=json&prettyprint=true";
		return fetch_url (url);
		}
	
	public static String fetch_youtube_comments (String video_id)
		{
		Log.i ("vtest", "Requesting YouTube comments: " + video_id);
		String url = "http://gdata.youtube.com/feeds/api/videos/" + video_id + "/comments?v=2&alt=json&start-index=1&prettyprint=true";		
		return fetch_url (url);
		}
	
	public static String fetch_youtube_live_info (String video_id)
		{
		Log.i ("vtest", "Requesting YouTube live broadcast info: " + video_id);
		String url = "http://gdata.youtube.com/feeds/api/users/live/broadcasts/" + video_id + "/states?v=2&alt=json&prettyprint=true";
		return fetch_url (url);
		}
	
	public static String fetch_url (String url)
		{
		HttpClient client = new DefaultHttpClient ();
	
		Log.i ("vtest", "calling YouTube API: " + url);
	
		HttpGet request = new HttpGet (url);
		String answer;
		ResponseHandler <String> responseHandler = new BasicResponseHandler ();
	
		try
			{
			answer = client.execute (request, responseHandler);
			// Log.i ("vtest", "fetch returns: " + answer);
			}
		catch (Exception ex)
			{
			// 'org.apache.http.client.HttpResponseException: Not Found'
			answer = "ERROR:" + ex.toString ();
			}
	
		return answer;
		}
	
	public static void youtube_live_info
			(final metadata config, final String video_id, final Handler handler, final Callback update)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				String data = fetch_youtube_live_info (video_id);
				final String when = parse_youtube_live_info (config, video_id, data);
				if (update != null)
					handler.post (new Runnable()
						{
						public void run()
							{
							if (update != null)
								update.run_string (when);
							}
						});
				}
			};
		
		t.start();
		}
	
	public static void youtube_channel_search_in_thread
			(final metadata config, final String term, final Handler handler, final Callback update)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				Log.i ("vtest", "youtube_channel_search_in_thread: \"" + term + "\"");
				String data = fetch_youtube ("s", term, 1);
				final String channels[] = parse_youtube_search (config, data);
				if (update != null)
					handler.post (new Runnable()
						{
						public void run()
							{
							if (update != null)
								update.run_string_array (channels);
							}
						});
				}
			};
	
		t.start();
		}
	
	public static void fetch_and_parse_by_id_in_thread 
			(final Context ctx, final metadata config, final String channel_id, final boolean allow_cache, final Handler handler, final Runnable update)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				Callback call_my_runnable = new Callback()
					{
					public void run_string (String channel)
						{
						Log.i ("vtest", "BLORT runnable 1");
						handler.post (update);
						}
					};
				if (config.is_youtube (channel_id))
					{
					Log.i ("vtest", "BLORT1: " + channel_id);
					if (allow_cache)
						return_any_cache (ctx, handler, call_my_runnable, config, channel_id);
					String data = fetch_and_parse_by_id (config, channel_id);
					store_in_cache (ctx, config, channel_id, data);
					config.set_channel_meta_by_id (channel_id, "loaded", "yes");
					handler.post (update);
					}
				else
					{
					if (allow_cache)
						return_any_cache (ctx, handler, call_my_runnable, config, channel_id);
					// if (config.programs_in_real_channel(channel_id) > 0)
						// TODO: temp!
						// return;
					Log.i ("vtest", "BLORTX: " + channel_id + " is a 9x9 channel");
					fetch_and_parse_32 (ctx, handler, call_my_runnable, config, channel_id, 1);
					}
				}
			};
	
		t.start();
		}

	public static void fetch_and_parse_by_id_in_thread 
			(final Context ctx, final metadata config, final String channel_id, 
					final boolean allow_cache, final Handler handler, final Callback update, final String arg1, final Object arg2)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				/* want to callback with two arguments, not just one */
				Callback call_my_callable = new Callback()
					{
					public void run_string (String channel)
						{
						handler.post (new Runnable()
							{
							public void run()
								{
								if (update != null)
									{
									Log.i ("vtest", "BLORT callback 2");
									update.run_string_and_object (arg1, arg2);
									}
								}
							});
						}
					};
					
				if (config.is_youtube (channel_id))
					{
					Log.i ("vtest", "BLORT2: " + channel_id);
					if (allow_cache)
						return_any_cache (ctx, handler, call_my_callable, config, channel_id);
					String data = fetch_and_parse_by_id (config, channel_id);
					store_in_cache (ctx, config, channel_id, data);
					config.set_channel_meta_by_id (channel_id, "loaded", "yes");
					handler.post (new Runnable()
						{
						@Override
						public void run()
							{
							update.run_string_and_object (arg1, arg2);
							}						
						});
					}
				else
					{
					Log.i ("vtest", "BLORTX: " + channel_id + " is a 9x9 channel");
					if (allow_cache)
						return_any_cache (ctx, handler, call_my_callable, config, channel_id);					
					if (config.programs_in_real_channel(channel_id) > 0)
						// TODO: temp!
						return;
					fetch_and_parse_32 (ctx, handler, call_my_callable, config, channel_id, 1);
					}
				}
			};
		
		t.start();
		}
	
	public static void extend_channel (final metadata config, final String channel_id, final Handler h, final Runnable callback)
		{
		if (channel_id == null)
			{
			return;
			}
		
		String extending = config.pool_meta (channel_id, "extending");
		
		/* avoid overhead of creating thread */
		if (extending != null && !extending.equals(""))
			return;
			
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				String extending = config.pool_meta (channel_id, "extending");
				if (extending == null || extending.equals(""))
					{
					config.set_channel_meta_by_id (channel_id, "extending", "true");
					String extent = config.pool_meta (channel_id, "extent");
					final int new_extent = extent == null ? 51 : Integer.parseInt (extent) + 50;
					String nature = config.pool_meta (channel_id, "nature");
					
					if (new_extent >= 201)
						{
						Log.i ("vtest", "new extent would be over 201 " + new_extent + ", will not extend " + channel_id);
						return;
						}
					
					if (nature.equals ("667"))
						{
						Log.i ("vtest", "will not extend special channel with nature " + nature);
						return;
						}
					else if (config.is_youtube (channel_id))
						{
						String username = config.pool_meta (channel_id, "extra");
						Log.i ("vtest", "extending youtube channel \"" + channel_id + "\": " + username + " (at: " + extent + ")");
						String data = fetch_youtube (nature, username, new_extent);
						parse_youtube (config, channel_id, nature, username, data);
						config.set_channel_meta_by_id (channel_id, "extent", Integer.toString (new_extent));
						config.set_channel_meta_by_id (channel_id, "extending", "");
						h.post (callback);
						}
					else
						{
						/* 9x9 channel */
						Log.i ("vtest", "extending 9x9 channel \"" + channel_id + "\" (at: " + extent + ")");
						/* wants a Callback not a Runnable */
						Callback call_my_callable = new Callback()
							{
							public void run_string (String channel)
								{
								h.post (new Runnable()
									{
									public void run()
										{
										config.set_channel_meta_by_id (channel_id, "extent", Integer.toString (new_extent));
										config.set_channel_meta_by_id (channel_id, "extending", "");	
										h.post (callback);
										}
									});
								}
							};
						fetch_and_parse_32 (null, h, call_my_callable, config, channel_id, new_extent);
						}		
					}	
				else
					Log.i ("vtest", "already in process of extending " + channel_id);
				}
			};
			
		t.start();
		}
	
	public static String fetch_and_parse_by_id (final metadata config, String channel_id)
		{
		Log.i ("vtest", "fetch and parse by channel id: " + channel_id);
		String nature = config.pool_meta (channel_id, "nature");
		String extra = config.pool_meta (channel_id, "extra");
		if (config.is_youtube (channel_id))
			{
			String data = fetch_and_parse_youtube (config, channel_id, nature, extra);
			return data;
			}
		else
			{
			Log.i ("vtest", "!! channel " + channel_id + " nature is: " + nature);
			// fetch_and_parse_teltel (config, channel_id, nature, extra);
			return null;
			}
		}
	
	public static String fetch_and_parse_youtube (metadata config, String channel_id, String nature, String username)
		{
		Log.i ("vtest", "fetch_and_parse_youtube channel \"" + channel_id + "\": " + username);
		String data = fetch_youtube (nature, username, 1);
		
		if (data != null)
			{
			config.set_channel_meta_by_id (channel_id, "extent", "1");
			parse_youtube (config, channel_id, nature, username, data);
			}
		
		/* in case we want to cache the data */
		return data;
		}
	
	public static void parse_youtube (metadata config, String channel_id, String nature, String username, String data)
		{
		if (data == null)
			{
			Log.i ("vtest", "youtube channel JSON data is null");
			return;
			}
		if (data.startsWith ("ERROR"))
			{
			Log.i ("vtest", "error in HTTP request: " + data);
			return;
			}
		
		int sort_base = config.highest_sort (channel_id);
		
		Log.i ("vtest", "(channel " + channel_id + ") data starts with: " + data.substring(0,20));
		try
			{
			JSONObject json = new JSONObject (data);
			JSONObject dataObject = null;
			try
				{
				dataObject = json.getJSONObject ("feed");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no dataObject! channel: " + channel_id);
				return;
				}
			
			int total_count = 0;
			try
				{
				JSONObject countObject = dataObject.getJSONObject("openSearch$totalResults");
				total_count = countObject.getInt ("$t");
				Log.i ("vtest", "YouTube count for " + channel_id + ": " + total_count);
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "no openSearch$totalResults for channel: " + channel_id);
				}
			
			JSONArray entries = null;
			try
				{
				entries = dataObject.getJSONArray ("entry");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no entries! channel: " + channel_id);
				return;
				}
			
			for (int i = 0; i < entries.length(); i++)
				{
				// Log.i ("vtest", "entry: " + i + " of " + entries.length ());
				JSONObject entry = entries.getJSONObject (i);
				if (entry == null)
					{
					Log.i ("vtest", "JSON: entry #" + i + " is null! channel: " + channel_id);
					continue;
					}
			      
				JSONObject title_container = entry.getJSONObject ("title");
				String title = (title_container != null) ? title_container.getString ("$t") : "[no title]";
				
				boolean unplayable = false;
				
				JSONArray access_list = null;
				try
					{ access_list = entry.getJSONArray ("yt$accessControl"); } 
				catch (Exception ex)
					{
					unplayable = true;
					Log.i ("vtest", "JSON: no yt$accessControl -- marking as unplayable");
					};
					
				if (access_list != null)
					{
					for (int a = 0; a < access_list.length(); a++)
						{
						JSONObject access_entry = access_list.getJSONObject (a);
						String access_action = access_entry.getString ("action");
						String access_permission = access_entry.getString ("permission");
						// Log.i ("vtest", "ACCESS :: ACTION=" + access_action + " PERMISSION=" + access_permission);
						if (access_action.equals ("embed") && access_permission.equals ("denied"))
							unplayable = true;
						if (access_action.equals ("syndicate") && access_permission.equals ("denied"))
							unplayable = true;						
						}
					}

				if (!unplayable)
					{
					JSONObject app_control = null;
					try
						{ app_control = entry.getJSONObject ("app$control"); }
					catch (Exception ex)
						{
						// Log.i ("vtest", "JSON: no app$control");
						};					
					
					if (app_control != null)
						{
						JSONObject yt_state = null;
						try
							{ yt_state = app_control.getJSONObject ("yt$state"); }
						catch (Exception ex)
							{
							Log.i ("vtest", "JSON: no yt$state");
							unplayable = true;
							};
						if (yt_state != null)
							{
							String state_name = yt_state.getString ("name");
							String state_reason = yt_state.getString ("reasonCode");
							// Log.i ("vtest", "YT STATE :: NAME=" + state_name + " REASON=" + state_reason);
							if (state_name.equals ("restricted") && !state_reason.equals ("limitedSyndication"))
								unplayable = true;
							}
						}
					}
				
				if (unplayable)
					{
					Log.i ("vtest", "video is probably unplayable, skipping");
					continue;
					}
				
				String video_url = null;
				if (nature.equals ("3"))
					{
			 		JSONObject video_id_container = entry.getJSONObject ("id");
					video_url = (video_id_container != null) ? video_id_container.getString ("$t") : "";
					}
				else if (nature.equals ("4") || nature.equals ("s"))
					{
					JSONArray links = entry.getJSONArray ("link");
					if (links != null)
						{
						JSONObject first_link = links.getJSONObject (0);
						video_url = first_link.getString ("href");
						}
					}
				
				String thumb = "";
				String upload_date = "";
				String desc = "";
				String duration = "";
				
				JSONObject media_group = entry.getJSONObject ("media$group");
				if (media_group != null)
					{
					JSONArray media_thumbnails = null;
					JSONObject thumb_container = null;
					try
						{ media_thumbnails = media_group.getJSONArray ("media$thumbnail"); } 
					catch (Exception ex)
						{ Log.i ("vtest", "JSON: no media$thumbnail"); };
					if (media_thumbnails != null)
						thumb_container = media_thumbnails.getJSONObject (2);
					thumb = (thumb_container != null) ? thumb_container.getString ("url") : "";
					
				 	JSONObject timestamp_container = media_group.getJSONObject ("yt$uploaded");
					upload_date = (timestamp_container != null) ? timestamp_container.getString ("$t") : "";
					
					JSONObject duration_container = null;
				 	try
				 		{ duration_container = media_group.getJSONObject ("yt$duration"); }
				 	catch (Exception ex)
				 		{ Log.i ("vtest", "JSON: no yt$duration"); };
					duration = (duration_container != null) ? duration_container.getString ("seconds") : "";
					
					JSONObject desc_container = null;
					try
						{ desc_container = media_group.getJSONObject ("media$description"); }
					catch (Exception ex)
						{ Log.i ("vtest", "JSON: no yt$duration"); };
					desc = (desc_container != null) ? desc_container.getString ("$t") : "";
					}
				
				/* example 2012-11-11T05:57:15.000Z -- SimpleDateFormat is not documented as working with "Z" type timezones */
				upload_date = upload_date.replace ("Z", "-0000");
				
				String timestamp;
                SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                try
                	{
					Date dt = sdf.parse (upload_date);
					timestamp = "" + dt.getTime();
                	}
                catch (ParseException e1)
                	{
					timestamp = "";
                	}
                
				// Log.i ("vtest", "video url: " + video_url);
				
				String video_id = null;
				if (nature.equals ("3"))
					{
					int voffset = video_url.indexOf ("?v=");
					if (voffset > 0)
						{
						try
							{
							video_id = video_url.substring (voffset + 3, voffset + 3 + 11);
							}
						catch (Exception e)
							{
							e.printStackTrace();
							Log.i ("vtest", "Error with ?v= in URL: " + video_url);
							}
						}
					else
						{
						voffset = video_url.indexOf ("video:");
						if (voffset > 0)
							{
							video_id = video_url.substring (voffset + 6, voffset + 6 + 11);
							}
						}
					}
				else if (nature.equals ("4"))
					{
					// Log.i ("vtest", "playlist episode url: " + video_url);
					int voffset = video_url.indexOf ("video:");
					if (voffset > 0)
						{
						video_id = video_url.substring (voffset + 6, voffset + 6 + 11);
						}
					else
						{
						voffset = video_url.indexOf ("api/videos/");
						if (voffset > 0)
							{
							video_id = video_url.substring (voffset + 11, voffset + 11 + 11);
							}
						else
							{
							voffset = video_url.indexOf ("?v=");
							if (voffset > 0)
								{
								video_id = video_url.substring (voffset + 3, voffset + 3 + 11);
								}
							}
						}
					}
				video_url = "http://www.youtube.com/watch?v=" + video_id;

				// Log.i ("vtest", "id: " + video_id + " url: " + video_url + " title: " + title + " thumb: " + thumb);

				Hashtable <String, String> program = new Hashtable <String, String> ();

				program.put ("sort", Integer.toString (sort_base + 1 + i));
				program.put ("id", video_id);
				program.put ("channel", channel_id);
				program.put ("name", title);
				program.put ("desc", desc);
				program.put ("thumb", thumb);
				program.put ("url1", video_url);
				program.put ("url2", "");
				program.put ("url3", "");
				program.put ("url4", "");
				program.put ("timestamp", timestamp);
				program.put ("duration", duration);

				if (channel_id.equals ("virtual:following") && username != null)
					{
					String real_channel = config.youtube_username_to_channel_id (username);
					program.put ("real_channel", real_channel);
					}
				
				config.add_programs_from_youtube (program);
				}

			config.set_channel_meta_by_id (channel_id, "fetched", "1");
			config.set_channel_meta_by_id (channel_id, "count", Integer.toString (total_count));
			}
		catch (JSONException e)
			{
			e.printStackTrace ();
			}
		}
	
	// d.feed.entry[0].title.$t
	// d.feed.entry[0].updated.$t
	// d.feed.entry[0].author[0].uri.$t  <- https://gdata.youtube.com/feeds/api/users/machinima
	// d.feed.entry[0].media$thumbnail[0].url   <-- channel thumb
	// d.feed.entry[0].gd$feedLink[0].countHint
	
	public static String[] parse_youtube_search (metadata config, String data)
		{
		if (data == null)
			{
			Log.i ("vtest", "youtube channel JSON data is null");
			return null;
			}
		if (data.startsWith ("ERROR"))
			{
			Log.i ("vtest", "error in HTTP request: " + data);
			return null;
			}

		String commas = "";
		
		Log.i ("vtest", "search data starts with: " + data.substring(0,20));
		try
			{
			JSONObject json = new JSONObject (data);
			JSONObject dataObject = null;
			try
				{
				dataObject = json.getJSONObject ("feed");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no dataObject in search data!");
				return null;
				}
			JSONArray entries = null;
			try
				{
				entries = dataObject.getJSONArray ("entry");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no entries in search data!");
				return null;
				}
			
			for (int i = 0; i < entries.length(); i++)
				{
				Log.i ("vtest", "search entry: " + i + " of " + entries.length());
				JSONObject entry = entries.getJSONObject (i);
				if (entry == null)
					{
					Log.i ("vtest", "JSON: entry #" + i + " of search data is null!");
					continue;
					}
			      
				JSONObject title_container = entry.getJSONObject ("title");
				String title = (title_container != null) ? title_container.getString ("$t") : "[no title]";
	
				JSONObject updated_container = entry.getJSONObject ("updated");			
				String updated_date = (updated_container != null) ? updated_container.getString ("$t") : "";
				
				/* example 2012-11-11T05:57:15.000Z -- SimpleDateFormat is not documented as working with "Z" type timezones */
				updated_date = updated_date.replace ("Z", "-0000");
				
				String timestamp = "";
	            SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	            try
	            	{
					Date dt = sdf.parse (updated_date);
					timestamp = "" + dt.getTime();
	            	}
	            catch (ParseException e1)
	            	{
	            	}
	            
	            JSONArray authors = null;
	            try
	            	{
	            	authors = entry.getJSONArray ("author");
	            	}
	            catch (Exception e2)
	            	{
	            	Log.i ("vtest", "no authors in this search entry");
	            	continue;
	            	}
	            
				JSONObject first_author = authors.getJSONObject (0);
				JSONObject first_uri_container = first_author.getJSONObject ("uri");	
				
				String uri = first_uri_container.getString ("$t");

	            JSONArray media_thumbnails = null;
	            try
	            	{
	            	media_thumbnails = entry.getJSONArray ("media$thumbnail");
	            	}
	            catch (Exception e3)
	            	{
	            	Log.i ("vtest", "no media thumbnails in this search entry");
	            	continue;
	            	}
	            
	            JSONObject first_media_thumbnail = media_thumbnails.getJSONObject (0);
	            
				String channel_thumb = first_media_thumbnail.getString ("url");
				
				
	            JSONArray gd_feed_links = null;
	            try
	            	{
	            	gd_feed_links = entry.getJSONArray ("gd$feedLink");
	            	}
	            catch (Exception e3)
	            	{
	            	Log.i ("vtest", "no gd$feedLink in this search entry");
	            	continue;
	            	}
	            
	            JSONObject first_gd_feed_link = gd_feed_links.getJSONObject (0);
	            
	            int count_hint = first_gd_feed_link.getInt ("countHint");
	         
	            if (count_hint <= 0)
	            	{
	            	Log.i ("vtest", "no episodes seem to be in this search entry, skipping");
	            	continue;
	            	}

	            String fields[] = uri.split ("/");
	            String username = null;
	            if (fields.length > 0)
	            	username = fields [fields.length - 1];
	            
				Log.i ("vtest", "title: " + title);
				Log.i ("vtest", "uri: " + uri);
				Log.i ("vtest", "username: " + username);
				Log.i ("vtest", "channel_thumb: " + channel_thumb);
				Log.i ("vtest", "count hint: " + count_hint);
	
				String channel_id = "=" + username;
				config.add_channel (0, channel_id, title, "", channel_thumb, "" + count_hint, "", "", "3", username);
				
				config.set_channel_meta_by_id (channel_id, "timestamp", timestamp);

				commas += (commas.isEmpty() ? "" : ",") + channel_id;
				}
			}
		catch (JSONException e)
			{
			e.printStackTrace ();
			}
		
		/* this is absurd but temporary */
		if (commas.equals(""))
			return new String[] {};
		else
			return commas.split (",");
		}
		
	public static String parse_youtube_live_info (metadata config, String video_id, String data)
		{
		// x.feed.yt$when.start
		
		JSONObject json = null;
		try
			{
			json = new JSONObject (data);
			}
		catch (Exception ex)
			{
			Log.i ("vtest", "-- live info: JSON parse error: " + video_id);
			return null;
			}

		JSONObject feed = null;		
		try
			{
			feed = json.getJSONObject ("feed");
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			Log.i ("vtest", "-- live info: JSON: no \"feed\" in JSON for: " + video_id);
			return null;
			}

		JSONObject when = null;		
		try
			{
			when = feed.getJSONObject ("yt$when");
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			Log.i ("vtest", "-- live info: JSON: no \"yt$when\" in JSON for: " + video_id);
			return null;
			}

		String start_date = null;		
		try
			{
			start_date = when.getString ("start");
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			Log.i ("vtest", "-- live info: JSON: no \"start\" in JSON for: " + video_id);
			return null;
			}

		/* example 2012-11-11T05:57:15.000Z -- SimpleDateFormat is not documented as working with "Z" type timezones */
		start_date = start_date.replace ("Z", "-0000");
		
		Log.i ("vtest", "-- live info: start date is: " + start_date);
		
		String timestamp = null;
        SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        try
        	{
			Date dt = sdf.parse (start_date);
			timestamp = "" + dt.getTime();
        	}
        catch (ParseException e1)
        	{
			timestamp = "";
        	}
        
		return timestamp;
		}
	
	/* 3.2 style channels with subepisodes */
	
	public static void fetch_and_parse_32
			(final Context ctx, final Handler h, final Callback callback, final metadata config, final String channel_id, final int start)
		{
		Calendar now = Calendar.getInstance();
		int hour = now.get (Calendar.HOUR_OF_DAY);
		
		String userstuff = config.usertoken == null ? "" : ("&user=" + config.usertoken + "&userToken=" + config.usertoken); // TODO: remove userToken= 
		String query = "programInfo?channel=" + channel_id + userstuff + "&start=" + start + "&count=50" + "&time=" + hour;
		
		new playerAPI (h, config, query)
			{
			public void success (String[] lines)
				{
				if (ctx != null && start == 1)
					{
					String data = null;
					for (String line: lines)
						{
						if (data == null)
							data = line;
						else
							data += "\n" + line;
						}
					
					store_in_cache (ctx, config, channel_id, lines);
					}
				config.parse_program_info_32 (lines);
				config.set_channel_meta_by_id (channel_id, "extent", "1");
				config.set_channel_meta_by_id (channel_id, "loaded", "yes");
				h.post (new Runnable()
					{
					public void run()
						{
						if (callback != null)
							callback.run_string (channel_id);
						}
					});
				}
	
			public void failure (int code, String errtext)
				{
				if (code == 1000)
					{
					// 1000 can occur here: NOT PURCHASED
					Log.i ("vtest", "Channel not purchased!");
					}
				else
					Log.i ("vtest", "ERROR! " + errtext);	
				}
			};
		}
	
	public static void fetch_youtube_in_thread (final Handler h, final Runnable callback, 
							final metadata config, final String channel_id, final String nature, final String username)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				fetch_and_parse_youtube (config, channel_id, nature, username);
				if (callback != null)
					h.post (callback);
				}
			};
			
		t.start();
		}
	
	public static void full_channel_fetch (final Handler h, final Callback callback, final metadata config, final String channel_id)
		{
		Log.i ("vtest", "full channel_fetch: " + channel_id);
		
		final String verify_id = config.pool_meta (channel_id, "id");
		if (verify_id != null && !verify_id.equals (""))
			{
			fetch_and_parse_by_id_in_thread (null, h, callback, config, channel_id, false);
			return;
			}
		
		Thread t = new Thread()
			{
			public void run()
				{
				new playerAPI (h, config, "channelLineup?channel=" + channel_id)
					{
					public void success (String[] lines)
						{
						if (lines.length >= 1)
							{
							String id = config.parse_channel_info_line (lines [0]);
							if (id.equals (channel_id))
								fetch_and_parse_by_id_inner (null, h, callback, config, channel_id);
							else
								Log.i ("vtest", "full channel fetch: channel not found");
							}
						else
							Log.i ("vtest", "full channel fetch: error or channel not found");
						}
	
					public void failure (int code, String errtext)
						{
						Log.i ("vtest", "full channel fetch ERROR! " + errtext);
						}
					};
				}
			};
			
		t.start();
		}
	
	public static void fetch_and_parse_by_id_in_thread 
			(final Context ctx, final Handler h, final Callback callback, final metadata config, final String channel_id, final boolean allow_cache)
		{
		Log.i ("vtest", "fetch and parse by channel id: " + channel_id);
		
		Thread t = new Thread()
			{
			public void run()
				{
				if (allow_cache)
					return_any_cache (ctx, h, callback, config, channel_id);
				String data = fetch_and_parse_by_id_inner (ctx, h, callback, config, channel_id);
				if (config.is_youtube (channel_id))
					{
					config.set_channel_meta_by_id (channel_id, "loaded", "yes");	
					store_in_cache (ctx, config, channel_id, data);
					}
				}
			};
			
		t.start();
		}

	public static String fetch_and_parse_by_id_inner
			(Context ctx, final Handler h, final Callback callback, final metadata config, final String channel_id)
		{
		if (channel_id != null)
			{
			final String extra = config.pool_meta (channel_id, "extra");

			if (config.is_youtube (channel_id))
				{
				if (extra == null)
					{
					Log.i ("vtest", "channel " + channel_id + " has no \"extra\" field!");
					return null;
					}	
			
				String nature = config.pool_meta (channel_id, "nature");
				String data = fetch_and_parse_youtube (config, channel_id, nature, extra);
				
				h.post (new Runnable()
					{
					public void run()
						{
						if (callback != null)
							callback.run_string (channel_id);
						}
					});
				
				return data;
				}
			else
				{
				/* must do the callback from within fetch_and_parse_32 */
				fetch_and_parse_32 (ctx, h, callback, config, channel_id, 1);
				}
			}
		else
			{
			if (callback != null)
				callback.run_string (channel_id);
			}
		
		return null;
		}
	
	public static void return_any_cache (Context ctx, final Handler h, final Callback callback, final metadata config, final String channel_id)
		{
		final String nature = config.pool_meta (channel_id, "nature");
		final String extra = config.pool_meta (channel_id, "extra");
		String dir = config.is_youtube (channel_id) ? "ch-youtube-cache" : "ch-flipr-cache";
		if (extra != null)
			{
			if (!thumbnail.make_app_dir (ctx, config, dir)) return;
			String filename = ctx.getFilesDir() + "/" + config.api_server + "/" + dir + "/" + channel_id + ".cache";
			Log.i ("vtest", "** READING FROM " + dir + " CACHE: " + filename);
			File f = new File (filename);
			if (f.exists() && f.length() > 0)
				{
				String data = read_entire_file (ctx, f);
				if (config.is_youtube (channel_id))
					parse_youtube (config, channel_id, nature, extra, data);
				else
					{
					String lines[] = data.split ("\n");
					try
						{
						config.parse_program_info_32 (lines);
						}
					catch (Exception ex)
						{
						/* We don't want to crash just because a cache is bad */
						ex.printStackTrace();
						return;
						}
					}
				if (callback != null)
					{
					Log.i ("vtest", "BLORT CALLBACK: " + channel_id);
					callback.run_string (channel_id);
					}
				}
			}

		}
	
	public static void store_in_cache (Context ctx, metadata config, String channel_id, String data)
		{	
		if (data != null && channel_id != null)
			{
			String dir = config.is_youtube (channel_id) ? "ch-youtube-cache" : "ch-flipr-cache";
			if (!thumbnail.make_app_dir (ctx, config, dir)) return;
			String filename = ctx.getFilesDir() + "/" + config.api_server + "/" + dir + "/" + channel_id + ".cache";
			Log.i ("vtest", "** WRITING TO " + dir + " CACHE: " + filename);
			File f = new File (filename);
			write_entire_file (ctx, f, data);
			}
		}

	public static void store_in_cache (Context ctx, metadata config, String channel_id, String lines[])
		{	
		if (lines != null && channel_id != null)
			{
			String dir = config.is_youtube (channel_id) ? "ch-youtube-cache" : "ch-flipr-cache";
			if (!thumbnail.make_app_dir (ctx, config, dir)) return;
			String filename = ctx.getFilesDir() + "/" + config.api_server + "/" + dir + "/" + channel_id + ".cache";
			Log.i ("vtest", "** WRITING TO " + dir + " CACHE: " + filename);
			File f = new File (filename);
			write_entire_file (ctx, f, lines);
			}
		}
	
	//
	//public static String read_entire_file (Context ctx, File f)
		//{
		//String content = null;
		//try
			//{
			///* oh nasty */
			// content = new Scanner (f).useDelimiter ("\\Z").next();
			//}
		//catch (Exception ex)
			//{
			//ex.printStackTrace();
			//}
		//Log.i ("vtest", "read " + content.length() + " characters");
		//return content;
		//}
	
	public static String read_entire_file (Context ctx, File f)
		{
		StringBuffer stringBuffer = new StringBuffer();
		
		try
		{
		BufferedReader bufferedReader = new BufferedReader (new FileReader(f));
		 
		String line = null;
		 
		while ((line = bufferedReader.readLine()) != null)
			{
			stringBuffer.append(line).append("\n");
			}
		}
		catch (Exception ex)
		{	
		ex.printStackTrace();
		}
		return stringBuffer.toString();
		}
	
	public static void write_entire_file (Context ctx, File f, String data)
		{
	    try
	        {
	        BufferedWriter output = new BufferedWriter (new FileWriter (f));
	        output.write (data);
	        output.flush();
	        output.close();
	        }
	    catch (IOException ex)
	    	{
	    	ex.printStackTrace();
	    	}
		}

	public static void write_entire_file (Context ctx, File f, String lines[])
		{
	    try
	        {
	    	int count = 0;
	        BufferedWriter output = new BufferedWriter (new FileWriter (f));
	        for (String line: lines)
	        	{
	        	if (count++ > 0)
	        		output.write ("\n" + line);
	        	else
	        		output.write (line);
	        	}
	        output.flush();
	        output.close();
	        }
	    catch (IOException ex)
	    	{
	    	ex.printStackTrace();
	    	}
		}
	
	public static void fetch_youtube_comments_in_thread
			(final Handler h, final Runnable callback, final metadata config, final String video_id) 
		{
		String requested = config.program_meta (video_id, "comments_fetched");
		if (requested != null && requested.equals ("true"))
			return;
		config.set_program_meta (video_id, "comments_fetched", "true");
		
		Thread t = new Thread()
			{
			public void run()
				{
				String comments = fetch_youtube_comments (video_id);
				parse_youtube_comments_json (config, video_id, comments);
				if (callback != null)
					h.post (callback);
				gather_thumbnails_for_comments (config, video_id);
				}
			};
		
		t.start();
		}
	
	public static void parse_youtube_comments_json (metadata config, String video_id, String data)
		{
		try
			{
			JSONObject json = new JSONObject (data);
			JSONObject feed = null;
			
			try
				{
				feed = json.getJSONObject ("feed");
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				Log.i ("vtest", "-- comments: JSON: no \"feed\" in JSON for: " + video_id);
				return;
				}
			
			JSONArray entries = null;
			try
				{
				entries = feed.getJSONArray ("entry");
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				Log.i ("vtest", "-- comments: JSON: no \"entry\" in JSON for: " + video_id);
				return;
				}
				
			if (entries.length() == 0)
				{
				Log.i ("vtest", "-- no comments for: " + video_id);
				return;
				}
			else
				Log.i ("vtest", "-- number of comments: " + entries.length());
			
			for (int i = 0; i < entries.length(); i++)
				{
				String title = null;
				String content = null;
				String author = null;
				String published = null;
				
				JSONObject entry = entries.getJSONObject (i);
				Log.i ("vtest", "entry: " + i + " of " + entries.length());
				
		 		JSONObject title_container = entry.getJSONObject ("title");
				title = title_container.getString ("$t");
				
		 		JSONObject content_container = entry.getJSONObject ("content");
				content = content_container.getString ("$t");
				
		 		JSONObject published_container = entry.getJSONObject ("published");
				published = published_container.getString ("$t");
				
				JSONArray author_array = entry.getJSONArray ("author");
				if (author_array.length() > 0)
					{
					JSONObject author0 = author_array.getJSONObject (0);
			 		JSONObject author_container = author0.getJSONObject ("name");
					author = author_container.getString ("$t");
					}
				
				Log.i ("vtest", "comments for #" + i);
				Log.i ("vtest", "-- title: " + title);
				Log.i ("vtest", "-- content: " + content);
				Log.i ("vtest", "-- author: " + author);
				Log.i ("vtest", "-- published: " + published);
				
				Comment c = new Comment();
				c.title = title;
				c.text = content;
				c.author = author;
				c.date = published;
				config.set_comment (video_id, Integer.toString(i), c);
				}
			}
		catch (JSONException e)
			{
			e.printStackTrace ();
			}
		}
	
	public static void gather_thumbnails_for_comments (metadata config, String video_id)
		{
		// https://gdata.youtube.com/feeds/api/users/ajIVLarCa3FMvlQwbrfFNA?prettyprint=true
		String maxcomment = config.program_meta (video_id, "maxcomment");
		if (maxcomment != null && !maxcomment.equals(""))
			{
			int num_comments = Integer.parseInt (maxcomment);
			for (int i = 0; i < num_comments; i++)
				{
				Comment c = config.get_comment (video_id, Integer.toString (i));
				}
			}
		}
	
	public static void fetch_youtube_channel_information (final metadata config, final String username)
		{
		String data = fetch_youtube_stats (username);
		if (data == null)
			{
			Log.i ("vtest", "youtube channel JSON data is null");
			return;
			}
		if (data.startsWith ("ERROR"))
			{
			Log.i ("vtest", "error in HTTP request");
			return;
			}
		Log.i ("vtest", "stats data starts with: " + data.substring(0,20));
		
		String title = null;
		String desc = null;
		String thumb = null;
		
		String channel_id = config.youtube_username_to_channel_id (username);
		
		try
			{
			JSONObject json = new JSONObject (data);
			JSONObject entry = null;
			try
				{
				entry = json.getJSONObject ("entry");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "stats: JSON: no \"entry\" in JSON for: " + username);
				return;
				}

	 		JSONObject title_container = entry.getJSONObject ("title");
			title = title_container.getString ("$t");
			
	 		JSONObject desc_container = entry.getJSONObject ("content");
			desc = desc_container.getString ("$t");

	 		JSONObject thumb_container = entry.getJSONObject ("media$thumbnail");
			thumb = thumb_container.getString ("url");
			}
		catch (JSONException e)
			{
			e.printStackTrace ();
			}
		
		Log.i ("vtest", "[stats for " + channel_id + "] name=" + title + ", thumb=" + thumb);
		
		if (title != null)
			{
			Log.i ("vtest", "+++++++++++++++++++++++++++++ CHANNEL " + channel_id + " name set to: " + title);
			config.set_channel_meta_by_id (channel_id, "name", title);
			}

		if (desc != null)
			config.set_channel_meta_by_id (channel_id, "desc", desc);
		
		if (thumb != null)
			config.set_channel_meta_by_id (channel_id, "thumb", thumb);
		}
	
	/* Originally, I had a loop to pop urls off the stack, and call Thread.currentThread().sleep (1000) between
	   iterations. But this behaved so poorly under Android that all posts to the UI thread would not get executed
	   until after ALL iterations were complete. */
	
	/* THAT IS WHY THIS PARTICULAR PART IS A MESS */
	
	static Timer batch_timer = null;
	
	static Lock batch_lock = new ReentrantLock();
	
	static metadata x_config = null;
	static Handler x_handler = null;
	
	static Runnable x_update_callback = null;
	static Runnable x_finished_callback = null;
	
	static Stack <String> x_urls = null;
	static Stack <String> x_titles = null;
	static Stack <String> x_thumbs = null;
	static Stack <String> x_ids = null;
	
	static class BatchTask extends TimerTask
   		{  
		public void run()
       		{
			boolean was_empty = true;
			
			String url = null;
			String username = null;
			String channel_id = null;
			String title = null;
			String thumb = null;
			String desc = null;
			String ytid = null;
			
			try
				{
				batch_lock.lock();
				
				was_empty = x_urls.empty();
				if (!was_empty)
					{
					url = x_urls.pop();
					username = url_to_youtube_username (url);
					channel_id = x_config.youtube_username_to_channel_id (username);
					title = x_titles.pop();
					thumb = x_thumbs.pop();
					ytid = x_ids.pop();
					desc = "";
					}
				}
			finally
				{
				batch_lock.unlock();
				}
			
			if (!was_empty)
				{
				Log.i ("vtest", "getting status from pool for channel: " + channel_id);
				String status = x_config.pool_meta (channel_id, "status");
				
				/* this is not a username, but a special youtube id, required to delete a subscription */
				x_config.set_channel_meta_by_id (channel_id, "ytid", ytid);
				
				if (status.equals ("2"))
					{
					Log.i ("vtest", "batch channel: #" + username);
					// x_config.add_channel (-1, "#" + username, title, desc, thumb, "0", "0", "0", "3", username);					
		
					fetch_and_parse_youtube (x_config, "virtual:following", "3", username);
					
					fetch_youtube_channel_information (x_config, username);
					
					String ponderosa = x_config.generate_ponderosa (username);
					
					post_ponderosa (x_handler, x_config, channel_id, ponderosa); 
					
					Log.i ("vtest", "PONDEROSA: " + ponderosa);
							
					x_handler.post (x_update_callback);
					}
				}
			else
				{
				Log.i ("vtest", "end of batch");
				batch_timer.cancel();
				
				post_basic_feed_information (x_config);

				batch_timer = null;
				x_handler.post (x_finished_callback);
				}
       		}  
  		}
	
	/* for any channel just crawled, submit channel information to server */
	
	public static void post_basic_feed_information (metadata config)
		{
		String payload = "";
		
		Stack <String> twos = config.channels_with_status ("2");				
		while (!twos.empty())
			{
			String id = twos.pop();
			
			String youtube_username = "";
			
			String name  = config.pool_meta (id, "name");
			String desc  = config.pool_meta (id, "desc");
			String thumb = config.pool_meta (id, "thumb");
			
			String thumb1  = "";
			String thumb2  = "";
			String thumb3  = "";
			String content = "";
			
			if (name == null) { name = ""; }
			if (desc == null) { desc = ""; }
			
			name = util.encodeURIComponent (name);
			desc = util.encodeURIComponent (desc);
			
			// from python on channelwatch:
			// print >>out, id + '\t' + ch ['youtube'] + '\t' + ret ['title'] + '\t' + ret ['thumb'] + '\t' + thumbs[1] + '\t'
			// + thumbs[2] + '\t' + thumbs[3] + '\t' + urllib.quote_plus (ret ['desc'].encode ("utf-8")) + '\t'
			// + urllib.quote_plus (ret ['content'].encode ("utf-8")) + '\t' + update_time + '\t' + str(pcount) + '\t' + last_episode_title
			
			String line = id + "\t" + youtube_username + "\t" + name + "\t" + thumb + "\t" 
			         + thumb1 + "\t" + thumb2 + "\t" + thumb3 + "\t" + desc + "\t" + content;
			
			payload += line + "\n";
			}
		
		if (payload.equals (""))
			{
			Log.i ("vtest", "basic feed: server already up to date");
			return;
			}
		
		Log.i ("vtest", "channelUpdate payload: " + payload);
		
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost ("http://" + x_config.api_server + "/playerAPI/channelUpdate");

		ResponseHandler <String> response_handler = new BasicResponseHandler ();
		
	    try
	    	{
	        List <NameValuePair> kv = new ArrayList <NameValuePair> (3);
	        kv.add (new BasicNameValuePair ("user", x_config.usertoken));
	        kv.add (new BasicNameValuePair ("payload", payload));
	        httppost.setEntity (new UrlEncodedFormEntity (kv));

	        String response = httpclient.execute (httppost, response_handler);
	        
	        Log.i ("vtest", "channelUpdate response: " + response);
	    	}
	    catch (Exception ex)
	    	{
	    	}
		}
	
	public static void post_ponderosa (Handler h, metadata config, final String channel_id, final String ponderosa)
		{
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost ("http://" + config.api_server + "/playerAPI/virtualChannelAdd");

		ResponseHandler <String> response_handler = new BasicResponseHandler ();
		
	    try
	    	{
	        List <NameValuePair> kv = new ArrayList <NameValuePair> (3);
	        kv.add (new BasicNameValuePair ("channel", channel_id));
	        kv.add (new BasicNameValuePair ("user", config.usertoken));
	        kv.add (new BasicNameValuePair ("payload", ponderosa));
	        httppost.setEntity (new UrlEncodedFormEntity (kv));

	        String response = httpclient.execute (httppost, response_handler);
	        
	        Log.i ("vtest", "ponderosa response: " + response);
	    	}
	    catch (Exception ex)
	    	{
	    	}
		}

	public static void load_batch_of_youtube_channels
			(final metadata m, final Stack <String> urls, final Stack <String> titles, final Stack <String> thumbs, final Stack <String> ids, 
					final Handler h, final Runnable light_refresh, final Runnable finished)
		{
		Log.i ("vtest", "load_batch_of_youtube_channels");		
		Thread t = new Thread()
			{
			public void run()
				{
				x_config = m;
				x_handler = h;
				
				x_update_callback = light_refresh;
				x_finished_callback = finished;
				
				x_urls = urls;
				x_titles = titles;
				x_thumbs = thumbs;
				x_ids = ids;
				
				if (batch_timer == null)
					{
					Log.i ("vtest", "creating batch timer");
					batch_timer = new Timer();
					batch_timer.scheduleAtFixedRate (new BatchTask(), 1000, 500);
					}
				else
					Log.i ("vtest", "batch timer is not null! will not start another one.");
				}
			};
			
		t.run();
		}
	
	public static void OLD_load_batch_of_youtube_channels
			(final metadata m, final Stack <String> urls, final Stack <String> titles, final Stack <String> thumbs, 
					final Handler h, final Callback callback, final Runnable finished)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				batch_timer = new Timer();
				batch_timer.scheduleAtFixedRate (new BatchTask(), 1000, 500);
				int count = 0;
				
				Log.i ("vtest", "Begin loading batch");
				while (!urls.empty())
		        	{
					final String url = urls.pop();
					final String username = url_to_youtube_username (url);
					
					String channel_id = m.youtube_username_to_channel_id (username);
					String title = titles.pop();
					String thumb = thumbs.pop();
					String desc = "";
					
					if (channel_id != null)
						{
						m.add_channel (++count, channel_id, title, desc, thumb, "0", "0", "0", "3", username);					
	
						fetch_and_parse_youtube (m, channel_id, "3", username);
						
						h.post (new Runnable()
							{
							public void run()
								{
								if (callback != null)
									callback.run_string (username);
								}
							});
						
						try
							{
							Log.i ("vtest", "load_batch_of_youtube_channels: sleep 1000");
							Thread.currentThread().sleep (1000);
							}
						catch (InterruptedException ex)
							{
							ex.printStackTrace();
							}
						}
		        	}
				Log.i ("vtest", "End loading batch");

				h.post (finished);
				}
			};
			
		t.run();
		}
	
	public static String url_to_youtube_username (String url)
		{
		if (url.contains ("watch"))
			{
			int voffset = url.indexOf ("?v=");
			if (voffset > 0)
				url = url.substring (voffset + 3, voffset + 3 + 11);
			}
		else
			{
			url = url.replaceAll ("/uploads$", "");
			url = url.replaceAll ("^https?://gdata.youtube.com/feeds/api/(users|videos)/", "");
			}
		return url;
		}
	
	public static void subscribe_on_youtube (metadata m, String youtube_username)
		{
		String xml = "";
		
		if (m.youtube_auth_token == null)
			return;
				
		Log.i ("vtest", "subscribe on YouTube: " + youtube_username);
		
		xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";		
		xml += "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:yt=\"http://gdata.youtube.com/schemas/2007\">";
		xml += "  <category scheme=\"http://gdata.youtube.com/schemas/2007/subscriptiontypes.cat\" term=\"channel\"/>";
		xml += "  <yt:username>" + youtube_username + "</yt:username>";
		xml += "</entry>";
		
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost ("https://gdata.youtube.com/feeds/api/users/default/subscriptions");
	    
	    StringEntity se = null;
	    try
	    	{
			se = new StringEntity (xml, HTTP.UTF_8);
	    	}
		catch (UnsupportedEncodingException ex)
			{
			ex.printStackTrace();
			return;
			}
	    
	    httppost.setHeader ("Content-Type", "application/atom+xml");
	    httppost.setHeader ("Authorization", "Bearer " + m.youtube_auth_token);
	    httppost.setHeader ("GData-Version", "2");
		httppost.setHeader ("X-GData-Key", "key=AI39si5HrNx2gxiCnGFlICK4Bz0YPYzGDBdJHfZQnf-fClL2i7H_A6Fxz6arDBriAMmnUayBoxs963QLxfo-5dLCO9PCX-DTrA");
		
	    httppost.setEntity (se);
	    
		ResponseHandler <String> response_handler = new BasicResponseHandler();
		
		String response = null;
	    try
	    	{
	        response = httpclient.execute (httppost, response_handler);
	        Log.i ("vtest", "subscribe on youtube response: " + response);
	    	}
	    catch (Exception ex)
	    	{
			ex.printStackTrace();	    
			try
				{
				String errtext = EntityUtils.toString (httppost.getEntity());
				Log.i ("vtest", "POST ERROR: " + errtext);
				}
			catch (Exception ex2)
				{
				ex2.printStackTrace();
				}
	    	}
		}
	
	public static void delete_on_youtube (metadata m, String youtube_username)
		{		
		if (m.youtube_auth_token == null)
			return;
		
		Log.i ("vtest", "delete on YouTube: " + youtube_username);
		
		String channel_id = m.youtube_username_to_channel_id (youtube_username);
		String ytid = m.pool_meta (channel_id, "ytid");
			
		if (ytid == null || ytid.equals (""))
			{
			Log.i ("vtest", "ytid not known for channel: " + youtube_username);
			return;
			}
		
	    HttpClient httpclient = new DefaultHttpClient();
	    
	    /* send the ytid and not the username! weird */
	    String unsub_url = "https://gdata.youtube.com/feeds/api/users/default/subscriptions/" + ytid;
	    Log.i ("vtest", "unsub url: " + unsub_url);
	    HttpDelete httpdelete = new HttpDelete (unsub_url);
	    
	    httpdelete.setHeader ("Content-Type", "application/atom+xml"); /* example has this but why? */
	    httpdelete.setHeader ("Authorization", "Bearer " + m.youtube_auth_token);
	    httpdelete.setHeader ("GData-Version", "2");
		httpdelete.setHeader ("X-GData-Key", "key=AI39si5HrNx2gxiCnGFlICK4Bz0YPYzGDBdJHfZQnf-fClL2i7H_A6Fxz6arDBriAMmnUayBoxs963QLxfo-5dLCO9PCX-DTrA");
	    
		ResponseHandler <String> response_handler = new BasicResponseHandler();
		
		String response = null;
	    try
	    	{
	        response = httpclient.execute (httpdelete, response_handler);
	        Log.i ("vtest", "channelUpdate response: " + response);
	    	}
	    catch (Exception ex)
	    	{
			ex.printStackTrace();	    
			}
		}
	}