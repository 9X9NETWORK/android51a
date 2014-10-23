package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

import tv.tv9x9.player.main.toplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SettingsLayer extends StandardFragment
	{
	boolean original_notify_setting = false;
	boolean original_sound_setting = false;
	boolean original_vibrate_setting = false;

	metadata config = null;
	
    public interface OnSettingsListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void signout_from_app_or_facebook();
    	public void enable_about_layer();
    	public void slide_in_password();
    	public void toast_by_resource (int resource_id);
    	public void slide_in_terms (toplayer layer);
    	public void slide_in_privacy (toplayer layer);
    	public void toggle_menu();
    	public void slide_away_password();
    	public void alert (String text);
    	public toplayer get_current_layer();
    	public void disable_settings_layer();
		}    
    
    OnSettingsListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
    	
    	/* is this legal? if so, is nasty */
    	OnSettingsListener a = (OnSettingsListener) this.getActivity();
    	boolean is_tablet = a.is_tablet();
    	
    	log ("onCreateView -- tablet? " + is_tablet);
    	
        return inflater.inflate (is_tablet ? R.layout.settings_tablet_new : R.layout.settings_phone_new, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnSettingsListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnSettingsListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "settings";
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
    	set_visibility (true);
    	}
    
    public void set_visibility (boolean visible)
    	{
    	if (mCallback.is_tablet())
	    	{
			View vSettings = getView().findViewById (R.id.settingslayer_tablet);
			if (vSettings != null)
				vSettings.setVisibility (visible ? View.VISIBLE : View.GONE);
	    	}
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
		View vAccountSection = getView().findViewById (R.id.account_section);
		if (vAccountSection != null)
			vAccountSection.setVisibility ((config.usertoken == null || config.email.equals ("[via Facebook]")) ? View.GONE : View.VISIBLE);
		
		View vPasswordSection = getView().findViewById (R.id.password_section);
		if (vPasswordSection != null)
			vPasswordSection.setVisibility ((config.usertoken == null || config.email.equals ("[via Facebook]")) ? View.GONE : View.VISIBLE);

		View vAboutButton = getView().findViewById (R.id.settings_about);
		if (vAboutButton != null)
			vAboutButton.setVisibility (config.about_us_url == null ? View.GONE : View.VISIBLE);

		View vAboutDivider = getView().findViewById (R.id.settings_about_divider);
		if (vAboutDivider != null)
			vAboutDivider.setVisibility (config.about_us_url == null ? View.GONE : View.VISIBLE);
		
		TextView vVersion = (TextView) getView().findViewById (R.id.version);
		if (vVersion != null)
			{
			String version_code = "[unknown]";			
			try
				{
				PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo (getActivity().getPackageName(), 0);
				version_code = pInfo.versionName;
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}			
			String txt_version = getResources().getString (R.string.version);
			vVersion.setText (txt_version + " " + version_code);
			}
		
		TextView vNameReadonly = (TextView) getView().findViewById (R.id.settings_name_readonly);
		if (vNameReadonly != null)
			vNameReadonly.setText (config.usertoken == null ? "" : config.username);

		TextView vEmailReadonly = (TextView) getView().findViewById (R.id.settings_email_readonly);
		if (vEmailReadonly != null)
			vEmailReadonly.setText (config.usertoken == null ? "" : config.email);
		
		TextView vSettingsEmail = (TextView) getView().findViewById (R.id.settings_email);
		if (vSettingsEmail != null)
			vSettingsEmail.setText (config.usertoken == null ? "" : config.email);
		
		String vService = Context.VIBRATOR_SERVICE;
		Vibrator vibrator = (Vibrator) getActivity().getSystemService (vService);
		
		View vVibrateSection2 = getView().findViewById (R.id.settings_vibrate);
		if (vVibrateSection2 != null)
			vVibrateSection2.setVisibility (config.notifications_enabled && vibrator.hasVibrator() ? View.VISIBLE : View.GONE);	

		View vVibrateDivider2 = getView().findViewById (R.id.settings_vibrate_divider);
		if (vVibrateDivider2 != null)
			vVibrateDivider2.setVisibility (config.notifications_enabled && vibrator.hasVibrator() ? View.VISIBLE : View.GONE);
		
		ImageView vVibrateWhen = (ImageView) getView().findViewById (R.id.vibrate_when_notified_image);
		if (vVibrateWhen != null)
			vVibrateWhen.setImageResource (config.notify_with_vibrate ? R.drawable.check_checked_52 : R.drawable.check_unchecked_52);
		
		Switch vNotifySwitch = (Switch) getView().findViewById (R.id.settings_notification_switch);
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
		
		View vVibrate = getView().findViewById (R.id.settings_vibrate);
		if (vVibrate != null)
			vVibrate.setAlpha (config.notifications_enabled ? 1.0f : 0.25f);
		
		Switch vVibrateSwitch = (Switch) getView().findViewById (R.id.settings_vibrate_switch);
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
		
		View vSound = getView().findViewById (R.id.settings_sound);
		if (vSound != null)
			vSound.setAlpha (config.notifications_enabled ? 1.0f : 0.25f);
		
		Switch vSoundSwitch = (Switch) getView().findViewById (R.id.settings_sound_switch);
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
        futil.write_file (getActivity(), "config.notifications", filedata);
        log_notification_settings();
		}
	
	public void load_notification_settings (metadata config)
		{
		this.config = config;
		
		String config_data = futil.read_file (getActivity(), "config.notifications");
		
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
		final View vLayer = getView().findViewById (mCallback.is_phone() ? R.id.settingslayer_phone : R.id.settingslayer_tablet);
		
		View vBanner = getView().findViewById (R.id.settings_banner);
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
		
		ImageView vAppIcon = (ImageView) getView().findViewById (R.id.settings_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getActivity().getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
	
		View vSignout = vLayer.findViewById (R.id.settings_signout);
		if (vSignout != null)
			vSignout.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: settings signout");
		        	mCallback.signout_from_app_or_facebook();
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
		        	mCallback.slide_in_password();
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
		        	mCallback.enable_about_layer();
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
		        	mCallback.slide_in_terms (toplayer.SETTINGS);
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
		        	mCallback.slide_in_privacy (toplayer.SETTINGS);
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

		TextView vEmail = (TextView) getView().findViewById (R.id.settings_email);
		String email = vEmail.getText().toString();
		
		TextView vOldPassword = (TextView) getView().findViewById (R.id.settings_old_password);
		String old_password = vOldPassword.getText().toString();
		
		TextView vNewPassword = (TextView) getView().findViewById (R.id.settings_new_password);
		String new_password = vNewPassword.getText().toString();
	
		TextView vConfirm = (TextView) getView().findViewById (R.id.settings_verify_password);
		String confirm = vConfirm.getText().toString();
		
		if (!old_password.equals ("") && !new_password.equals (""))
			{
			if (!new_password.equals (confirm))
				{
				mCallback.toast_by_resource (R.string.tlogin_pw_no_match);
				return;
				}		
	
			if (new_password.length() < 6)
				{
				mCallback.toast_by_resource (R.string.tlogin_pw_six);
				return;
				}
			
			kk = "oldPassword,password";
			vv = util.encodeURIComponent (old_password) + "," + util.encodeURIComponent (new_password);
			}
		
		/* if true the final result of the "save" won't be known immediately */
		boolean will_background = true;
		
		if (config != null && config.usertoken != null && kk != null && !kk.equals (""))
			{
			new playerAPI (mCallback.get_main_thread(), config, "setUserProfile?user=" + config.usertoken + "&key=" + kk + "&value=" + vv)
				{
				public void success (String[] lines)
					{
					mCallback.toast_by_resource (R.string.saved);
					settings_exit();
					}
				public void failure (int code, String errtext)
					{
					if (code == 201 || errtext.startsWith ("201"))
						mCallback.toast_by_resource (R.string.current_password_wrong);
					else
						mCallback.alert ("Failure saving your changes: " + errtext);
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
			mCallback.toast_by_resource (R.string.saved);
			nothing_changed = false;
			}
		
		if (nothing_changed)
			mCallback.toast_by_resource (R.string.nothing_changed);
		else if (!will_background)
			settings_exit();
		}
	
	public void settings_exit()
		{
		if (mCallback.get_current_layer() == toplayer.SETTINGS)
			{
			log ("settings exit");
			final View vLayer = getView().findViewById (mCallback.is_phone() ? R.id.settingslayer_phone : R.id.settingslayer_tablet);
	    	if (mCallback.is_tablet())
	    		vLayer.setVisibility (View.GONE);
	    	else
	    		mCallback.toggle_menu();
			}
		else if (mCallback.get_current_layer() == toplayer.PASSWORD)
			{
			log ("password exit");
			mCallback.slide_away_password();
			}
		else if (mCallback.get_current_layer() == toplayer.TERMS)
			{
			/* don't ever want to return to TERMS, but settings isn't a proper layer on a tablet, etc. */
			mCallback.enable_home_layer();
			}
		else
			{
			if (mCallback.is_tablet())
				{
				mCallback.disable_settings_layer();
				}
			else
				log ("current layer is: " + mCallback.get_current_layer() + ", stay in settings");
			}
		}
	}