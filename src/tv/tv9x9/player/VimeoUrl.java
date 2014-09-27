package tv.tv9x9.player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VimeoUrl
	{
	public static void log (String text)
		{
		Log.i ("vtest", text);
		}
	
	public static void fetch_vimeo_url_in_thread
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
				log (">>>>>>>>>>> vimeo " + episode_id + ": " + url);
				String new_url = get_vimeo_url (url);
				log (">>>>>>>>>>> vimeo new url: " + new_url);
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
	
	public static String get_vimeo_url (String url)
		{
		String data = futil.get_any_webfile (url, null, null);
		
		String vimeo_api_url = null;
		
		Pattern pattern = Pattern.compile ("data-config-url=\"([^\"]*)\"");
		
		while (true)
			{
			Matcher matcher = pattern.matcher (data);
			if (matcher.find (0))
				{
				// int matchStart = matcher.start(1);
				// int matchEnd = matcher.end();
				vimeo_api_url = matcher.group (1);
				break;
				}
			else
				break;
			}
			
		String json_string = null;
		
		if (vimeo_api_url != null)
			{
			vimeo_api_url = vimeo_api_url.replaceAll ("&amp;", "&");
			json_string = futil.get_any_webfile (vimeo_api_url, null, null);
			}
		else
			return null;
		
		// x.request.files.h264.mobile.url
		
		if (json_string != null)
			{
			log ("found JSON string: " + json_string);
			try
				{
				JSONObject json = new JSONObject (json_string);
				JSONObject request = json.getJSONObject ("request");
				JSONObject files = request.getJSONObject ("files");
				JSONObject h264 = files.getJSONObject ("h264");
				JSONObject mobile = h264.getJSONObject ("mobile");
				String vimeo_url = mobile.getString ("url");
				return vimeo_url;
				}
			catch (JSONException ex)
				{
				ex.printStackTrace();
				}
			}
		else
			log ("no JSON match for vimeo url: " + url);
		return null;
		}
	}