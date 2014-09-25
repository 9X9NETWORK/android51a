package tv.tv9x9.player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProtectedUrl
	{
	public static void log (String text)
		{
		Log.i ("vtest", text);
		}
	
	public static void fetch_protected_url_in_thread
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
				log (">>>>>>>>>>> protected " + episode_id + ": " + url);
				
				// String query = "generateSignedUrl?url=" + util.encodeURIComponent (url);
				String query = "generateSignedUrls?url=" + url;
	    		new playerAPI (handler, config, query)
					{
					public void success (String[] lines)
						{
						String new_url = lines [0];
						config.set_main_episode_url (episode_id, new_url);
						log (">>>>>>>>>>> protected new url: " + new_url);
						/* change away from type 5 so this isn't called again */
						config.set_program_meta (episode_id, "type", "0");
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
					public void failure (int code, String errtext)
						{
						}
					};
				}
			};
		
		t.start();
		}
	}
