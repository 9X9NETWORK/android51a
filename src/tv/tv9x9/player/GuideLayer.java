package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class GuideLayer extends StandardFragment
	{
	metadata config = null;
	
    public interface OnGuideListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
        public void launch_player (String channel_id, String channels[]);
        public void launch_player (String channel_id, String episode_id, String channels[]);
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void enable_signin_layer (Runnable callback);
    	public int screen_width();
    	public void toggle_menu();
    	public void toggle_menu	(final Callback cb);
    	public void toast_by_resource (int id);
    	public void set_follow_icon_state (View v, String channel_id, int follow_resource_id, int unfollow_resource_id);
    	public void query_following (final Callback callback);
    	public void load_channel_then (final String channel_id, final boolean allow_cache, final Callback callback, final String arg1, final Object arg2);
    	public boolean dongle_mode();
    	public void follow_or_unfollow (String channel_id, View v);
		}    
    
    OnGuideListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.guide_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnGuideListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnGuideListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "guide";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
    
	public void setup_guide_buttons()
		{
		View vGuideLayer = getView().findViewById (R.id.guidelayer);
		if (vGuideLayer != null)
			vGuideLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("guide layer ate my tap!");
		        	}
				});
		
		if (mCallback.is_phone())
			{
			TextView vGuideTitle = (TextView) getView().findViewById (R.id.guide_title);
			vGuideTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 20);
			
			TextView vGuideMeta = (TextView) getView().findViewById (R.id.guide_meta);
			vGuideMeta.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
			}
		
		View vGuideAsGuest = getView().findViewById (R.id.guide_as_guest);
		if (vGuideAsGuest != null)
			vGuideAsGuest.setVisibility (config.usertoken != null ? View.GONE : View.VISIBLE);
	
		View vGuideAsGuestDialog = getView().findViewById (R.id.guide_as_guest_dialog);
		if (vGuideAsGuestDialog != null)
			{
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vGuideAsGuestDialog.getLayoutParams();
			layout.width = (int) (0.8f * (float) mCallback.screen_width());
			vGuideAsGuestDialog.setLayoutParams (layout);
			}
		
		View vGagHome = getView().findViewById (R.id.guide_as_guest_home);
		if (vGagHome != null)
			vGagHome.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide as guest -- home");
		        	mCallback.toggle_menu (new Callback()
			        	{
			        	public void run()
			        		{
				        	mCallback.enable_home_layer();
				        	mCallback.toggle_menu();
			        		}
			        	});
		        	}
				});		
		
		View vGagSignIn = getView().findViewById (R.id.guide_as_guest_signin);
		if (vGagSignIn != null)
			vGagSignIn.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide as guest -- sign in");
		        	mCallback.toggle_menu (new Callback()
		        		{
		        		public void run()
		        			{
			        		mCallback.enable_signin_layer (null);
			        		mCallback.toggle_menu();
		        			}
		        		});		        	
		        	}
				});			
		}

	public void update_guide_metadata()
		{
		// TODO
		}
		
	public void data_changed()
		{
		if (grid_slider != null)
			grid_slider.notifyDataSetChanged();
		}
	
	public void refresh_guide()
		{
		if (config.usertoken != null)
			{
			config.forget_subscriptions();
			mCallback.query_following (refresh_guide_inner);
			}
		else
			mCallback.toast_by_resource (R.string.please_login_first);
		}
	
	final Callback refresh_guide_inner = new Callback()
		{
		@Override
		public void run_string (String arg1)
			{
			init_3x3_grid (config);
			}
		};
	
	int current_set = 1;
	int grid_cursor = 0;
	
	GridSlider grid_slider = null;	
	StoppableViewPager vPager = null;
	
	int top_lefts[] = { 11, 14, 17, 41, 44, 47, 71, 74, 77 };
	
	/* we don't want to redraw everything, but we will need to remove the background from this square */
	FrameLayout previous_cursor_view = null;
	
	public void init_3x3_grid (metadata config)
		{
		log ("init 3x3 grid");
		
		this.config = config;
	    grid_slider = new GridSlider();
	
	    vPager = (StoppableViewPager) getView().findViewById (R.id.top3x3pager);
	    vPager.setAdapter (grid_slider);
	    
		current_set = 1;
		previous_cursor_view = null;	   
		}
	
	FrameLayout current_slider_view = null;
	
	class Swapgrid
		{
		FrameLayout box = null;
		GridView gv = null;
		int set = 0;
		public Swapgrid (int a_set)
			{
			set = a_set;
			}
		};
		
	/* this is implemented using the base class! */
		
	public class GridSlider extends PagerAdapter
		{
		boolean first_time = true;
		
	    @Override
	    public int getCount()
	    	{
	        return 9;
	    	}
	
	    @Override
	    public void notifyDataSetChanged()
	    	{
	    	super.notifyDataSetChanged();
	    	if (current_slider_view != null)
	    		redraw_3x3 (current_slider_view, current_set - 1);
	    	}
	    
		@Override
		public boolean isViewFromObject (View view, Object object)
			{
			return (((Swapgrid) object).box) == (FrameLayout) view;
			}
		
		@Override
		public Object instantiateItem (ViewGroup container, int position)
			{
			log ("[PAGER] instantiate: " + position);
			
			final Swapgrid sg = new Swapgrid (position);			
	
			FrameLayout box = (FrameLayout) View.inflate (getActivity(), R.layout.grid, null);
			
			box.setTag (R.id.container, position);			
			sg.box = box;
	
			redraw_3x3 (box, position);
			// download_grid_thumbs_for_this_3x3();
			
			onInflatedSlidePage (box);
			
			((StoppableViewPager) container).addView (box, 0);
			return sg;
			}
		
		@Override
		public void destroyItem (ViewGroup container, int position, Object object)
			{
			log ("[PAGER] destroy: " + position);
			Swapgrid sg = (Swapgrid) object;
			((StoppableViewPager) container).removeView (sg.box);
			}
		
		@Override
		public void setPrimaryItem (ViewGroup container, int position, Object object)
			{
			log ("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% primary 3x3: " + position);
			current_slider_view = ((Swapgrid) object).box;
			
			  /* set height and width, since ViewPager always fills the screen */
			  Swapgrid sg = (Swapgrid) object;
			  StoppableViewPager pager = (StoppableViewPager) container;
			  sg.box.measure
			  	((MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)),
			  	 (MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)));
			  final int width = sg.box.getMeasuredWidth();
			  final int height = sg.box.getMeasuredHeight();
			  log ("+++++++++++++ W:" + width + " ++++++++++ H:" + height);
			  // pager.setLayoutParams (new LinearLayout.LayoutParams (/*width*/ screen_width, Math.max (height, 1)));
			  LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) pager.getLayoutParams();
			  layout.width = mCallback.screen_width();
			  layout.height = Math.max (height, 1);
			  pager.setLayoutParams (layout);
			  pager.invalidate();
	
			  /* fix a rendering bug in ViewPager */
			  if (first_time)
			  	{
				first_time = false;
				pager.post (new Runnable()
					{
					@Override
					public void run()
						{
						View altpager = getView().findViewById (R.id.top3x3pager);
						LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) altpager.getLayoutParams();
						layout.width = mCallback.screen_width();
						layout.height = Math.max (height, 1);
						altpager.setLayoutParams (layout);
						altpager.invalidate();
					  	}
					});
			  	}
			  
		    select (position);
			download_grid_thumbs_for_this_3x3 (sg.box);
			redraw_3x3 (current_slider_view, position);
			}
	
		public void select (int num)
			{
			log ("%%%%%%%%%%%%%%%%%%%%%%%%%%%%% [PAGER] selected: " + num);			
			current_set = num + 1;
			onSelected3x3 (current_set);			
			}
		}

	public void onInflatedSlidePage (View v)
		{
		/* override this */
		}
	
	public void highlight_3x3_square (int grid_position, FrameLayout v, boolean changing)
		{
		log ("highlight_3x3_square: " + grid_position);
		
		if (v == null)
			{
			log ("*** highlight_3x3_square: view is null!");
			return;
			}
		
		if (previous_cursor_view != null)
			{
			int previous_position = (Integer) previous_cursor_view.getTag (R.layout.gridsquare9x9);
			erase_previous_cursor();
			log ("highlight: " + previous_position + " -> " + grid_position);
			}		
		else
			{
			if (grid_cursor > 0)
				{
				log ("presumably the gridslider needs to be redrawn");
				previous_cursor_view = recover_previous_view (grid_cursor);
				if (previous_cursor_view != null)
					log ("recovered!");
				erase_previous_cursor();
				// redraw_3x3 (current_slider_view, current_set - 1);
				}
			else
				log ("highlight: previous is null");
			}
		
		if (changing)
			set_grid_cursor (grid_position);
		
		previous_cursor_view = v;		
	
		final String channel_id = config.channel_meta (grid_position, "id");	
		
		ImageView vEpicon = (ImageView) v.findViewById (R.id.epicon);
		if (vEpicon != null)
			vEpicon.setAlpha (1.0f);
		ImageView vPlayBall = (ImageView) v.findViewById (R.id.playball);
		if (vPlayBall != null)
			{
			vPlayBall.setVisibility (channel_id != null ? View.VISIBLE : View.GONE);
			if (mCallback.is_phone())
				{
				FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vPlayBall.getLayoutParams();
				layout.bottomMargin = 0;
				vPlayBall.setLayoutParams (layout);
				}
			}
		TextView vTitle = (TextView) v.findViewById (R.id.title);
		if (vTitle != null)
			vTitle.setTextColor (Color.rgb (0xFF, 0xFF, 0xFF));
		
		if (mCallback.dongle_mode() && !v.isFocused())
			v.requestFocus();
		
		TextView vLargeTitle = (TextView) getView().findViewById (R.id.guide_title);
		TextView vLargeMeta = (TextView) getView().findViewById (R.id.guide_meta);
		ImageView vFollow = (ImageView) getView().findViewById (R.id.guide_follow);
		
		vLargeTitle.setVisibility (channel_id == null ? View.INVISIBLE : View.VISIBLE);
		vLargeMeta.setVisibility (channel_id == null ? View.INVISIBLE : View.VISIBLE);
		vFollow.setVisibility (channel_id == null ? View.INVISIBLE : View.VISIBLE);
	
		if (channel_id != null)
			{
			/* there is no longer a follow/unfollow button on individual grid items */
			mCallback.set_follow_icon_state (vFollow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			// vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow_black);
			vFollow.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide follow/unfollow");
		        	mCallback.follow_or_unfollow (channel_id, v);
		        	}
		    	});
			}
		
		if (channel_id != null)
			{
			String name = config.pool_meta (channel_id, "name"); 
			vLargeTitle.setText (name);
			if (config.channel_loaded (channel_id))
				{	
				highlight_3x3_square_inner_inner (channel_id);
				}
			else
				{
				String txt_wait = getResources().getString (R.string.wait);
				vLargeMeta.setText (txt_wait);
				mCallback.load_channel_then (channel_id, false, highlight_3x3_square_inner, channel_id, null);
				}
			}			
		}	
	
	final Callback highlight_3x3_square_inner = new Callback()
		{
		@Override
		public void run_string_and_object (String channel_id, Object arg2)
			{
			highlight_3x3_square_inner_inner (channel_id);
			}
		};	
	
	public void highlight_3x3_square_inner_inner (String channel_id)
		{
		int display_count = config.display_channel_count (channel_id);
		TextView vLargeMeta = (TextView) getView().findViewById (R.id.guide_meta);
		if (vLargeMeta != null)
			{
			String txt_episode = getResources().getString (R.string.episode_lc);		
			String txt_episodes = getResources().getString (R.string.episodes_lc);
			vLargeMeta.setText ("" + display_count + " " + (display_count == 1 ? txt_episode : txt_episodes));
			}
		}
	
	public void redraw_3x3 (FrameLayout v, int set_position)
		{
		int top_left = top_lefts [set_position];		
	    
		int channel_position = 0;
		for (int j = top_left; j < top_left + 30; j += 10)
			for (int i = j; i < j + 3; i++)
				{
				channel_position++;
				log ("3x3 render: " + i);
				int cid = getResources().getIdentifier ("c" + channel_position, "id", getActivity().getPackageName());
				if (cid != 0)
					{
					FrameLayout v2 = (FrameLayout) v.findViewById (cid);
					if (v2 != null)
						redraw_3x3_square (v2, i);
					}
				}
		}    
	
	public void redraw_3x3_square (FrameLayout v, int grid_position)
		{	
		String channel_id = config.channel_meta (grid_position, "id");
		log ("%% redraw_3x3_square: " + grid_position + ", id: " + channel_id);
		
		String name = config.channel_meta (grid_position, "name");
		
		boolean channel_thumbnail_found = false;
		boolean episode_thumbnail_found = false;
		
		TextView vTitle = (TextView) v.findViewById (R.id.title);
		ImageView vChannelicon = (ImageView) v.findViewById (R.id.chicon);
		ImageView vEpisodeicon = (ImageView) v.findViewById (R.id.epicon);		
		
		if (channel_id != null && !channel_id.equals (""))
			{
			if (vTitle != null)
				{
				vTitle.setText (name);
				vTitle.setVisibility (mCallback.is_phone() ? View.GONE : View.VISIBLE);
				}
			
			if (vChannelicon != null)
				{
				String filename = getActivity().getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
				File f = new File (filename);
				if (f.exists ())
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						vChannelicon.setImageBitmap (bitmap);
						channel_thumbnail_found = true;
						}					
					}
				}
			
			String filename = getActivity().getFilesDir() + "/" + config.api_server + "/xthumbs/" + channel_id + ".png";
			
			File f = new File (filename);
			if (f.exists())
				{
				Bitmap bitmap = BitmapFactory.decodeFile (filename);
				if (bitmap != null)
					{
					episode_thumbnail_found = true;
					vEpisodeicon.setImageBitmap (bitmap);
					}
				}
			}
		else
			{
			if (vTitle != null)
				vTitle.setVisibility (View.GONE);
			}
		
		if (vChannelicon != null)
			{
			if (!channel_thumbnail_found)
				vChannelicon.setImageResource (R.drawable.none);
			}
		
		if (!episode_thumbnail_found)
			vEpisodeicon.setImageResource (R.drawable.unavailable_3x3);
		
		if (vChannelicon != null)
			vChannelicon.setVisibility ((channel_id != null && !channel_id.equals ("")) ? View.VISIBLE : View.GONE);
		
		if (!episode_thumbnail_found)
			{
			if (channel_id == null || channel_id.equals (""))
				vEpisodeicon.setImageResource (R.drawable.darkgray);
			else
				vEpisodeicon.setImageResource (R.drawable.unavailable_3x3);
			// when I use the below image, it artifacts with a single white line at the bottom!
			// vEpisodeicon.setImageResource (R.drawable.bg_3x3_normal);
			}
		
		ImageView bg = (ImageView) v.findViewById (R.id.bgsquare);
		if (bg != null)
			{
			if (grid_position == grid_cursor)
				{
				log ("cursor highlight: " + grid_position);
				highlight_3x3_square (grid_position, v, false);
				}
			else
				dehighlight_3x3_square (v);
			bg.setTag (R.id.bgsquare, grid_position);			
			}
	
		/* the extra 0.2 adds enough to compensate and make the squares truly square */
		int width = (int) ((float) mCallback.screen_width() / 3.2);	
	    
	    TableRow.LayoutParams layout = (TableRow.LayoutParams) v.getLayoutParams();   
	    layout.width = width;
	    layout.height = width; // (int) ((float) w / 1.77f);
	    v.setLayoutParams (layout);
		
		v.setTag (R.layout.gridsquare9x9, grid_position);
		
		v.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	int grid_position = (Integer) v.getTag (R.layout.gridsquare9x9);
	        	onNewClick3x3 (v, grid_position);
	        	}
	    	});
		
		if (mCallback.is_phone())
			{
			if (vChannelicon != null)
				{
				FrameLayout.LayoutParams layout3 = (FrameLayout.LayoutParams) vChannelicon.getLayoutParams();
				layout3.height = mCallback.actual_pixels (15);
				layout3.width = mCallback.actual_pixels (15);
				layout3.topMargin = mCallback.actual_pixels (2);
				layout3.leftMargin = mCallback.actual_pixels (2);
				vChannelicon.setLayoutParams (layout3);
				}
			
			ImageView vPlayBall = (ImageView) v.findViewById (R.id.playball);
			if (1 == 2 && vPlayBall != null)
				{
				FrameLayout.LayoutParams layout3 = (FrameLayout.LayoutParams) vPlayBall.getLayoutParams();
				layout3.height = mCallback.actual_pixels (20);
				layout3.width = mCallback.actual_pixels (20);
				layout3.bottomMargin = 0;
				vPlayBall.setLayoutParams (layout3);
				}			
			}
		}
		
	public void onNewClick3x3 (View v, int grid_position)
		{
		// int grid_position = top_lefts [current_set-1] + 10 * (position / 3) + (position % 3);
		String channel_id = config.channel_meta (grid_position, "id");						
		log ("3x3 grid click: " + grid_position + " (current is: " + grid_cursor + "), current set: " + current_set + ", channel: " + channel_id);
		
		if (grid_position != grid_cursor)
			{
			log ("changing selection: " + grid_cursor + " -> " + grid_position);
			highlight_3x3_square (grid_position, (FrameLayout) v, true);
			}
		else
			{
			if (channel_id == null)
				mCallback.toast_by_resource (R.string.no_channel);
			else
				mCallback.launch_player (channel_id, config.subscribed_channels());
			}
		}
	
	public void download_grid_thumbs_for_this_3x3 (View v)
		{
		for (int position = 1; position <= 9; position++)
			{
			int grid_position = 10 * current_set + position;
			String channel_id = config.channel_meta (grid_position, "id");
			if (channel_id != null && !channel_id.equals (""))
	    		{
	    		String filename = getActivity().getFilesDir() + "/" + config.api_server + "/xthumbs/" + channel_id + ".png";
	    		File f = new File (filename);
	    		if (!f.exists())
	    			download_sample_thumb (channel_id);
	    		}
			}
		}    
	
	Hashtable < String, Boolean > sample_downloaded = new Hashtable < String, Boolean > ();
	
	public void download_sample_thumb (final String channel_id)
		{
		if (sample_downloaded.get (channel_id) == null)
			{
			sample_downloaded.put (channel_id, true);
			new playerAPI (mCallback.get_main_thread(), config, "latestEpisode?channel=" + channel_id)
				{
				public void success (String[] lines)
					{
					for (int i = 0; i < lines.length; i++)
						{
						String fields[] = lines [i].split ("\t");
						log ("---DOWNLOAD LINE--- " + lines[i]);
						thumbnail.sample_thumb (getActivity(), config, channel_id, fields[2], mCallback.get_main_thread(), episode_thumbs_updated);
						}
					}
				public void failure (int code, String errtext)
					{
					}
				};
			}
		}
	
	final Runnable episode_thumbs_updated = new Runnable()
		{
		@Override
		public void run()
			{
			if (grid_slider != null && current_slider_view != null)
				{
				for (int c = 1; c <= 9; c++)
					{
					int id = getResources().getIdentifier ("c" + c, "id", getActivity().getPackageName());
					if (id != 0)
						{
						FrameLayout v = (FrameLayout) current_slider_view.findViewById (id);
						if (v != null)
							{
							int position = (Integer) v.getTag (R.layout.gridsquare9x9);
							redraw_3x3_square (v, position);
							}
						}
					}
	
				/* TODO */
				}
			}
		};	
		
	public void set_grid_cursor (int new_cursor)
		{
		if (grid_cursor != new_cursor)
			{
			grid_cursor = new_cursor;
			log ("set_grid_cursor: " + new_cursor);
			/* this is much too costly -- setting it in the onClick for now */
			// top9x9_adapter.notifyDataSetChanged();
			}
		update_guide_metadata();		
		}
		
	public void erase_previous_cursor()
		{
		if (previous_cursor_view != null)
			{
			log ("erase previous cursor");
			dehighlight_3x3_square (previous_cursor_view);
			previous_cursor_view.postInvalidate();
			}
		else
			log ("erase_previous_cursor: previous is null");
		}
	
	public void dehighlight_3x3_square (View v)
		{
		if (v.getTag (R.layout.gridsquare9x9) != null)
			{
			int gn = (Integer) v.getTag (R.layout.gridsquare9x9);
			log ("dehighlight_3x3_square: " + gn);
			}
		else
			log ("dehighlight_3x3_square: unknown number");
		
		ImageView vEpicon = (ImageView) v.findViewById (R.id.epicon);
		if (vEpicon != null)
			vEpicon.setAlpha (0.5f);
		ImageView vPlayBall = (ImageView) v.findViewById (R.id.playball);
		if (vPlayBall != null)
			vPlayBall.setVisibility (View.GONE);
		TextView vTitle = (TextView) v.findViewById (R.id.title);
		if (vTitle != null)
			vTitle.setTextColor (Color.rgb (0xA0, 0xA0, 0xA0));
		v.postInvalidate();
		}			
	
	public FrameLayout recover_previous_view (int grid_position)
		{
		for (int c = 1; c <= 9; c++)
			{
			int id = getResources().getIdentifier ("c" + c, "id", getActivity().getPackageName());
			if (id != 0)
				{
				FrameLayout v = (FrameLayout) current_slider_view.findViewById (id);
				if (v != null)
					{
					int position = (Integer) v.getTag (R.layout.gridsquare9x9);
					log ("looking... " + position);
					if (position == grid_position)
						return v;
					}
				}
			}
		return null;
		}
	
	public void onSelected3x3 (int set)
		{	
		redraw_dots();
		}
	
	public void redraw_dots()
		{
		for (int i = 1; i <= 9; i++)
			{
			int id = getResources().getIdentifier ("dot" + i, "id", getActivity().getPackageName());
			if (id > 0)
				{
	    		ImageView vDot = (ImageView) getView().findViewById (id);
	    		if (vDot != null)
	    			{
	    			int normal = mCallback.dongle_mode() ? R.drawable.white_dot : R.drawable.white_dot_tablet;
	    			int highlit = mCallback.dongle_mode() ? R.drawable.white_dot_highlight : R.drawable.white_dot_tablet_highlight;
	    			vDot.setImageResource (i == current_set ? highlit : normal);
	    			}
				}
			}	
		}

  	}