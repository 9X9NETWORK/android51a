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

import tv.tv9x9.player.MessagesLayer.OnMessagesListener;
import tv.tv9x9.player.SocialLayer.OnSocialListener;
import tv.tv9x9.player.StoreLayer.OnStoreListener;
import tv.tv9x9.player.SearchLayer.OnSearchListener;
import tv.tv9x9.player.SettingsLayer.OnSettingsListener;
import tv.tv9x9.player.GuideLayer.OnGuideListener;
import tv.tv9x9.player.AppsLayer.OnAppsListener;
import tv.tv9x9.player.FeedbackLayer.OnFeedbackListener;
import tv.tv9x9.player.PasswordLayer.OnPasswordListener;
import tv.tv9x9.player.HomeLayer.OnHomeListener;
import tv.tv9x9.player.NagLayer.OnNagListener;
import tv.tv9x9.player.SigninLayer.OnSigninListener;
import tv.tv9x9.player.ChatLayer.OnChatListener;

import tv.tv9x9.player.metadata.Bears;

import tv.tv9x9.player.AppsLayer.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
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
import android.view.ViewParent;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;

import com.facebook.*;
import com.facebook.Session.StatusCallback;
import com.facebook.android.Util;
import com.facebook.model.GraphUser;
import com.facebook.widget.*;

import com.flurry.android.FlurryAgent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.ads.*;
import com.android.vending.billing.IInAppBillingService; 

