package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import tv.tv9x9.player.main.toplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class StoreLayer extends StandardFragment implements StoreAdapter.mothership
	{
	metadata config = null;
	
    boolean store_initialized = false;
    
	int category_stride = 27;
	int category_total_channels = 0;
	
	String category_channels[] = null;
	
	/* current category shown */
	int current_category_index = -1;
	
	/* the final category channel (a spinner) has been exposed, and the next step is being obtained */
	boolean outgoing_category_query = false;
	
	StoreAdapter store_adapter = null;
	
    public interface OnStoreListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
        public void launch_player (String channel_id, String channels[]);
        public void launch_player (String channel_id, String episode_id, String channels[]);
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void track_layer (toplayer layer);
    	public void close_menu();
    	public void alert (String text);
    	public void follow_or_unfollow (String channel_id, View v);
    	public int screen_width();
    	public void share_episode (String channel_id, String episode_id);
    	public void set_follow_icon_state (View v, String channel_id, int follow_resource_id, int unfollow_resource_id);
    	public void parse_special_tags (String type, String tags, String set_id);
		}    
    
    OnStoreListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.store_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnStoreListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnMessagesListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "messages";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
    
	public void store_init (final metadata config)
		{
		this.config = config;
		
		if (!store_initialized)
			{
			store_initialized = true;
			
			/* this is a ListView */
			View vStoreListPhone = getView().findViewById (R.id.store_list_phone);
			vStoreListPhone.setVisibility (is_tablet() ? View.GONE : View.VISIBLE);
			
			/* this is a GridView */
			View vStoreListTablet = getView().findViewById (R.id.store_list_tablet);
			vStoreListTablet.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);
			
			AbsListView vStore = (AbsListView) getView().findViewById (mCallback.is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
			// String category_id = category_list [current_category_index];
			// store_adapter = new StoreAdapter (this, (StoreAdapter.mothership) this, config, current_category_index, category_id, category_channels);
			store_adapter = new StoreAdapter (getActivity(), (StoreAdapter.mothership) this, config, -1, null, new String [0]);
			vStore.setAdapter (store_adapter);
			}
		}

	public void setup_store_buttons()
		{
		View vCategoryName = getView().findViewById (R.id.category_handle);
		if (vCategoryName != null)
			vCategoryName.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	toggle_category_layer();
		        	}
				});	
		
		View vStore = getView().findViewById (R.id.storelayer);		
		if (vStore != null)
			vStore.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("store layer ate my tap!");
		        	}
				});				
		}

	/* full refresh */
	public void store_refresh()
		{
		config.init_query_cache();
		top_categories();
		}
	
	/* partial refresh, such as a channel subscription update */
	public void refresh_store_data()
		{
		if (store_adapter != null)
			store_adapter.notifyDataSetChanged();	
		}
	
	public void toggle_category_layer()
		{
		View vCategoryLayer = getView().findViewById (R.id.category_layer);
		boolean is_visible = vCategoryLayer.getVisibility() == View.VISIBLE;
		vCategoryLayer.setVisibility (is_visible ? View.GONE : View.VISIBLE);
		}
	
	public void category_click (String id)
		{
		for (int i = 0; i < category_list.length; i++)
			if (category_list [i].equals (id))
				{
				load_category (i, 0);
				mCallback.close_menu();
				mCallback.track_layer (toplayer.STORE);
				}
		}
	
	public void top_categories()
		{
		final String query = "category";
				
		if (config.query_cache.get (query) != null)
			{
			log ("top_categories: using cached data");
			top_categories_inner (config.query_cache.get (query));
			return;
			}
		
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (mCallback.get_main_thread(), config, query)
			{
			public void success (String[] lines)
				{
				try
					{
					long vc_end = System.currentTimeMillis();
					long elapsed = (vc_end - vc_start) / 1000L;
					log ("[" + query + "] lines received: " + lines.length + ", elapsed: " + elapsed);
					config.query_cache.put (query, lines);
					top_categories_inner (lines);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					getActivity().finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				mCallback.alert ("ERROR! " + errtext);
				}
			};		
		}
	
	LineItemAdapter category_adapter = null;
	
	String category_list[] = null;
	
	/* right now we only need names */
	Hashtable < String, String > category_names = new Hashtable < String, String > ();
	
	ListView vCategories = null;
	
	public void top_categories_inner (String[] lines)
		{
		int section = 0;
		int num_categories = 0;
		
		for (String line: lines)
			{
			if (line.equals ("--"))
				section++;
			else if (section == 1)
				num_categories++;
			}
		
		if (num_categories < 1)
			{
			String txt_no_store = getResources().getString (R.string.store_not_available);
			mCallback.alert (txt_no_store);
			return;
			}
		
		category_list = new String [num_categories];
		
		int count = 0;
		section = 0;
		
		for (String line: lines)
			{
			log ("top category. LINE: " + line);
			if (line.equals ("--"))
				section++;
			else if (section == 1)
				{
				String fields[] = line.split ("\t");
				category_list [count++] = fields [0];
				category_names.put (fields[0], fields[1]);
				log ("category " + fields[0] + " => " + fields[1]);
				}
			}
		
		vCategories = (ListView) getView().findViewById (R.id.category_list);		
		if (vCategories != null)
			{
			category_adapter = new LineItemAdapter (getActivity(), category_list, toplayer.STORE);
			vCategories.setAdapter (category_adapter);
			}
		
		if (vCategories != null)
			vCategories.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView parent, View v, int position, long id)
					{
					if (position < category_list.length)
						{
						log ("category click: " + position);
						load_category (position, 0);
						toggle_category_layer();
						}
					}
				});					
				
		mCallback.redraw_menu();
		
		/* load the first category */
		load_category (0, 0);		
		}
	
	public boolean outgoing_category_queries_pending()
		{
		return outgoing_category_query;
		}
	
	public void load_category (final int index, final int starting)
		{
		/* http://player.9x9.tv/playerAPI/categoryInfo?category=6&lang=en&start=30&count=3 */	
		
		outgoing_category_query = true;
		
		final AbsListView vStore = (AbsListView) getView().findViewById (is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
		final View vSpinner = getView().findViewById (R.id.store_progress);
		
		if (starting == 0)
			vStore.setSelection (0);
		
		String category_id = category_list [index];	
		set_store_category_name (category_id);
		
		final String query = "categoryInfo?category=" + category_id
				+ "&region=" + config.region + "&count=" + category_stride + "&start=" + starting;
		
		if (config.query_cache.get (query) != null)
			{
			log ("load category " + category_id + ": using cached data");
			load_category_inner (index, starting, config.query_cache.get (query));
			return;
			}
				
		if (starting == 0)
			{
			vStore.setVisibility (View.GONE);
			vSpinner.setVisibility (View.VISIBLE);
			}
		
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (mCallback.get_main_thread(), config, query)
			{
			public void success (String[] lines)
				{
				try
					{
					vStore.setVisibility (View.VISIBLE);
					vSpinner.setVisibility (View.GONE);
					store_spinner (false);
					long vc_end = System.currentTimeMillis();
					long elapsed = (vc_end - vc_start) / 1000L;
					log ("[" + query + "] lines received: " + lines.length + ", elapsed: " + elapsed);
					config.query_cache.put (query, lines);
					load_category_inner (index, starting, lines);
					}
				catch (Exception ex)
					{
					vStore.setVisibility (View.VISIBLE);
					vSpinner.setVisibility (View.GONE);
					store_spinner (false);
					ex.printStackTrace();
					getActivity().finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				mCallback.alert ("ERROR! " + errtext);
				}
			};		
		}
	
	public void store_spinner (boolean visible)
		{
		// vStore.setVisibility (visible ? View.GONE : View.VISIBLE);
		}
	
	public void load_category_inner (int index, int starting, String lines[])
		{
		String category_id = category_list [index];
		
		int section = 0;
		int count = 0;
		int expected_count = 0;
		
		/* "total" and "stride" are presently unreliable, so count the lines by iteration */
		for (String line: lines)
			{
			if (line.equals ("--"))
				section++;
			else if (section == 0)
				{
				log ("KV: " + line);
				String fields[] = line.split ("\t");
				if (fields.length >= 2)
					{
					if (fields[0].equals ("count"))					
						/* category_stride = Integer.parseInt (fields[1]) */ ;
					else if (fields[0].equals ("total"))
						category_total_channels = Integer.parseInt (fields[1]);
					else if (fields[0].equals ("channeltag"))
						mCallback.parse_special_tags ("store", fields[1], category_id);
					}
				}
			else if (section == 2)
				{
				String fields[] = line.split ("\t");
				if (fields.length > 5)
					expected_count++;
				}
			}
		/* prevent any crazy -- when higher it could download over a thousand thumbnails */
		if (expected_count > category_stride)
			{
			log ("too many channels returned for this category! restricting to " + category_stride);
			expected_count = category_stride;
			}
		
		int allocation = 1 + starting + expected_count;
		if (allocation > category_total_channels)
			allocation = category_total_channels;
		String new_category_channels[] = new String [allocation];
				
		for (int i = 0; i < starting; i++)
			new_category_channels [i] = category_channels [i];
	
		section = 0;
		count = starting;
		
		for (String line: lines)
			{
			// Log.i ("vtest", "load category inner LINE: " + line); // noisy
			if (line.equals ("--"))
				section++;
			else if (section == 2)
				{
				String fields[] = line.split ("\t");
				// Log.i ("vtest", "[store] in category " + category_id + ", channel: " + fields[1]); // noisy
				config.parse_channel_info_line (line);
				if (count < new_category_channels.length)
					new_category_channels [count++] = fields[1];
				}
			}
		
		/* sentinel to indicate more channels available via stepping API */
		if (allocation == 1 + starting + expected_count)
			new_category_channels [count++] = "+";
		
		category_channels = new_category_channels;
		
		log ("load category inner (" + category_id + "): " + expected_count + " channels, total now: " + allocation);
				
		current_category_index = index;
		outgoing_category_query = false;
		
		for (int i = 0; i < category_channels.length; i++)
			log ("CATCH load_cat" + i + ": " + category_channels [i]);
	
		store_adapter.set_content (current_category_index, category_id, category_channels);	
		redraw_store_list();
		
		thumbnail.stack_thumbs (getActivity(), config, category_channels, -1, mCallback.get_main_thread(), store_channel_thumb_updated);
	
		if (category_adapter != null)
			category_adapter.notifyDataSetChanged();
			
		set_store_category_name (category_id);
		
		AbsListView vStore = (AbsListView) getView().findViewById (is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
		vStore.setVisibility (View.VISIBLE);
		
		View vSpinner = getView().findViewById (R.id.store_progress);
		vSpinner.setVisibility (View.GONE);
		}
	
	final Runnable store_channel_thumb_updated = new Runnable()
		{
		public void run()
			{
			if (store_adapter != null)
				store_adapter.notifyDataSetChanged();
			}
		};
	
	public void set_store_category_name (String category_id)
		{
		String name = category_names.get (category_id);			
		String txt_category_colon = getResources().getString (R.string.categorycolon);
		
		TextView vCategoryName = (TextView) getView().findViewById (R.id.category_name);
		if (vCategoryName != null)
			{
			vCategoryName.setText (txt_category_colon + " " + name);
			if (mCallback.is_phone())
				vCategoryName.setTextSize (TypedValue.COMPLEX_UNIT_SP, 18);
			}
		}
	public void redraw_store_list()
		{	
		}
		
	class LineItemAdapter extends ArrayAdapter <String>
		{
		Activity context;
		String list[] = null;
		toplayer layer = null;
	
		LineItemAdapter (Activity context, String list[], toplayer layer)
			{
			super (context, R.layout.category_item, list);
			this.context = context;
			this.layer = layer;
			set_content (list);
			}
	
		public void set_content (String list[])
			{
			/* if the first element is null, generate a new array without it */
			if (list.length > 0 && list[0] == null)
				list = Arrays.copyOfRange (list, 1, list.length);
			
			log ("LineItemAdapter " + layer.toString() + " changed (" + list.length + " entries)");
			this.list = list;
			notifyDataSetChanged();
			}
		
		public String get_id (int position)
			{
			return list [position];
			}
		
		@Override
		public int getCount()
			{
			return list.length;
			}
		
		@Override
		public View getView (int position, View convertView, ViewGroup parent)
			{
			log ("lineitem getView: " + position);
			
			if (position >= getCount())
				{
				Log.e ("vtest", "race condition! position=" + position + " but list.length is " + list.length);
				return null;
				}
			
			View row = convertView;			
			
			/* the recycled views have issues with ellipse, and these are cheap, so issue a fresh one each time */
			if (true || row == null)
				{
				row = getActivity().getLayoutInflater().inflate (R.layout.category_item, null);
				}
			
			String name = null;			
	
			String id = list [position];
			
			if (layer == toplayer.STORE)
				name = category_names.get (id);
			else if (layer == toplayer.HOME)
				name = config.pool_meta (id, "name");
			
			TextView vTitle = (TextView) row.findViewById (R.id.title);
			vTitle.setText (name != null ? name : "?");
	
			if (mCallback.is_phone())		
				{
				vTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 24);
				// LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTitle.getLayoutParams();
				// layout.height = pixels_70;
				// vTitle.setLayoutParams (layout);
				}
			
			return row;
			}
		}
	
	@Override
	public void follow_or_unfollow (String channel_id, View v)
		{
		mCallback.follow_or_unfollow (channel_id, v);
		}
	
	@Override
	public File	getFilesDir()
		{
		return getActivity().getFilesDir();
		}
	
	@Override
	public int screen_width()
		{
		return mCallback.screen_width();
		}
	
	@Override
	public int actual_pixels (int dp)
		{
		return mCallback.actual_pixels (dp);
		}
	
	@Override
    public void launch_player (String channel_id, String channels[])
		{
		mCallback.launch_player (channel_id, channels);
		}
	
	@Override
	public boolean is_tablet()
		{
		return mCallback.is_tablet();
		}
	
	@Override
	public void share_episode (String channel_id, String episode_id)
		{
		mCallback.share_episode (channel_id, episode_id);
		}
	
	@Override
	public void set_follow_icon_state (View v, String channel_id, int follow_resource_id, int unfollow_resource_id)
		{
		mCallback.set_follow_icon_state (v, channel_id, follow_resource_id, unfollow_resource_id);
		}
	}