package tv.tv9x9.player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Livestream
	{
	public static void log (String text)
		{
		Log.i ("vtest", text);
		}
	
	public static void fetch_livestream_url_in_thread
			(final metadata config, final String episode_id, final Handler handler, final Callback update)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				String url = config.best_url_or_first_subepisode (episode_id);
				if (url.contains (";"))
					{
					String fields[] = url.split (";");
					url = fields [0];
					}
				log (">>>>>>>>>>> livestream " + episode_id + ": " + url);
				String new_url = get_livestream_url (url);
				log (">>>>>>>>>>> livestream new url: " + new_url);
				if (new_url != null)
					config.set_main_episode_url (episode_id, new_url);
				
				if (update != null)
					handler.post (new Runnable()
						{
						public void run()
							{
							if (update != null)
								update.run();
							}
						});
				}
			};
		
		t.start();
		}
	
	public static String get_livestream_url (String url)
		{
		String data = futil.get_any_webfile (url, null, null);
		
		String json_string = null;
		
		Pattern pattern = Pattern.compile ("<script type=\"text/javascript\">window.config = ([^;]*);</script>");
		
		while (true)
			{
			Matcher matcher = pattern.matcher (data);
			if (matcher.find (0))
				{
				// int matchStart = matcher.start(1);
				// int matchEnd = matcher.end();
				json_string = matcher.group (1);
				break;
				}
			else
				break;
			}
				
		/*
		 * x.event.stream_info.m3u8_url
"http://api.new.livestream.com/broadcasts/62953295.m3u8?dw=100&hdnea=st=1411…13/*~hmac=8f9932ea9e1d8b01044ffc4ed0e2e2e11bba37c2cee4460106b5e02cd59326e2"
		 */
		
		if (json_string != null)
			{
			log ("found JSON string: " + json_string);
			try
				{
				JSONObject json = new JSONObject (json_string);
				JSONObject event = json.getJSONObject ("event");
				JSONObject stream_info = event.getJSONObject ("stream_info");
				String m3u8_url = stream_info.getString ("m3u8_url");
				return m3u8_url;
				}
			catch (JSONException ex)
				{
				ex.printStackTrace();
				}
			}
		else
			log ("no JSON match for livestream url: " + url);
		return null;
		}
	}
