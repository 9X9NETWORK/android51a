package tv.tv9x9.player;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import tv.tv9x9.player.HorizontalListView.OnScrollListener;
import tv.tv9x9.player.metadata.Comment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.facebook.*;
import com.facebook.Session.StatusCallback;
import com.facebook.android.Util;
import com.facebook.model.GraphUser;
import com.facebook.widget.*;

public class main extends VideoBaseActivity
	{
	/* set of channels, for up/down flipping */
	// String arena[] = null; inherit from VideoBaseActivity
	
	boolean single_channel = false;
	boolean single_episode = false;
	
	public main()
		{
		identity = "main";
		}
	
	enum toplayer { HOME, PLAYBACK, SIGNIN, GUIDE, STORE, SEARCH, SETTINGS, TERMS };
	
	toplayer current_layer = toplayer.HOME;
	toplayer layer_before_signin = toplayer.HOME;
	
	String current_home_stack = null;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		home_configure_white_label();
		adjust_layout_for_screen_size();
		onVideoActivityLayout();
		setup_home_buttons();
		fezbuk1 (savedInstanceState);
		}

	@Override
	protected void onStop()
		{
		super.onStop();
		}
		
	@Override
	public void onResume()
		{
		super.onResume();
		if (uiHelper != null)
			uiHelper.onResume();
		try
			{
			String fb_app_id = getResources().getString (R.string.fb_app_id);
			com.facebook.AppEventsLogger.activateApp (this, fb_app_id);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
    @Override
    public void onPause()
    	{
        super.onPause();
        uiHelper.onPause();
    	}
    
    @Override
    public void onDestroy()
    	{
        super.onDestroy();
        uiHelper.onDestroy();
    	}    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    	{
        super.onActivityResult (requestCode, resultCode, data);
        uiHelper.onActivityResult (requestCode, resultCode, data);
    	}
	
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event)
		{
		super.onKeyDown (keyCode, event);
		
		log ("key code: " + keyCode);
	    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MEDIA_STOP)
	    	{
	    	}

	    return false;
		}
	
	/* back needs onKeyUp, but video tapping needs onKeyDown or it will require double-taps */
	
	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event)
		{
		super.onKeyUp (keyCode, event);
		
	    if (keyCode == KeyEvent.KEYCODE_BACK)
	    	{
	    	int orientation = getRequestedOrientation();
	    	
	    	if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
	    		{
	    		log ("reset orientation to portrait");
	    		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    		if (current_layer == toplayer.PLAYBACK)
	    			{
	    			log ("re-entering playback in portrait mode");
	    			onVideoActivityLayout();
	    			return false;
	    			}
	    		}
	    	
	    	log ("key BACK (layer " + current_layer.toString() + ")");
	    	if (current_layer == toplayer.PLAYBACK)
	    		{
    			pause_video();
    			enable_home_layer();
	    		}
	    	else if (current_layer == toplayer.HOME)
	    		{
	    		View vChannelOverlay = findViewById (R.id.channel_overlay);
	    		boolean channel_overlay_visible = vChannelOverlay.getVisibility() == View.VISIBLE;
	    		if (menu_is_extended())
	    			toggle_menu();
	    		else if (channel_overlay_visible)
	    			toggle_channel_overlay (current_home_page);
	    		else
	    			exit_stage_left();
	    		}
	    	else if (current_layer == toplayer.STORE)
	    		{
	    		View vCategoryLayer = findViewById (R.id.category_layer);
	    		if (vCategoryLayer.getVisibility() == View.VISIBLE)
	    			toggle_category_layer();
	    		else
	    			toggle_menu();
	    		}
	    	else if (current_layer == toplayer.TERMS)
	    		{
	    		slide_away_terms();
	    		}
	    	else if (current_layer == toplayer.SIGNIN || current_layer == toplayer.GUIDE
	    				|| current_layer == toplayer.SEARCH || current_layer == toplayer.SETTINGS)
	    		toggle_menu();
	    	return true;
	    	}
	    return false;
		}

	public void adjust_layout_for_screen_size()
		{
		if (is_phone())
			{
			View vTopControls = findViewById (R.id.top_controls);
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTopControls.getLayoutParams();
			layout.height = pixels_50;
			vTopControls.setLayoutParams (layout);
			
			View vControls = findViewById (R.id.controls);
			LinearLayout.LayoutParams layout3 = (LinearLayout.LayoutParams) vControls.getLayoutParams();
			layout3.height = pixels_50;
			vControls.setLayoutParams (layout3);
			
			TextView vEpisodeTitle = (TextView) findViewById (R.id.episode_title);
			vEpisodeTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 22);
			TextView vEpisodeAge = (TextView) findViewById (R.id.episode_age);
			vEpisodeAge.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			TextView vNumCommentsHeader = (TextView) findViewById (R.id.num_comments_header);
			vNumCommentsHeader.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			TextView vNumCommentsDot = (TextView) findViewById (R.id.num_comments_dot);
			vNumCommentsDot.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);			
			TextView vNumComments = (TextView) findViewById (R.id.num_comments);
			vNumComments.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			TextView vFromPrefix = (TextView) findViewById (R.id.playback_from_prefix);
			vFromPrefix.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
			TextView vPlaybackChannel = (TextView) findViewById (R.id.playback_channel);
			vPlaybackChannel.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			
			TextView vPlaybackEpisodeCount = (TextView) findViewById (R.id.playback_episode_count);
			vPlaybackEpisodeCount.setTextSize (TypedValue.COMPLEX_UNIT_SP, 18);
			TextView vPlaybackEpisodePlural = (TextView) findViewById (R.id.playback_episode_plural);
			vPlaybackEpisodePlural.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
			
			View vChannelIcon = findViewById (R.id.playback_channel_icon);
			LinearLayout.LayoutParams layout2 = (LinearLayout.LayoutParams) vChannelIcon.getLayoutParams();
			layout2.height = pixels_40;
			layout2.width = pixels_40;
			vChannelIcon.setLayoutParams (layout2);
			
			View vChannelIconLandscape = findViewById (R.id.playback_channel_icon_landscape);
			LinearLayout.LayoutParams layout6 = (LinearLayout.LayoutParams) vChannelIconLandscape.getLayoutParams();
			layout6.height = pixels_40;
			layout6.width = pixels_40;
			vChannelIconLandscape.setLayoutParams (layout6);
			
			View vPlaybackShare = findViewById (R.id.playback_share);
			LinearLayout.LayoutParams layout4 = (LinearLayout.LayoutParams) vPlaybackShare.getLayoutParams();
			layout4.height = pixels_30;
			layout4.width = pixels_30;
			vPlaybackShare.setLayoutParams (layout4);
			
			View vPlaybackFollow = findViewById (R.id.playback_follow);
			LinearLayout.LayoutParams layout5 = (LinearLayout.LayoutParams) vPlaybackFollow.getLayoutParams();
			layout5.height = pixels_30;
			layout5.width = pixels_30;
			vPlaybackFollow.setLayoutParams (layout5);
			}
		
		/* this is a ListView */
		View vSearchListPhone = findViewById (R.id.search_list_phone);
		vSearchListPhone.setVisibility (is_tablet() ? View.GONE : View.VISIBLE);
		
		/* this is a GridView */
		View vSearchListTablet = findViewById (R.id.search_list_tablet);
		vSearchListTablet.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);
		
		/* this is a ListView */
		View vStoreListPhone = findViewById (R.id.store_list_phone);
		vStoreListPhone.setVisibility (is_tablet() ? View.GONE : View.VISIBLE);
		
		/* this is a GridView */
		View vStoreListTablet = findViewById (R.id.store_list_tablet);
		vStoreListTablet.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);
		}
	
	public void onVideoActivityFlingUp()
		{
		if (current_layer == toplayer.PLAYBACK)
			{
		   	if (!single_channel && !single_episode)
				{
				if (playback_channel_adapter != null)
					{
					final int next = next_channel_index() - 1;
					in_main_thread.post (new Runnable()
						{
						@Override
						public void run()
							{
							ListView vPlaybackChannels = (ListView) findViewById (R.id.playback_channel_list);			
							vPlaybackChannels.smoothScrollToPosition (next);
							}
						});
					}
				next_channel();
				}
			}
		}

	public void onVideoActivityFlingDown()
		{
		if (current_layer == toplayer.PLAYBACK)
			{
	    	if (!single_channel && !single_episode)
				{
				if (playback_channel_adapter != null)
					{
					final int prev = previous_channel_index() - 1;
					in_main_thread.post (new Runnable()
						{
						@Override
						public void run()
							{
							ListView vPlaybackChannels = (ListView) findViewById (R.id.playback_channel_list);			
							vPlaybackChannels.smoothScrollToPosition (prev);
							}
						});
					}
				previous_channel();
				}
			}
		}

	public void onVideoActivityFlingLeft()
		{
		perform_fling_left();
		}

	public void onVideoActivityFlingRight()
		{
		previous_episode();
		}

	@Override
	public void onVideoActivityReady()
		{
		resume_as_logged_in_user();
		// attach_relay();
		setup_home_buttons();
		populate_home();
		}
	
	public void home_configure_white_label()
		{
		int top_bars[] = { R.id.home_top_bar, R.id.guide_top_bar, R.id.store_top_bar, R.id.search_top_bar };
		
		for (int top_bar: top_bars)
			{
			View vTopBar = findViewById (top_bar);
			configure_white_label_for (vTopBar);
			}
		
		String signin_logo = getResources().getString (R.string.signin_logo);
		if (signin_logo != null)
			{
			int logo_id = getResources().getIdentifier (signin_logo, "drawable", getPackageName());
			
			ImageView vSigninLogo = (ImageView) findViewById (R.id.signin_logo);
			vSigninLogo.setImageResource (logo_id);
			
			ImageView vTermsLogo = (ImageView) findViewById (R.id.terms_logo);
			vTermsLogo.setImageResource (logo_id);
			}
		
		String signin_bg = getResources().getString (R.string.signin_bg);
		if (signin_bg != null)
			{
			int bg = getResources().getIdentifier (signin_bg, "drawable", getPackageName());
			
			View vSigninLayer = findViewById (R.id.signinlayer);
			vSigninLayer.setBackgroundResource (bg);

			View vTermsLayer = findViewById (R.id.termslayer);
			vTermsLayer.setBackgroundResource (bg);
			}
		
		boolean uses_chromecast = getResources().getBoolean (R.bool.uses_chromecast);
		
		View vMediaRouteButton = findViewById (R.id.media_route_button);
		// if (vMediaRouteButton != null)
		// 	vMediaRouteButton.setVisibility (uses_chromecast ? View.VISIBLE : View.GONE);
		
		if (!uses_chromecast)
			{
			((ViewManager) vMediaRouteButton.getParent()).removeView (vMediaRouteButton);
			}
		}

	public void configure_white_label_for (View vTopBar)
		{
		String logo = getResources().getString (R.string.logo);
		int logo_id = getResources().getIdentifier (logo, "drawable", getPackageName());
		
		ImageView vLogo = (ImageView) vTopBar.findViewById (R.id.logo);	
		vLogo.setImageResource (logo_id);
		View vResizableTopBar = vTopBar.findViewById (R.id.top_bar_resizable);
		
		setup_top_bar (vResizableTopBar);
		}
	
	public void resume_as_logged_in_user()
		{
		String u = futil.read_file (this, "user@" + config.api_server);
		if (!u.startsWith ("ERROR:"))
			{
			config.usertoken = u;
			log ("GOT SAVED TOKEN: " + config.usertoken);
			}
		u = futil.read_file (this, "name@" + config.api_server);
		if (!u.startsWith ("ERROR:"))
			{
			config.username = u;
			log ("GOT SAVED USERNAME: " + config.username);
			}
		u = futil.read_file (this, "email@" + config.api_server);
		if (!u.startsWith ("ERROR:"))
			{
			config.email = u;
			log ("GOT SAVED EMAIL: " + config.email);
			}
		if (config.username != null && config.username.startsWith ("YOUTUBE::"))
			{
			alert ("YouTube accounts not supported with this version!");
			finish();
			}	
		
		if (config.usertoken != null)
			query_following (null);
		}
		
	public void setup_top_bar (View v)
		{
		LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) v.getLayoutParams();
		layout.height = is_phone() ? pixels_60 : pixels_60;
		v.setLayoutParams (layout);
		
		if (is_phone())
			{
			EditText vTerm = (EditText) v.findViewById (R.id.term);
			vTerm.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			vTerm.setPadding (pixels_6, pixels_6, pixels_6, pixels_6);
			}
		}
	
	public void activate_layer (toplayer layer)
		{
    	make_layer_visible (layer);
    	if (menu_is_extended())
    		toggle_menu();
    	if (layer == toplayer.GUIDE)
    		enable_guide_layer();
		}
	
	public void make_layer_visible (toplayer layer)
		{
		log ("make layer visible: " + layer.toString());
		
		if (layer == toplayer.PLAYBACK)
			{
			View video_layer = video_layer();
			video_layer.setVisibility (View.VISIBLE);
			}
		else
			disable_video_layer();
		
		set_layer (layer);
		}
	
	public void setup_home_buttons()
		{
		View vHomeTopBar = findViewById (R.id.home_top_bar);
		
		View vMenu = vHomeTopBar.findViewById (R.id.menubutton);
		if (vMenu != null)
			vMenu.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: home menu button");
		        	toggle_menu();
		        	}
				});	
		
		View vSearch = vHomeTopBar.findViewById (R.id.searchbutton);
		if (vSearch != null)
			vSearch.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: home search button");
		        	enable_search_apparatus (R.id.home_top_bar);
		        	}
				});	

		View vRefresh = vHomeTopBar.findViewById (R.id.refresh);
		if (vRefresh != null)
			vRefresh.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: home refresh button");
		        	refresh_home();
		        	}
				});	
		
		View vHomeLayer = home_layer();
		if (vHomeLayer != null)
			vHomeLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});					
		}
	
	public void toggle_menu()
		{
		toggle_menu (null);
		}
	
	public void toggle_menu	(final Callback cb)
		{	
		/* set the width of the menu */
		final LinearLayout vMenu = (LinearLayout) findViewById (R.id.menu_layer);		
        FrameLayout.LayoutParams menu_layout = (FrameLayout.LayoutParams) vMenu.getLayoutParams();		
        menu_layout.width = left_column_width();
        vMenu.setLayoutParams (menu_layout);
        
		final View vHome = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
        
        /* when from_margin is 0, the column is extended */
        final int from_margin = layout.leftMargin;
        final int to_margin = (layout.leftMargin == 0) ? left_column_width() : 0;    		
        
		Animation a = new Animation()
			{
		    @Override
		    protected void applyTransformation (float interpolatedTime, Transformation t)
		    	{
		        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
		        layout.leftMargin = (int) (from_margin + (to_margin - from_margin) * interpolatedTime);
		        layout.width = screen_width;
		        vHome.setLayoutParams (layout);		        
		    	}
			};

		a.setAnimationListener (new AnimationListener()
			{
			@Override
			public void onAnimationEnd(Animation animation)
				{
				if (cb != null)	
					cb.run();
				}

			@Override
			public void onAnimationRepeat (Animation animation) {}

			@Override
			public void onAnimationStart (Animation animation) {}
			});
		
		a.setDuration (400);	    		
		vHome.startAnimation (a);
		
		setup_menu_buttons();
		}
	
	public boolean menu_is_extended()
		{
		final View vHome = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();	
        return layout.leftMargin != 0;
		}
	
	public void hide_menu_immediately()
		{
		// View vHome = home_layer();	
		final View vHome = findViewById (R.id.slidingpanel);	
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
        layout.leftMargin = 0;
        layout.width = screen_width;
        vHome.setLayoutParams (layout);
		}

	public int left_column_width()
		{
	    float percent = is_phone() ? 0.65f : 0.65f;
	    return (int) (screen_width * percent);
		}
	
	public void setup_menu_buttons()
		{
		View vSignin = findViewById (R.id.signin_button);
		if (vSignin != null)
			vSignin.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin button");
		        	toggle_menu();
		        	enable_signin_layer();
		        	}
				});	
		
		View vHome = findViewById (R.id.menu_home);
		if (vHome != null)
			vHome.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: menu home");
		        	toggle_menu();
		        	enable_home_layer();
		        	}
				});	

		View vGuide = findViewById (R.id.menu_guide);
		if (vGuide != null)
			vGuide.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: menu guide");
		        	toggle_menu();
		        	enable_guide_layer();
		        	}
				});	

		View vStore = findViewById (R.id.menu_store);
		if (vStore != null)
			vStore.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: menu store");
		        	toggle_menu();
		        	enable_store_layer();
		        	}
				});	
		
		View vSettings = findViewById (R.id.menu_settings);
		if (vSettings != null)
			vSettings.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: menu settings");		        	
		        	toggle_menu();
		        	if (config.usertoken != null)
		        		enable_settings_layer();
		        	else
		        		enable_signin_layer();
		        	}
				});
		
		View vHelp = findViewById (R.id.menu_help);
		if (vHelp != null)
			vHelp.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: menu help");
		        	}
				});		
		
		View vSignout = findViewById (R.id.menu_signout);
		if (vSignout != null)
			vSignout.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: menu signout");
		        	if (config != null && config.email.equals ("[via Facebook]"))
		        		facebook_logout();
		        	else
		        		signout();
		        	}
				});			
		
		/* username adjustments */
		
		vSignin.setVisibility (config.usertoken == null ? View.VISIBLE : View.GONE);		
		
		View vIdentity = findViewById (R.id.identity);
		vIdentity.setVisibility (config.usertoken == null ? View.GONE : View.VISIBLE);

		View vSettingsDivider = findViewById (R.id.menu_settings_divider);
		View vSignoutDivider = findViewById (R.id.menu_signout_divider);
		
		/* presently nothing in settings is changeable by Facebook users */
		boolean is_facebook = config.email != null && config.email.equals ("[via Facebook]");
		
		vSettings.setVisibility (is_facebook ? View.GONE : View.VISIBLE);
		vSettingsDivider.setVisibility (is_facebook ? View.GONE : View.VISIBLE);
		
		vSignout.setVisibility (config.usertoken == null ? View.GONE : View.VISIBLE);
		vSignoutDivider.setVisibility (config.usertoken == null ? View.GONE : View.VISIBLE);
		
		if (config.usertoken != null)
			{
			TextView vName = (TextView) findViewById (R.id.menu_name);
			vName.setText (config.username);
			
			TextView vEmail = (TextView) findViewById (R.id.menu_email);
			vEmail.setText (config.email);
			}
		}

	@Override
	public void onVideoActivitySignout()
		{
		setup_menu_buttons();
		}
	
	@Override
	public void onKnobDragging()
		{
		reset_osd_display_time();
		}
	
	@Override
	public void onVideoActivityPlayback()
		{
		make_osd_visible();
		}
	
	@Override
	public void onVideoActivityTouched()
		{
		toggle_osd_and_pause();
		}
	
	@Override
	public void onVideoActivityPauseOrPlay (boolean paused)
		{
		ImageView vPausePlay = (ImageView) findViewById (R.id.pause_or_play);
		if (vPausePlay != null)
			vPausePlay.setImageResource (paused ? R.drawable.play_tablet : R.drawable.pause_tablet);	
		if (paused)
			make_osd_visible();
		}
	
	@Override
	public void onVideoActivityProgress (boolean is_playing, int offset, int duration, float pct)
		{
		videoActivityUpdateProgressBar (offset, duration);
		}

	@Override
	public void onLastEpisode()
		{
		log ("last episode, exiting player -- back to grid");
		exit_stage_left();
		}
	
	@Override
	public void onVideoActivityLayout()
		{
		if (current_layer != toplayer.PLAYBACK)
			return;
		
		if (video_is_minimized)
			return;
		
		View vContainer = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams playerParams = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();
		
	    // float px120 = TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
	    
		// final View vPOI = findViewById (R.id.poi_h);
		// boolean make_chrome_visible = osd_visible_in_full_screen || vPOI.getVisibility() == View.VISIBLE;
		boolean make_chrome_visible = false;
		
		View vMenuLayer = findViewById (R.id.menu_layer);
		View vSlidingPanel = findViewById (R.id.slidingpanel);
		View vVideoLayer = video_layer();
		View vPlaybackBody = findViewById (R.id.playbackbody);
		View vControls = findViewById (R.id.controls);
		View vLandscapeTitles = findViewById (R.id.landscape_titles);
		View vTopControls = findViewById (R.id.top_controls);
		
		FrameLayout.LayoutParams video_layout = (FrameLayout.LayoutParams) vVideoLayer.getLayoutParams();
		
		SpecialFrameLayout yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);
		
		LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
		
		int orientation = getRequestedOrientation();
		boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		
		if (fullscreen || landscape)
			{
			log ("Fullscreen layout");
	
			// vMenuLayer.setVisibility (View.GONE);
			// vSlidingPanel.setVisibility (View.GONE);
			vPlaybackBody.setVisibility (View.GONE);
			vControls.setVisibility (View.GONE);
			vLandscapeTitles.setVisibility (View.VISIBLE);
			// vVideoLayer.setVisibility (View.VISIBLE);
			vTopControls.setVisibility (View.GONE);
			
			playerParams.width = LayoutParams.MATCH_PARENT;
			playerParams.height = LayoutParams.MATCH_PARENT;
			playerParams.topMargin = 0;
			playerParams.bottomMargin = 0;
			playerParams.leftMargin = 0;
			playerParams.rightMargin = 0;
			vContainer.setLayoutParams (playerParams);
			
			video_layout.width = LayoutParams.MATCH_PARENT;
			video_layout.height = LayoutParams.MATCH_PARENT;
			
			vVideoLayer.setLayoutParams (video_layout);
			
			wrapper_layout.width = LayoutParams.MATCH_PARENT;
			wrapper_layout.height = LayoutParams.MATCH_PARENT;
			
			yt_wrapper.setLayoutParams (wrapper_layout);
			}
		else
			{
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
				{
				log ("Landscape but w/o fullscreen mode");
			
				vControls.setVisibility (View.GONE);
				vLandscapeTitles.setVisibility (View.VISIBLE);	
				vTopControls.setVisibility (View.GONE);
				
				// playerParams.width = uiParams.width = 0;
				// playerParams.height = uiParams.height = MATCH_PARENT;
				playerParams.width = MATCH_PARENT;
				playerParams.height = MATCH_PARENT;
				playerParams.topMargin = 0;
				playerParams.bottomMargin = 0;
				playerParams.leftMargin = 0;
				playerParams.rightMargin = 0;
				vContainer.setLayoutParams (playerParams);
				}
			else
				{
				log ("Portrait layout");
				// playerParams.width = uiParams.width = MATCH_PARENT;
				
				// vMenuLayer.setVisibility (View.VISIBLE);
				// vSlidingPanel.setVisibility (View.VISIBLE);
				vControls.setVisibility (View.VISIBLE);
				vPlaybackBody.setVisibility (View.VISIBLE);
				vLandscapeTitles.setVisibility (View.GONE);
				vTopControls.setVisibility (View.VISIBLE);
				// vVideoLayer.setVisibility (View.VISIBLE);
				
				playerParams.width = MATCH_PARENT;
				playerParams.height = WRAP_CONTENT;
				playerParams.topMargin = 0;
				playerParams.bottomMargin = 0;
				playerParams.leftMargin = 0;
				playerParams.rightMargin = 0;				
				vContainer.setLayoutParams (playerParams);
				
				// video_layout.width = LayoutParams.MATCH_PARENT;
				// video_layout.height = LayoutParams.MATCH_PARENT;				
				// vVideoLayer.setLayoutParams (video_layout);
				
				wrapper_layout.width = LayoutParams.MATCH_PARENT;
				wrapper_layout.height = LayoutParams.WRAP_CONTENT;
				
				yt_wrapper.setLayoutParams (wrapper_layout);
				yt_wrapper.postInvalidate();
				
				vPlaybackBody.postInvalidate();
				}
			}
		
		LinearLayout.LayoutParams titles_layout = (LinearLayout.LayoutParams) vLandscapeTitles.getLayoutParams();
		titles_layout.width = (int) ((float) screen_height / 3f);
		vLandscapeTitles.setLayoutParams (titles_layout);
		
		reset_video_size();
		}
	
	public void OLD_onVideoActivityLayout()
  		{
		if (current_layer != toplayer.PLAYBACK)
			return;
		
		if (1 == 1) return;
		
		View vContainer = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams playerParams = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();
		
	    float px120 = TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
	    
		// final View vPOI = findViewById (R.id.poi_h);
		boolean make_chrome_visible = osd_visible_in_full_screen; // || vPOI.getVisibility() == View.VISIBLE;
		
		if (fullscreen)
			{
			log ("Fullscreen layout");
			
			// When in fullscreen, the visibility of all other views than the player should be set to
			// GONE and the player should be laid out across the whole screen.
			playerParams.width = LayoutParams.MATCH_PARENT;
			playerParams.height = LayoutParams.MATCH_PARENT;
			
			// playerParams.setMargins (0, 0, 0, 0);
			vContainer.setLayoutParams (playerParams);
			}
		else
			{
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
				{
				/* DON'T THINK THIS CASE OCCURS */
				log ("Landscape but w/o fullscreen mode");				
				
				// playerParams.width = uiParams.width = 0;
				// playerParams.height = uiParams.height = MATCH_PARENT;
				playerParams.width = MATCH_PARENT;
				playerParams.height = MATCH_PARENT;
				// playerParams.weight = 1;
				// baseLayout.setOrientation(LinearLayout.HORIZONTAL);
				playerParams.setMargins (0, 0, 0, 0);
				vContainer.setLayoutParams (playerParams);
				}
			else
				{
				log ("Portrait layout");
				// playerParams.width = uiParams.width = MATCH_PARENT;
				playerParams.width = MATCH_PARENT;
				playerParams.height = WRAP_CONTENT;
				playerParams.setMargins (/*left*/ 0, /* top */ 0, /*right*/ 0, /*bottom*/ 0);
				vContainer.setLayoutParams (playerParams);
				// if (started == 3)
				// 	add_images_to_h();
				}
			}
  		}
	
	public void next_channel()
		{
		String next_id = next_channel_id();
		log ("previous channel id: " + next_id);		
		change_channel (next_id);	
		}

	private int next_channel_index()
		{		
		int you_are_here = 0;
		
		for (int i = 0; i < arena.length; i++)
			{
			String a = arena [i];
			log ("ARENA: " + a);
			if (a != null && player_real_channel.equals (a))
				{
				you_are_here = i;
				break;
				}
			}
		
		return (you_are_here + 1 >= arena.length) ? 1 : you_are_here + 1;
		}
	
	private String next_channel_id()
		{
		if (arena != null)
			return arena [ next_channel_index() ];
		else
			return player_real_channel;
		}

	public void previous_channel()
		{
		String prev_id = previous_channel_id();
		log ("previous channel id: " + prev_id);		
		change_channel (prev_id);		
		}
	
	private int previous_channel_index()
		{
		int you_are_here = 0;
		
		for (int i = 1; i < arena.length; i++)
			{
			String a = arena [i];
			log ("ARENA: " + a);
			if (player_real_channel.equals (a))
				{
				you_are_here = i;
				break;
				}
			}
		
		return (you_are_here <= 1) ? arena.length - 1 : you_are_here - 1;
		}
	
	private String previous_channel_id()
		{
		if (arena != null)
			return arena [ previous_channel_index() ];
		else
			return player_real_channel;
		}	
	
	String pending_channel = null;
	
	private void change_channel (String channel_id)
		{
		pending_channel = channel_id;
		log ("change channel to: " + channel_id);
		if (config.programs_in_real_channel (channel_id) < 1)
			{
			String nature = config.pool_meta (channel_id, "nature");
			String yt_username = config.pool_meta (channel_id, "extra");			
			ytchannel.fetch_youtube_in_thread (in_main_thread, change_channel_inner, config, channel_id, nature, yt_username);
			}
		else
			change_channel_inner_inner();
		}
	
	final Runnable change_channel_inner = new Runnable()
		{
		public void run()
			{
			change_channel_inner_inner();
			}
		};
	
	public void change_channel_inner_inner()
		{
		player_real_channel = pending_channel;
		program_line = config.program_line_by_id (player_real_channel);
		current_episode_index = 1;
		
		if (player_real_channel != null && program_line != null)
			{
			start_playing();
			
			String nature = config.pool_meta (player_real_channel, "nature");
			
			if (!nature.equals ("3") && !nature.equals ("4") && !nature.equals ("5"))
				thumbnail.download_titlecard_images_for_channel (main.this, config, player_real_channel, in_main_thread, titlecard_image_update);
			}		
		
		String episode_id = program_line != null && program_line.length >= 1 ? program_line [0] : null;
		onVideoActivityRefreshMetadata (player_real_channel, episode_id);
		}		
	
	/* updates just the channel thumb, not other metadata -- to prevent endless looping */
		
	final Runnable update_channel_thumb = new Runnable()
		{
		public void run()
			{
			String actual_channel_id = player_real_channel;			
			if (program_line != null)
				{
				String episode_id = program_line [current_episode_index - 1];
				
				if (player_real_channel.contains (":"))				
					actual_channel_id = config.program_meta (episode_id, "real_channel");
				}
			if (!actual_channel_id.contains (":"))
				update_channel_thumb_inner (actual_channel_id);
			}
		};
		
		
	public boolean update_channel_thumb_inner (String actual_channel_id)
		{
		boolean channel_thumbnail_found = false;
		
		ImageView vChannelIcon1 = (ImageView) findViewById (R.id.chicon);
		
		String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + actual_channel_id + ".png";
		
		File f = new File (filename);
		if (f.exists())
			{
			int pixels = 72;
			Bitmap bitmap = BitmapFactory.decodeFile (filename);
			if (bitmap != null)
				{
				Bitmap bitmap2 = bitmappery.getRoundedCornerBitmap (bitmap, pixels);
				if (bitmap2 != null)
					{
					channel_thumbnail_found = true;
					if (vChannelIcon1 != null)
						vChannelIcon1.setImageBitmap (bitmap2);				
					}
				bitmap.recycle();
				}
			}
		
		return channel_thumbnail_found;
		}
		
	final Runnable update_metadata = new Runnable()
		{
		public void run()
			{
			update_metadata_inner();
			}
		};
	
	public void update_metadata_inner()
		{
		String actual_channel_id = player_real_channel;
		
		if (program_line == null || current_episode_index > program_line.length)
			return;
		
		String episode_id = program_line [current_episode_index - 1];
		String episode_name = config.program_meta (episode_id, "name");
		
		String channel_name = null;
		if (actual_channel_id.contains (":"))
			actual_channel_id = config.program_meta (episode_id, "real_channel");
		channel_name = config.pool_meta (actual_channel_id, "name");
		
		TextView vChannel = (TextView) findViewById (R.id.tunedchannel);
		if (vChannel != null)
			vChannel.setText (channel_name);		

		TextView vLandscapeChannel = (TextView) findViewById (R.id.landscape_channel_name);
		if (vLandscapeChannel != null)
			vLandscapeChannel.setText (channel_name);
		
		TextView vEpisodeName = (TextView) findViewById (R.id.eptitle);
		if (vEpisodeName != null)
			vEpisodeName.setText (episode_name != null && !episode_name.equals ("") ? episode_name : "[no episode name]");

		TextView vLandscapeEpisode = (TextView) findViewById (R.id.landscape_episode_name);
		if (vLandscapeEpisode != null)
			vLandscapeEpisode.setText (episode_name != null && !episode_name.equals ("") ? episode_name : "[no episode name]");
		
		
		if (!update_channel_thumb_inner (actual_channel_id))
			{
			String channel_line_of_one[] = { actual_channel_id };
			thumbnail.stack_thumbs (main.this, config, channel_line_of_one, in_main_thread, update_channel_thumb);
			}		

		TextView vEpisodeTitle = (TextView) findViewById (R.id.episode_title);
		vEpisodeTitle.setText (episode_name);
		
		//TOTO: TEMPORARY!
		vEpisodeTitle.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	toggle_extended_comments_view();
	        	}
			});	
		
		View vPlaybackBodyNormal = findViewById (R.id.playbackbody_normal_view);
		if (vPlaybackBodyNormal != null)
			vPlaybackBodyNormal.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	toggle_extended_comments_view();
	        	}
			});	
			
		String timestamp = config.program_meta (episode_id, "timestamp");
		String ago = util.ageof (Long.parseLong (timestamp) / 1000);
		
		TextView vAgo = (TextView) findViewById (R.id.episode_age);
		if (vAgo != null)
			vAgo.setText (ago);
		
		String txt_follow = getResources().getString (R.string.follow);
		String txt_unfollow = getResources().getString (R.string.unfollow);
		
		String funf = config.is_subscribed (actual_channel_id) ? txt_unfollow : txt_follow;
		
		ImageView vInfo = (ImageView) findViewById (R.id.infobutton);
		if (vInfo != null)
			vInfo.setVisibility (current_subepisode > 0 ? View.VISIBLE : View.INVISIBLE);
		
		TextView vSubepisode = (TextView) findViewById (R.id.subepisodetitle);
		if (vSubepisode != null)
			{
			if (current_subepisode > 0)
				{
				if (vSubepisode != null)
					{
					String subepisode_name = config.program_meta (episode_id, "sub-" + current_subepisode + "-name");
					vSubepisode.setText (subepisode_name != null ? subepisode_name : "[this subepisode has no name]");
					}
				}
			else
				{
				vSubepisode.setVisibility (View.GONE);
				}
			}
		
		String channel_position = null;
		
		int grid_cursor = config.first_position_of (actual_channel_id);
		if (grid_cursor > 0)
			channel_position = "" + (grid_cursor / 10) + "-" + (grid_cursor % 10);
		
		log ("update_metadata: channel=|" + channel_name + "|");
		log ("update_metadata: episode=|" + episode_name + "|");		
		}
	
	@Override
	public void update_metadata_mini (String episode_id)
		{
		String episode_name = config.program_meta (episode_id, "name");
		TextView vEpisodeName = (TextView) findViewById (R.id.eptitle);
		if (vEpisodeName != null)
			vEpisodeName.setText (episode_name != null && !episode_name.equals ("") ? episode_name : "[no episode name]");
		String timestamp = config.program_meta (episode_id, "timestamp");
		String ago = util.ageof (Long.parseLong (timestamp) / 1000);		
		TextView vAgo = (TextView) findViewById (R.id.episode_age);
		if (vAgo != null)
			vAgo.setText (ago);
		}
	
	public void toggle_extended_comments_view()
		{
    	View vNormal = findViewById (R.id.playbackbody_normal_view);
    	View vExtended = findViewById (R.id.playbackbody_comments_view);
    	boolean is_extended = vExtended.getVisibility() == View.VISIBLE;
    	vNormal.setVisibility (is_extended ? View.VISIBLE : View.GONE);
    	vExtended.setVisibility (is_extended ? View.GONE : View.VISIBLE);
		vExtended.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	toggle_extended_comments_view();
	        	}
			});
		}
	
	final Runnable go_halfscreen = new Runnable()
		{
		public void run()
			{			
			log ("go halfscreen");
			update_metadata_inner();
			videoFragment.set_full_screen (false);
			}
		};
		
	final Runnable go_fullscreen = new Runnable()
		{
		public void run()
			{			
			log ("go fullscreen");
			videoFragment.set_full_screen (true);
			}
		};		
	
	public void follow_or_unfollow (String channel_id)
		{
		if (config.is_subscribed (channel_id))
			unsubscribe (channel_id);
		else
			subscribe (channel_id);
		}
	
	public void subscribe (final String real_channel)
		{
		if (config.usertoken != null)
			{
			final int position =  config.youtube_auth_token == null ? config.first_empty_position() : 0;
	    	if (position >= 0)
				{
	    		int server_position = (position == 0) ? 0 : config.client_to_server (position);
	    		new playerAPI (in_main_thread, config, "subscribe?user=" + config.usertoken + "&channel=" + real_channel + "&grid=" + server_position)
					{
					public void success (String[] lines)
						{
						toast_by_resource (R.string.following_yay);
						config.place_in_channel_grid (real_channel, position, true);
						config.set_channel_meta_by_id (real_channel, "subscribed", "1");						
						String youtube_username = config.pool_meta (real_channel, "extra");
						ytchannel.subscribe_on_youtube (config, youtube_username);
						config.subscriptions_altered = config.grid_update_required = true;
						update_layer_after_subscribe (real_channel);
						}
					public void failure (int code, String errtext)
						{
						alert ("Subscription failure: " + errtext);
						config.dump_subscriptions();
						}
					};
				}
			}
		else
			{
			/* this can't actually happen */
			toast_by_resource (R.string.please_login_first);
			}
		}
		
	public void unsubscribe (final String real_channel)
		{
		if (config.usertoken != null)
			{
			final int position =  config.youtube_auth_token == null ? config.first_position_of (real_channel) : 0;
	    	if (position >= 0)
				{
	    		int server_position = (position == 0) ? 0 : config.client_to_server (position);
	    		new playerAPI (in_main_thread, config, "unsubscribe?user=" + config.usertoken + "&channel=" + real_channel + "&grid=" + server_position)
					{
					public void success (String[] lines)
						{
						toast_by_resource (R.string.unfollowing_yay);
						config.place_in_channel_grid (real_channel, position, false);
						config.set_channel_meta_by_id (real_channel, "subscribed", "0");		
						String youtube_username = config.pool_meta (real_channel, "extra");
						ytchannel.delete_on_youtube (config, youtube_username);
						config.subscriptions_altered = config.grid_update_required = true;
						update_layer_after_subscribe (real_channel);
						}
					public void failure (int code, String errtext)
						{
						alert ("Failure unsubscribing: " + errtext);
						config.dump_subscriptions();
						}
					};
				}
			}
		else
			{
			/* this can't actually happen */
			toast_by_resource (R.string.please_login_first);
			}
		}
	
	public void update_layer_after_subscribe (String channel_id)
		{
		if (current_layer == toplayer.STORE)
			{
			if (store_adapter != null)
				store_adapter.notifyDataSetChanged();
			}
		else if (current_layer == toplayer.SEARCH)
			{
			if (search_adapter != null)
				search_adapter.notifyDataSetChanged();
			}
		else if (current_layer == toplayer.GUIDE)
			{			
			ImageView vFollow = (ImageView) findViewById (R.id.guide_follow);
			vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow_black);
			redraw_3x3 (current_slider_view, current_set - 1);
			}
		else if (current_layer == toplayer.HOME)
			{
			if (current_swap_object != null)
				{
				if (current_swap_object.channel_adapter != null)
					current_swap_object.channel_adapter.notifyDataSetChanged();
				}
			}
		else if (current_layer == toplayer.PLAYBACK)
			{
			ImageView vFollow = (ImageView) findViewById (R.id.playback_follow);
			vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow);
			ImageView vFollowLandscape = (ImageView) findViewById (R.id.playback_follow_landscape);
			vFollowLandscape.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow);			
			}
		}
	
	public View home_layer()
		{
		// return findViewById (is_tablet() ? R.id.home_tablet : R.id.home_layer);
		return findViewById (R.id.home_layer);
		}
	
	public void set_layer (toplayer layer)
		{
		log ("set layer: " + layer.toString());
		
		View home_layer = home_layer();
		home_layer.setVisibility (layer == toplayer.HOME ? View.VISIBLE : View.GONE);

		View guide_layer = findViewById (R.id.guidelayer);
		guide_layer.setVisibility (layer == toplayer.GUIDE ? View.VISIBLE : View.GONE);

		View store_layer = findViewById (R.id.storelayer);
		store_layer.setVisibility (layer == toplayer.STORE ? View.VISIBLE : View.GONE);

		View search_layer = findViewById (R.id.searchlayer);
		search_layer.setVisibility (layer == toplayer.SEARCH ? View.VISIBLE : View.GONE);	
		
		View settings_layer = findViewById (R.id.settingslayer);
		settings_layer.setVisibility (layer == toplayer.SETTINGS ? View.VISIBLE : View.GONE);
		
		View terms_layer = findViewById (R.id.termslayer);
		terms_layer.setVisibility (layer == toplayer.TERMS ? View.VISIBLE : View.GONE);
		
		View signin_layer = findViewById (R.id.signinlayer);
		signin_layer.setVisibility (layer == toplayer.SIGNIN ? View.VISIBLE : View.GONE);
		
		current_layer = layer;
		}
	
	/*** SIGNIN ************************************************************************************************/
	
	public void enable_signin_layer()
		{
		disable_video_layer();
		
		/* terms layer can only be started from signin, so ignore it */
		if (current_layer != toplayer.TERMS)
			layer_before_signin = current_layer;
		
		set_layer (toplayer.SIGNIN);		
		
		setup_signin_buttons();
		
		if (is_phone())
			{
			View vGossamer = findViewById (R.id.gossamer);
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vGossamer.getLayoutParams();
			layout.leftMargin = pixels_30;
			layout.rightMargin = pixels_30;
			layout.topMargin = pixels_30;
			layout.bottomMargin = pixels_30;
			vGossamer.setLayoutParams (layout);
			}
		}

	public void setup_signin_buttons()
		{
		int editables[] = { R.id.sign_in_email_container,
							R.id.sign_in_password_container, 
							R.id.sign_up_name_container,
							R.id.sign_up_email_container,
							R.id.sign_up_password_container,
							R.id.sign_up_verify_container };
		
		for (int editable: editables)
			{
			final View vContainer = findViewById (editable);
			if (vContainer != null)
				vContainer.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	EditText vEditable = (EditText) vContainer.findViewWithTag ("editable");
			        	vEditable.requestFocusFromTouch();
			        	InputMethodManager imm = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE); 
			            imm.showSoftInput (vEditable, 0);
			        	}
					});	
			}

		View vBack = findViewById (R.id.signin_back);
		if (vBack != null)
			vBack.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin back");
		        	toggle_menu (new Callback()
			        	{
						@Override
						public void run()
							{
							activate_layer (layer_before_signin);
							}		        	
			        	});
		        	}
				});	
		
		View vSignin = findViewById (R.id.sign_in_button);
		if (vSignin != null)
			vSignin.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin");
		        	proceed_with_signin();
		        	}
				});
		
		View vSignup = findViewById (R.id.sign_up_button);
		if (vSignup != null)
			vSignup.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signup");
		        	proceed_with_signup();
		        	}
				});
		
		View vSigninTab = findViewById (R.id.sign_in_tab);
		if (vSigninTab != null)
			vSigninTab.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin tab");
		        	sign_in_tab();
		        	}
				});

		View vSignupTab = findViewById (R.id.sign_up_tab);
		if (vSignupTab != null)
			vSignupTab.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin tab");
		        	sign_up_tab();
		        	}
				});

		View vByCreating = findViewById (R.id.by_creating);
		if (vByCreating != null)
			vByCreating.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: by creating");
		        	slide_in_terms();
		        	}
				});
		
		View vSigninLayer = findViewById (R.id.signinlayer);
		if (vSigninLayer != null)
			vSigninLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});
		
		fezbuk2();
		}

	public void sign_in_tab()
		{
		adjust_signin_tabs (true);
		}	
	
	public void sign_up_tab()
		{
		adjust_signin_tabs (false);
		}	
	
	public void adjust_signin_tabs (boolean is_sign_in)
		{
		View vSignInContent = findViewById (R.id.sign_in_content);
		View vSignUpContent = findViewById (R.id.sign_up_content);	
		
		vSignInContent.setVisibility (is_sign_in ? View.VISIBLE : View.GONE);
		vSignUpContent.setVisibility (is_sign_in ? View.GONE : View.VISIBLE);
		
		View vSignInTab = findViewById (R.id.sign_in_tab);
		View vSignUpTab = findViewById (R.id.sign_up_tab);
		
		vSignInTab.setBackgroundResource (is_sign_in ? R.drawable.gossamerleft : R.drawable.gossamerleftoff);
		vSignUpTab.setBackgroundResource (is_sign_in ? R.drawable.gossamerrightoff : R.drawable.gossamerright);		
		}
	
	public void proceed_with_signin()
		{
		TextView emailView = (TextView) findViewById (R.id.sign_in_email);
		final String email = emailView.getText().toString();
	
		/* use any view to turn off soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow (emailView.getApplicationWindowToken(), 0);
	    
		TextView passwordView = (TextView) findViewById (R.id.sign_in_password);
		String password = passwordView.getText().toString();
		
		password = util.encodeURIComponent (password);
		String encoded_email = util.encodeURIComponent (email);
		
		if (!email.contains ("@") || email.contains (" "))
			{
			toast_by_resource (R.string.tlogin_valid_email);
			return;
			}
		
		if (password.length() < 6)
			{
			toast_by_resource (R.string.tlogin_pw_six);
			return;
			}
		
		log ("email: " + email + " password: " + password);
		new playerAPI (in_main_thread, config, "login?email=" + encoded_email + "&password=" + password)
			{
			public void success (String[] lines)
				{
				signed_in_with_facebook = false;
				config.email = email;
				process_login_data (email, lines);
				toast_by_resource (R.string.signed_in);
				query_following (null);
	        	toggle_menu (new Callback()
		        	{
					@Override
					public void run()
						{
						activate_layer (layer_before_signin);
						}		        	
		        	});
				}
			public void failure (int code, String errtext)
				{
				if (code == 201)
					toast_by_resource (R.string.login_failure);
				else
					{
					String txt_failure = getResources().getString (R.string.login_failure);
					alert (txt_failure + ": " + errtext);
					}
				}
			};
		}
	
	public void proceed_with_signup()
		{
		TextView emailView = (TextView) findViewById (R.id.sign_up_email);
		final String email = emailView.getText().toString();
	
		/* use any view to turn off soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow (emailView.getApplicationWindowToken(), 0);
	    
		TextView nameView = (TextView) findViewById (R.id.sign_up_name);
		String name = nameView.getText().toString();
		
		TextView passwordView = (TextView) findViewById (R.id.sign_up_password);
		String password = passwordView.getText().toString();
		
		TextView confirmView = (TextView) findViewById (R.id.sign_up_verify);
		String confirm = confirmView.getText().toString();
		
		if (!password.equals (confirm))
			{
			toast_by_resource (R.string.tlogin_pw_no_match);
			return;
			}
		
		if (password.length() < 6)
			{
			toast_by_resource (R.string.tlogin_pw_six);
			return;
			}
		
		if (!email.contains ("@") || email.contains (" "))
			{
			toast_by_resource (R.string.tlogin_valid_email);
			return;
			}
		
		String encoded_email = util.encodeURIComponent (email);
		String encoded_password = util.encodeURIComponent (password);
		
		new playerAPI (in_main_thread, config, "signup?name=" + name + "&email=" + encoded_email + "&password=" + encoded_password)
			{
			public void success (String[] lines)
				{
				signed_in_with_facebook = false;
				config.email = email;
				process_login_data (email, lines);
				toast_by_resource (R.string.sign_up_successful);
	        	toggle_menu (new Callback()
		        	{
					@Override
					public void run()
						{
						activate_layer (layer_before_signin);
						}		        	
		        	});				
				}
			public void failure (int code, String errtext)
				{
				String txt_failure = getResources().getString (R.string.login_failure);	
				alert (txt_failure + ": " + errtext);
				}
			};
		}

	public void process_login_data (String email, String[] lines)
		{
		String token = null;
		String name = null;
		
		log ("login accepted for: " + email);
		
		for (String line: lines)
			{
			log ("login text: " + line);
			String[] fields = line.split ("\t");
			if (fields[0].equals ("token"))
				{
				if (fields.length >= 2)
					token = config.usertoken = fields[1];
				}
			if (fields[0].equals ("name"))
				{
				if (fields.length >= 2)
					name = config.username = fields[1];
				}
			}
		
		if (email == null && signed_in_with_facebook)
			email = config.email = "[via Facebook]";
		else if (email != null)
			config.email = email;
		
		futil.write_file (main.this, "user@" + config.api_server, token);
		
		if (name != null)
			futil.write_file (main.this, "name@" + config.api_server, name);
		
		if (email != null)
			futil.write_file (main.this, "email@" + config.api_server, email);
		}	

	/*** TERMS *****************************************************************************************************/
	
	public void enable_terms_layer()
		{
		disable_video_layer();
		
		set_layer (toplayer.TERMS);
		
		setup_terms_buttons();
		
		if (is_phone())
			{
			View vGossamer = findViewById (R.id.gossamer);
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vGossamer.getLayoutParams();
			layout.leftMargin = pixels_30;
			layout.rightMargin = pixels_30;
			layout.topMargin = pixels_30;
			layout.bottomMargin = pixels_30;
			vGossamer.setLayoutParams (layout);
			}
		
		terms_tab();
		
		/* sometimes the terms layer background is not redrawing! force it here */
		View vTermsLayer = findViewById (R.id.termslayer);
		vTermsLayer.postInvalidate();
		}

	public void setup_terms_buttons()
		{
		View vBack = findViewById (R.id.terms_back);
		if (vBack != null)
			vBack.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: terms back");
		        	slide_away_terms();
		        	}
				});		
		
		View vTermsLayer = findViewById (R.id.termslayer);
		if (vTermsLayer != null)
			vTermsLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});
		
		View vTermsTab = findViewById (R.id.terms_tab);
		if (vTermsTab != null)
			vTermsTab.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: terms tab");
		        	terms_tab();
		        	}
				});

		View vPrivacyTab = findViewById (R.id.privacy_tab);
		if (vPrivacyTab != null)
			vPrivacyTab.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: privacy tab");
		        	privacy_tab();
		        	}
				});
		}

	public void slide_in_terms()
		{
		toggle_menu (new Callback()
	    	{
	    	public void run()
	    		{
	    		enable_terms_layer();
	    		toggle_menu();
	    		}
	    	});
		}
	
	public void slide_away_terms()
		{
    	toggle_menu (new Callback()
	    	{
	    	public void run()
	    		{
	    		enable_signin_layer();
	    		toggle_menu();
	    		}
	    	});
		}
		
	public void terms_tab()
		{
		adjust_terms_tabs (true);
		load_terms_content (R.id.terms_content_html, "terms");
		}	
	
	public void privacy_tab()
		{
		adjust_terms_tabs (false);
		load_terms_content (R.id.privacy_content_html, "privacy");
		}	

	public void adjust_terms_tabs (boolean is_terms)
		{
		View vTermsContent = findViewById (R.id.terms_content);
		View vPrivacyContent = findViewById (R.id.privacy_content);	
		
		vTermsContent.setVisibility (is_terms ? View.VISIBLE : View.GONE);
		vPrivacyContent.setVisibility (is_terms ? View.GONE : View.VISIBLE);
		
		View vTermsTab = findViewById (R.id.terms_tab);
		View vPrivacyTab = findViewById (R.id.privacy_tab);
		
		vTermsTab.setBackgroundResource (is_terms ? R.drawable.gossamerleft : R.drawable.gossamerleftoff);
		vPrivacyTab.setBackgroundResource (is_terms ? R.drawable.gossamerrightoff : R.drawable.gossamerright);		
		}
	
	public void load_terms_content (int id, String action)
		{
		WebView vWebPage = (WebView) findViewById (id); 
		
		vWebPage.setWebViewClient (new WebViewClient()
			{
		    @Override
		    public boolean shouldOverrideUrlLoading (WebView view, String url)
		    	{
		    	return false;
		    	}
			});
		
		String mso = config.mso == null ? "9x9" : config.mso;
		String language = get_language();
		if (language == null)
			language = "en";
		if (language.equals ("tw") || language.equals ("cn"))
			language = "zh";
		
		String url = "http://mobile.9x9.tv/android/" + mso + "/support/" + device_type() + "/" + language + "/";
		if (action != null)
			{
			if (action.equals ("terms") || action.equals ("privacy"))
				url = "http://mobile.9x9.tv/" + mso + "/" + action + "/" + language + "/";
			}
		
		vWebPage.loadUrl (url);
		}

	/*** FACEBOOK **************************************************************************************************/
	
	boolean signed_in_with_facebook = false;
	
	private Session session = null;
	private String access_token = "";
	
	LoginButton loginButton = null;
	public GraphUser user = null;
	
    private UiLifecycleHelper uiHelper = null;
    
	/* stolen */
	public boolean has_facebook()
		{
	    Intent intent = new Intent (Intent.ACTION_SEND);
	    intent.putExtra (Intent.EXTRA_TEXT, "test probe");
	    intent.setType ("text/plain");

	    final PackageManager pm = this.getApplicationContext().getPackageManager();
	    for (ResolveInfo resolveInfo: pm.queryIntentActivities (intent, PackageManager.MATCH_DEFAULT_ONLY))
	    	{
	        ActivityInfo activity = resolveInfo.activityInfo;
	        // log ("activity:: " +  activity.name);
	        if (activity.name.contains ("com.facebook.katana"))
	            return true;
	    	}
	    return false;
		}		
	
    private Session.StatusCallback fb_callback = new Session.StatusCallback()
		{
	    @Override
	    public void call (Session session, SessionState state, Exception exception)
	    	{
	    	main.this.session = session;
	    	log ("FB SESSION STATE CHANGE (STATUS CALLBACK): " + state.toString());
	        onSessionStateChange (session, state, exception);
	        if (state != SessionState.CLOSED)
		        {
		        if (access_token == null || access_token.equals (""))
		        	{
		        	access_token = session.getAccessToken();
		        	}
		    	if (access_token != null && !access_token.equals (""))
		    		{
		    		// alert ("GOT ACCESS TOKEN: " + access_token);
		    		log ("got Facebook access token: " + access_token);
		    		facebook_get_userinfo();
		    		}
		        }
	        else if (config.usertoken != null)
	        	signout();
	    	}
		};

	private void onSessionStateChange (Session session, SessionState state, Exception exception)
		{
		if (exception instanceof FacebookOperationCanceledException)
			log ("FB: FacebookOperationCanceledException");
		else if (exception instanceof FacebookAuthorizationException)
			log ("FB: FacebookAuthorizationException");
	    }
    
	public void fezbuk1 (Bundle savedInstanceState)
		{
		session = new Session.Builder(this).setApplicationId("361253423962738").build();
		
        uiHelper = new UiLifecycleHelper (this, fb_callback);
        uiHelper.onCreate (savedInstanceState);
		}
	
	public void fezbuk2()
		{
	    loginButton = (LoginButton) findViewById (R.id.fblogin);
	    if (loginButton != null)
		    {
		    loginButton.setReadPermissions (Arrays.asList ("email", "user_location", "user_birthday"));
		    loginButton.setUserInfoChangedCallback (new LoginButton.UserInfoChangedCallback()
		    	{
		        @Override
		        public void onUserInfoFetched (final GraphUser user)
		        	{
		        	log ("FACEBOOK LOGIN BUTTON CALLBACK!");
		        	process_fb_user (user);
		        	}
		    	});
		    }
		}

	public void process_fb_user (final GraphUser user)
		{
	    main.this.user = user;
	    
		if (user == null)
			{
			log ("GraphUser is null -- no facebook on this device?");
			return;
			}
	
		if (user.getId() == null)
			{
			log ("GraphUser.getId() is null -- no facebook on this device?");
			return;
			}
		
		if (access_token == null || access_token.equals (""))
			access_token = session.getAccessToken();
			
	    log ("fb id: " + user.getId());
	    log ("fb name: " + user.getName());
	    log ("fb username: " + user.getUsername());
	    log ("fb first name: " + user.getFirstName());
	    log ("fb link: " + user.getLink());
	    log ("fb email: " + user.asMap().get("email"));
	    log ("fb location: " + user.getLocation());
	    log ("fb token: " + access_token);
	    log ("fb access token (saved): " + access_token);
	    log ("fb expire: " + session.getExpirationDate());
	    log ("fb birthday: " + user.getBirthday());
	    
	    Date expire = session.getExpirationDate();
	    long expire_epoch = expire.getTime();
	    
	    if (expire_epoch < 0)
	    	{
	    	/* Facebook is returning a nonsensical negative number, ignore such */
	    	log ("got negative number from FB expire: " + expire_epoch);
	    	expire_epoch = new Date().getTime() + 3600 * 100000;
	    	}
	    
	    if (config.usertoken == null && user.asMap().get("email").toString().contains ("@"))
	    	{
	       	String query = "fbSignup?id=" + user.getId()
					 + "&name=" + util.encodeURIComponent (user.getName())
					 + "&username=" + util.encodeURIComponent (user.getUsername())
					 + "&token=" + util.encodeURIComponent (access_token)
					 + "&email=" + user.asMap().get ("email");
	       	
	    	log ("QUERY: " + query);
	    	
			new playerAPI (in_main_thread, config, query)
				{
				public void success (String[] lines)
					{
					signed_in_with_facebook = true;
					for (String line: lines)
						log ("FB LOGIN: " + line);
					process_login_data ("[via Facebook]", lines);
					alert ("Logged in via Facebook: " + user.getName());					
					query_following (null);
					toggle_menu();
					}
				public void failure (int code, String errtext)
					{
					// String txt_failure = getResources().getString (R.string.login_failure);
					// alert (txt_failure + ": " + errtext);
					alert ("FB LOGIN FAILURE");
					}
				};
	    	}
		}
	
	public void facebook_logout()
		{
		log ("FACEBOOK LOGOUT");
		
		session = Session.getActiveSession();
		
		if (session != null)
			{
			session.close();
			session.closeAndClearTokenInformation();
			toast ("Signed out via Facebook");
			}
		else
			log ("fb session is null");
		
		signout();
		
		config.usertoken = config.username = config.email = null;
		setup_menu_buttons();
		}	
			
	public void facebook_get_userinfo()
		{
		// method not visible!
		// AccessToken accessToken = AccessToken.createFromString (access_token, null, AccessTokenSource.FACEBOOK_APPLICATION_NATIVE);		
		// Session.open (accessToken, null);
		
		StatusCallback or_callback = new StatusCallback()
			{
		    @Override
		    public void call (Session session, SessionState state, Exception exception)
		    	{
		    	if (exception != null)
		    		{
		    		log ("fb openForRead failure");
		    		exception.printStackTrace();
		    		}
		    	log ("fb openForRead callback: opened=" + session.isOpened());
		        }
			};
		
		if (!session.isOpened())
			{
			log ("need to open FB session");
		    Session.OpenRequest openRequest = new Session.OpenRequest (main.this);
		    // openRequest.setLoginBehavior (SessionLoginBehavior.SSO_WITH_FALLBACK);
		    openRequest.setCallback (or_callback);
		    session.openForRead (openRequest);
			}
		else
			log ("FB session already open");
		
		log ("GET FB USERINFO");
		Request request = Request.newMeRequest (session, new Request.GraphUserCallback()
			{
		    @Override
		    public void onCompleted (GraphUser user, Response response)
		    	{
		        // If the response is successful
		        // if (session == Session.getActiveSession())
		        	{
		            if (user != null)
		            	{
		            	log ("got FACEBOOK user info!");
			            process_fb_user (user);
		            	}
		        	}
		        if (response.getError() != null)
		        	{
		            log ("FB me request: error: " + response.getError());
		        	}
		    	}
			});
		request.executeAsync();
		}

	/*** HOME **************************************************************************************************/

	LineItemAdapter channel_overlay_adapter = null;

	HomeSlider home_slider = null;
	StoppableViewPager vHomePager = null;
	
	public void enable_home_layer()
		{
		disable_video_layer();
		set_layer (toplayer.HOME);

		reset_arena_to_home();		
		
		if (home_slider == null)
			{
			log ("+++++++++++++++++++++++++++++++ new HomeSlider +++++++++++++++++++++++++++++++++++");
	        home_slider = new HomeSlider();
	        vHomePager = (StoppableViewPager) home_layer().findViewById (R.id.homepager);
	        vHomePager.setAdapter (home_slider);
			}
		}

	
	class Swaphome
		{
		int set = 0;
		FrameLayout home_page = null;
		String arena[] = null;
		ListView vChannelOverlayList = null;
		LineItemAdapter channel_overlay_adapter = null;
		StoppableListView vChannels = null;
		ChannelAdapter channel_adapter = null;
		public Swaphome (int a_set)
			{
			set = a_set;
			}
		};

	FrameLayout current_home_page = null;
	Swaphome current_swap_object = null;
		
	/* this is implemented using the base class! */
		
    public class HomeSlider extends PagerAdapter
    	{
    	boolean first_time = true;
    	
        @Override
        public int getCount()
        	{
            return portal_stack_ids.length;
        	}

		@Override
		public boolean isViewFromObject (View view, Object object)
			{
			return (((Swaphome) object).home_page) == (FrameLayout) view;
			}
		
		@Override
		public Object instantiateItem (ViewGroup container, int position)
			{
			log ("[PAGER] instantiate: " + position);
			
			final Swaphome sh = new Swaphome (position);			

			FrameLayout home_page = (FrameLayout) View.inflate (main.this, R.layout.home_page, null);
			
			View vTabletPreamble = home_page.findViewById (R.id.tablet_preamble);
			vTabletPreamble.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);

			if (is_tablet())
				{
				ImageView vSetBanner = (ImageView) home_page.findViewById (R.id.set_banner);
				String set_banner = getResources().getString (R.string.set_banner);
				int set_banner_id = getResources().getIdentifier (set_banner, "drawable", getPackageName());
				vSetBanner.setBackgroundResource (set_banner_id);
				}
			
			if (is_tablet())
				{
				View vChannelList = home_page.findViewById (R.id.channel_list);
				LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vChannelList.getLayoutParams();
				layout.leftMargin = 0;
				layout.rightMargin = 0;
				vChannelList.setLayoutParams (layout);
				}
			
			diminish_side_titles (home_page, true);
			
			home_page.setTag (R.id.container, position);			
			sh.home_page = home_page;
			((StoppableViewPager) container).addView (home_page, 0);
			
			TextView vTitle = (TextView) sh.home_page.findViewById (R.id.primary_set_title);
			vTitle.setText (portal_stack_names [position]);
			
			TextView vLeft = (TextView) sh.home_page.findViewById (R.id.left_set_title);
			vLeft.setText (position == 0 ? "" : portal_stack_names [position-1]);

			TextView vRight = (TextView) sh.home_page.findViewById (R.id.right_set_title);
			vRight.setText (position == portal_stack_ids.length - 1 ? "" : portal_stack_names [position+1]);
			
			View vModeThumbs = sh.home_page.findViewById (R.id.mode_thumbs);
			if (vModeThumbs != null)
				vModeThumbs.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: thumb mode");
			        	sh.channel_adapter.mini_mode = false;
			        	sh.channel_adapter.notifyDataSetChanged();
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
			        	sh.channel_adapter.mini_mode = true;
			        	sh.channel_adapter.notifyDataSetChanged();
			        	}
					});				
			
			query_pile (portal_stack_ids [position], new Callback()
				{
				public void run_string (String id)
					{
					sh.arena = config.list_of_channels_in_set (id);
					
					sh.vChannelOverlayList = (ListView) sh.home_page.findViewById (R.id.channel_overlay_list);		
					if (sh.vChannelOverlayList != null)
						{
						sh.channel_overlay_adapter = new LineItemAdapter (main.this, sh.arena, toplayer.HOME);
						sh.vChannelOverlayList.setAdapter (sh.channel_overlay_adapter);
						}
					
					sh.vChannels = (StoppableListView) sh.home_page.findViewById (R.id.channel_list);
					if (sh.vChannels != null)
						{
						sh.channel_adapter = new ChannelAdapter (main.this, sh.arena, sh);
						sh.vChannels.setAdapter (sh.channel_adapter);
			    		LayoutInflater inflater = main.this.getLayoutInflater();
			    		View shim = inflater.inflate (R.layout.footer_shim, null);
			    		sh.vChannels.addFooterView (shim);
						}
					
					Runnable channel_thumberino = new Runnable()
						{
						public void run()
							{
							if (sh.channel_adapter != null)
								sh.channel_adapter.notifyDataSetChanged();
							}
						};
					thumbnail.stack_thumbs (main.this, config, sh.arena, in_main_thread, channel_thumberino);
					}
				});
								
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
			log ("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% primary 3x3: " + position);
			diminish_side_titles (current_home_page, true);
			current_swap_object = (Swaphome) object;
			current_home_page = ((Swaphome) object).home_page;
			diminish_side_titles (current_home_page, false);
			}
    	}

    
    
	public void setup_channel_overlay (final View parent)
		{
		if (is_phone())
			{
			View vChannelOverlay = parent.findViewById (R.id.channel_overlay);
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vChannelOverlay.getLayoutParams();
			layout.topMargin = 0; // -pixels_80;
			layout.bottomMargin = pixels_40;
			layout.leftMargin = pixels_40;
			layout.rightMargin = pixels_40;
			vChannelOverlay.setLayoutParams (layout);
			}	
		
		ListView vChannelOverlayList = (ListView) parent.findViewById (R.id.channel_overlay_list);		
		if (vChannelOverlayList != null)
			{
			channel_overlay_adapter = new LineItemAdapter (this, arena, toplayer.HOME);
			vChannelOverlayList.setAdapter (channel_overlay_adapter);
			}
		else
			alert ("NO CHANNEL OVERLAY LIST");
		
		if (vChannelOverlayList != null)
			vChannelOverlayList.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView parent, View v, int position, long id)
					{
					if (position < arena.length)
						{
						log ("channel overlay click: " + position);
						toggle_channel_overlay (parent);
						String channel_id = channel_overlay_adapter.get_id (position);
			        	launch_player (channel_id, arena);
						}
					}
				});							
		}
			
	public void toggle_channel_overlay (View parent)
		{
		View vChannelOverlay = home_layer().findViewById (R.id.channel_overlay);
		boolean is_visible = vChannelOverlay.getVisibility() == View.VISIBLE;
		vChannelOverlay.setVisibility (is_visible ? View.GONE : View.VISIBLE);
		}
	
	public void reset_arena_to_home()
		{
		if (current_home_stack != null)
			{
			if (config.number_of_channels_in_set (current_home_stack) > 0)
				{
				log ("reset arena to set: " + current_home_stack);
				arena = config.list_of_channels_in_set (current_home_stack);
				}
			else
				{
				log ("set \"" + current_home_stack + "\" seems to have vanished, repopulating...");				
				populate_home();
				}
			}
		else
			log ("reset_arena_to_home: current home stack is null!");
		}
	
	public void populate_home()
		{
		portal_frontpage();
		}
	
	public void refresh_home()
		{
		vc_cache = new Hashtable < String, String[] > ();
		config.init_pools();
		populate_home();
		}
	
	/* ------------------------------------ frontpage ------------------------------------ */
	
	public void network_retry()
		{
		AlertDialog.Builder builder = new AlertDialog.Builder (main.this);
		
		String txt_not_ready = getResources().getString (R.string.network_not_ready);
		String txt_retry = getResources().getString (R.string.retry);
		String txt_quit = getResources().getString (R.string.quit);
		
		builder.setMessage (txt_not_ready);
		builder.setPositiveButton (txt_retry, new DialogInterface.OnClickListener()
			{
			@Override
			public void onClick (DialogInterface arg0, int arg1)
				{
				portal_frontpage();
				}
			});
		
		builder.setNeutralButton (txt_quit, new DialogInterface.OnClickListener()
			{
			@Override
			public void onClick (DialogInterface arg0, int arg1)
				{
				exit_stage_left();
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
			alert ("Unable to connect to network");
			}
		}
	
	long frontpage_start = 0L;

	public void portal_frontpage()
		{
		Calendar now = Calendar.getInstance();
		int hour = now.get (Calendar.HOUR_OF_DAY);
		
		frontpage_start = System.currentTimeMillis();
		
		new playerAPI (in_main_thread, config, "portal?time=" + hour + "&v=32")
			{
			public void success (String[] lines)
				{
				if (lines.length < 3)
					{
					alert ("Frontpage failure");
					}
				else
					{
					long frontpage_end = System.currentTimeMillis();
					
					log ("frontpage API took: " + ((frontpage_end - frontpage_start) / 1000L) + " seconds");
	
					frontpage_start = System.currentTimeMillis() / 1000L;
					process_portal_frontpage_data (lines);
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
					alert ("Frontpage failure: " + errtext.replaceAll ("^ERROR:", ""));
				}
			};
		}

	String portal_stack_ids[] = null;
	String portal_stack_names[] = null;
	String portal_stack_episode_thumbs[] = null;
	String portal_stack_channel_thumbs[] = null;	
	
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
				if (fields[1].equals ("Nordstrome"))
					fields[1] = "Nordstrom";
				portal_stack_ids [stack_count] = fields [0];
				portal_stack_names [stack_count] = fields [1];
				if (fields.length >= 4)
					portal_stack_episode_thumbs [stack_count] = fields [3];
				if (fields.length >= 6)
					portal_stack_channel_thumbs [stack_count] = fields [5];				
				stack_count++;
				log ("frontpage :: " + fields[0] + ": " + fields[1]);
				}
			}
	
		// init_home();
		enable_home_layer();
		}	
	 
	LinearLayout sets_scroller = null;
	
	public void OLD_setup_sets_scroller()
		{
		if (sets_scroller == null)
			{
			String text_array[] = { "Daily News", "Selected YouTube", "Whatever's on Third", "Chinese News" };
			HorizontalScrollView vHoriz = null ; // (HorizontalScrollView) findViewById (R.id.sets_horiz);
			sets_scroller = new LinearLayout (main.this);
			sets_scroller.setLayoutParams (new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			// LinearLayout vHorizInside = (LinearLayout) findViewById (R.id.sets_horiz_inside);
			sets_scroller.setOrientation(LinearLayout.HORIZONTAL);
			
			int count = 0;
			View item = View.inflate (main.this, R.layout.set_item, null);
			sets_scroller.addView (item, count++);
			for (int i = 0; i < portal_stack_names.length; i++)
				{
				item = View.inflate (main.this, R.layout.set_item, null);
				TextView vText = (TextView) item.findViewById (R.id.set_title);
				vText.setText (portal_stack_names[i]);
				sets_scroller.addView (item, count++);
				
				final int set_index = i;
				item.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	String set_id = portal_stack_ids [set_index];
			        	log ("click on: set scroller, set " + set_id);
			        	
			        	if (!set_id.equals (current_home_stack))
			        		{
			        		// init_home_with_set (set_id);
			        		OLD_update_sets_scroller();
			        		}
			        	else
			        		{
			        		channel_overlay_adapter.set_content (arena);
			        		// toggle_channel_overlay(); TODO:
			        		}
			        	}
					});	
				}
			
			item = View.inflate (main.this, R.layout.set_item, null);
			sets_scroller.addView (item, count++);
			
			vHoriz.addView (sets_scroller);
			
			OLD_update_sets_scroller();
			}
		}
	
	public void OLD_update_sets_scroller()
		{
		for (int i = 1; i < sets_scroller.getChildCount() - 1; i++)
			{
			View v = sets_scroller.getChildAt (i);
			TextView vTitle = (TextView) v.findViewById (R.id.set_title);
			View vChevron = v.findViewById (R.id.down_chevron);
			if (v != null)
				{
				log ("+++ PS " + (i-1) + " = " + portal_stack_ids[i-1] + " vs current: " + current_home_stack);
				if (portal_stack_ids[i-1].equals (current_home_stack))
					{
					vTitle.setTextColor (Color.argb (0xFF, 0xFF, 0xFF, 0xFF));
					v.setBackgroundColor (Color.argb (0xFF, 0x00, 0x00, 0x00));
					vChevron.setVisibility (View.VISIBLE);
					}
				else
					{
					vTitle.setTextColor (Color.argb (0xFF, 0x00, 0x00, 0x00));
					v.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
					vChevron.setVisibility (View.GONE);
					}
				}
			}
		}
		
	public void query_pile (String id, Callback callback)
		{
		if (id.equals ("virtual:following"))
			query_following (callback);
		else
			query_set_info (id, callback);
		}
	
	public void query_following (final Callback callback)
		{
		if (config.usertoken == null)
			{
			if (callback != null)
				callback.run_string ("virtual:following");
			return;
			}
		
		final String query = "channelLineup?user=" + config.usertoken + "&setInfo=true&v=32";
		
		if (vc_cache.get (query) != null)
			{
			log ("using cached value");
			config.parse_channel_info_with_setinfo (vc_cache.get (query));
			if (callback != null)
				callback.run_string ("virtual:following");
			return;
			}
		
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (in_main_thread, config, query)
			{
			public void success (String[] chlines)
				{
				try
					{
					long vc_end = System.currentTimeMillis();
					long elapsed = (vc_end - vc_start) / 1000L;
					log ("[" + query + "] lines received: " + chlines.length + ", elapsed: " + elapsed);
					config.parse_channel_info_with_setinfo (chlines);
					if (callback != null)
						callback.run_string ("virtual:following");
					String subscribed_channels[] = config.subscribed_channels();
					thumbnail.stack_thumbs (main.this, config, subscribed_channels, in_main_thread, subscription_thumb_updated);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				alert ("ERROR! " + errtext);
				}
			};
		}

	final Runnable subscription_thumb_updated = new Runnable()
		{
		public void run()
			{
			if (current_layer == toplayer.GUIDE)
				{
				episode_thumbs_updated.run();
				}
			}
		};
	
	Hashtable < String, String[] > vc_cache = new Hashtable < String, String[] > ();
	
	void query_set_info (final String id, final Callback callback)
		{
		String query = null;
		
		if (id.equals ("virtual:shared"))
			{
			// query = "shareInChannelList?channel=" + shared_channel;
			}
		else
			{
			String short_id = id.replaceAll ("^virtual:", "");
			query = "setInfo?set=" + short_id + "&v=32";
			}
		
		log (query);
		
		if (vc_cache.get (query) != null)
			{
			log ("using cached value");
			config.parse_setinfo (id, vc_cache.get (query));
			callback.run_string (id);
			return;
			}
		
		final String final_query = query;
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (in_main_thread, config, final_query)
			{
			public void success (String[] chlines)
				{
				try
					{
					long vc_end = System.currentTimeMillis();
					long elapsed = (vc_end - vc_start) / 1000L;
					log ("[" + final_query + "] lines received: " + chlines.length + ", elapsed: " + elapsed);
					vc_cache.put (final_query, chlines);
					if (id.equals ("virtual:shared"))
						{
						// shared_set_owner = config.parse_setinfo (id, chlines);
						// adjust_piles();
						}
					else
						config.parse_setinfo (id, chlines);
					callback.run_string (id);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				alert ("ERROR! " + errtext);
				}
			};
		}

	public void redraw_home()
		{
		}
	
	class ChannelAdapter extends ArrayAdapter <String>
		{
		Activity context;
		String content[] = null;
		boolean requested_channel_load[] = null;
		boolean requested_channel_thumbs[] = null;
		Swaphome sh = null;
		boolean mini_mode = true;
		
		ChannelAdapter (Activity context, String content[], Swaphome sh)
			{
			super (context, is_tablet() ? R.layout.channel_tablet : R.layout.channel, content);
			this.context = context;
			this.content = content;
			requested_channel_load = new boolean [content.length];
			Arrays.fill (requested_channel_load, Boolean.FALSE);
			requested_channel_thumbs = new boolean [content.length];
			Arrays.fill (requested_channel_thumbs, Boolean.FALSE);
			this.sh = sh;
			}
	
		@Override
		public int getCount()
			{
			return content.length - 1;
			}
		
		@Override
		public View getView (final int position, View convertView, ViewGroup parent)
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
			
			View row = convertView;			
			
			int wanted_layout_type = is_tablet() ? (mini_mode ? R.layout.channel_mini : R.layout.channel_tablet) : R.layout.channel;			
			int cached_layout_type = wanted_layout_type;
								
			/* determine what type of row this actually is */
			if (row != null && is_tablet())
				{
				/* episode2 id only occurs in thumb mode */
				cached_layout_type = (row.findViewById (R.id.episode2) != null) ? R.layout.channel_tablet : R.layout.channel_mini;
				}
			
			if (wanted_layout_type == R.layout.channel)
				log ("layout type: R.layout.channel");
			else if (wanted_layout_type == R.layout.channel_tablet)
				log ("layout type: R.layout.channel_tablet");
			else if (wanted_layout_type == R.layout.channel_mini)
				log ("layout type: R.layout.channel_mini");
			
			if (true || row == null || wanted_layout_type != cached_layout_type)
				{
				row = inflater.inflate (wanted_layout_type, null);
				}

			if (is_phone())
				adjust_for_device (row);			
			
			TextView vTitle = (TextView) row.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (name != null ? name : "?");

			ImageView vChannelIcon = (ImageView) row.findViewById (R.id.channel_icon);
			View vProgress = row.findViewById (R.id.progress);
			
			boolean channel_thumbnail_found = false;
			if (1 == 2)
				{
				String filename = getFilesDir() + "/" + config.api_server + "/xthumbs/" + channel_id + ".png";
				
				File f = new File (filename);
				if (f.exists())
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						channel_thumbnail_found = true;
						vChannelIcon.setImageBitmap (bitmap);
						}
					}
				}
			

			View vChannelFrame = row.findViewById (R.id.channel_frame);
			
			if (is_tablet())
				{
				float factor = mini_mode ? 0.3f : 0.45f;
				int width = (int) ((screen_width - pixels_40) * factor);
				int height = (int) ((float) width / 1.77);
				// FrameLayout.LayoutParams pic_layout = (FrameLayout.LayoutParams) vChannelIcon.getLayoutParams();
				LinearLayout.LayoutParams pic_layout = (LinearLayout.LayoutParams) vChannelFrame.getLayoutParams();
				pic_layout.height = height;
				pic_layout.width = width;
				vChannelFrame.setLayoutParams (pic_layout);
				}
			else
				{
				int width = screen_width;
				int height = (int) ((float) width / 1.77 * 0.8);
				FrameLayout.LayoutParams pic_layout = (FrameLayout.LayoutParams) vChannelIcon.getLayoutParams();
				pic_layout.height = height;
				vChannelIcon.setLayoutParams (pic_layout);
				}
			
			// vProgress.setVisibility (channel_thumbnail_found ? View.GONE : View.VISIBLE);
			vProgress.setVisibility (View.GONE);
			
			if (!channel_thumbnail_found)
				{
				if (vChannelIcon != null)
					vChannelIcon.setImageResource (R.drawable.store_unavailable);					
				}	
			
			// View vChannelFrame = row.findViewById (R.id.channel_frame);
			if (vChannelFrame != null)
				vChannelFrame.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: channel " + channel_id);
			        	launch_player (channel_id, content);
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
			        	launch_player (channel_id, content);
			        	}
					});	
			
			ImageView vFollow = (ImageView) row.findViewById (R.id.follow);
			if (vFollow != null)
				{
				vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow);
				vFollow.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: home follow/unfollow channel " + channel_id);
			        	follow_or_unfollow (channel_id);
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
			        	share_episode (channel_id, null);
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
			
			String program_line[] = config.program_line_by_id (channel_id);
			if (program_line != null && program_line.length > 0)
				{
				fill_in_four_episode_thumbs (channel_id, program_line, row, sh, requested_channel_thumbs [position]);
				requested_channel_thumbs [position] = true;
				}
			else
				{
				Callback after_load = new Callback()
					{
					@Override
					public void run_string_and_object (final String channel_id, Object row)
						{
						String new_program_line[] = config.program_line_by_id (channel_id);
						fill_in_four_episode_thumbs (channel_id, new_program_line, (View) row, sh, requested_channel_thumbs [position]);
						requested_channel_thumbs [position] = true;
						notifyDataSetChanged();
						}
					};
				TextView vSubTitle = (TextView) row.findViewById (R.id.subtitle);
				if (vSubTitle != null)
					vSubTitle.setText ("[...]");
				if (!requested_channel_load [position])
					{
					requested_channel_load [position] = true;
					load_channel_then (channel_id, after_load, channel_id, row);
					}
				}

			TextView vChannelName = (TextView) row.findViewById (R.id.channel_name);
			if (vChannelName != null)
				vChannelName.setText (name != null ? name : "?");
			
			int num_episodes = config.programs_in_real_channel (channel_id);

			String txt_episode = getResources().getString (R.string.episode_lc);
			String txt_episodes = getResources().getString (R.string.episodes_lc);
			
			TextView vEpisodeCount = (TextView) row.findViewById (R.id.episode_count);
			if (vEpisodeCount != null)
				vEpisodeCount.setText ("" + num_episodes);
			
			TextView vEpisodePlural = (TextView) row.findViewById (R.id.episode_plural);
			if (vEpisodePlural != null)
				vEpisodePlural.setText (num_episodes == 1 ? txt_episode : txt_episodes);
			
			TextView vAgo = (TextView) row.findViewById (R.id.ago);
			if (vAgo != null)
				{				
				if (num_episodes > 0 && program_line != null && program_line.length > 0)
					{
					vAgo.setVisibility (View.VISIBLE);
					String episode_id = program_line [0];
					String timestamp = config.program_meta (episode_id, "timestamp");
					String ago = util.ageof (Long.parseLong (timestamp) / 1000);
					vAgo.setText (ago);
					}
				else
					vAgo.setVisibility (View.GONE);
				}
			
			boolean small_channel_thumbnail_found = false;
			
			ImageView vSmallChannelIcon = (ImageView) row.findViewById (R.id.small_channel_icon);
			if (vSmallChannelIcon != null)
				{
				String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
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

			
			return row;
			}
		
		public void adjust_for_device (View row)
			{
			if (is_phone())
				{				
				TextView vFirstEpisodeTitle = (TextView) row.findViewById (R.id.first_episode_title);
				vFirstEpisodeTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 24);
				
				TextView vAgo = (TextView) row.findViewById (R.id.ago);
				vAgo.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
				
				View vSmallChannelIcon = row.findViewById (R.id.small_channel_icon);
				LinearLayout.LayoutParams layout6 = (LinearLayout.LayoutParams) vSmallChannelIcon.getLayoutParams();
				layout6.height = pixels_40;
				layout6.width = pixels_40;
				vSmallChannelIcon.setLayoutParams (layout6);
				
				TextView vChannelFromHeader = (TextView) row.findViewById (R.id.channel_from_header);
				vChannelFromHeader.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
				
				TextView vChannelName = (TextView) row.findViewById (R.id.channel_name);
				vChannelName.setTextSize (TypedValue.COMPLEX_UNIT_SP, 18);
				
				TextView vEpisodeCount = (TextView) row.findViewById (R.id.episode_count);
				vEpisodeCount.setTextSize (TypedValue.COMPLEX_UNIT_SP, 20);
				
				TextView vEpisodePlural = (TextView) row.findViewById (R.id.episode_plural);
				vEpisodePlural.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
				}
			}
		}
		
	public void fill_in_four_episode_thumbs (final String channel_id, String program_line[], View parent, final Swaphome sh, boolean requested_thumbs)
		{
		boolean e0_found = true, e1_found = true, e2_found = true, e3_found = true;
		
		Runnable triple_update_thumbs = new Runnable()
			{
			@Override
			public void run()
				{
				log ("triple update thumbs: " + channel_id);
				if (sh.channel_adapter != null)
					sh.channel_adapter.notifyDataSetChanged();
				}
			};
			
		if (program_line != null)
			{
			if (program_line.length >= 1)
				{	
				e0_found = fill_in_episode_thumb (program_line[0], parent, R.id.channel_icon, R.id.first_episode_title);
				}
			if (program_line.length >= 2)
				{
				if (is_tablet())
					e1_found = fill_in_episode_thumb (program_line[1], parent, R.id.episode1, R.id.episode1_title);
				}
			if (program_line.length >= 3)
				{
				if (is_tablet())
					e2_found = fill_in_episode_thumb (program_line[2], parent, R.id.episode2, R.id.episode2_title);
				}
			if (is_phone())
				{
				View vEpisode3 = parent.findViewById (R.id.episode3);
				if (vEpisode3 != null)
					vEpisode3.setVisibility (View.GONE);
				}			
			else
				{
				if (program_line.length >= 4)
					{
					if (is_tablet())
						e3_found = fill_in_episode_thumb (program_line[3], parent, R.id.episode3, R.id.episode3_title);
					}
				}
			
			if (!e0_found || !e1_found || !e2_found || !e3_found)
				{
				if (!requested_thumbs)
					thumbnail.download_first_n_episode_thumbs
							(main.this, config, channel_id, is_tablet() ? 4 : 1, in_main_thread, triple_update_thumbs);
				}
			}
		else
			{
			fill_in_episode_thumb (null, parent, R.id.episode1, R.id.episode1_title);
			if (is_tablet())
				{
				fill_in_episode_thumb (null, parent, R.id.episode2, R.id.episode2_title);
				fill_in_episode_thumb (null, parent, R.id.episode3, R.id.episode3_title);
				}
			}
		
		if (program_line != null && program_line.length >= 1)
			{
			TextView vSubTitle = (TextView) parent.findViewById (R.id.subtitle);
			if (vSubTitle != null)
				{
				String episode_id = program_line [0];
				String timestamp = config.program_meta (episode_id, "timestamp");
				String ago = util.ageof (Long.parseLong (timestamp) / 1000);
				String txt_episode = getResources().getString (R.string.episode_lc);
				String txt_episodes = getResources().getString (R.string.episodes_lc);
				String subtitle = ago + " • " + program_line.length + " " + (program_line.length == 1 ? txt_episode : txt_episodes);
				vSubTitle.setText (subtitle);
				}
			}
		}
	
	public boolean fill_in_episode_thumb (String episode_id, View parent, int resource_id, int title_resource_id)
		{	
		boolean thumbnail_found = false;
		
		String episode_title = "";
		if (episode_id != null)
			episode_title = config.program_meta (episode_id, "name");
		
		ImageView vThumb = (ImageView) parent.findViewById (resource_id);
		if (vThumb != null)
			{
			if (episode_id != null)
				{
				log ("********************** FILL IN EPISODE THUMB res(" + resource_id + ") episode(" + episode_id + ")");
				String filename = main.this.getFilesDir() + "/" + config.episode_in_cache (episode_id);
				
				File f = new File (filename);
				if (f.exists())
					{
					if (f.length() > 0)
						{
						if (false && resource_id == R.id.channel_icon)
							{
							Bitmap bitmap = BitmapFactory.decodeFile (filename);
							if (bitmap != null)
								{
								Bitmap bitmap2 = bitmappery.getRoundedTopTwoCorners (bitmap);
								if (bitmap2 != null)
									{
									vThumb.setImageBitmap (bitmap2);
									thumbnail_found = true;
									}
								}			
							}
						else
							{
							Bitmap bitmap = BitmapFactory.decodeFile (filename);
							if (bitmap != null)
								vThumb.setImageBitmap (bitmap);
							}
						}
					else
						{
						vThumb.setImageResource (R.drawable.store_unavailable);
						}
					thumbnail_found = true;
					}				
				}
			if (!thumbnail_found)
				vThumb.setImageResource (R.drawable.store_unavailable);
			}
		else
			return true;
		
		if (title_resource_id != 0)
			{
			TextView vTitle = (TextView) parent.findViewById (title_resource_id);
			if (vTitle != null)
				vTitle.setText (episode_title);
			}
		
		return thumbnail_found;
		}
	
	public EpisodeAdapter setup_horiz (HorizontalListView horiz, final String channel_id)
		{
		final EpisodeAdapter horiz_adapter = new EpisodeAdapter (this, channel_id);
				       
		if (horiz == null)
			return null;
		
		horiz.setAdapter (horiz_adapter);
	       
		horiz.setOnItemClickListener (new OnItemClickListener()
			{
			public void onItemClick (AdapterView <?> parent, View v, int position, long id)
				{
				Log.i ("vtest", "playback click: " + position);
				play_nth_episode_in_channel (horiz_adapter.get_channel_id(), position + 1);
				}
			});
	
		horiz.setOnScrollListener (new OnScrollListener()
			{
			@Override
			public void onScrollStarted()
				{
				StoppableListView vChannel = (StoppableListView) findViewById (R.id.channel_list);
				vChannel.setPagingEnabled (false);
				}

			@Override
			public void onScrollFinished()
				{
				StoppableListView vChannel = (StoppableListView) findViewById (R.id.channel_list);
				vChannel.setPagingEnabled (true);
				}
			});
			
			
		load_channel_then (channel_id, setup_horiz_inner, channel_id, horiz);

		return horiz_adapter;
		}
	
	final Callback setup_horiz_inner = new Callback()
		{
		@Override
		public void run_string_and_object (final String channel_id, Object horiz_obj)
			{
			final HorizontalListView horiz = (HorizontalListView) horiz_obj;
			Runnable horiz_update_thumbs = new Runnable()
				{
				@Override
				public void run()
					{
					log ("horiz update thumbs: " + channel_id);
					EpisodeAdapter horiz_adapter = (EpisodeAdapter) horiz.getAdapter();
					horiz_adapter.notifyDataSetChanged();
					}
				};

			thumbnail.download_episode_thumbnails (main.this, config, channel_id, in_main_thread, horiz_update_thumbs);
			}
		};
	
	class EpisodeAdapter extends ArrayAdapter <String>
		{
		Activity context;
		String channel_id = null;
		
		String program_line[] = null;
		
		EpisodeAdapter (Activity context, String channel_id)
			{
			super (context, R.layout.episode);
			config.program_line_by_id (channel_id);
			this.context = context;
			this.channel_id = channel_id;
			}
		
		public void set_channel_id (String channel_id)
			{
			if (!channel_id.equals (this.channel_id))
				{
				this.channel_id = channel_id;
	    		program_line = config.program_line_by_id (channel_id);
				notifyDataSetChanged();
				}
			}
		
		public String get_channel_id()
			{
			return channel_id;
			}
		
	    @Override  
	    public int getCount()
	    	{  
	    	if (program_line == null || program_line.length == 0)
	    		program_line = config.program_line_by_id (channel_id);
	        return program_line != null ? program_line.length : 0;  
	    	}  
	
	    @Override  
	    public View getView (int position, View convertView, ViewGroup parent)
	    	{
	    	View row = convertView;
	    		    	
	    	if (row == null)
	    		{
	    		LayoutInflater inflater = context.getLayoutInflater();
	    		row = inflater.inflate (R.layout.episode, null);
	    		}
			
	    	log ("horiz " + channel_id + " getView: " + position);
	    	
			TextView vEptitle = (TextView) row.findViewById (R.id.eptitle);
			ImageView vPic = (ImageView) row.findViewById (R.id.pic);
			ProgressBar vProgress = (ProgressBar) row.findViewById (R.id.progress);
			
			String program_id = program_line [position];
			
			vEptitle.setText (config.program_meta (program_id, "name"));
			
			int gray = Color.rgb (0xC0, 0xC0, 0xC0);
			int orange = Color.rgb (0xFF, 0x99, 0x00);
			int black = Color.rgb (0x00, 0x00, 0x00);
			
			View vBorder = row.findViewById (R.id.border);
			vBorder.setBackgroundColor (position == current_episode_index - 1 ? orange : black);
			
			// vEptitle.setTextColor ((position + 1 == current_episode_index) ? orange : gray);
			
			String filename = main.this.getFilesDir() + "/" + config.episode_in_cache (program_id);
			File f = new File (filename);
			if (f.exists())
				{
				vProgress.setVisibility (View.GONE);
				vPic.setVisibility (View.VISIBLE);
				if (f.length() > 0)
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					vPic.setImageBitmap (bitmap);
					}
				else
					{
					vPic.setImageResource (R.drawable.store_unavailable);
					}
				}
			else
				{
				vPic.setVisibility (View.GONE);
				vProgress.setVisibility (View.VISIBLE);
				}
			
			return row;
	    	}
	    };
	
    public class SetsAdapter extends PagerAdapter
    	{
    	 Context ctx;
    	 String text_array[];

    	 public SetsAdapter (Context ctx, String[] text_array)
    	 	{
    		this.text_array = text_array;
    		this.ctx = ctx;
    	 	}

    	 public int getCount()
    	 	{
    		return text_array.length;
    	 	}

    	 @Override
    	 public Object instantiateItem (View collection, int position)
    	 	{
    		// TextView view = new TextView (ctx);
    		// view.setText (text_array [position]);
    		View view = inflater.inflate (R.layout.set_item, null); 
    		TextView vTitle = (TextView) view.findViewById (R.id.set_title);
    		vTitle.setText (text_array [position]);
    		LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTitle.getLayoutParams();  
    		layout.width = (int) ((float) screen_width * 0.8);
    		vTitle.setLayoutParams (layout);
    		view.setLayoutParams (new LayoutParams ((int) ((float) screen_width * 0.33), LayoutParams.MATCH_PARENT));
    		((ViewPager) collection).addView (view, 0);
    		return view;
    	 	}

    	View previous_primary_item = null;
    	
 		@Override
 		public void setPrimaryItem (ViewGroup container, int position, Object object)
 			{
 			View view = (View) object;

    		TextView vTitle = (TextView) view.findViewById (R.id.set_title);
    		vTitle.setBackgroundColor (Color.rgb (0xA0, 0xA0, 0xA0));   	  		
    		if (previous_primary_item != null)
    			{
        		TextView vPrevTitle = (TextView) previous_primary_item.findViewById (R.id.set_title);
    			vPrevTitle.setBackgroundColor (Color.rgb (0xFF, 0xFF, 0xFF));
    			}
    		vTitle.setBackgroundColor (Color.rgb (0xA0, 0xA0, 0xA0));  	
    		previous_primary_item = view;
 			}
 		
    	@Override
    	public void destroyItem (View arg0, int arg1, Object arg2)
    		{
    		((ViewPager) arg0).removeView ((View) arg2);
    	 	}

    	@Override
    	public boolean isViewFromObject (View arg0, Object arg1)
    	 	{
    		return arg0 == ((View) arg1);
    	 	}

    	@Override
    	public Parcelable saveState()
    	 	{
    		return null;
    	 	}
    	}
	    
	/*** PLAYER **************************************************************************************************/
	    
	EpisodeAdapter playback_episode_adapter = null;
	PlaybackChannelAdapter playback_channel_adapter = null;
	PlaybackCommentsAdapter playback_comments_adapter = null;
	
	public void play_channel (String channel_id)
		{
		if (current_layer != toplayer.PLAYBACK)
			{
			enable_player_layer();
			setup_player_adapters (channel_id);
			}

		play_first (channel_id);
		}
	
	public void play_nth_episode_in_channel (String channel_id, int position)
		{
		program_line = config.program_line_by_id (channel_id);
		String episode_id = program_line [position - 1];
		
		if (current_layer != toplayer.PLAYBACK)
			{
			enable_player_layer();
			setup_player_adapters (channel_id);
			}
		
		redraw_playback (channel_id, episode_id);
		play_nth (channel_id, position);
		}

	final Runnable playback_episode_thumb_updated = new Runnable()
		{
		public void run()
			{
			if (playback_episode_adapter != null)
				playback_episode_adapter.notifyDataSetChanged();
			}
		};
	
	final Runnable playback_comments_updated = new Runnable()
		{
		public void run()
			{
			TextView vNumComments = (TextView) findViewById (R.id.num_comments);
			
			if (playback_comments_adapter != null)
				{
				String num_comments = config.program_meta (current_episode_id, "maxcomment");
				log ("playback comments updated (episode " + current_episode_id + ", " + num_comments + " comments)");
				if (num_comments != null && !num_comments.equals (""))
					{
					playback_comments_adapter.set_episode_id (current_episode_id);
					playback_comments_adapter.set_number_of_comments (Integer.parseInt (num_comments));
					vNumComments.setText (num_comments);
					playback_comments_adapter.notifyDataSetChanged();
					}
				else
					vNumComments.setText ("0");
				
				playback_comments_adapter.notifyDataSetChanged();
				}
			}
		};
		
	String current_episode_id = null;
	
	@Override
	public void onVideoActivityRefreshMetadata (String channel_id, String episode_id)
		{
		log ("onVideoActivityRefreshMetadata");
		
		current_episode_id = episode_id;
		
		if (current_layer == toplayer.PLAYBACK)
			{
			if (playback_episode_adapter != null)
				playback_episode_adapter.set_channel_id (channel_id);
			if (playback_channel_adapter != null)				
				playback_channel_adapter.notifyDataSetChanged();
						
			playback_comments_updated.run();
			
			thumbnail.download_episode_thumbnails (main.this, config, channel_id, in_main_thread, playback_episode_thumb_updated);
			
			ImageView vFollow = (ImageView) findViewById (R.id.playback_follow);
			if (vFollow != null)
				vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow);
			
			ImageView vFollowLandscape = (ImageView) findViewById (R.id.playback_follow_landscape);
			if (vFollowLandscape != null)
				vFollowLandscape.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow);
			
			ytchannel.fetch_youtube_comments_in_thread (in_main_thread, playback_comments_updated, config, episode_id);
			
			String channel_name = config.pool_meta (channel_id, "name");
			int num_episodes = config.programs_in_real_channel (channel_id);
			String episode_desc = config.program_meta (episode_id, "desc");
			
			TextView vChannelName = (TextView) findViewById (R.id.playback_channel);
			if (vChannelName != null)
				vChannelName.setText (channel_name);
			
			TextView vDesc = (TextView) findViewById (R.id.playback_episode_description);
			if (vDesc != null)
				vDesc.setText (episode_desc);
			
			String txt_episode = getResources().getString (R.string.episode_lc);
			String txt_episodes = getResources().getString (R.string.episodes_lc);
			
			TextView vEpisodeCount = (TextView) findViewById (R.id.playback_episode_count);
			if (vEpisodeCount != null)
				vEpisodeCount.setText ("" + num_episodes);
			
			TextView vEpisodePlural = (TextView) findViewById (R.id.playback_episode_plural);
			if (vEpisodePlural != null)
				vEpisodePlural.setText (num_episodes == 1 ? txt_episode : txt_episodes);
			
			boolean channel_thumbnail_found = false;
			
			ImageView vChannelIcon = (ImageView) findViewById (R.id.playback_channel_icon);
			ImageView vChannelIconLandscape = (ImageView) findViewById (R.id.playback_channel_icon_landscape);
			if (vChannelIcon != null || vChannelIconLandscape != null)
				{
				String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
				File f = new File (filename);
				if (f.exists())
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						Bitmap bitmap2 = bitmappery.getRoundedCornerBitmap (bitmap, 70);
						if (bitmap2 != null)
							{
							if (vChannelIcon != null)
								vChannelIcon.setImageBitmap (bitmap2);
							if (vChannelIconLandscape != null)
								vChannelIconLandscape.setImageBitmap (bitmap2);
							channel_thumbnail_found = true;
							}
						}			
					}
				}		
			}
		}
	
	public void setup_player_adapters (String channel_id)
		{
		log ("setup player adapters");
		
		HorizontalListView horiz = (HorizontalListView) findViewById (R.id.playback_episodes);
		playback_episode_adapter = setup_horiz (horiz, channel_id);
		
		ListView vPlaybackChannels = (ListView) findViewById (R.id.playback_channel_list);
		if (vPlaybackChannels != null)
			{
			playback_channel_adapter = new PlaybackChannelAdapter (main.this);
			vPlaybackChannels.setAdapter (playback_channel_adapter);
			
			vPlaybackChannels.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView <?> parent, View v, int position, long id)
					{
					Log.i ("vtest", "playback channel click: " + position);
					String channel_id = arena [position + 1];
					play_channel (channel_id);	
					}
				});			
			}
		
		ListView vPlaybackComments = (ListView) findViewById (R.id.playback_comments_list);
		if (vPlaybackComments != null)
			{
			playback_comments_adapter = new PlaybackCommentsAdapter (main.this);
			vPlaybackComments.setAdapter (playback_comments_adapter);
			
			vPlaybackComments.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView parent, View v, int position, long id)
					{
					log ("click: revert");
					toggle_extended_comments_view();
					}
				});		
			}
		/* this listener won't work on the container layout, playbackbody_comments_view */
		if (false)
			vPlaybackComments.setOnTouchListener (new View.OnTouchListener()
				{
		        @Override
		        public boolean onTouch (View v, MotionEvent event)
		        	{
		        	log ("VCOMMENTS ONTOUCH");
		            switch (event.getAction())
		            	{
			            case MotionEvent.ACTION_DOWN:
			            	toggle_extended_comments_view();
			                break;
			            case MotionEvent.ACTION_UP:
			            case MotionEvent.ACTION_CANCEL:
			                break;
			            }
		            return false;
		        	}
				});
		
		View vExpand = findViewById (R.id.expand);
		vExpand.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	log ("click on: expand");
	        	swap_orientation();
	        	}
			});			
		}
	
	public void swap_orientation()
		{
    	int orientation = getRequestedOrientation();
    	// setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setRequestedOrientation
		(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ?
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	
	int fully_expanded_height = 0;
	int fully_expanded_width = 0;
	
	public void expand_video()
		{
		if (!video_is_expanded())
			expand_or_contract();
		}
	
	public boolean video_is_expanded()
		{
		View video = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) video.getLayoutParams();
		return (layout.width == screen_width || layout.width == MATCH_PARENT);
		}
	
	public void expand_or_contract()
		{
		View video = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) video.getLayoutParams();
		
		if (layout.width == screen_width || layout.width == MATCH_PARENT)
			set_size_of_video (true);
		else
			set_size_of_video (false);
		}
	
	public void reset_video_size()
		{
		View video = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) video.getLayoutParams();
		
		if (layout.width == screen_width || layout.width == MATCH_PARENT)
			set_size_of_video (false);
		else
			set_size_of_video (true);
		}
	
	public void set_size_of_video (boolean shrunk)
		{
		setup_playback_buttons();
		
    	int orientation = getRequestedOrientation();
    	boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		View video = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) video.getLayoutParams();
		SpecialFrameLayout yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);
		LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
		View vControls = findViewById (R.id.controls);
		View vTopControls = findViewById (R.id.top_controls);
		
		if (fully_expanded_height == 0)
			{
			fully_expanded_width = videoFragment.video_width;
			fully_expanded_height = videoFragment.video_height;
			}
		
		if (shrunk)
			{
			float aspect = (float) fully_expanded_height / (float) fully_expanded_width;
			int inset = (int) ((float) (landscape ? screen_height : screen_width) / 5);
			log ("aspect: " + aspect + " LR: " + pixels_120 + " TB: " + (int) (inset * aspect));
			layout.width = (landscape ? screen_height : fully_expanded_width) - 2 * inset;
			layout.height = (landscape ? screen_width : fully_expanded_height) - 2 * (int) (inset * aspect);	
			video.setLayoutParams (layout);
			// vControls.setVisibility (landscape ? View.VISIBLE : View.GONE);
			vControls.setVisibility (View.VISIBLE);
			vTopControls.setVisibility (View.VISIBLE);
			wrapper_layout.weight = landscape ? 1.0f : 0f;
			wrapper_layout.height = landscape ? 0 : fully_expanded_height;
			wrapper_layout.width = landscape ? MATCH_PARENT : fully_expanded_width;
			yt_wrapper.setLayoutParams (wrapper_layout);
			yt_wrapper.postInvalidate();
			}
		else
			{
			wrapper_layout.weight = landscape ? 1.0f : 0f;
			wrapper_layout.height = landscape ? 0 : fully_expanded_height;
			wrapper_layout.width = landscape ? MATCH_PARENT : fully_expanded_width;
			yt_wrapper.setLayoutParams (wrapper_layout);
			layout.width = landscape ? MATCH_PARENT : fully_expanded_width;
			layout.height = landscape ? MATCH_PARENT : fully_expanded_height;
			video.setLayoutParams (layout);			
			vControls.setVisibility (landscape ? View.GONE : View.VISIBLE);
			vTopControls.setVisibility (landscape ? View.GONE : View.VISIBLE);
			}	
		
		vControls.postInvalidate();
		
		ImageView vExpand = (ImageView) findViewById (R.id.expand);
		vExpand.setImageResource (landscape ? R.drawable.icon_shrink : R.drawable.icon_expand);
		}
	
	public void redraw_playback (String channel_id, String episode_id)
		{
		/* redraws the bottom ListView */
		}
	
	public View video_layer()
		{
		return findViewById (R.id.video_layer_new);
		}
	
	public void enable_player_layer()
		{		
		video_normal();
		set_layer (toplayer.PLAYBACK);
		
		View video_layer = video_layer();
		video_layer.setVisibility (View.VISIBLE);		
		
		View playback_body = findViewById (R.id.playbackbody);
		playback_body.setVisibility (View.VISIBLE);
		
		hide_menu_immediately();
		setup_playback_buttons();
		}

	public void playback_back()
		{
    	int orientation = getRequestedOrientation();
    	
    	if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    		{    		
    		log ("reset orientation to portrait");
    		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			onVideoActivityLayout();
			}
    	else
    		{
    		pause_video();
    		enable_home_layer();
    		}	
		}
	
	public void setup_playback_buttons()
		{
		View vBack = findViewById (R.id.playback_back);
		if (vBack != null)
			vBack.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: back");
		        	playback_back();
		        	}
				});	
		
		View vPausePlay = findViewById (R.id.pause_or_play);
		if (vPausePlay != null)
			vPausePlay.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: pause/play");
		        	pause_or_play();
		        	reset_osd_display_time();
		        	}
				});	
		
		View vMinify = findViewById (R.id.minify);
		if (vMinify != null)
			vMinify.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: video minimize");
		        	
		        	int orientation = getRequestedOrientation();
		        	boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		        	
		        	if (landscape)
		        		{
		        		swap_orientation();
		        		v.postDelayed (new Runnable()
		        			{
							@Override
							public void run()
								{
								video_minimize();
								}		        		
		        			}, 50);
		        		}
		        	else
		        		video_minimize();
		        	}
				});	
		
		View vFlipUp = findViewById (R.id.flip_up);
		if (vFlipUp != null)
			vFlipUp.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: flip up");
			    	flip_up();
		        	}
				});		

		View vFlipDown = findViewById (R.id.flip_down);
		if (vFlipDown != null)
			vFlipDown.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: flip down");
			    	flip_down();
		        	}
				});	
		
		View vFlipLeft = findViewById (R.id.flip_left);
		if (vFlipLeft != null)
			vFlipLeft.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: flip left");
			    	flip_left();
		        	}
				});	

		View vFlipRight = findViewById (R.id.flip_right);
		if (vFlipRight != null)
			vFlipRight.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: flip left");
			    	flip_right();
		        	}
				});	
		
		int orientation = getRequestedOrientation();
		boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		
		ImageView vPlaybackFollow = (ImageView) findViewById (R.id.playback_follow);
		if (vPlaybackFollow != null)
			{
			vPlaybackFollow.setVisibility (config.usertoken != null ? View.VISIBLE : View.GONE);
			vPlaybackFollow.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: playback follow/unfollow");
		        	follow_or_unfollow (player_real_channel);
		        	}
				});	
			}
		
		ImageView vPlaybackFollowLandscape = (ImageView) findViewById (R.id.playback_follow_landscape);
		if (vPlaybackFollowLandscape != null)
			{
			vPlaybackFollowLandscape.setVisibility (config.usertoken != null && landscape ? View.VISIBLE : View.GONE);
			vPlaybackFollowLandscape.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: playback follow/unfollow");
		        	follow_or_unfollow (player_real_channel);
		        	}
				});	
			}
		View vPlaybackShare = findViewById (R.id.playback_share);
		if (vPlaybackShare != null)
			vPlaybackShare.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: playback share"); 
		        	share_episode (player_real_channel, null);
		        	}
				});	
		
		View vPlaybackShareLandscape = findViewById (R.id.playback_share_landscape);
		if (vPlaybackShareLandscape != null)
			{
			vPlaybackShareLandscape.setVisibility (landscape ? View.VISIBLE : View.GONE);			
			vPlaybackShareLandscape.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: playback share"); 
		        	share_episode (player_real_channel, null);
		        	}
				});	
			}
		
		View vVideoLayer = video_layer();
		if (vVideoLayer != null)
			vVideoLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat these */
		        	}
				});			
		
		/* and some additional adjustments */
		
		View vSpacer = findViewById (R.id.spacer);
		vSpacer.setVisibility (landscape ? View.GONE : View.VISIBLE);
		
		View vLandscapeEpisodeName = findViewById (R.id.landscape_episode_name);
		vLandscapeEpisodeName.setVisibility (landscape ? View.VISIBLE : View.GONE);
		}

	public void flip_left()
		{
		onVideoActivityFlingRight();
		}
	
	public void flip_right()
		{
		onVideoActivityFlingLeft();
		}	
	
	public void flip_up()
		{
		onVideoActivityFlingDown();
		}	
	
	public void flip_down()
		{
		onVideoActivityFlingUp();
		}	

	/* since the video layer is on top, it is never submerged and a video can inadvertently play because of built-in
	   race conditions in the YouTube API. Try to prevent this from happening. */

	@Override
	public boolean able_to_play_video()
		{
		if (current_layer != toplayer.PLAYBACK)
			return false;
		
		View vVideoLayer = video_layer();
		if (vVideoLayer.getVisibility() != View.VISIBLE)
			return false;
		
		return true;
		}
	
	class PlaybackChannelAdapter extends ArrayAdapter <String>
		{
		Activity context;
	
		PlaybackChannelAdapter (Activity context)
			{
			super (context, R.layout.playbackchannel, arena);
			this.context = context;
			}
	
		@Override
		public int getCount()
			{
			return arena.length - 1;
			}
		
		@Override
		public View getView (int position, View convertView, ViewGroup parent)
			{
			// log ("playback channel getView: " + position); // noisy
			
			if (position >= getCount())
				{
				Log.e ("vtest", "race condition! position=" + position + " but arena.length is " + arena.length);
				return null;
				}
			
			View row = convertView;			
			
			if (row == null)
				{
				row = inflater.inflate (R.layout.playbackchannel, null);
				}
	
			final String channel_id = arena [position + 1];
			
			// log ("channel id: " + channel_id); // noisy
			
			boolean active_channel = channel_id.equals (player_real_channel);
			
			String name = config.pool_meta (channel_id, "name");
			
			TextView vTitle = (TextView) row.findViewById (R.id.title);
			vTitle.setText (name != null ? name : "?");
			vTitle.setTextColor (active_channel ? Color.rgb (0xFF, 0xFF, 0xFF) : Color.rgb (0x00, 0x00, 0x00));
			
			// LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTitle.getLayoutParams();
			vTitle.setPadding (pixels_6, (active_channel ? pixels_2 : pixels_6), pixels_6, (active_channel ? 0 : pixels_6));
			
			String subtitle = null;
			
			if (program_line != null && program_line.length > 0)
				{
				String episode_id = program_line [0];
				String timestamp = config.program_meta (episode_id, "timestamp");
				String ago = util.ageof (Long.parseLong (timestamp) / 1000);
				String txt_episode = getResources().getString (R.string.episode_lc);
				String txt_episodes = getResources().getString (R.string.episodes_lc);
				subtitle = ago + " • " + program_line.length + " " + (program_line.length == 1 ? txt_episode : txt_episodes);
				}

			TextView vSubTitle = (TextView) row.findViewById (R.id.subtitle);
			vSubTitle.setText (subtitle != null && active_channel ? subtitle : "?");
			vSubTitle.setVisibility (active_channel && subtitle != null ? View.VISIBLE : View.GONE);
			
			View vEntry = row.findViewById (R.id.entry);
			vEntry.setBackgroundColor (active_channel ? Color.rgb (0xA0, 0xA0, 0xA0) : Color.rgb (0xFF, 0xFF, 0xFF));
					
			return row;
			}
		}
	
	class PlaybackCommentsAdapter extends ArrayAdapter <String>
		{
		Activity context;
		String episode_id = null;
		int number_of_comments = 0;
		
		PlaybackCommentsAdapter (Activity context)
			{
			super (context, R.layout.playbackchannel, arena);
			this.context = context;
			}
	
		@Override
		public boolean isEnabled (int position)
			{
			return true;			
			}
		
		@Override
		public int getCount()
			{
			return get_number_of_comments() + 2;
			}
		
		public void set_episode_id (String episode_id)
			{
			this.episode_id = episode_id;
			}
		
		public int get_number_of_comments()
			{	
			return number_of_comments;
			}
		
		public void set_number_of_comments (int number_of_comments)
			{
			this.number_of_comments = number_of_comments;
			}
		
		@Override
		public int getItemViewType (int position)
			{
			if (position == 0)
				return 1;
			else if (position == 1)
				return 2;
			else
				return 0;
			}
		 
		@Override
		public int getViewTypeCount()
			{
			return 3;
			}
		
		@Override
		public View getView (int position, View convertView, ViewGroup parent)
			{
			// log ("playback channel getView: " + position); // noisy
			
			if (position >= getCount())
				{
				Log.e ("vtest", "race condition! position=" + position + " but length is " + getCount());
				return null;
				}
			
			View row = convertView;			
			
			if (row == null)
				{
				if (position == 0)
					row = inflater.inflate (R.layout.comment_0, null);
				else if (position == 1)
					row = inflater.inflate (R.layout.comment_1, null);
				else
					row = inflater.inflate (R.layout.comment_item, null);
				}
						
			if (position == 0)
				{
				// TextView vTitle = (TextView) row.findViewById (R.id.episode_title);
				// TextView vAgo = (TextView) row.findViewById (R.id.episode_age);
				TextView vDesc = (TextView) row.findViewById (R.id.episode_desc);
				
				// String title = config.program_meta (episode_id, "name");
				// String timestamp = config.program_meta (episode_id, "timestamp");
				String desc = config.program_meta (episode_id, "desc");
				
				// String ago = "";
				// if (timestamp != null && !timestamp.equals (""))
				// 	ago = util.ageof (Long.parseLong (timestamp) / 1000);
				
				// vTitle.setText (title);
				vDesc.setText (desc);
				// vAgo.setText (ago);
				}
			else if (position == 1)
				{
				TextView vHeader = (TextView) row.findViewById (R.id.num_comments_header);
				TextView vNum = (TextView) row.findViewById (R.id.num_comments);
				String num_comments = config.program_meta (episode_id, "maxcomment");
				if (num_comments == null) num_comments = "";
				vNum.setText (num_comments);
				}
			else
				{
				TextView vAuthor = (TextView) row.findViewById (R.id.author);
				TextView vComment = (TextView) row.findViewById (R.id.comment);
				TextView vAgo = (TextView) row.findViewById (R.id.ago);
				vAuthor.setVisibility (View.VISIBLE);
				vAgo.setVisibility (View.VISIBLE);
				Comment c = config.get_comment (episode_id, Integer.toString (position - 2));
				vAuthor.setText (c.author);
				vComment.setText (c.text);
				
				/* example 2012-11-11T05:57:15.000Z -- SimpleDateFormat is not documented as working with "Z" type timezones */
				String date = c.date.replace ("Z", "-0000");
				
				long timestamp;
                SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                try
                	{
					Date dt = sdf.parse (date);
					timestamp = dt.getTime();
                	}
                catch (ParseException e1)
                	{
					timestamp = 0;
                	}
                
        		String ago = util.ageof (timestamp / 1000);
        		vAgo.setText (ago);
				}
			
			return row;
			}
		}

	boolean video_is_minimized = false;
	
	@Override
	public boolean video_is_minimized()
		{
		return video_is_minimized;
		}
	
	@Override
	public boolean video_visible_somewhere()
		{
		return video_is_minimized || (!menu_is_extended() && current_layer == toplayer.PLAYBACK);
		}
	
	public void disable_video_layer()
		{
		if (!video_is_minimized)
			{
			View video_layer = video_layer();
			video_layer.setVisibility (View.GONE);
			}
		}
	
	int minimized_height = 0;
	int minimized_width = 0;
	
	public void video_minimize()
		{
		log ("video minimize");
		
		expand_video();
		
		video_is_minimized = true;
		
		View vMenuLayer = findViewById (R.id.menu_layer);
		View vSlidingPanel = findViewById (R.id.slidingpanel);
		View vVideoLayer = video_layer();
		View vPlaybackBody = findViewById (R.id.playbackbody);
		View vContainer = findViewById (R.id.video_fragment_container);
		View vBacking = findViewById (R.id.backing_controls);
		View vControls = findViewById (R.id.controls);
		View vTitlecard = findViewById (R.id.titlecard);
		View yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);	
		View vTopControls = findViewById (R.id.top_controls);
		
		FrameLayout.LayoutParams video_layout = (FrameLayout.LayoutParams) vVideoLayer.getLayoutParams();			
		LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
		SpecialFrameLayout.LayoutParams container_layout = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();
		
		vPlaybackBody.setVisibility (View.GONE);
		vBacking.setVisibility (View.GONE);
		vTitlecard.setVisibility (View.GONE);
		vControls.setVisibility (View.GONE);
		if (vTopControls != null)
			vTopControls.setVisibility (View.GONE);
				
		if (fully_expanded_height == 0)
			{
			fully_expanded_width = videoFragment.video_width;
			fully_expanded_height = videoFragment.video_height;
			}
		
		float aspect = (float) fully_expanded_height / (float) fully_expanded_width;
		if (aspect < 0.2f)
			aspect = 0.5625f;
		
		// video_layout.width = (int) ((float) screen_width * 0.6);
		// video_layout.height = (int) ((float) video_layout.width * aspect);
		// video_layout.topMargin = (screen_height - video_layout.height) / 2 - pixels_60;
		// video_layout.leftMargin = (screen_width - video_layout.width) / 2 - pixels_60;		
		// vVideoLayer.setLayoutParams (video_layout);
				
		// wrapper_layout.width = LayoutParams.MATCH_PARENT;
		// wrapper_layout.height = LayoutParams.MATCH_PARENT;
		
		// yt_wrapper.setLayoutParams (wrapper_layout);
		
		wrapper_layout.width = screen_width;
		// wrapper_layout.height = screen_height;
		wrapper_layout.height = MATCH_PARENT;
		
		yt_wrapper.setLayoutParams (wrapper_layout);		
		
		minimized_width = (int) ((float) screen_width * 0.6);
		minimized_height = (int) ((float) minimized_width * aspect);
		
		log ("******* w: " + fully_expanded_width + " *************** h: " + fully_expanded_height + " ************ aspect: " + aspect);
		log ("******* minimized_width: " + minimized_width + ", ********** minimized_height: " + minimized_height);
		container_layout.width = minimized_width; 
		container_layout.height = minimized_height;
		container_layout.topMargin = 0;
		container_layout.bottomMargin = pixels_60;
		container_layout.leftMargin = 0;
		container_layout.rightMargin = pixels_60;
		container_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;		
		// container_layout.topMargin = (screen_height - new_height) / 2 - pixels_60;
		/// container_layout.bottomMargin = pixels_60;
		/// container_layout.leftMargin = (screen_width - new_width) / 2 - pixels_60;	
		
		vContainer.setLayoutParams (container_layout);
		
		
		video_layout.width = screen_width;
		video_layout.height = minimized_height + 2 * pixels_60;
		video_layout.topMargin = 0;
		video_layout.bottomMargin = 0;
		video_layout.leftMargin = 0;
		video_layout.rightMargin = 0;
		video_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		vVideoLayer.setLayoutParams (video_layout);
		
		
		vContainer.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	log ("CONSUMED CLICK");
	        	/* eat this */
	        	}
			});	
		vContainer.setOnTouchListener (new OnTouchListener()
		{

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
				{
				log ("CONSUMED TOUCH");
				return true;
				}
		
		});
		
		vVideoLayer.setOnClickListener (null); 
		vVideoLayer.setClickable (false);
		vPlaybackBody.setClickable (false);
		yt_wrapper.setClickable (false);
		
		vVideoLayer.setOnTouchListener (new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
				{
				log ("***************** VIDEO LAYER onTouch");
				return false;
				}		
		});
		yt_wrapper.setOnTouchListener (new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
				{
				log ("***************** YT WRAPPER LAYER onTouch");
				return false;
				}		
		});
		
		yt_wrapper.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
		// vVideoLayer.setBackgroundColor (Color.argb (0x40, 0xFF, 0x00, 0x00));
		vVideoLayer.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
		
		unlaunch_player();
		vVideoLayer.postInvalidate();
		}
	
	public void video_normal()
		{
		log ("video normal");
		video_is_minimized = false;
		
		View vMenuLayer = findViewById (R.id.menu_layer);
		View vSlidingPanel = findViewById (R.id.slidingpanel);
		View vVideoLayer = video_layer();
		View vPlaybackBody = findViewById (R.id.playbackbody);
		View vContainer = findViewById (R.id.video_fragment_container);		
		View vBacking = findViewById (R.id.backing_controls);
		View vControls = findViewById (R.id.controls);		
		View vTitlecard = findViewById (R.id.titlecard);
		View yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);	
		View vTopControls = findViewById (R.id.top_controls);
		
		FrameLayout.LayoutParams video_layout = (FrameLayout.LayoutParams) vVideoLayer.getLayoutParams();			
		LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
		SpecialFrameLayout.LayoutParams container_layout = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();
		
		vPlaybackBody.setVisibility (View.VISIBLE);
		vBacking.setVisibility (View.VISIBLE);
		vTitlecard.setVisibility (View.VISIBLE);
		vControls.setVisibility (View.VISIBLE);
		if (vTopControls != null)
			vTopControls.setVisibility (View.VISIBLE);
		
		video_layout.width = screen_width;
		video_layout.height = MATCH_PARENT;
		video_layout.topMargin = 0;
		video_layout.bottomMargin = 0;
		video_layout.leftMargin = 0;
		video_layout.rightMargin = 0;
		video_layout.gravity = Gravity.CENTER;
		vVideoLayer.setLayoutParams (video_layout);
		
		// vVideoLayer.setOnClickListener (null);
		
		yt_wrapper.setBackgroundColor (Color.argb (0xFF, 0x00, 0x00, 0x00));
		// vVideoLayer.setBackgroundColor (Color.argb (0xFF, 0xFF, 0xFF, 0xFF));
		vVideoLayer.setBackgroundColor (Color.argb (0x40, 0x00, 0x00, 0xFF));
		
		wrapper_layout.width = screen_width;
		wrapper_layout.height = WRAP_CONTENT;
		wrapper_layout.topMargin = 0;
		wrapper_layout.leftMargin = 0;
		wrapper_layout.bottomMargin = 0;
		wrapper_layout.rightMargin = 0;
		
		yt_wrapper.setLayoutParams (wrapper_layout);
		
		container_layout.width = screen_width;
		container_layout.height = WRAP_CONTENT;
		container_layout.topMargin = 0;
		container_layout.bottomMargin = 0;
		container_layout.leftMargin = 0;
		container_layout.rightMargin = 0;
		container_layout.gravity = Gravity.CENTER;
		
		vContainer.setLayoutParams (container_layout);
		
    	expand_video();
		vVideoLayer.postInvalidate();
		
		View vProgress = findViewById (R.id.progresscontainer);
		vProgress.postInvalidate();
		}
	
	@Override
	public boolean onVideoActionTapped()
		{
		log ("onVideoActionTapped");
		if (video_is_minimized)
			{
			if (max_pixels_minimized_box_moved < 100)
				relaunch_player();
			}
		else
			expand_or_contract();
		return true;
		}
	

	int max_pixels_minimized_box_moved = 0;
	
	@Override
	public boolean onVideoActionDown()
		{
		max_pixels_minimized_box_moved = 0;
		return false;
		}
	
	@Override
	public boolean onVideoActionUp (int deltaX, int deltaY)
		{
		log ("onVideoActionUp :: deltaX: " + deltaX + " deltaY: " + deltaY);
		if (video_is_minimized)
			{
			if (deltaX > pixels_100)
				{
		    	if (current_layer != toplayer.PLAYBACK)
		    		{
		    		log ("video disappear");
					pause_video();
			    	video_normal();
		    		disable_video_layer();
		    		}
		    	else
		    		{
		    		log ("video maximize only");
		    		video_normal();
		    		}
				}			
			}			
		else
			{
			View vWrapper = findViewById (R.id.ytwrapper2);
			if (Math.abs (deltaX) >= vWrapper.getWidth() * 0.2)
				{
				if (deltaX > 0.0)
					{
					log ("fling left");
					onVideoActivityFlingLeft();
					}
				else
					{
					log ("fling right");
					onVideoActivityFlingRight();
					}	
				}
			else if (Math.abs (deltaY) >= vWrapper.getHeight() * 0.2)
				{
				if (deltaY > 0.0)
					{
					log ("fling up");
					onVideoActivityFlingUp();
					}
				else
					{
					log ("fling down");
					onVideoActivityFlingDown();
					}  				
				}	    			
			}
		return false;
		}
	
	@Override
	public boolean onVideoHorizontal (int deltaX)
		{
		View vContainer = findViewById (R.id.video_fragment_container);	
		int max_drag = screen_width - pixels_60 - minimized_width;
		
		if (deltaX >= 0 && deltaX < max_drag)
			{
			SpecialFrameLayout.LayoutParams container_layout = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();			
			container_layout.rightMargin = pixels_60 + deltaX;			
			vContainer.setLayoutParams (container_layout);
			
			float percent = (float) deltaX / (float) max_drag;
			float new_alpha = 1.0f - (percent / 1.0f);
			// vContainer.setAlpha (new_alpha);
			log ("HORIZ MOVE: " + percent + " NEW ALPHA: " + new_alpha);
			if (deltaX > max_pixels_minimized_box_moved)
				max_pixels_minimized_box_moved = deltaX;
			}
		else
			vContainer.setAlpha (1.0f);
		
		return false;
		}
	
	/*** GUIDE **************************************************************************************************/
	
	public void enable_guide_layer()
		{
		disable_video_layer();		
		set_layer (toplayer.GUIDE);		
		setup_guide_buttons();
		init_3x3_grid();
		}
	
	public void setup_guide_buttons()
		{
		View vGuideTopBar = findViewById (R.id.guide_top_bar);
		
		View vMenu = vGuideTopBar.findViewById (R.id.menubutton);
		if (vMenu != null)
			vMenu.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide menu button");
		        	toggle_menu();
		        	}
				});	
		
		View vSearch = vGuideTopBar.findViewById (R.id.searchbutton);
		if (vSearch != null)
			vSearch.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide search button");
		        	enable_search_apparatus (R.id.guide_top_bar);
		        	}
				});		
		
		View vGuideLayer = findViewById (R.id.guidelayer);
		if (vGuideLayer != null)
			vGuideLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});
		
		View vRefresh = vGuideTopBar.findViewById (R.id.refresh);
		if (vRefresh != null)
			vRefresh.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide refresh button");
		        	refresh_guide();
		        	}
				});	
		
		if (is_phone())
			{
			TextView vGuideTitle = (TextView) findViewById (R.id.guide_title);
			vGuideTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 26);
			
			TextView vGuideMeta = (TextView) findViewById (R.id.guide_meta);
			vGuideMeta.setTextSize (TypedValue.COMPLEX_UNIT_SP, 18);
			}
		
		View vGuideAsGuest = findViewById (R.id.guide_as_guest);
		if (vGuideAsGuest != null)
			vGuideAsGuest.setVisibility (config.usertoken != null ? View.GONE : View.VISIBLE);

		View vGuideAsGuestDialog = findViewById (R.id.guide_as_guest_dialog);
		if (vGuideAsGuestDialog != null)
			{
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vGuideAsGuestDialog.getLayoutParams();
			layout.width = (int) (0.8f * (float) screen_width);
			vGuideAsGuestDialog.setLayoutParams (layout);
			}
		
		View vGagHome = findViewById (R.id.guide_as_guest_home);
		if (vGagHome != null)
			vGagHome.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide as guest -- home");
		        	toggle_menu (new Callback()
			        	{
			        	public void run()
			        		{
				        	enable_home_layer();
				        	toggle_menu();
			        		}
			        	});
		        	}
				});		
		
		View vGagSignIn = findViewById (R.id.guide_as_guest_signin);
		if (vGagSignIn != null)
			vGagSignIn.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide as guest -- sign in");
		        	toggle_menu (new Callback()
		        		{
		        		public void run()
		        			{
			        		enable_signin_layer();
			        		toggle_menu();
		        			}
		        		});		        	
		        	}
				});			
		}
	
	public void update_guide_metadata()
		{
		// TODO
		}
		
	public void refresh_guide()
		{
		if (config.usertoken != null)
			{
			config.forget_subscriptions();
			query_following (refresh_guide_inner);
			}
		else
			toast_by_resource (R.string.please_login_first);
		}
	
	final Callback refresh_guide_inner = new Callback()
		{
		@Override
		public void run_string (String arg1)
			{
			init_3x3_grid();
			}
		};
	
	int current_set = 1;
	int grid_cursor = 0;
	
	GridSlider grid_slider = null;	
	StoppableViewPager vPager = null;
	
	int top_lefts[] = { 11, 14, 17, 41, 44, 47, 71, 74, 77 };
	
	/* we don't want to redraw everything, but we will need to remove the background from this square */
	FrameLayout previous_cursor_view = null;
	
	public void init_3x3_grid()
		{
		log ("init 3x3 grid");

        grid_slider = new GridSlider();

        vPager = (StoppableViewPager) findViewById (R.id.top3x3pager);
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
		public boolean isViewFromObject (View view, Object object)
			{
			return (((Swapgrid) object).box) == (FrameLayout) view;
			}
		
		@Override
		public Object instantiateItem (ViewGroup container, int position)
			{
			log ("[PAGER] instantiate: " + position);
			
			final Swapgrid sg = new Swapgrid (position);			

			FrameLayout box = (FrameLayout) View.inflate (main.this, R.layout.grid, null);
			
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
			  layout.width = screen_width;
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
						View altpager = findViewById (R.id.top3x3pager);
						LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) altpager.getLayoutParams();
						layout.width = screen_width;
						layout.height = Math.max (height, 1);
						altpager.setLayoutParams (layout);
						altpager.invalidate();
					  	}
					});
			  	}
			  
		    select (position);
			download_grid_thumbs_for_this_3x3 (sg.box);
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
			if (is_phone())
				{
				FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vPlayBall.getLayoutParams();
				layout.bottomMargin = 0;
				vPlayBall.setLayoutParams (layout);
				}
			}
		TextView vTitle = (TextView) v.findViewById (R.id.title);
		if (vTitle != null)
			vTitle.setTextColor (Color.rgb (0xFF, 0xFF, 0xFF));
		
		if (dongle_mode && !v.isFocused())
			v.requestFocus();
		
		TextView vLargeTitle = (TextView) findViewById (R.id.guide_title);
		TextView vLargeMeta = (TextView) findViewById (R.id.guide_meta);
		ImageView vFollow = (ImageView) findViewById (R.id.guide_follow);
		
		vLargeTitle.setVisibility (channel_id == null ? View.INVISIBLE : View.VISIBLE);
		vLargeMeta.setVisibility (channel_id == null ? View.INVISIBLE : View.VISIBLE);
		vFollow.setVisibility (channel_id == null ? View.INVISIBLE : View.VISIBLE);

		if (channel_id != null)
			{
			vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow_black);
			vFollow.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide follow/unfollow");
		        	follow_or_unfollow (channel_id);
		        	}
		    	});
			}
		
		if (channel_id != null)
			{
			String name = config.pool_meta (channel_id, "name"); 
			vLargeTitle.setText (name);
			int count = config.programs_in_real_channel (channel_id);
			if (count > 0)
				highlight_3x3_square_inner_inner (channel_id);
			else
				{
				String txt_wait = getResources().getString (R.string.wait);
				vLargeMeta.setText (txt_wait);
				load_channel_then (channel_id, highlight_3x3_square_inner, channel_id, null);
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
    	// android:text="1 hour ago · 15 episodes"
		int count = config.programs_in_real_channel (channel_id);
		TextView vLargeMeta = (TextView) findViewById (R.id.guide_meta);
		String txt_episode = getResources().getString (R.string.episode_lc);		
		String txt_episodes = getResources().getString (R.string.episodes_lc);
		vLargeMeta.setText ("" + count + " " + (count == 1 ? txt_episode : txt_episodes));
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
				int cid = getResources().getIdentifier ("c" + channel_position, "id", getPackageName());
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
				vTitle.setVisibility (is_phone() ? View.GONE : View.VISIBLE);
				}
			
			if (vChannelicon != null)
				{
				String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
				File f = new File (filename);
				if (f.exists ())
					{
					/*
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						Bitmap bitmap2 = bitmappery.getRoundedCornerBitmap (bitmap, 50);
						if (bitmap2 != null)
							{
							vChannelicon.setImageBitmap (bitmap2);
							channel_thumbnail_found = true;
							}
						}
					*/
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						vChannelicon.setImageBitmap (bitmap);
						channel_thumbnail_found = true;
						}					
					}
				}
			
			String filename = getFilesDir() + "/" + config.api_server + "/xthumbs/" + channel_id + ".png";
			
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
		int width = (int) ((float) screen_width / 3.2);	
	    
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
		
		if (is_phone())
			{
			if (vChannelicon != null)
				{
				FrameLayout.LayoutParams layout3 = (FrameLayout.LayoutParams) vChannelicon.getLayoutParams();
				layout3.height = pixels_15;
				layout3.width = pixels_15;
				layout3.topMargin = pixels_2;
				layout3.leftMargin = pixels_2;
				vChannelicon.setLayoutParams (layout3);
				}
			
			ImageView vPlayBall = (ImageView) v.findViewById (R.id.playball);
			if (1 == 2 && vPlayBall != null)
				{
				FrameLayout.LayoutParams layout3 = (FrameLayout.LayoutParams) vPlayBall.getLayoutParams();
				layout3.height = pixels_20;
				layout3.width = pixels_20;
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
				toast_by_resource (R.string.no_channel);
			else
				launch_player (channel_id, config.subscribed_channels());
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
	    		String filename = getFilesDir() + "/" + config.api_server + "/xthumbs/" + channel_id + ".png";
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
			new playerAPI (in_main_thread, config, "latestEpisode?channel=" + channel_id)
				{
				public void success (String[] lines)
					{
					for (int i = 0; i < lines.length; i++)
						{
						String fields[] = lines [i].split ("\t");
						log ("---DOWNLOAD LINE--- " + lines[i]);
						thumbnail.sample_thumb (main.this, config, channel_id, fields[2], in_main_thread, episode_thumbs_updated);
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
					int id = getResources().getIdentifier ("c" + c, "id", getPackageName());
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
			int id = getResources().getIdentifier ("c" + c, "id", getPackageName());
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
			int id = getResources().getIdentifier ("dot" + i, "id", getPackageName());
			if (id > 0)
				{
	    		ImageView vDot = (ImageView) findViewById (id);
	    		if (vDot != null)
	    			{
	    			int normal = dongle_mode ? R.drawable.white_dot : R.drawable.white_dot_tablet;
	    			int highlit = dongle_mode ? R.drawable.white_dot_highlight : R.drawable.white_dot_tablet_highlight;
	    			vDot.setImageResource (i == current_set ? highlit : normal);
	    			}
				}
			}	
		}
    
    toplayer previous_layer = toplayer.HOME;
    String previous_arena[] = null;
    String unlaunched_player_arena[] = null;

    public void launch_player (String channel_id, String channels[])
    	{
    	previous_layer = current_layer;
    	previous_arena = arena;
    	
    	if (channels.length > 0 && channels [0] != null)
    		{
    		String new_channels[] = new String [channels.length + 1];
    		for (int i = 0; i < channels.length; i++)
    			new_channels [i+1] = channels [i];
    		new_channels [0] = null;
    		channels = new_channels;
    		}
    	arena = channels;    	
    	play_channel (channel_id);
    	}
    
    public void unlaunch_player()
    	{
    	unlaunched_player_arena = arena;
    	arena = previous_arena;
    	make_layer_visible (previous_layer);
    	}
    
    public void relaunch_player()
    	{
    	log ("relaunch player");
    	arena = unlaunched_player_arena;
    	make_layer_visible (toplayer.PLAYBACK);
    	video_normal();
    	}
    
	/*** STORE **************************************************************************************************/
    
    boolean store_initialized = false;
    
	int category_stride = 27;
	int category_total_channels = 0;
	
	String category_channels[] = null;
	
	/* current category shown */
	int current_category_index = -1;
	
	/* the final category channel (a spinner) has been exposed, and the next step is being obtained */
	boolean outgoing_category_query = false;
	
	StoreAdapter store_adapter = null;
	
	public void enable_store_layer()
		{
		disable_video_layer();
		set_layer (toplayer.STORE);
		setup_store_buttons();
		store_init();
		top_categories();
		
		if (is_phone())
			{
			View vCategoryLayer = findViewById (R.id.category_layer);
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vCategoryLayer.getLayoutParams();
			layout.topMargin = -pixels_80;
			layout.bottomMargin = pixels_40;
			layout.leftMargin = pixels_40;
			layout.rightMargin = pixels_40;
			vCategoryLayer.setLayoutParams (layout);
			}	
		}	
	
	public void store_init()
		{
		if (!store_initialized)
			{
			store_initialized = true;
			AbsListView vStore = (AbsListView) findViewById (is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
			store_adapter = new StoreAdapter (this, category_channels);
			vStore.setAdapter (store_adapter);
			}
		}
	
	public void setup_store_buttons()
		{
		View vStoreTopBar = findViewById (R.id.store_top_bar);
		
		View vMenu = vStoreTopBar.findViewById (R.id.menubutton);
		if (vMenu != null)
			vMenu.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: store menu button");
		        	toggle_menu();
		        	}
				});	
		
		View vSearch = vStoreTopBar.findViewById (R.id.searchbutton);
		if (vSearch != null)
			vSearch.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: store search button");
		        	enable_search_apparatus (R.id.store_top_bar);
		        	}
				});			
				
		View vRefresh = vStoreTopBar.findViewById (R.id.refresh);
		if (vRefresh != null)
			vRefresh.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: store refresh button");
		        	store_refresh();
		        	}
				});	
		
		View vCategoryName = findViewById (R.id.category_handle);
		if (vCategoryName != null)
			vCategoryName.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	toggle_category_layer();
		        	}
				});	
		
		View vStore = findViewById (R.id.storelayer);		
		if (vStore != null)
			vStore.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});				
		}
	
	public void store_refresh()
		{
    	config.init_query_cache();
		top_categories();
		}
	
	public void toggle_category_layer()
		{
		View vCategoryLayer = findViewById (R.id.category_layer);
		boolean is_visible = vCategoryLayer.getVisibility() == View.VISIBLE;
		vCategoryLayer.setVisibility (is_visible ? View.GONE : View.VISIBLE);
		}
	
	public void top_categories()
		{
		final String query = "category?v=32&lang=" + config.region;
				
		if (config.query_cache.get (query) != null)
			{
			log ("top_categories: using cached data");
			top_categories_inner (config.query_cache.get (query));
			return;
			}
		
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (in_main_thread, config, query)
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
					finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				alert ("ERROR! " + errtext);
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
			alert (txt_no_store);
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
		
		vCategories = (ListView) findViewById (R.id.category_list);		
		if (vCategories != null)
			{
			category_adapter = new LineItemAdapter (this, category_list, toplayer.STORE);
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
		
		/* load the first category */
		load_category (0, 0);
		}

	public void load_category (final int index, final int starting)
		{
		/* http://player.9x9.tv/playerAPI/categoryInfo?category=6&lang=en&start=30&count=3 */	
		
		String category_id = category_list [index];
		
		final String query = "categoryInfo?category=" + category_id
				+ "&v=32&region=" + config.region + "&count=" + category_stride + "&start=" + starting;
		
		if (config.query_cache.get (query) != null)
			{
			log ("load category " + category_id + ": using cached data");
			load_category_inner (index, starting, config.query_cache.get (query));
			return;
			}
				
		if (starting == 0)
			set_spinner (View.VISIBLE);
		
		final long vc_start = System.currentTimeMillis();
		
		new playerAPI (in_main_thread, config, query)
			{
			public void success (String[] lines)
				{
				try
					{
					set_spinner (View.GONE);
					long vc_end = System.currentTimeMillis();
					long elapsed = (vc_end - vc_start) / 1000L;
					log ("[" + query + "] lines received: " + lines.length + ", elapsed: " + elapsed);
					config.query_cache.put (query, lines);
					load_category_inner (index, starting, lines);
					}
				catch (Exception ex)
					{
					ex.printStackTrace();
					finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				alert ("ERROR! " + errtext);
				}
			};		
		}
	
	public void set_spinner (int visibility)
		{
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
				if (fields[0].equals ("count"))					
					/* category_stride = Integer.parseInt (fields[1]) */ ;
				else if (fields[0].equals ("total"))
					category_total_channels = Integer.parseInt (fields[1]);
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

		store_adapter.set_content (category_channels);	
		redraw_store_list();
		
		thumbnail.stack_thumbs (main.this, config, category_channels, in_main_thread, store_channel_thumb_updated);

		if (category_adapter != null)
			category_adapter.notifyDataSetChanged();
		
		String name = category_names.get (category_id);
		
		TextView vCategoryName = (TextView) findViewById (R.id.category_name);
		vCategoryName.setText ("Category: " + name);
		}

	final Runnable store_channel_thumb_updated = new Runnable()
		{
		public void run()
			{
			if (store_adapter != null)
				store_adapter.notifyDataSetChanged();
			}
		};
	
	public void redraw_store_list()
		{	
		}
	
	public class StoreAdapter extends BaseAdapter
		{
		private Context context;
		private String channels[] = null;
		
		public StoreAdapter (Context c, String channels[])
			{
			context = c;
			this.channels = channels;
			}
	
		public void set_content (String channels[])
			{
			this.channels = channels;
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
			LinearLayout rv = null;
					
			log ("store getView: " + position);
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (main.this, is_tablet() ? R.layout.store_item_tablet : R.layout.store_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			ImageView vChannelicon = (ImageView) rv.findViewById (R.id.chicon); 
			ImageView vEpisodeicon = (ImageView) rv.findViewById (R.id.epicon); 
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			TextView vMeta = (TextView) rv.findViewById (R.id.meta);
			
			if (vTitle == null)
				{
				log ("getView: [position" + position + "] vTitle is null!");
				}
					
			if (position < channels.length && channels [position] != null)
				{
				final String channel_id = channels [position];
				
				if (channel_id.equals ("+"))
					{
					/* store only, not for search */
					rv = (LinearLayout) View.inflate (main.this, R.layout.store_item_more, null);		
					if (!outgoing_category_query)
						{
						outgoing_category_query = true;
						load_category (current_category_index, position);
						}
					}
				else
					{
					String name = config.pool_meta (channel_id, "name");
					
					String timestamp = config.pool_meta (channel_id, "timestamp");
					String ago = timestamp == null ? "" : util.ageof (Long.parseLong (timestamp) / 1000);
					
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
						
						String txt_episode = getResources().getString (R.string.episode_lc);		
						String txt_episodes = getResources().getString (R.string.episodes_lc);
						
						vMeta.setText (ago + " • " + icount + " " + (icount == 1 ? txt_episode : txt_episodes));
						}
					
					if (vEpisodeicon != null)
						{
						vEpisodeicon.setImageResource (R.drawable.store_unavailable);
						
						boolean channel_thumbnail_found = false;
						
						if (vChannelicon != null)
							{
							String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
							
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
						
						boolean episode_thumbnail_found = false;
						String filename = getFilesDir() + "/" + config.api_server + "/xthumbs/" + channel_id + ".png";
						
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
							bmOptions.inSampleSize = 2;
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
						
						ImageView vFollow = (ImageView) rv.findViewById (R.id.follow);
						
						if (vFollow != null)
							{
							int follow_icon = is_tablet() ? R.drawable.icon_follow : R.drawable.icon_follow_black;
							int unfollow_icon = is_tablet() ? R.drawable.icon_unfollow : R.drawable.icon_unfollow_press;
							vFollow.setImageResource (config.is_subscribed (channel_id) ? unfollow_icon : follow_icon);
							}
						
						if (vFollow != null)
							vFollow.setOnClickListener (new OnClickListener()
								{
						        @Override
						        public void onClick (View v)
						        	{
						        	log ("click on: store follow " + channel_id);
						        	follow_or_unfollow (channel_id);
						        	}
								});
						
						View vShare = rv.findViewById (R.id.share);
						if (vShare != null)
							vShare.setOnClickListener (new OnClickListener()
								{
						        @Override
						        public void onClick (View v)
						        	{
						        	log ("click on: store share " + channel_id);
						        	share_episode (channel_id, null);
						        	}
								});	
						
						if (vEpisodeicon != null)
							vEpisodeicon.setOnClickListener (new OnClickListener()
								{
						        @Override
						        public void onClick (View v)
						        	{
						        	log ("click on: store play " + channel_id);
						        	launch_player (channel_id, channels);
						        	}
								});							
						}
					}
				}
			else
				{
				vChannelicon.setImageResource (R.drawable.unavailable);		
				vEpisodeicon.setImageResource (R.drawable.store_unavailable);
				vTitle.setText ("");
				}
			
			FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vEpisodeicon.getLayoutParams();
			layout.height = (int) ((float) (screen_width - pixels_40) / 1.77 * 0.55);
			vEpisodeicon.setLayoutParams (layout);
			
			return rv;
			}	
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
				row = inflater.inflate (R.layout.category_item, null);
				}
			
			String name = null;			

			String id = list [position];
			
			if (layer == toplayer.STORE)
				name = category_names.get (id);
			else if (layer == toplayer.HOME)
				name = config.pool_meta (id, "name");
			
			TextView vTitle = (TextView) row.findViewById (R.id.title);
			vTitle.setText (name != null ? name : "?");

			if (is_phone())		
				{
				vTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 24);
				// LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTitle.getLayoutParams();
				// layout.height = pixels_70;
				// vTitle.setLayoutParams (layout);
				}
			
			return row;
			}
		}
	
	/*** SEARCH **************************************************************************************************/
	
	boolean search_initialized = false;
	
	StoreAdapter search_adapter = null;
	
	String search_channels[] = null;
	
	public void enable_search_apparatus (final int view_id)
		{
		View vContainer = findViewById (view_id);
		if (vContainer != null)
			{
			View vSearchContainer = vContainer.findViewById(R.id.search_container);
			if (vSearchContainer.getVisibility() == View.VISIBLE)
				{
				disable_search_apparatus (view_id);
				return;
				}
			
			View vLogo = vContainer.findViewById (R.id.logo);
			View vPhantomRefresh = vContainer.findViewById (R.id.phantom_refresh);
			
			vLogo.setVisibility (View.GONE);
			vPhantomRefresh.setVisibility (View.GONE);
			vSearchContainer.setVisibility (View.VISIBLE);
			
			View vCancel = vContainer.findViewById (R.id.search_cancel);
			vCancel.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: search cancel");
		        	disable_search_apparatus (view_id);
		        	}
				});
			
			final EditText vTerm = (EditText) vContainer.findViewById (R.id.term);
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
							InputMethodManager imm = (InputMethodManager) getSystemService (INPUT_METHOD_SERVICE);
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
	
	public void disable_search_apparatus (int view_id)
		{
		View vContainer = findViewById (view_id);
		if (vContainer != null)
			{
			View vSearchContainer = vContainer.findViewById (R.id.search_container);
			vSearchContainer.setVisibility (View.GONE);
			View vLogo = vContainer.findViewById (R.id.logo);
			View vPhantomRefresh = vContainer.findViewById (R.id.phantom_refresh);
			vLogo.setVisibility (View.VISIBLE);
			vPhantomRefresh.setVisibility (View.INVISIBLE);
			}
		}
	
	public void enable_search_layer()
		{
		disable_video_layer();
		set_layer (toplayer.SEARCH);
		search_init();
		setup_search_buttons();
		}
	
	public void search_init()
		{
		if (!search_initialized)
			{
			search_initialized = true;
			search_adapter = new StoreAdapter (this, search_channels);
			AbsListView vSearch = (AbsListView) findViewById (is_tablet() ? R.id.search_list_tablet : R.id.search_list_phone);
			vSearch.setAdapter (search_adapter);
			}
		}
	
	public void setup_search_buttons()
		{
		View vSearchTopBar = findViewById (R.id.search_top_bar);
		
		View vMenu = vSearchTopBar.findViewById (R.id.menubutton);
		if (vMenu != null)
			vMenu.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: search menu button");
		        	toggle_menu();
		        	}
				});	
		
		View vSearch = vSearchTopBar.findViewById (R.id.searchbutton);
		if (vSearch != null)
			vSearch.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: search layer search button");
		        	enable_search_apparatus (R.id.search_top_bar);
		        	}
				});			

		View vSearchLayer = findViewById (R.id.searchlayer);		
		if (vSearchLayer != null)
			vSearchLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});				
		}
	
	public void perform_search (final String term)
		{
		String encoded_term = util.encodeURIComponent (term);
		
		if (encoded_term == null)
			return;

		set_spinner (View.VISIBLE);
		prepare_search_screen (term);
		
		new playerAPI (in_main_thread, config, "search?text=" + encoded_term + "&v=32")
			{
			public void success (String[] chlines)
				{
				int count = 0;
	
				log ("search lines received: " + chlines.length);
	
				set_spinner (View.GONE);
				
				int section = 0;
				int num_channels = 0;
				
				for (String s: chlines)
					{
					if (s.equals ("--"))
						{
						section++;
						continue;
						}
					if (section == 3)
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
						Log.i ("vtest", "search CHLINE: " + s);
						String[] fields = s.split ("\t");					
						channel_ids [count] = fields[1];
						count++;
						}
					}

				search_channels = channel_ids;
				search_adapter.set_content (search_channels);
				search_adapter.notifyDataSetChanged();
				
				redraw_search_list();
				
				thumbnail.stack_thumbs (main.this, config, search_channels, in_main_thread, search_channel_thumb_updated);
				}
	
			public void failure (int code, String errtext)
				{
				Log.i ("vtest", "ERROR! " + errtext);
				}
			};
		}
	
	public void redraw_search_list()
		{
		AbsListView vSearch = (AbsListView) findViewById (is_tablet() ? R.id.search_list_tablet : R.id.search_list_phone);			
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
	
	public void prepare_search_screen (String term)
		{
		enable_search_layer();
		enable_search_apparatus (R.id.store_top_bar);
		String txt_searched_for = getResources().getString (R.string.searched_for_colon);
		TextView vTermUsed = (TextView) findViewById (R.id.search_term_used);
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
		
	/*** SETTINGS **************************************************************************************************/
		
	public void enable_settings_layer()
		{
		disable_video_layer();
		set_layer (toplayer.SETTINGS);
		redraw_settings();
		setup_settings_buttons();
		}	
	
	public void redraw_settings()
		{
		TextView vNameReadonly = (TextView) findViewById (R.id.settings_name_readonly);
		vNameReadonly.setText (config.username);

		TextView vEmailReadonly = (TextView) findViewById (R.id.settings_email_readonly);
		vEmailReadonly.setText (config.email);
		
		TextView vSettingsEmail = (TextView) findViewById (R.id.settings_email);
		vSettingsEmail.setText (config.email);
		}
	
	public void setup_settings_buttons()
		{		
		View vTopBar = findViewById (R.id.settings_top_bar_resizable);
		LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTopBar.getLayoutParams();
		layout.height = is_phone() ? pixels_100 : pixels_160;
		vTopBar.setLayoutParams (layout);
		
		View vCancel = findViewById (R.id.settings_cancel);
		if (vCancel != null)
			vCancel.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings cancel button");
		        	toggle_menu();
		        	}
				});	

		View vSave = findViewById (R.id.settings_save);
		if (vSave != null)
			vSave.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings save button");
		        	save_settings();
		        	}
				});
		
		View vSettingsLayer = findViewById (R.id.settingslayer);
		if (vSettingsLayer != null)
			vSettingsLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	}
				});		
		}
	
	public void save_settings()
		{
		String kk = null;
		String vv = null;

		TextView vEmail = (TextView) findViewById (R.id.settings_email);
		String email = vEmail.getText().toString();
		
		TextView vOldPassword = (TextView) findViewById (R.id.settings_old_password);
		String old_password = vOldPassword.getText().toString();
		
		TextView vNewPassword = (TextView) findViewById (R.id.settings_new_password);
		String new_password = vNewPassword.getText().toString();
	
		TextView vConfirm = (TextView) findViewById (R.id.settings_verify_password);
		String confirm = vConfirm.getText().toString();
		
		if (!old_password.equals ("") && !new_password.equals (""))
			{
			if (!new_password.equals (confirm))
				{
				toast_by_resource (R.string.tlogin_pw_no_match);
				return;
				}		
	
			if (new_password.length() < 6)
				{
				toast_by_resource (R.string.tlogin_pw_six);
				return;
				}
			
			kk = "oldPassword,password";
			vv = util.encodeURIComponent (old_password) + "," + util.encodeURIComponent (new_password);
			}
		
		if (config != null && config.usertoken != null && kk != null && !kk.equals (""))
			new playerAPI (in_main_thread, config, "setUserProfile?user=" + config.usertoken + "&key=" + kk + "&value=" + vv)
				{
				public void success (String[] lines)
					{
					toast_by_resource (R.string.saved);
					finish();
					}
				public void failure (int code, String errtext)
					{
					if (code == 201 || errtext.startsWith ("201"))
						toast_by_resource (R.string.current_password_wrong);
					else
						alert ("Failure saving your changes: " + errtext);
					}
				};
		}
	
	@Override
	public void onActionDown()
		{
		if (current_layer == toplayer.HOME)
			diminish_side_titles (current_home_page, true);
		}
	
	@Override
	public void onActionUp()
		{
		if (current_layer == toplayer.HOME)
			diminish_side_titles (current_home_page, false);
		}
	
	public void diminish_side_titles (View parent, boolean hide)
		{
		if (parent != null)
			{
			View vLeftSetTitle = parent.findViewById (R.id.left_set_title);
	        vLeftSetTitle.setVisibility (hide ? View.INVISIBLE : View.VISIBLE);
	        
	        View vRightSetTitle = parent.findViewById (R.id.right_set_title);
	        vRightSetTitle.setVisibility (hide ? View.INVISIBLE : View.VISIBLE);
			}
		}
	
	@Override
	public boolean inside_big_draggable_thing (float x, float y)
		{
		View vContainer = findViewById (R.id.slidingpanel);		
		if (menu_is_extended())
			return point_inside_view (x, y, vContainer);
		else if (current_layer == toplayer.HOME)
			{
			/* can drag between sets */
			// return point_inside_view (x, y, vContainer);
			return false;
			}
		else
			return false;
		}
	
	int big_thing_left_margin = 0;
	
	@Override
	public void onBigThingDown()
		{
		log ("onBigThingStart");
		View vSliding = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vSliding.getLayoutParams();	
        big_thing_left_margin = layout.leftMargin;       
		}
	
	@Override
	public void onBigThingMove (int deltaX, int deltaY)
		{	
		log ("onBigThingMove: " + deltaX);
		View vSliding = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vSliding.getLayoutParams();	
        
        if (big_thing_left_margin != 0)
        	{
        	/* dragging to close extended menu */
            if (deltaX < 0)
            	deltaX = 0;
            if (big_thing_left_margin - deltaX < 0)
            	deltaX = big_thing_left_margin;
            layout.leftMargin = big_thing_left_margin - deltaX;
            vSliding.setLayoutParams (layout);
        	}
        else
        	{
        	/* dragging to switch sets */
            layout.leftMargin = - deltaX;
            layout.width = screen_width;
            vSliding.setLayoutParams (layout);
        	}
		}
	
	@Override
	public void onBigThingUp (int deltaX, int deltaY)
		{		
		log ("onBigThingUp: " + deltaX);        
		
		if (big_thing_left_margin != 0)
			{
        	/* dragging to close extended menu */
			final View vHome = findViewById (R.id.slidingpanel);		
	        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
	        
	        /* when from_margin is 0, the column is extended */
	        final int from_margin = layout.leftMargin;
	        final int to_margin = (deltaX > 100) ? 0 : left_column_width();    		
	        
			Animation a = new Animation()
				{
			    @Override
			    protected void applyTransformation (float interpolatedTime, Transformation t)
			    	{
			        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
			        layout.leftMargin = (int) (from_margin + (to_margin - from_margin) * interpolatedTime);
			        layout.width = screen_width;
			        vHome.setLayoutParams (layout);		        
			    	}
				};
				
			a.setDuration (400);	    		
			vHome.startAnimation (a);
			}
		else
			{
			/* dragging to switch sets */
			}
		}
	}