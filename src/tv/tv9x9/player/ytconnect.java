package tv.tv9x9.player;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Stack;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class ytconnect
	{
	metadata m = null;
	
	AccountManager am = null;
	Account[] accounts = null;
		
	Context saved_context = null;
	Handler saved_handler = null;
	
	Runnable saved_yt_callback_1 = null; /* yt_connect_loading */
	Runnable saved_yt_callback_2 = null; /* yt_finished_loading */
	Runnable saved_yt_callback_3 = null; /* launch_add_account */
	Runnable saved_yt_callback_4 = null; /* yt_connect_error */
	Runnable saved_yt_callback_5 = null; /* yt_connect_refresh */
	Runnable saved_yt_callback_6 = null; /* yt_connect_light_refresh */
	
	Account chosen_account = null;
	
	Stack <String> urls   = new Stack <String> ();
	Stack <String> titles = new Stack <String> ();
	Stack <String> thumbs = new Stack <String> ();
	Stack <String> ids    = new Stack <String> ();
	
	ytconnect (metadata config, Context ctx)
		{
		m = config;
		saved_context = ctx;
		};
		
	public void oauth2_stuff (Handler h)
		{
		Log.i ("vtest", "OAUTH2 STUFF");
		
	    saved_handler = h;
	    
	    am = AccountManager.get (saved_context);
	    accounts = am.getAccountsByType ("com.google");
	    
	    /* make sure the cast from ctx to Activity is safe -- always call this function with Activity context not a local context */
	    if (! (saved_context instanceof Activity))
	    	{
	    	Log.i ("vtest", "ASSERT: context is not an instance of activity");
	    	return;
	    	}

	    h.post (select_account);
		}
	
	public void set_callback_1 (Runnable r)
		{
		saved_yt_callback_1 = r;
		}
	public void set_callback_2 (Runnable r)
		{
		saved_yt_callback_2 = r;
		}
	public void set_callback_3 (Runnable r)
		{
		saved_yt_callback_3 = r;
		}	
	public void set_callback_4 (Runnable r)
		{
		saved_yt_callback_4 = r;
		}	
	public void set_callback_5 (Runnable r)
		{
		saved_yt_callback_5 = r;
		}	
	public void set_callback_6 (Runnable r)
		{
		saved_yt_callback_6 = r;
		}	
	
	final Runnable select_account = new Runnable()
		{
		public void run()
			{
			if (accounts == null)
				{
				AlertDialog.Builder builder = new AlertDialog.Builder (saved_context);

				builder.setMessage ("No accounts!");
				builder.setNeutralButton ("OK", null);

				builder.create().show();
				return;
				}
			
			String txt_add = saved_context.getResources().getString (R.string.add_account);
			String txt_select = saved_context.getResources().getString (R.string.select_youtube_account);
			
			final CharSequence[] items = new CharSequence [accounts.length + 1];
			for (int i = 0; i < accounts.length; i++)
				items [i] = (CharSequence) accounts [i].name;
			items [accounts.length] = txt_add;
			
			AlertDialog.Builder builder = new AlertDialog.Builder (saved_context);
			
			builder.setTitle (txt_select);
			builder.setItems (items, new DialogInterface.OnClickListener()
				{
				@Override
				public void onClick (DialogInterface arg0, int arg1)
					{
					if (arg1 < accounts.length)
						{
						chosen_account = accounts [arg1];
						Log.i ("vtest", "picked: " + chosen_account);
			    		m.youtube_account_name = chosen_account.name;
						get_token_for (chosen_account);
						}
					else
						{
						/* add an account by launching Android Settings program */
						saved_handler.post (saved_yt_callback_3);
						}
					}
				});
	
			builder.create().show();
			}
		};
	
	public void use_saved_youtube_account (Handler h, String email)
		{
	    saved_handler = h;
	    
	    am = AccountManager.get (saved_context);
	    accounts = am.getAccountsByType ("com.google");
	    
	    for (Account account: accounts)
	    	{
	    	if (account.name.equals (email))
	    		{
	    		Log.i ("vtest", "using saved account: " + account.name);
	    		chosen_account = account;
	    		m.youtube_account_name = account.name;
				get_token_for (chosen_account);
	    		}
	    	}
		}
	
	public void get_token_for (Account account)
		{
	    Bundle options = new Bundle();
	    
	    // String scope = "Manage your tasks";
	    // String scope = "http://gdata.youtube.com";
	    // String scope = "https://www.googleapis.com/auth/youtube";
	    String scope = "oauth2:https://gdata.youtube.com";
	    
	    am.getAuthToken
	    	(
	        account, 	                 // Account retrieved using getAccountsByType()
	        scope,				         // Auth scope
	        options,                     // Authenticator-specific options
	        (Activity) saved_context,    // Your activity
	        new OnTokenAcquired(),       // Callback called when a token is successfully acquired
	        new Handler (new OnError())  // Callback called if an error occurs
	    	);
		}
	
	private class OnTokenAcquired implements AccountManagerCallback <Bundle>
		{
		@Override
		public void run (AccountManagerFuture <Bundle> result)
			{
	        try
	        	{
				Bundle bundle = result.getResult();
				String token = bundle.getString (AccountManager.KEY_AUTHTOKEN);
				Log.i ("vtest", "OAUTH2 GOT TOKEN: " + token);
				m.youtube_auth_token = token;
				get_youtube_subscriptions();
	        	}
	        catch (Exception ex)
	        	{
				ex.printStackTrace();
	        	}
			}
		}

	private class OnError implements Handler.Callback
		{
		@Override
	    public boolean handleMessage (Message message)
	    	{
	        Log.i ("vtest", "OAUTH2 ERROR: " + message.toString());
	        return true;
	    	}
		}

	public void get_youtube_subscriptions()
		{
		saved_handler.post (saved_yt_callback_1);
		}
	
	public void get_youtube_subscriptions_inner()
		{
		Thread t = new Thread()
			{
			public void run()
				{
				String url = "https://gdata.youtube.com/feeds/api/users/default/subscriptions?prettyprint=true&alt=json&max-results=50&v=2";
				
				String[] header_keys = { "Authorization" };
				String[] header_values = { "Bearer " + m.youtube_auth_token };
				
				Log.i ("vtest", "getting subscription list from YouTube");
	
				while (true)
					{
					Log.i ("vtest", "[ytconnect] SUBSCRIPTION URL: " + url);
					
					String result = futil.get_any_webfile (null, url, header_keys, header_values);
					
					if (result.startsWith ("ERROR:"))
						{
						send_up_error (nice_error (result));
						return;
						}
					
					url = parse_youtube_subscriptions (result);
					if (url == null)
						break;
					}
				
				main_startup_sequence();
				}
			};
		
		t.start();
		}
	
	public String nice_error (String text)
		{
		if (text.startsWith ("ERROR:"))
			{
			if (text.contains ("NoLinkedYouTubeAccount"))
				{
				// org.apache.http.client.HttpResponseException: NoLinkedYouTubeAccount
				// return "That account is not linked to YouTube!";
				String txt_not_linked = saved_context.getResources().getString (R.string.not_linked);
				return txt_not_linked;
				}
			else if (text.contains ("Token invalid"))
				{
				// org.apache.http.client.HttpResponseException: Token invalid
				String txt_account_invalid = saved_context.getResources().getString (R.string.account_invalid);
				return txt_account_invalid;
				}
			}
		return text;
		}
	
	public void send_up_error (final String message)
		{
		/* first send the error message text, using this convoluted way, which is static */
		saved_handler.post (new Runnable()
			{
			public void run()
				{
				youtube_batch_set_error_text.run_string (message);
				}
			});
		/* next initiate callback in non-static mode */
		saved_handler.post (saved_yt_callback_4);
		}
	
	public String parse_youtube_subscriptions (String data)
		{
		String next_href = null;
		
		Log.i ("vtest", "parse youtube subscriptions");

		if (data == null)
			{
			Log.i ("vtest", "youtube channel JSON data is null");
			return null;
			}
		
		if (data.startsWith ("ERROR"))
			{
			Log.i ("vtest", "error in HTTP request");
			return null;
			}

		Log.i ("vtest", "data starts with: " + data.substring (0,20));
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
				Log.i ("vtest", "JSON: no feed object!");
				return null;
				}
			
			JSONArray links = null;
			try
				{
				links = dataObject.getJSONArray ("link");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no links!");
				}
				
			/* if there is continuation */
			if (links != null)
				{
				for (int i = 0; i < links.length(); i++)
					{
					// Log.i ("vtest", "link: " + i + " of " + links.length());
					JSONObject link = links.getJSONObject (i);
					String rel = link.getString ("rel");
					if (rel.equals ("next"))
						next_href = link.getString ("href");
					}
				}
			
			JSONArray entries = null;
			try
				{
				entries = dataObject.getJSONArray ("entry");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no entries!");
				main_startup_sequence();
				return null;
				}
			
			for (int i = 0; i < entries.length(); i++)
				{
				// Log.i ("vtest", "entry: " + i + " of " + entries.length());
				    	 
				try
					{
					JSONObject entry = entries.getJSONObject (i);
	
					JSONObject title_container = entry.getJSONObject ("title");
					String title = title_container.getString ("$t");
					
					JSONArray link_container = entry.getJSONArray ("link");
					JSONObject first_link = link_container.getJSONObject (0);
					String href = first_link.getString ("href");
					
					JSONObject thumb_container = entry.getJSONObject ("media$thumbnail");
					String thumb = thumb_container.getString ("url");
					
					JSONObject id_container = entry.getJSONObject ("id");
					String ytid = id_container.getString ("$t");
					ytid = ytid.substring (ytid.lastIndexOf (":") + 1);
					
					Log.i ("vtest", title + " :: " + href + " :: " + ytid);
					
					title = title.replaceAll ("^Activity of: ", "");
					
					href = href.replaceAll ("\\?.*$", "");
					
					urls.push (href);
					titles.push (title);
					thumbs.push (thumb);
					ids.push (ytid);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}			
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		return next_href;
		}
	
	final Callback youtube_batch_update = new Callback()
		{
		@Override
		public void run_string (String arg)
			{
			Log.i ("vtest", "YOUTUBE BATCH UPDATE: " + arg);
			// TODO: Below two lines need to be reconnected!
			// home.yt_connect_update r = new home.yt_connect_update (arg); 
			// saved_handler.post (r);
			}
		};
		
	final Callback youtube_batch_set_error_text = new Callback()
		{
		@Override
		public void run_string (String arg)
			{
			Log.i ("vtest", "YOUTUBE BATCH UPDATE: " + arg);
			// TODO: Below two lines need to be reconnected!
			// home.yt_connect_set_error_text r = new home.yt_connect_set_error_text (arg);
			// saved_handler.post (r);
			}
		};

	/* at this point we just have a list of subscriptions, but no IDs */
		
	public void main_startup_sequence()
		{
		Log.i ("vtest", "[ytconnect] main_startup_sequence");
		
		m.youtube_9x9_mirror_created_this_session = false;
		
		Log.i ("vtest", "chosen account: " + chosen_account.toString());
		Log.i ("vtest", "chosen account name: " + chosen_account.name);
		
		final String user_9x9 = chosen_account.name.replaceAll ("@", "-AT-") + "@" + "9x9.tv";		
		// final String user_9x9 = "DELETEME-14-" + chosen_account.name.replaceAll ("@", "-AT-") + "@" + "9x9.tv";		
		final String password = util.md5 (user_9x9 + "/\\oo/\\");
		
		String u = futil.read_file (saved_context, "user@" + m.api_server);
		if (!u.startsWith ("ERROR:"))
			{
			m.usertoken = u;
			Log.i ("vtest", "[ytconnect] GOT SAVED TOKEN: " + m.usertoken);
			main_startup_sequence_ii();
			}
		else
			{
			Log.i ("vtest", "[ytconnect] obtain account name");
			String query = "obtainAccount?name=" + user_9x9 + "&email=" + user_9x9 + "&password=" + password;
			new playerAPI (saved_handler, m, query)
				{
				public void success (String[] lines)
					{
					m.usertoken = null;
					m.username = chosen_account.name;
					
					for (String line: lines)
						{
						Log.i ("vtest", "obtainAccount text: " + line);
						String[] fields = line.split ("\t");
						if (fields[0].equals ("token"))
							{
							m.usertoken = fields[1];
							Log.i ("vtest", "YouTube Connect obtained 9x9 account: " + m.usertoken);
							futil.write_file (saved_context, "user@" + m.api_server, m.usertoken);
							futil.write_file (saved_context, "name@" + m.api_server, "YOUTUBE::" + m.youtube_account_name);
							}
						else if (fields[0].equals ("created") && fields[1].equals ("0"))
							{
							m.youtube_9x9_mirror_created_this_session = true;
							Log.i ("vtest", "This account has never been setup for 9x9");
							}
						}
					if (m.usertoken != null)
						main_startup_sequence_ii();
					}
		
				public void failure (int code, String errtext)
					{
					Log.i ("vtest", "[ytconnect] obtainAccount failure: " + errtext);
					}
				};
			}
		}
	
	/* at this point we have the list of subscriptions, and a 9x9 usertoken */
	
	public void main_startup_sequence_ii()
		{
		String all = "";
		for (String url: urls)
			{
			String username = ytchannel.url_to_youtube_username (url);
			if (!all.equals (""))
				all += ",";
			all += username;
			}
		
		m.set_all_channel_meta ("subscribed", "0");
		
		String query = null;
		if (m.youtube_9x9_mirror_created_this_session)
			query = "bulkSubscribe?user=" + m.usertoken + "&channelNames=" + all;
		else
			query = "bulkIdentifier?channelNames=" + all;
		
		new playerAPI (saved_handler, m, query)
			{
			public void success (String[] lines)
				{
				int count = 0;
				for (String line: lines)
					{
					Log.i ("vtest", "BULK IDENTIFY: " + line);
					String channel_id = m.parse_virtual_channel_line (++count, "virtual:following", line);
					m.set_channel_meta_by_id (channel_id, "subscribed", "1"); 
					}
				main_startup_sequence_iii();
				}
			public void failure (int code, String errtext)
				{
				Log.i ("vtest", "[ytconnect] bulk failure: " + errtext);
				}
			};
		}
	
	/* at this point we have subscriptions including their 9x9 IDs, and a 9x9 usertoken */
	
	public void main_startup_sequence_iii()
		{
		/* continue main sequence, by getting YouTube What's New */
		main_startup_sequence_iv();
		
		/* background processing */
		pre_process_batch_inner();
		}
	
	public void main_startup_sequence_iv()
		{
		String url = "https://gdata.youtube.com/feeds/api/users/default/newsubscriptionvideos?alt=json&prettyprint=true&max-results=50";
		
		String[] header_keys = { "Authorization" };
		String[] header_values = { "Bearer " + m.youtube_auth_token };
		
		Log.i ("vtest", "getting whatsnew feed from YouTube");
		
		String result = futil.get_any_webfile (null, url, header_keys, header_values);
		
		if (result.startsWith ("ERROR:"))
			send_up_error (nice_error (result));
		else
			main_startup_sequence_v (result);
		}
	
	public void main_startup_sequence_v (String data)
		{
		parse_youtube_whatsnew (data);
		saved_handler.post (saved_yt_callback_2);
		}
	
	public void pre_process_batch_inner()
		{
		if (urls.empty())
			{
			Log.i ("vtest", "pre_process_batch_inner: no subscriptions");
			}
		
		String all = "";
		for (String url: urls)
			{
			String username = ytchannel.url_to_youtube_username (url);
			if (!all.equals (""))
				all += ",";
			all += username;
			}
		
		Log.i ("vtest", "[ytconnect] bulk subscribe");
		
		// m.set_all_channel_meta ("subscribed", "0");
		
		String query = "bulkSubscribe?user=" + m.usertoken + "&channelNames=" + all;
		new playerAPI (saved_handler, m, query)
			{
			public void success (String[] lines)
				{
				Stack <String> twos = m.channels_with_status ("2");
				if (twos.empty())
					{
					Log.i ("vtest", "pre_process_batch_inner: batch already up to date");
					/* even if they are up to date, we need their ytid's */
					while (!urls.empty())
						{
						String url = urls.pop();
						String ytid = ids.pop();
						String username = ytchannel.url_to_youtube_username (url);
						String channel_id = m.youtube_username_to_channel_id (username);
						m.set_channel_meta_by_id (channel_id, "ytid", ytid);
						m.set_channel_meta_by_id (channel_id, "subscribed", "1");
						}
					}
				else
					ytchannel.load_batch_of_youtube_channels (m, urls, titles, thumbs, ids, saved_handler, saved_yt_callback_6, saved_yt_callback_5);
				}
			
			public void failure (int code, String errtext)
				{
				}
			};			
			
		Log.i ("vtest", "** ALL **: " + all);
		}	
	
	public void parse_youtube_whatsnew (String data)
		{
		Log.i ("vtest", "parse youtube whatsnew feed");
		
		if (data == null)
			{
			Log.i ("vtest", "youtube whatsnew feed JSON data is null");
			return;
			}
		
		if (data.startsWith ("ERROR:"))
			{
			Log.i ("vtest", "error in HTTP request");
			return;
			}
		
		Log.i ("vtest", "data starts with: " + data.substring (0,20));
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
				Log.i ("vtest", "JSON: no feed object!");
				return;
				}
			
			JSONArray entries = null;
			try
				{
				entries = dataObject.getJSONArray ("entry");
				}
			catch (Exception ex)
				{
				Log.i ("vtest", "JSON: no entries!");
				return;
				}
			
			for (int i = 0; i < entries.length(); i++)
				{
				// Log.i ("vtest", "entry: " + i + " of " + entries.length());
				    	 
				try
					{
					String fields[];
					
					JSONObject entry = entries.getJSONObject (i);
	
					// JSONObject id_container = entry.getJSONObject ("id");
					// String id = id_container.getString ("$t");
					// fields = id.split ("/");
					// String video_id = fields [fields.length - 1];
					
					JSONObject title_container = entry.getJSONObject ("title");
					String title = title_container.getString ("$t");
					
					JSONObject published_container = entry.getJSONObject ("published");
					String published = published_container.getString ("$t");
					
					JSONObject content_container = entry.getJSONObject ("content");
					String desc = title_container.getString ("$t");
					
					JSONArray link_container = entry.getJSONArray ("link");
					JSONObject first_link = link_container.getJSONObject (0);
					String href = first_link.getString ("href");
					String video_id = ytchannel.url_to_youtube_username (href);
					
					JSONArray author_container = entry.getJSONArray ("author");
					JSONObject first_author = author_container.getJSONObject (0);
					String author_name = first_author.getString ("name");
					JSONObject author_uri_container = first_author.getJSONObject ("uri");
					String author_uri = author_uri_container.getString ("$t");
					fields = author_uri.split ("/");
					String author_username = fields [fields.length - 1].toLowerCase();
					
					JSONObject media_group = entry.getJSONObject ("media$group");
					
					JSONArray thumb_container = media_group.getJSONArray ("media$thumbnail");
					// JSONObject first_thumb = thumb_container.getJSONObject (thumb_container.length() > 1 ? 1 : 0);
					JSONObject first_thumb = thumb_container.getJSONObject (0);
					String thumb = first_thumb.getString ("url");
					
					JSONObject duration_container = media_group.getJSONObject ("yt$duration");
					String duration = duration_container.getString ("seconds");
					
					/* example 2012-11-11T05:57:15.000Z -- SimpleDateFormat is not documented as working with "Z" type timezones */
					published = published.replace ("Z", "-0000");
					
					String timestamp;
	                SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	                try
	                	{
						Date dt = sdf.parse (published);
						timestamp = "" + dt.getTime();
	                	}
	                catch (ParseException e1)
	                	{
						timestamp = "";
	                	}
	                
					Log.i ("vtest", "[whatsnew] " + author_username + " :: " + video_id + " :: " + title + " :: " + timestamp);

					String channel_id = m.youtube_username_to_channel_id (author_username);
					if (channel_id == null)
						{
						Log.i ("vtest", "YouTube username \"" + author_username + "\" cannot be resolved to a 9x9 channel id -- skipped");
						continue;
						}
				
					Hashtable <String, String> program = new Hashtable <String, String> ();
					
					program.put ("sort", Integer.toString(i));
					program.put ("id", "following" + ":" + video_id);
					program.put ("channel", "virtual:following");
					program.put ("real_channel", channel_id);
					program.put ("name", title);
					program.put ("desc", desc);
					program.put ("thumb", thumb);
					program.put ("url1", href);
					program.put ("url2", "");
					program.put ("url3", "");
					program.put ("url4", "");
					program.put ("timestamp", timestamp);
					program.put ("duration", duration);
					
					m.add_programs_from_youtube (program);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}	
	}