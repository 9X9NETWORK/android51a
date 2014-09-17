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

public class PasswordLayer extends StandardFragment
	{
	boolean original_notify_setting = false;
	boolean original_sound_setting = false;
	boolean original_vibrate_setting = false;

	metadata config = null;
	
    public interface OnPasswordListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void toast_by_resource (int resource_id);
    	public void toggle_menu();
    	public void slide_away_password();
    	public void alert (String text);
    	public toplayer get_current_layer();
    	public void disable_password_layer();
		}    
    
    OnPasswordListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
    	
    	/* is this legal? if so, is nasty */
    	OnPasswordListener a = (OnPasswordListener) this.getActivity();
    	boolean is_tablet = a.is_tablet();
    	
    	log ("onCreateView -- tablet? " + is_tablet);
    	
        return inflater.inflate (is_tablet ? R.layout.password_tablet : R.layout.password_phone, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnPasswordListener) activity;
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
    	identity = "password";
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
			View vSettings = getView().findViewById (R.id.passwordlayer_tablet);
			if (vSettings != null)
				vSettings.setVisibility (visible ? View.VISIBLE : View.GONE);
	    	}
    	}
	
	public void redraw_password()
		{
		View vAccountSection = getView().findViewById (R.id.account_section);
		if (vAccountSection != null)
			vAccountSection.setVisibility ((config.usertoken == null || config.email.equals ("[via Facebook]")) ? View.GONE : View.VISIBLE);
		
		View vPasswordSection = getView().findViewById (R.id.password_section);
		if (vPasswordSection != null)
			vPasswordSection.setVisibility ((config.usertoken == null || config.email.equals ("[via Facebook]")) ? View.GONE : View.VISIBLE);
		
		TextView vNameReadonly = (TextView) getView().findViewById (R.id.settings_name_readonly);
		if (vNameReadonly != null)
			vNameReadonly.setText (config.usertoken == null ? "" : config.username);

		TextView vEmailReadonly = (TextView) getView().findViewById (R.id.settings_email_readonly);
		if (vEmailReadonly != null)
			vEmailReadonly.setText (config.usertoken == null ? "" : config.email);
		
		TextView vSettingsEmail = (TextView) getView().findViewById (R.id.settings_email);
		if (vSettingsEmail != null)
			vSettingsEmail.setText (config.usertoken == null ? "" : config.email);
		}
	
	public void save_password()
		{
		boolean nothing_changed = true;
		
		String kk = null;
		String vv = null;
		
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
					password_exit();
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
		
		if (nothing_changed)
			mCallback.toast_by_resource (R.string.nothing_changed);
		else if (!will_background)
			password_exit();
		}
	
	public void password_exit()
		{
		if (mCallback.get_current_layer() == toplayer.PASSWORD)
			{
			log ("password exit");
			mCallback.slide_away_password();
			}
		else
			{
			if (mCallback.is_tablet())
				{
				mCallback.disable_password_layer();
				}
			else
				log ("current layer is: " + mCallback.get_current_layer() + ", stay in settings");
			}
		}
	
	public void setup_password_buttons()
		{
		/*
		ImageView vAppIcon = (ImageView) getView().findViewById (R.id.password_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getActivity().getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
		*/
		
		/*
		View vBanner = getView().findViewById (R.id.password_banner);
		if (vBanner != null)
			vBanner.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: password banner");
		        	password_exit();
		        	}
				});
		*/
		
		View vCancel = getView().findViewById (R.id.password_cancel);
		if (vCancel != null)
			vCancel.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("password cancel");
		        	mCallback.slide_away_password();
		        	}
				});	

		View vSave = getView().findViewById (R.id.password_save);
		if (vSave != null)
			vSave.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("password save");
		        	save_password();
		        	}
				});	
		
		View vLayer = getView().findViewById (mCallback.is_phone() ? R.id.passwordlayer_phone : R.id.passwordlayer_tablet);
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
	}