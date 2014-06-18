package tv.tv9x9.player;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StoreAdapter extends BaseAdapter
	{
	private Context context;
	
	private String category_id = null;
	private String channels[] = null;
	
	mothership ms = null;
	metadata config = null;
	
	int current_category_index = 0;
	
	interface mothership
		{
		public int screen_width();
		public int actual_pixels (int dp);
		public void log (String text);
		public boolean is_tablet();
		public void load_category (final int index, final int starting);
		public void follow_or_unfollow (String channel_id, final View v);
		public void share_episode (String channel_id, String episode_id);
		public void launch_player (String channel_id, String channels[]);
		public boolean outgoing_category_queries_pending();
		public void set_follow_icon_state (View v, String channel_id, int follow_resource_id, int unfollow_resource_id);
		};
	
	public StoreAdapter (Context c, mothership ms, metadata config, int current_category_index, String category_id, String channels[])
		{
		context = c;
		this.channels = channels;
		this.category_id = category_id;
		this.ms = ms;
		this.config = config;
		this.current_category_index = current_category_index;
		}
	
	public void set_content (int current_category_index, String category_id, String channels[])
		{
		this.channels = channels;
		this.current_category_index = current_category_index;
		this.category_id = category_id;
		notifyDataSetChanged();
		}
	
	@Override
	public int getCount()
		{			
		return channels == null ? 0 : channels.length;
		}
	
	@Override
	public int getViewTypeCount()
		{
		return 2;
		}
	
	@Override
	public int getItemViewType (int position)
		{
		return channels [position].equals ("+") ? 1 : 0;
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
		FrameLayout rv = null;
				
		ms.log ("store getView: " + position);
		
		if (convertView == null)
			rv = (FrameLayout) View.inflate (context, ms.is_tablet() ? R.layout.store_item_tablet : R.layout.store_item, null);				
		else
			rv = (FrameLayout) convertView;
					
		if (rv == null)
			{
			ms.log ("getView: [position " + position + "] rv is null!");
			return null;
			}
		
		ImageView vChannelicon = (ImageView) rv.findViewById (R.id.chicon); 
		ImageView vEpisodeicon = (ImageView) rv.findViewById (R.id.epicon); 
		
		TextView vTitle = (TextView) rv.findViewById (R.id.title);
		TextView vMeta = (TextView) rv.findViewById (R.id.meta);
		
		if (vTitle == null)
			{
			ms.log ("getView: [position" + position + "] vTitle is null!");
			}
				
		if (position < channels.length && channels [position] != null)
			{
			final String channel_id = channels [position];
			
			if (channel_id.equals ("+"))
				{
				/* store only, not for search */
				rv = (FrameLayout) View.inflate (context, R.layout.store_item_more, null);		
				if (!ms.outgoing_category_queries_pending())
					ms.load_category (current_category_index, position);
				}
			else
				{
				String name = config.pool_meta (channel_id, "name");
				
				String timestamp = config.pool_meta (channel_id, "timestamp");
				String ago = timestamp == null ? "" : util.ageof (context, Long.parseLong (timestamp) / 1000);
				
				if (vTitle != null)
					vTitle.setText (name == null ? "" : name);
				
				if (vMeta != null)
					{
					int icount = config.programs_in_real_channel (channel_id);
					if (icount == 0)
						{
						String count = config.pool_meta (channel_id, "count");
						if (count != null && !count.equals (""))
							icount = Integer.parseInt (count);
						}
					
					String txt_episode = context.getResources().getString (R.string.episode_lc);		
					String txt_episodes = context.getResources().getString (R.string.episodes_lc);
					
					vMeta.setText (ago + " • " + icount + " " + (icount == 1 ? txt_episode : txt_episodes));
					}
				
				ImageView vSpecialTag = (ImageView) rv.findViewById (R.id.special_tag);
				if (vSpecialTag != null && category_id != null)
					{
					String tag = config.get_special_tag (channel_id, "store", category_id);
					Log.i ("vtest", "tag: " + tag + " category: " + category_id + " channel: " + channel_id);
					if (tag != null && !tag.equals (""))
						{
						int resource = 0;
						if (tag.equals ("recommended") || tag.equals ("best"))
							resource = R.drawable.app_tag_best_en;
						else if (tag.equals ("hot"))
							resource = R.drawable.app_tag_hot_en;
						vSpecialTag.setVisibility (resource != 0 ? View.VISIBLE : View.GONE);
						if (resource != 0)
							vSpecialTag.setImageResource (resource);
						}
					else
						vSpecialTag.setVisibility (View.GONE);
					}
				
				if (vEpisodeicon != null)
					{
					vEpisodeicon.setImageResource (R.drawable.store_unavailable);
					
					boolean channel_thumbnail_found = false;
					
					if (vChannelicon != null)
						{
						String filename = context.getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
						
						File f = new File (filename);
						if (f.exists ())
							{
							Bitmap bitmap = BitmapFactory.decodeFile (filename);
							if (bitmap != null)
								{
								channel_thumbnail_found = true;
								vChannelicon.setImageBitmap (bitmap);
								}
							}
						}
					
					String thumb_pile = channel_id.startsWith ("=") ? "cthumbs" : "xthumbs";
					
					boolean episode_thumbnail_found = false;
					String filename = context.getFilesDir() + "/" + config.api_server + "/" + thumb_pile + "/" + channel_id + ".png";
					
					File f = new File (filename);
					if (f.exists ())
						{
						BitmapFactory.Options bmOptions = new BitmapFactory.Options();
						bmOptions.inJustDecodeBounds = true;
						
						/* don't actually need this but might in the future */
						BitmapFactory.decodeFile (filename, bmOptions);
						float width = bmOptions.outWidth;
						float height = bmOptions.outHeight;
						
						bmOptions.inJustDecodeBounds = false;
						// bmOptions.inSampleSize = 2; // TODO: DOWNSAMPLED HERE
						bmOptions.inPurgeable = true;
						
						Bitmap bitmap = BitmapFactory.decodeFile (filename, bmOptions);
						if (bitmap != null)
							{
							episode_thumbnail_found = true;
							vEpisodeicon.setImageBitmap (bitmap);
							}
						}
					
					if (!channel_thumbnail_found)
						{
						if (vChannelicon != null)
							vChannelicon.setImageResource (R.drawable.noimage);
						}
				
					if (!episode_thumbnail_found)
						vEpisodeicon.setImageResource (R.drawable.store_unavailable);

					View vFollow = rv.findViewById (R.id.follow);
					
					// int follow_icon = ms.is_tablet() ? R.drawable.icon_follow : R.drawable.icon_follow_black;
					// int unfollow_icon = ms.is_tablet() ? R.drawable.icon_unfollow : R.drawable.icon_unfollow_press;
					int follow_icon = ms.is_tablet() ? R.drawable.icon_heart : R.drawable.icon_heart_black;
					int unfollow_icon = ms.is_tablet() ? R.drawable.icon_heart_active : R.drawable.icon_heart_active;
					
					Log.i ("vtest", "channel: " + channel_id + " subscribed=" + config.is_subscribed (channel_id));
					
					ms.set_follow_icon_state (vFollow, channel_id, follow_icon, unfollow_icon);
					
					if (vFollow != null)
						vFollow.setOnClickListener (new OnClickListener()
							{
					        @Override
					        public void onClick (View v)
					        	{
					        	ms.log ("click on: store follow " + channel_id);
					        	ms.follow_or_unfollow (channel_id, v);
					        	}
							});
					
					View vShare = rv.findViewById (R.id.share);
					if (vShare != null)
						vShare.setOnClickListener (new OnClickListener()
							{
					        @Override
					        public void onClick (View v)
					        	{
					        	ms.log ("click on: store share " + channel_id);
					        	ms.share_episode (channel_id, null);
					        	}
							});	
					
					if (vEpisodeicon != null)
						vEpisodeicon.setOnClickListener (new OnClickListener()
							{
					        @Override
					        public void onClick (View v)
					        	{
					        	ms.log ("click on: store play " + channel_id);
					        	ms.launch_player (channel_id, channels);
					        	}
							});							
					}
				}
			}
		else
			{
			if (vChannelicon != null)
				vChannelicon.setImageResource (R.drawable.unavailable);
			if (vEpisodeicon != null)
				vEpisodeicon.setImageResource (R.drawable.store_unavailable);
			vTitle.setText ("");
			}
		
		if (vEpisodeicon != null)
			{
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vEpisodeicon.getLayoutParams();
			layout.height = (int) ((float) (ms.screen_width() - ms.actual_pixels (40)) / 1.77 * 0.55);
			vEpisodeicon.setLayoutParams (layout);
			}
		
		return rv;
		}	
	}	