public class main extends VideoBaseActivity 
		implements OnMessagesListener, OnSocialListener, OnStoreListener, OnSearchListener, OnNagListener, OnSigninListener, OnChatListener,
						OnSettingsListener, OnGuideListener, OnAppsListener, OnFeedbackListener, OnPasswordListener, OnHomeListener
	{
	boolean single_channel = false;
	boolean single_episode = false;
	
	public main()
		{
		identity = "main";
		}
	
	/* note, SIGNOUT, HELP, CATEGORY_ITEM and APP_ITEM are not real layers, but menu items */
	enum toplayer { HOME, PLAYBACK, SIGNIN, GUIDE, STORE, SEARCH, SETTINGS, TERMS, APPS, SIGNOUT, CHAT,
					HELP, CATEGORY_ITEM, APP_ITEM, SHAKE, ABOUT, MESSAGES, NAG, TEST, PASSWORD, ADVERT, SOCIAL, FEEDBACK };
	
	toplayer current_layer = toplayer.HOME;
	
	String current_home_stack = null;
	
	Bundle fezbuk_bundle = null;
	
	/* the traveling window uses this as its right and bottom margin */
	int MARGINALIA = pixels_20;
	
	/* number of messages for the Messages layer */
	int messages_count = 0;
	
	/* a count of subscribes and unsubscribes, to optimize redrawing */
	int subscription_changes_this_session = 0;
	
	/* videos marked as "Watched", viewed successively -- used for hint trigger */
	int watched_videos_clicked_in_a_row = 0;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		adjust_layout_for_screen_size();
		home_configure_white_label();
		onVideoActivityLayout();
		setup_global_buttons();
		shake_detect();
		
		/* ugly hack because we need to call fezbuk1 in ready() not onCreate() */
		fezbuk_bundle = savedInstanceState;
		
		/* use recalculated value instead of just 20 */
		MARGINALIA = is_phone() ? pixels_10 : pixels_20;
		
		/* home layout needs to be told what it is */
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		set_screen_config (landscape);
		
		billing_create();
		}

	public void forcibly_remove_fb_buttons()
		{
		
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
		
		if (any_remembered_locations())
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
        billing_destroy();
        close_watch_db();
    	}    
    
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    	{
        super.onActivityResult (requestCode, resultCode, data);
        
        if (requestCode == 1911)
        	{
        	purchase_activity_result (requestCode, resultCode, data);
        	return;
    		}
        
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
	    	
	    	View vStoreHint = findViewById (R.id.home_store_hint);
	    	if (vStoreHint.getVisibility() == View.VISIBLE)
	    		{
    			log ("removing store hint");
	    		vStoreHint.setVisibility (View.GONE);
	    		return true;
	    		}
	    	
	    	if (is_tablet())
	    		{
				Fragment f = getSupportFragmentManager().findFragmentById (R.id.settings_fragment_container_tablet);		
				if (!f.isHidden())
					{
					log ("hide Settings fragment");
					disable_settings_layer();
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
	    	
	    	if (is_tablet())
	    		{
				Fragment f = getSupportFragmentManager().findFragmentById (R.id.password_fragment_container_tablet);		
				if (!f.isHidden())
					{
					log ("hide password fragment");
					disable_password_layer();
					return_to_settings_layer();
					return true;
					}    		
	    		}
	    	
	    	if (current_layer == toplayer.PLAYBACK)
	    		{
	    		track_event ("navigation", "back", "back", 0);
	    		
	    		if (any_remembered_locations())
	    			{
	    			log ("BACK in playback: return to remembered location");
	    			restore_location();	    			
	    			}
	    		else
	    			{
	    			if (us_market())
		    			{
	    				log ("BACK in playback: minimize");
	    				video_minimize (true);
		    			}
	    			else
		    			{
		    			log ("BACK in playback: full stop");
		    			player_full_stop (true);
		    			}
	    			}
	    		}
	    	else if (current_layer == toplayer.HOME)
	    		{
	    		if (menu_is_extended())
	    			toggle_menu();
	    		else
	    			exit_stage_left();
	    		}
	    	else if (current_layer == toplayer.STORE)
	    		{
	    		View vCategoryLayer = findViewById (R.id.category_layer);
	    		if (vCategoryLayer.getVisibility() == View.VISIBLE)
	    			store_class().toggle_category_layer();
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
	    		signin_class().zero_signin_data();
	    		}
	    	else if (current_layer == toplayer.ADVERT)
	    		{
	    		/* TODO: check for 6 seconds elapsed before allowing this */
	    		}
	    	else if (current_layer == toplayer.GUIDE || current_layer == toplayer.SEARCH 
	    				|| current_layer == toplayer.SETTINGS || current_layer == toplayer.APPS
	    				|| current_layer == toplayer.SHAKE || current_layer == toplayer.ABOUT
	    				|| current_layer == toplayer.MESSAGES || current_layer == toplayer.TEST
	    				|| current_layer == toplayer.FEEDBACK || current_layer == toplayer.SOCIAL
	    				|| current_layer == toplayer.CHAT)
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
			View vChannelIconSquare = findViewById (R.id.playback_channel_icon_square);
			LinearLayout.LayoutParams layout2 = (LinearLayout.LayoutParams) vChannelIconSquare.getLayoutParams();
			layout2.height = pixels_40;
			layout2.width = pixels_40;
			vChannelIconSquare.setLayoutParams (layout2);

			/* video_layer_new.xml */
			View vChannelIconCircle = findViewById (R.id.playback_channel_icon_circle);
			LinearLayout.LayoutParams layout2c = (LinearLayout.LayoutParams) vChannelIconCircle.getLayoutParams();
			layout2c.height = pixels_40;
			layout2c.width = pixels_40;
			vChannelIconCircle.setLayoutParams (layout2c);
			
			/* video_layer_new.xml */	
			View vChannelIconLandscapeSquare = findViewById (R.id.playback_channel_icon_landscape_square);
			LinearLayout.LayoutParams layout6 = (LinearLayout.LayoutParams) vChannelIconLandscapeSquare.getLayoutParams();
			layout6.height = pixels_40;
			layout6.width = pixels_40;
			vChannelIconLandscapeSquare.setLayoutParams (layout6);
			
			/* video_layer_new.xml */	
			View vChannelIconLandscapeCircle = findViewById (R.id.playback_channel_icon_landscape_circle);
			LinearLayout.LayoutParams layout6c = (LinearLayout.LayoutParams) vChannelIconLandscapeCircle.getLayoutParams();
			layout6c.height = pixels_40;
			layout6c.width = pixels_40;
			vChannelIconLandscapeCircle.setLayoutParams (layout6c);
			
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
		View vNotNeededSettingsLayer = findViewById (is_phone() ? R.id.settings_fragment_container_tablet : R.id.settings_fragment_container_phone);
		if (vNotNeededSettingsLayer != null)
			((ViewManager) vNotNeededSettingsLayer.getParent()).removeView (vNotNeededSettingsLayer);
		
		/* remove the phone/tablet sublayer which we won't use */
		View vNotNeededPasswordLayer = findViewById (is_phone() ? R.id.password_fragment_container_tablet : R.id.password_fragment_container_phone);
		if (vNotNeededPasswordLayer != null)
			((ViewManager) vNotNeededPasswordLayer.getParent()).removeView (vNotNeededPasswordLayer);
		
		/* this is a ListView */
		View vSearchListPhone = findViewById (R.id.search_list_phone);
		vSearchListPhone.setVisibility (is_tablet() ? View.GONE : View.VISIBLE);
		
		/* this is a GridView */
		View vSearchListTablet = findViewById (R.id.search_list_tablet);
		vSearchListTablet.setVisibility (is_tablet() ? View.VISIBLE : View.GONE);
		
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
	
	public Handler get_main_thread()
		{
		return in_main_thread;
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
		
		home_class().refresh();
		}
	
	@Override
	public void onVideoActivityFlingUp()
		{
		if (current_layer == toplayer.PLAYBACK)
			{
		   	if (!single_channel && !single_episode)
				{
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
		
		adjust_layouts_for_bear_type();
		
		/* redraw buttons, because config is now available and may disable some of them */
		setup_global_buttons();
		
		resume_as_logged_in_user();
		home_class().setup_home_buttons();
		populate_home();
		
		if (has_facebook())
			{
			fezbuk1 (fezbuk_bundle);
			fezbuk_bundle = null;
			}
		
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
		settings_class().load_notification_settings (config);
		
		GcmSetup gcmInstance = new GcmSetup (main.this, config, in_main_thread);
		gcmInstance.gcm_register();
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				get_pay_channel_info();
				}
			}, 4000);
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				/* ui needs the notification count for the Message layer */
				messages_count = messages_class().messages_gather (true).size();
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
		
		open_watch_db();
		}
	
	public void adjust_layouts_for_bear_type()
		{
		/* remove circle icons from USA layout, or square icons from Taiwan layout */
		
		int disappear_real_icon_id = us_market() ? R.id.real_channel_icon_circle : R.id.real_channel_icon_square;
		int disappear_portrait_icon_id = us_market() ? R.id.playback_channel_icon_circle : R.id.playback_channel_icon_square;
		int disappear_landscape_icon_id = us_market() ? R.id.playback_channel_icon_landscape_circle : R.id.playback_channel_icon_landscape_square;

		for (int icon: new Integer[] { disappear_real_icon_id, disappear_portrait_icon_id, disappear_landscape_icon_id })
			{
			View vIcon = findViewById (icon);
			if (vIcon != null)
				((ViewManager) vIcon.getParent()).removeView (vIcon);
			}
		}
	
	public void home_configure_white_label()
		{
		for (int top_bar: new Integer[] { R.id.sliding_top_bar })
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
		u = futil.read_file (this, "userid@" + config.api_server);
		if (!u.startsWith ("ERROR:"))
			{
			config.userid = u;
			log ("GOT SAVED USERID: " + config.userid);
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
	
	public int subscription_changes_this_session()
		{
		return subscription_changes_this_session;
		}
	
	public toplayer get_current_layer()
		{
		return current_layer;
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
			{
			if (config != null && config.search_on_off.equals ("off"))
				{
				/* search can be disabled on the server side */
				vSearch.setVisibility (View.GONE);
				}
			
			vSearch.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: global search button");
		        	View vBar = findViewById (R.id.sliding_top_bar);
		        	search_class().enable_search_apparatus (config, vBar);
		        	}
				});	
			}
		
		vSlidingTopBar.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	/* eat this */
	        	log ("click on: top bar");
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
		        // log ("menu-ANIM: " + layout.leftMargin);
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
	
	public void refresh_menu_adapter()
		{
		if (menu_adapter != null)
			menu_adapter.notifyDataSetChanged();
		}
	
	public void redraw_menu_if_created()
		{
		if (menu_adapter != null)
			redraw_menu();
		}
			
	public void redraw_menu()
		{
		if (config == null)
			return;
		
		if (!initialized_app_menu)
			{
			initialized_app_menu = true;
			in_main_thread.postDelayed (new Runnable()
				{
				@Override
				public void run()
					{
					apps_class().init_apps (config);
					}
				}, 15000);
			}
		
		setup_menu_buttons();
		
		/*
		 * if we want categories in the menu. note: bitrot
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
		
		if (config != null && !config.store_on_off.equals ("off"))
			items.push (new menuitem (toplayer.STORE, R.string.store, R.drawable.icon_store, R.drawable.icon_store_press));
	
		/*
		 * if we want categories in the menu. note: bitrot
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

		if (config.social_server != null)
			items.push (new menuitem (toplayer.SOCIAL, R.string.social, R.drawable.icon_social, R.drawable.icon_social_gray));

		if (config.chat_server != null && config.userid != null)
			items.push (new menuitem (toplayer.CHAT, R.string.chat, R.drawable.icon_chat, R.drawable.icon_chat));
		
		items.push (new menuitem (toplayer.FEEDBACK, R.string.feedback, R.drawable.icon_nav_feedback, R.drawable.icon_nav_feedback));
		
		/* no help screen has been provided yet */
		// items.push (new menuitem (toplayer.HELP, R.string.help, R.drawable.icon_help, R.drawable.icon_help));
		
		app[] recommended_apps = apps_class().get_recommended_apps();
		
		if (recommended_apps != null && recommended_apps.length > 0)
			{
			items.push (new menuitem (toplayer.APPS, R.string.suggested_tv_apps, R.drawable.icon_apps, R.drawable.icon_apps_press));
			if (apps_expanded)
				{			
				for (app a: recommended_apps)
					items.push (new menuitem (a));
				}
			}
		
		if (config.about_us_url != null && !config.about_us_url.contains ("http"))
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
				store_class().category_click (item.id);
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
				apps_class().launch_suggested_app (item.app_item.title, item.app_item.market_url);
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
			    
			case CHAT:
	        	close_menu();				
				log ("click on: menu chat");
				enable_chat_layer();
				break;
						
			case FEEDBACK:
				close_menu();
				log ("click on: menu feedback");
				enable_feedback_layer();
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
					app apps[] = apps_class().get_known_apps();
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
		
			/*
			 * obsolete code, also somewhat bitrot
			 * 
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
				
			 *
			 */
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
		signin_class().zero_signin_data();
		
		/* the settings view might be in the slider */
		settings_class().redraw_settings();
		}
	
	@Override
	public void onVideoActivitySignout()
		{
		redraw_menu();
		setup_menu_buttons();
		signin_class().zero_signin_data();
		/* the settings view might be in the slider */
		settings_class().redraw_settings();
		/* force the guide to redraw, or it keeps the old squares */
		guide_class().data_changed();
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
		if (us_market() && config.autoplay_setting)
			autoplay_next_channel();
		else
			player_full_stop (true);
		}
	
	public void autoplay_next_channel()
		{
		log ("autoplay to next channel");
		
		String next_id = next_channel_id();
		String channel_name = config.pool_meta (next_id, "name");
		
		video_message ("Next program playing in 6 seconds\n\nNext Program:\n\n" + channel_name);
		ugly_video_hack();
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				if (current_layer == toplayer.PLAYBACK)
					{
					View vTitlecard = findViewById (R.id.titlecard);
					if (vTitlecard.getVisibility() == View.VISIBLE)
						{
						log ("will now autoplay next channel");
						next_channel();
						return;
						}
					}
				log ("we seem to have moved on, will not autoplay next channel");
				}
			}, 6000);
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
		
		/* the "skip" already seen video button */
		update_skippistan();
		
		reset_video_size();
		}
	
	/* the "skip videos" hint/control just below the video, which can have three different states when enabled */
	public void update_skippistan()
		{
		View vSkippistan = findViewById (R.id.skippistan);
		if (vSkippistan != null)
			{
			int orientation = getResources().getConfiguration().orientation;
			boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
			
			if (landscape || config == null || !us_market())
				{
				vSkippistan.setVisibility (View.GONE);
				return;
				}
			if (!reconcile_program_line())
				{
				/* data is bad, nonexistent, or is being updated */
				vSkippistan.setVisibility (View.GONE);
				return;
				}

			String episode_id = program_line [current_episode_index - 1];
			boolean seen = was_video_seen (episode_id);
			vSkippistan.setVisibility (seen ? View.VISIBLE : View.GONE);
			
			if (is_phone())
				{
				/* not enough space with both displayed */
				View vAlreadySeen = findViewById (R.id.already_seen);
				vAlreadySeen.setVisibility (View.GONE);
				View vSkipAll = findViewById (R.id.skip_all);
				LinearLayout.LayoutParams skipLayout = (LinearLayout.LayoutParams) vSkipAll.getLayoutParams();
				skipLayout.leftMargin = pixels_20;
				vSkipAll.setLayoutParams (skipLayout);
				}
			
			if (seen)
				{
				final View vShould = findViewById (R.id.should_i_skip);
				final View vJust = findViewById (R.id.i_just_skipped);
				final View vThree = findViewById (R.id.three_in_a_row);
				
				if (config.skip_setting)
					{
					if (watched_videos_clicked_in_a_row >= 3)
						{
						log ("skippistan: three watched videos clicked in a row");
						vShould.setVisibility (View.GONE);
						vJust.setVisibility (View.GONE);
						vThree.setVisibility (View.VISIBLE);
						vSkippistan.setOnClickListener (new OnClickListener()
							{
					        @Override
					        public void onClick (View v)
					        	{
					        	log ("click on: skippistan: activate settings");
					        	player_full_stop (true);
					        	enable_settings_layer();
					        	}
							});
						}
					else
						{
						log ("skippistan: skip setting is on, do not display message");
						vSkippistan.setVisibility (View.GONE);
						}
					}
				else
					{
					log ("skippistan: video already seen, and skip setting is off");
					vShould.setVisibility (View.VISIBLE);
					vJust.setVisibility (View.GONE);
					vThree.setVisibility (View.GONE);
					vSkippistan.setOnClickListener (new OnClickListener()
						{
				        @Override
				        public void onClick (View v)
				        	{
				        	log ("click on: skippistan: user wants to skip");
				        	config.skip_setting = true;
				        	set_preference ("skip-setting", "on");
							vShould.setVisibility (View.GONE);
							vJust.setVisibility (View.VISIBLE);
							vThree.setVisibility (View.GONE);
				        	}
						});
					}
				}
			}
		}
	
	public int where_am_i()
		{
		int you_are_here = 0;
		
		if (arena != null && player_real_channel != null)
			{
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
			}
		return you_are_here;
		}
	
	public void next_channel()
		{
		String next_id = next_channel_id();
		log ("next channel id: " + next_id);		
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
					if (current_episode_index < program_line.length)
						{
						String episode_id = program_line [current_episode_index - 1];
						
						if (player_real_channel.contains (":"))				
							actual_channel_id = config.program_meta (episode_id, "real_channel");
						}
					else
						{
						log ("episode index out of range. current episode line:");
						config.dump_episode_line (program_line);
						}
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
		update_skippistan();
		
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
				if (config.equal_episodes (episode_id, potential_episode_id))
					{
					current_episode_index = i + 1;
					break;
					}
				}
			
			if (playback_episode_pager != null) ;
				playback_episode_pager.notifyDataSetChanged();
			}
		}
	
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

			/* loadData is too buggy. Must use loadDataWithBaseURL */
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
		        		remember_location();
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
	    			/* this channel is not in our database, and resides on YouTube only. add to database */
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
						guide_class().data_changed();
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
						guide_class().data_changed();
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

	@Override
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
			store_class().refresh_store_data();
			}
		else if (current_layer == toplayer.SEARCH)
			{
			search_class().search_refresh();
			}
		else if (current_layer == toplayer.GUIDE)
			{			
			set_follow_icon_state (R.id.guide_follow, channel_id, R.drawable.icon_heart, R.drawable.icon_heart_active);
			guide_class().data_changed();	
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
		
		home_class().refresh_home_slider();
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
	
	@Override
	public void set_layer (toplayer layer)
		{
		log ("set layer: " + layer.toString());

    	View vStoreHint = findViewById (R.id.home_store_hint);
    	vStoreHint.setVisibility (View.GONE);
    	
		/* overlay! */
		if (layer != toplayer.SIGNIN)
			{
			int frag = is_tablet() ? R.id.signin_fragment_container_tablet : R.id.signin_fragment_container_phone;			
			Fragment f = getSupportFragmentManager().findFragmentById (frag);		
			if (!f.isHidden())
				{
				log ("hide Signin fragment");
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.hide (f);  
		        ft.commit();
				}
			}
		else
			{
			int frag = is_tablet() ? R.id.signin_fragment_container_tablet : R.id.signin_fragment_container_phone;
			Fragment f = getSupportFragmentManager().findFragmentById (frag);		
			if (f.isHidden())
				{
				log ("show Signin fragment");
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.show (f);  
		        ft.commit();
				}
			
			/* note! this is an overlay, return */
			return;
			}
		
		/* overlay! */
		if (layer != toplayer.PASSWORD)
			{
			int frag = is_tablet() ? R.id.password_fragment_container_tablet : R.id.password_fragment_container_phone;			
			Fragment f = getSupportFragmentManager().findFragmentById (frag);		
			if (!f.isHidden())
				{
				log ("hide Password fragment");
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.hide (f);  
		        ft.commit();
				}
			}
		else
			{
			int frag = is_tablet() ? R.id.password_fragment_container_tablet : R.id.password_fragment_container_phone;
			Fragment f = getSupportFragmentManager().findFragmentById (frag);		
			if (f.isHidden())
				{
				log ("show Password fragment");
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.show (f);  
		        ft.commit();
				}
			
			/* note! this is an overlay, return */
			return;
			}
	
		/* overlay! */
		if (layer != toplayer.SETTINGS)
			{
			int frag = is_tablet() ? R.id.settings_fragment_container_tablet : R.id.settings_fragment_container_phone;			
			Fragment f = getSupportFragmentManager().findFragmentById (frag);		
			if (!f.isHidden())
				{
				log ("hide Settings fragment");
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.hide (f);  
		        ft.commit();
				}
			}
		else
			{
			int frag = is_tablet() ? R.id.settings_fragment_container_tablet : R.id.settings_fragment_container_phone;
			Fragment f = getSupportFragmentManager().findFragmentById (frag);		
			if (f.isHidden())
				{
				log ("show Settings fragment");
		        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		        ft.show (f);  
		        ft.commit();
				}
			
			if (is_phone())
				{
				/* for phone version, if this isn't by default GONE, the settings layer flashes briefly on startup */
				View vSettings = findViewById (R.id.settingslayer_phone);
				vSettings.setVisibility (View.VISIBLE);
				}
			/* note! this is an overlay, return (for tablet at least) */
			return;
			}

		if (layer != toplayer.SOCIAL)
			{
			social_class().close();
			}
	
		if (layer != toplayer.CHAT)
			{
			chat_class().close();
			}
		
		Fragment f;
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction(); 
		
		f = getSupportFragmentManager().findFragmentById (R.id.message_fragment_container);
		if (layer == toplayer.MESSAGES) ft.show (f); else ft.hide (f);
		
		f = getSupportFragmentManager().findFragmentById (R.id.feedback_fragment_container);
		if (layer == toplayer.FEEDBACK) ft.show (f); else ft.hide (f);

		f = getSupportFragmentManager().findFragmentById (R.id.social_fragment_container);
		if (layer == toplayer.SOCIAL) ft.show (f); else ft.hide (f);

		f = getSupportFragmentManager().findFragmentById (R.id.store_fragment_container);
		if (layer == toplayer.STORE) ft.show (f); else ft.hide (f);

		f = getSupportFragmentManager().findFragmentById (R.id.guide_fragment_container);
		if (layer == toplayer.GUIDE) ft.show (f); else ft.hide (f);
		
		f = getSupportFragmentManager().findFragmentById (R.id.apps_fragment_container);
		if (layer == toplayer.APPS) ft.show (f); else ft.hide (f);

		f = getSupportFragmentManager().findFragmentById (R.id.nag_fragment_container);
		if (layer == toplayer.NAG) ft.show (f); else ft.hide (f);
		
		f = getSupportFragmentManager().findFragmentById (R.id.chat_fragment_container);
		if (layer == toplayer.CHAT) ft.show (f); else ft.hide (f);
		
		ft.commit();
		
		
		View vTopBar = findViewById (R.id.sliding_top_bar);
		vTopBar.setVisibility (layer == toplayer.TERMS || layer == toplayer.FEEDBACK ? View.GONE : View.VISIBLE);
		
		View home_layer = findViewById (R.id.home_layer);
		home_layer.setVisibility (layer == toplayer.HOME ? View.VISIBLE : View.GONE);

		View guide_layer = findViewById (R.id.guidelayer);
		guide_layer.setVisibility (layer == toplayer.GUIDE ? View.VISIBLE : View.GONE);

		View search_layer = findViewById (R.id.searchlayer);
		search_layer.setVisibility (layer == toplayer.SEARCH ? View.VISIBLE : View.GONE);	
		
		View terms_layer = findViewById (R.id.termslayer_new);
		terms_layer.setVisibility (layer == toplayer.TERMS ? View.VISIBLE : View.GONE);
		
		View signin_layer = findViewById (is_phone() ? R.id.signinlayer_phone : R.id.signinlayer_tablet); // TODO FIX
		if (signin_layer != null)
			signin_layer.setVisibility (layer == toplayer.SIGNIN ? View.VISIBLE : View.GONE);

		View messages_layer = findViewById (R.id.messagelayer);
		messages_layer.setVisibility (layer == toplayer.MESSAGES ? View.VISIBLE : View.GONE);
		
		View about_layer = findViewById (R.id.aboutlayer);
		about_layer.setVisibility (layer == toplayer.ABOUT ? View.VISIBLE : View.GONE);
		
		View shake_layer = findViewById (R.id.shakelayer);
		shake_layer.setVisibility (layer == toplayer.SHAKE ? View.VISIBLE : View.GONE);
		
		View test_layer = findViewById (R.id.testlayer);
		test_layer.setVisibility (layer == toplayer.TEST ? View.VISIBLE : View.GONE);
		
		View social_layer = findViewById (R.id.sociallayer);
		social_layer.setVisibility (layer == toplayer.SOCIAL ? View.VISIBLE : View.GONE);
		
		View feedback_layer = findViewById (R.id.feedbacklayer);
		feedback_layer.setVisibility (layer == toplayer.FEEDBACK ? View.VISIBLE : View.GONE);		
		
		View chat_layer = findViewById (R.id.chatlayer);
		chat_layer.setVisibility (layer == toplayer.CHAT ? View.VISIBLE : View.GONE);
		
		current_layer = layer;
		
		redraw_menu();
		}
	
	/*** SIGNIN ************************************************************************************************/
	
	public void enable_signin_layer (Runnable callback)
		{
		disable_video_layer();
		
		/* terms layer can only be started from signin, so ignore it */
		if (current_layer != toplayer.TERMS && current_layer != toplayer.SIGNIN && current_layer != toplayer.NAG)
			signin_class().set_layer_before_signin (current_layer);
		
		set_layer (toplayer.SIGNIN);		

		signin_class().init (config);
		signin_class().set_signin_layer_callback (callback);
		signin_class().setup_signin_layer_buttons (callback);

		track_layer (toplayer.SIGNIN);
		}

    public SigninLayer signin_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
		int frag = is_tablet() ? R.id.signin_fragment_container_tablet : R.id.signin_fragment_container_phone;
		Fragment f = fm.findFragmentById (frag);	
	    return (tv.tv9x9.player.SigninLayer) f;
		}
    
	/*** TERMS *****************************************************************************************************/
	
	toplayer terms_previous_layer;
	
	public void enable_terms_layer()
		{
		enable_terms_layer (current_layer);
		}
	
	public void enable_terms_layer (toplayer layer)
		{
		disable_video_layer();
		
		if (layer != toplayer.TERMS)
			terms_previous_layer = layer;
		set_layer (toplayer.TERMS);
		
		setup_terms_buttons();
		terms_tab();
		
		/* sometimes the terms layer background is not redrawing! bugs with Android WebView. force it here */
		View vTermsLayer = findViewById (R.id.termslayer_new);
		vTermsLayer.postInvalidate();
		
		/* and even then that doesn't always work! Also the only purpose a "postInvalidateDelayed" method
		   can possibly have is as workarounds for Android layout bugs exactly like this */		
		for (int i: new int[] { 500, 1000, 5000 })
			vTermsLayer.postInvalidateDelayed (i);
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
		slide_in_terms (current_layer);
		}
	
	public void slide_in_terms (final toplayer layer)
		{
		toggle_menu (new Callback()
	    	{
	    	public void run()
	    		{
	    		enable_terms_layer (layer);
	    		terms_tab();
	    		toggle_menu();
	    		}
	    	});
		}

	public void slide_in_privacy()
		{
		slide_in_privacy (current_layer);
		}
	
	public void slide_in_privacy (final toplayer layer)
		{
		toggle_menu (new Callback()
	    	{
	    	public void run()
	    		{
	    		enable_terms_layer (layer);
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
	    		log ("terms: setting previous layer: " + terms_previous_layer);
	    		set_layer (terms_previous_layer);
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
		String language = get_language_en_or_zh();
		
		String url = "http://mobile.flipr.tv/android/" + mso + "/support/" + device_type() + "/" + language + "/";
		if (action != null)
			{
			if (action.equals ("terms") || action.equals ("privacy"))
				url = "http://mobile.flipr.tv/" + mso + "/" + action + "/" + language + "/";
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
	
    public boolean signed_in_with_facebook()
    	{
    	return signed_in_with_facebook;
    	}
    
    public void set_signed_in_with_facebook (boolean value)
    	{
    	signed_in_with_facebook = value;
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
	
	private Session.StatusCallback fb_callback = null;
	
	public void init_fb_callbacks()
		{
		fb_callback = new Session.StatusCallback()
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
		}
	private void onSessionStateChange (Session session, SessionState state, Exception exception)
		{
		if (exception instanceof FacebookOperationCanceledException)
			log ("FB: FacebookOperationCanceledException");
		else if (exception instanceof FacebookAuthorizationException)
			log ("FB: FacebookAuthorizationException");
	    }
    
	public void onFacebookLayout (View parent)
		{
		if (!has_facebook())
			{
			for (final int button: new int[] { R.id.fblogin, R.id.nag2_fblogin, R.id.nag3_fblogin })
				{
				View v = parent.findViewById (button);
				if (v != null)
					((ViewManager) v.getParent()).removeView (v);
				}
			}
		}
	
	public void fezbuk1 (Bundle savedInstanceState)
		{
		if (has_facebook() && config.facebook_app_id != null)
			{
			init_fb_callbacks();
			session = new Session.Builder (this).setApplicationId (config.facebook_app_id).build();
	        uiHelper = new UiLifecycleHelper (this, fb_callback);
	        if (uiHelper != null)
	        	uiHelper.onCreate (savedInstanceState);
			}
		}
	
	public void fezbuk2 (View parent)
		{
		if (has_facebook())
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
					signin_class().process_login_data ("[via Facebook]", lines);
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
								activate_layer (signin_class().get_layer_before_signin());
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
		signin_class().zero_signin_data();
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
		apps_class().init_apps (config);
		apps_class().setup_apps_buttons();
		track_layer (toplayer.APPS);
		}
	
    public AppsLayer apps_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.apps_fragment_container);
	    return (tv.tv9x9.player.AppsLayer) f;
		}
	
	/*** NAG **************************************************************************************************/

	public void enable_nag_layer()
		{
		disable_video_layer();
		set_layer (toplayer.NAG);		
		signin_class().set_layer_before_signin (toplayer.HOME);
		nag_class().init (config);
		}
	
    public NagLayer nag_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.nag_fragment_container);
	    return (tv.tv9x9.player.NagLayer) f;
		}

	/*** HOME **************************************************************************************************/
	
	public void enable_home_layer()
		{
		disable_video_layer();
		set_layer (toplayer.HOME);
		
		if (home_class().have_portal_stack())
			{
			reset_arena_to_home();
			home_class().init (config);					
			home_class().create_set_slider();
			home_class().position_set_slider();
			}
		
		track_layer (toplayer.HOME);
		
		home_class().home_hint_animation();
		home_class().refresh();
		}
	
	public void populate_home()
		{
		home_class().portal_frontpage (config);
		}
		
    public HomeLayer home_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.home_fragment_container);
	    return (tv.tv9x9.player.HomeLayer) f;
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
	
    /* shared by HomeLayer and StoreLayer */
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
	
	public void enable_home_store_hint()
		{
		final View vHint = findViewById (R.id.home_store_hint);
		vHint.setVisibility (View.VISIBLE);
				
		if (is_phone())
			{
			View vBox = findViewById (R.id.home_store_hint_box);
			FrameLayout.LayoutParams skipLayout = (FrameLayout.LayoutParams) vBox.getLayoutParams();
			skipLayout.rightMargin = 0;
			vBox.setLayoutParams (skipLayout);
			}
		
		final View vButton = findViewById (R.id.hint_menu_button);
		vButton.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	vHint.setVisibility (View.GONE);
				toggle_menu (new Callback()
			    	{
			    	public void run()
			    		{
			    		enable_store_layer();
			    		toggle_menu();
			    		}
			    	});
	        	}
			});
		
		vHint.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	vHint.setVisibility (View.GONE);
	        	}
			});	
		}
	
	/*** HINTS **************************************************************************************************/

	public void playback_hint()
		{
		if (!seen_bouncy_playback_hint)
			{
			load_hint_settings();
			
			if (seen_bouncy_playback_hint)
				return;
			
			seen_bouncy_playback_hint = true;
			save_hint_settings();

			in_main_thread.post (new Runnable()
				{
				@Override
				public void run()
					{
					if (us_market())
						ordinary_playback_hint();
					else
						bouncy_playback_hint_animation();
					}
				});
			}
		}
	
	public void ordinary_playback_hint()
		{
		final View vHint = findViewById (R.id.playback_subscribe_hint);
		
		final View vHoriz = findViewById (R.id.playback_horiz);
		
		final int bottom_base = vHoriz.getVisibility() == View.VISIBLE ? vHoriz.getHeight() : 0;
		
		/* this error sometimes occurs. It is impossible! we have no RelativeLayout anywhere */
		/* java.lang.ClassCastException: android.widget.RelativeLayout$LayoutParams cannot be cast to android.widget.FrameLayout$LayoutParams */
		
		/* tried to fix this, and this happened */
		/* java.lang.ClassCastException: android.widget.FrameLayout$LayoutParams cannot be cast to android.widget.LinearLayout$LayoutParams */
		
		// final LayoutParams container_layout = (LayoutParams) vHint.getLayoutParams();
		
		ViewParent vParent = vHint.getParent();
		
		if (vParent instanceof FrameLayout)
			{
			log ("hint parent is FrameLayout");
			final FrameLayout.LayoutParams container_layout = (FrameLayout.LayoutParams) vHint.getLayoutParams();
			container_layout.bottomMargin = bottom_base;
			vHint.setLayoutParams (container_layout);
			}
		else if (vParent instanceof RelativeLayout)
			{
			log ("hint parent is RelativeLayout");
			final RelativeLayout.LayoutParams container_layout = (RelativeLayout.LayoutParams) vHint.getLayoutParams();
			container_layout.bottomMargin = bottom_base;
			vHint.setLayoutParams (container_layout);
			}
		else
			{
			log ("hint parent neither is FrameLayout nor RelativeLayout, giving up");
			vHint.setVisibility (View.GONE);
			return;
			}
		
		vHint.setVisibility (View.VISIBLE);
		
		in_main_thread.postDelayed (new Runnable()
			{
			@Override
			public void run()
				{
				vHint.setVisibility (View.GONE);
				}
			}, 6000);
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
		
	    as.play (animFI);
	    as.play (halfUP).after (animFI);
	    as.play (fullDOWN1).after (halfUP);
	    as.play (fullUP1).after (fullDOWN1);
	    as.play (fullDOWN2).after (fullUP1);
	    as.play (fullUP2).after (fullDOWN2);
	    as.play (halfDOWN).after (fullUP2);	    
	    as.play (animFO).after (halfDOWN);
	     
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
    
	boolean loaded_hint_settings = false;
	
	boolean seen_bouncy_home_hint = false;
	boolean seen_bouncy_playback_hint = false;
	boolean seen_visit_store_hint = false;
	
	public boolean get_hint_setting (String hint)
		{
		if (!loaded_hint_settings)
			load_hint_settings();

		if (hint.equals ("seen-bouncy-home-hint"))
			return seen_bouncy_home_hint;
		else if (hint.equals ("seen-bouncy-playback-hint"))
			return seen_bouncy_playback_hint;
		else if (hint.equals ("seen-visit-store-hint"))
			return seen_visit_store_hint;
		else
			return false;
		}

	public void set_hint_setting (String hint, boolean value)
		{
		if (!loaded_hint_settings)
			load_hint_settings();
	
		if (hint.equals ("seen-bouncy-home-hint"))
			seen_bouncy_home_hint = value;
		else if (hint.equals ("seen-bouncy-playback-hint"))
			seen_bouncy_playback_hint = value;
		else if (hint.equals ("seen-visit-store-hint"))
			seen_visit_store_hint = value;
		
		save_hint_settings();
		}
	
	public void save_hint_settings()
		{
		log ("save hint_settings");
		String filedata = "seen-bouncy-home-hint" + "\t" + (seen_bouncy_home_hint ? "yes" : "no") + "\n"
				        + "seen-bouncy-playback-hint" + "\t" + (seen_bouncy_playback_hint ? "yes" : "no") + "\n"
				        + "seen-visit-store-hint" + "\t" + (seen_visit_store_hint ? "yes" : "no") + "\n";
        futil.write_file (main.this, "config.hints", filedata);
		}
	
	public void load_hint_settings()
		{
		String config_data = futil.read_file (this, "config.hints");
		
		/* initialize */
		if (config_data.startsWith ("ERROR:"))
			{
			log ("initialize notifications file");
			seen_bouncy_home_hint = false;
			seen_bouncy_playback_hint = false;
			seen_visit_store_hint = false;
			save_hint_settings();
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
					if (fields[0].equals ("seen-bouncy-home-hint"))
						seen_bouncy_home_hint = fields[1].equals ("yes");
					if (fields[0].equals ("seen-bouncy-playback-hint"))
						seen_bouncy_playback_hint = fields[1].equals ("yes");			
					if (fields[0].equals ("seen-visit-store-hint"))
						seen_visit_store_hint = fields[1].equals ("yes");						
					}
				}
			}
		
		loaded_hint_settings = true;
		}
	
	/* ------------------------------------ frontpage ------------------------------------ */

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
				guide_class().episode_thumbs_updated.run();
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
	
	public EpisodeSlider NEW_setup_horiz (ViewPager horiz, final String channel_id)
		{
		final EpisodeSlider horiz_adapter = new EpisodeSlider (horiz, channel_id, new String[0]);
				       
		if (horiz == null)
			return null;
		
		horiz.setAdapter (horiz_adapter);
		log ("new setup horiz load channel: " + channel_id);
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

	@SuppressLint ("NewApi")
	@SuppressWarnings ("deprecation")
	public void setImageAlpha (ImageView vImage, int alpha)
		{
		if (android.os.Build.VERSION.SDK_INT >= 16)
			{
			vImage.setImageAlpha (alpha);
			}
		else
			{
			vImage.setAlpha (alpha);
			}
		}
	
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
			
			setImageAlpha (vPic, position == current_episode_index -1 ? 0xFF : 0xA0);

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
	
 	/*** PLAYER **************************************************************************************************/
	    
	PlaybackChannelAdapter playback_channel_adapter = null;
	// PlaybackCommentsAdapter playback_comments_adapter = null;
	
	public void play_channel (final String channel_id)
		{
		enable_player_layer (channel_id, new Runnable()
			{
			@Override
			public void run()
				{
				in_main_thread.postDelayed (new Runnable()
					{
					@Override
					public void run()
						{
						playback_hint();
						}
					}, 3000);
				play_first (channel_id);
				}
			});
		}
	
	public void play_episode_in_channel (final String channel_id, final String episode_id)
		{
		enable_player_layer (channel_id, new Runnable()
			{
			@Override
			public void run()
				{
				log ("Play episode: " + episode_id + " in channel " + channel_id);
				play (channel_id, episode_id);
				}
			});
		}
	
	@Override
	public void play_nth_episode_in_channel (final String channel_id, final int position)
		{
		program_line = config.program_line_by_id (channel_id);
		if (program_line != null)
			{
			if (position <= program_line.length)
				{
				final String episode_id = program_line [position - 1];
				
				enable_player_layer (channel_id, new Runnable()
					{
					@Override
					public void run()
						{
						redraw_playback (channel_id, episode_id);
						play_nth (channel_id, position);
						}
					});
				}
			else
				config.dump_episode_line (program_line);
			}
		}

	final Runnable playback_episode_thumb_updated = new Runnable()
		{
		public void run()
			{
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
	
	Set <String> real_channel_load_requests = new HashSet <String> ();
	Set <String> real_channel_thumb_download_requests = new HashSet <String> ();
	
	public void update_channel_icon (final String channel_id)
		{
		int real_icon_id = us_market() ? R.id.real_channel_icon_square : R.id.real_channel_icon_circle;
		int portrait_icon_id = us_market() ? R.id.playback_channel_icon_square : R.id.playback_channel_icon_circle;
		int landscape_icon_id = us_market() ? R.id.playback_channel_icon_landscape_square : R.id.playback_channel_icon_landscape_circle;		
		ImageView vChannelIcon = (ImageView) findViewById (portrait_icon_id);
		ImageView vChannelIconLandscape = (ImageView) findViewById (landscape_icon_id);
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
		
		View vVirtualLink = findViewById (R.id.virtual_link);
		if (vVirtualLink != null)
			{			
			program_line = config.program_line_by_id (channel_id);
			
			if (program_line == null || current_episode_index > program_line.length)
				{
				vVirtualLink.setVisibility (View.GONE);
				return;
				}
			
			if (!reconcile_program_line())
				return;
			
			String episode_id = program_line [current_episode_index - 1];
			final String real_channel = config.program_meta (episode_id, "real_channel");		
			
			if (real_channel != null && !real_channel.equals ("") && !real_channel.equals (channel_id))
				{
				String filename = getFilesDir() + "/" + config.api_server + "/cthumbs/" + real_channel + ".png";
				File f = new File (filename);
				boolean thumb_exists = f.exists();
				
				final View vRealChannelProgress = findViewById (R.id.real_channel_progress);				
				final ImageView vRealChannelIcon = (ImageView) findViewById (real_icon_id);	
				
				if (!thumb_exists)
					{
					/* spinner! */
					}
				
				if (!config.channel_loaded (real_channel))
					{
					vRealChannelProgress.setVisibility (View.VISIBLE);
					vRealChannelIcon.setVisibility (View.GONE);
					
					vVirtualLink.setOnClickListener (new OnClickListener()
						{
				        @Override
				        public void onClick (View v)
				        	{
				        	/* disable taps until channel is loaded */
				        	}
						});	
					
					final Callback after_real_load = new Callback()
						{
						public void run_string_and_object (String channel_id, Object o)
							{
							update_channel_icon (channel_id);
							vRealChannelProgress.setVisibility (View.GONE);
							vRealChannelIcon.setVisibility (View.VISIBLE);
							}
						};
					
					if (!real_channel_load_requests.contains (real_channel))
						{
						log ("requesting real channel information for: " + real_channel + " (channel=" + channel_id + ", current episode_index=" + current_episode_index + ")");
						// config.dump_episode_line (program_line);
						real_channel_load_requests.add (real_channel);
						load_channel_then (real_channel, false, after_real_load, channel_id, null);
						}
					}
				else
					{
					vRealChannelProgress.setVisibility (View.GONE);
					vRealChannelIcon.setVisibility (View.VISIBLE);
					
					vVirtualLink.setOnClickListener (new OnClickListener()
						{
				        @Override
				        public void onClick (View v)
				        	{
				        	log ("click on: virtual channel link");
				        	/* launch channel but with a set only of itself */
				        	launch_player (real_channel, new String[] { real_channel });
				        	}
						});	
					}
				
				String name = config.pool_meta (real_channel, "name");
				
				TextView vRealName = (TextView) findViewById (R.id.real_channel_name);
				vRealName.setText (name != null ? name : "");
					
				if (thumb_exists)
					{
					Bitmap bitmap = BitmapFactory.decodeFile (filename);
					if (bitmap != null)
						{
						Bitmap bitmap2 = bitmappery.getRoundedCornerBitmap (bitmap, 70);
						if (bitmap2 != null)
							{
							vRealChannelIcon.setImageBitmap (bitmap2);
							}
						}			
					}
				else
					{
					/* don't have a thumb. download one if possible */
					String thumb = config.pool_meta (real_channel, "thumb");
					if (thumb != null && !thumb.equals ("") && !real_channel_thumb_download_requests.contains (real_channel))
						{
						real_channel_thumb_download_requests.add (real_channel);
						thumbnail.stack_thumbs (main.this, config, new String[] { real_channel }, -1, in_main_thread, new Runnable()
							{
							public void run()
								{
								update_channel_icon (channel_id);
								}
							});
						}
					}
				
				vVirtualLink.setVisibility (View.VISIBLE);
				}
			else
				vVirtualLink.setVisibility (View.GONE);
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
		
		ViewPager vPager = (ViewPager) findViewById (R.id.playback_horiz);
		if (vPager != null)			
			playback_episode_pager = NEW_setup_horiz (vPager, channel_id);
		
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
	
	/* Enable the player layer, activating any hints. Once the hints have displayed, continue
	   flow by calling back the provided Runnable */

	public void enable_player_layer (String channel_id, final Runnable callback)
		{
		if (current_layer == toplayer.PLAYBACK)
			{
			callback.run();
			return;
			}
			
		enable_player_layer (channel_id);
		
		int orientation = getResources().getConfiguration().orientation;
		boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
		
		/* there are two different hints */
		if (!landscape)
			{
			String language = get_language_en_or_zh();
			
			String visits = get_preference ("visits-to-playback-page");
			if (visits.equals (""))
				visits = "0";
			visits = Integer.toString (1 + Integer.parseInt (visits));
			set_preference ("visits-to-playback-page", visits);
			
			if (!get_preference ("seen-playback-h-hint").equals ("yes"))
				{
				set_preference ("seen-playback-h-hint", "yes");
				// alert_with_image (language.equals ("zh") ? R.drawable.hint_playback_h_zh : R.drawable.hint_playback_h_en);
				video_message_with_image (language.equals ("zh") ? R.drawable.hint_playback_h_zh : R.drawable.hint_playback_h_en);
				in_main_thread.postDelayed (new Runnable()
					{
					@Override
					public void run()
						{
						if (current_layer == toplayer.PLAYBACK)
							{
							View vTitlecard = findViewById (R.id.titlecard);
							vTitlecard.setVisibility (View.GONE);
							callback.run();
							}
						}
					}, 6000);
				return;
				}
			
			if (Integer.parseInt (visits) >= 2 && !get_preference ("seen-playback-v-hint").equals ("yes"))
				{
				set_preference ("seen-playback-v-hint", "yes");
				// alert_with_image (language.equals ("zh") ? R.drawable.hint_playback_v_zh : R.drawable.hint_playback_v_en);
				video_message_with_image (language.equals ("zh") ? R.drawable.hint_playback_v_zh : R.drawable.hint_playback_v_en);
				in_main_thread.postDelayed (new Runnable()
					{
					@Override
					public void run()
						{
						if (current_layer == toplayer.PLAYBACK)
							{
							View vTitlecard = findViewById (R.id.titlecard);
							vTitlecard.setVisibility (View.GONE);
							callback.run();
							}
						}
					}, 6000);
				return;
				}
			}
		
		if (callback != null)
			in_main_thread.post (callback);
		}
	
	public void enable_player_layer (String channel_id)
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
				
		setup_player_adapters (channel_id);
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
    		if (any_remembered_locations())
				{
				log ("BACK in playback: return to remembered location");
				restore_location();	    			
				}
    		else if (us_market())
    			{
				log ("BACK in playback: minimize");
				video_minimize (true);
    			}
			else
				{
				log ("BACK in playback: full stop");
				player_full_stop (true);
				}
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
		        	track_event ("navigation", "back", "back", 0);
		        	playback_back();
		        	}
				});	
		
		View vReturnPOI = findViewById (R.id.portrait_poi_return_instruction);
		if (vReturnPOI != null)
			vReturnPOI.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: POI back");
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
					int watched_id = getResources().getIdentifier ("epwatched" + i, "id", getPackageName());
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
					
					final boolean was_seen = was_video_seen (episode);
					
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
				        	if (was_seen)
				        		watched_videos_clicked_in_a_row++;
				        	else
				        		watched_videos_clicked_in_a_row = 0;
				        	}
						});					
					
					View vWatched = hrow.findViewById (watched_id);
					vWatched.setVisibility (was_seen ? View.VISIBLE : View.GONE);
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
		guide_class().init_3x3_grid (config);
		guide_class().setup_guide_buttons();
		track_layer (toplayer.GUIDE);
		}
	
    public GuideLayer guide_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.guide_fragment_container);
	    return (tv.tv9x9.player.GuideLayer) f;
		}
    
	/*** LAUNCH **************************************************************************************************/
    
    toplayer previous_layer = toplayer.HOME;
    String previous_arena[] = null;
    String unlaunched_player_arena[] = null;

    public void launch_player (String channel_id, String channels[])
    	{
    	launch_player (channel_id, null, channels);
    	}
 
    @Override
    public void launch_player (String channel_id, String episode_id, String channels[])
		{
    	/* bug #12465: when waking, sometimes time will accumulate even if the player is not playing */
    	reset_time_played();
    	
    	if (!check_pay_access (channel_id))
    		{
    		return;
    		}
    	
		if (current_layer != toplayer.PLAYBACK)
			previous_layer = current_layer;
		
		previous_arena = arena;
		set_arena (channels);
		
		launch_in_progress = false;
		
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
    
    /* The test layer is a quick place used to test whatever the thing-of-the-moment is. Not for deployment into production. */
 
    public void enable_test_layer()
    	{
		disable_video_layer();
		set_layer (toplayer.TEST);
		
		/*
		int adnum = config.next_advert();
		log ("next advert is: " + adnum);
		String url = config.advert_meta (adnum, "url");
		String id = config.advert_meta (adnum, "id");
		String name = config.advert_meta (adnum, "name");
		log ("advert url: " + url);
		if (url != null && !url.equals (""))
			launch_direct_ad (url, id, name);
		*/
		
		test_in_app_purchase();
    	}
    
	/*** FEEDBACK **************************************************************************************************/
    
    public void enable_feedback_layer()
    	{
    	feedback_class().feedback_init (config, current_layer);
    	
		disable_video_layer();
		set_layer (toplayer.FEEDBACK);
		feedback_class().setup_feedback_buttons();
		feedback_class().redraw_feedback();
    	}
    
    public FeedbackLayer feedback_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.feedback_fragment_container);
	    return (tv.tv9x9.player.FeedbackLayer) f;
		}
    
	/*** SOCIAL **************************************************************************************************/
    
    public void enable_social_layer()
		{
		disable_video_layer();
		set_layer (toplayer.SOCIAL);		
		social_class().start_social (config);
		}

    public SocialLayer social_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.social_fragment_container);
	    return (tv.tv9x9.player.SocialLayer) f;
		}
    
	/*** CHAT **************************************************************************************************/
    
    public void enable_chat_layer()
		{
		disable_video_layer();
		set_layer (toplayer.CHAT);		
		chat_class().start_chat (config);
		}

    public ChatLayer chat_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.chat_fragment_container);
	    return (tv.tv9x9.player.ChatLayer) f;
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
    			forget_video_location();
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
    			forget_video_location();
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
        
	    try
	    	{
		    AdRequest adRequest = new AdRequest.Builder()
	    	.addTestDevice (AdRequest.DEVICE_ID_EMULATOR)
	    	.addTestDevice ("9B1327240A0F06351FD043013CDD9072")
	    	.build();
		    
	    	interstitial.loadAd (adRequest);
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
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
    
	public void enable_store_layer()
		{
		disable_video_layer();
		set_layer (toplayer.STORE);
		store_class().setup_store_buttons();
		store_class().store_init (config);
		store_class().top_categories();
		
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
	
    public StoreLayer store_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.store_fragment_container);
	    return (tv.tv9x9.player.StoreLayer) f;
		}
    
	/*** SEARCH **************************************************************************************************/
	
	public void enable_search_layer()
		{
		disable_video_layer();
		set_layer (toplayer.SEARCH);
		search_class().search_init (config);
		search_class().setup_search_buttons();
		}
		
    public SearchLayer search_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.search_fragment_container);
	    return (tv.tv9x9.player.SearchLayer) f;
		}
    
	/*** MESSAGES **************************************************************************************************/
	
	public void enable_messages_layer()
		{
		disable_video_layer();
		set_layer (toplayer.MESSAGES);
		messages_class().setup_messages_buttons();
		messages_class().messages_init (config);
		messages_class().messages_display_content();
		track_layer (toplayer.MESSAGES);
		}		

    public MessagesLayer messages_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
	    Fragment f = fm.findFragmentById (R.id.message_fragment_container);
	    return (tv.tv9x9.player.MessagesLayer) f;
		}
	
	/*** SETTINGS **************************************************************************************************/
	
	public void enable_settings_layer()
		{
		disable_video_layer();
		signin_class().zero_signin_data();
		set_layer (toplayer.SETTINGS);
		
		settings_class().init (config);
		
		settings_class().load_notification_settings (config);
		settings_class().remember_notification_settings();

		settings_class().redraw_settings();
		settings_class().setup_settings_buttons();
		}	
	
	public void disable_settings_layer()
		{
		int frag = is_tablet() ? R.id.settings_fragment_container_tablet : R.id.settings_fragment_container_phone;
		Fragment f = getSupportFragmentManager().findFragmentById (frag);		
		if (!f.isHidden())
			{
			log ("hide Settings fragment");
		    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		    ft.hide (f);  
		    ft.commit();
			}	
		}
	
	/* bring back the settings layer, but with no alternation in its saved state */
	public void return_to_settings_layer()
		{
		int frag = is_tablet() ? R.id.settings_fragment_container_tablet : R.id.settings_fragment_container_phone;
		Fragment f = getSupportFragmentManager().findFragmentById (frag);		
		if (!f.isHidden())
			{
			log ("show Settings fragment");
		    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		    ft.show (f);  
		    ft.commit();
			}	
		}
	
    public SettingsLayer settings_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
		int frag = is_tablet() ? R.id.settings_fragment_container_tablet : R.id.settings_fragment_container_phone;
	    Fragment f = fm.findFragmentById (frag);
	    return (tv.tv9x9.player.SettingsLayer) f;
		}
    
	/*** PASSWORD **************************************************************************************************/
	
	public void enable_password_layer()
		{
		password_class().init (config);
		disable_video_layer();
		signin_class().zero_signin_data();
		set_layer (toplayer.PASSWORD);	
		password_class().redraw_password();
		password_class().setup_password_buttons();
		}	
	
	public void disable_password_layer()
		{
		int frag = is_tablet() ? R.id.password_fragment_container_tablet : R.id.password_fragment_container_phone;
		Fragment f = getSupportFragmentManager().findFragmentById (frag);		
		if (!f.isHidden())
			{
			log ("hide Password fragment");
		    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();  
		    ft.hide (f);  
		    ft.commit();
			}	
		}
	
    public PasswordLayer password_class()
		{    	
	    FragmentManager fm = getSupportFragmentManager();
		int frag = is_tablet() ? R.id.password_fragment_container_tablet : R.id.password_fragment_container_phone;
	    Fragment f = fm.findFragmentById (frag);
	    return (tv.tv9x9.player.PasswordLayer) f;
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
	
	/*** MISC ******************************************************************************************************/
	
	boolean finger_is_down = false;
	
	@Override
	public void onActionDown()
		{
		finger_is_down = true;
		}
	
	@Override
	public void onActionUp()
		{
		finger_is_down = false;
		}
	
	@Override
	public void onActionMove (int deltaX, int deltaY)
		{	
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
		
	/********* VIEWING HISTORY **************************************************************************************/				
		
	RecordManager recMan = null;
		
	public String dbfile()
		{
		String userid = config.userid == null ? "guest" : config.userid;
		return getFilesDir() + "/watched." + config.mso + "." + userid + ".db";
		}
	
	public void open_watch_db()
		{
		close_watch_db();
		try
			{
			recMan = RecordManagerFactory.createRecordManager (dbfile());
			}
		catch (IOException ex)
			{
			ex.printStackTrace();
			}
		}
	
	public void close_watch_db()
		{
		if (recMan != null)
			{
			try
				{
				recMan.commit();
				recMan.close();
				}
			catch (IOException ex)
				{
				ex.printStackTrace();
				}
			}
		}
	
	@Override
	public boolean was_video_seen (String episode_id)
		{        
		log ("-------------------- >>> wasVideoSeen? (" + episode_id + ") <<< --------------------");
		if (episode_id != null && recMan != null)
			{
	        String recordName = "watched";
	        PrimaryTreeMap <String, Integer> map = recMan.treeMap (recordName);         
	        
	        if (map != null)
	        	{
	        	Integer count = map.get (episode_id);
	        	if (count != null)
	        		return count > 0;
	        	}
	        
	        return false;
			}
		else
			return false;
		}
        
	public void set_video_watched (String episode_id)
		{
		String recordName = "watched";
        PrimaryTreeMap <String, Integer> map = recMan.treeMap (recordName);
                
        Integer count = map.get (episode_id);
        if (count == null)
        	count = 0;
        
        map.put (episode_id, count + 1);
        
		try
			{
			recMan.commit();
			}
		catch (IOException ex)
			{
			ex.printStackTrace();
			}
		}
	
	@Override
	public void onVideoWatched (String channel_id, String episode_id)
		{
		log ("-------------------- >>> onVideoWatched (" + channel_id + ") episode(" + episode_id + ")" + " <<< --------------------");
		set_video_watched (episode_id);
		}
	
	public String get_preference (String preference)
		{
		SharedPreferences prefs = getSharedPreferences (main.class.getSimpleName(), Context.MODE_PRIVATE);
		return prefs.getString (preference, "");
		}

	public void set_preference (String preference, String value)
		{
		SharedPreferences prefs = getSharedPreferences (main.class.getSimpleName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
	    editor.putString (preference, value);
	    editor.commit();
		}

	/********* IN-APP BILLING **************************************************************************************/
		
	IInAppBillingService billing_service = null;

	ServiceConnection billing_service_connection = new ServiceConnection()
		{
		@Override
		public void onServiceDisconnected (ComponentName name)
			{
			billing_service = null;
			}

		@Override
		public void onServiceConnected (ComponentName name, IBinder service)
			{
			billing_service = IInAppBillingService.Stub.asInterface (service);
		    }
		};	
		
	public void billing_create()
		{
		Intent serviceIntent = new Intent ("com.android.vending.billing.InAppBillingService.BIND");
		serviceIntent.setPackage ("com.android.vending");
		bindService (serviceIntent, billing_service_connection, Context.BIND_AUTO_CREATE);
		}
	
	public void billing_destroy()
		{
	    if (billing_service_connection != null)
	        unbindService (billing_service_connection);
		}
	
	public void test_in_app_purchase()
		{
		log ("testing in-app-purchase");
		
		ArrayList <String> skuList = new ArrayList <String> ();
		
		skuList.add ("android.test.purchased");
		skuList.add ("tv.9x9.sample1");
		skuList.add ("samplechannel2");
		
		query_sku_in_thread (skuList);
		}
	
	public void query_sku_in_thread (final ArrayList <String> skuList)
		{
		Thread t = new Thread ()
			{
			@Override
			public void run()
				{
				log ("testing in-app-purchase");
				query_sku (skuList);
				}
			};
		
		t.start();
		}
	
	public void query_sku (final ArrayList <String> skuList)
		{
		Bundle querySkus = new Bundle();
		querySkus.putStringArrayList ("ITEM_ID_LIST", skuList);

		Bundle skuDetails = null;
		try
			{
			log ("Google Play: calling getSkuDetails");
			String purchase_type = "subs"; // might be "inapp", but Lili says we will only be using subscriptions
			skuDetails = billing_service.getSkuDetails (3, getPackageName(), purchase_type, querySkus);
			}
		catch (RemoteException ex)
			{
			log ("error obtaining SKU details from Google Play:");
			ex.printStackTrace();
			return;
			}
		
		int response = skuDetails.getInt ("RESPONSE_CODE");
		if (response == 0)
			{
			ArrayList <String> responseList = skuDetails.getStringArrayList ("DETAILS_LIST");
		   
			log ("Google Play: getSkuDetails number of items returned: " + responseList.size());
			
		    for (String thisResponse: responseList)
		   		{
		    	try
		    		{
		    		JSONObject object = new JSONObject (thisResponse);
			    	final String sku = object.getString ("productId");
			    	final String price = object.getString ("price");
			    	log ("productId: " + sku + " price: " + price);
			    	in_main_thread.post (new Runnable()
			    		{
			    		@Override
			    		public void run()
			    			{
					    	enable_sku (sku, price);
					    	config.set_pay_info_by_sku (sku, "productId", price);
					    	config.set_pay_info_by_sku (sku, "price", price);
			    			}
			    		});
		    		}
		    	catch (JSONException ex)
		    		{
		    		ex.printStackTrace();
		    		}
		   		}
			}
		else
			log ("Google Play: getSkuDetails response code is: " + response);
		}
	 
	public void enable_sku (final String sku, final String price)
		{
		int resource_id = 0;
		
		if (sku.equals ("tv.9x9.sample1"))
			resource_id = R.id.sample_1_buy;
		else if (sku.equals ("samplechannel2"))
			resource_id = R.id.sample_2_buy;
		else if (sku.equals ("android.test.purchased"))
			resource_id = R.id.google_test_buy;
		
		View vButton = findViewById (resource_id);
		if (vButton != null)
			{
			vButton.setVisibility (View.VISIBLE);
			vButton.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	test_purchase (sku);
		        	}
				});
			}
		}
	
	/* store this as a global for now. This is bad */
	String sku_purchase_in_progress = null;
	
	public void test_purchase (String sku)
		{
		try
			{
			sku_purchase_in_progress = sku;
			Bundle buyIntentBundle = billing_service.getBuyIntent (3, getPackageName(), sku, "subs", "wombat");
			PendingIntent pi = buyIntentBundle.getParcelable ("BUY_INTENT");
			startIntentSenderForResult (pi.getIntentSender(), 1911, new Intent(), Integer.valueOf (0), Integer.valueOf (0), Integer.valueOf (0));
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
	/*
	 * I can't figure out where these are defined
	 * BILLING_RESPONSE_RESULT_OK = 0;
	 * BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
	 * ...2 is service unavailable...
	 * BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
	 * BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
	 * BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
	 * BILLING_RESPONSE_RESULT_ERROR = 6;
	 * BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	 * BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;
	 */
	
	static int BILLING_RESPONSE_RESULT_OK = 0;
	static int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	
	void purchase_activity_result (int requestCode, int resultCode, Intent data)
		{ 
		log ("purchase_activity_result :: requestCode=" + requestCode + " resultCode=" + resultCode);
		
		if (requestCode == 1911)
			{           
			int responseCode = data.getIntExtra ("RESPONSE_CODE", 0);
			String purchaseData = data.getStringExtra ("INAPP_PURCHASE_DATA");
			String dataSignature = data.getStringExtra ("INAPP_DATA_SIGNATURE");
	        
			log ("responseCode=" + responseCode);
			
			// purchase_activity_result :: requestCode=1911 resultCode=0
			if (responseCode == BILLING_RESPONSE_RESULT_OK)
				{
				if (purchaseData != null)
					{
					try
						{
						JSONObject jo = new JSONObject (purchaseData);
						String sku = jo.getString ("productId");
						String purchaseToken = jo.getString ("purchaseToken");
						String channel_id = config.get_pay_info (sku, "channel");
						log ("sku \"" + sku + "\" purchase successful, channel: " + channel_id + ", token is: " + purchaseToken);
						if (channel_id != null && !channel_id.equals (""))
							{
							purchase_activity_result_ii (channel_id, sku, purchaseToken);
							}
						else
							{
							/* this might be a test purchase, do nothing */
							}
	
						}
					catch (JSONException ex)
						{
						alert ("in-app-purchase failure:");
						ex.printStackTrace();
						}
					}
				}
			else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED)
				{
				log ("billing response: ALREADY PURCHASED (" + purchaseData + ")");
				config.set_pay_info_by_sku (sku_purchase_in_progress, "paid", "true");
				String channel_id = config.get_pay_info_by_sku (sku_purchase_in_progress, "channel");
				launch_player (channel_id, new String[] { channel_id });
				}
			else
				{
				alert ("Error " + responseCode + " making in-app purchase.");
				}
			}
		}
	
	public void purchase_activity_result_ii (final String channel_id, final String sku, final String purchaseToken)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
	    		new playerAPI (in_main_thread, config, "addPurchase?user=" + config.usertoken + "&productId=" + sku + "&purchaseToken=" + purchaseToken)
					{
					public void success (String[] lines)
						{
						process_purchase_result (lines);
						force_load_of_channel (channel_id, new Runnable()
							{
							@Override
							public void run()
								{
								log ("launching pay channel: " + channel_id);
								launch_player (channel_id, new String[] { channel_id });
								}
							});
						}
					public void failure (int code, String errtext)
						{
						toast ("Error " + code + " making in-app purchase: " + errtext);
						}
					};
				}
			};
		
		t.start();
		}
		
	
	public void verify_purchases_directly()
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				verify_purchases_directly_inner();
				}
			};
		t.start();
		}
	
	public void verify_purchases_directly_inner()
		{
		try
			{
			Bundle ownedItems = billing_service.getPurchases (3, getPackageName(), "subs", null);
			int response = ownedItems.getInt ("RESPONSE_CODE");
			
			if (response == 0)
				{
				ArrayList <String> ownedSkus = ownedItems.getStringArrayList ("INAPP_PURCHASE_ITEM_LIST");
				ArrayList <String> purchaseDataList = ownedItems.getStringArrayList ("INAPP_PURCHASE_DATA_LIST");
				
				for (int i = 0; i < purchaseDataList.size(); ++i)
					{
				    String purchaseData = purchaseDataList.get (i);
				    String sku = ownedSkus.get (i);
				    if (purchaseData != null)
				    	{
						JSONObject jo = new JSONObject (purchaseData);
						String purchaseToken = jo.getString ("purchaseToken");
						String channel_id = config.get_pay_info (sku, "channel");
						String paid_str = config.get_pay_info (sku, "paid");
						boolean paid = paid_str != null && paid_str.equals ("true");
						
						if (!paid)
							{
				    		new playerAPI (in_main_thread, config, "addPurchase?user=" + config.usertoken + "&productId=" + sku + "&purchaseToken=" + purchaseToken)
								{
								public void success (String[] lines)
									{
									process_purchase_result (lines);
									}
								public void failure (int code, String errtext)
									{
									}
								};
							}
				    	}
					}
				}
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		}
	
	public void get_pay_channel_info()
		{
		if (config.usertoken == null)
			{
			get_pay_channel_info_ii();
			return;
			}
		
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
	    		new playerAPI (in_main_thread, config, "getPurchases?user=" + config.usertoken)
					{
					public void success (String[] lines)
						{
						process_purchase_result (lines);
						get_pay_channel_info_ii();
						}
					public void failure (int code, String errtext)
						{
						}
					};
				}
			};
		
		t.start();
		}
	
	public void process_purchase_result (String[] lines)
		{
		for (String line: lines)
			{
			log ("pay channel I: " + line);
			String fields[] = line.split ("\t");
			if (fields.length > 2)
				{
				config.set_pay_info (fields[0], "sku", fields[1]);
				config.set_pay_info (fields[0], "paid", "true");
				}
			}
		}
	
	public void get_pay_channel_info_ii()
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
	    		new playerAPI (in_main_thread, config, "getItems?os=android")
					{
					public void success (String[] lines)
						{
						ArrayList <String> skuList = new ArrayList <String> ();
						
						for (String line: lines)
							{
							log ("pay channel II: " + line);
							String fields[] = line.split ("\t");
							if (fields.length > 2)
								{
								if (fields[2].equals ("1"))
									{
									/* 1 for Google Play, 2 for AppStore */
									config.set_pay_info (fields[0], "sku", fields[1]);
									skuList.add (fields[1]);
									}
								}							
							}
						
						log ("known sku's for this domain: " + skuList.size());
						if (skuList.size() > 0)
							{
							/* information returned by playerAPI is inadequate. Ask Google for the rest */
							query_sku (skuList);
							}
												
						verify_purchases_directly();
						}
					public void failure (int code, String errtext)
						{
						}
					};
				}
			};
		
		t.start();
		}
	
	@Override
	public void onNoPayAccess (final String channel_id)
		{
		if (config.usertoken != null)
			{
			alert ("You don't have access to this pay channel!");
			log ("onNoPayAccess: no access to this pay channel: " + channel_id);
			String sku = config.get_pay_info (channel_id, "sku");
			if (sku != null)
				test_purchase (sku);
			else
				alert ("This channel does not have an SKU!");
			}
		else
			actual_alert ("Please sign in to purchase this pay channel");
		}
	}	