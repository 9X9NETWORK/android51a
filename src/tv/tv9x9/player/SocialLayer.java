package tv.tv9x9.player;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SocialLayer extends StandardFragment
	{
	metadata config = null;
	
    SocialAdapter social_adapter = null;
    
    public class social
    	{	
    	String username;
    	String text;
    	String type;
    	String post_id;
    	String format;
    	String object_id;
    	String link;
    	String date;
    	String link_name;
    	String link_desc;
    	String link_caption;
    	int num_images;
    	String images[] = null;
    	}
    
    social social_feed[] = new social [0];
    
    Social soc = null;
    
    boolean soc_shim_added = false;
	
    public interface OnSocialListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
        public void launch_player (String channel_id, String channels[]);
        public void launch_player (String channel_id, String episode_id, String channels[]);
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public int screen_width();
    	public void remember_location();
		}    
    
    OnSocialListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.social_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnSocialListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnSocialListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "social";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
    
    public void start_social (final metadata config)
	    {
	    this.config = config;
	    
		/* zap any earlier data */
		social_feed = new social [0];
			
		ListView vSocial = (ListView) getView().findViewById (R.id.social_list);
		social_adapter = new SocialAdapter (getActivity(), social_feed);
		vSocial.setAdapter (social_adapter);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View shim = inflater.inflate (R.layout.footer_shim_d9, null);
		vSocial.addFooterView (shim);
		
		vSocial.setOnItemClickListener (new OnItemClickListener()
			{
			public void onItemClick (AdapterView parent, View v, int position, long id)
				{
				if (position < social_feed.length)
					{
					String link = social_feed [position].link;
					if (link != null)
						{
						log ("social click: " + position + ", link: " + link);
			        	Intent wIntent = new Intent (Intent.ACTION_VIEW, Uri.parse (link));
			        	try
			        		{
				        	startActivity (wIntent);
			        		}
			        	catch (Exception ex)
			        		{
			        		ex.printStackTrace();
			        		}
						}
					else
						log ("social click: " + position + ", but no link");
					}
				}
			});	
		
		if (soc != null)
			{
			try { soc.close(); } catch (Exception ex) {};
			soc = null;
			}
		
		soc = new Social (config);
		
		Callback onConnected = new Callback()
			{
			@Override
			public void run_string (String text)
				{
				log ("callback: social connected");
				soc.post ("MSO " + config.mso);
				}
			};
			
		Callback onRead = new Callback()
			{
			@Override
			public void run_string (String text)
				{
				log ("callback: social line read: " + text);
				if (text.startsWith ("DATA ") || text.startsWith ("{"))
					{
					social new_feed[] = new social [social_feed.length + 1];
					System.arraycopy (social_feed, 0, new_feed, 1, social_feed.length);
					
					String data = text.replaceAll ("^DATA ", "");
										
					JSONObject json = null;
					try
						{
						json = new JSONObject (data);
						}
					catch (JSONException ex)
						{						
						ex.printStackTrace();
						}
					
					if (json == null)
						{
						log ("json is null, ignoring line");
						return;
						}
					
					// ** got line: {"username": "9x9.tv", "name": "9x9.tv", "text": "Thanks to all of your suggestions, we've added this feature that you've all been waiting for! http://blog.9x9.tv/2013/02/bring-your-youtube-subscriptions-sign.html You can download the app here: https://play.google.com/store/apps/details?id=tv.tv9x9.player", "userid": "283883488348574", "date": "2013-02-08T10:02:29+0000", "postid": "283883488348574_524590450895569", "type": "facebook"}
	
					String x_username = null;
					String x_text = null;
					String x_type = null;
					String x_post_id = null;
					String x_object_id = null;
					String x_format = null;
					String x_link = null;
					String x_date = null;
					String x_link_name = null;
					String x_link_desc = null;
					String x_link_caption = null;
					String x_num_images = null;
					JSONArray x_images = null;
					
					try { x_username = json.getString ("username"); } catch (JSONException ex) {};
					try { x_text = json.getString ("text"); } catch (JSONException ex) {};
					
									
					if (x_username == null || x_text == null)
						{
						log ("social: incomplete data");
						return;
						}
	
					try { x_type = json.getString ("type"); } catch (JSONException ex) {};
					try { x_post_id = json.getString ("postid"); } catch (JSONException ex) {};
					try { x_format = json.getString ("format");} catch (JSONException ex) {};
					try { x_object_id = json.getString ("objectid"); } catch (JSONException ex) {};
					try { x_link = json.getString ("link"); } catch (JSONException ex) {};
					try { x_date = json.getString ("date"); } catch (JSONException ex) {};
					try { x_link_name = json.getString ("link-name"); } catch (JSONException ex) {};
					try { x_link_desc = json.getString ("link-description"); } catch (JSONException ex) {};
					try { x_link_caption = json.getString ("link-caption"); } catch (JSONException ex) {};
					try { x_num_images = json.getString ("num_images"); } catch (JSONException ex) {};
					try { x_images = json.getJSONArray ("images"); } catch (JSONException ex) {};
					
					social item = new social();
					
					int num_images = Integer.parseInt (x_num_images);
					item.images = new String [num_images];					
					for (int k = 0; k < num_images; k++)
						{
						String url = null;
						try { url = x_images.getString (k); } catch (Exception x) {};
						item.images [k] = url;
						}
					
					item.username = x_type.equals ("twitter") ? "@" + x_username : x_username;
					item.text = x_text;
					item.type = x_type;
					item.post_id = x_post_id;
					item.format = x_format;
					item.object_id = x_object_id;
					item.link = x_link;
					item.date = x_date;
					item.link_name = x_link_name;
					item.link_desc = x_link_desc;
					item.link_caption = x_link_caption;
					item.num_images = num_images;
					
					log ("SOCIAL: user=" + item.username + " format=" + item.format + " object=" + item.object_id);
					
					new_feed [0] = item; 
					social_feed = new_feed;
					
					mCallback.get_main_thread().post (new Runnable()
						{
						@Override
						public void run()
							{
							social_adapter.set_content (social_feed);
							}
						});
					}
				}
			};
			
		Callback onError = new Callback()
			{
			@Override
			public void run_string (String text)
				{
				log ("callback: social error: " + text);
				soc.close();
				soc = null;
				}
			};
			
		soc.open (onConnected, onRead, onError);
				
		View vStop = getView().findViewById (R.id.soc_stop);
		if (vStop != null)
			vStop.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	soc.close();
		        	}
				});
		}
    
    public void close()
	    {
		if (soc != null)
			{
			soc.close();
			soc = null;
			}
	    }
    
	public class SocialAdapter extends BaseAdapter
		{
		private Context context;
		private social socials[] = null;
		// private boolean requested_image[] = null;
		private Set <String> requested_image = new HashSet <String> ();
				
		public SocialAdapter (Context context, social socials[])
			{
			this.context = context;
			this.socials = socials;
			// requested_image = new boolean [socials.length];
			// Arrays.fill (requested_image, Boolean.FALSE);
			}
	
		public void set_content (social socials[])
			{
			this.socials = socials;
			// requested_image = new boolean [socials.length];
			// Arrays.fill (requested_image, Boolean.FALSE);
			notifyDataSetChanged();
			}
		
		@Override
		public int getCount()
			{			
			log ("getcount: " + socials.length);
			return socials == null ? 0 : socials.length;
			}
		
		@Override
		public Object getItem (int position)
			{
			return position;
			}
	
		@Override
		public long getItemId (int position)
			{
			return position;
			}
		
		@Override
		public View getView (final int position, View convertView, ViewGroup parent)
			{
			LinearLayout rv = null;
					
			log ("social getView: " + position + " (of " + getCount() + ")");
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (getActivity(), R.layout.social_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			if (position > socials.length)
				{
				log ("getView: position is " + position + " but only have " + socials.length + " items!");
				return null;
				}
			
			social soc = socials [position];
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (soc.username);
			
			/*
			 * option 1 - ordinary text. Does not have hotlinks
			 * 
			TextView vText = (TextView) rv.findViewById (R.id.text);
			if (vText != null)
				vText.setText (soc.text);
			*/
			
			/*
			 * option 2 - webview. has hotlinks, but problems with view height
			 * 
			WebView vDesc = (WebView) rv.findViewById (R.id.webtext);
			if (vDesc != null)
				{
				vDesc.setHorizontalScrollBarEnabled (false);
				vDesc.getSettings().setLayoutAlgorithm (LayoutAlgorithm.SINGLE_COLUMN);
				vDesc.setScrollBarStyle (View.SCROLLBARS_INSIDE_OVERLAY);
				
				// this is used as the default encoding of decode.
				vDesc.getSettings().setDefaultTextEncodingName ("utf-8");

				vDesc.loadDataWithBaseURL ("http://www.youtube.com/", soc.text, "text/html", "utf-8", null);
				}
			*/
			
			/*
			 * option 3 - create a line-by-line fake webview-like abomination
			 */
			LinearLayout vLineByLine = (LinearLayout) rv.findViewById (R.id.line_by_line);
			fill_line_by_line_view (vLineByLine, soc.text);
			
			TextView vCounter = (TextView) rv.findViewById (R.id.soc_counter);
			if (vCounter != null)
				vCounter.setText ("#" + Integer.toString (socials.length - position));
			
			TextView vSource = (TextView) rv.findViewById (R.id.message_source);
			if (vSource != null)
				vSource.setText (" on " + soc.type);
			
			log ("SOCIAL #" + Integer.toString (socials.length - position) + ": user=" + soc.username + " format=" + soc.format + " object=" + soc.object_id);
			
			TextView vWhat = (TextView) rv.findViewById (R.id.soc_what);
			if (vWhat != null)
				vWhat.setText (soc.format + " (" + soc.num_images + (soc.num_images == 1 ? " image" : " images") + ")");
			
			TextView vAgo = (TextView) rv.findViewById (R.id.message_ago);
			if (vAgo != null)
				{
				String ago = "";
				if (soc.date.length() == "1409103232".length())
					ago = util.ageof (getActivity(), Long.parseLong (soc.date));
				vAgo.setText (ago);
				}
		
			TextView vLinkName = (TextView) rv.findViewById (R.id.link_name);
			if (vLinkName != null)
				{
				if (soc.link_name != null)
					{
					vLinkName.setText (soc.link_name);
					vLinkName.setVisibility (View.VISIBLE);
					}
				else
					vLinkName.setVisibility (View.GONE);
				}
	
			TextView vLinkDesc = (TextView) rv.findViewById (R.id.link_description);
			if (vLinkDesc != null)
				{
				if (soc.link_name != null)
					{
					vLinkDesc.setText (soc.link_desc);
					vLinkDesc.setVisibility (View.VISIBLE);
					}
				else
					vLinkDesc.setVisibility (View.GONE);
				}
			
			TextView vLinkCaption = (TextView) rv.findViewById (R.id.link_caption);
			if (vLinkCaption != null)
				{
				if (soc.link_caption != null)
					{
					vLinkCaption.setText (soc.link_caption);
					vLinkCaption.setVisibility (View.VISIBLE);
					}
				else
					vLinkCaption.setVisibility (View.GONE);
				}
			
			int inside_width = mCallback.screen_width() - mCallback.actual_pixels (8) * 4;
			
			View vRow1 = rv.findViewById (R.id.soc_mosaic_row_1);
			View vRow2 = rv.findViewById (R.id.soc_mosaic_row_2);		
			
			View vImage0 = rv.findViewById (R.id.soc_image_container);
			View vImage1 = rv.findViewById (R.id.soc_image_1);
			View vImage2 = rv.findViewById (R.id.soc_image_2);
			View vImage3 = rv.findViewById (R.id.soc_image_3);
			View vImage4 = rv.findViewById (R.id.soc_image_4);
			View vImage5 = rv.findViewById (R.id.soc_image_5);
			View vImage6 = rv.findViewById (R.id.soc_image_6);
			
			if (soc.num_images == 0)
				{
				vRow1.setVisibility (View.GONE);
				vRow2.setVisibility (View.GONE);
				vImage0.setVisibility (View.GONE);
				}
			else if (soc.num_images == 1)
				{
				vRow1.setVisibility (View.GONE);
				vRow2.setVisibility (View.GONE);
				vImage0.setVisibility (View.VISIBLE);
				fill_soc_image (rv, R.id.soc_image, soc, 0, position);
				View vPlayArrow = rv.findViewById (R.id.soc_image_play);
				vPlayArrow.setVisibility (soc.format.equals ("video") ? View.VISIBLE : View.GONE);
				}
			else if (soc.num_images >= 2)
				{
				vImage0.setVisibility (View.GONE);
				vRow1.setVisibility (View.VISIBLE);
				vRow2.setVisibility (soc.num_images >= 4 ? View.VISIBLE : View.GONE);
				
				vImage1.setVisibility (View.VISIBLE);
				vImage2.setVisibility (soc.num_images >= 2 ? View.VISIBLE : View.GONE);
				vImage3.setVisibility (soc.num_images == 3 || soc.num_images >= 5 ? View.VISIBLE : View.GONE);
				vImage4.setVisibility (soc.num_images >= 4 ? View.VISIBLE : View.GONE);
				vImage5.setVisibility (soc.num_images >= 4 ? View.VISIBLE : View.GONE);
				vImage6.setVisibility (soc.num_images >= 6 ? View.VISIBLE : View.GONE);			
				
				fill_soc_image (rv, R.id.soc_image_1, soc, 0, position);
				fill_soc_image (rv, R.id.soc_image_2, soc, 1, position);
				if (soc.num_images == 3 || soc.num_images >= 5)
					fill_soc_image (rv, R.id.soc_image_3, soc, 2, position);
				if (soc.num_images >= 4)
					fill_soc_image (rv, R.id.soc_image_4, soc, soc.num_images == 4 ? 2 : 3, position);
				if (soc.num_images >= 5)
					fill_soc_image (rv, R.id.soc_image_5, soc, soc.num_images == 4 ? 3 : 4, position);
				if (soc.num_images >= 6)
					fill_soc_image (rv, R.id.soc_image_6, soc, 5, position);
				}
			
			return rv;
			}	
		
		private void fill_soc_image (View parent, int resource_id, social soc, int image_num, int position)
			{
			ImageView vThumb = (ImageView) parent.findViewById (resource_id);
			if (vThumb != null)
				{
				boolean used_thumbnail = false;
				
				String image = soc.images [image_num];
				if (image != null && image.startsWith ("http"))
					{
					String filename = getActivity().getFilesDir() + "/" + config.api_server + "/soc/" + soc.post_id + "--number--" + image_num + ".png";
					File f = new File (filename);
					if (f.exists())
						{
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							{
							vThumb.setImageBitmap (bitmap);
							vThumb.setVisibility (View.VISIBLE);
							used_thumbnail = true;
							}								
						}
					
					String identifier = soc.post_id + " | " + image_num;
					
					if (!used_thumbnail && !requested_image.contains (identifier))
						{
						requested_image.add (identifier);
						
						if (soc.format != null && soc.format.equals ("photo") && soc.num_images == 1 && soc.object_id != null)
							{
							/* this might be quite large */
							String throwaway_token = "167147783347469|PUMgsuQgK9wOOVoYecDZV4b2-sw";
							image = "https://graph.facebook.com/" + soc.object_id + "/picture?access_token=" + throwaway_token + "&type=normal"; 
							}
						
						if (image.contains ("safe_image.php"))
							{
							/* extract original url from an embedded field in the Facebook url. undocumented */
							String fields[] = image.split ("&");
							for (String field: fields)
								{ 
								if (field.startsWith ("url="))
									{
									image = util.decodeURIComponent (field.substring (4));
									break;
									}
								}
							}
						
						int max_width = soc.num_images > 2 ? mCallback.screen_width() / 2 : mCallback.screen_width() - mCallback.actual_pixels (20);
						
						thumbnail.download_soc_image (getActivity(), config, soc.post_id, image_num, image, max_width, mCallback.get_main_thread(), new Runnable()
							{
							@Override
							public void run()
								{
								notifyDataSetChanged();
								}
							});
						}
					
	
					
					/*
					 *   1 -   X
					 *   2 -   X X
					 *   3 -   X X X
					 *   4 -   X X
					 *         X X
					 *   5 -   X X X
					 *         X X      
					 *   
					 */
					
					if (!used_thumbnail)
						vThumb.setVisibility (View.GONE);
					}
				}
			}
		}
	
	public void fill_line_by_line_view (LinearLayout vLineByLine, String text)
		{		
		/*
		ScrollView vContainer = (ScrollView) getView().findViewById (R.id.desc_scroll_container);
		vContainer.scrollTo (0, 0);
		*/
		
		// LinearLayout vLineByLine = (LinearLayout) getView().findViewById (R.id.line_by_line);
		vLineByLine.removeAllViews();
		
		if (text != null)
			{
			String lines[] = text.split ("\n");
			for (String line: lines)
				{
				// log ("LINE: |" + line + "|");				
				add_single_line (vLineByLine, line);
				}
			}
	
		/*
		ScrollView.LayoutParams layout = new ScrollView.LayoutParams
				(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT);
		
		vContainer.updateViewLayout (vDesc, layout);
		*/
		}

	public void add_single_line (LinearLayout vDesc, String line)
		{
		Pattern pattern = android.util.Patterns.WEB_URL;
		
		int maxchar = mCallback.is_phone() ? 16 : 24;
		
		int viewcount = 0;
		View append_views[] = new View [10];
		
	    String process_afterwards = null;
	    
		Matcher matcher = pattern.matcher (line);
		if (matcher.find (0))
			{
			/* create a LinearLayout */
			int match_start = matcher.start(1);
			int match_end = matcher.end();
			
			String text_to_left = match_start > 0 ? line.substring (0, match_start - 1) : "";
			String text = line.substring (match_start, match_end);					
			String text_to_right = line.substring (match_end);
			
			// log ("V1: |" + text_to_left + "|");
			// log ("V2: |" + text + "|");
			// log ("V3: |" + text_to_right + "|");
			
			TextView v1 = new TextView (getActivity());
			v1.setText (text_to_left);
			v1.setTextColor (Color.rgb (0xC0, 0xC0, 0xC0));
			v1.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			if (text_to_left.contains ("http") || text_to_left.contains ("HTTP"))
				v1.setMaxLines (1);
		    // v1.setBackgroundColor (Color.RED);
		    
			TextView v2 = new TextView (getActivity());
			v2.setText (text);
			v2.setTextColor (Color.rgb (0x00, 0x00, 0xFF));
			v2.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			if (!text.startsWith ("http") && !text.startsWith ("Http") && !text.startsWith ("HTTP"))
				text = "http://" + text;
			v2.setMaxLines (1);
			final String final_text = text;
			v2.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("description url click: " + final_text);
		        		mCallback.remember_location();
			        	Intent wIntent = new Intent (Intent.ACTION_VIEW, Uri.parse (final_text));
			        	try
			        		{
				        	startActivity (wIntent);
			        		}
			        	catch (Exception ex)
			        		{
			        		ex.printStackTrace();
			        		}
			        	}
					});
		    // v2.setBackgroundColor (Color.GREEN);
		    
			Matcher matcher3 = pattern.matcher (text_to_right);
			if (matcher3.find (0))
				{
				/* the third component has more links! recurse, but must be done after processing */
				process_afterwards = text_to_right;
				text_to_right = "";
				}
				
			TextView v3 = new TextView (getActivity());
			v3.setText (text_to_right);
			v3.setTextColor (Color.rgb (0xC0, 0xC0, 0xC0));
			v3.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			if (text_to_right.contains ("http") || text_to_right.contains ("HTTP"))
			v3.setMaxLines (1);
		    // v3.setBackgroundColor (Color.BLUE);
		
		    if (v1.length() > maxchar && v3.length() > maxchar)
		    	{
		    	// log ("scenario 1: v1 // v2 // v3");
				append_views [viewcount++] = v1;
				append_views [viewcount++] = v2;
				append_views [viewcount++] = v3;				    	
		    	}
		    else if (v1.length() > maxchar)
		    	{
		    	// log ("scenario 2: v1 // v2 + v3");
				append_views [viewcount++] = v1;
				LinearLayout linear = new LinearLayout (getActivity());	
				linear.addView (v2);
				linear.addView (v3);
				append_views [viewcount++] = linear;
		    	}
		    else if (v3.length() > maxchar)
		    	{
		    	// log ("scenario 3: v1 + v2 // v3");
				LinearLayout linear = new LinearLayout (getActivity());	
		    	linear.addView (v1);
		    	linear.addView (v2);
				append_views [viewcount++] = linear;
				append_views [viewcount++] = v3;
		    	}
		    else
		    	{
		    	// log ("scenario 4: v1 + v2 + v3");
				LinearLayout linear = new LinearLayout (getActivity());	
		    	linear.addView (v1);
		    	linear.addView (v2);
		    	linear.addView (v3);	
				append_views [viewcount++] = linear;
		    	}
			}
		else
			{
			/* create just a TextView */
			// log ("ORDINARY: |" + line + "|");
			TextView v = new TextView (getActivity());
		    v.setText (line);
		    // v.setBackgroundColor (Color.WHITE);
		    append_views [viewcount++] = v;
			}
		
		for (View v: append_views)
			{
			if (v != null)
				{
				LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams (WRAP_CONTENT, WRAP_CONTENT);
				v.setLayoutParams (layout);
				// log ("adding append_view");
				vDesc.addView (v);
				}
			}	
		
		if (process_afterwards != null)
			{
			// log ("AFTERLINE: " + process_afterwards);
			add_single_line (vDesc, process_afterwards);
			}
		}	
	}