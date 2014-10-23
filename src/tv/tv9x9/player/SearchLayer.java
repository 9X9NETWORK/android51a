package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;

import tv.tv9x9.player.main.toplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SearchLayer extends StandardFragment implements StoreAdapter.mothership
	{
	metadata config = null;
		
	boolean search_initialized = false;
	
	StoreAdapter search_adapter = null;
	
	String search_channels[] = null;
	
	View vSearchContainer = null;
		
    public interface OnSearchListener
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
    	
    	public void enable_search_layer();
    	public StoreLayer store_class();
    	public void track_event (String category, String action, String label, long value);
    	public void signout_from_app_or_facebook();
    	public void load_channel_then (final String channel_id, final boolean allow_cache, final Callback callback, final String arg1, final Object arg2);
    	public void alert_then_exit (String text);
		}    
    
    OnSearchListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.search_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnSearchListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnSearchListener");
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
    
	public void enable_search_apparatus (metadata config, final View vContainer)
		{
		this.config = config;
		vSearchContainer = vContainer;
		
		if (vContainer != null)
			{
			View vSearchContainer = vContainer.findViewById(R.id.search_container);
			if (vSearchContainer.getVisibility() == View.VISIBLE)
				{
				disable_search_apparatus (vContainer);
				return;
				}
			
			View vLogo = vContainer.findViewById (R.id.logo);
			// View vPhantomRefresh = vContainer.findViewById (R.id.phantom_refresh);
			
			vLogo.setVisibility (View.GONE);
			// vPhantomRefresh.setVisibility (View.GONE);
			vSearchContainer.setVisibility (View.VISIBLE);
			
			final EditText vTerm = (EditText) vContainer.findViewById (R.id.term);
			
			View vCancel = vContainer.findViewById (R.id.search_cancel);
			vCancel.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: search cancel");
		        	// disable_search_apparatus (view_id);
		        	if (vTerm != null)
		        		vTerm.setText ("");
		        	}
				});
			
		    vTerm.setOnKeyListener (new OnKeyListener()
			    {
				@Override
				public boolean onKey (View v, int keyCode, KeyEvent event)
					{
					if (event.getAction() == KeyEvent.ACTION_UP)
						{					
						if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
							{
							String term = vTerm.getText().toString();
	
							vTerm.clearFocus();						
							InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE);
							imm.hideSoftInputFromWindow (vTerm.getWindowToken(), 0);
							
				        	perform_search (term);
							return true;
							}
						}
					return false;
					}						
			    });
			}
		}
	
	public void disable_search_apparatus (View vContainer)
		{
		if (vContainer != null)
			{
			View vSearchContainer = vContainer.findViewById (R.id.search_container);
			vSearchContainer.setVisibility (View.INVISIBLE);
			View vLogo = vContainer.findViewById (R.id.logo);
			// View vPhantomRefresh = vContainer.findViewById (R.id.phantom_refresh);
			vLogo.setVisibility (View.VISIBLE);
			// vPhantomRefresh.setVisibility (View.INVISIBLE);
			}
		}
	
	public void search_init (metadata config)
		{
		this.config = config;
		if (!search_initialized)
			{
			search_initialized = true;
			search_adapter = new StoreAdapter (getActivity(), (StoreAdapter.mothership) this, config, -1, null, search_channels);
			AbsListView vSearch = (AbsListView) getView().findViewById (is_tablet() ? R.id.search_list_tablet : R.id.search_list_phone);
			vSearch.setAdapter (search_adapter);
			}
		}

	public void setup_search_buttons()
		{
		View vSearchLayer = getView().findViewById (R.id.searchlayer);		
		if (vSearchLayer != null)
			vSearchLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("search layer ate my tap!");
		        	}
				});				
		}

	boolean search_9x9_done = false;
	boolean search_youtube_done = false;
	
	String channel_ids_9x9[] = null;
	String channel_ids_youtube[] = null;
	
	public void perform_search (final String term)
		{
		if (term.startsWith ("server="))
			{
			String fields[] = term.split ("=");
			if (fields.length >= 2)
				{
				mCallback.signout_from_app_or_facebook();
				config.api_server = fields[1] + ".flipr.tv";
				log ("switching API server to: " + fields[1]);
				String filedata = "api-server\t" + config.api_server + "\n" + "region\t" + config.region + "\n";
	            futil.write_file (getActivity(), "config", filedata);
				mCallback.alert_then_exit ("Please restart the app, to use API server " + config.api_server);
				return;
				}
			}
		else if (term.matches("^#\\d+"))
			{
			search_only_for_channel (term.replace ("#", ""));
			return;
			}
		
		String encoded_term = util.encodeURIComponent (term);
	
		if (encoded_term == null)
			return;
	
		mCallback.store_class().store_spinner (true);
		prepare_search_screen (term);
		
		search_9x9_done = search_youtube_done = false;
		channel_ids_9x9 = null;
		channel_ids_youtube = null;
		
		mCallback.track_event ("function", "submitSearch", term, 0);
				
		new playerAPI (mCallback.get_main_thread(), config, "search?text=" + encoded_term)
			{
			public void success (String[] chlines)
				{
				int count = 0;
	
				log ("search lines received: " + chlines.length);
	
				mCallback.store_class().store_spinner (false);
				
				int section = 0;
				int num_channels = 0;
				
				for (String s: chlines)
					{
					if (s.equals ("--"))
						{
						section++;
						continue;
						}
					if (section == 3) /* or 2?? */
						{
						config.parse_channel_info_line (s);
						num_channels++;
						}
					}
				
				String channel_ids[] = new String [num_channels];
				
				section = 0;
				count = 0;
				
				for (String s: chlines)
					{
					if (s.equals ("--"))
						{
						section++;
						continue;
						}
					if (section == 3)
						{
						String[] fields = s.split ("\t");			
						log ("__search channel (9x9): " + fields[1] + " == " + config.pool_meta(fields[1], "extra"));
						channel_ids [count] = fields[1];
						count++;
						}
					}
			
				search_9x9_done = true;
				channel_ids_9x9 = channel_ids;
				
				search_is_ready();
				}
	
			public void failure (int code, String errtext)
				{
				Log.i ("vtest", "ERROR! " + errtext);
				search_9x9_done = true;
				search_is_ready();
				}
			};
			
		ytchannel.youtube_channel_search_in_thread (config, encoded_term, mCallback.get_main_thread(), new Callback()
			{
			public void run_string_array (String a[])
				{
				if (a != null)
					{
					for (String channel: a)
						{
						log ("__search channel (youtube): " + channel);
						}
					}
				search_youtube_done = true;
				channel_ids_youtube = a;
				search_is_ready();
				}
			});
		}
	
	public void search_is_ready()
		{		
		if (search_9x9_done && search_youtube_done)
			{
			if (channel_ids_9x9 != null && channel_ids_youtube == null)
				{
				log ("search is ready: only 9x9 channels came back");
				search_channels = channel_ids_9x9;
				}
			else if (channel_ids_9x9 == null && channel_ids_youtube != null)
				{
				log ("search is ready: only youtube channels came back");
				search_channels = channel_ids_youtube;
				}
			else if (channel_ids_9x9 == null && channel_ids_youtube == null)
				{
				log ("search is ready: NO channels came back");
				search_channels = new String [0];
				}
			else
				{	
				Set <String> youtubes = new HashSet <String> ();
				
				/* make a list of all the YouTube usernames in the 9x9 result portion */
				for (String id: channel_ids_9x9)
					{
					// if (config.is_youtube (id))  <-- Unfortunately some of the channels are mirrored, and show up as type 6 (a server bug?)
						{
						String extra = "=" + config.pool_meta (id, "extra").toLowerCase();
						log ("9x9 youtube channel " + id + " => " + extra);
						youtubes.add (extra);
						}
					}
								
				int true_num_youtubes = 0;
				
				/* how many YouTube channels are actually not in the 9x9 results */
				if (channel_ids_youtube != null)
					for (String id: channel_ids_youtube)
						{
						if (!youtubes.contains (id.toLowerCase()))
							true_num_youtubes++;
						}
				
				/* merge */
				int count = 0;
				String search_channels_new[] = new String [channel_ids_9x9.length + true_num_youtubes];
				
				for (int i = 0; i <=  java.lang.Math.max (channel_ids_9x9.length, channel_ids_youtube.length); i++)
					{
					if (i < channel_ids_9x9.length)
						search_channels_new [count++] = channel_ids_9x9 [i];	
					if (i < channel_ids_youtube.length)
						{
						String id = channel_ids_youtube [i];
						if (!youtubes.contains (id.toLowerCase()))
							search_channels_new [count++] = id;
						else
							log ("omitted because it is already a 9x9 channel: " + id);
						}
					}				
				log ("search is ready: merge 9x9 and youtube channels");
				search_channels = search_channels_new;
				}
			
			search_adapter.set_content (-1, null, search_channels);
			search_adapter.notifyDataSetChanged();
			
			redraw_search_list();
			
			thumbnail.stack_thumbs
				(getActivity(), config, search_channels, mCallback.screen_width() / 5, mCallback.get_main_thread(), search_channel_thumb_updated);
			}
		else
			log ("search_is_ready? search_9x9_done " + search_9x9_done + ", search_youtube_done " + search_youtube_done);
		}
	
	public void search_only_for_channel (String channel_id)
		{
		log ("special search for channel: " + channel_id);
		prepare_search_screen ("Channel " + channel_id);
		search_channels = new String[] { channel_id };
		redraw_search_list();
		
		final Callback search_channel_special_inner = new Callback()
			{
			public void run_string_and_object (String channel_id, Object o)
				{
				log ("search only loaded");
				
				search_adapter.set_content (-1, null, search_channels);
				search_adapter.notifyDataSetChanged();
				
				redraw_search_list();
				
				thumbnail.stack_thumbs
					(getActivity(), config, search_channels, mCallback.screen_width() / 5, mCallback.get_main_thread(), search_channel_thumb_updated);
				}
			};
		
		mCallback.load_channel_then (channel_id, false, search_channel_special_inner, channel_id, null);
		}
	
	public void redraw_search_list()
		{
		AbsListView vSearch = (AbsListView) getView().findViewById (is_tablet() ? R.id.search_list_tablet : R.id.search_list_phone);			
		vSearch.setOnItemClickListener (new OnItemClickListener()
			{
			public void onItemClick (AdapterView <?> parent, View v, int position, long id)
				{
				if (position < search_channels.length)
					{
					log ("search click: " + position);
					String channel_id = search_channels [position];
					launch_player (channel_id, search_channels);
					}
				}
			});
		}
	
	public void search_refresh()
		{
		if (search_adapter != null)
			search_adapter.notifyDataSetChanged();
		}
	
	public void prepare_search_screen (String term)
		{
		mCallback.enable_search_layer();
		enable_search_apparatus (config, vSearchContainer);
		String txt_searched_for = getResources().getString (R.string.searched_for_colon);
		TextView vTermUsed = (TextView) getView().findViewById (R.id.search_term_used);
		vTermUsed.setText (txt_searched_for + " " + term);
		}
	
	final Runnable search_channel_thumb_updated = new Runnable()
		{
		public void run()
			{
			if (search_adapter != null)
				search_adapter.notifyDataSetChanged();
			}
		};
	
        
	
		
	/**** mothership ****/
	
	@Override
	public void load_category (final int index, final int starting)
		{
		/* only for StoreLayer */
		}
	
	@Override
	public boolean outgoing_category_queries_pending()
		{
		/* only for StoreLayer */
		return false;
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