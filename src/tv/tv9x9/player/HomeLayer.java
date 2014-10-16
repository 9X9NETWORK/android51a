package tv.tv9x9.player;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import tv.tv9x9.player.main.toplayer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class HomeLayer extends StandardFragment
	{
	metadata config = null;
		
	/* style for home page, with 4 thumbs (false), or just 1 (true) */
	boolean mini_mode = false;
	
	HomeSlider home_slider = null;
	StoppableViewPager vHomePager = null;
	
	String portal_stack_ids[] = null;
	String portal_stack_names[] = null;
	String portal_stack_episode_thumbs[] = null;
	String portal_stack_channel_thumbs[] = null;	
	String portal_stack_banners[] = null;
	
    public interface OnHomeListener
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
    	public void alert (String text);
    	public toplayer get_current_layer();
    	public void share_episode (String channel_id, String episode_id);
    	public void update_metadata_inner();
    	public void follow_or_unfollow (String channel_id, View v);
    	public void set_follow_icon_state (View v, String channel_id, int follow_resource_id, int unfollow_resource_id);
    	public void clear_metadata();
    	public void load_channel_then (final String channel_id, final boolean allow_cache, final Callback callback, final String arg1, final Object arg2);
    	public boolean fill_in_episode_thumb (String episode_id, View parent, int resource_id, int title_resource_id, boolean use_blank);
    	public boolean get_hint_setting (String hint);
    	public void set_hint_setting (String hint, boolean value);
    	public void set_player_real_channel (String channel_id);
    	public int subscription_changes_this_session();
    	public void query_pile (String id, Callback callback);
    	public void exit_stage_left();
    	public void parse_special_tags (String type, String tags, String set_id);
		}    
    
    OnHomeListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.home_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnHomeListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnHomeListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "home";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
	
    public void init (metadata config)
	    {
    	this.config = config;
    	
		if (home_slider == null)
			{
			log ("+++++++++++++++++++++++++++++++ new HomeSlider +++++++++++++++++++++++++++++++++++");
		    home_slider = new HomeSlider();
		    vHomePager = (StoppableViewPager) getView().findViewById (R.id.homepager);
		    vHomePager.setAdapter (home_slider);
		    if (portal_stack_ids.length > 1)
		    	{
		    	int unique_sets = portal_stack_ids.length / 100;
		    	int start_set = unique_sets * 50;
		    	log ("start set: " + start_set);
		    	vHomePager.setCurrentItem (start_set);
		    	}
			}	
		
		setup_home_buttons();
	    }
	
    public void refresh()
    	{
    	if (home_slider != null)
    		home_slider.refresh();
    	}
    
	public void setup_home_buttons()
		{
		View vHomeLayer = getView();
		if (vHomeLayer != null)
			vHomeLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("home layer ate my tap!");
		        	}
				});					
		}
    
	public boolean have_portal_stack()
		{
		return portal_stack_ids != null;
		}
	
	
	
	
	
	
	
	public void process_portal_frontpage_data (String[] lines)
		{
		/* first count the number of stacks */
		
		int num_stacks = 0;
		for (String line: lines)
			{
			if (line.equals ("--"))
				break;
			num_stacks++;
			}
		
		portal_stack_ids = new String [num_stacks];
		portal_stack_names = new String [num_stacks];
		portal_stack_episode_thumbs = new String [num_stacks];
		portal_stack_channel_thumbs = new String [num_stacks];
		portal_stack_banners = new String [num_stacks];
		
		int section = 0;		
		int stack_count = 0;
		
		for (String line: lines)
			{
			if (line.equals ("--"))
				{
				section++;
				continue;
				}
			String fields[] = line.split ("\t");
			if (section == 0 && fields.length >= 2)
				{
				/* sets */
				portal_stack_ids [stack_count] = fields [0];
				portal_stack_names [stack_count] = fields [1];
				if (fields.length >= 4)
					portal_stack_episode_thumbs [stack_count] = fields [3];
				if (fields.length >= 6)
					portal_stack_channel_thumbs [stack_count] = fields [5];
				if (fields.length >= 7)
					portal_stack_banners [stack_count] = fields [6];
				if (fields.length >= 9)
					mCallback.parse_special_tags ("set", fields[8], fields[0]);
				stack_count++;
				log ("frontpage :: " + fields[0] + ": " + fields[1]);
				}
			else if (section == 1)
				{
				/* channels from first set */
				// add_channel_to_set (virtual_channel_id, s, ++scount);
				}
			else if (section == 2)
				{
				/* programs, from potentially all channels in section 1 */
				// parse_program_info_32_line (virtual_channel_id, pcount++, s);
				}
			}
		
		String original_portal_stack_ids[] = portal_stack_ids;
		String original_portal_stack_banners[] = portal_stack_banners;
		
		if (portal_stack_ids.length > 1)
			{
			/* makin' copies: the only sensible way to loop around the home pages is to use ViewPager with
			   duplicate data repeated. The user will never reach the end unless she is a nutter. */
			
			int repeat = 100;
			int new_num_stacks = repeat * num_stacks;
			
			String new_portal_stack_ids[] = new String [new_num_stacks];
			String new_portal_stack_names[] = new String [new_num_stacks];
			String new_portal_stack_episode_thumbs[] = new String [new_num_stacks];
			String new_portal_stack_channel_thumbs[] = new String [new_num_stacks];
			String new_portal_stack_banners[] = new String [new_num_stacks];
			
			int count = 0;
			
			for (int i = 0; i < repeat; i++)
				for (int j = 0; j < num_stacks; j++)
					{
					new_portal_stack_ids [count] = portal_stack_ids [j];
					new_portal_stack_names [count] = portal_stack_names [j];
					new_portal_stack_episode_thumbs [count] = portal_stack_episode_thumbs [j];
					new_portal_stack_channel_thumbs [count] = portal_stack_channel_thumbs [j];
					new_portal_stack_banners [count] = portal_stack_banners [j];
					count++;
					}
			
			/*
			portal_stack_ids = new_portal_stack_ids;
			portal_stack_names = new_portal_stack_names;
			portal_stack_episode_thumbs = new_portal_stack_episode_thumbs;
			portal_stack_channel_thumbs = new_portal_stack_channel_thumbs;		
			portal_stack_banners = new_portal_stack_banners;
			*/
			}
		
		create_set_slider();
		
		final Runnable update = new Runnable()
			{
			@Override
			public void run()
				{
				home_slider_data_set_changed();
				}
			};
		
		if (mCallback.is_tablet())
			{
			thumbnail.download_set_banners (getActivity(), config, 
					original_portal_stack_ids, original_portal_stack_banners, 
						mCallback.get_main_thread(), update);
			}
		}	


	long frontpage_start = 0L;

	public void portal_frontpage (metadata config)
		{
		this.config = config;
		
		/* the start activity may have already one this network retrieval */
		if (config.portal_api_cache != null)
			{
			log ("using prefetched portal API");
			process_portal_frontpage_data (config.portal_api_cache);
			config.portal_api_cache = null;
			portal_frontpage_ii();
			return;
			}
		
		Calendar now = Calendar.getInstance();
		int hour = now.get (Calendar.HOUR_OF_DAY);
		
		frontpage_start = System.currentTimeMillis();
		
		String type = config.homepage_style_override != null ? config.homepage_style_override : config.homepage_style; // "portal", "whatson"
		
		new playerAPI (mCallback.get_main_thread(), config, "portal?time=" + hour + "&type=" + type + "&minimal=true")
			{
			public void success (String[] lines)
				{
				if (lines.length < 1)
					{
					mCallback.alert ("Frontpage failure");
					}
				else
					{
					long frontpage_end = System.currentTimeMillis();
					
					log ("frontpage API took: " + ((frontpage_end - frontpage_start) / 1000L) + " seconds");
	
					frontpage_start = System.currentTimeMillis() / 1000L;
					process_portal_frontpage_data (lines);
					portal_frontpage_ii();
					}
				}
			public void failure (int code, String errtext)
				{
				// when our server was taken down by YiWen once:
				// org.apache.http.client.HttpResponseException: Service Temporarily Unavailable
				log ("frontpage error: " + errtext);
				if (errtext.contains ("ConnectException") || errtext.contains ("UnknownHostException"))
					network_retry();
				else
					mCallback.alert ("Frontpage failure: " + errtext.replaceAll ("^ERROR:", ""));
				}
			};
		}

	public void portal_frontpage_ii()
		{
		if (mCallback.get_current_layer() != toplayer.PLAYBACK && mCallback.get_current_layer() != toplayer.NAG)
			{
			/* only if we haven't gone to playback, such as a share or notify */
			mCallback.enable_home_layer();
			}
		}
	
	public void network_retry()
		{
		AlertDialog.Builder builder = new AlertDialog.Builder (getActivity());
		
		String txt_not_ready = getResources().getString (R.string.network_not_ready);
		String txt_retry = getResources().getString (R.string.retry);
		String txt_quit = getResources().getString (R.string.quit);
		
		builder.setMessage (txt_not_ready);
		builder.setPositiveButton (txt_retry, new DialogInterface.OnClickListener()
			{
			@Override
			public void onClick (DialogInterface arg0, int arg1)
				{
				portal_frontpage (config);
				}
			});
		
		builder.setNeutralButton (txt_quit, new DialogInterface.OnClickListener()
			{
			@Override
			public void onClick (DialogInterface arg0, int arg1)
				{
				mCallback.exit_stage_left();
				}
			});
	
		AlertDialog dialog = builder.create();
		
		try
			{
			dialog.show();	
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mCallback.alert ("Unable to connect to network");
			}
		}	
	
	
	
	

	
	
	
	
	public void refresh_home()
		{
		log ("refresh home");
		config.query_cache = new Hashtable < String, String[] > ();
		
		if (home_slider != null)
			home_slider.reload_data();
		}
	
	public void refresh_home_slider()
		{
		if (home_slider != null)
			home_slider.refresh();
		}
	
	public void home_slider_data_set_changed()
		{
		if (home_slider != null)
			home_slider.notifyDataSetChanged();
		}
	
	class Swaphome
		{
		int set = 0;
		FrameLayout home_page = null;
		String arena[] = null;
		ListView vChannelOverlayList = null;
		StoppableListView vChannels = null;
		ChannelAdapter channel_adapter = null;
		String set_id = null;
		public Swaphome (int a_set)
			{
			set = a_set;
			}
		boolean shim_added = false;
		int subscription_change_count = 0;
		};
		
	/* this is implemented using the base class! */
		
	Swaphome current_swap_object = null;
		
	public class HomeSlider extends PagerAdapter
		{
		boolean first_time = true;
	
		FrameLayout current_home_page = null;
		
	    @Override
	    public int getCount()
	    	{
	        return portal_stack_ids == null ? 0 : portal_stack_ids.length;
	    	}
	
		@Override
		public boolean isViewFromObject (View view, Object object)
			{
			return (((Swaphome) object).home_page) == (FrameLayout) view;
			}
		
		@Override
		public Object instantiateItem (final ViewGroup container, final int position)
			{
			log ("[PAGER] instantiate: " + position);
			
			final Swaphome sh = new Swaphome (position);			
			sh.set_id = portal_stack_ids [position];
			sh.subscription_change_count = mCallback.subscription_changes_this_session();
			
			FrameLayout home_page = (FrameLayout) View.inflate (getActivity(), R.layout.home_page, null);
			
			View vTabletPreamble = home_page.findViewById (R.id.tablet_preamble);
			vTabletPreamble.setVisibility (mCallback.is_tablet() ? View.VISIBLE : View.GONE);
	
			if (mCallback.is_tablet())
				{
				boolean banner_found = false;
				ImageView vSetBanner = (ImageView) home_page.findViewById (R.id.set_banner);
				
				if (vSetBanner != null)
					{
					String filename = getActivity().getFilesDir() + "/" + config.api_server + "/bthumbs/" + sh.set_id + ".png";				
					File f = new File (filename);
					if (f.exists())
						{
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							{
							banner_found = true;
							vSetBanner.setImageBitmap (bitmap);
							}
						else
							log ("set banner exists but bitmap is null: " + filename);
						}
					else
						log ("set banner does not exist: " + sh.set_id);
					
					if (!banner_found)
						{
						String set_banner = getResources().getString (R.string.set_banner);
						int set_banner_id = getResources().getIdentifier (set_banner, "drawable", getActivity().getPackageName());
						vSetBanner.setBackgroundResource (set_banner_id);
						}
					}
				}
			
			View vChannelList = home_page.findViewById (R.id.channel_list);
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vChannelList.getLayoutParams();
			if (mCallback.is_tablet())
				{
				layout.leftMargin = 0;
				layout.rightMargin = 0;
				}
			else
				{
				layout.topMargin = mCallback.actual_pixels (6);
				}
			vChannelList.setLayoutParams (layout);
			
			if (mCallback.is_tablet() && sh.channel_adapter != null)
				set_mini_mode_thumbs (sh.home_page);
			
			home_page.setTag (R.id.container, position);			
			sh.home_page = home_page;
			((StoppableViewPager) container).addView (home_page, 0);
			
			TextView vBannerSetTitle = (TextView) sh.home_page.findViewById (R.id.banner_set_title);
			if (vBannerSetTitle != null)
				vBannerSetTitle.setText (portal_stack_names [position]);
			
			View vModeThumbs = sh.home_page.findViewById (R.id.mode_thumbs);
			if (vModeThumbs != null)
				vModeThumbs.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: thumb mode");
			        	mini_mode = false;
			        	sh.channel_adapter.notifyDataSetChanged();
			        	set_mini_mode_thumbs (sh.home_page);
			        	}
					});	
			
			View vModeList = sh.home_page.findViewById (R.id.mode_list);
			if (vModeList != null)
				vModeList.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: list mode");
			        	mini_mode = true;
			        	sh.channel_adapter.notifyDataSetChanged();
			        	set_mini_mode_thumbs (sh.home_page);
			        	}
					});				
			
			load_data (sh);
								
			return sh;
			}
		
		@Override
		public void destroyItem (ViewGroup container, int position, Object object)
			{
			log ("[PAGER] destroy: " + position);
			Swaphome sh = (Swaphome) object;
			((StoppableViewPager) container).removeView (sh.home_page);
			}
		
		@Override
		public void setPrimaryItem (ViewGroup container, int position, Object object)
			{
			Swaphome sh = (Swaphome) object;
			// log ("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% primary 3x3: " + position); // noisy!
			current_swap_object = sh;
			if (sh != null)
				{
				current_home_page = sh.home_page;
	
				if (sh.channel_adapter != null)
					{
					boolean need_refresh = false;
					
					/* the mini mode might have changed */
					if (sh.channel_adapter.mini_mode_was_reset())
						need_refresh = true;
					
					/* if this is the first time we have been primary, do a refresh */
					if (sh.channel_adapter.set_primary() == 1)
						need_refresh = true;
					
					/* and someone might have subscribed to a channel since this was last redrawn */
					if (mCallback.subscription_changes_this_session() != sh.subscription_change_count)
						{
						/* try to minimize expensive calls to notifyDataSetChanged */
						sh.subscription_change_count = mCallback.subscription_changes_this_session();
						need_refresh = true;
						}
					
					if (need_refresh)
						sh.channel_adapter.notifyDataSetChanged();
					}
				position_set_slider();
				}
			}
		
	    
	    @Override
	    public CharSequence getPageTitle (int position)
	    	{
	    	return portal_stack_names [position];
	    	}
	
		public void set_mini_mode_thumbs (View v)
			{
			if (mCallback.is_tablet())
				{
				ImageView vModeThumbs = (ImageView) v.findViewById (R.id.mode_thumbs);
				if (vModeThumbs != null)
					vModeThumbs.setImageResource (mini_mode ? R.drawable.icon_thumb : R.drawable.icon_thumb_on);
				ImageView vModeList = (ImageView) v.findViewById (R.id.mode_list);
				if (vModeList != null)
					vModeList.setImageResource (mini_mode ? R.drawable.icon_list_on : R.drawable.icon_list);
				}
			}
		
		public void reload_data()
			{	
			load_data (current_swap_object);
			}
		
		public void load_data (final Swaphome sh)
			{		
			if (sh.home_page != null)
				{
				View vProgress = sh.home_page.findViewById (R.id.set_progress);
				if (vProgress != null)
					vProgress.setVisibility (View.VISIBLE);
				
				View vPull = sh.home_page.findViewById (R.id.pull_to_refresh);
				if (vPull != null)
					vPull.setVisibility (View.GONE);
				}
			
			mCallback.query_pile (portal_stack_ids [sh.set], new Callback()
				{
				public void run_string (String id)
					{
					if (sh.home_page != null)
						{
						View vProgress = sh.home_page.findViewById (R.id.set_progress);
						if (vProgress != null)
							vProgress.setVisibility (View.GONE);
					
						View vPull = sh.home_page.findViewById (R.id.pull_to_refresh);
						if (vPull != null)
							vPull.setVisibility (View.VISIBLE);
						}
					
					sh.arena = config.list_of_channels_in_set (id);
					
					View vRefresh = sh.home_page.findViewById (R.id.pull_to_refresh);
					if (vRefresh != null)
						vRefresh.setVisibility (sh.arena != null && sh.arena.length > 0 ? View.VISIBLE : View.GONE);
					
					sh.vChannels = (StoppableListView) sh.home_page.findViewById (R.id.channel_list);
					if (sh.vChannels != null)
						{
						sh.channel_adapter = new ChannelAdapter (getActivity(), sh.arena, sh);
						sh.vChannels.setAdapter (sh.channel_adapter);
						sh.vChannels.set_refresh_function (mCallback.get_main_thread(), new Runnable()
							{
							@Override
							public void run()
								{
								refresh_home();
								}							
							});
						
						Callback c = new Callback()
							{
							public boolean return_boolean()
								{
								// return finger_is_down;
								/* can return false always for now, because UI requirement has changed */
								return false;
								}
							};
						sh.vChannels.set_finger_is_down_function (c);
	
			    		LayoutInflater inflater = getActivity().getLayoutInflater();
			    		if (!sh.shim_added)
				    		{
				    		View shim = inflater.inflate (R.layout.footer_shim_d9, null);
				    		sh.vChannels.addFooterView (shim);
				    		sh.shim_added = true;
				    		}
			    		set_mini_mode_thumbs (sh.home_page);
						}
					
					Runnable channel_thumberino = new Runnable()
						{
						public void run()
							{
							if (sh.channel_adapter != null)
								sh.channel_adapter.notifyDataSetChanged();
							}
						};
					log ("thumbnailing: " + TextUtils.join (",", sh.arena));
					thumbnail.stack_thumbs (getActivity(), config, sh.arena, -1, mCallback.get_main_thread(), channel_thumberino);
					}
				});
			}
		
		public void refresh()
			{
			if (current_swap_object != null)
				{
				if (current_swap_object.channel_adapter != null)
					current_swap_object.channel_adapter.notifyDataSetChanged();
				}
			}
		}   
	
	boolean have_set_positions = false;
	int set_offsets[] = null;
	int set_widths[] = null;
	
	/* obsolete */
	public void create_set_slider()
		{	
		have_set_positions = false;
		
		final LinearLayout vSetSlider = (LinearLayout) getView().findViewById (R.id.set_slider);
		
		for (int i = 0; i < portal_stack_names.length; i++)
			{
			TextView vText = new TextView (getActivity());
			vText.setText (portal_stack_names [i]);
			vText.setTextColor (Color.rgb (0x00, 0x00, 0x00));
			vText.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			
			vSetSlider.addView (vText);
			
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vText.getLayoutParams();
			layout.height = MATCH_PARENT;
			layout.leftMargin = 1;
			layout.rightMargin = 1;
			layout.gravity = Gravity.CENTER;
			vText.setLayoutParams (layout);
			
			vText.setBackgroundColor (Color.rgb (0xFF, 0x66, 0x00));	
			vText.setPadding (mCallback.actual_pixels (20), 0, mCallback.actual_pixels (20), 0);
			vText.setGravity (Gravity.CENTER);
			}
	
		vSetSlider.post (new Runnable()
			{
			@Override
			public void run()
				{
				set_offsets = new int [portal_stack_names.length];
				set_widths = new int [portal_stack_names.length];
				
				for (int i = 0; i < portal_stack_names.length; i++)
					{
					View v = vSetSlider.getChildAt (i);
					set_offsets [i] = v.getLeft();
					set_widths [i] = v.getWidth();
					log ("set offset: " + set_offsets [i] + ", width: " + set_widths [i]);
					have_set_positions = true;
					position_set_slider();
					if (vHomePager != null)
						vHomePager.setOnPageChangeListener (new SliderListener());
					
				    class SimpleTabColorizer implements SlidingTabLayout.TabColorizer
				    	{
				        private int[] mIndicatorColors;
				        private int[] mDividerColors;
	
				        @Override
				        public final int getIndicatorColor (int position)
				        	{
				            return mIndicatorColors [position % mIndicatorColors.length];
				        	}
	
				        @Override
				        public final int getDividerColor (int position)
				        	{
				            return mDividerColors [position % mDividerColors.length];
				        	}
	
				        void setIndicatorColors (int... colors)
				        	{
				            mIndicatorColors = colors;
				        	}
	
				        void setDividerColors (int... colors)
				        	{
				            mDividerColors = colors;
				        	}
				    	}
				    
					SimpleTabColorizer colorizer = new SimpleTabColorizer();
					colorizer.setIndicatorColors (Color.rgb (0xFF, 0xAA, 0x00));					
				    colorizer.setDividerColors (Color.argb (0x60, 0xFF, 0xFF, 0xFF));
					SlidingTabLayout mSlidingTabLayout = (SlidingTabLayout) getView().findViewById (R.id.sliding_tabs);
					mSlidingTabLayout.setCustomTabColorizer (colorizer);
					mSlidingTabLayout.setViewPager (vHomePager);
					}
				}	
			});
		}
	
	/* when we wanted a customized set slider, instead of SlidingTabStrip.java */
	boolean haz_custom_set_slider = false;
	
	/* obsolete! */
	public void position_set_slider()
		{	
		final View vIndicator = getView().findViewById (R.id.set_indicator);
		if (haz_custom_set_slider && have_set_positions && current_swap_object != null)
			{
			vIndicator.post(new Runnable()
				{
				@Override
				public void run()
					{
					int set = current_swap_object.set;
					log ("positioning set slider to: " + set + " left: " + set_offsets [set] + " width: " + set_widths [set]); // noisy
					vIndicator.setVisibility (View.VISIBLE);
					final FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vIndicator.getLayoutParams();
					// layout.leftMargin = (int) (1.5 * (float) set_offsets [set]);
					layout.width = set_widths [set];
					layout.setMargins(set_offsets [set], 0, 0, 0);
					vIndicator.setLayoutParams (layout);
					}			
				});			
			}
		else
			vIndicator.setVisibility (View.GONE);
		}
	
	private class SliderListener extends ViewPager.SimpleOnPageChangeListener
		{
		@Override
		public void onPageScrolled (int position, final float positionOffset, int positionOffsetPixels)
			{
			super.onPageScrolled (position, positionOffset, positionOffsetPixels);
			log ("SLIDER position=" + position + " positionOffset=" + positionOffset + " positionOffsetPixels=" + positionOffsetPixels);
			
			if (have_set_positions && current_swap_object != null)
				{
				final View vIndicator = getView().findViewById (R.id.set_indicator);
				vIndicator.post (new Runnable()
					{
					@Override
					public void run()
						{
						int set = current_swap_object.set;
	
						final FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vIndicator.getLayoutParams();
						// layout.width = set_widths [set];
						int left = (int) ((1.0 + positionOffset) * set_offsets [set]);
						layout.setMargins (left, 0, 0, 0);
						vIndicator.setLayoutParams (layout);
						}
					});
				}
			}
	    }
    
	class ChannelAdapter extends ArrayAdapter <String>
		{
		Activity context;
		String content[] = null;
		boolean requested_channel_load[] = null;
		boolean requested_channel_thumbs[] = null;
		Swaphome sh = null;
		Bitmap thumbits[] = null;
		String which_thumbs[] = null;
		String first_episode[] = null;
		boolean saved_mini_mode = false;
		
		/* the number of times this has been front and center in its container */
		int primary_count = 0;
		
		ChannelAdapter (Activity context, String content[], Swaphome sh)
			{
			super (context, mCallback.is_tablet() ? R.layout.channel_tablet : R.layout.channel, content);
			this.context = context;
			this.content = content;
			this.sh = sh;
			saved_mini_mode = mini_mode;
			init_arrays();
			
			/* I think sometimes the main thread is too busy to get every update */
			mCallback.get_main_thread().postDelayed (new Runnable()
				{
				public void run()
					{	
					notifyDataSetChanged();
					}
				}, 10000);
			}
		
		public void init_arrays()
			{
			requested_channel_load = new boolean [content.length];
			Arrays.fill (requested_channel_load, Boolean.FALSE);
			requested_channel_thumbs = new boolean [content.length];
			Arrays.fill (requested_channel_thumbs, Boolean.FALSE);
			thumbits = new Bitmap [content.length];
			Arrays.fill (thumbits, null);
			first_episode = new String [content.length];
			Arrays.fill (first_episode, null);
			which_thumbs = new String [content.length];
			Arrays.fill (which_thumbs, null);
			}
		
		public int set_primary ()
			{
			return ++primary_count;
			}
		
		@Override
		public int getCount()
			{
			return content.length - 1;
			}
		
		@Override
		public int getViewTypeCount()
			{
			return 2;
			}
		
		public boolean mini_mode_was_reset()
			{
			return saved_mini_mode != mini_mode;
			}
		
		@Override
		public void notifyDataSetChanged()
			{
			saved_mini_mode = mini_mode;
			super.notifyDataSetChanged();
			}
		
		@Override
		public int getItemViewType (int position)
			{
			if (mCallback.is_phone())
				return 0;
			else
				return mini_mode ? 0 : 1;
			}
		
		@Override
		public View getView (final int position, View convertView, final ViewGroup parent)
			{			
			final String channel_id = content [position + 1];			
			String name = config.pool_meta (channel_id, "name");
			
			if (name != null)
				log ("channel getView: " + position + " (channel: " + channel_id + "=" + name + ")");
			else
				log ("channel getView: " + position + " (channel: " + channel_id + ")");
			
			if (position >= getCount())
				{
				Log.e ("vtest", "race condition! position=" + position + " but content.length is " + content.length);
				return null;
				}
			
			final View row;			
			if (convertView == null)
				{
				int wanted_layout_type = mCallback.is_tablet() ? (mini_mode ? R.layout.channel_mini : R.layout.channel_full) : R.layout.channel;
				log ("ChannelAdapter inflate row type: " + wanted_layout_type);
				row = getActivity().getLayoutInflater().inflate (wanted_layout_type, null);
				}
			else
				row = convertView;
			
			if (mCallback.is_phone())
				adjust_for_device (row);			
			
			TextView vTitle = (TextView) row.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (name != null ? name : ("Channel " + channel_id));
		
			ImageView vChannelIcon = (ImageView) row.findViewById (R.id.channel_icon);
		
			View vChannelFrame = row.findViewById (R.id.channel_frame);
			
			int big_thumb_width = 0;
			int big_thumb_height = 0;
			
			if (mCallback.is_tablet())
				{
				// float factor = mini_mode ? 0.3f : 0.45f;
				float factor = mini_mode ? 0.3f : 0.5f;
				big_thumb_width = (int) ((mCallback.screen_width() - mCallback.actual_pixels (40)) * factor);
				big_thumb_height = (int) ((float) big_thumb_width / 1.77);
				LinearLayout.LayoutParams pic_layout = (LinearLayout.LayoutParams) vChannelFrame.getLayoutParams();
				pic_layout.height = big_thumb_height;
				pic_layout.width = big_thumb_width;
				vChannelFrame.setLayoutParams (pic_layout);						
				}
			else
				{
				big_thumb_width = mCallback.screen_width();
				big_thumb_height = (int) ((float) big_thumb_width / 1.77 * 0.8);
				FrameLayout.LayoutParams pic_layout = (FrameLayout.LayoutParams) vChannelIcon.getLayoutParams();
				pic_layout.height = big_thumb_height;
				vChannelIcon.setLayoutParams (pic_layout);
				}
			
			if (vChannelFrame != null)
				vChannelFrame.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: channel " + channel_id);
			        	mCallback.set_player_real_channel (channel_id);
			        	mCallback.clear_metadata();
			        	mCallback.update_metadata_inner();
			        	mCallback.launch_player (channel_id, content);
			        	}
					});	
		
			View vTwoTitleLines = row.findViewById (R.id.two_title_lines);
			if (vTwoTitleLines != null)
				vTwoTitleLines.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: channel " + channel_id);
			        	mCallback.set_player_real_channel (channel_id);
			        	mCallback.clear_metadata();
			        	mCallback.update_metadata_inner();
			        	mCallback.launch_player (channel_id, content);
			        	}
					});	
			
			ImageView vSpecialTag = (ImageView) row.findViewById (R.id.special_tag);
			if (vSpecialTag != null)
				{
				String tag = config.get_special_tag (channel_id, "set", sh.set_id);
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
			
			final View vFollow = row.findViewById (R.id.follow);
			if (vFollow != null)
				{
				mCallback.set_follow_icon_state (vFollow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
				vFollow.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: home follow/unfollow channel " + channel_id);
			        	mCallback.follow_or_unfollow (channel_id, v);
			        	}
					});
				}
			
			if (vSpecialTag != null)
				{
				vSpecialTag.setOnClickListener (new OnClickListener()
					{
					/* note this duplicates vFollow with that being sent! in case special tag is hit accidentally */
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: home follow/unfollow (special tag) channel " + channel_id);
			        	mCallback.follow_or_unfollow (channel_id, vFollow);
			        	}
					});
				}
			
			View vShare = row.findViewById (R.id.share);
			if (vShare != null)
				vShare.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: home share " + channel_id);
			        	mCallback.share_episode (channel_id, null);
			        	}
					});	
			
			/* often the user misses the share or follow button slightly. eat these and don't launch playback! */
			View vTitleRow = row.findViewById (R.id.title_row);
			if (vTitleRow != null)
				vTitleRow.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	/* eat this */
			        	}
					});		
			
			ImageView vTriple = (ImageView) row.findViewById (R.id.triple_thumb);
			if (vTriple != null)
				vTriple.setImageResource (R.drawable.canary);
			
			final Runnable triple_update_thumbs = new Runnable()
				{
				@Override
				public void run()
					{
					log ("triple update thumbs: " + channel_id);
					
					/* force the first thumbnail into "episode_thumb_1" */
					config.program_line_by_id (channel_id);
					
					thumbits [position] = null;
					notifyDataSetChanged();
					}
				};
				
			String program_line[] = config.program_line_by_id (channel_id);	
			fill_in_large_episode_thumb (position, channel_id, program_line, row);
			
			/* determine the current thumb lineup, to compare with actual thumb lineup. If these differ, the
			   underlying channel has been updated and the triple_thumb should be redrawn */
			
			String expected_thumbs = null;
			if (program_line != null && program_line.length > 0)
				{
				String e1 = program_line.length >= 2 ? program_line [1] : null;
				String e2 = program_line.length >= 3 ? program_line [2] : null;
				expected_thumbs = e1 + " | " + e2;
				}
			
			if (vTriple != null)
				{
				Bitmap bm = null;
				if (thumbits [position] == null || (expected_thumbs != null && !expected_thumbs.equals (which_thumbs [position])))
					{
					bm = triple_thumbnail (channel_id, position, program_line, big_thumb_width);
					thumbits [position] = bm;
					}
				else
					bm = thumbits [position];
				
				if (bm != null)
					vTriple.setImageBitmap (bm);
				}
			
			if (program_line != null && program_line.length > 0)
				{
				if (!requested_channel_thumbs [position])
					{
					requested_channel_thumbs [position] = true;
					
					int n_thumbs = mCallback.is_tablet() ? 4 : 1;
					log ("** request " + n_thumbs + " thumbs: " + channel_id + " (position: " + position + ")");
					
					thumbnail.download_first_n_episode_thumbs
							(getActivity(), config, channel_id, n_thumbs, mCallback.get_main_thread(), triple_update_thumbs);
					}
				else
					log ("already requested first n thumbs for: " + channel_id + " (position: " + position + ")");
				}
			else
				{
				TextView vSubTitle = (TextView) row.findViewById (R.id.subtitle);
				if (vSubTitle != null)
					{
					long ts = config.get_most_appropriate_timestamp (channel_id);
					int display_episodes = config.display_channel_count (channel_id);
					String ago = util.ageof (getActivity(), ts);
					String txt_episode = getResources().getString (R.string.episode_lc);
					String txt_episodes = getResources().getString (R.string.episodes_lc);
					String subtitle = ago + " • " + display_episodes + " " + (display_episodes == 1 ? txt_episode : txt_episodes);
					vSubTitle.setText (subtitle);
					}
				
				String requested_q_thumbs = config.pool_meta (channel_id, "requested_q_thumbs");
				if (requested_q_thumbs == null || !requested_q_thumbs.equals ("true"))
					{
					config.set_channel_meta_by_id (channel_id, "requested_q_thumbs", "true");
					int n_thumbs = mCallback.is_tablet() ? 4 : 1;
					thumbnail.download_q_thumbs (getActivity(), config, channel_id, n_thumbs, mCallback.get_main_thread(), triple_update_thumbs);
					}
				}
			
			int num_episodes = config.programs_in_real_channel (channel_id);
			
			if (!config.channel_loaded (channel_id))
				{
				final Callback after_load = new Callback()
					{
					@Override
					public void run_string_and_object (final String channel_id, Object row)
						{
						int n_thumbs = mCallback.is_tablet() ? 4 : 1;
						log ("** request " + n_thumbs + " thumbs: " + channel_id + " (position: " + position + ")");
						
						thumbnail.download_first_n_episode_thumbs
							(getActivity(), config, channel_id, n_thumbs, mCallback.get_main_thread(), triple_update_thumbs);
						
						requested_channel_thumbs [position] = true;
						notifyDataSetChanged();
						}
					};
		
				/* load channels only if this has been a primary display at least once -- keeps stuff from loading in the background and using up CPU */
				if (false && primary_count > 0 && !requested_channel_load [position])
					{
					requested_channel_load [position] = true;
					int urgency = 600 * (num_episodes > 0 ? 1 : 0);
					mCallback.get_main_thread().postDelayed (new Runnable()
						{
						@Override
						public void run()
							{
							mCallback.load_channel_then (channel_id, true, after_load, channel_id, row);
							}
						}, urgency * position * position);
					}
				}
		
			TextView vChannelName = (TextView) row.findViewById (R.id.channel_name);
			if (vChannelName != null)
				vChannelName.setText (name != null ? name : "?");
			
			int display_episodes = config.display_channel_count (channel_id);
			
			String txt_episode = getResources().getString (R.string.episode_lc);
			String txt_episodes = getResources().getString (R.string.episodes_lc);
			
			TextView vEpisodeCount = (TextView) row.findViewById (R.id.episode_count);
			if (vEpisodeCount != null)
				vEpisodeCount.setText ("" + display_episodes);
			
			TextView vEpisodePlural = (TextView) row.findViewById (R.id.episode_plural);
			if (vEpisodePlural != null)
				vEpisodePlural.setText (display_episodes == 1 ? txt_episode : txt_episodes);
			
			TextView vAgo = (TextView) row.findViewById (R.id.ago);
			if (vAgo != null)
				{			
				long ts = config.get_most_appropriate_timestamp (channel_id);
				String ago = util.ageof (getActivity(), ts);
				vAgo.setText (ago);
				}
			
			if (mCallback.is_phone())
				{
				boolean small_channel_thumbnail_found = false;
				
				ImageView vSmallChannelIcon = (ImageView) row.findViewById (R.id.small_channel_icon);
				if (vSmallChannelIcon != null)
					{
					String filename = getActivity().getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
					File f = new File (filename);
					if (f.exists())
						{
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							{
							Bitmap bitmap2 = bitmappery.getRoundedCornerBitmap (bitmap, 70);
							if (bitmap2 != null)
								{
								vSmallChannelIcon.setImageBitmap (bitmap2);
								small_channel_thumbnail_found = true;
								}
							}
						}
					}
				}
			
			return row;
			}
		
		public void adjust_for_device (View row)
			{
			if (mCallback.is_phone())
				{				
				TextView vFirstEpisodeTitle = (TextView) row.findViewById (R.id.first_episode_title);
				if (vFirstEpisodeTitle != null)
					vFirstEpisodeTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 20);
				
				TextView vAgo = (TextView) row.findViewById (R.id.ago);
				if (vAgo != null)
					vAgo.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
				
				View vSmallChannelIcon = row.findViewById (R.id.small_channel_icon);
				if (vSmallChannelIcon != null)
					{
					LinearLayout.LayoutParams layout6 = (LinearLayout.LayoutParams) vSmallChannelIcon.getLayoutParams();
					layout6.height = mCallback.actual_pixels (32);
					layout6.width = mCallback.actual_pixels (32);
					vSmallChannelIcon.setLayoutParams (layout6);
					}
				
				TextView vChannelFromHeader = (TextView) row.findViewById (R.id.channel_from_header);
				if (vChannelFromHeader != null)
					vChannelFromHeader.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
				
				TextView vChannelName = (TextView) row.findViewById (R.id.channel_name);
				if (vChannelName != null)
					vChannelName.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
				
				TextView vEpisodeCount = (TextView) row.findViewById (R.id.episode_count);
				if (vEpisodeCount != null)
					vEpisodeCount.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
				
				TextView vEpisodePlural = (TextView) row.findViewById (R.id.episode_plural);
				if (vEpisodePlural != null)
					vEpisodePlural.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
				}
			}
		
		public void fill_in_large_episode_thumb (int position, final String channel_id, String program_line[], View parent)
			{
			if (program_line != null && program_line.length >= 1)
				{
				boolean use_blank = first_episode [position] == null;
				mCallback.fill_in_episode_thumb (program_line[0], parent, R.id.channel_icon, (mCallback.is_phone() ? R.id.first_episode_title : 0), use_blank);
				
				first_episode [position] = program_line[0];
				
				TextView vSubTitle = (TextView) parent.findViewById (R.id.subtitle);
				if (vSubTitle != null)
					{
					long ts = config.get_most_appropriate_timestamp (channel_id);
					int display_episodes = config.display_channel_count (channel_id);
					String ago = util.ageof (getActivity(), ts);
					String txt_episode = getResources().getString (R.string.episode_lc);
					String txt_episodes = getResources().getString (R.string.episodes_lc);
					String subtitle = ago + " • " + display_episodes + " " + (display_episodes == 1 ? txt_episode : txt_episodes);
					vSubTitle.setText (subtitle);
					}
				}
			else
				{
				ImageView vThumb = (ImageView) parent.findViewById (R.id.channel_icon);
				
				boolean used_thumbnail = false;
				String t1 = config.pool_meta (channel_id, "episode_thumb_1");
				
				if (t1 != null && t1.startsWith ("http"))
					{
					String f1 = (t1 == null) ? null : getActivity().getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (t1) + ".png";
										
					File f = new File (f1);
					if (f.exists())
						{
						if (f.length() > 0)
							{
							Bitmap bitmap = BitmapFactory.decodeFile (f1);
							if (bitmap != null)
								{
								log ("==> THUMBERY bitmap w:" + bitmap.getWidth() + ", h:" + bitmap.getHeight());
								vThumb.setImageBitmap (bitmap);
								used_thumbnail = true;
								}
							}
						}
					else
						{
						/* no episode icon is found. if this is a live channel (type 13), try a channel icon */
						String nature = config.pool_meta (channel_id, "nature");
						if (nature != null && nature.equals ("13"))
							{
							String cfilename = getActivity().getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
							File cf = new File (cfilename);
							if (cf.exists())
								{
								Bitmap bitmap = BitmapFactory.decodeFile (cfilename);
								if (bitmap != null)
									{
									vThumb.setImageBitmap (bitmap);
									used_thumbnail = true;
									}								
								}
							}
						}
					}
				if (!used_thumbnail)
					vThumb.setImageResource (R.drawable.store_unavailable);
				
				if (mCallback.is_phone())
					{
					TextView vFirstTitle = (TextView) parent.findViewById (R.id.first_episode_title);
					if (vFirstTitle != null)
						{
						String e1 = config.pool_meta (channel_id, "episode_title_1");
						vFirstTitle.setText (e1 != null && !e1.equals ("") ? e1 : "");
						}
					}
				}
			}
		
		public Bitmap triple_thumbnail (String channel_id, int position, String program_line[], int big_thumb_width)
			{
			int margin_cruft = mCallback.actual_pixels (6 + 12 + 12 + 6 + 6 + 6);
			
			// int thumb_width = (screen_width - margin_cruft - big_thumb_width) / 3;
			int thumb_width = (mCallback.screen_width() - margin_cruft - big_thumb_width) / 2;		
			int thumb_height = (int) ((float) thumb_width / 1.77);
			
			log ("-->THUMB WIDTH :: " + thumb_width + " (big thumb width: " + big_thumb_width + ", screen width: " + mCallback.screen_width() + ")");
			
			if (program_line != null && program_line.length > 1)
				{
				String e1 = program_line.length >= 2 ? program_line [1] : null;
				String e2 = program_line.length >= 3 ? program_line [2] : null;
				String e3 = program_line.length >= 4 ? program_line [3] : null;
				
				String f1 = (e1 == null) ? null : getActivity().getFilesDir() + "/" + config.episode_in_cache (e1);
				String f2 = (e2 == null) ? null : getActivity().getFilesDir() + "/" + config.episode_in_cache (e2);
				String f3 = (e3 == null) ? null : getActivity().getFilesDir() + "/" + config.episode_in_cache (e3);
			
				which_thumbs [position] = e1 + " | " + e2;
				
				return bitmappery.generate_double_thumbnail (channel_id, thumb_width, thumb_height, mCallback.actual_pixels (10), f1, f2);
				}
			else
				{
				String e2 = config.pool_meta (channel_id, "episode_thumb_2");
				String e3 = config.pool_meta (channel_id, "episode_thumb_3");	
				String e4 = config.pool_meta (channel_id, "episode_thumb_4");
				
				if ((e2 != null && !e2.equals("")) || (e3 != null && !e3.equals("")) || (e4 != null && !e4.equals("")))
					{
					String f2 = (e2 == null) ? null : getActivity().getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (e2) + ".png";
					String f3 = (e3 == null) ? null : getActivity().getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (e3) + ".png";
					String f4 = (e4 == null) ? null : getActivity().getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (e4) + ".png";
				
					which_thumbs [position] = e2 + " | " + e3;
					
					return bitmappery.generate_double_thumbnail (channel_id, thumb_width, thumb_height, mCallback.actual_pixels (10), f2, f3);
					}
				else
					return null;
				}		
		}
		}
	
	public void bouncy_home_hint_animation()
		{
		if (!mCallback.get_hint_setting ("seen-bouncy-home-hint"))
			{
			mCallback.set_hint_setting ("seen-bouncy-home-hint", true);
			
			final View vHint = getView().findViewById (R.id.home_swipe_hint);
			
			AnimatorSet as = new AnimatorSet();
			
			ValueAnimator animH1 = ValueAnimator.ofInt (mCallback.actual_pixels (100), mCallback.actual_pixels (150));
			ValueAnimator animH2 = ValueAnimator.ofInt (mCallback.actual_pixels (150), mCallback.actual_pixels (120));		
			ValueAnimator animH3 = ValueAnimator.ofInt (mCallback.actual_pixels (150), mCallback.actual_pixels (100));
			
			int dy = mCallback.is_phone() ? mCallback.actual_pixels (20) : mCallback.actual_pixels (30);
			
			ValueAnimator halfLEFT  = ValueAnimator.ofInt ( 0,  -dy);
			ValueAnimator halfRIGHT = ValueAnimator.ofInt (-dy,  0 );
			ValueAnimator fullLEFT  = ValueAnimator.ofInt (+dy, -dy);
			ValueAnimator fullRIGHT = ValueAnimator.ofInt (-dy, +dy);
			
			ValueAnimator.AnimatorUpdateListener listenerH = new ValueAnimator.AnimatorUpdateListener()
		    	{
		        @Override
		        public void onAnimationUpdate (ValueAnimator valueAnimator)
		        	{
		            int val = (Integer) valueAnimator.getAnimatedValue();
		            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) vHint.getLayoutParams();
		            layoutParams.height = val;
		            log ("ANIM height: " + val);
		            final FrameLayout.LayoutParams lp = layoutParams;
		            vHint.setLayoutParams(lp);
		        	}
		    	};
	
			ValueAnimator.AnimatorUpdateListener listenerM = new ValueAnimator.AnimatorUpdateListener()
		    	{
		        @Override
		        public void onAnimationUpdate (ValueAnimator valueAnimator)
		        	{
		            int val = (Integer) valueAnimator.getAnimatedValue();
		            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) vHint.getLayoutParams();
		            layoutParams.leftMargin = val;
		            final FrameLayout.LayoutParams lp = layoutParams;
		            vHint.setLayoutParams(lp);
		        	}
		    	};
		    	
		    animH1.addUpdateListener (listenerH);
		    animH2.addUpdateListener (listenerH);
		    animH3.addUpdateListener (listenerH);	
		    
		    halfLEFT.addUpdateListener (listenerM);
		    halfRIGHT.addUpdateListener (listenerM);
		    fullLEFT.addUpdateListener (listenerM);
		    fullRIGHT.addUpdateListener (listenerM);
		    
			ObjectAnimator animFI = ObjectAnimator.ofFloat (vHint, "alpha", 0.0f, 1.0f);
			ObjectAnimator animFO = ObjectAnimator.ofFloat (vHint, "alpha", 1.0f, 0.0f);			
	
			animFI.setDuration (300);
			animH1.setDuration (1000);
			animH2.setDuration (1000);
			animH3.setDuration (400);		
			animFO.setDuration (300);
			halfLEFT.setDuration (300);
			halfRIGHT.setDuration (300);
			fullLEFT.setDuration (600);
			fullRIGHT.setDuration (600);
			
		    as.play(animFI).with(animH1);
		    as.play(animH3).after(animH1);
		    as.play(halfLEFT).after(animH3);
		    as.play(fullRIGHT).after(halfLEFT);
		    as.play(fullLEFT).after(fullRIGHT);
		    as.play(halfRIGHT).after(fullLEFT);	    
		    as.play(animFO).after(halfRIGHT);
		     
			as.start();		
			}
		}
	}