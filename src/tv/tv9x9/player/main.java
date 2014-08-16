package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import tv.tv9x9.player.HorizontalListView.OnScrollListener;
import twitter4j.TwitterException;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

// import com.google.android.gms.gcm;

import com.facebook.*;
import com.facebook.Session.StatusCallback;
import com.facebook.android.Util;
import com.facebook.model.GraphUser;
import com.facebook.widget.*;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.google.android.gms.ads.*;

public class main extends VideoBaseActivity implements StoreAdapter.mothership
	{
	boolean single_channel = false;
	boolean single_episode = false;
	
	public main()
		{
		identity = "main";
		}
	
	/* note, SIGNOUT, HELP, CATEGORY_ITEM and APP_ITEM are not real layers, but menu items */
	enum toplayer { HOME, PLAYBACK, SIGNIN, GUIDE, STORE, SEARCH, SETTINGS, TERMS, APPS, SIGNOUT, 
					HELP, CATEGORY_ITEM, APP_ITEM, SHAKE, ABOUT, MESSAGES, NAG, TEST, PASSWORD, ADVERT, SOCIAL };
	
	toplayer current_layer = toplayer.HOME;
	toplayer layer_before_signin = toplayer.HOME;
	
	String current_home_stack = null;
	
	Bundle fezbuk_bundle = null;
	
	/* the traveling window uses this as its right and bottom margin */
	int MARGINALIA = pixels_20;
	
	/* style for home page, with 4 thumbs (false), or just 1 (true) */
	boolean mini_mode = false;
	
	/* number of messages for the Messages layer */
	int messages_count = 0;
	
	/* a count of subscribes and unsubscribes, to optimize redrawing */
	int subscription_changes_this_session = 0;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		adjust_layout_for_screen_size();
		home_configure_white_label();
		onVideoActivityLayout();
		setup_global_buttons();
		setup_home_buttons();
		shake_detect();
		
		/* ugly hack because we need to call fezbuk1 in ready() not onCreate() */
		fezbuk_bundle = savedInstanceState;
		
		/* use recalculated value instead of just 20 */
		MARGINALIA = is_phone() ? pixels_10 : pixels_20;
		
		/* home layout needs to be told what it is */
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		set_screen_config (landscape);
		}

	@Override
	public void onStart()
		{
		super.onStart();
		if (flurry_id_sent)
			FlurryAgent.onStartSession (this, config.flurry_id);
		}
	
	@Override
	public void onStop()
		{
		super.onStop();
		if (flurry_id_sent)
			FlurryAgent.onEndSession (this);
		if (config != null)
			config.interrupt_with_notification  = null;
		}
		
	@Override
	public void onResume()
		{
		super.onResume();
		
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
		
		View vMask = findViewById (R.id.top_mask);
		if (vMask != null)
			vMask.setVisibility (View.GONE);
			
		reset_time_played();
		
		if (restore_video_location)
			{
			log ("will restore video");
			if (videoFragment != null)
				{
				videoFragment.set_startup_function (in_main_thread, new Runnable()
					{
					@Override
					public void run()
						{
						log ("video restore startup function");
						restore_location();
						if (video_is_minimized)
							video_minimize (false);
						}						
					});
				}
			else
				log ("videoFragment is null");
			}
		
		shake_resume();
		if (uiHelper != null)
			uiHelper.onResume();
		}
	
    @Override
    public void onPause()
    	{
        super.onPause();
        shake_pause();
        if (uiHelper != null)
        	uiHelper.onPause();
        
        if (!chromecasted)
        	{
        	if (video_is_minimized)
				{
        		log ("saving state of minimized layvideo");
        		remember_location();
				}

        	if (video_is_minimized || current_layer == toplayer.PLAYBACK)
        		{	
        		if (player != null)
        			player.add_to_time_played();
        		}
        	
        	player_full_stop (false);
        	}
    	}
    
    @Override
    public void onDestroy()
    	{
        super.onDestroy();
        if (uiHelper != null)
        	uiHelper.onDestroy();
    	}    
    
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    	{
        super.onActivityResult (requestCode, resultCode, data);
        if (uiHelper != null)
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
	    	
	    	if (is_tablet())
	    		{
	    		View vSettings = findViewById (R.id.settingslayer_tablet);
	    		if (vSettings.getVisibility() == View.VISIBLE)
	    			{
	    			log ("disable settings overlay");
	    			vSettings.setVisibility (View.GONE);
	    			return true;
	    			}
	    		
	    		View vSignin = findViewById (R.id.signinlayer_tablet);
	    		if (vSignin.getVisibility() == View.VISIBLE)
	    			{
	    			log ("disable signin overlay");
	    			vSignin.setVisibility (View.GONE);
	    			return true;
	    			}	    		
	    		}
	    	
	    	if (current_layer == toplayer.PLAYBACK)
	    		{
	    		track_event ("navigation", "back", "back", 0);
	    		player_full_stop (true);
	    		}
	    	else if (current_layer == toplayer.HOME)
	    		{
	    		View vChannelOverlay = findViewById (R.id.channel_overlay);
	    		boolean channel_overlay_visible = vChannelOverlay != null && vChannelOverlay.getVisibility() == View.VISIBLE;
	    		if (menu_is_extended())
	    			toggle_menu();
	    		else if (channel_overlay_visible)
	    			{
	    			if (home_slider != null)
	    				toggle_channel_overlay (home_slider.current_home_page);
	    			}
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
	    	else if (current_layer == toplayer.PASSWORD)
	    		{
	    		slide_away_password();
	    		}
	    	else if (current_layer == toplayer.SIGNIN)
	    		{
	    		if (!is_phone())
		    		{
		    		View vSigning = findViewById (R.id.signin_or_signup);
		    		if (vSigning.getVisibility() == View.VISIBLE)
		    			{
		    			return true;
		    			}
		    		}
	    		toggle_menu();
	    		zero_signin_data();
	    		}
	    	else if (current_layer == toplayer.ADVERT)
	    		{
	    		/* TODO: check for 6 seconds elapsed before allowing this */
	    		}
	    	else if (current_layer == toplayer.GUIDE || current_layer == toplayer.SEARCH 
	    				|| current_layer == toplayer.SETTINGS || current_layer == toplayer.APPS
	    				|| current_layer == toplayer.SHAKE || current_layer == toplayer.ABOUT
	    				|| current_layer == toplayer.MESSAGES || current_layer == toplayer.TEST)
	    		toggle_menu();
	    	return true;
	    	}
	    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
	    	{
	    	chromecast_volume_down();
	    	}
	    else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
	    	{	
	    	chromecast_volume_up();
	    	}
	    
	    return false;
		}

	public void reset_time_played()
		{
		if (player != null)
			player.reset_time_played();
		}
	
	public void adjust_layout_for_screen_size()
		{
		if (is_phone())
			{			
			View vTopControls = findViewById (R.id.top_controls);
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTopControls.getLayoutParams();
			layout.height = pixels_50;
			vTopControls.setLayoutParams (layout);
			
			/* video_layer_new.xml */
			View vControls = findViewById (R.id.controls);
			LinearLayout.LayoutParams layout3 = (LinearLayout.LayoutParams) vControls.getLayoutParams();
			layout3.height = pixels_50;
			vControls.setLayoutParams (layout3);
			
			/* video_layer_new.xml */
			TextView vEpisodeTitle = (TextView) findViewById (R.id.episode_title);
			vEpisodeTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 22);
			
			/* video_layer_new.xml */
			TextView vEpisodeAge = (TextView) findViewById (R.id.episode_age);
			vEpisodeAge.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
			
			// TextView vNumCommentsHeader = (TextView) findViewById (R.id.num_comments_header);
			// vNumCommentsHeader.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			// TextView vNumCommentsDot = (TextView) findViewById (R.id.num_comments_dot);
			// vNumCommentsDot.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);			
			// TextView vNumComments = (TextView) findViewById (R.id.num_comments);
			// vNumComments.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			
			/* video_layer_new.xml */
			TextView vFromPrefix = (TextView) findViewById (R.id.playback_from_prefix);
			vFromPrefix.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
			
			/* video_layer_new.xml */
			TextView vPlaybackChannel = (TextView) findViewById (R.id.playback_channel);
			vPlaybackChannel.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			
			/* video_layer_new.xml, no longer used */
			TextView vPlaybackEpisodeCount = (TextView) findViewById (R.id.playback_episode_count);
			vPlaybackEpisodeCount.setTextSize (TypedValue.COMPLEX_UNIT_SP, 18);
			
			/* video_layer_new.xml, no longer used */
			TextView vPlaybackEpisodePlural = (TextView) findViewById (R.id.playback_episode_plural);
			vPlaybackEpisodePlural.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
			
			/* video_layer_new.xml */
			View vChannelIcon = findViewById (R.id.playback_channel_icon);
			LinearLayout.LayoutParams layout2 = (LinearLayout.LayoutParams) vChannelIcon.getLayoutParams();
			layout2.height = pixels_40;
			layout2.width = pixels_40;
			vChannelIcon.setLayoutParams (layout2);
			
			/* video_layer_new.xml */
			View vChannelIconLandscape = findViewById (R.id.playback_channel_icon_landscape);
			LinearLayout.LayoutParams layout6 = (LinearLayout.LayoutParams) vChannelIconLandscape.getLayoutParams();
			layout6.height = pixels_40;
			layout6.width = pixels_40;
			vChannelIconLandscape.setLayoutParams (layout6);
			
			/* video_layer_new.xml */
			View vPlaybackShare = findViewById (R.id.playback_share);
			LinearLayout.LayoutParams layout4 = (LinearLayout.LayoutParams) vPlaybackShare.getLayoutParams();
			layout4.height = pixels_30;
			layout4.width = pixels_30;
			vPlaybackShare.setLayoutParams (layout4);
			
			/* video_layer_new.xml */
			View vPlaybackFollow = findViewById (R.id.playback_follow);
			LinearLayout.LayoutParams layout5 = (LinearLayout.LayoutParams) vPlaybackFollow.getLayoutParams();
			layout5.height = pixels_30;
			layout5.width = pixels_30;
			vPlaybackFollow.setLayoutParams (layout5);
			}
		
		/* remove the phone/tablet sublayer which we won't use */
		View vNotNeededSigninLayer = findViewById (is_phone() ? R.id.signinlayer_tablet : R.id.signinlayer_phone);
		if (vNotNeededSigninLayer != null)
			((ViewManager) vNotNeededSigninLayer.getParent()).removeView (vNotNeededSigninLayer);
		
		/* remove the phone/tablet sublayer which we won't use */
		View vNotNeededSettingsLayer = findViewById (is_phone() ? R.id.settingslayer_tablet : R.id.settingslayer_phone);
		if (vNotNeededSettingsLayer != null)
			((ViewManager) vNotNeededSettingsLayer.getParent()).removeView (vNotNeededSettingsLayer);

		/* remove the phone/tablet sublayer which we won't use */
		View vNotNeededPasswordLayer = findViewById (is_phone() ? R.id.passwordlayer_tablet : R.id.passwordlayer_phone);
		if (vNotNeededPasswordLayer != null)
			((ViewManager) vNotNeededPasswordLayer.getParent()).removeView (vNotNeededPasswordLayer);
		
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
		
		/* this is a ListView */
		View vMessageListPhone = findViewById (R.id.message_list_phone);
		vMessageListPhone.setVisibility (is_tablet() ? View.GONE : View.VISIBLE);
		
		/* this is a GridView */
		View vMessageListTablet = findViewById (R.id.message_list_tablet);
		vMessageListTablet.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);
		}
	

	@Override
	public void onConfigurationChanged (Configuration newConfig)
		{
		super.onConfigurationChanged (newConfig);
		boolean landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
		set_screen_config (landscape);
		}
	
	public void set_screen_config (boolean landscape)
		{
		final View vMenu = findViewById (R.id.menu_layer);	
        FrameLayout.LayoutParams menu_layout = (FrameLayout.LayoutParams) vMenu.getLayoutParams();	
        
        View vPanel = findViewById (R.id.slidingpanel);
        FrameLayout.LayoutParams panel_layout = (FrameLayout.LayoutParams) vPanel.getLayoutParams();
        
        View vMenuButton = findViewById (R.id.menubutton);
        
        int new_screen_width = 0;
        int new_screen_height = 0;
        
		try
			{
			DisplayMetrics dm = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics (dm);
		    new_screen_width = dm.widthPixels;
		    new_screen_height = dm.heightPixels;
			}
		catch (Exception ex)
			{
			/* nothing */
			}

		int true_width = 0;
		if (landscape)
			true_width = new_screen_width > new_screen_height ? new_screen_width : new_screen_height;
		else
			true_width = new_screen_width > new_screen_height ? new_screen_height : new_screen_width;				
        
		log ("configuration changed: landscape=" + landscape + " true_width=" + true_width + " w=" + new_screen_width + " h=" + new_screen_height);
		
		// I/vtest   (10577): [main] configuration changed: landscape=false true_width=1824 w=1200 h=1824

		
		if (landscape)
			{	
	        menu_layout.width = left_column_width();
	        panel_layout.width = true_width - menu_layout.width;
	        panel_layout.leftMargin = 0;
			panel_layout.gravity = Gravity.RIGHT | Gravity.TOP;
			vMenuButton.setVisibility (View.GONE);
			}
		else
			{	
	        menu_layout.width = left_column_width();
	        panel_layout.width = MATCH_PARENT;
	        panel_layout.leftMargin = 0;
			panel_layout.gravity = Gravity.LEFT | Gravity.TOP;
			vMenuButton.setVisibility (View.VISIBLE);
			}
		
        vMenu.setLayoutParams (menu_layout);
        vPanel.setLayoutParams (panel_layout);
		}
	
	public void player_full_stop (boolean unlaunch_player)
		{
		if (chromecasted)
			{
			chromecast_send_simple ("stop");
			}
		else
			{
			/* this is messy. Can't use onPaused because that will probably occur after analytics */
			if (player != null && !player.is_paused())
				player.add_to_time_played();
			pause_video();
			playerFragment.stop();
			}
		player_real_channel = "?UNDEFINED";    			
		analytics ("back");
		player_real_channel = null;
		
		if (previous_layer == toplayer.SHAKE)
			enable_shake_layer();
		else
			{
			if (unlaunch_player)	
				unlaunch_player();
			}
		}
	
	@Override
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
							if (vPlaybackChannels != null)
								vPlaybackChannels.smoothScrollToPosition (next);
							}
						});
					}
				track_event ("navigation", "swipePG", "swipePG", 0);
				next_channel();
				}
			}
		}

	@Override
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
							if (vPlaybackChannels != null)
								vPlaybackChannels.smoothScrollToPosition (prev);
							}
						});
					}
				track_event ("navigation", "swipePG", "swipePG", 0);
				previous_channel();
				}
			}
		}

	@Override
	public void onVideoActivityFlingLeft()
		{
		if (current_layer == toplayer.PLAYBACK)
			{
			track_event ("navigation", "swipeEP", "swipeEP", 0);
			perform_fling_left();
			if (playback_episode_adapter != null)
				{
				playback_episode_adapter.notifyDataSetChanged();
				HorizontalListView vEpisodes = (HorizontalListView) findViewById (R.id.playback_episodes);			
				vEpisodes.scrollTo (where_am_i() * (pixels_200 + pixels_20));
				}
			if (playback_episode_pager != null)
				{
				playback_episode_pager.rejigger();
				}
			}
		}

	@Override
	public void onVideoActivityFlingRight()
		{
		if (current_layer == toplayer.PLAYBACK)
			{
			track_event ("navigation", "swipeEP", "swipeEP", 0);
			previous_episode();
			if (playback_episode_adapter != null)
				{
				playback_episode_adapter.notifyDataSetChanged();
				HorizontalListView vEpisodes = (HorizontalListView) findViewById (R.id.playback_episodes);			
				vEpisodes.scrollTo (where_am_i() * (pixels_200 + pixels_20));
				}
			if (playback_episode_pager != null)
				{
				playback_episode_pager.rejigger();
				}
			}
		}

	@Override
	public void onVideoActivityRejigger()
		{
		if (playback_episode_pager != null)
			playback_episode_pager.rejigger();
		}
	
	boolean flurry_id_sent = false;
	
	@Override
	public void onVideoActivityVideoStarted()
		{
		if (video_is_minimized)
			video_minimize (false);
		}
	
	@Override
	public void onVideoActivityReady()
		{
		/* normally this is sent in onStart(), but config might not be available then */
		if (config.flurry_id != null)
			FlurryAgent.onStartSession (this, config.flurry_id);
		flurry_id_sent = true;
		
		resume_as_logged_in_user();
		setup_home_buttons();
		populate_home();
		
		fezbuk1 (fezbuk_bundle);
		fezbuk_bundle = null;
		
		try
			{
			if (config.facebook_app_id != null)
				com.facebook.AppEventsLogger.activateApp (this, config.facebook_app_id);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		/* this will create notification settings */
		load_notification_settings();
		
		gcm_register();
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				/* ui needs the notification count for the Message layer */
				messages_count = messages_gather (true).size();
				}
			}, 4000);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();	
		if (extras != null)
			{
			String message = extras.getString ("tv.9x9.message");
			if (message != null)
				track_event ("notification", "toLaunchApp", message, 0);
			
			String channel = extras.getString ("tv.9x9.channel");
			if (channel != null)
				{
                String fake_set[] = new String[] { channel };  
                String episode = extras.getString ("tv.9x9.episode");
                if (episode != null)
                	log ("launch channel: " + channel + ", episode: " + episode);	
                else
                	log ("launch channel: " + channel);
                launch_player (channel, episode, fake_set);
				}
			}

		File nagged = new File (getFilesDir(), "nagged");
		if (!nagged.exists())
			{
			try
				{
				nagged.createNewFile();
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			if (config.usertoken == null)
				{
				if (config.signup_nag.equals ("once") || config.signup_nag.equals ("always"))
					enable_nag_layer();
				}
			}
		
		config.interrupt_with_notification = interrupt_with_notification;
		}
	
	public void home_configure_white_label()
		{
		int top_bars[] = { R.id.sliding_top_bar };
		
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
			if (vSigninLogo != null)
				vSigninLogo.setImageResource (logo_id);
			
			ImageView vTermsLogo = (ImageView) findViewById (R.id.terms_logo);
			if (vTermsLogo != null)
				vTermsLogo.setImageResource (logo_id);
			}
		
		String signin_bg = getResources().getString (R.string.signin_bg);
		if (signin_bg != null)
			{
			int bg = getResources().getIdentifier (signin_bg, "drawable", getPackageName());
			if (bg != 0)
				{
				// View vSigninLayer = findViewById (is_phone() ? R.id.signinlayer_phone : R.id.signinlayer_tablet);
				// vSigninLayer.setBackgroundResource (bg);
	
				View vTermsLayer = findViewById (R.id.termslayer_new);
				vTermsLayer.setBackgroundResource (bg);
				}
			}
		
		boolean uses_chromecast = getResources().getBoolean (R.bool.uses_chromecast);

		if (!uses_chromecast)
			{
			log ("this app is configured to not use chromecast");
			View vMediaRouteButton = findViewById (R.id.media_route_button);
			if (vMediaRouteButton != null)
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
		
		ImageView vLogo = (ImageView) v.findViewById (R.id.logo);	
		vLogo.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	log ("click on: logo");
	        	}
			});	
		}
	
	public void activate_layer (toplayer layer)
		{
    	make_layer_visible (layer);

    	close_menu();
    	
    	if (layer == toplayer.GUIDE)
    		enable_guide_layer();
    	else if (layer == toplayer.HOME)
    		enable_home_layer();
    	else
    		track_layer (layer);
		}
	
	public void track_layer (toplayer layer)
		{
		switch (layer)
			{
			case HOME:
				track_screen ("home");
				break;
				
			case GUIDE:
				track_screen ("guide");
				break;
				
			case STORE:
				track_screen ("store");
				break;
				
			case SETTINGS:
				track_screen ("settings");
				break;
				
			case ABOUT:
				track_screen ("about");
				break;
				
			case APPS:
				track_screen ("appDirectory");
				break;
				
			case SHAKE:
				track_screen ("shake");
				break;
				
			case MESSAGES:
				track_screen ("messages");
				break;
				
			case HELP:
				track_screen ("help");
				break;
				
			case SIGNIN:
				track_screen ("signIn");
				break;
				
			default:
				break;
			}
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
	
	public void setup_global_buttons()
		{
		View vSlidingTopBar = findViewById (R.id.sliding_top_bar);
		
		View vMenu = vSlidingTopBar.findViewById (R.id.menubutton);
		if (vMenu != null)
			vMenu.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: sliding menu button");
		        	toggle_menu (true);
		        	}
				});
		
		View vSearch = vSlidingTopBar.findViewById (R.id.searchbutton);
		if (vSearch != null)
			vSearch.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: global search button");
		        	enable_search_apparatus (R.id.sliding_top_bar);
		        	}
				});	
		
		
		/*
		View vRefresh = vSlidingTopBar.findViewById (R.id.refresh);
		if (vRefresh != null)
			vRefresh.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	if (current_layer == toplayer.GUIDE)
			        	{
			        	log ("click on: guide refresh button");
			        	refresh_guide();
			        	}
		        	else if (current_layer == toplayer.STORE)
		        		{
    		        	log ("click on: store refresh button");
    		        	store_refresh();
		        		}
		        	else if (current_layer == toplayer.HOME)
		        		{
			        	log ("click on: home refresh button");
			        	refresh_home();
		        		}
		        	}
				});
		*/	
		}
	
	public void setup_home_buttons()
		{
		View vHomeLayer = home_layer();
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
	
	public void toggle_menu()
		{
		toggle_menu (false, null);
		track_screen ("menu");
		}
	
	public void toggle_menu (boolean track)
		{
		toggle_menu (track, null);
		}
	
	public void toggle_menu	(final Callback cb)
		{
		toggle_menu (false, cb);
		}
	
	public void toggle_menu	(boolean track, final Callback cb)
		{	
		redraw_menu();
		
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		
		/* set the width of the menu */
		final LinearLayout vMenu = (LinearLayout) findViewById (R.id.menu_layer);		
        FrameLayout.LayoutParams menu_layout = (FrameLayout.LayoutParams) vMenu.getLayoutParams();		
        menu_layout.width = left_column_width();
        vMenu.setLayoutParams (menu_layout);
        
		final View vHome = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
        // layout.gravity = (landscape ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP;
        
        /* when from_margin is 0, the column is extended */
        final int from_margin = layout.leftMargin;
        final int to_margin = (layout.leftMargin == 0) ? left_column_width() : 0;    		
        
        log ("toggle menu: track=" + track + " orientation=" + orientation + " landscape=" + landscape + " from_margin=" + from_margin + " to_margin=" + to_margin);
        
        if (track)
	        {
	        if (from_margin != 0)
	        	track_current_screen();
	        else
	    		track_screen ("menu");
	        }
        
		set_invisible_mask (from_margin == 0);
		
		Animation a = new Animation()
			{
		    @Override
		    protected void applyTransformation (float interpolatedTime, Transformation t)
		    	{
		        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
		        layout.leftMargin = (int) (from_margin + (to_margin - from_margin) * interpolatedTime);
		        layout.width = screen_width;
		        log ("menu-ANIM: " + layout.leftMargin);
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
		if (!landscape)
		vHome.startAnimation (a);
		
		setup_menu_buttons();
		}
	
	public void set_invisible_mask (boolean visible)
		{
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		
		if (landscape)
			{
			/* don't appear to lose control of the app */
			visible = false;
			}
		
		View vMask = findViewById (R.id.invisible_mask);
		if (vMask != null)
			{
			log ("set invisible mask: " + visible);
			vMask.setVisibility (visible ? View.VISIBLE : View.GONE);
			max_delta_x = 0;
			if (visible)
				vMask.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: INVISIBLE MASK");
			        	if (max_delta_x < 10)
			        		toggle_menu();
			        	}
					});	
			}
		else
			log ("can't find invisible mask!");
		}
	
	public boolean menu_is_extended()
		{
		final View vHome = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();	
        return layout.leftMargin != 0;
		}
	
	public void hide_menu_immediately()
		{
		log ("hide menu immediately");
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		final View vHome = findViewById (R.id.slidingpanel);	
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vHome.getLayoutParams();
        layout.leftMargin = 0;
        if (!landscape)
        	layout.width = screen_width;
        vHome.setLayoutParams (layout);
        set_invisible_mask (false);
		}

	public int left_column_width()
		{
	    float percent = is_phone() ? 0.65f : 0.65f;
	    return (int) (screen_width * percent);
		}
	
	public class menuitem
		{
		toplayer type;
		String title;
		String id;
		int unselected_drawable;
		int selected_drawable;
		app app_item;
		menuitem (toplayer type, int title_resource, int unselected_drawable, int selected_drawable)
			{
			this.type = type;
			this.title = getResources().getString (title_resource);
			this.unselected_drawable = unselected_drawable;
			this.selected_drawable = selected_drawable;
			this.app_item = null;
			}
		menuitem (app app_item)
			{
			this.type = toplayer.APP_ITEM;
			this.title = app_item.title;
			this.unselected_drawable = 0;
			this.selected_drawable = 0;
			this.app_item = app_item;
			}
		menuitem (toplayer type, String id, String title)
			{
			this.type = type;
			this.title = title;
			this.unselected_drawable = 0;
			this.selected_drawable = 0;
			this.app_item = null;
			this.id = id;
			}		
		}	
	
	public boolean apps_expanded = true;
	public boolean categories_expanded = false;
	
	public boolean initialized_app_menu = false;
	public boolean initialized_category_menu = false;
	
	public void redraw_menu()
		{
		if (config == null)
			return;
		
		if (!initialized_app_menu)
			{
			initialized_app_menu = true;
			in_main_thread.postDelayed(new Runnable()
				{
				@Override
				public void run()
					{
					init_apps();
					}
				}, 15000);
			}
		
		setup_menu_buttons();
		
		/*
		 * if we want categories in the menu
		if (category_list == null && !initialized_app_menu)
			{
			initialized_category_menu = true;
			top_categories();
			}
		*/
		
		boolean is_facebook = config != null && config.email != null && config.email.equals ("[via Facebook]");
		
		Stack <menuitem> items = new Stack <menuitem> ();
		
		items.push (new menuitem (toplayer.HOME, R.string.home, R.drawable.icon_home, R.drawable.icon_home_press));
		items.push (new menuitem (toplayer.GUIDE, R.string.my_following, R.drawable.icon_heart_black, R.drawable.icon_heart_press));
		items.push (new menuitem (toplayer.STORE, R.string.store, R.drawable.icon_store, R.drawable.icon_store_press));
	
		/*
		 * if we want categories in the menu
		if (current_layer == toplayer.STORE && category_list != null)
			{				
			for (String category_id: category_list)
				{
				String name = category_names.get (category_id);
				if (name != null)
					items.push (new menuitem (toplayer.CATEGORY_ITEM, category_id, name));
				}
			}
		 */
		
		items.push (new menuitem (toplayer.MESSAGES, R.string.messages, R.drawable.icon_notification, R.drawable.icon_notification_press));
		
		if (config.shake_and_discover_feature)
			items.push (new menuitem (toplayer.SHAKE, R.string.shake, R.drawable.icon_shake_black, R.drawable.icon_shake_black));
		
		items.push (new menuitem (toplayer.SETTINGS, R.string.settings, R.drawable.icon_setting, R.drawable.icon_setting_press));
		
		// items.push (new menuitem (toplayer.TEST, R.string.test, R.drawable.icon_setting, R.drawable.icon_setting_press));

		// items.push (new menuitem (toplayer.SOCIAL, R.string.social, R.drawable.icon_setting, R.drawable.icon_setting_press));
		
		/* no help screen has been provided yet */
		// items.push (new menuitem (toplayer.HELP, R.string.help, R.drawable.icon_help, R.drawable.icon_help));
		
		items.push (new menuitem (toplayer.APPS, R.string.suggested_tv_apps, R.drawable.icon_apps, R.drawable.icon_apps_press));
		
		if (apps_expanded && recommended_apps != null)
			{			
			for (app a: recommended_apps)
				items.push (new menuitem (a));
			}
		
		if (config.about_us_url != null)
			items.push (new menuitem (toplayer.ABOUT, R.string.about_us, R.drawable.icon_about, R.drawable.icon_about_press));
		
		if (config.usertoken != null)
			items.push (new menuitem (toplayer.SIGNOUT, R.string.sign_out, R.drawable.icon_signout, R.drawable.icon_signout));
		
		int count = items.size();
		
		menuitem item_list[] = new menuitem [count];
		
		while (items.size() > 0)
			{
			menuitem item = items.pop();
			item_list [--count] = item;	
			}

		if (menu_adapter == null)
			{
			menu_adapter = new MenuAdapter (main.this, item_list);
			ListView vMenuList = (ListView) findViewById (R.id.menu_list);
			vMenuList.setAdapter (menu_adapter);
			vMenuList.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView parent, View v, int position, long id)
					{
					menu_click (menu_adapter.get_item (position));
					}
				});
			}
		else
			menu_adapter.set_content (item_list);
		
		/* the triple button in the top bar, not really in the menu but needs to sync with it */
		TextView vMenuNotifyCount = (TextView) findViewById (R.id.menu_notify_count);
		if (vMenuNotifyCount != null)
			{
			vMenuNotifyCount.setText ("" + messages_count);
			vMenuNotifyCount.setVisibility (messages_count < 1 ? View.GONE : View.VISIBLE);
			}
		}
	
	public void close_menu()
		{
		if (menu_is_extended())
			toggle_menu (false);
		}
	
	public void menu_click (menuitem item)
		{
		switch (item.type)
			{
			case HOME:
	        	log ("click on: menu home");
	        	close_menu();
	        	enable_home_layer();
	        	break;
	        	
			case GUIDE:
	        	log ("click on: menu guide");
	        	close_menu();
	        	enable_guide_layer();	        	
	        	break;
	        	
			case STORE:
	        	log ("click on: menu store");
	        	close_menu();
	        	enable_store_layer();
	        	break;

			case CATEGORY_ITEM:
				log ("click on: menu category item: " + item.title);
				for (int i = 0; i < category_list.length; i++)
					if (category_list [i].equals (item.id))
						{
						load_category (i, 0);
						close_menu();
						track_layer (toplayer.STORE);
						}
				break;
				
			case SHAKE:
	        	log ("click on: menu shake");
	        	close_menu();
	        	enable_shake_layer();
				break;
				
			case TEST:
	        	log ("click on: menu test");
	        	close_menu();
	        	enable_test_layer();
				break;
				
			case ABOUT:
	        	log ("click on: menu about");
	        	close_menu();
	        	enable_about_layer();
				break;
				
			case SETTINGS:
	        	log ("click on: menu settings");		        	
	        	close_menu();
	        	enable_settings_layer();
	        	break;
	        	
			case MESSAGES:
				log ("click on: menu messages");
				close_menu();
				enable_messages_layer();
				break;
				
			case HELP:
				log ("click on: menu help");
				break;
				
			case APPS:
	        	log ("click on: menu apps");
	        	close_menu();
	        	track_event ("navigation", "toAppDirectory", "toAppDirectory", 0);
	        	enable_apps_layer();
	        	break;
	        	
			case APP_ITEM:
				log ("click on: menu app item: " + item.app_item.title);
				launch_suggested_app (item.app_item.title, item.app_item.market_url);
				break;
				
			case SIGNOUT:
	        	log ("click on: menu signout");
	        	signout_from_app_or_facebook();
	        	break;
	    
			case SOCIAL:
	        	close_menu();				
				log ("click on: menu social");
				enable_social_layer();
				break;
				
			case NAG:
				log ("click on: nag");
				close_menu();
				enable_nag_layer();
				break;
				
			default:
				break;
			}
		}
	
    MenuAdapter menu_adapter = null;
    
	public class MenuAdapter extends BaseAdapter
		{
		private Context context;
		private menuitem menu[] = null;
		
		public MenuAdapter (Context c, menuitem menu[])
			{
			this.context = c;
			this.menu = menu;
			}
	
		public void set_content (menuitem menu[])
			{
			this.menu = menu;
			notifyDataSetChanged();
			}
		
		@Override
		public int getCount()
			{			
			return menu == null ? 0 : menu.length;
			}
		
		@Override
		public Object getItem (int position)
			{
			return position;
			}
	
		public menuitem get_item (int position)
			{
			return menu [position];
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
					
			// log ("menu getView: " + position + " (of " + getCount() + ")"); // noisy
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (main.this, R.layout.menu_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (menu [position].title);
			
			int icon = current_layer == menu [position].type ? menu [position].selected_drawable : menu [position].unselected_drawable;
			
			ImageView vIcon = (ImageView) rv.findViewById (R.id.icon);
			if (vIcon != null)
				{
				vIcon.setVisibility (icon == 0 ? View.INVISIBLE : View.VISIBLE);
				vIcon.setImageResource (icon);
				}
			
			View vDownload = rv.findViewById (R.id.download);
			if (vDownload != null)
				vDownload.setVisibility (menu [position].type == toplayer.APP_ITEM ? View.VISIBLE : View.GONE);
			
			
			
			ImageView vAppIcon = (ImageView) rv.findViewById (R.id.app_icon); 	
			if (vAppIcon != null)
				{		
				if (menu [position].type == toplayer.APP_ITEM)
					{
					boolean app_icon_found = false;
					String filename = getFilesDir() + "/" + config.api_server + "/apps/" + menu [position].app_item.basename + ".png";
					
					File f = new File (filename);
					if (f.exists ())
						{
						log ("exists: " + filename);
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							{
							app_icon_found = true;
							vAppIcon.setImageBitmap (bitmap);
							}
						}
					vAppIcon.setVisibility (app_icon_found ? View.VISIBLE : View.INVISIBLE);
					}
				else
					vAppIcon.setVisibility (View.GONE);
				}			
			
			TextView vCount = (TextView) rv.findViewById (R.id.count);
			if (vCount != null)
				{
				boolean count_visible = menu [position].type == toplayer.APPS;
				if (menu [position].type == toplayer.MESSAGES && messages_count > 0)
					count_visible = true;
				vCount.setVisibility (count_visible ? View.VISIBLE : View.GONE);
				if (menu [position].type == toplayer.APPS)					
					{
					vCount.setText (apps == null ? "0" : ("" + apps.length));
					vCount.setBackgroundResource (R.drawable.menu_number_bg);	
					vCount.setTextColor (Color.BLACK);
					}
				else if (menu [position].type == toplayer.MESSAGES)
					{
					vCount.setText ("" + messages_count);
					vCount.setBackgroundResource (R.drawable.menu_number_bg_red);
					vCount.setTextColor (Color.WHITE);
					}
				}
		
			if (menu [position].type == toplayer.CATEGORY_ITEM && current_category_index >= 0)
				{
				String current_category_id = category_list [current_category_index];
				if (menu [position].id.equals (current_category_id))
					{
					vTitle.setTextColor (Color.WHITE);
					rv.setBackgroundColor (Color.argb (0xFF, 0x77, 0x77, 0x77));
					vAppIcon.setImageResource (R.drawable.icon_category);
					vAppIcon.setVisibility (View.VISIBLE);
					}
				else
					{
					vTitle.setTextColor (Color.BLACK);
					rv.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
					vAppIcon.setVisibility (View.INVISIBLE);
					}
				}
			else if (current_layer == toplayer.PASSWORD && menu [position].type == toplayer.SETTINGS)
				{
				/* password is part of settings */
				vTitle.setTextColor (Color.argb (0xFF, 0x77, 0x77, 0x77));
				rv.setBackgroundColor (Color.WHITE);
				}
			else if (current_layer == menu [position].type)
				{
				vTitle.setTextColor (Color.argb (0xFF, 0x77, 0x77, 0x77));
				rv.setBackgroundColor (Color.WHITE);
				}
			else
				{
				vTitle.setTextColor (Color.BLACK);
				rv.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
				}
				
			return rv;
			}	
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
		        	enable_signin_layer (null);
		        	}
				});	
				
		/* username adjustments */
		
		vSignin.setVisibility (config.usertoken == null ? View.VISIBLE : View.GONE);		
		
		View vIdentity = findViewById (R.id.identity);
		vIdentity.setVisibility (config.usertoken == null ? View.GONE : View.VISIBLE);
		
		if (config.usertoken != null)
			{
			TextView vName = (TextView) findViewById (R.id.menu_name);
			vName.setText (config.username);
			
			TextView vEmail = (TextView) findViewById (R.id.menu_email);
			vEmail.setText (config.email);
			}
		}

	public void signout_from_app_or_facebook()
		{
		if (config != null)
			{
			if (config.email != null && config.email.equals ("[via Facebook]"))
				facebook_logout();
			else
				signout();
			}
		zero_signin_data();
		
		/* the settings view might be in the slider */
		redraw_settings();
		}
	
	@Override
	public void onVideoActivitySignout()
		{
		redraw_menu();
		setup_menu_buttons();
		zero_signin_data();
		/* the settings view might be in the slider */
		redraw_settings();
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
	public void fixup_pause_or_play_button()
		{
		if (player != null && !chromecasted)
			{
			/* if in main thread */
			if (Looper.myLooper() == Looper.getMainLooper())
				fixup_pause_or_play_inner();
			
			for (int delay: new Integer[] { 10, 100, 250, 500, 1000, 2000, 3000, 4000, 5000 })
				{
				in_main_thread.postDelayed (new Runnable()
					{
					@Override
					public void run()
						{
						fixup_pause_or_play_inner();						
						}	
					}, delay);
				}
			}
		}
	
	public void fixup_pause_or_play_inner()
		{
		if (!chromecasted)
			{
			boolean is_paused = player == null || player.is_paused();
			ImageView vPausePlay = (ImageView) findViewById (R.id.pause_or_play);
			if (vPausePlay != null)
				vPausePlay.setImageResource (is_paused ? R.drawable.play_tablet : R.drawable.pause_tablet);
			}
		}
	
	@Override
	public void onVideoActivityProgress (boolean is_playing, long offset, long duration, float pct)
		{
		videoActivityUpdateProgressBar (offset, duration);
		
		/* update pause button when we can */
		if (!chromecasted && Looper.myLooper() == Looper.getMainLooper())
			fixup_pause_or_play_inner();
		}

	@Override
	public void onLastEpisode()
		{
		log ("last episode!");
		player_full_stop (true);
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
		
		View vUnderStatus = findViewById (R.id.underneath_status_bar);
		
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		// int orientation = getRequestedOrientation();
		// boolean landscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		
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
			
			final View decorView = getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
			decorView.setSystemUiVisibility(uiOptions);
			
			/* try to hide the status bar if it appears. this code does NOT work as intended */
			decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener()
				{
			    @Override
			    public void onSystemUiVisibilityChange (int visibility)
			    	{
			        // Note that system bars will only be "visible" if none of the
			        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
			        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) 
			        	{
			            // TODO: The system bars are visible. Make any desired
			            // adjustments to your UI, such as showing the action bar or
			            // other navigational controls.
						int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
						decorView.setSystemUiVisibility(uiOptions);
			        	}
			        else
			        	{
			            // TODO: The system bars are NOT visible. Make any desired
			            // adjustments to your UI, such as hiding the action bar or
			            // other navigational controls.
			        	}
			    	}
				});
			
			/*
			Rect rectangle = new Rect();
			Window window = getWindow();
			window.getDecorView().getWindowVisibleDisplayFrame (rectangle);
			int statusBarHeight = rectangle.top;
			int contentViewTop = window.findViewById (Window.ID_ANDROID_CONTENT).getTop();
			int titleBarHeight = contentViewTop - statusBarHeight;
			*/
			
			int sbh = 0;
			int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
			if (resourceId > 0)
				sbh = getResources().getDimensionPixelSize(resourceId);
			
			LinearLayout.LayoutParams status_layout = (LinearLayout.LayoutParams) vUnderStatus.getLayoutParams();
			status_layout.height = sbh;
			vUnderStatus.setLayoutParams (status_layout);
			
			vUnderStatus.setVisibility (View.VISIBLE);
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
				
				vUnderStatus.setVisibility (View.GONE);
				
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
	
	public int where_am_i()
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
		return you_are_here;
		}
	
	public void next_channel()
		{
		String next_id = next_channel_id();
		log ("previous channel id: " + next_id);		
		change_channel (next_id);	
		}

	private int next_channel_index()
		{		
		int you_are_here = where_am_i();
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
		int you_are_here = where_am_i();
		return (you_are_here <= 1) ? arena.length - 1 : you_are_here - 1;
		}
	
	private String previous_channel_id()
		{
		if (arena != null)
			return arena [ previous_channel_index() ];
		else
			return player_real_channel;
		}	
	
	private void change_channel (String channel_id)
		{
		log ("change channel to: " + channel_id);
		if (!config.channel_loaded (channel_id))
			load_channel_then (channel_id, false, change_channel_inner_CB, channel_id, null);
		else
			change_channel_inner_inner (channel_id);
		}
	
	final Callback change_channel_inner_CB = new Callback()
		{
		public void run_string_and_object (String channel_id, Object o)
			{
			change_channel_inner_inner (channel_id);
			}
		};
	
	public void change_channel_inner_inner (String channel_id)
		{
		player_real_channel = channel_id;
		program_line = config.program_line_by_id (player_real_channel);
		current_episode_index = 1;
		
		if (player_real_channel != null && program_line != null)
			{
			start_playing();
			if (!config.is_youtube (player_real_channel))
				thumbnail.download_titlecard_images_for_channel (main.this, config, player_real_channel, in_main_thread, titlecard_image_update);
			}		
		
		String episode_id = program_line != null && program_line.length >= 1 ? program_line [0] : null;
		
		if (playback_episode_pager != null)
			playback_episode_pager.set_content (channel_id, program_line);
		
		onVideoActivityRefreshMetadata (player_real_channel, episode_id);
		}		
	
	/* updates just the channel thumb, not other metadata -- to prevent endless looping */
		
	final Runnable update_channel_thumb = new Runnable()
		{
		public void run()
			{
			if (player_real_channel != null)
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
			}
		};
		
		
	public boolean update_channel_thumb_inner (String actual_channel_id)
		{
		boolean channel_thumbnail_found = false;
		if (1 == 2)
		{		
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
	
	public void clear_metadata()
		{
		for (int id: new Integer[] 
				{ R.id.playback_channel, R.id.landscape_channel_name, R.id.landscape_episode_name, R.id.episode_title, R.id.episode_age })
			{
			TextView v = (TextView) findViewById (id);
			if (v != null)
				v.setText ("");
			}
		LinearLayout vDesc = (LinearLayout) findViewById (R.id.desc_scroll);
		vDesc.removeAllViews();		
		}
	
	public void update_metadata_inner()
		{
		String actual_channel_id = player_real_channel;
		log ("update metadata inner: " + actual_channel_id);
		
		if (actual_channel_id == null)
			return;
		
		/* first, do whatever we can if this is not a virtual channel */
		
		String channel_name = null;
				
		if (!actual_channel_id.contains (":"))
			channel_name = config.pool_meta (actual_channel_id, "name");
		
		// TextView vChannel = (TextView) findViewById (R.id.tunedchannel);
		TextView vChannel = (TextView) findViewById (R.id.playback_channel);
		TextView vLandscapeChannel = (TextView) findViewById (R.id.landscape_channel_name);
		
		if (channel_name != null)
			{
			if (vChannel != null)
				vChannel.setText (channel_name);				
			if (vLandscapeChannel != null)
				vLandscapeChannel.setText (channel_name);		
			update_channel_icon (actual_channel_id);
			}
		
		program_line = config.program_line_by_id (actual_channel_id);
		
		if (program_line == null || current_episode_index > program_line.length)
			return;
		
		/* now if we have episodes, more can be done */
		
		String episode_id = program_line [current_episode_index - 1];
		String episode_name = config.program_meta (episode_id, "name");
		String episode_desc = config.program_meta (episode_id, "desc");
		
		if (actual_channel_id.contains (":"))
			actual_channel_id = config.program_meta (episode_id, "real_channel");
		channel_name = config.pool_meta (actual_channel_id, "name");
		
		if (vChannel != null)
			vChannel.setText (channel_name);		
		if (vLandscapeChannel != null)
			vLandscapeChannel.setText (channel_name);
		
		TextView vLandscapeEpisode = (TextView) findViewById (R.id.landscape_episode_name);
		if (vLandscapeEpisode != null)
			vLandscapeEpisode.setText (episode_name != null && !episode_name.equals ("") ? episode_name : "[no episode name]");
				
		if (!update_channel_thumb_inner (actual_channel_id))
			{
			String channel_line_of_one[] = { actual_channel_id };
			thumbnail.stack_thumbs (main.this, config, channel_line_of_one, -1, in_main_thread, update_channel_thumb);
			}		

		TextView vEpisodeTitle = (TextView) findViewById (R.id.episode_title);
		vEpisodeTitle.setText (episode_name);
		
		NEW_fill_episode_description (episode_desc);
		
		/*
		vEpisodeTitle.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	toggle_extended_comments_view();
	        	}
			});	
		*/
		
		/*
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
		*/
		
		String timestamp = config.program_meta (episode_id, "timestamp");		
		String ago = util.ageof (main.this, Long.parseLong (timestamp) / 1000);
		
		TextView vAgo = (TextView) findViewById (R.id.episode_age);
		if (vAgo != null)
			vAgo.setText (ago);
		
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
		
		update_episode_count (player_real_channel);
		
		log ("update_metadata: channel=|" + channel_name + "|");
		log ("update_metadata: episode=|" + episode_name + "|");		
		}
	
	@Override
	public void update_metadata_mini (String episode_id)
		{
		String episode_name = config.program_meta (episode_id, "name");
		String episode_desc = config.program_meta (episode_id, "desc");
		TextView vEpisodeName = (TextView) findViewById (R.id.episode_title);
		if (vEpisodeName != null)
			vEpisodeName.setText (episode_name != null && !episode_name.equals ("") ? episode_name : "[no episode name]");
		String timestamp = config.program_meta (episode_id, "timestamp");
		String ago = util.ageof (main.this, Long.parseLong (timestamp) / 1000);		
		TextView vAgo = (TextView) findViewById (R.id.episode_age);
		if (vAgo != null)
			vAgo.setText (ago);
		fill_episode_description (episode_desc);		
		
		if (program_line != null)
			{
			for (int i = 0; i < program_line.length; i++)
				{
				String potential_episode_id = program_line [i];
				if (episode_id.equals (potential_episode_id))
					{
					current_episode_index = i + 1;
					break;
					}
				}
			if (playback_episode_adapter != null)
				playback_episode_adapter.notifyDataSetChanged();
			
			if (playback_episode_pager != null) ;
				playback_episode_pager.notifyDataSetChanged();
			}
		}
	
	/*
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
	*/
	
	public void fill_episode_description (String episode_desc)
		{
		if (episode_desc != null)
			{
			Pattern pattern = android.util.Patterns.WEB_URL;

			/* the below does a lot of mangling so that we can use WEB_URL; would not be necessary with
			   a simpler pattern, but a simpler pattern would likely match far fewer URLs */
			
			while (true)
				{
				Matcher matcher = pattern.matcher (episode_desc);
				if (matcher.find (0))
					{
					// int matchStart = matcher.start(1);
					// int matchEnd = matcher.end();
					String before = matcher.group (0);
					String after = before;
					after = after.replaceAll ("^http:", "xTTp:");
					after = after.replaceAll ("^https:", "xTTpS:");
					after = "<a href=\"" + after + "\">" + after + "</a>";
					after = after.replaceAll ("\\.", "\032");
					log ("URL FOUND: " + before + " => " + after);
					episode_desc = episode_desc.replace (before, after);
					}		
				else
					break;
				}
			
			episode_desc = episode_desc.replaceAll ("xTTpS:", "https:");
			episode_desc = episode_desc.replaceAll ("xTTp:", "http:");	
			episode_desc = episode_desc.replaceAll ("\032", ".");			
			episode_desc = episode_desc.replaceAll ("\n", "<br>\n");
			String margin = "-" + pixels_10 + "px ";
			String padding = pixels_10 + "px ";
			// margin = "-0px ";  uncomment these two lines to see white-box problem
			// padding = "0px ";
			String css = "background-color: #1d1d1d; color: #c0c0c0; margin: " + margin + "; padding: " + padding + "; overflow: hidden;";
			String styleblock = "<style> a:link { color: #ffffff; } </style>";
			episode_desc = styleblock + "<div style=\"" + css + "\">" + "<div width=\"95%\">" + episode_desc + "</div></div>";
			}
		
		WebView vDesc = (WebView) findViewById (R.id.playback_episode_description);
		if (vDesc != null)
			{

			vDesc.setHorizontalScrollBarEnabled (false);
			vDesc.getSettings().setLayoutAlgorithm (LayoutAlgorithm.SINGLE_COLUMN);
			vDesc.setScrollBarStyle (View.SCROLLBARS_INSIDE_OVERLAY);
			
			// this is used as the default encoding of decode.
			vDesc.getSettings().setDefaultTextEncodingName ("utf-8");

			// vDesc.loadData (episode_desc == null ? "" : episode_desc, "text/html", "utf-8");
			vDesc.loadDataWithBaseURL ("http://www.youtube.com/", episode_desc == null ? "" : episode_desc, "text/html", "utf-8", null);
			}
		}
	
	public void NEW_fill_episode_description (String episode_desc)
		{		
		ScrollView vContainer = (ScrollView) findViewById (R.id.desc_scroll_container);
		vContainer.scrollTo (0, 0);
		
		LinearLayout vDesc = (LinearLayout) findViewById (R.id.desc_scroll);
		vDesc.removeAllViews();
		
		if (episode_desc != null)
			{
			String lines[] = episode_desc.split ("\n");
			for (String line: lines)
				{
				// log ("LINE: |" + line + "|");				
				NEW_fill_episode_description_process_line (vDesc, line);
				}
			}

		ScrollView.LayoutParams layout = new ScrollView.LayoutParams
				(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT);
		
		vContainer.updateViewLayout (vDesc, layout);
		}
	
	public void NEW_fill_episode_description_process_line (LinearLayout vDesc, String line)
		{
		Pattern pattern = android.util.Patterns.WEB_URL;
		
		int maxchar = is_phone() ? 16 : 24;
		
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
			
			TextView v1 = new TextView (this);
			v1.setText (text_to_left);
			v1.setTextColor (Color.rgb (0xC0, 0xC0, 0xC0));
			v1.setTextSize (TypedValue.COMPLEX_UNIT_SP, 16);
			if (text_to_left.contains ("http") || text_to_left.contains ("HTTP"))
				v1.setMaxLines (1);
		    // v1.setBackgroundColor (Color.RED);
		    
			TextView v2 = new TextView (this);
			v2.setText (text);
			v2.setTextColor (Color.rgb (0xFF, 0xFF, 0xFF));
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
				
			TextView v3 = new TextView (this);
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
				LinearLayout linear = new LinearLayout (this);	
				linear.addView (v2);
				linear.addView (v3);
				append_views [viewcount++] = linear;
		    	}
		    else if (v3.length() > maxchar)
		    	{
		    	// log ("scenario 3: v1 + v2 // v3");
				LinearLayout linear = new LinearLayout (this);	
		    	linear.addView (v1);
		    	linear.addView (v2);
				append_views [viewcount++] = linear;
				append_views [viewcount++] = v3;
		    	}
		    else
		    	{
		    	// log ("scenario 4: v1 + v2 + v3");
				LinearLayout linear = new LinearLayout (this);	
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
			TextView v = new TextView (this);
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
			NEW_fill_episode_description_process_line (vDesc, process_afterwards);
			}
		}
	
	final Runnable go_halfscreen = new Runnable()
		{
		public void run()
			{			
			log ("go halfscreen");
			update_metadata_inner();
			player.set_full_screen (false);
			}
		};
		
	final Runnable go_fullscreen = new Runnable()
		{
		public void run()
			{			
			log ("go fullscreen");
			player.set_full_screen (true);
			}
		};		
	
	public void follow_or_unfollow (String channel_id, View v)
		{
		if (config.is_subscribed (channel_id))
			unsubscribe (channel_id, v);
		else
			subscribe (channel_id, v);
		}
	
	public void subscribe (final String real_channel, final View v)
		{
		if (config.usertoken != null)
			{
    		if (follow_or_unfollow_in_progress)
				{
				log ("follow or unfollow already in progress");
				return;
				}
			final int position = config.youtube_auth_token == null ? config.first_empty_position() : 0;
	    	if (position >= 0)
				{
	    		int server_position = (position == 0) ? 0 : config.client_to_server (position);
	    		
	    		if (real_channel.startsWith ("="))
	    			{
	    			subscribe_via_post (real_channel, position, v);
	    			return;
	    			}
	    		
	    		make_follow_icon_a_spinner (v);
	    		new playerAPI (in_main_thread, config, "subscribe?user=" + config.usertoken + "&channel=" + real_channel + "&grid=" + server_position)
					{
					public void success (String[] lines)
						{
						subscription_changes_this_session++;
						make_follow_icon_normal (v);
						toast_by_resource (R.string.following_yay);
						config.place_in_channel_grid (real_channel, position, true);
						config.set_channel_meta_by_id (real_channel, "subscribed", "1");						
						String youtube_username = config.pool_meta (real_channel, "extra");
						ytchannel.subscribe_on_youtube (config, youtube_username);
						config.subscriptions_altered = config.grid_update_required = true;
						track_event ("function", "follow", "follow", 0, real_channel);
						if (grid_slider != null)
							grid_slider.notifyDataSetChanged();
						update_layer_after_subscribe (real_channel);
						}
					public void failure (int code, String errtext)
						{
						make_follow_icon_normal (v);
						alert ("Subscription failure: " + errtext);
						config.dump_subscriptions();
						}
					};
				}
			}
		else
			{
			enable_signin_layer (new Runnable()
				{
				@Override
				public void run()
					{
					if (config.usertoken != null)
						subscribe (real_channel, v);
					}
				});
			}
		}
		
	public void unsubscribe (final String real_channel, final View v)
		{
		if (config.usertoken != null)
			{
    		if (follow_or_unfollow_in_progress)
				{
				log ("follow or unfollow already in progress");
				return;
				}
			final int position =  config.youtube_auth_token == null ? config.first_position_of (real_channel) : 0;
	    	if (position >= 0)
				{
	    		make_follow_icon_a_spinner (v);
	    		int server_position = (position == 0) ? 0 : config.client_to_server (position);
	    		new playerAPI (in_main_thread, config, "unsubscribe?user=" + config.usertoken + "&channel=" + real_channel + "&grid=" + server_position)
					{
					public void success (String[] lines)
						{
						subscription_changes_this_session++;
						make_follow_icon_normal (v);
						toast_by_resource (R.string.unfollowing_yay);
						config.place_in_channel_grid (real_channel, position, false);
						config.set_channel_meta_by_id (real_channel, "subscribed", "0");		
						String youtube_username = config.pool_meta (real_channel, "extra");
						ytchannel.delete_on_youtube (config, youtube_username);
						config.subscriptions_altered = config.grid_update_required = true;
						track_event ("function", "unfollow", "unfollow", 0, real_channel);
						if (grid_slider != null)
							grid_slider.notifyDataSetChanged();
						update_layer_after_subscribe (real_channel);
						}
					public void failure (int code, String errtext)
						{
						make_follow_icon_normal (v);
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
	
	boolean follow_or_unfollow_in_progress = false;
	
	public void set_follow_icon_state (int resource_id, String channel_id, int follow_resource_id, int unfollow_resource_id)
		{
		View v = findViewById (resource_id);
		if (v != null && channel_id != null)
			set_follow_icon_state (v, channel_id, follow_resource_id, unfollow_resource_id);
		}

	public void set_follow_icon_state (View v, String channel_id, int follow_resource_id, int unfollow_resource_id)
		{
		if (v != null && channel_id != null)
			{
			ImageView vFollowIcon = (ImageView) v.findViewById (R.id.follow_icon);
			if (vFollowIcon != null)
				vFollowIcon.setImageResource (config.is_subscribed (channel_id) ? unfollow_resource_id : follow_resource_id);
			}
		}
	
	public void make_follow_icon_a_spinner (View v)
		{
		follow_or_unfollow_in_progress = true;
		if (v != null)
			{
			View vSpinner = v.findViewById (R.id.follow_progress);
			if (vSpinner != null)
				vSpinner.setVisibility (View.VISIBLE);
			View vIcon = v.findViewById (R.id.follow_icon);
			if (vIcon != null)
				vIcon.setVisibility (View.GONE);
			}
		}
	
	public void make_follow_icon_normal (View v)
		{
		follow_or_unfollow_in_progress = false;
		if (v != null)
			{
			View vSpinner = v.findViewById (R.id.follow_progress);
			if (vSpinner != null)
				vSpinner.setVisibility (View.GONE);
			View vIcon = v.findViewById (R.id.follow_icon);
			if (vIcon != null)
				vIcon.setVisibility (View.VISIBLE);
			}
		}
	
	public void update_layer_after_subscribe (String channel_id)
		{
		log ("update layer after subscribe: " + channel_id);
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
			set_follow_icon_state (R.id.guide_follow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			grid_slider.notifyDataSetChanged();	
			// redraw_3x3 (current_slider_view, current_set - 1);
			}
		else if (current_layer == toplayer.HOME)
			{
			/* see home_slider update below */
			}
		else if (current_layer == toplayer.PLAYBACK)
			{
			set_follow_icon_state (R.id.playback_follow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			set_follow_icon_state (R.id.playback_follow_landscape, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);		
			}
		
		if (home_slider != null)
			home_slider.refresh();
		}

	public void subscribe_via_post (final String real_channel, final int position, final View v)
		{
		make_follow_icon_a_spinner (v);
		
		final String channel_name = config.pool_meta (real_channel, "name");
		final String channel_image = config.pool_meta (real_channel, "thumb");
		final String url = "http://www.youtube.com/user/" + real_channel.replaceAll ("^=", "");
		
		String program_line[] = config.program_line_by_id (real_channel);
		
		final String e1_thumb = (program_line != null && program_line.length > 0) ? config.program_meta (program_line[0], "thumb") : "";
		final String e2_thumb = (program_line != null && program_line.length > 1) ? config.program_meta (program_line[1], "thumb") : "";
		final String e3_thumb = (program_line != null && program_line.length > 2) ? config.program_meta (program_line[2], "thumb") : "";
		
		final String e1_name = (program_line != null && program_line.length > 0) ? config.program_meta (program_line[0], "name") : "";
		final String e2_name = (program_line != null && program_line.length > 1) ? config.program_meta (program_line[1], "name") : "";
		final String e3_name = (program_line != null && program_line.length > 2) ? config.program_meta (program_line[2], "name") : "";

		if (e1_thumb != null && e1_thumb.startsWith ("http"))
			config.set_channel_meta_by_id (real_channel, "episode_thumb_1", e1_thumb);
				
		// channelSubmit response:
		// 0	SUCCESS
		// I/vtest   (15411): --
		// I/vtest   (15411): 32604	Test of FAITH	https://lh4.googleusercontent.com/-IsaoVr_-NPU/AAAAAAAAAAI/AAAAAAAAAAA/dBRQPKmAthM/photo.jpg	3	

		final int server_position = config.client_to_server (position);
	
		log ("channelSubmit url=" + url);
		log ("channelSubmit position=" + position + ", grid=" + server_position);
		log ("channelSubmit name=" + channel_name);
		
		Thread t = new Thread ()
			{
			@Override
			public void run()
				{
			    HttpClient httpclient = new DefaultHttpClient();
			    HttpPost httppost = new HttpPost ("http://" + config.api_server + "/playerAPI/channelSubmit");

				ResponseHandler <String> response_handler = new BasicResponseHandler();

		        List <NameValuePair> kv = new ArrayList <NameValuePair> (7);
		        kv.add (new BasicNameValuePair ("mso", config.mso));
		        kv.add (new BasicNameValuePair ("url", url));
		        kv.add (new BasicNameValuePair ("grid", "" + server_position));
		        kv.add (new BasicNameValuePair ("langCode", "" + config.region));
		        kv.add (new BasicNameValuePair ("user", config.usertoken));
		        kv.add (new BasicNameValuePair ("name", channel_name + "|" + e1_name + "|" + e2_name + "|" + e3_name));
		        kv.add (new BasicNameValuePair ("image", channel_image + "|" + e1_thumb + "|" + e2_thumb + "|" + e3_thumb));

				try
			    	{
			        httppost.setEntity (new UrlEncodedFormEntity (kv));
			        String response = httpclient.execute (httppost, response_handler);			        
			        Log.i ("vtest", "channelSubmit response: " + response);
			        String lines[] = response.split ("\n");
			        if (lines.length >= 3)
			        	{
			        	String fields[] = lines[0].split ("\t");
			        	if (fields[0].equals ("0"))
			        		{
							subscription_changes_this_session++;
			        		/* first do a little dance to rename this fake channel */
			        		fields = lines[2].split ("\t");
			        		final String new_channel_id = fields[0];
			        		log ("new channel id is: " + new_channel_id + ", position is: " + position);
			        		config.copy_channel (real_channel, new_channel_id);
			        		player_real_channel = new_channel_id;
			        		if (playback_episode_pager != null)
			        			playback_episode_pager.set_channel_id_only (new_channel_id);
			        		
			        		/* perform actual subscription on main thread */
			        		in_main_thread.post (new Runnable()
			        			{
								@Override
								public void run()
									{
									toast_by_resource (R.string.following_yay);
									config.place_in_channel_grid (new_channel_id, position, true);
									config.set_channel_meta_by_id (new_channel_id, "subscribed", "1");						
									String youtube_username = config.pool_meta (new_channel_id, "extra");
									ytchannel.subscribe_on_youtube (config, youtube_username);
									config.subscriptions_altered = config.grid_update_required = true;
									track_event ("function", "follow", "follow", 0, new_channel_id);
									update_layer_after_subscribe (new_channel_id);
									thumbnail.stack_thumbs (main.this, config, new String[] { new_channel_id }, -1, null, null);
									}			        			
			        			});

			        		}
			        	}
			    	}
			    catch (Exception ex)
			    	{
			        Log.i ("vtest", "channelSubmit failure");
			        ex.printStackTrace();
			    	}
			
				/* UI change must be done on the main thread */
				in_main_thread.post (new Runnable()
					{
					@Override
					public void run()
						{
					    make_follow_icon_normal (v);
						}					
					});
				}
			};

		t.start();
		}
	
	public View home_layer()
		{
		// return findViewById (is_tablet() ? R.id.home_tablet : R.id.home_layer);
		return findViewById (R.id.home_layer);
		}
	
	public void set_layer (toplayer layer)
		{
		log ("set layer: " + layer.toString());
		
		/* this is an overlay but will always go back to settings */
		View password_layer = findViewById (is_phone() ? R.id.passwordlayer_phone : R.id.passwordlayer_tablet);
		password_layer.setVisibility (layer == toplayer.PASSWORD ? View.VISIBLE : View.GONE);
		
		if (is_tablet() && (layer == toplayer.SETTINGS || layer == toplayer.SIGNIN || layer == toplayer.PASSWORD))
			{
			View settings_layer = findViewById (R.id.settingslayer_tablet);
			settings_layer.setVisibility (layer == toplayer.SETTINGS ? View.VISIBLE : View.GONE);
			
			View signin_layer = findViewById (R.id.signinlayer_tablet);
			signin_layer.setVisibility (layer == toplayer.SIGNIN ? View.VISIBLE : View.GONE);
			
			return;
			}	
		
		View vTopBar = findViewById (R.id.sliding_top_bar);
		vTopBar.setVisibility (layer == toplayer.TERMS ? View.GONE : View.VISIBLE);
		
		View home_layer = home_layer();
		home_layer.setVisibility (layer == toplayer.HOME ? View.VISIBLE : View.GONE);

		View guide_layer = findViewById (R.id.guidelayer);
		guide_layer.setVisibility (layer == toplayer.GUIDE ? View.VISIBLE : View.GONE);

		View store_layer = findViewById (R.id.storelayer);
		store_layer.setVisibility (layer == toplayer.STORE ? View.VISIBLE : View.GONE);

		View search_layer = findViewById (R.id.searchlayer);
		search_layer.setVisibility (layer == toplayer.SEARCH ? View.VISIBLE : View.GONE);	
		
		View settings_layer = findViewById (is_phone() ? R.id.settingslayer_phone : R.id.settingslayer_tablet);
		settings_layer.setVisibility (layer == toplayer.SETTINGS ? View.VISIBLE : View.GONE);
		
		View terms_layer = findViewById (R.id.termslayer_new);
		terms_layer.setVisibility (layer == toplayer.TERMS ? View.VISIBLE : View.GONE);
		
		View signin_layer = findViewById (is_phone() ? R.id.signinlayer_phone : R.id.signinlayer_tablet); // TODO FIX
		if (signin_layer != null)
			signin_layer.setVisibility (layer == toplayer.SIGNIN ? View.VISIBLE : View.GONE);
		
		View apps_layer = findViewById (R.id.appslayer);
		apps_layer.setVisibility (layer == toplayer.APPS ? View.VISIBLE : View.GONE);

		View messages_layer = findViewById (R.id.messagelayer);
		messages_layer.setVisibility (layer == toplayer.MESSAGES ? View.VISIBLE : View.GONE);
		
		View about_layer = findViewById (R.id.aboutlayer);
		about_layer.setVisibility (layer == toplayer.ABOUT ? View.VISIBLE : View.GONE);
		
		View shake_layer = findViewById (R.id.shakelayer);
		shake_layer.setVisibility (layer == toplayer.SHAKE ? View.VISIBLE : View.GONE);

		View nag_layer = findViewById (R.id.nag_layer);
		nag_layer.setVisibility (layer == toplayer.NAG ? View.VISIBLE : View.GONE);
		
		View test_layer = findViewById (R.id.testlayer);
		test_layer.setVisibility (layer == toplayer.TEST ? View.VISIBLE : View.GONE);
		
		View social_layer = findViewById (R.id.sociallayer);
		social_layer.setVisibility (layer == toplayer.SOCIAL ? View.VISIBLE : View.GONE);
		
		// View advert_layer = findViewById (R.id.direct_ad_layer);
		// advert_layer.setVisibility (layer == toplayer.ADVERT ? View.VISIBLE : View.GONE);
		
		current_layer = layer;
		
		redraw_menu();
		}
	
	/*** SIGNIN ************************************************************************************************/
	
	public void enable_signin_layer (Runnable callback)
		{
		disable_video_layer();
		
		/* terms layer can only be started from signin, so ignore it */
		if (current_layer != toplayer.TERMS && current_layer != toplayer.SIGNIN && current_layer != toplayer.NAG)
			layer_before_signin = current_layer;
		
		set_layer (toplayer.SIGNIN);		
		
		setup_signin_buttons (callback);
		
		sign_in_tab();
		
		track_layer (toplayer.SIGNIN);
		}

	public void setup_signin_buttons (final Runnable callback)
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
			        	if (vEditable != null)
			        		{
			        		vEditable.requestFocusFromTouch();
				        	InputMethodManager imm = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE); 
				            imm.showSoftInput (vEditable, 0);
			        		}
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
							zero_signin_data();
							activate_layer (layer_before_signin);
							if (callback != null)
								callback.run();
							}		        	
			        	});
		        	}
				});	
		
		View vBanner = findViewById (R.id.signin_banner);
		if (vBanner != null)
			vBanner.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin banner");
					zero_signin_data();
					activate_layer (layer_before_signin);
					if (callback != null)
						callback.run();
		        	}
				});
		
		
		ImageView vAppIcon = (ImageView) findViewById (R.id.signin_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
		
		View vSignin = findViewById (R.id.sign_in_button);
		if (vSignin != null)
			vSignin.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin");
		        	proceed_with_signin (callback);
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
		        	proceed_with_signup (callback);
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
		        	log ("click on: signup tab");
		        	sign_up_tab();
		        	}
				});

		TextView vTermsButton = (TextView) findViewById (R.id.terms_button);
		if (vTermsButton != null)
			{
			vTermsButton.setPaintFlags (vTermsButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			vTermsButton.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: terms");
		        	slide_in_terms();
		        	}
				});
			}
		TextView vPrivacyButton = (TextView) findViewById (R.id.privacy_button);
		if (vPrivacyButton != null)
			{
			vPrivacyButton.setPaintFlags (vPrivacyButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			vPrivacyButton.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: privacy");
		        	slide_in_privacy();
		        	}
				});
			}
		
		if (!is_phone())
			{
			View vSignInChoice = findViewById (R.id.sign_in_choice);
			if (vSignInChoice != null)
				vSignInChoice.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: signin choice");
			        	sign_in_tab();
			        	}
					});
			
			View vSignUpChoice = findViewById (R.id.sign_up_choice);
			if (vSignUpChoice != null)
				vSignUpChoice.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: signup choice");
			        	sign_up_tab();
			        	}
					});
			
			View vCancel = findViewById (R.id.signin_signup_cancel);
			if (vCancel != null)
				vCancel.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: signin cancel");
			        	}
					});
			
			View vSigninSignup = findViewById (R.id.signin_signup_button);
			if (vSigninSignup != null)
				vSigninSignup.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: signin/signup");
			        	View vSignUpContent = findViewById (R.id.sign_up_content);
			        	if (vSignUpContent.getVisibility() == View.VISIBLE) 
			        		proceed_with_signup (callback);
			        	else
			        		proceed_with_signin (callback);
			        	}
					});			
			}
		
		View vSigninLayer = findViewById (R.id.signinlayer);
		if (vSigninLayer != null)
			vSigninLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("signin layer ate my tap!");
		        	}
				});
		
		View vMain = findViewById (R.id.main);
		fezbuk2 (vMain);
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
		int white = Color.WHITE;
		int gray = Color.rgb (0x77, 0x77, 0x77);
		int yellow = Color.rgb (0xFF, 0xAA, 0x00);
		int dark = Color.rgb (0x1D, 0x1D, 0x1D);
		
		View vSignInContent = findViewById (R.id.sign_in_content);
		View vSignUpContent = findViewById (R.id.sign_up_content);	
		
		int not_visible = is_tablet() ? View.INVISIBLE : View.GONE;
		not_visible = View.GONE; // TODO FIX!!!
		
		vSignInContent.setVisibility (is_sign_in ? View.VISIBLE : not_visible);
		vSignUpContent.setVisibility (is_sign_in ? not_visible : View.VISIBLE);
		
		TextView vSignInTabText = (TextView) findViewById (R.id.sign_in_tab_text);
		TextView vSignUpTabText = (TextView) findViewById (R.id.sign_up_tab_text);
		vSignInTabText.setTextColor (is_sign_in ? white : gray);
		vSignUpTabText.setTextColor (is_sign_in ? gray : white);
		
		View vSignInTabBar = findViewById (R.id.sign_in_tab_bar);
		View vSignUpTabBar = findViewById (R.id.sign_up_tab_bar);
		
		vSignInTabBar.setBackgroundColor (is_sign_in ? yellow : dark);
		vSignUpTabBar.setBackgroundColor (is_sign_in ? dark : yellow);
		}
	
	public void zero_signin_data()
		{
		/* and settings */
		int editables[] = { R.id.sign_in_email, R.id.sign_in_password, R.id.sign_up_name, R.id.sign_up_email,
				R.id.sign_up_password, R.id.sign_up_verify, R.id.settings_old_password, R.id.settings_new_password, R.id.settings_verify_password };
		for (int editable: editables)
			{
			EditText v = (EditText) findViewById (editable);
			if (v != null)
				v.setText ("");
			}
		
		}
	
	public void proceed_with_signin (final Runnable callback)
		{
		EditText emailView = (EditText) findViewById (R.id.sign_in_email);
		final String email = emailView.getText().toString();
	
		/* use any view to turn off soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow (emailView.getApplicationWindowToken(), 0);
	    
		EditText passwordView = (EditText) findViewById (R.id.sign_in_password);
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
				track_event ("signIn", "signInWithEmail", "signInWithEmail", 0);
	        	toggle_menu (new Callback()
		        	{
					@Override
					public void run()
						{
						zero_signin_data();
						/* the settings view might be in the slider */
						redraw_settings();
						activate_layer (layer_before_signin);
						if (callback != null)
							callback.run();
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
	
	public void proceed_with_signup (final Runnable callback)
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
				track_event ("signUp", "signUpWithEmail", "signUpWithEmail", 0);
	        	toggle_menu (new Callback()
		        	{
					@Override
					public void run()
						{
						zero_signin_data();
						/* the settings view might be in the slider */
						redraw_settings();
						activate_layer (layer_before_signin);
						if (callback != null)
							callback.run();
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
			{
			if (current_layer == toplayer.SIGNIN)
				track_event ("signIn", "signInWithFB", "signInWithFB", 0);
			email = config.email = "[via Facebook]";
			}
		else if (email != null)
			config.email = email;
		
		futil.write_file (main.this, "user@" + config.api_server, token);
		
		if (name != null)
			futil.write_file (main.this, "name@" + config.api_server, name);
		
		if (email != null)
			futil.write_file (main.this, "email@" + config.api_server, email);
		
		zero_signin_data();
		/* the settings view might be in the slider */
		redraw_settings();
		}	

	/*** TERMS *****************************************************************************************************/
	
	toplayer terms_previous_layer;
	
	public void enable_terms_layer()
		{
		disable_video_layer();
		
		terms_previous_layer = current_layer;
		set_layer (toplayer.TERMS);
		
		setup_terms_buttons();
		terms_tab();
		
		/* sometimes the terms layer background is not redrawing! force it here */
		View vTermsLayer = findViewById (R.id.termslayer_new);
		vTermsLayer.postInvalidate();
		
		/* and even then that doesn't always work! Also the only purpose a "postInvalidateDelayed" method
		   can possibly have is as workarounds for Android layout bugs exactly like this */		
		for (int i: new int[] { 500, 1000, 5000 })
			vTermsLayer.postInvalidateDelayed (i);;
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
		
		ImageView vAppIcon = (ImageView) findViewById (R.id.terms_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
		
		View vTermsLayer = findViewById (R.id.termslayer_new);
		if (vTermsLayer != null)
			vTermsLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("terms layer ate my tap!");
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
	    		terms_tab();
	    		toggle_menu();
	    		}
	    	});
		}
	
	public void slide_in_privacy()
		{
		toggle_menu (new Callback()
	    	{
	    	public void run()
	    		{
	    		enable_terms_layer();
	    		privacy_tab();
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
	    		if (is_tablet())
	    			{
	    			/* set the background layer. on tablets, signin is an overlay */
	    			set_layer (terms_previous_layer);
	    			}
	    		enable_signin_layer (null);
	    		sign_up_tab();
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
		
		String txt_privacy = getResources().getString (R.string.privacypolicy);
		String txt_terms = getResources().getString (R.string.terms_of_service);
		
		TextView vBanner = (TextView) findViewById (R.id.terms_banner_text);
		vBanner.setText (is_terms ? txt_terms : txt_privacy);
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
		
		log ("terms/privacy url: " + url);
		vWebPage.loadUrl (url);
		}

	/*** FACEBOOK **************************************************************************************************/
	
	boolean signed_in_with_facebook = false;
	
	private Session session = null;
	private String access_token = "";
	
	public GraphUser user = null;
	
    private UiLifecycleHelper uiHelper = null;
    
	/* this formerly worked. After an update (Android? Facebook?) it has broken! */
	public boolean OLD_has_facebook()
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
	
	public boolean has_facebook()
		{
		/* can something on the system handle a fb:// intent? */
        final String urlFb = "fb://page/192338590805862";
        Intent intent = new Intent (Intent.ACTION_VIEW);
        intent.setData (Uri.parse (urlFb));
        final PackageManager packageManager = getPackageManager();
        List <ResolveInfo> list = packageManager.queryIntentActivities (intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (list.size() > 0);
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
		if (config.facebook_app_id != null)
			{
			session = new Session.Builder (this).setApplicationId (config.facebook_app_id).build();
	        uiHelper = new UiLifecycleHelper (this, fb_callback);
	        if (uiHelper != null)
	        	uiHelper.onCreate (savedInstanceState);
			}
		}
	
	public void fezbuk2 (View parent)
		{
		for (final int button: new int[] { R.id.fblogin, R.id.nag2_fblogin, R.id.nag3_fblogin })
			{
			LoginButton vButton = (LoginButton) parent.findViewById (button);
		    if (vButton != null)
			    {
		    	log ("enabling facebook login ui button: " + button);
		    	vButton.setReadPermissions (Arrays.asList ("email", "user_location", "user_birthday"));
		    	vButton.setUserInfoChangedCallback (new LoginButton.UserInfoChangedCallback()
			    	{
			        @Override
			        public void onUserInfoFetched (final GraphUser user)
			        	{
			        	log ("FACEBOOK LOGIN BUTTON CALLBACK!");
			        	if (button == R.id.nag2_fblogin || button == R.id.nag3_fblogin)
			        		{
			        		/* this only tracks if login is successful, which isn't 100% what PM requests, but is the best we can do */
			        		track_event ("signIn", "signInWithFB-enforce", "signInWithFB-enforce", 0);
			        		}
			        	process_fb_user (user);
			        	}
			    	});
			    }
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
		        	toggle_menu (new Callback()
			        	{
						@Override
						public void run()
							{
							if (current_layer == toplayer.NAG)
								enable_home_layer();
							else
								activate_layer (layer_before_signin);
							}		        	
			        	});
					}
				public void failure (int code, String errtext)
					{
					// String txt_failure = getResources().getString (R.string.login_failure);
					// alert (txt_failure + ": " + errtext);
					alert ("FACEBOOK LOGIN FAILURE!");
					}
				};
	    	}
	    else
	    	{
	    	log ("fell through: enable home layer");
			enable_home_layer();
	    	}
		}
	
	@Override
	public void signout()
		{
		super.signout();
		zero_signin_data();
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

	/*** APPS **************************************************************************************************/
	
	public void enable_apps_layer()
		{
		disable_video_layer();
		set_layer (toplayer.APPS);
		init_apps();
		setup_apps_buttons();
		track_layer (toplayer.APPS);
		}
	
	public void setup_apps_buttons()
		{
		AbsListView vAppsList = (AbsListView) findViewById (is_phone() ? R.id.apps_list_phone : R.id.apps_list_tablet);				
		vAppsList.setOnItemClickListener (new OnItemClickListener()
			{
			public void onItemClick (AdapterView parent, View v, int position, long id)
				{
				log ("onItemClick: " + position);
				if (position < apps.length)
					{
					log ("app list click: " + position);
					launch_suggested_app (apps [position].title, apps [position].market_url);
					}
				}
			});
		
		View vAppsLayer = findViewById (R.id.appslayer);
		if (vAppsLayer != null)
			vAppsLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat these */
		        	log ("apps layer ate my tap!");
		        	}
				});		
		}

	public class app
		{
		String title;
		String description;
		String icon_url;
		String market_url;
		String basename;
		}
	
	app apps[] = null;
	app recommended_apps[] = null;
	
    AppsAdapter apps_adapter = null;
    
	public void init_apps()
		{
		if (apps == null)
			{
			new playerAPI (in_main_thread, config, "relatedApps?os=android&sphere=" + config.region)
				{
				public void success (String[] lines)
					{
					int section = 0, count0 = 0, count1 = 0;
					for (int i = 0; i < lines.length; i++)
						{
						if (lines[i].equals ("--"))
							section++;
						else if (section == 0)
							count0++;
						else if (section == 1)
							count1++;
						}
							
					app new_recommended_apps[] = new app [count0];
					app new_apps[] = new app [count1];

					section = 0;
					count0 = 0;
					count1 = 0;
					
					Pattern pattern = Pattern.compile ("id=([^&]*)", Pattern.CASE_INSENSITIVE);
					
					for (int i = 0; i < lines.length; i++)
						{
						if (lines[i].equals ("--"))
							section++;
						else if (section == 0 || section == 1)
							{
							app a = new app();
							String fields[] = lines[i].split ("\t");
							a.title = fields.length > 0 ? fields[0] : "";
							a.description = fields.length > 1 ? fields[1] : "";
							a.icon_url = fields.length > 2 ? fields[2] : "";
							a.market_url = fields.length > 3 ? fields[3] : "";
							/* market://details?id=tv.ddtv.player9x9tv&hl=en */
							Matcher m = pattern.matcher (a.market_url);
							if (m.find())
								a.basename = m.group (1);
							if (section == 0)
								new_recommended_apps [count0++] = a;
							if (section == 1)
								new_apps [count1++] = a;
							log ("app: " + a.market_url);
							}
						}
					
					recommended_apps = new_recommended_apps;
					apps = new_apps;
		
					if (menu_adapter != null)
						redraw_menu();
					
					View vPhone = findViewById (R.id.apps_list_phone);
					vPhone.setVisibility (is_phone() ? View.VISIBLE : View.GONE);
					
					View vTablet = findViewById (R.id.apps_list_tablet);
					vTablet.setVisibility (is_phone() ? View.GONE : View.VISIBLE);
					
					AbsListView vAppsList = (AbsListView) findViewById (is_phone() ? R.id.apps_list_phone : R.id.apps_list_tablet);
					apps_adapter = new AppsAdapter (main.this, apps);
					vAppsList.setAdapter (apps_adapter);
					
					setup_apps_buttons();
					download_app_thumbs();
					}
				public void failure (int code, String errtext)
					{
					}
				};
			}
		else
			{
			if (apps_adapter != null)
				apps_adapter.notifyDataSetChanged();
			}
		}
	
	public void download_app_thumbs()
		{			
		Runnable apps_thumberino = new Runnable()
			{
			public void run()
				{
				if (apps_adapter != null)
					apps_adapter.notifyDataSetChanged();
				if (menu_adapter != null && apps_expanded)
					menu_adapter.notifyDataSetChanged();
				}
			};

		Stack <String> filenames = new Stack <String> ();
		Stack <String> urls = new Stack <String> ();

		for (app a: apps)
			{
			filenames.push (a.basename);
			urls.push (a.icon_url);
			}
		
		thumbnail.download_list_of_images (main.this, config, "apps", filenames, urls, true, in_main_thread, apps_thumberino);
		}
	
	public void launch_suggested_app (String name, String url)
		{
		if (url != null && !url.equals (""))
			{
			Intent intent = new Intent (Intent.ACTION_VIEW, Uri.parse (url));
			startActivity (intent);
			track_event ("install", "toDownload-others", name, 0);
			}
		}
	
	public class AppsAdapter extends BaseAdapter
		{
		private Context context;
		private app apps[] = null;
		
		public AppsAdapter (Context context, app apps[])
			{
			this.context = context;
			this.apps = apps;
			}

		@Override
		public int getCount()
			{			
			log ("getcount: " + apps.length);
			return apps == null ? 0 : apps.length;
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
					
			log ("apps getView: " + position + " (of " + getCount() + ")");
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (main.this, R.layout.app_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (apps [position].title);
			
			TextView vDesc = (TextView) rv.findViewById (R.id.desc);
			if (vDesc != null)
				vDesc.setText (apps [position].description);

			boolean icon_found = false;
			
			ImageView vIcon = (ImageView) rv.findViewById (R.id.icon); 	
			if (vIcon != null)
				{
				String filename = getFilesDir() + "/" + config.api_server + "/apps/" + apps [position].basename + ".png";
				
				File f = new File (filename);
				if (f.exists ())
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						icon_found = true;
						vIcon.setImageBitmap (bitmap);
						}
					}
				}
			
			View vBottomBar = rv.findViewById (R.id.bottom_bar);
			vBottomBar.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);
			
			String txt_download = getResources().getString (R.string.download);
			String txt_coming_soon = getResources().getString (R.string.coming_soon);
			
			TextView vDownload = (TextView) rv.findViewById (R.id.download_button);
			if (vDownload != null)
				{
				if (apps [position].market_url == null || apps [position].market_url.equals(""))
					vDownload.setText (txt_coming_soon);
				else
					vDownload.setText (txt_download);
				}
			
			return rv;
			}	
		}	

	
	
	
	
	
	
	
	
	
	
	/*** NAG **************************************************************************************************/
	
	NagSlider nag_slider = null;
	
	public void enable_nag_layer()
		{
		disable_video_layer();
		set_layer (toplayer.NAG);
		
		layer_before_signin = toplayer.HOME;
		
        nag_slider = new NagSlider();
        StoppableViewPager vHomePager = (StoppableViewPager) findViewById (R.id.nagpager);
        vHomePager.setAdapter (nag_slider);
		}
	
	class Swapnag
		{
		int page_number = 0;
		LinearLayout page = null;
		public Swapnag (int page_number)
			{
			this.page_number = page_number;
			}
		};

	/* this is implemented using the base class! */
		
	public class NagSlider extends PagerAdapter
		{		
	    @Override
	    public int getCount()
	    	{
	        return 3;
	    	}
	
		@Override
		public boolean isViewFromObject (View view, Object object)
			{
			return (((Swapnag) object).page) == (LinearLayout) view;
			}
		
		@Override
		public Object instantiateItem (final ViewGroup container, final int position)
			{
			log ("[NAG] instantiate: " + position);
			
			final Swapnag sh = new Swapnag (position);			
	
			int layout_resource = 0;
			switch (position)
				{
				case 0: layout_resource = R.layout.nag1; break;
				case 1: layout_resource = R.layout.nag2; break;
				case 2: layout_resource = R.layout.nag3; break;				
				}
			
			LinearLayout page = (LinearLayout) View.inflate (main.this, layout_resource, null);
			sh.page = page;
			
			((StoppableViewPager) container).addView (page, 0);
			
			String signin_logo = getResources().getString (R.string.signin_logo);
			if (signin_logo != null)
				{
				int logo_id = getResources().getIdentifier (signin_logo, "drawable", getPackageName());
				
				ImageView vLogo = (ImageView) page.findViewById (R.id.nag_logo);
				if (vLogo != null)
					vLogo.setImageResource (logo_id);
				}
			
			TextView vSkip = (TextView) page.findViewById (R.id.skip_this_step);
			if (vSkip != null)
				{
				vSkip.setPaintFlags (Paint.UNDERLINE_TEXT_FLAG);
				vSkip.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: nag skip");
			        	enable_home_layer();
			        	}
					});	
				}
			
			TextView vSignUp = (TextView) page.findViewById (R.id.nag3_sign_up);
			if (vSignUp != null)
				{
				vSignUp.setPaintFlags (Paint.UNDERLINE_TEXT_FLAG);
				vSignUp.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: nag sign up");
						track_event ("signIn", "signInWithEmail-enforce", "signInWithEmail-enforce", 0);
		        		enable_signin_layer (new Runnable()
			    			{
			        		@Override
			        		public void run()
				        		{
			        			enable_home_layer();
				        		}
			    			});
			        	}
					});	
				}
			
			/* disable facebook buttons if there is no facebook app on this device */
			if (!has_facebook())
				{
				log ("no facebook, disabling buttons");
				for (int button: new int[] { R.id.nag2_fblogin, R.id.nag3_fblogin })
					{
					LoginButton vButton = (LoginButton) page.findViewById (button);
				    if (vButton != null)
					    vButton.setVisibility (View.GONE);
					}
				}
			else
				fezbuk2 (page);
			
			return sh;
			}
		
		@Override
		public void destroyItem (ViewGroup container, int position, Object object)
			{
			log ("[NAG] destroy: " + position);
			Swapnag sh = (Swapnag) object;
			((StoppableViewPager) container).removeView (sh.page);
			}
		
		@Override
		public void setPrimaryItem (ViewGroup container, int position, Object object)
			{
			Swapnag sh = (Swapnag) object;
			ImageView vDot1 = (ImageView) findViewById (R.id.nag_dot1);
			ImageView vDot2 = (ImageView) findViewById (R.id.nag_dot2);
			ImageView vDot3 = (ImageView) findViewById (R.id.nag_dot3);			
			vDot1.setImageResource (position == 0 ? R.drawable.walkthrough_dot_active : R.drawable.walkthrough_dot);
			vDot2.setImageResource (position == 1 ? R.drawable.walkthrough_dot_active : R.drawable.walkthrough_dot);
			vDot3.setImageResource (position == 2 ? R.drawable.walkthrough_dot_active : R.drawable.walkthrough_dot);			
			}
		}   

	/*** HOME **************************************************************************************************/

	LineItemAdapter channel_overlay_adapter = null;

	HomeSlider home_slider = null;
	StoppableViewPager vHomePager = null;
	
	public void enable_home_layer()
		{
		disable_video_layer();
		set_layer (toplayer.HOME);
		
		if (portal_stack_ids != null)
			{
			reset_arena_to_home();
			
			if (home_slider == null)
				{
				log ("+++++++++++++++++++++++++++++++ new HomeSlider +++++++++++++++++++++++++++++++++++");
		        home_slider = new HomeSlider();
		        vHomePager = (StoppableViewPager) home_layer().findViewById (R.id.homepager);
		        vHomePager.setAdapter (home_slider);
		        if (portal_stack_ids.length > 1)
		        	{
		        	int unique_sets = portal_stack_ids.length / 100;
		        	int start_set = unique_sets * 50;
		        	log ("start set: " + start_set);
		        	vHomePager.setCurrentItem (start_set);
		        	}
				}			
			
			create_set_slider();
			position_set_slider();
			}
		
		track_layer (toplayer.HOME);
		
		bouncy_home_hint_animation();
		}
	
	public void bouncy_home_hint_animation()
		{
		final View vHint = findViewById (R.id.home_swipe_hint);
		
		AnimatorSet as = new AnimatorSet();
		
		ValueAnimator animH1 = ValueAnimator.ofInt (pixels_100, pixels_150);
		ValueAnimator animH2 = ValueAnimator.ofInt (pixels_150, pixels_120);		
		ValueAnimator animH3 = ValueAnimator.ofInt (pixels_150, pixels_100);
		
		int dy = is_phone() ? pixels_20 : pixels_30;
		
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

	public void bouncy_playback_hint_animation()
		{
		final View vCircle = findViewById (R.id.follow_hint_circle);
		
		final View vHint = findViewById (R.id.playback_follow_hint);
		final View vContainer = findViewById (R.id.playback_follow_hint_container);
		
		final View vHoriz = findViewById (R.id.playback_horiz);
		
		final int bottom_base = vHoriz.getVisibility() == View.VISIBLE ? vHoriz.getHeight() : 0;
		
		final FrameLayout.LayoutParams container_layout = (FrameLayout.LayoutParams) vContainer.getLayoutParams();
		container_layout.bottomMargin = bottom_base;
		vContainer.setLayoutParams (container_layout);
		
		vContainer.setVisibility (View.VISIBLE);
		vCircle.setVisibility (View.VISIBLE);
        
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) vHint.getLayoutParams();
        layoutParams.bottomMargin = -pixels_25;
        vHint.setLayoutParams (layoutParams);
        
		AnimatorSet as = new AnimatorSet();
		
		int dy = pixels_5;
		
		ValueAnimator halfUP    = ValueAnimator.ofInt (  0, +dy);
		ValueAnimator halfDOWN  = ValueAnimator.ofInt (+dy,   0);
		ValueAnimator fullUP1   = ValueAnimator.ofInt (-dy, +dy);
		ValueAnimator fullDOWN1 = ValueAnimator.ofInt (+dy, -dy);
		
		ValueAnimator fullUP2   = ValueAnimator.ofInt (-dy, +dy);
		ValueAnimator fullDOWN2 = ValueAnimator.ofInt (+dy, -dy);
		
		ValueAnimator.AnimatorUpdateListener listenerH = new ValueAnimator.AnimatorUpdateListener()
	    	{
	        @Override
	        public void onAnimationUpdate (ValueAnimator valueAnimator)
	        	{
	            int val = (Integer) valueAnimator.getAnimatedValue();
	            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) vHint.getLayoutParams();
	            layoutParams.height = val;
	            layoutParams.width = val;
	            vHint.setLayoutParams (layoutParams);
	        	}
	    	};
	
		ValueAnimator.AnimatorUpdateListener listenerM = new ValueAnimator.AnimatorUpdateListener()
	    	{
	        @Override
	        public void onAnimationUpdate (ValueAnimator valueAnimator)
	        	{
	            int val = (Integer) valueAnimator.getAnimatedValue();
	            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) vHint.getLayoutParams();
	            layoutParams.bottomMargin = -pixels_25 + val;
	            final FrameLayout.LayoutParams lp = layoutParams;
	            vHint.setLayoutParams(lp);
	        	}
	    	};
	    
	    halfUP.addUpdateListener (listenerM);
	    halfDOWN.addUpdateListener (listenerM);
	    fullUP1.addUpdateListener (listenerM);
	    fullDOWN1.addUpdateListener (listenerM);
	    fullUP2.addUpdateListener (listenerM);
	    fullDOWN2.addUpdateListener (listenerM);
	    
		ObjectAnimator animFI = ObjectAnimator.ofFloat (vHint, "alpha", 0.0f, 1.0f);
		ObjectAnimator animFO = ObjectAnimator.ofFloat (vHint, "alpha", 1.0f, 0.0f);			
	
		animFI.setDuration (300);		
		animFO.setDuration (300);
		halfUP.setDuration (400);
		halfDOWN.setDuration (400);
		fullUP1.setDuration (800);
		fullDOWN1.setDuration (800);
		fullUP2.setDuration (800);
		fullDOWN2.setDuration (800);
		
	    as.play(animFI);
	    as.play(halfUP).after(animFI);
	    as.play(fullDOWN1).after(halfUP);
	    as.play(fullUP1).after(fullDOWN1);
	    as.play(fullDOWN2).after(fullUP1);
	    as.play(fullUP2).after(fullDOWN2);
	    as.play(halfDOWN).after(fullUP2);	    
	    as.play(animFO).after(halfDOWN);
	     
	    int total_duration = 300 + 400 + 800 + 800 + 800 + 800 + 400 + 300;
	    
		as.start();
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				vCircle.setVisibility (View.GONE);
				}
			}, total_duration);
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				vContainer.setVisibility (View.GONE);
				}
			}, total_duration + 300);
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
			sh.subscription_change_count = subscription_changes_this_session;
			
			FrameLayout home_page = (FrameLayout) View.inflate (main.this, R.layout.home_page, null);
			
			View vTabletPreamble = home_page.findViewById (R.id.tablet_preamble);
			vTabletPreamble.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);

			if (is_tablet())
				{
				boolean banner_found = false;
				ImageView vSetBanner = (ImageView) home_page.findViewById (R.id.set_banner);
				
				if (vSetBanner != null)
					{
					String filename = getFilesDir() + "/" + config.api_server + "/bthumbs/" + sh.set_id + ".png";				
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
						int set_banner_id = getResources().getIdentifier (set_banner, "drawable", getPackageName());
						vSetBanner.setBackgroundResource (set_banner_id);
						}
					}
				}
			
			if (is_tablet())
				{
				View vChannelList = home_page.findViewById (R.id.channel_list);
				FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vChannelList.getLayoutParams();
				layout.leftMargin = 0;
				layout.rightMargin = 0;
				vChannelList.setLayoutParams (layout);
				}
			
			if (is_tablet() && sh.channel_adapter != null)
				set_mini_mode_thumbs (sh.home_page);
			
			diminish_side_titles (home_page, true);
			
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
			log ("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% primary 3x3: " + position);
			diminish_side_titles (current_home_page, true);
			current_swap_object = sh;
			if (sh != null)
				{
				current_home_page = sh.home_page;
				diminish_side_titles (current_home_page, false);
	
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
					if (subscription_changes_this_session != sh.subscription_change_count)
						{
						/* try to minimize expensive calls to notifyDataSetChanged */
						sh.subscription_change_count = subscription_changes_this_session;
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
			if (is_tablet())
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
			
			query_pile (portal_stack_ids [sh.set], new Callback()
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
						sh.vChannels.set_refresh_function (in_main_thread, new Runnable()
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
								return finger_is_down;
								}
							};
						sh.vChannels.set_finger_is_down_function (c);

			    		LayoutInflater inflater = main.this.getLayoutInflater();
			    		if (!sh.shim_added)
				    		{
				    		View shim = inflater.inflate (R.layout.footer_shim, null);
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
					thumbnail.stack_thumbs (main.this, config, sh.arena, -1, in_main_thread, channel_thumberino);
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
		
	boolean have_set_positions = false;
	int set_offsets[] = null;
	int set_widths[] = null;
	
	public void create_set_slider()
		{	
		have_set_positions = false;
		
		final LinearLayout vSetSlider = (LinearLayout) findViewById (R.id.set_slider);
		
		for (int i = 0; i < portal_stack_names.length; i++)
			{
			TextView vText = new TextView (this);
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
			vText.setPadding (pixels_20, 0, pixels_20, 0);
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
					
				    final byte DEFAULT_DIVIDER_COLOR_ALPHA = 0x20;
				    final int themeForegroundColor = Color.rgb (0xFF, 0xFF, 0x00);
				    
					// colorizer.setDividerColors (setColorAlpha (themeForegroundColor, DEFAULT_DIVIDER_COLOR_ALPHA));
			        // colorizer.setDividerColors (Color.argb(0x20, 0xFF, 0xFF, 0x00));
				    colorizer.setDividerColors (Color.argb (0xFF, 0xFF, 0xFFD, 0x0F));
					SlidingTabLayout mSlidingTabLayout = (SlidingTabLayout) findViewById (R.id.sliding_tabs);
					mSlidingTabLayout.setCustomTabColorizer (colorizer);
					mSlidingTabLayout.setViewPager (vHomePager);
					}
				}	
			});
		}
	
	public void position_set_slider()
		{	
		final View vIndicator = findViewById (R.id.set_indicator);
		if (have_set_positions && current_swap_object != null)
			{
			vIndicator.post(new Runnable()
				{
				@Override
				public void run()
					{
					int set = current_swap_object.set;
					log ("positioning set slider to: " + set + " left: " + set_offsets [set] + " width: " + set_widths [set]);
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
				final View vIndicator = findViewById (R.id.set_indicator);
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
		log ("refresh home");
		config.query_cache = new Hashtable < String, String[] > ();
		
		if (home_slider != null)
			home_slider.reload_data();
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
		
		String type = "portal"; // "portal", "whatson"
		
		new playerAPI (in_main_thread, config, "portal?time=" + hour + "&type=" + type + "&minimal=true")
			{
			public void success (String[] lines)
				{
				if (lines.length < 1)
					{
					alert ("Frontpage failure");
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
					alert ("Frontpage failure: " + errtext.replaceAll ("^ERROR:", ""));
				}
			};
		}

	public void portal_frontpage_ii()
		{
		if (current_layer != toplayer.PLAYBACK && current_layer != toplayer.NAG)
			{
			/* only if we haven't gone to playback, such as a share or notify */
			enable_home_layer();
			}
		}
	
	String portal_stack_ids[] = null;
	String portal_stack_names[] = null;
	String portal_stack_episode_thumbs[] = null;
	String portal_stack_channel_thumbs[] = null;	
	String portal_stack_banners[] = null;
	
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
					parse_special_tags ("set", fields[8], fields[0]);
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
				if (home_slider != null)
					home_slider.notifyDataSetChanged();
				}
			};
		
		if (is_tablet())
			{
			thumbnail.download_set_banners (main.this, config, 
					original_portal_stack_ids, original_portal_stack_banners, 
						in_main_thread, update);
			}
		}	
			
	public void parse_special_tags (String type, String tags, String set_id)
		{
		if (tags != null && !tags.equals(""))
			{
			String taglists[] = tags.split (";");
			for (String taglist: taglists)
				{
				if (taglist.contains (":") && !taglist.endsWith (":"))
					{
					String tagkv[] = taglist.split (":");
					if (tagkv.length >= 2)
						{
						String ids[] = tagkv[1].split (",");
						for (String id: ids)
							config.set_special_tag (id, type, set_id, tagkv[0]);
						}
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
		
		final String query = "channelLineup?user=" + config.usertoken + "&setInfo=true";
		
		if (config.query_cache.get (query) != null)
			{
			log ("using cached value");
			config.parse_channel_info_with_setinfo (config.query_cache.get (query));
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
					thumbnail.stack_thumbs (main.this, config, subscribed_channels, -1, in_main_thread, subscription_thumb_updated);
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
	
	void query_set_info (final String id, final Callback callback)
		{
		String query = null;
		
		log ("query set info: " + id);
		
		if (id.equals ("virtual:shared"))
			{
			// query = "shareInChannelList?channel=" + shared_channel;
			}
		else
			{
			Calendar now = Calendar.getInstance();
			int hour = now.get (Calendar.HOUR_OF_DAY);
			String short_id = id.replaceAll ("^virtual:", "");
			query = "setInfo?set=" + short_id + "&time=" + hour + "&programInfo=false";
			}
		
		log (query);
		
		if (config.query_cache.get (query) != null)
			{
			log ("using cached value");
			config.parse_setinfo (id, config.query_cache.get (query));
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
					config.query_cache.put (final_query, chlines);
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
		Bitmap thumbits[] = null;
		String first_episode[] = null;
		boolean saved_mini_mode = false;
		
		/* the number of times this has been front and center in its container */
		int primary_count = 0;
		
		ChannelAdapter (Activity context, String content[], Swaphome sh)
			{
			super (context, is_tablet() ? R.layout.channel_tablet : R.layout.channel, content);
			this.context = context;
			this.content = content;
			this.sh = sh;
			saved_mini_mode = mini_mode;
			init_arrays();
			
			/* I think sometimes the main thread is too busy to get every update */
			in_main_thread.postDelayed (new Runnable()
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
			if (is_phone())
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
				int wanted_layout_type = is_tablet() ? (mini_mode ? R.layout.channel_mini : R.layout.channel_full) : R.layout.channel;
				log ("ChannelAdapter inflate row type: " + wanted_layout_type);
				row = inflater.inflate (wanted_layout_type, null);
				}
			else
				row = convertView;
			
			if (is_phone())
				adjust_for_device (row);			
			
			TextView vTitle = (TextView) row.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (name != null ? name : ("Channel " + channel_id));

			ImageView vChannelIcon = (ImageView) row.findViewById (R.id.channel_icon);

			View vChannelFrame = row.findViewById (R.id.channel_frame);
			
			int big_thumb_width = 0;
			int big_thumb_height = 0;
			
			if (is_tablet())
				{
				// float factor = mini_mode ? 0.3f : 0.45f;
				float factor = mini_mode ? 0.3f : 0.55f;
				big_thumb_width = (int) ((screen_width - pixels_40) * factor);
				big_thumb_height = (int) ((float) big_thumb_width / 1.77);
				LinearLayout.LayoutParams pic_layout = (LinearLayout.LayoutParams) vChannelFrame.getLayoutParams();
				pic_layout.height = big_thumb_height;
				pic_layout.width = big_thumb_width;
				vChannelFrame.setLayoutParams (pic_layout);						
				}
			else
				{
				big_thumb_width = screen_width;
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
			        	player_real_channel = channel_id;
			        	clear_metadata();
			        	update_metadata_inner();
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
			        	player_real_channel = channel_id;
			        	clear_metadata();
			        	update_metadata_inner();
			        	launch_player (channel_id, content);
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
				set_follow_icon_state (vFollow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
				vFollow.setOnClickListener (new OnClickListener()
					{
			        @Override
			        public void onClick (View v)
			        	{
			        	log ("click on: home follow/unfollow channel " + channel_id);
			        	follow_or_unfollow (channel_id, v);
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
			        	follow_or_unfollow (channel_id, vFollow);
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
			
			if (vTriple != null)
				{
				Bitmap bm = null;
				if (thumbits [position] == null)
					{
					bm = triple_thumbnail (channel_id, program_line, big_thumb_width);
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
					
					int n_thumbs = is_tablet() ? 4 : 1;
					log ("** request " + n_thumbs + " thumbs: " + channel_id + " (position: " + position + ")");
					
					thumbnail.download_first_n_episode_thumbs
							(main.this, config, channel_id, n_thumbs, in_main_thread, triple_update_thumbs);
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
					String ago = util.ageof (main.this, ts);
					String txt_episode = getResources().getString (R.string.episode_lc);
					String txt_episodes = getResources().getString (R.string.episodes_lc);
					String subtitle = ago + " • " + display_episodes + " " + (display_episodes == 1 ? txt_episode : txt_episodes);
					vSubTitle.setText (subtitle);
					}
				
				String requested_q_thumbs = config.pool_meta (channel_id, "requested_q_thumbs");
				if (requested_q_thumbs == null || !requested_q_thumbs.equals ("true"))
					{
					config.set_channel_meta_by_id (channel_id, "requested_q_thumbs", "true");
					int n_thumbs = is_tablet() ? 4 : 1;
					thumbnail.download_q_thumbs (main.this, config, channel_id, n_thumbs, in_main_thread, triple_update_thumbs);
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
						int n_thumbs = is_tablet() ? 4 : 1;
						log ("** request " + n_thumbs + " thumbs: " + channel_id + " (position: " + position + ")");
						
						thumbnail.download_first_n_episode_thumbs
							(main.this, config, channel_id, n_thumbs, in_main_thread, triple_update_thumbs);
						
						requested_channel_thumbs [position] = true;
						notifyDataSetChanged();
						}
					};

				/* load channels only if this has been a primary display at least once -- keeps stuff from loading in the background and using up CPU */
				if (false && primary_count > 0 && !requested_channel_load [position])
					{
					requested_channel_load [position] = true;
					int urgency = 600 * (num_episodes > 0 ? 1 : 0);
					in_main_thread.postDelayed (new Runnable()
						{
						@Override
						public void run()
							{
							load_channel_then (channel_id, true, after_load, channel_id, row);
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
				String ago = util.ageof (main.this, ts);
				vAgo.setText (ago);
				}
			
			if (is_phone())
				{
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
				}
			
			return row;
			}
		
		public void adjust_for_device (View row)
			{
			if (is_phone())
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
					layout6.height = pixels_32;
					layout6.width = pixels_32;
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
				fill_in_episode_thumb (program_line[0], parent, R.id.channel_icon, (is_phone() ? R.id.first_episode_title : 0), use_blank);
				
				first_episode [position] = program_line[0];
				
				TextView vSubTitle = (TextView) parent.findViewById (R.id.subtitle);
				if (vSubTitle != null)
					{
					long ts = config.get_most_appropriate_timestamp (channel_id);
					int display_episodes = config.display_channel_count (channel_id);
					String ago = util.ageof (main.this, ts);
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
					String f1 = (t1 == null) ? null : main.this.getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (t1) + ".png";
										
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
							String cfilename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
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
				
				if (is_phone())
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
		}
	
	public Bitmap triple_thumbnail (String channel_id, String program_line[], int big_thumb_width)
		{
		int margin_cruft = (int) (density * (5 + 10 + 10 + 5 + 5 + 5));
		
		// int thumb_width = (screen_width - margin_cruft - big_thumb_width) / 3;
		int thumb_width = (screen_width - margin_cruft - big_thumb_width) / 2;		
		int thumb_height = (int) ((float) thumb_width / 1.77);
		
		log ("-->THUMB WIDTH :: " + thumb_width + " (big thumb width: " + big_thumb_width + ", screen width: " + screen_width + ")");
		
		if (program_line != null && program_line.length > 1)
			{
			String e1 = program_line.length >= 2 ? program_line [1] : null;
			String e2 = program_line.length >= 3 ? program_line [2] : null;
			String e3 = program_line.length >= 4 ? program_line [3] : null;
			
			String f1 = (e1 == null) ? null : main.this.getFilesDir() + "/" + config.episode_in_cache (e1);
			String f2 = (e2 == null) ? null : main.this.getFilesDir() + "/" + config.episode_in_cache (e2);
			String f3 = (e3 == null) ? null : main.this.getFilesDir() + "/" + config.episode_in_cache (e3);

			return bitmappery.generate_double_thumbnail (channel_id, thumb_width, thumb_height, pixels_10, f1, f2);
			}
		else
			{
			String e2 = config.pool_meta (channel_id, "episode_thumb_2");
			String e3 = config.pool_meta (channel_id, "episode_thumb_3");	
			String e4 = config.pool_meta (channel_id, "episode_thumb_4");
			
			if ((e2 != null && !e2.equals("")) || (e3 != null && !e3.equals("")) || (e4 != null && !e4.equals("")))
				{
				String f2 = (e2 == null) ? null : main.this.getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (e2) + ".png";
				String f3 = (e3 == null) ? null : main.this.getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (e3) + ".png";
				String f4 = (e4 == null) ? null : main.this.getFilesDir() + "/" + config.api_server + "/qthumbs/" + channel_id + "/" + util.md5 (e4) + ".png";
				
				return bitmappery.generate_double_thumbnail (channel_id, thumb_width, thumb_height, pixels_10, f2, f3);
				}
			else
				return null;
			}
		}
	
	public boolean fill_in_episode_thumb (String episode_id, View parent, int resource_id, int title_resource_id, boolean use_blank)
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
				// log ("********************** FILL IN EPISODE THUMB res(" + resource_id + ") episode(" + episode_id + ")"); // noisy
				String filename = main.this.getFilesDir() + "/" + config.episode_in_cache (episode_id);
				
				File f = new File (filename);
				if (f.exists())
					{
					if (f.length() > 0)
						{
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							vThumb.setImageBitmap (bitmap);
						}
					else
						{
						if (use_blank)
							vThumb.setImageResource (R.drawable.store_unavailable);
						}
					thumbnail_found = true;
					}				
				}
			if (!thumbnail_found)
				{
				if (use_blank)
					vThumb.setImageResource (R.drawable.store_unavailable);
				}
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
				track_event ("navigation", "selectEP", "selectEP", 0);
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
			
			
		load_channel_then (channel_id, false, setup_horiz_inner, channel_id, horiz);

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
					EpisodeAdapter horiz_adapter = (EpisodeAdapter) horiz.getAdapter();
					horiz_adapter.notifyDataSetChanged();
					}
				};

			// thumbnail.download_episode_thumbnails (main.this, config, channel_id, in_main_thread, horiz_update_thumbs);
			}
		};
	
	public EpisodeSlider NEW_setup_horiz (ViewPager horiz, final String channel_id)
		{
		final EpisodeSlider horiz_adapter = new EpisodeSlider (horiz, channel_id, new String[0]);
				       
		if (horiz == null)
			return null;
		
		horiz.setAdapter (horiz_adapter);						
		load_channel_then (channel_id, false, NEW_setup_horiz_inner, channel_id, horiz);

		return horiz_adapter;
		}
	
	final Callback NEW_setup_horiz_inner = new Callback()
		{
		@Override
		public void run_string_and_object (final String channel_id, Object horiz_obj)
			{
			String episodes[] = config.program_line_by_id (channel_id);
			final ViewPager horiz = (ViewPager) horiz_obj;
			EpisodeSlider horiz_adapter = (EpisodeSlider) horiz.getAdapter();
			horiz_adapter.set_content (channel_id, episodes);
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
			
	    	// log ("horiz " + channel_id + " getView: " + position); // noisy
	    	
			TextView vEptitle = (TextView) row.findViewById (R.id.eptitle);
			ImageView vPic = (ImageView) row.findViewById (R.id.pic);
			ProgressBar vProgress = (ProgressBar) row.findViewById (R.id.progress);
			
			String program_id = program_line [position];
			
			vEptitle.setText (config.program_meta (program_id, "name"));
			
			int gray = Color.rgb (0xC0, 0xC0, 0xC0);
			int orange = Color.rgb (0xFF, 0x99, 0x00);
			int black = Color.rgb (0x00, 0x00, 0x00);
			int white = Color.rgb (0xFF, 0xFF, 0xFF);
			
			View vBorder = row.findViewById (R.id.border);
			vBorder.setBackgroundColor (position == current_episode_index - 1 ? white : black);
			
			vPic.setImageAlpha (position == current_episode_index -1 ? 0xFF : 0xA0);
			
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
	// PlaybackCommentsAdapter playback_comments_adapter = null;
	
	public void play_channel (String channel_id)
		{
		if (current_layer != toplayer.PLAYBACK)
			{
			enable_player_layer();
			in_main_thread.postDelayed (new Runnable()
				{
				@Override
				public void run()
					{
					bouncy_playback_hint_animation();
					}
				}, 3000);
			setup_player_adapters (channel_id);
			// setup_player_fragment (channel_id);
			}
		play_first (channel_id);
		}
	
	public void play_episode_in_channel (String channel_id, String episode_id)
		{
		if (current_layer != toplayer.PLAYBACK)
			{
			enable_player_layer();
			setup_player_adapters (channel_id);
			}
		log ("Play episode: " + episode_id + " in channel " + channel_id);
		play (channel_id, episode_id);
		}
	
	@Override
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
			if (playback_episode_pager != null)
				playback_episode_pager.notifyDataSetChanged();
			}
		};
	
	/*
	final Runnable playback_comments_updated = new Runnable()
		{
		public void run()
			{
			int num_comments = 0;
			TextView vNumComments = (TextView) findViewById (R.id.num_comments);
			
			if (playback_comments_adapter != null)
				{
				String num_comments_string = config.program_meta (current_episode_id, "maxcomment");
				log ("playback comments updated (episode " + current_episode_id + ", " + num_comments + " comments)");
				if (num_comments_string != null && !num_comments_string.equals (""))
					{
					num_comments = Integer.parseInt (num_comments_string);
					}
					
				vNumComments.setText ("" + num_comments); // moved
				
				playback_comments_adapter.set_episode_id (current_episode_id); // moved
				playback_comments_adapter.set_number_of_comments (num_comments); // movied
				
				playback_comments_adapter.notifyDataSetChanged();
				}
			}
		};
	*/
		
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
			if (playback_episode_pager != null)
				{
				String content[] = config.program_line_by_id (channel_id);
				playback_episode_pager.set_content (channel_id, content);
				// playback_episode_pager.notifyDataSetChanged();
				}
			if (playback_channel_adapter != null)				
				playback_channel_adapter.notifyDataSetChanged();
						
			// playback_comments_updated.run();
			
			// thumbnail.download_episode_thumbnails (main.this, config, channel_id, in_main_thread, playback_episode_thumb_updated);
			
			set_follow_icon_state (R.id.playback_follow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			set_follow_icon_state (R.id.playback_follow_landscape, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			
			// ytchannel.fetch_youtube_comments_in_thread (in_main_thread, playback_comments_updated, config, episode_id);
			
			String channel_name = config.pool_meta (channel_id, "name");
			String episode_desc = config.program_meta (episode_id, "desc");
			
			TextView vChannelName = (TextView) findViewById (R.id.playback_channel);
			if (vChannelName != null)
				vChannelName.setText (channel_name);
			
			NEW_fill_episode_description (episode_desc);
			
			update_episode_count (channel_id);
			
			update_channel_icon (channel_id);			
			}
		}

	public void update_channel_icon (String channel_id)
		{
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
						}
					}			
				}
			}		
		}
	
	public void update_episode_count (String channel_id)
		{
		int display_episodes = config.programs_in_real_channel (channel_id);
				
		String txt_episode = getResources().getString (R.string.episode_lc);
		String txt_episodes = getResources().getString (R.string.episodes_lc);
		
		TextView vEpisodeCount = (TextView) findViewById (R.id.playback_episode_count);
		if (vEpisodeCount != null)
			vEpisodeCount.setText ("" + display_episodes);
		
		TextView vEpisodePlural = (TextView) findViewById (R.id.playback_episode_plural);
		if (vEpisodePlural != null)
			vEpisodePlural.setText (display_episodes == 1 ? txt_episode : txt_episodes);
		}

	/* minimized risk -- don't alter fragments unless we've switched to the player fragment */
	boolean fragment_changed = false;
	
	public void setup_player_fragment (String channel_id)
		{
		String nature = config.pool_meta (channel_id, "nature");
		if (nature.equals ("667"))
			{
			fragment_changed = true;
			show_player_fragment();
			hide_video_fragment();
			}
		else
			{
			if (fragment_changed)
				{
				show_video_fragment();
				hide_player_fragment();
				fragment_changed = false;
				}
			}
		}
	
	EpisodeSlider playback_episode_pager = null;
	
	public void setup_player_adapters (String channel_id)
		{
		log ("setup player adapters");
		
		HorizontalListView horiz = (HorizontalListView) findViewById (0 /* R.id.playback_episodes */);
		if (horiz != null)
			playback_episode_adapter = setup_horiz (horiz, channel_id);
		
		ViewPager vPager = (ViewPager) findViewById (R.id.playback_horiz);
		if (vPager != null)			
			playback_episode_pager = NEW_setup_horiz (vPager, channel_id);
		
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
		
		/*
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
		*/
		
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
    	boolean landscape = fullscreen || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		log ("set size of video: shrunk=" + shrunk + " landscape=" + landscape);
		
		View video = findViewById (R.id.video_fragment_container);
		SpecialFrameLayout.LayoutParams layout = (SpecialFrameLayout.LayoutParams) video.getLayoutParams();
		View video2 = findViewById (R.id.player_fragment_container);
		SpecialFrameLayout.LayoutParams layout2 = (SpecialFrameLayout.LayoutParams) video2.getLayoutParams();		
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
			// set_reasonable_player_size(); /* for video2 */ MAY BE WRONG TODO
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
		
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		if (landscape)
			onVideoActivityLayout();
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
    		player_full_stop (true);	
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
		        	track_event ("navigation", "back", "back", 0);
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
		        	
		        	track_event ("function", "minimizeVideo", "minimizeVideo", 0);
		        	
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
								video_minimize (true);
								}		        		
		        			}, 50);
		        		}
		        	else
		        		video_minimize (true);
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
		
		View vEphide = findViewById (R.id.playback_collapser);
		if (vEphide != null)
			vEphide.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: episode bar hide/unhide");
			    	hide_unhide_episode_bar();
		        	}
				});	
		
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		
		View vPlaybackFollow = findViewById (R.id.playback_follow);
		if (vPlaybackFollow != null)
			{
			vPlaybackFollow.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (final View v)
		        	{
		        	log ("click on: playback follow/unfollow");
		        	
		        	if (config.usertoken == null)
		        		{
		        		remember_location();
		        		enable_signin_layer (new Runnable()
		        			{
			        		@Override
			        		public void run()
				        		{
			        			if (config.usertoken != null)
			        				{
			        				follow_or_unfollow (player_real_channel, v);
			        				restore_location();
			        				}
				        		}
		        			});
		        		}
		        	else
		        		follow_or_unfollow (player_real_channel, v);
		        	}
				});	
			}
		
		View vPlaybackFollowLandscape = findViewById (R.id.playback_follow_landscape);
		if (vPlaybackFollowLandscape != null)
			{
			vPlaybackFollowLandscape.setVisibility (config.usertoken != null && landscape ? View.VISIBLE : View.GONE);
			vPlaybackFollowLandscape.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: playback follow/unfollow");
		        	follow_or_unfollow (player_real_channel, v);
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
		        	share();
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
		        	share();
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
		        	log ("video layer ate my tap!");
		        	}
				});			
		
		/* and some additional adjustments */
		
		View vSpacer = findViewById (R.id.spacer);
		vSpacer.setVisibility (landscape ? View.GONE : View.VISIBLE);
		
		View vLandscapeEpisodeName = findViewById (R.id.landscape_episode_name);
		vLandscapeEpisodeName.setVisibility (landscape ? View.VISIBLE : View.GONE);
		}

	public void hide_unhide_episode_bar()
		{
		View vHoriz = findViewById (R.id.playback_horiz);
		if (vHoriz != null)
			{
			boolean is_visible = vHoriz.getVisibility() == View.VISIBLE;
			vHoriz.setVisibility (is_visible ? View.GONE : View.VISIBLE);
			ImageView vHide = (ImageView) findViewById (R.id.playback_ephide);
			if (vHide != null)
				vHide.setImageResource (is_visible ? R.drawable.icon_eplist_open : R.drawable.icon_eplist_close);
			/* an ugly white artifact appears below the view if this is not done */
			video_layer().postInvalidate();
			}
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
		if (current_layer != toplayer.PLAYBACK && !video_is_minimized)
			{
			log ("able to play video? No, because we are not in playback mode");
			return false;
			}
		
		View vVideoLayer = video_layer();
		if (vVideoLayer.getVisibility() != View.VISIBLE)
			{
			log ("able to play video? No, because video layer is not visible");
			return false;
			}
		
		log ("able to play video? Yes");
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
				long ts = config.get_most_appropriate_timestamp (channel_id);
				String ago = util.ageof (main.this, ts);
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
	
	/*
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
				if (num_comments == null || num_comments.equals("")) num_comments = "0";
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
				
				// example 2012-11-11T05:57:15.000Z -- SimpleDateFormat is not documented as working with "Z" type timezones
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
                
        		String ago = util.ageof (main.this, timestamp / 1000);
        		vAgo.setText (ago);
				}
			
			return row;
			}
		}
	*/
	
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
			log ("disable video layer");
			View video_layer = video_layer();
			video_layer.setVisibility (View.GONE);			
			}
		else
			log ("disable video layer: ignored, video is minimized");
		}
	
	int minimized_height = 0;
	int minimized_width = 0;
	
	public void video_minimize (boolean perform_unlaunch)
		{
		log ("video minimize");
		
		expand_video();
		
		video_is_minimized = true;
		
		View vMenuLayer = findViewById (R.id.menu_layer);
		View vSlidingPanel = findViewById (R.id.slidingpanel);
		View vVideoLayer = video_layer();
		View vPlaybackBody = findViewById (R.id.playbackbody);
		View vContainer = findViewById (R.id.video_fragment_container);
		View vContainer2 = findViewById (R.id.player_fragment_container);
		View vBacking = findViewById (R.id.backing_controls);
		View vControls = findViewById (R.id.controls);
		View vTitlecard = findViewById (R.id.titlecard);
		View yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);	
		View vTopControls = findViewById (R.id.top_controls);
		View vChromecast = findViewById (R.id.chromecast_window);
		
		FrameLayout.LayoutParams video_layout = (FrameLayout.LayoutParams) vVideoLayer.getLayoutParams();			
		LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
		SpecialFrameLayout.LayoutParams container_layout = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();
		SpecialFrameLayout.LayoutParams container2_layout = (SpecialFrameLayout.LayoutParams) vContainer2.getLayoutParams();		
		FrameLayout.LayoutParams backing_layout = (FrameLayout.LayoutParams) vBacking.getLayoutParams();
		FrameLayout.LayoutParams chromecast_layout = (FrameLayout.LayoutParams) vChromecast.getLayoutParams();
		
		vPlaybackBody.setVisibility (View.GONE);
		vBacking.setVisibility (chromecasted ? View.VISIBLE : View.GONE);
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
		
		minimized_width = (int) ((is_phone() ? 210 : 260) * density);
		// minimized_width = (int) ((float) screen_width * 0.6);
		// ******* minimized_width: 500, ********** minimized_height: 282

		minimized_height = (int) ((float) minimized_width * aspect);
		
		log ("******* w: " + fully_expanded_width + " *************** h: " + fully_expanded_height + " ************ aspect: " + aspect);
		log ("******* minimized_width: " + minimized_width + ", ********** minimized_height: " + minimized_height);
		container_layout.width = minimized_width; 
		container_layout.height = minimized_height;
		container_layout.topMargin = 0;
		container_layout.bottomMargin = MARGINALIA;
		container_layout.leftMargin = 0;
		container_layout.rightMargin = MARGINALIA;
		container_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;		
		// container_layout.topMargin = (screen_height - new_height) / 2 - pixels_60;
		/// container_layout.bottomMargin = pixels_60;
		/// container_layout.leftMargin = (screen_width - new_width) / 2 - pixels_60;	
		
		vContainer.setLayoutParams (container_layout);
		
		container2_layout.width = minimized_width; 
		container2_layout.height = minimized_height;
		container2_layout.topMargin = 0;
		container2_layout.bottomMargin = MARGINALIA;
		container2_layout.leftMargin = 0;
		container2_layout.rightMargin = MARGINALIA;
		container2_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;	
		
		vContainer2.setLayoutParams (container2_layout);
		
		video_layout.width = screen_width;
		// video_layout.height = minimized_height + 2 * pixels_60;
		video_layout.height = minimized_height + MARGINALIA + pixels_20;
		video_layout.topMargin = 0;
		video_layout.bottomMargin = 0;
		video_layout.leftMargin = 0;
		video_layout.rightMargin = 0;
		video_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		vVideoLayer.setLayoutParams (video_layout);
		
		chromecast_layout.width = minimized_width;
		chromecast_layout.height = minimized_height;
		chromecast_layout.rightMargin = MARGINALIA;
		chromecast_layout.bottomMargin = MARGINALIA;
		chromecast_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;		
		vChromecast.setLayoutParams (chromecast_layout);
		
		backing_layout.width = minimized_width;
		backing_layout.height = minimized_height;
		backing_layout.rightMargin = MARGINALIA;
		backing_layout.bottomMargin = MARGINALIA;
		backing_layout.gravity = Gravity.BOTTOM | Gravity.RIGHT;				
		vBacking.setLayoutParams (backing_layout);
		
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
			public boolean onTouch (View arg0, MotionEvent arg1)
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
			public boolean onTouch (View arg0, MotionEvent arg1)
				{
				log ("***************** VIDEO LAYER onTouch");
				return false;
				}		
		});
		yt_wrapper.setOnTouchListener (new OnTouchListener()
		{
			@Override
			public boolean onTouch (View arg0, MotionEvent arg1)
				{
				log ("***************** YT WRAPPER LAYER onTouch");
				return false;
				}		
		});
		
		yt_wrapper.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
		// vVideoLayer.setBackgroundColor (Color.argb (0x40, 0xFF, 0x00, 0x00));
		vVideoLayer.setBackgroundColor (Color.argb (0x00, 0x00, 0x00, 0x00));
		
		if (perform_unlaunch)
			unlaunch_player();
		
		vVideoLayer.postInvalidate();
		
		if (chromecasted)
			{
			log ("minimize, chromecasted");
			}
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
		View vContainer2 = findViewById (R.id.player_fragment_container);		
		View vBacking = findViewById (R.id.backing_controls);
		View vControls = findViewById (R.id.controls);		
		View vTitlecard = findViewById (R.id.titlecard);
		View yt_wrapper = (SpecialFrameLayout) findViewById (R.id.ytwrapper2);	
		View vTopControls = findViewById (R.id.top_controls);
		View vChromecast = findViewById (R.id.chromecast_window);
		
		FrameLayout.LayoutParams video_layout = (FrameLayout.LayoutParams) vVideoLayer.getLayoutParams();			
		LinearLayout.LayoutParams wrapper_layout = (LinearLayout.LayoutParams) yt_wrapper.getLayoutParams();
		SpecialFrameLayout.LayoutParams container_layout = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();
		SpecialFrameLayout.LayoutParams container2_layout = (SpecialFrameLayout.LayoutParams) vContainer2.getLayoutParams();		
		FrameLayout.LayoutParams backing_layout = (FrameLayout.LayoutParams) vBacking.getLayoutParams();
		FrameLayout.LayoutParams chromecast_layout = (FrameLayout.LayoutParams) vChromecast.getLayoutParams();
		
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
				
		container2_layout.width = screen_width;
		container2_layout.height = (int) (screen_width / 1.77);
		container2_layout.topMargin = 0;
		container2_layout.bottomMargin = 0;
		container2_layout.leftMargin = 0;
		container2_layout.rightMargin = 0;
		container2_layout.gravity = Gravity.CENTER;
		vContainer2.setLayoutParams (container2_layout);
		
		chromecast_layout.width = MATCH_PARENT;
		chromecast_layout.height = (int) (screen_width / 1.77);
		chromecast_layout.rightMargin = 0;		
		chromecast_layout.bottomMargin = 0;
		chromecast_layout.gravity = Gravity.CENTER;		
		vChromecast.setLayoutParams (chromecast_layout);
		
		backing_layout.width = MATCH_PARENT;
		backing_layout.height = (int) (screen_width / 1.77);
		backing_layout.rightMargin = 0;
		backing_layout.bottomMargin = 0;
		backing_layout.gravity = Gravity.CENTER;				
		vBacking.setLayoutParams (backing_layout);
		
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
			if (max_pixels_minimized_box_moved < pixels_100)
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
			if (Math.abs (deltaX) > pixels_100)
				{
		    	if (current_layer != toplayer.PLAYBACK)
		    		{
		    		log ("video disappear");
		    		if (chromecasted)
			    		{
		    			chromecast_send_simple ("stop");
			    		}
		    		else
		    			{
						pause_video();
			    		}
				    video_normal();
			    	disable_video_layer();		    		
		    		}
		    	else
		    		{
		    		log ("video maximize only");
		    		video_normal();
		    		}
				}	
			else
				{
				/* reset margins */
				log ("onVideoActionUp: small deltaX: " + deltaX);
				video_minimize (false);
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
		View vContainer2 = findViewById (R.id.player_fragment_container);		
		View vBacking = findViewById (R.id.backing_controls);
		View vChromecast = findViewById (R.id.chromecast_window);				
		
		int max_drag = screen_width - MARGINALIA - minimized_width;
		
		if (/* deltaX >= 0 && */ deltaX < max_drag)
			{
			SpecialFrameLayout.LayoutParams container_layout = (SpecialFrameLayout.LayoutParams) vContainer.getLayoutParams();	
			SpecialFrameLayout.LayoutParams container2_layout = (SpecialFrameLayout.LayoutParams) vContainer2.getLayoutParams();
			FrameLayout.LayoutParams backing_layout = (FrameLayout.LayoutParams) vBacking.getLayoutParams();
			FrameLayout.LayoutParams chromecast_layout = (FrameLayout.LayoutParams) vChromecast.getLayoutParams();
			
			container_layout.rightMargin = MARGINALIA + deltaX;			
			vContainer.setLayoutParams (container_layout);
			
			container2_layout.rightMargin = MARGINALIA + deltaX;			
			vContainer2.setLayoutParams (container2_layout);
			
			backing_layout.rightMargin = MARGINALIA + deltaX;
			vBacking.setLayoutParams (backing_layout);
			
			chromecast_layout.rightMargin = MARGINALIA + deltaX;
			vChromecast.setLayoutParams (chromecast_layout);
			
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

	class SwapEpisodes
		{
		LinearLayout hrow = null;
		int position = 0;
		public SwapEpisodes (int position)
			{
			this.position = position;
			}
		};
	
		SwapEpisodes current_horiz_swap = null;
	
	/* this is implemented using the base class! */

	public class EpisodeSlider extends PagerAdapter
		{
		String content[] = null;
		String channel_id = null;
		
		ViewPager vp = null;
		
		public EpisodeSlider (ViewPager vp, String channel_id, String content[])
			{			
			super();
			
			this.vp = vp;
			this.channel_id = channel_id;
			
			if (content == null)
				content = new String [0];
			this.content = content; 
			
			log ("episode slider: " + content.length + " episodes");
			
			/* set the height of the viewpager */
			
			int per = is_phone() ? 3 : 4;    
			int smudge = (int) (per * 6 * density);
			
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vp.getLayoutParams();
			int width = screen_width / per;
			int height = (int) ((float) width / 1.77);
			layout.height = height;
			vp.setLayoutParams (layout);
	        }
		
		public void set_content (String channel_id, String content[])
			{
			this.channel_id = channel_id;
			if (content == null)
				content = new String [0];
			this.content = content; 
			log ("episode slider reset: " + content.length + " episodes");
			notifyDataSetChanged();
			}
		
		public void set_channel_id_only (String channel_id)
			{
			this.channel_id = channel_id;
			}
		
		@Override
		public void notifyDataSetChanged()
			{			
			super.notifyDataSetChanged();
			log ("[HORIZ] notifyDataSetChanged");
			redraw_swap (current_horiz_swap);
			}
		
		public void rejigger()
			{
			if (vp != null)
				{
		    	int per = is_phone() ? 3 : 4;
				int page_wanted = (current_episode_index - 1) / per;
				vp.setCurrentItem (page_wanted, true);
				}
			redraw_swap (current_horiz_swap);
			}
		
	    @Override
	    public int getCount()
	    	{
	    	int per = is_phone() ? 3 : 4;
	    	int length = content != null ? (content.length + per - 1) / per : 0;
	    	// log ("** HORIZ length (" + channel_id + "): " + length); // busy
	        return length;
	    	}
	
		@Override
		public boolean isViewFromObject (View view, Object object)
			{
			return (((SwapEpisodes) object).hrow) == (LinearLayout) view;
			}
		
		@Override
		public Object instantiateItem (ViewGroup container, int position)
			{
			log ("[HORIZ] instantiate: " + position);
			
			final SwapEpisodes swap = new SwapEpisodes (position);			
	
			LinearLayout hrow = (LinearLayout) View.inflate (main.this, R.layout.episode_row, null);					
			swap.hrow = hrow;
			
			if (is_phone())
				{
				View vBox = hrow.findViewById (R.id.ep3_box);
				vBox.setVisibility (View.GONE);
				}
			
			/* fill in data */					
			
			((ViewPager) container).addView (hrow, 0);
			redraw_swap (swap);			
			
			return swap;
			}
		
		/* remember which are downloaded, to prevent looping when episode thumbnails are bad */
		Set <String> episode_download_requests = new HashSet <String> ();
		
		public void redraw_swap (final SwapEpisodes swap)
			{
			if (swap != null)
				{
				log ("*** redraw swap: " + swap.position);
				
				LinearLayout hrow = swap.hrow;
				
				final int per = is_phone() ? 3 : 4;
				int base = swap.position * per;
				
				String episodes[] = new String [4];
				boolean all_thumbs_found = true;
				
				for (int i = 0; i < per; i++)
					{
					int resource_id = getResources().getIdentifier ("ep" + i, "id", getPackageName());
					int title_id = getResources().getIdentifier ("eptitle" + i, "id", getPackageName());
					int box_id = getResources().getIdentifier ("ep" + i + "_box", "id", getPackageName());
					String episode = null;
					if (content != null)
						episode = base + i < content.length ? content [base + i] : null;
					episodes [i] = episode;
					if (title_id != 0)
						{
						TextView vTitle = (TextView) hrow.findViewById (title_id);
						if (vTitle != null)
							vTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
						}					

					String nature = config.pool_meta (channel_id, "nature");
					boolean use_blank = nature == null || !nature.equals ("13");
					
					if (!fill_in_episode_thumb (episode, hrow, resource_id, title_id, use_blank))
						{
						boolean found_episode_thumb = false;
						
						if (nature != null && nature.equals ("13"))
							{
							/* no thumbnail, try a channel thumbnail if this is a live channel (type 13) */
							String cfilename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
							File cf = new File (cfilename);
							if (cf.exists())
								{
								Bitmap bitmap = BitmapFactory.decodeFile (cfilename);
								if (bitmap != null)
									{
									ImageView vThumb = (ImageView) hrow.findViewById (resource_id);
									if (vThumb != null)
										{
										vThumb.setImageBitmap (bitmap);
										found_episode_thumb = true;
										}
									}	
								}
							}
						if (!found_episode_thumb)
							all_thumbs_found = false;
						}
					
					View vBorder = hrow.findViewById (resource_id);
					vBorder.setBackgroundColor (content != null && base + i == current_episode_index - 1 ? Color.WHITE : Color.BLACK);
					View vBox = hrow.findViewById (box_id);
					vBox.setVisibility (episode == null ? View.INVISIBLE : View.VISIBLE);
					// log ("[HORIZ] current: " + (current_episode_index - 1) + ", this one: " + (base + i)); // busy
					
					final int i_final = i;
					vBox.setOnClickListener (new OnClickListener()
						{
				        @Override
				        public void onClick (View v)
				        	{
				        	int index = 1 + swap.position * per + i_final;
				        	log ("playback click: " + index);
							track_event ("navigation", "selectEP", "selectEP", 0);
				        	play_nth_episode_in_channel (channel_id, index);
				        	}
						});					
					}
						
				if (!is_phone())
					{
					String episode3 = null;
					if (content != null)
						episode3 = base + 3 < content.length ? content [base+3] : null;
					episodes [3] = episode3;
					if (!fill_in_episode_thumb (episode3, hrow, R.id.ep3, 0, true))
						all_thumbs_found = false;
					}
				
				log ("all thumbs found: " + all_thumbs_found);
		
				String req = player_real_channel + "$" + TextUtils.join (":", episodes);
				boolean already_downloaded = episode_download_requests.contains (req);
				episode_download_requests.add (req);
				
				if (!all_thumbs_found && !already_downloaded)
					{
					/* download episode thumbs */
					thumbnail.download_specific_episode_thumbnails
							(main.this, config, channel_id, episodes, in_main_thread, new Runnable()
						{
						@Override
						public void run()
							{
							redraw_swap (swap);
							}									
						});
					}
				
				if (content != null && base + 4 >= content.length)
					ytchannel.extend_channel (config, channel_id, in_main_thread, new Runnable()
						{						
						@Override
						public void run()
							{
							String program_line[] = config.program_line_by_id (channel_id);
							set_content (channel_id, program_line);
							redraw_swap (swap);
							update_metadata_inner();
							}					
						});
				}
			}
		
		@Override
		public void destroyItem (ViewGroup container, int position, Object object)
			{
			log ("[HORIZ] destroy: " + position);
			SwapEpisodes swap = (SwapEpisodes) object;
			((ViewPager) container).removeView (swap.hrow);
			}
		
		@Override
		public void setPrimaryItem (ViewGroup container, int position, Object object)
			{
			log ("[HORIZ]: primary item: " + position);
			if (object != null)
				{
				current_horiz_swap = (SwapEpisodes) object;
				redraw_swap (current_horiz_swap);
				}
			}
		}		
	
	/*** GUIDE **************************************************************************************************/
	
	public void enable_guide_layer()
		{
		disable_video_layer();		
		set_layer (toplayer.GUIDE);		
		setup_guide_buttons();
		init_3x3_grid();
		track_layer (toplayer.GUIDE);
		}
	
	public void setup_guide_buttons()
		{
		View vGuideLayer = findViewById (R.id.guidelayer);
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
		
		if (is_phone())
			{
			TextView vGuideTitle = (TextView) findViewById (R.id.guide_title);
			vGuideTitle.setTextSize (TypedValue.COMPLEX_UNIT_SP, 20);
			
			TextView vGuideMeta = (TextView) findViewById (R.id.guide_meta);
			vGuideMeta.setTextSize (TypedValue.COMPLEX_UNIT_SP, 14);
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
			        		enable_signin_layer (null);
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
			/* there is no longer a follow/unfollow button on individual grid items */
			set_follow_icon_state (vFollow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			// vFollow.setImageResource (config.is_subscribed (channel_id) ? R.drawable.icon_unfollow : R.drawable.icon_follow_black);
			vFollow.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: guide follow/unfollow");
		        	follow_or_unfollow (channel_id, v);
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
				load_channel_then (channel_id, false, highlight_3x3_square_inner, channel_id, null);
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
		TextView vLargeMeta = (TextView) findViewById (R.id.guide_meta);
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
    	launch_player (channel_id, null, channels);
    	}
 
    public void launch_player (String channel_id, String episode_id, String channels[])
		{
    	/* bug #12465: when waking, sometimes time will accumulate even if the player is not playing */
    	reset_time_played();
    	
		if (current_layer != toplayer.PLAYBACK)
			previous_layer = current_layer;
		
		previous_arena = arena;
		set_arena (channels);
		
		if (episode_id != null)
			play_episode_in_channel (channel_id, episode_id);
		else
			play_channel (channel_id);
		}
    
    public void set_arena (String channels[])
    	{
		if (channels.length > 0 && channels [0] != null)
			{
			String new_channels[] = new String [channels.length + 1];
			for (int i = 0; i < channels.length; i++)
				new_channels [i+1] = channels [i];
			new_channels [0] = null;
			channels = new_channels;
			}
		arena = channels;    	
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
    
	/*** TEST **************************************************************************************************/
    
    public void enable_test_layer()
    	{
		disable_video_layer();
		set_layer (toplayer.TEST);
		
		int adnum = config.next_advert();
		log ("next advert is: " + adnum);
		String url = config.advert_meta (adnum, "url");
		String id = config.advert_meta (adnum, "id");
		String name = config.advert_meta (adnum, "name");
		log ("advert url: " + url);
		if (url != null && !url.equals (""))
			launch_direct_ad (url, id, name);
    	}
    
	/*** SOCIAL **************************************************************************************************/
    
    SocialAdapter social_adapter = null;
    
    public class social
    	{	
    	String username;
    	String text;
    	}
    
    social social_feed[] = new social [0];
    
    public void enable_social_layer()
		{
		disable_video_layer();
		set_layer (toplayer.SOCIAL);	
	
		Callback cb = new Callback()
			{
			@Override
			public void run_two_strings (String username, String text)
				{
				log ("@" + username + " :: " + text);
				
				social new_feed[] = new social [social_feed.length + 1];
				System.arraycopy (social_feed, 0, new_feed, 1, social_feed.length);
				
				social item = new social();
				item.username = username;
				item.text = text;
				
				new_feed [0] = item; 
				social_feed = new_feed;
				
				// if (social_feed.length % 25 == 0)
					in_main_thread.post (new Runnable()
						{
						@Override
						public void run()
							{
							social_adapter.set_content (social_feed);
							}
						});
					}
			};	
		
		ListView vSocial = (ListView) findViewById (R.id.social_list);
		social_adapter = new SocialAdapter (this, social_feed);
		vSocial.setAdapter (social_adapter);
		
		final Streamy s = new Streamy();
		s.t0 (cb);
		
		View vStop = findViewById (R.id.soc_stop);
		if (vStop != null)
			vStop.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	s.close();
		        	}
				});
		}
    
	public class SocialAdapter extends BaseAdapter
		{
		private Context context;
		private social socials[] = null;
		
		public SocialAdapter (Context context, social socials[])
			{
			this.context = context;
			this.socials = socials;
			}
	
		public void set_content (social socials[])
			{
			this.socials = socials;
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
				rv = (LinearLayout) View.inflate (main.this, R.layout.social_item, null);				
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
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText ("@" + socials [position].username);
			
			TextView vText = (TextView) rv.findViewById (R.id.text);
			if (vText != null)
				vText.setText (socials [position].text);
	
			TextView vCounter = (TextView) rv.findViewById (R.id.soc_counter);
			if (vCounter != null)
				vCounter.setText ("#" + Integer.toString (socials.length - position));
			
			return rv;
			}	
		}	

	/*** ADVERTISING **************************************************************************************************/
    
    @Override
    public boolean advertise (final Runnable r)
    	{
    	log ("advertising regime: " + config.advertising_regime);
    	if (config.advertising_regime != null)
	    	{
    		if (config.advertising_regime.equals ("direct-video"))
    			{		 
    			restore_video_location = false;
    			videoFragment.set_startup_function (in_main_thread, r);
    			int adnum = config.next_advert();
    			log ("next advert is: " + adnum);
    			String url = config.advert_meta (adnum, "url");
    			String id = config.advert_meta (adnum, "id");
    			String name = config.advert_meta (adnum, "name");
    			if (url != null && !url.equals (""))
	    			{
	    			launch_direct_ad (url, id, name);
	    			return true;
	    			}
    			}
    		else if (config.advertising_regime.equals ("admob"))
	    		{
		    	restore_video_location = false;
				videoFragment.set_startup_function (in_main_thread, r);
		    	admob_interstitial_advertisement (r);
		    	return true;
	    		}
	    	}
   		return false;
    	}
    
    public void launch_direct_ad (String video_url, String id, String name)
    	{
		View vMask = findViewById (R.id.top_mask);
		if (vMask != null)
			vMask.setVisibility (View.VISIBLE);
		
		Intent wIntent = new Intent (this, DirectAdvert.class);
		wIntent.putExtra ("tv.9x9.advert", video_url);
		wIntent.putExtra ("tv.9x9.advert_id", id);
		wIntent.putExtra ("tv.9x9.advert_name", name);
		startActivity (wIntent);
    	}
    
    public void admob_interstitial_advertisement (final Runnable r)
    	{
    	final InterstitialAd interstitial = new InterstitialAd (this);

    	String ADMOB_UNIT_ID = config.admob_key;
    	
	    interstitial.setAdUnitId (ADMOB_UNIT_ID);   

	    interstitial.setAdListener (new AdListener()
	    	{
            @Override
            public void onAdLoaded()
            	{
            	log ("interstitial ad loaded");
            	interstitial.show();
            	}

            @Override
            public void onAdClosed()
            	{
            	log ("interstitial ad closed");
                // in_main_thread.post (r);
            	}
	    	});
        
	    AdRequest adRequest = new AdRequest.Builder()
	    	.addTestDevice (AdRequest.DEVICE_ID_EMULATOR)
	    	.addTestDevice ("9B1327240A0F06351FD043013CDD9072")
	    	.build();
	    
	    interstitial.loadAd (adRequest);
    	}
    
	/*** ADVERT **************************************************************************************************/
    
    /* This isn't used. Moved it to its own activity (like Admob) */
    
    public void enable_advert_layer()
    	{
    	pause_video();
		disable_video_layer();
		
		// hide_both_fragments();
		
		set_layer (toplayer.ADVERT);
    	}
    
	/*** SHAKE **************************************************************************************************/
    
    public void enable_shake_layer()
    	{
		disable_video_layer();
		set_layer (toplayer.SHAKE);
		setup_shake_buttons();
		init_shake (false);
    	}
    
    Stack <String> shake_channel_stack = null;
    boolean shake_initialized = false;
    String shake_channels[] = null;
    
    public void init_shake (boolean force)
    	{
    	if (!shake_initialized || force)
    		{
    		shake_initialized = true;
    		
    		/* make sure there is at least something */
    		if (shake_channel_stack == null)
    			shake_channel_stack = new Stack <String> ();
    		
    		String query;
    		
    		if (config.usertoken != null)
    			query = "channelStack?stack=recommend&user=" + config.usertoken;
    		else
    			query = "channelStack?stack=recommend";
    		
    		new playerAPI (in_main_thread, config, query)
				{
				public void success (String[] lines)
					{
					log ("[shake] lines received: " + lines.length);
					config.parse_channel_info (lines);
					
					if (!shake_channel_stack.isEmpty())
						shake_channel_stack = new Stack <String> ();
		    		
					for (String line: lines)
						if (!line.equals (""))
							{
							String fields[] = line.split ("\t");
							shake_channel_stack.push (fields[1]);
							}
					}		
				public void failure (int code, String errtext)
					{
					alert ("ERROR! " + errtext);
					}
				};	
    		}
    	}
    
	public void setup_shake_buttons()
		{
		View vShake = findViewById (R.id.shakelayer);		
		if (vShake != null)
			vShake.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("shake layer ate my tap!");
		        	}
				});				
		}
  
	/*** ABOUT **************************************************************************************************/
    
    public void enable_about_layer()
    	{
		disable_video_layer();
		set_layer (toplayer.ABOUT);
		load_about_webview();
    	}
    
	public void load_about_webview()
		{
		WebView vAbout = (WebView) findViewById (R.id.about_content);
		if (vAbout != null)
			{
			/*
			vAbout.setWebViewClient (new WebViewClient()
				{
			    @Override
			    public boolean shouldOverrideUrlLoading (WebView view, String url)
			    	{
			    	return true;
			    	}
				});
			*/
			
			if (config.about_us_url != null)
				{
				if (config.about_us_url.startsWith ("http"))
					vAbout.loadUrl (config.about_us_url);
				else
					vAbout.loadDataWithBaseURL ("http://www.flipr.tv/", config.about_us_url, "text/html", "utf-8", null);
				}
			}
    	}
    
	public void setup_about_buttons()
		{
		View vAbout = findViewById (R.id.aboutlayer);		
		if (vAbout != null)
			vAbout.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("about layer ate my tap!");
		        	}
				});				
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
		
		track_layer (toplayer.STORE);
		}	
	
	public void store_init()
		{
		if (!store_initialized)
			{
			store_initialized = true;
			AbsListView vStore = (AbsListView) findViewById (is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
			// String category_id = category_list [current_category_index];
			// store_adapter = new StoreAdapter (this, (StoreAdapter.mothership) this, config, current_category_index, category_id, category_channels);
			store_adapter = new StoreAdapter (this, (StoreAdapter.mothership) this, config, -1, null, new String [0]);
			vStore.setAdapter (store_adapter);
			}
		}
	
	public void setup_store_buttons()
		{
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
		        	log ("store layer ate my tap!");
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
		final String query = "category";
				
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
				
		redraw_menu();
		
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
		
		final AbsListView vStore = (AbsListView) findViewById (is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
		final View vSpinner = findViewById (R.id.store_progress);
		
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
		
		new playerAPI (in_main_thread, config, query)
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
					finish();
					}
				}
	
			public void failure (int code, String errtext)
				{
				alert ("ERROR! " + errtext);
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
						parse_special_tags ("store", fields[1], category_id);
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
		
		thumbnail.stack_thumbs (main.this, config, category_channels, -1, in_main_thread, store_channel_thumb_updated);

		if (category_adapter != null)
			category_adapter.notifyDataSetChanged();
			
		set_store_category_name (category_id);
		
		AbsListView vStore = (AbsListView) findViewById (is_tablet() ? R.id.store_list_tablet : R.id.store_list_phone);
		vStore.setVisibility (View.VISIBLE);
		
		View vSpinner = findViewById (R.id.store_progress);
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
		
		TextView vCategoryName = (TextView) findViewById (R.id.category_name);
		if (vCategoryName != null)
			{
			vCategoryName.setText (txt_category_colon + " " + name);
			if (is_phone())
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
			vSearchContainer.setVisibility (View.INVISIBLE);
			View vLogo = vContainer.findViewById (R.id.logo);
			// View vPhantomRefresh = vContainer.findViewById (R.id.phantom_refresh);
			vLogo.setVisibility (View.VISIBLE);
			// vPhantomRefresh.setVisibility (View.INVISIBLE);
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
			search_adapter = new StoreAdapter (this, (StoreAdapter.mothership) this, config, -1, null, search_channels);
			AbsListView vSearch = (AbsListView) findViewById (is_tablet() ? R.id.search_list_tablet : R.id.search_list_phone);
			vSearch.setAdapter (search_adapter);
			}
		}
	
	public void setup_search_buttons()
		{
		View vSearchLayer = findViewById (R.id.searchlayer);		
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
				signout_from_app_or_facebook();
				config.api_server = fields[1] + ".flipr.tv";
				log ("switching API server to: " + fields[1]);
				String filedata = "api-server\t" + config.api_server + "\n" + "region\t" + config.region + "\n";
                futil.write_file (main.this, "config", filedata);
				alert_then_exit ("Please restart the app, to use API server " + config.api_server);
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

		store_spinner (true);
		prepare_search_screen (term);
		
		search_9x9_done = search_youtube_done = false;
		channel_ids_9x9 = null;
		channel_ids_youtube = null;
		
		track_event ("function", "submitSearch", term, 0);
				
		new playerAPI (in_main_thread, config, "search?text=" + encoded_term)
			{
			public void success (String[] chlines)
				{
				int count = 0;
	
				log ("search lines received: " + chlines.length);
	
				store_spinner (false);
				
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
			
		ytchannel.youtube_channel_search_in_thread (config, encoded_term, in_main_thread, new Callback()
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
			
			thumbnail.stack_thumbs (main.this, config, search_channels, screen_width / 5, in_main_thread, search_channel_thumb_updated);
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
				
				thumbnail.stack_thumbs (main.this, config, search_channels, screen_width / 5, in_main_thread, search_channel_thumb_updated);
				}
			};
		
		load_channel_then (channel_id, false, search_channel_special_inner, channel_id, null);
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
		enable_search_apparatus (R.id.sliding_top_bar);
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
		
	/*** MESSAGES **************************************************************************************************/
	    
	boolean messages_initialized = false;
	
	AbsListView messages_view = null;
	MessagesAdapter messages_adapter = null;
	
	public void enable_messages_layer()
		{
		disable_video_layer();
		set_layer (toplayer.MESSAGES);
		setup_messages_buttons();
		messages_init();
		messages_display_content();
		track_layer (toplayer.MESSAGES);
		}	
	
	public void messages_init()
		{
		if (!messages_initialized)
			{
			messages_initialized = true;
			messages_view = (AbsListView) findViewById (is_tablet() ? R.id.message_list_tablet : R.id.message_list_phone);
			messages_adapter = new MessagesAdapter (this, new message[] {});
			messages_view.setAdapter (messages_adapter);
					
			if (is_phone())
				{
				/* ListView supports footer but GridView does not */
	    		LayoutInflater inflater = main.this.getLayoutInflater();
	    		View shim = inflater.inflate (R.layout.footer_shim, null);
	    		((ListView) messages_view).addFooterView (shim);
				}
			}
		}
	
	public void setup_messages_buttons()
		{
		View vStore = findViewById (R.id.messagelayer);		
		if (vStore != null)
			vStore.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("message layer ate my tap!");
		        	}
				});				
		}
	
	public class message
		{
		String mso;
		String title;
		String description;
		String channel;
		String episode;
		String timestamp;
		String logo;
		}
	
	public message parse_notification_file (String ts)
		{
		message m = new message();
    	File configfile = new File (getFilesDir(), "notifications/" + ts);
    	try
    		{
    	    FileReader reader = new FileReader (configfile);
    	    BufferedReader br = new BufferedReader (reader);
    	    String line = br.readLine();    
    	    while (line != null)
    	    	{
    	    	while (line != null)
    	    		{
    	    		String fields[] = line.split ("\t");
    	    		if (fields.length >= 2)
    	    			{
    	    			log ("notification " + ts + " k: " + fields[0] + " v: " + fields[1]);
    					if (fields[0].equals ("title"))
    						m.title = fields[1];
    					if (fields[0].equals ("mso"))
    						m.mso = fields[1];    		
    					if (fields[0].equals ("channel"))
    						m.channel = fields[1];      
    					if (fields[0].equals ("episode"))
    						m.episode = fields[1];    
    					if (fields[0].equals ("text"))
    						m.description = fields[1];
    					if (fields[0].equals ("logo"))
    						m.logo = fields[1];
    	    			}
    	    		line = br.readLine();    	    		
    	    		}
    	    	}
    	    reader.close();
    	    m.timestamp = ts.replaceAll ("\\.seen", "");
    	    if (m.title == null)
    	    	m.title = getResources().getString (R.string.app_name);
    		}
    	catch (IOException ex)
    		{
    	    ex.printStackTrace();
    	    return null;
    		}
    	return m;
		}
	
	public Stack <message> messages_gather (boolean unseen_only)
		{
		File dir = new File (getFilesDir(), "notifications");
		
		File[] files = dir.listFiles();
	
		/* to allow filtering out duplicates */
		HashSet <String> message_de_duper = new HashSet <String> ();
		
		if (files != null)
			{
			/* sort in the reverse of what we need, because of Stack */
			Arrays.sort (files);
			}
		
		final Stack <message> stack = new Stack <message> ();
		
		if (files != null)
	        for (File f: files)
	        	{
	        	log ("file: " + f);
	        	String parts[] = f.toString().split ("/");
	        	/* last component of the filepath is mostly a timestamp */
	        	String ts = parts [parts.length-1];
	        	if (ts.endsWith (".seen"))
	        		{
	        		if (unseen_only)
	        			continue;
	        		}
	        	else
	        		{
	        		if (!unseen_only)
	        			{
		        		f.renameTo (new File (f.toString() + ".seen"));
		        		ts = ts + ".seen";
	        			}
	        		}
	        	message m = parse_notification_file (ts);
	        	if (m != null && m.title != null)
	        		{
	        		String ident = m.title + "\t" + m.description + "\t" + m.channel + "\t" + m.episode;
	        		if (!message_de_duper.contains (ident))
	        			{
	        			stack.push (m);
	        			message_de_duper.add (ident);
	        			}
	        		else
	        			log ("a very similar notification already exists -- filtered out");
	        		}
	        	}

		return stack;
		}
	
	public void messages_display_content()
		{
		final Stack <message> stack = messages_gather (false);
		
		final Stack <String> channels = new Stack <String> ();
		final Stack <String> unknown_channels = new Stack <String> ();
       
		/* for the UI badge */
		messages_count = 0;
		redraw_menu();
		
        int count = 0;
        message messages[] = new message [stack.size()];
        while (!stack.empty())
        	{
        	message m = stack.pop();
    		if (m.channel != null)
				{
				channels.push (m.channel);
				String id = config.pool_meta (m.channel, "id");
				if (id == null || id.equals (""))
					unknown_channels.push (m.channel);
				}
        	messages [count++] = m;
        	}
        
		log ("[notifications] total messages: " + stack.size());
		log ("[notifications] total channels: " + channels.size());
		log ("[notifications] total unknown channels: " + unknown_channels.size());
		
		View vNo = findViewById (R.id.no_new_messages);
		vNo.setVisibility (messages == null || messages.length == 0 ? View.VISIBLE : View.GONE);
		messages_view.setVisibility (messages == null || messages.length == 0 ? View.GONE : View.VISIBLE);

		if (messages_adapter != null)
			{
			messages_adapter.set_content (messages);			
			messages_view.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView parent, View v, int position, long id)
					{
					message m = messages_adapter.get_item (position);
					if (m.channel != null)
						{
			            String fake_set[] = new String[] { m.channel };
			            if (m.episode != null)
			            	launch_player (m.channel, m.episode, fake_set);
			            else
			            	launch_player (m.channel, fake_set);
						}
					else
						enable_home_layer();
					}
				});
			}
		
		final Runnable download_thumbs = new Runnable()
			{
			public void run()
				{
				if (channels.size() > 0)
					{
			        int count = 0;
			        String channel_list[] = new String [channels.size()];
			        while (!channels.empty())
			        	channel_list [count++] = channels.pop();
			    	
			        final Runnable update_notify_thumbs = new Runnable()
						{
						public void run()
							{
							messages_adapter.notifyDataSetChanged();
							}
						};
					
			        thumbnail.stack_thumbs (main.this, config, channel_list, -1, in_main_thread, update_notify_thumbs);
					}
				}
			};
		
		if (messages.length > 0)
			{
			if (unknown_channels.size() > 0)
				{
		        int uncount = 0;
		        String ids[] = new String [unknown_channels.size()];
		        while (!unknown_channels.empty())
		        	ids [uncount++] = unknown_channels.pop();
		        
		        String channel_ids = TextUtils.join (",", ids); 
		        
				new playerAPI (in_main_thread, config, "channelLineup?channel=" + channel_ids)
					{
					public void success (String[] chlines)
						{
						log ("load channel for notifications, lines received: " + chlines.length);
						config.parse_channel_info (chlines);
						download_thumbs.run();
						}
			
					public void failure (int code, String errtext)
						{
						log ("ERROR! " + errtext);
						}
					};
				}
			else
				download_thumbs.run();
			}
		}
		
	public class MessagesAdapter extends BaseAdapter
		{
		private Context context;
		private message messages[] = null;
		
		public MessagesAdapter (Context context, message messages[])
			{
			this.context = context;
			this.messages = messages;
			}

		public void set_content (message messages[])
			{
			this.messages = messages;
			notifyDataSetChanged();
			}
		
		public message get_item (int position)
			{
			return messages [position];
			}
			
		@Override
		public int getCount()
			{			
			// log ("getcount: " + messages.length);
			return messages == null ? 0 : messages.length;
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
					
			log ("messages getView: " + position + " (of " + getCount() + ")");
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (main.this, R.layout.message_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (messages [position].title);
			
			TextView vDesc = (TextView) rv.findViewById (R.id.desc);
			if (vDesc != null)
				vDesc.setText (messages [position].description);

			TextView vAgo = (TextView) rv.findViewById (R.id.message_ago);
			if (vAgo != null)
				{
				String ago = util.ageof (main.this, Long.parseLong (messages [position].timestamp) / 1000);
				vAgo.setText (ago);
				}
			
			boolean icon_found = false;
			String channel_id = messages [position].channel;
			
			ImageView vIcon = (ImageView) rv.findViewById (R.id.icon); 	
			if (vIcon != null)
				{
				if (channel_id != null)
					{
					log ("[notify] channel: " + channel_id);
					
					String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
						
					File f = new File (filename);
					if (f.exists())
						{
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							{
							icon_found = true;
							vIcon.setImageBitmap (bitmap);
							}
						}
					else
						log ("channel icon for " + channel_id + " not found");
					}
				else
					{
					icon_found = true;
					vIcon.setImageResource (R.drawable.home_channel);
					}
				}
			
			/* default should not be home_channel though */
			if (!icon_found)
				vIcon.setImageResource (R.drawable.home_channel);		
			
			if (is_tablet())
				{
				View vMessageBlock = rv.findViewById (R.id.message_block);
				if (vMessageBlock != null)
					{
					LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vMessageBlock.getLayoutParams(); 
					layout.rightMargin = pixels_4;
					layout.leftMargin = pixels_4;
					vMessageBlock.setLayoutParams (layout);
					}
				}
			return rv;
			}	
		}	

	/*** SETTINGS **************************************************************************************************/
		
	boolean original_notify_setting = false;
	boolean original_sound_setting = false;
	boolean original_vibrate_setting = false;
	
	public void enable_settings_layer()
		{
		disable_video_layer();
		zero_signin_data();
		set_layer (toplayer.SETTINGS);
		
		load_notification_settings();
		remember_notification_settings();

		redraw_settings();
		setup_settings_buttons();
		}	
	
	public void remember_notification_settings()
		{
		original_notify_setting = config.notifications_enabled;
		original_sound_setting = config.notify_with_sound;
		original_vibrate_setting = config.notify_with_vibrate;
		}

	public void restore_notification_settings()
		{
		config.notifications_enabled = original_notify_setting;
		config.notify_with_sound = original_sound_setting;
		config.notify_with_vibrate = original_vibrate_setting;
		}
	
	public void redraw_settings()
		{
		View vAccountSection = findViewById (R.id.account_section);
		if (vAccountSection != null)
			vAccountSection.setVisibility ((config.usertoken == null || config.email.equals ("[via Facebook]")) ? View.GONE : View.VISIBLE);
		
		View vPasswordSection = findViewById (R.id.password_section);
		if (vPasswordSection != null)
			vPasswordSection.setVisibility ((config.usertoken == null || config.email.equals ("[via Facebook]")) ? View.GONE : View.VISIBLE);

		View vAboutButton = findViewById (R.id.settings_about);
		if (vAboutButton != null)
			vAboutButton.setVisibility (config.about_us_url == null ? View.GONE : View.VISIBLE);

		View vAboutDivider = findViewById (R.id.settings_about_divider);
		if (vAboutDivider != null)
			vAboutDivider.setVisibility (config.about_us_url == null ? View.GONE : View.VISIBLE);
		
		TextView vVersion = (TextView) findViewById (R.id.version);
		if (vVersion != null)
			{
			String version_code = "[unknown]";			
			try
				{
				PackageInfo pInfo = getPackageManager().getPackageInfo (getPackageName(), 0);
				version_code = pInfo.versionName;
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}			
			String txt_version = getResources().getString (R.string.version);
			vVersion.setText (txt_version + " " + version_code);
			}
		
		TextView vNameReadonly = (TextView) findViewById (R.id.settings_name_readonly);
		if (vNameReadonly != null)
			vNameReadonly.setText (config.usertoken == null ? "" : config.username);

		TextView vEmailReadonly = (TextView) findViewById (R.id.settings_email_readonly);
		if (vEmailReadonly != null)
			vEmailReadonly.setText (config.usertoken == null ? "" : config.email);
		
		TextView vSettingsEmail = (TextView) findViewById (R.id.settings_email);
		if (vSettingsEmail != null)
			vSettingsEmail.setText (config.usertoken == null ? "" : config.email);
		
		String vService = Context.VIBRATOR_SERVICE;
		Vibrator vibrator = (Vibrator) getSystemService (vService);

		View vSoundSection = findViewById (R.id.sound_when_notified);
		if (vSoundSection != null)
			vSoundSection.setVisibility (config.notifications_enabled ? View.VISIBLE : View.GONE);
		
		View vSoundDivider = findViewById (R.id.sound_notifications_divider);
		if (vSoundDivider != null)
			vSoundDivider.setVisibility (config.notifications_enabled ? View.VISIBLE : View.GONE);
		
		View vVibrateSection = findViewById (R.id.vibrate_when_notified);
		if (vVibrateSection != null)
			vVibrateSection.setVisibility (config.notifications_enabled && vibrator.hasVibrator() ? View.VISIBLE : View.GONE);
		
		View vVibrateSection2 = findViewById (R.id.settings_vibrate);
		if (vVibrateSection2 != null)
			vVibrateSection2.setVisibility (config.notifications_enabled && vibrator.hasVibrator() ? View.VISIBLE : View.GONE);	
		
		View vVibrateDivider = findViewById (R.id.vibrate_notifications_divider);
		if (vVibrateDivider != null)
			vVibrateDivider.setVisibility (config.notifications_enabled && vibrator.hasVibrator() ? View.VISIBLE : View.GONE);

		View vVibrateDivider2 = findViewById (R.id.settings_vibrate_divider);
		if (vVibrateDivider2 != null)
			vVibrateDivider2.setVisibility (config.notifications_enabled && vibrator.hasVibrator() ? View.VISIBLE : View.GONE);
		
		ImageView vNotify = (ImageView) findViewById (R.id.enable_notifications_image);
		if (vNotify != null)
			vNotify.setImageResource (config.notifications_enabled ? R.drawable.check_checked_52 : R.drawable.check_unchecked_52);
		
		ImageView vVibrateWhen = (ImageView) findViewById (R.id.vibrate_when_notified_image);
		if (vVibrateWhen != null)
			vVibrateWhen.setImageResource (config.notify_with_vibrate ? R.drawable.check_checked_52 : R.drawable.check_unchecked_52);
		
		ImageView vSoundWhen = (ImageView) findViewById (R.id.sound_when_notified_image);
		if (vSoundWhen != null)
			vSoundWhen.setImageResource (config.notify_with_sound ? R.drawable.check_checked_52 : R.drawable.check_unchecked_52);
		
		Switch vNotifySwitch = (Switch) findViewById (R.id.settings_notification_switch);
		if (vNotifySwitch != null)
			{
			vNotifySwitch.setChecked (config.notifications_enabled);
			vNotifySwitch.setOnCheckedChangeListener (new OnCheckedChangeListener()
				{
				@Override
				public void onCheckedChanged (CompoundButton view, boolean isChecked)
					{
					config.notifications_enabled = isChecked;
					save_notification_settings();
					redraw_settings();
					}
				});
			}
		
		View vVibrate = findViewById (R.id.settings_vibrate);
		if (vVibrate != null)
			vVibrate.setAlpha (config.notifications_enabled ? 1.0f : 0.25f);
		
		Switch vVibrateSwitch = (Switch) findViewById (R.id.settings_vibrate_switch);
		if (vVibrateSwitch != null)
			{
			vVibrateSwitch.setClickable (config.notifications_enabled);
			vVibrateSwitch.setChecked (config.notify_with_vibrate);
			vVibrateSwitch.setOnCheckedChangeListener (new OnCheckedChangeListener()
				{
				@Override
				public void onCheckedChanged (CompoundButton view, boolean isChecked)
					{
					config.notify_with_vibrate = isChecked;
					save_notification_settings();
					}
				});			
			}
		
		View vSound = findViewById (R.id.settings_sound);
		if (vSound != null)
			vSound.setAlpha (config.notifications_enabled ? 1.0f : 0.25f);
		
		Switch vSoundSwitch = (Switch) findViewById (R.id.settings_sound_switch);
		if (vSoundSwitch != null)
			{
			vSoundSwitch.setClickable (config.notifications_enabled);
			vSoundSwitch.setChecked (config.notify_with_sound);
			vSoundSwitch.setOnCheckedChangeListener (new OnCheckedChangeListener()
				{
				@Override
				public void onCheckedChanged (CompoundButton view, boolean isChecked)
					{
					config.notify_with_sound = isChecked;
					save_notification_settings();
					}
				});				
			}
		}
	
	public void save_notification_settings()
		{
		log ("save notification_settings");
		String filedata = "notifications" + "\t" + (config.notifications_enabled ? "on" : "off") + "\n"
				+ "notify-with-sound" + "\t" + (config.notify_with_sound ? "on" : "off") + "\n"
				+ "notify-with-vibrate" + "\t" + (config.notify_with_vibrate ? "on" : "off") + "\n";
        futil.write_file (main.this, "config.notifications", filedata);
        log_notification_settings();
		}
	
	public void load_notification_settings()
		{
		String config_data = futil.read_file (this, "config.notifications");
		
		/* initialize */
		if (config_data.startsWith ("ERROR:"))
			{
			log ("initialize notifications file");
			config.notifications_enabled = true;
			config.notify_with_sound = config.notify_with_sound_default;
			config.notify_with_vibrate = config.notify_with_vibrate_default;
			log_notification_settings();
			save_notification_settings();
			return;
			}
		else
			{
			String lines[] = config_data.split ("\n");
			for (String line: lines)
				{
				String fields[] = line.split ("\t");
				if (fields.length >= 2)
					{
					log ("k: " + fields[0] + " v: " + fields[1]);
					if (fields[0].equals ("notifications"))
						config.notifications_enabled = fields[1].equals ("on");
					if (fields[0].equals ("notify-with-sound"))
						config.notify_with_sound = fields[1].equals ("on");
					if (fields[0].equals ("notify-with-vibrate"))
						config.notify_with_vibrate = fields[1].equals ("on");					
					}
				}
			log_notification_settings();
			}
		}
	
	public void log_notification_settings()
		{
		log ("notifications enabled: " + config.notifications_enabled);
		log ("notify_with_sound: " + config.notify_with_sound);				
		log ("notify_with_vibrate: " + config.notify_with_vibrate);		
		}
	
	public void setup_settings_buttons()
		{
		final View vLayer = findViewById (is_phone() ? R.id.settingslayer_phone : R.id.settingslayer_tablet);
		
		/*
		if (!is_tablet())
			{
			View vTopBar = vLayer.findViewById (R.id.settings_top_bar_resizable);
			LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vTopBar.getLayoutParams();
			layout.height = is_phone() ? pixels_60 : pixels_160;
			vTopBar.setLayoutParams (layout);		
			}
		*/
		
		View vBanner = findViewById (R.id.settings_banner);
		if (vBanner != null)
			vBanner.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings banner");
		        	restore_notification_settings();
		        	settings_exit();
		        	}
				});
		
		ImageView vAppIcon = (ImageView) findViewById (R.id.settings_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
		
		View vNotifications = findViewById (R.id.enable_notifications);
		if (vNotifications != null)
			vNotifications.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings enable notifications checkbox");
		        	config.notifications_enabled = !config.notifications_enabled;
		        	redraw_settings();;
		        	}
				});
		
		View vVibrateWhen = findViewById (R.id.vibrate_when_notified);
		if (vVibrateWhen != null)
			vVibrateWhen.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings vibrate checkbox");
		        	config.notify_with_vibrate = !config.notify_with_vibrate;
		        	redraw_settings();
		        	}
				});
		
		View vSoundWhen = findViewById (R.id.sound_when_notified);
		if (vSoundWhen != null)
			vSoundWhen.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings sound checkbox");
		        	config.notify_with_sound = !config.notify_with_sound;
		        	redraw_settings();
		        	}
				});	
		
		View vCancel = vLayer.findViewById (R.id.settings_cancel);
		if (vCancel != null)
			vCancel.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings cancel button");
		        	restore_notification_settings();
		        	settings_exit();
		        	}
				});	

		View vSave = vLayer.findViewById (R.id.settings_save);
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
		
		View vSignout = vLayer.findViewById (R.id.settings_signout);
		if (vSignout != null)
			vSignout.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings signout");
		        	signout_from_app_or_facebook();
		        	}
				});
		
		View vEdit = vLayer.findViewById (R.id.settings_edit);
		if (vEdit != null)
			vEdit.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings edit account");
		        	slide_in_password();
		        	}
				});		

		View vAbout = vLayer.findViewById (R.id.settings_about);
		if (vAbout != null)
			vAbout.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings about");
		        	enable_about_layer();
		        	}
				});
		
		View vTerms = vLayer.findViewById (R.id.settings_terms);
		if (vTerms != null)
			vTerms.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings terms");
		        	slide_in_terms();
		        	}
				});		

		View vPrivacy = vLayer.findViewById (R.id.settings_privacy);
		if (vPrivacy != null)
			vPrivacy.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings privacy");
		        	slide_in_privacy();
		        	}
				});	
		
		if (vLayer != null)
			vLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("settings layer ate my tap!");
		        	}
				});		
		}
	
	public void save_settings()
		{
		boolean nothing_changed = true;
		
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
		
		/* if true the final result of the "save" won't be known immediately */
		boolean will_background = true;
		
		if (config != null && config.usertoken != null && kk != null && !kk.equals (""))
			{
			new playerAPI (in_main_thread, config, "setUserProfile?user=" + config.usertoken + "&key=" + kk + "&value=" + vv)
				{
				public void success (String[] lines)
					{
					toast_by_resource (R.string.saved);
					settings_exit();
					}
				public void failure (int code, String errtext)
					{
					if (code == 201 || errtext.startsWith ("201"))
						toast_by_resource (R.string.current_password_wrong);
					else
						alert ("Failure saving your changes: " + errtext);
					}
				};
			nothing_changed = false;
			}
		else
			will_background = false;		
			
		if (original_notify_setting != config.notifications_enabled
				|| original_sound_setting != config.notify_with_sound
				|| original_vibrate_setting != config.notify_with_vibrate)
			{
			save_notification_settings();
			remember_notification_settings();
			toast_by_resource (R.string.saved);
			nothing_changed = false;
			}
		
		if (nothing_changed)
			toast_by_resource (R.string.nothing_changed);
		else if (!will_background)
			settings_exit();
		}
	
	public void settings_exit()
		{
		if (current_layer == toplayer.SETTINGS)
			{
			log ("settings exit");
			final View vLayer = findViewById (is_phone() ? R.id.settingslayer_phone : R.id.settingslayer_tablet);
	    	if (is_tablet())
	    		vLayer.setVisibility (View.GONE);
	    	else
	    		toggle_menu();
			}
		else if (current_layer == toplayer.PASSWORD)
			{
			log ("password exit");
			slide_away_password();
			}
		else
			{
			if (is_tablet())
				{
				final View vLayer = findViewById (is_phone() ? R.id.settingslayer_phone : R.id.settingslayer_tablet);
				vLayer.setVisibility (View.GONE);
				}
			else
				log ("current layer is: " + current_layer + ", stay in settings");
			}
		}
	
	/*** PASSWORD **************************************************************************************************/
	
	public void enable_password_layer()
		{
		disable_video_layer();
		zero_signin_data();
		set_layer (toplayer.PASSWORD);	
		redraw_settings();
		setup_password_buttons();
		}	
	
	public void slide_in_password()
		{
		if (is_phone())
			{
			toggle_menu (new Callback()
		    	{
		    	public void run()
		    		{
		    		enable_password_layer();
		    		toggle_menu();
		    		}
		    	});
			}
		else
			enable_password_layer();
		}
	
	public void slide_away_password()
		{
		if (is_phone())
			{
	    	toggle_menu (new Callback()
		    	{
		    	public void run()
		    		{
		    		enable_settings_layer();
		    		toggle_menu();
		    		}
		    	});
			}
		else
			enable_settings_layer();
		}
	
	public void setup_password_buttons()
		{
		View vCancel = findViewById (R.id.password_cancel);
		if (vCancel != null)
			vCancel.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("password cancel");
		        	slide_away_password();
		        	}
				});	

		View vSave = findViewById (R.id.password_save);
		if (vSave != null)
			vSave.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("password save");
		        	save_settings();
		        	}
				});	
		
		View vLayer = findViewById (is_phone() ? R.id.passwordlayer_phone : R.id.passwordlayer_tablet);
		if (vLayer != null)
			vLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("password layer ate my tap!");
		        	}
				});		
		}
	
	/*** MISC ******************************************************************************************************/
	
	boolean finger_is_down = false;
	
	@Override
	public void onActionDown()
		{
		finger_is_down = true;
		if (current_layer == toplayer.HOME)
			{
			if (home_slider != null)
				; // diminish_side_titles (home_slider.current_home_page, true);
			}
		}
	
	@Override
	public void onActionUp()
		{
		finger_is_down = false;
		if (current_layer == toplayer.HOME)
			{
			if (home_slider != null)
				diminish_side_titles (home_slider.current_home_page, false);
			}
		}
	
	@Override
	public void onActionMove (int deltaX, int deltaY)
		{
		if (current_layer == toplayer.HOME)
			{
			if (home_slider != null)
				diminish_side_titles (home_slider.current_home_page, deltaX > pixels_20);
			}	
		}		
	
	public void diminish_side_titles (View parent, boolean hide)
		{
		if (parent != null)
			{
			/*
			View vLeftSetTitle = parent.findViewById (R.id.left_set_title);
	        vLeftSetTitle.setVisibility (hide ? View.INVISIBLE : View.VISIBLE);
	        
	        View vRightSetTitle = parent.findViewById (R.id.right_set_title);
	        vRightSetTitle.setVisibility (hide ? View.INVISIBLE : View.VISIBLE);
	        */
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
	int max_delta_x = 0;
	
	@Override
	public void onBigThingDown()
		{
		log ("onBigThingStart");
		View vSliding = findViewById (R.id.slidingpanel);		
        FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vSliding.getLayoutParams();	
        big_thing_left_margin = layout.leftMargin;
        max_delta_x = 0;
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
        	/*
            if (deltaX < 0)
            	deltaX = 0;
            if (big_thing_left_margin - deltaX < 0)
            	deltaX = big_thing_left_margin;
            layout.leftMargin = big_thing_left_margin - deltaX;
            vSliding.setLayoutParams (layout);
            if (deltaX > Math.abs (max_delta_x))
            	max_delta_x = Math.abs (deltaX);
            */
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
			if (1 == 2)
			{
			log ("onBigThingUp max delta X: " + max_delta_x);
			
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
			}
		else
			{
			/* dragging to switch sets */
			}
		}
	
	/* Google Cloud Messaging */
	
    GoogleCloudMessaging gcm = null;
    
    String EXTRA_MESSAGE = "message";
    String PROPERTY_REG_ID = "registration_id";
    String PROPERTY_APP_VERSION = "appVersion";
    int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    
	public void gcm_register()
		{
		if (config != null && config.gcm_sender_id != null)
			{
	        gcm = GoogleCloudMessaging.getInstance (this);
	        String regid = get_gcm_registration_id (getApplicationContext());

	        if (regid.isEmpty())
	        	register_in_background();
			}
		}
		
	public String get_gcm_registration_id (Context context)
		{
	    SharedPreferences prefs = get_gcm_preferences (context);
	    
	    String registration_id = prefs.getString (PROPERTY_REG_ID, "");
	    if (registration_id.isEmpty())
	    	{
	        log ("gcm: registration not found");
	        return "";
	    	}
	    
	    int registered_version = prefs.getInt (PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int current_version = get_app_version (context);
	    if (current_version >= 0 && registered_version != current_version)
	    	{
	        log ("gcm: App version changed");
	        return "";
	    	}
	    
	    log ("gcm: obtained saved GCM id: " + registration_id);
	    return registration_id;
		}
	
	private SharedPreferences get_gcm_preferences (Context context)
		{
	    return getSharedPreferences (main.class.getSimpleName(), Context.MODE_PRIVATE);
		}
	
	private void store_registration_id (Context context, String regId)
		{
	    final SharedPreferences prefs = get_gcm_preferences (context);
	    int appVersion = get_app_version (context);
	    log ("gcm: saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString (PROPERTY_REG_ID, regId);
	    editor.putInt (PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
		}
	
	public int get_app_version (Context context)
		{
		try
			{
	        PackageInfo packageInfo = context.getPackageManager().getPackageInfo (context.getPackageName(), 0);
	        return packageInfo.versionCode;
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			return -1;
			}
		}
	
	public void register_in_background()
		{
        try
        	{
        	final Context context = getApplicationContext();
                
    		Thread t = new Thread()
				{
				public void run()
					{
					log ("in a thread");
					
					gcm = GoogleCloudMessaging.getInstance (context);
					
		            log ("trying to register with GCM sender id: " + config.gcm_sender_id);                     
		            String regid = null;
					try
						{
						regid = gcm.register (config.gcm_sender_id);
						}
					catch (Exception ex)
						{
						ex.printStackTrace();
						return;
						}
		            log ("gcm: device registered, registration ID=" + regid);
		            store_registration_id (getApplicationContext(), regid);
		            
					// http://api.flipr.tv/playerAPI/deviceRegister?mso=crashcourse&type=gcm&token=xxxxxxxx
		    		new playerAPI (in_main_thread, config, "deviceRegister?type=gcm&token=" + regid)
						{
						public void success (String[] lines)
							{
							log ("gcm: successfully registered GCM on 9x9 server");
							}
						public void failure (int code, String errtext)
							{
							log ("gcm: failure registering GCM: " + errtext);
							}
						};
					}
				};
			
			t.start();
        	}
        catch (Exception ex)
        	{
        	ex.printStackTrace();
        	}
		}
	
	private ShakeDetector mShakeDetector = null;
	private SensorManager mSensorManager = null;
	private Sensor mAccelerometer = null;
	   
	/* this does not reliable get turned off, probably because of MediaPlayer */
	private boolean shake_in_progress = false;
	
	public void shake_detect()
		{
        mSensorManager = (SensorManager) getSystemService (Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor (Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector(new ShakeDetector.OnShakeListener()
        	{
            @Override
            public void onShake()
            	{
            	if (current_layer == toplayer.SHAKE || previous_layer == toplayer.SHAKE)
	            	{            		            
	            	if (!shake_in_progress)
		            	{
	            		for (int i = 0; i < 10; i++)
	            			log ("******************** SHAKE SHAKE SHAKE! ********************");
		                // Vibrator v = (Vibrator) getSystemService (Context.VIBRATOR_SERVICE);
		                // v.vibrate (500);
		                // play_sound (R.raw.shake);
		                shake_in_progress = true;
		                in_main_thread.postDelayed (new Runnable()
		                	{
							@Override
							public void run()
								{
								shake_in_progress = false;
								}}, 5000);
		                play_sound ("shake.mp3");
		            	}
	            	else
	            		log ("******************** SHAKE ALREADY IN PROGRESS ********************");
	            	}
            	else
            		log ("shake: not in SHAKE or PLAYBACK (via SHAKE) mode");
            	}
        	});
		}
	
	public void shake_resume()
		{
		if (mSensorManager != null)
			mSensorManager.registerListener (mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
		}

	public void shake_pause()
		{
		if (mSensorManager != null)
			mSensorManager.unregisterListener (mShakeDetector);
		}
	
	public void play_sound (int sound_id)
		{
		MediaPlayer mp = MediaPlayer.create (main.this, sound_id);
        mp.start();
        mp.setOnCompletionListener (new OnCompletionListener()
        	{
            @Override
            public void onCompletion(MediaPlayer mp)
            	{
                mp.release();
                
                shake_in_progress = false;
                
                String shake_channel = null;
                String fake_set[] = null;
                
                if (shake_channel_stack == null || shake_channel_stack.size() <= 1)
                	init_shake (true);
                
                if (shake_channel_stack == null || shake_channel_stack.isEmpty())
                	shake_channel = "1029"; /* Ellen -- should refill here, or when count=1 remaining */
                else
                	shake_channel = shake_channel_stack.pop();

                /* a runt set of only one channel */
                fake_set = new String[] { shake_channel };
                
                set_arena (fake_set);
                change_channel (shake_channel);
            	}
        	});
		}
	
	MediaPlayer mp = null;
	
	public void play_sound (String asset_filename)
		{
		if (mp != null)
			{
			mp.release();
			}
		
		mp = new MediaPlayer();
		
		AssetFileDescriptor descriptor = null;
		try
			{
			descriptor = getAssets().openFd (asset_filename);
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			};

		if (descriptor == null)
			{
			mp.release();
			return;
			}
		
		try
			{
			mp.setDataSource (descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mp.release();
			return;
			}

		try 
			{
			mp.prepare();
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mp.release();
			}
		
		try
			{
		    descriptor.close();	
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			mp.release();
			return;
			}
	    
	    mp.start();
	    mp.setOnCompletionListener (new OnCompletionListener()
	    	{
	        @Override
	        public void onCompletion(MediaPlayer mp)
	        	{
	            mp.release();
	            
	            shake_in_progress = false;
	            
	            String shake_channel = null;
	            String fake_set[] = null;
	            
	            if (shake_channel_stack == null || shake_channel_stack.size() <= 1)
	            	init_shake (true);
	            
	            if (shake_channel_stack == null || shake_channel_stack.isEmpty())
	            	shake_channel = "1029"; /* "Ellen" channel -- should refill here, or when count=1 remaining */
	            else
	            	shake_channel = shake_channel_stack.pop();
	
	            /* a runt set of only one channel */
	            fake_set = new String[] { shake_channel };
	            
	            launch_player (shake_channel, fake_set);
	        	}
	    	});
		}    
	
	final Runnable interrupt_with_notification = new Runnable()
		{
		public void run()
			{
			in_main_thread.post (new Runnable()
				{
				@Override
				public void run()
					{
					log ("INTERRUPT WITH NOTIFICATION!");
					}
				});
			}
		};
	}