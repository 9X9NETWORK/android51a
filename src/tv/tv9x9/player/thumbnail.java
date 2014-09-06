package tv.tv9x9.player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class thumbnail
	{
	public static Bitmap getImageBitmap (String url)
		{
		Bitmap bm = null;
		try
			{
			URL aURL = new URL (url);
			URLConnection conn = aURL.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream (is);
			bm = BitmapFactory.decodeStream (bis);
			bis.close();
			is.close();
			}
		catch (Exception ex)
			{
			Log.i ("vtest", "Error getting bitmap: " + url);
			ex.printStackTrace();
			}
		return bm;
		}
	
	public static void stack_thumbs
			(final Context ctx, final metadata m, final String channel_ids[], final int width, final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					if (!make_app_dir (ctx, m, "cthumbs")) return;
					if (!make_app_dir (ctx, m, "xthumbs")) return;
					
					if (channel_ids != null)
						{
						for (String channel_id: channel_ids)
							{
							if (channel_id == null || channel_id.equals ("0"))
								{
								/* special cases */
								continue;
								}							
							if (channel_id.contains (":"))
								{
								/* virtual channels don't have thumbnails */
								continue;
								}
							
							String url = m.pool_meta (channel_id, "thumb");							
							String filename = ctx.getFilesDir() + "/" + m.api_server + "/cthumbs/" + channel_id + ".png"; 
							
							download (channel_id, url, filename, false);
														
							if (width > 0)
								{
								bitmappery.resize_bitmap_in_place (filename, width);
								}
							
							if (update != null)
								in_main_thread.post (update);	
							
							/* data obtained via 3.2 has an extra thumbnail. These are used by the portal and store */
							
							String first_episode_thumb = m.pool_meta (channel_id, "episode_thumb_1");		
							if (first_episode_thumb != null && first_episode_thumb.startsWith ("http"))
								{
								String ep_filename = ctx.getFilesDir() + "/" + m.api_server + "/xthumbs/" + channel_id + ".png"; 
								download (channel_id, first_episode_thumb, ep_filename, false);
								
								if (update != null)
									in_main_thread.post (update);	
								}												
							}
						}
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};
	
		t.start();
		}

	public static void download_set_thumbs (final Context ctx, final metadata m, 
			final String set_ids[], final String set_episode_thumbs[], 
				final String set_channel_thumbs[], final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					if (!make_app_dir (ctx, m, "sthumbs")) return;
					if (!make_app_dir (ctx, m, "rthumbs")) return;
					
					for (int i = 0; i < set_ids.length; i++)
						{
						String set_id = set_ids [i];
						
						if (!set_id.startsWith ("$"))
							{
							String set_episode_thumb = set_episode_thumbs [i];
							String set_channel_thumb = set_channel_thumbs [i];
							
							String sfilename = ctx.getFilesDir() + "/" + m.api_server + "/sthumbs/" + set_id + ".png";		
							Log.i ("vtest", "---------------=====> " + set_id + " THUMBNAIL " + set_episode_thumb);
							download (set_id, set_episode_thumb, sfilename, true);
							
							if (update != null)
								in_main_thread.post (update);
							
							String rfilename = ctx.getFilesDir() + "/" + m.api_server + "/rthumbs/" + set_id + ".png";		
							Log.i ("vtest", "---------------=====> " + set_id + " THUMBNAIL " + set_channel_thumb);
							download (set_id, set_channel_thumb, rfilename, true);
							
							if (update != null)
								in_main_thread.post (update);
							}
						}
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};
	
		t.start();
		}

	public static void download_set_banners (final Context ctx, final metadata m, 
			final String set_ids[], final String set_banners[], final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					if (!make_app_dir (ctx, m, "bthumbs")) return;
					
					for (int i = 0; i < set_ids.length; i++)
						{
						String set_id = set_ids [i];
						
						if (!set_id.startsWith ("$"))
							{
							String set_banner = set_banners [i];
							if (set_banner != null && !set_banner.equals(""))
								{
								String sfilename = ctx.getFilesDir() + "/" + m.api_server + "/bthumbs/" + "_" + set_id + ".png";	
								String ffilename = ctx.getFilesDir() + "/" + m.api_server + "/bthumbs/"       + set_id + ".png";	
								Log.i ("vtest", "---------------=====> " + set_id + " THUMBNAIL " + set_banner);
								download (set_id, set_banner, sfilename, true);
								File sf = new File (sfilename);
								File ff = new File (ffilename);
								sf.renameTo (ff);
								if (update != null)
									in_main_thread.post (update);
								}
							}
						}
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};
	
		t.start();
		}

	
	public static void download (String id, String url, String filename, boolean clobber)
		{
		if (url != null && url.startsWith ("http"))
			{
			File f = new File (filename);
			if (!f.exists () || f.length() == 0 || clobber)
				{
				FileOutputStream out = null;
				
				try 
					{
					out = new FileOutputStream (filename);
					}
				catch (FileNotFoundException e1)
					{
					e1.printStackTrace();
					return;
					}
	
				Log.i ("vtest", "DOWNLOAD: " + id + " URL: " + url);
				Bitmap bmp = getImageBitmap (url);
	
				if (bmp == null)
					{
					/* leaves a zero byte file */
					Log.i ("vtest", "unable to download thumbnail");
					}
				else
					{
					try
						{
						bmp.compress (Bitmap.CompressFormat.PNG, 100, out);
						}
					catch (Exception e)
						{
						e.printStackTrace ();
						}
					}
				
				try
					{
					out.close();
					}
				catch (IOException e)
					{
					e.printStackTrace();
					return;
					}
				}
			}
		else
			{
			// Log.w ("vtest", "[thumbnail] id " + id + " invalid URL: " + url); // noisy
			}
		}
	
	/* download a single sample thumb into the xthumbs directory */
	
	public static void sample_thumb
			(final Context ctx, final metadata m, final String channel_id, final String url, final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					if (!make_app_dir (ctx, m, "xthumbs")) return;										
					
					String filename = ctx.getFilesDir() + "/" + m.api_server + "/xthumbs/" + channel_id + ".png"; 
							
					download (channel_id, url, filename, false);
					in_main_thread.post (update);	
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};
	
		t.start();
		}
	
	public static void download_channel_thumbnails (final Context ctx, final metadata m, final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					if (!make_app_dir (ctx, m, "cthumbs")) return;
	
					for (int i = 1; i <= 9; i++)
						for (int j = 1; j <= 9; j++)
							{
							if (m.occupied (i * 10 + j) && !m.channel_meta (i * 10 + j, "thumb").equals (""))
								{
								String id = m.channel_meta (i * 10 + j, "id");
								String filename = ctx.getFilesDir() + "/" + m.api_server + "/cthumbs/" + id + ".png";
								File f = new File (filename);								
								if (f.exists() && f.length() == 0)
									{
									Log.i ("vtest", "Thumbnail was zero bytes: " + filename);
									f.delete();
									}								
								if (!f.exists ())
									{
									String url = m.channel_meta (i * 10 + j, "thumb");
									if (url != null && url.startsWith ("http"))
										{
										Log.i ("vtest", "DOWNLOAD: " + (i * 10 + j) + " URL: " + url);
										Bitmap bmp = getImageBitmap (url);
		
										if (bmp == null)
											{
											Log.i ("vtest", "unable to download thumbnail");
											continue;
											}
										
										try
											{
											FileOutputStream out = new FileOutputStream (filename);
											bmp.compress (Bitmap.CompressFormat.PNG, 100, out);
											in_main_thread.post (update);
											}
										catch (Exception e)
											{
											e.printStackTrace ();
											}
										}									
									else
										Log.i ("vtest", "[channel thumbnails] Not a URL: " + url);
									}
								else
									Log.i ("vtest", "[channel thumbnails] thumb exists for position " + (i * 10 + j));
								}
							}
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};

		t.start();
		}

	public static void download_soc_image
			(final Context ctx, final metadata m, final String post_id, final String url, final int max_width, final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					if (!make_app_dir (ctx, m, "soc")) return;										
					
					String filename = ctx.getFilesDir() + "/" + m.api_server + "/soc/" + post_id + ".png"; 
							
					download (post_id, url, filename, false);					
					
					if (max_width > 0)
						{
						bitmappery.resize_bitmap_in_place (filename, max_width);
						}
										
					in_main_thread.post (update);	
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};
		
		t.start();
		}
	
	public static boolean make_app_dir (final Context ctx, final metadata m, String dir)
		{
		File sdir = new File (ctx.getFilesDir(), m.api_server);
		try
			{
			if (!sdir.exists())
				sdir.mkdir();
			}
		catch (Exception e)
			{
			Log.w ("fl", "Error creating directory: " + m.api_server);
			return false;
			}
		
		File edir = new File (ctx.getFilesDir() + "/" + m.api_server + "/" + dir);
		try
			{
			edir.mkdir();
			}
		catch (Exception e)
			{
			Log.w ("fl", "Error creating directory: " + m.api_server + "/" + dir);
			return false;
			}
		
		return true;
		}

	public static boolean download_single_episode_thumb (metadata m, Context ctx, String channel, String episode, Handler in_main_thread, Runnable update)
		{
		if (channel == null || episode == null)
			return false;
		
		String url = m.program_meta (episode, "thumb");
		String filepath = m.episode_in_cache (episode);

		Log.i ("vtest", "(ch " + channel + ", ep " + episode + ") url: " + url);
		
		if (url != null && filepath != null && url.startsWith ("http"))
			{
			String actual_channel = channel;
			if (channel.contains (":"))
				{
				actual_channel = m.program_meta (episode, "real_channel");
				if (!make_app_dir (ctx, m, "ethumbs/" + actual_channel)) return false;
				}
			String filename = ctx.getFilesDir() + "/" + filepath;
			File f = new File (filename);
			if (f.exists() && f.length() == 0)
				{
				Log.i ("vtest", "Thumbnail was zero bytes: " + filepath);
				f.delete();
				}
			if (!f.exists())
				{
				Log.i ("vtest", "DOWNLOAD: " + episode + " URL: " + url);

				Bitmap bmp = getImageBitmap (url);

				if (bmp == null)
					{
					Log.i ("vtest", "unable to download thumbnail");
					try
						{
						FileOutputStream out = new FileOutputStream (filename);
						out.close();
						}
					catch (Exception ex)
						{
						}
					in_main_thread.post (update);
					return false;
					}
				
				try
					{
					FileOutputStream out = new FileOutputStream (filename);
					// Log.i ("vtest", "XXX saving thumbnail to: " + filename);
					// Log.i ("vtest", "thumbnail dimensions: " + bmp.getWidth() + "x" + bmp.getHeight());
					if (bmp.getWidth() > 360)
						{
						Log.i ("vtest", "episode thumb is " + bmp.getWidth() + "x" + bmp.getHeight() + ", cropping");
						Bitmap cropped_bmp = bitmappery.cropped_episode_bitmap (bmp, 180, 320);
						if (cropped_bmp != null)
							{
							// bmp.recycle();
							bmp = cropped_bmp;
							}
						}
					bmp.compress (Bitmap.CompressFormat.PNG, 100, out);
					out.close();
					// bmp.recycle();
					}
				catch (Exception e)
					{
					e.printStackTrace ();
					}
				
				in_main_thread.post (update);
				return true;
				}
			else
				{
				// Log.i ("vtest", "XXX thumbnail already exists: " + filename);
				}
			}
		else
			{
			String filename = ctx.getFilesDir() + "/" + filepath;
			try
				{
				FileOutputStream out = new FileOutputStream (filename);
				out.close();
				}
			catch (Exception ex)
				{
				}
			}
		return false;
		}
	
	public static void download_episode_thumbnails (final Context ctx, final metadata m, final String channel, final Handler in_main_thread,
			final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					Log.i ("vtest", "----------- Download episode thumbnails for channel: " + channel);
	
					if (channel == null)
						return;
					
					if (!make_app_dir (ctx, m, "ethumbs")) return;
					
					if (!channel.contains (":"))
						if (!make_app_dir (ctx, m, "ethumbs/" + channel)) return;
	
					String program_line[] = m.program_line_by_id (channel);
					if (program_line != null)
						{
						for (String episode: program_line)
							download_single_episode_thumb (m, ctx, channel, episode, in_main_thread, update);
						}
					else
						Log.i ("vtest", "download episode thumbnails: no episodes in channel: " + channel);
					
					in_main_thread.post (update);
					}				
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			};

		t.start();
		}
	
	public static void download_specific_episode_thumbnails
			(final Context ctx, final metadata m, final String channel, final String episodes[], final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					Log.i ("vtest", "----------- Download specific episode thumbnails for channel: " + channel);
	
					if (channel == null)
						return;
					
					if (!make_app_dir (ctx, m, "ethumbs")) return;
					
					if (!channel.contains (":"))
						if (!make_app_dir (ctx, m, "ethumbs/" + channel)) return;
	
					if (episodes != null)
						{
						for (String episode: episodes)
							download_single_episode_thumb (m, ctx, channel, episode, in_main_thread, update);
						}
					
					in_main_thread.post (update);
					}				
				catch (Exception ex)
					{
					ex.printStackTrace();
					}
				}
			};

		t.start();
		}
	
	public static void download_first_n_episode_thumbs
			(final Context ctx, final metadata m, final String channel, final int n, final Handler h, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					Log.i ("vtest", "----------- Download first " + n + " episode thumbnails for channel: " + channel);
	
					if (channel == null)
						return;
					
					if (!make_app_dir (ctx, m, "ethumbs")) return;
					
					if (!channel.contains (":"))
						if (!make_app_dir (ctx, m, "ethumbs/" + channel)) return;
	
					int count = 0;
					
					String program_line[] = m.program_line_by_id (channel);
					if (program_line != null)
						{
						final int true_n = (n < program_line.length) ? n : program_line.length;
						for (int i = 0; i < true_n; i++)
							{					
							String episode = program_line [i];
							if (download_single_episode_thumb (m, ctx, channel, episode, h, update))
								count++;
							}
						}
					else
						Log.i ("vtest", "download episode thumbnails: no episodes in channel: " + channel);
					
					if (count > 0)
						h.post (update);
					}				
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};

		t.start();
		}
	
	public static void download_q_thumbs
			(final Context ctx, final metadata config, final String channel_id, final int n, final Handler h, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					Log.i ("vtest", "----------- Download first " + n + " q-thumbnails for channel: " + channel_id);
	
					if (channel_id == null)
						return;
					
					if (!make_app_dir (ctx, config, "qthumbs")) return;
					if (!make_app_dir (ctx, config, "qthumbs/" + channel_id)) return;
					
					int n_downloads = 0;
					
					for (int i = 1; i <= n; i++)
						{
						String thumb = config.pool_meta (channel_id, "episode_thumb_" + i);
						if (thumb != null && thumb.startsWith ("http"))
							{
							String filename = ctx.getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (thumb) + ".png";
							Log.i ("vtest", "Q-DOWNLOAD: " + thumb + " => " + filename);
							download_image (ctx, filename, thumb, false);
							n_downloads++;
							}
						}
					
					if (n_downloads > 0)
						h.post (update);
					}				
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};
	
		t.start();		
		}
	
	public static void download_stack_of_recent_thumbs (final Context ctx, final metadata m, 
			final Stack <String> channel_ids, final Stack <String> episodes, final Stack <String> thumbnails,
				final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread()
			{
			public void run()
				{
				try
					{
					Log.i ("vtest", "Download stack of recent thumbs");
	
					if (!make_app_dir (ctx, m, "ethumbs")) return;
					
					final Callback episode_thumb = new Callback()
						{
						@Override
						public void run_three_strings (String channel, String episode, String url)
							{
							if (!make_app_dir (ctx, m, "ethumbs/" + channel)) return;
							
							Log.i ("vtest", "may download (" + channel + "): " + url);
							String filepath = m.episode_in_cache (channel, episode);
							
							if (url != null && url.startsWith ("http"))
								{
								String filename = ctx.getFilesDir() + "/" + filepath;
								File f = new File (filename);
								if (f.exists() && f.length() == 0)
									{
									Log.i ("vtest", "Thumbnail was zero bytes: " + filepath);
									f.delete();
									}
								if (!f.exists ())
									{
									Log.i ("vtest", "DOWNLOAD: " + episode + " URL: " + url);
		
									Bitmap bmp = getImageBitmap (url);
		
									if (bmp == null)
										{
										Log.i ("vtest", "unable to download thumbnail");
										try
											{
											FileOutputStream out = new FileOutputStream (filename);
											out.close();
											}
										catch (Exception ex)
											{
											}
										return;
										}
									
									try
										{
										FileOutputStream out = new FileOutputStream (filename);
										bmp.compress (Bitmap.CompressFormat.PNG, 100, out);
										m.set_channel_meta_by_id (channel, "most_recent_thumb_id", episode);
										m.set_channel_meta_by_id (channel, "typical_episode_thumb", episode);
										out.close();
										}
									catch (Exception e)
										{
										e.printStackTrace ();
										}

									// bmp.recycle();
									in_main_thread.post (update);
									}
								else
									{
									Log.i ("vtest", "thumbnail already exists: " + filename);
									m.set_channel_meta_by_id (channel, "most_recent_thumb_id", episode);									
									}
								}
							else
								{
								// Log.i ("vtest", "Invalid URL: " + url); // noisy
								try
									{
									String filename = ctx.getFilesDir() + "/" + filepath;
									FileOutputStream out = new FileOutputStream (filename);
									out.close();
									in_main_thread.post (update);
									}
								catch (Exception ex)
									{
									}
								}
							}
						};
	
					while (!channel_ids.empty())
						{
						String channel_id = channel_ids.pop();
						String episode_id = episodes.pop();
						String thumb = thumbnails.pop();
						Log.i ("vtest", "DSRT: " + channel_id + " -> " + thumb);
						episode_thumb.run_three_strings (channel_id, episode_id, thumb);
						}
					
					in_main_thread.post (update);
					}				
				catch (Exception ex)
					{
					ex.printStackTrace();
					((Activity) ctx).finish();
					}
				}
			};

		t.start();		
		}
	
	public static void purge_old_thumbnails (final Context ctx, final metadata m)
		{
		Thread t = new Thread ()
			{
			public void run ()
				{
				Log.i ("vtest", "purging old thumbnails");
				
				String edir = ctx.getFilesDir() + "/" + m.api_server + "/" + "ethumbs";
				Log.i ("vtest", "purging: " + edir);
				descend_into (new File (edir));
				
				String cdir = ctx.getFilesDir() + "/" + m.api_server + "/" + "cthumbs";
				Log.i ("vtest", "purging: " + cdir);
				descend_into (new File (cdir));
				
				Log.i ("vtest", "thumbnail purge complete");
				}
			};
			
		t.start();
		}
	
	public static void descend_into (File dir)
		{
		long now = System.currentTimeMillis() / 1000;
		
		if (dir != null)
			{
			File[] dirlist = dir.listFiles();
			if (dirlist != null)
			    for (File entry: dirlist)
			    	{
			        if (entry.isDirectory())
			        	{
			            descend_into (entry);
			        	}
			        else
			        	{
			            File f = new File (entry.getPath());
			            if (f.exists())
			            	{
			            	long date = f.lastModified() / 1000;
			            	if (now - date > (3600 * 24))
			            			f.delete();
			            	}
			        	}
			    	}
			}
		}	
	
	public static void download_titlecard_images_for_channel
			(final Context ctx, final metadata config, final String channel, final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread ()
			{
			public void run ()
				{
				Log.i ("vtest", "download_titlecard_images_for_channel: " + channel);

				if (!make_app_dir (ctx, config, "titlecards")) return;
				if (!make_app_dir (ctx, config, "titlecards/" + channel)) return;
				
				String program_line[] = config.program_line_by_id (channel);

				if (program_line == null)
					return;
				
				for (String episode_id: program_line)
					{
					if (episode_id != null)
						{
						String total_subepisodes = config.program_meta (episode_id,  "total-subepisodes");
						if (total_subepisodes != null)
							{
							int nsub = Integer.parseInt (total_subepisodes);
							for (int sub = 1; sub <= nsub; sub++)
								{
								String begin_title_id = config.program_meta (episode_id, "sub-" + sub + "-begin-title");
								if (begin_title_id != null)
									download_titlecard_image (ctx, config, channel, episode_id, sub, "begin", begin_title_id);						
								String end_title_id = config.program_meta (episode_id, "sub-" + sub + "-end-title");
								if (end_title_id != null)
									download_titlecard_image (ctx, config, channel, episode_id, sub, "end", end_title_id);						
								}
							}
						}
					}				
				}
			};
			
		t.start();
		}

	public static void download_titlecard_image
			(Context ctx, metadata config, String channel, String episode, int sub, String begin_or_end, String titlecard_id)
		{
		String url = config.titlecard_meta (titlecard_id, "bgimage");
			
		String filepath = "titlecards/" + channel + "/" + titlecard_id + "." + begin_or_end + ".png";

		Log.i ("vtest", "DOWNLOAD TITLECARD IMAGE (sub " + sub + "): " + url + " => " + filepath);
		
		String filename = ctx.getFilesDir() + "/" + config.api_server + "/" + filepath;
		File f = new File (filename);
		if (f.exists() && f.length() == 0)
			{
			Log.i ("vtest", "Titlecard background was zero bytes: " + filepath);
			f.delete();
			}
		if (!f.exists ())
			{
			Log.i ("vtest", "TITLECARD DOWNLOAD: " + episode + " URL: " + url);

			Bitmap bmp = getImageBitmap (url);

			if (bmp == null)
				{
				Log.i ("vtest", "++ unable to download titlecard background");
				try
					{
					FileOutputStream out = new FileOutputStream (filename);
					out.close();
					}
				catch (Exception ex)
					{
					}
				return;
				}
			
			try
				{
				FileOutputStream out = new FileOutputStream (filename);
				bmp.compress (Bitmap.CompressFormat.PNG, 100, out);				
				out.close();
				Log.i ("vtest", "++ titlecard image saved as: " + filename);
				}
			catch (Exception e)
				{
				e.printStackTrace ();
				}
			}
		else
			Log.i ("vtest", "++ titlecard existed: " + filename);
		}
	
	public static void download_list_of_images
			(final Context ctx, final metadata config, final String dir, final Stack <String> filenames, final Stack <String> urls,
					final boolean clobber, final Handler in_main_thread, final Runnable update)
		{
		Thread t = new Thread ()
			{
			public void run ()
				{		
				if (!make_app_dir (ctx, config, dir)) return;
				
				while (!filenames.empty())
					{
					String filename = filenames.pop();
					String url = urls.pop();
					String full_filename = ctx.getFilesDir() + "/" + config.api_server + "/" + dir + "/" + filename + ".png";
					download_image (ctx, full_filename, url, clobber);
					in_main_thread.post (update);
					}			
				}
			};
			
		t.start();
		}
	
	public static void download_image (Context ctx, String filename, String url, boolean clobber)
		{
		File f = new File (filename);
		if (clobber || (f.exists() && f.length() == 0))
			{
			Log.i ("vtest", "Image was zero bytes: " + filename);
			f.delete();
			}
		if (!f.exists ())
			{
			Log.i ("vtest", "IMAGE DOWNLOAD: " + url);
		
			Bitmap bmp = getImageBitmap (url);
		
			if (bmp == null)
				{
				Log.i ("vtest", "++ unable to download image");
				try
					{
					FileOutputStream out = new FileOutputStream (filename);
					out.close();
					}
				catch (Exception ex)
					{
					}
				return;
				}
			
			try
				{
				FileOutputStream out = new FileOutputStream (filename);
				bmp.compress (Bitmap.CompressFormat.PNG, 100, out);				
				out.close();
				Log.i ("vtest", "++ image saved as: " + filename);
				}
			catch (Exception e)
				{
				e.printStackTrace ();
				}
			}
		else
			Log.i ("vtest", "++ image exists: " + filename);
		}	
	}