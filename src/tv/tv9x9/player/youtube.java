package tv.tv9x9.player;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class youtube
	{
	public static void youtube_url (final String video_id, final Callback callback)
		{
		Thread t = new Thread ()
			{
				public void run ()
					{
					String video_token = null;
					String fmt_url_map = null;
					String best_url = null;

					// String url =
					// "http://www.youtube.com/get_video_info?&video_id=" +
					// video_id + "&el=embedded&ps=default&eurl=&gl=US&hl=en";
					String url = "http://www.youtube.com/get_video_info?video_id=" + video_id;

					String p = futil.get_universal_webfile (url);
					if (p.startsWith ("ERROR:"))
						return;

					String fields[] = p.split ("&");
					for (String field : fields)
						{
						Log.i ("vtest", "YOUTUBE QUERY FIELD: " + field);
						String ab[] = field.split ("=");
						if (ab[0].equals ("url_encoded_fmt_stream_map"))
							{
							fmt_url_map = ab[1];
							try
								{
								fmt_url_map = URLDecoder.decode (ab[1], "UTF-8");
								}
							catch (UnsupportedEncodingException e)
								{
								// TODO Auto-generated catch block
								e.printStackTrace ();
								}
							}
						}

					if (fmt_url_map != null)
						{
						// mp4: 18 22 37 38

						String mappings[] = fmt_url_map.split (",");
						for (String mapped : mappings)
							{
							String decoded_map = null;
							Log.i ("vtest", "MAPPING " + mapped);
							try
								{
								decoded_map = URLDecoder.decode (mapped, "UTF-8");
								int b2 = decoded_map.indexOf ("; ");
								if (b2 > 0)
									{
									decoded_map = decoded_map.substring (4, b2);
									Log.i ("vtest", "MAPPING DECODED: " + decoded_map);
									// video/3gp
									if (decoded_map.indexOf ("type=video/mp4") > 0)
										best_url = decoded_map;
									}
								}
							catch (UnsupportedEncodingException e)
								{
								// TODO Auto-generated catch block
								e.printStackTrace ();
								}
							if (false)
								{
								String aabb[] = mapped.split ("\\|");
								Log.i ("vtest", "MAPPING " + aabb[0] + ": " + aabb[1]);
								if (aabb[0].equals ("18") || aabb[0].equals ("22") || aabb[0].equals ("37") || aabb[0].equals ("38"))
									{
									best_url = aabb[1];
									break;
									}
								}
							}
						}

					if (best_url != null)
						callback.run_string (best_url);
					}
			};

		t.start ();
		}
	}