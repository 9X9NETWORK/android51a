package tv.tv9x9.player;

import tv.tv9x9.player.main.toplayer;

import com.facebook.widget.LoginButton;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SigninLayer extends StandardFragment
	{
	metadata config = null;
		
	toplayer layer_before_signin = toplayer.HOME;
	
	SigninSlider signin_slider = null;
	
	Runnable signin_layer_callback = null;
	
    public interface OnSigninListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void track_event (String category, String action, String label, long value);
    	public void enable_signin_layer (Runnable callback);
    	public boolean has_facebook();
    	public void fezbuk2 (View parent);
    	public void slide_in_terms (toplayer layer);
    	public void slide_in_privacy (toplayer layer);
    	public void toast_by_resource (int id);
    	public void toggle_menu	(final Callback cb);
    	public void query_following (final Callback callback);
    	public void alert (String text);
    	public SettingsLayer settings_class();
    	public toplayer get_current_layer();
    	public void activate_layer (toplayer layer);
        public boolean signed_in_with_facebook();
        public void set_signed_in_with_facebook (boolean value);
    	public void onFacebookLayout (View parent);
		}    
    
    OnSigninListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
    	
    	/* is this legal? if so, is nasty */
    	OnSigninListener a = (OnSigninListener) this.getActivity();
    	boolean is_tablet = a.is_tablet();
        
    	log ("onCreateView -- tablet? " + is_tablet);
    	
    	View vLayout = inflater.inflate (is_tablet ? R.layout.signin_tablet_new : R.layout.signin_phone_new, container, false);
    	a.onFacebookLayout (vLayout);
        return vLayout;
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnSigninListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnSigninListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "nag";
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
    	
        signin_slider = new SigninSlider();
        StoppableViewPager vSigninPager = (StoppableViewPager) getView().findViewById (R.id.signin_pager);
        vSigninPager.setAdapter (signin_slider);
        
        create_signin_slider();
        
        int layout_id = mCallback.is_tablet() ? R.id.signinlayer_tablet : R.id.signinlayer_phone;
        
        View vLayout = getView().findViewById (layout_id);
        vLayout.setVisibility (View.VISIBLE);
    	}
    
    public void set_layer_before_signin (toplayer layer)
    	{
    	layer_before_signin = layer;
    	}
    
    public toplayer get_layer_before_signin()
    	{
    	return layer_before_signin;
    	}
    
    public void set_signin_layer_callback (Runnable r)
    	{
    	signin_layer_callback = r;    	
    	}
    
	public void setup_signin_layer_buttons (final Runnable callback)
		{
		ViewGroup vSigninGroup = (ViewGroup) getView().findViewById (R.id.sign_in_content);
		setup_edit_containers (vSigninGroup);
		
		ViewGroup vSignupGroup = (ViewGroup) getView().findViewById (R.id.sign_up_content);
		setup_edit_containers (vSignupGroup);
		
		View vBanner = getView().findViewById (R.id.signin_banner);
		if (vBanner != null)
			vBanner.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin banner");
					zero_signin_data();
					mCallback.activate_layer (layer_before_signin);
					if (callback != null)
						callback.run();
		        	}
				});		
		
		ImageView vAppIcon = (ImageView) getView().findViewById (R.id.signin_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getActivity().getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
	
		View vSigninLayer = getView().findViewById (R.id.signinlayer);
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
		}
	
	public void setup_signin_buttons (View v)
		{
		/* this catches the "Next" button on the final field, and removes any soft keyboard. Android's
		   keyboard APIs are downright awful, and setting the signin button to focusable will require double
		   tapping it. This seems the safest way to disappear the keyboard in an innocuous manner. */

		View vPassword = v.findViewById (R.id.sign_in_password);
		vPassword.setOnFocusChangeListener (new View.OnFocusChangeListener()
			{
	        @Override
	        public void onFocusChange (View v, boolean hasFocus)
	        	{
	        	if (!hasFocus)
	        		{
	        		/* turn off soft keyboard */
	        		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE);
	        	    imm.hideSoftInputFromWindow (v.getApplicationWindowToken(), 0);
	        		}
	            }
	        });
		
		View vSignin = getView().findViewById (R.id.sign_in_button);
		if (vSignin != null)
			vSignin.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signin");
		        	proceed_with_signin (signin_layer_callback);
		        	}
				});
	
		mCallback.fezbuk2 (v);
		}
	
	public void setup_signup_buttons (View v)
		{
		TextView vTermsButton = (TextView) v.findViewById (R.id.terms_button);
		if (vTermsButton != null)
			{
			vTermsButton.setPaintFlags (vTermsButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			vTermsButton.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: terms");
		        	mCallback.slide_in_terms (toplayer.SIGNIN);
		        	}
				});
			}
		TextView vPrivacyButton = (TextView) v.findViewById (R.id.privacy_button);
		if (vPrivacyButton != null)
			{
			vPrivacyButton.setPaintFlags (vPrivacyButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			vPrivacyButton.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: privacy");
		        	mCallback.slide_in_privacy (toplayer.SIGNIN);
		        	}
				});
			}		
		
		/* this catches the "Next" button on the final field, and removes any soft keyboard. Android's
		   keyboard APIs are downright awful, and setting the signup button to focusable will require double
		   tapping it. This seems the safest way to disappear the keyboard in an innocuous manner. */

		View vVerify = v.findViewById (R.id.sign_up_verify);
		vVerify.setOnFocusChangeListener (new View.OnFocusChangeListener()
			{
	        @Override
	        public void onFocusChange (View v, boolean hasFocus)
	        	{
	        	if (!hasFocus)
	        		{
	        		/* turn off soft keyboard */
	        		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE);
	        	    imm.hideSoftInputFromWindow (v.getApplicationWindowToken(), 0);
	        		}
	            }
	        });
		
		View vSignup = v.findViewById (R.id.sign_up_button);
		if (vSignup != null)
			vSignup.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("click on: signup");
		        	proceed_with_signup (signin_layer_callback);
		        	}
				});
		}
	
	/* look for "editcontainer"s and set them up as a larger touchable region to bring up the soft keyboard */
	
	public void setup_edit_containers (ViewGroup vBody)
		{
		if (vBody != null)
			{
			int n_children = vBody.getChildCount();
		    for (int i = 0; i < n_children; i++)
		    	{
		        final View vChild = vBody.getChildAt (i);
		        
		        String tag = (String) vChild.getTag();
		        if (tag != null && tag.equals ("editcontainer"))
		        	{
					vChild.setOnClickListener (new OnClickListener()
						{
				        @Override
				        public void onClick (View v)
				        	{
				        	EditText vEditable = (EditText) vChild.findViewWithTag ("editable");
				        	if (vEditable != null)
				        		{
				        		vEditable.requestFocusFromTouch();
					        	InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE); 
					            imm.showSoftInput (vEditable, 0);
				        		}
				        	}
			        	});
		        	}
		        else
		        	{
		        	/* only need to descend into ViewGroups that don't have tag "editcontainer" */
			        if (vChild instanceof ViewGroup)
			        	setup_edit_containers ((ViewGroup) vChild);
		        	}
		        }
			}
		}
	
	public void create_signin_slider()
		{	
		mCallback.get_main_thread().post (new Runnable()
			{
			@Override
			public void run()
				{							
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
				final SlidingTabLayout mSlidingTabLayout = (SlidingTabLayout) getView().findViewById (R.id.signin_tabs);
				mSlidingTabLayout.setCustomTabColorizer (colorizer);
		        final StoppableViewPager vSigninPager = (StoppableViewPager) getView().findViewById (R.id.signin_pager);
				mSlidingTabLayout.setViewPager (vSigninPager);
				mCallback.get_main_thread().post (new Runnable()
					{
					public void run()
						{
						/* Call setViewPager() a second time, after the first rendering, so that it can find the rendered
						   size. This allows it to draw the tabs with exactly 50% width, per the mockup. */
						mSlidingTabLayout.setViewPager (vSigninPager);
						}
					});
				}
			});
		}
	
	class Swapsign
		{
		int page_number = 0;
		ScrollView page = null;
		public Swapsign (int page_number)
			{
			this.page_number = page_number;
			}
		};
	
	/* this is implemented using the base class! */
		
	public class SigninSlider extends PagerAdapter
		{		
	    @Override
	    public int getCount()
	    	{
	        return 2;
	    	}
	
	    @Override
	    public CharSequence getPageTitle (int position)
	    	{
	    	return getResources().getString (position == 0 ? R.string.log_in : R.string.si_sign_up_button);
	    	}
	    
		@Override
		public boolean isViewFromObject (View view, Object object)
			{
			return (((Swapsign) object).page) == (ScrollView) view;
			}
		
		@Override
		public Object instantiateItem (final ViewGroup container, final int position)
			{
			log ("instantiate: " + position);
			
			Swapsign sh = new Swapsign (position);				
			int layout_resource = position == 0 ? R.layout.sign_in_content : R.layout.sign_up_content;
			
			ScrollView page = (ScrollView) View.inflate (getActivity(), layout_resource, null);
			sh.page = page;
			
			((StoppableViewPager) container).addView (page, 0);
			
			setup_edit_containers (page);
			
			if (position == 0)
				setup_signin_buttons (page);
			else
				setup_signup_buttons (page);
	
			return sh;
			}
		
		@Override
		public void destroyItem (ViewGroup container, int position, Object object)
			{
			log ("destroy: " + position);
			Swapsign sh = (Swapsign) object;
			((StoppableViewPager) container).removeView (sh.page);
			}
		
		@Override
		public void setPrimaryItem (ViewGroup container, int position, Object object)
			{
			Swapsign sh = (Swapsign) object;		
			}
		}   
	
	public void zero_signin_data()
		{
		/* and settings */
		int editables[] = { R.id.sign_in_email, R.id.sign_in_password, R.id.sign_up_name, R.id.sign_up_email,
				R.id.sign_up_password, R.id.sign_up_verify, R.id.settings_old_password, R.id.settings_new_password, R.id.settings_verify_password };
		for (int editable: editables)
			{
			EditText v = (EditText) getView().findViewById (editable);
			if (v != null)
				v.setText ("");
			}
		}
	
	public void proceed_with_signin (final Runnable callback)
		{
		EditText emailView = (EditText) getView().findViewById (R.id.sign_in_email);
		final String email = emailView.getText().toString();
	
		/* use any view to turn off soft keyboard */
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow (emailView.getApplicationWindowToken(), 0);
	    
		EditText passwordView = (EditText) getView().findViewById (R.id.sign_in_password);
		String password = passwordView.getText().toString();
		
		password = util.encodeURIComponent (password);
		String encoded_email = util.encodeURIComponent (email);
		
		if (!email.contains ("@") || email.contains (" "))
			{
			mCallback.toast_by_resource (R.string.tlogin_valid_email);
			return;
			}
		
		if (password.length() < 6)
			{
			mCallback.toast_by_resource (R.string.tlogin_pw_six);
			return;
			}
		
		log ("email: " + email + " password: " + password);
		new playerAPI (mCallback.get_main_thread(), config, "login?email=" + encoded_email + "&password=" + password)
			{
			public void success (String[] lines)
				{
				mCallback.set_signed_in_with_facebook (false);
				config.email = email;
				process_login_data (email, lines);
				mCallback.toast_by_resource (R.string.signed_in);
				mCallback.query_following (null);
				mCallback.track_event ("signIn", "signInWithEmail", "signInWithEmail", 0);
	        	mCallback.toggle_menu (new Callback()
		        	{
					@Override
					public void run()
						{
						zero_signin_data();
						/* the settings view might be in the slider */
						mCallback.settings_class().redraw_settings();
						mCallback.activate_layer (layer_before_signin);
						if (callback != null)
							callback.run();
						}		        	
		        	});
				}
			public void failure (int code, String errtext)
				{
				if (code == 201)
					mCallback.toast_by_resource (R.string.login_failure);
				else
					{
					String txt_failure = getResources().getString (R.string.login_failure);
					mCallback.alert (txt_failure + ": " + errtext);
					}
				}
			};
		}
	
	public void proceed_with_signup (final Runnable callback)
		{
		TextView emailView = (TextView) getView().findViewById (R.id.sign_up_email);
		final String email = emailView.getText().toString();
	
		/* use any view to turn off soft keyboard */
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow (emailView.getApplicationWindowToken(), 0);
	    
		TextView nameView = (TextView) getView().findViewById (R.id.sign_up_name);
		String name = nameView.getText().toString();
		
		TextView passwordView = (TextView) getView().findViewById (R.id.sign_up_password);
		String password = passwordView.getText().toString();
		
		TextView confirmView = (TextView) getView().findViewById (R.id.sign_up_verify);
		String confirm = confirmView.getText().toString();
		
		if (!password.equals (confirm))
			{
			mCallback.toast_by_resource (R.string.tlogin_pw_no_match);
			return;
			}
		
		if (password.length() < 6)
			{
			mCallback.toast_by_resource (R.string.tlogin_pw_six);
			return;
			}
		
		if (!email.contains ("@") || email.contains (" "))
			{
			mCallback.toast_by_resource (R.string.tlogin_valid_email);
			return;
			}
		
		String encoded_email = util.encodeURIComponent (email);
		String encoded_password = util.encodeURIComponent (password);
		
		new playerAPI (mCallback.get_main_thread(), config, "signup?name=" + name + "&email=" + encoded_email + "&password=" + encoded_password)
			{
			public void success (String[] lines)
				{
				mCallback.set_signed_in_with_facebook (false);
				config.email = email;
				process_login_data (email, lines);
				mCallback.toast_by_resource (R.string.sign_up_successful);
				mCallback.track_event ("signUp", "signUpWithEmail", "signUpWithEmail", 0);
	        	mCallback.toggle_menu (new Callback()
		        	{
					@Override
					public void run()
						{
						zero_signin_data();
						/* the settings view might be in the slider */
						mCallback.settings_class().redraw_settings();
						mCallback.activate_layer (layer_before_signin);
						if (callback != null)
							callback.run();
						}		        	
		        	});				
				}
			public void failure (int code, String errtext)
				{
				String txt_failure = getResources().getString (R.string.login_failure);	
				mCallback.alert (txt_failure + ": " + errtext);
				}
			};
		}
	
	public void process_login_data (String email, String[] lines)
		{
		String token = null;
		String name = null;
		String userid = null;
		
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
			else if (fields[0].equals ("name"))
				{
				if (fields.length >= 2)
					name = config.username = fields[1];
				}
			else if (fields[0].equals ("userid"))
				{
				if (fields.length >= 2)
					userid = config.userid = fields[1];
				}
			}
		
		if (email == null && mCallback.signed_in_with_facebook())
			{
			if (mCallback.get_current_layer() == toplayer.SIGNIN)
				mCallback.track_event ("signIn", "signInWithFB", "signInWithFB", 0);
			email = config.email = "[via Facebook]";
			}
		else if (email != null)
			config.email = email;
		
		futil.write_file (getActivity(), "user@" + config.api_server, token);
		
		if (name != null)
			futil.write_file (getActivity(), "name@" + config.api_server, name);
		
		if (email != null)
			futil.write_file (getActivity(), "email@" + config.api_server, email);
		
		if (userid != null)
			futil.write_file (getActivity(), "userid@" + config.api_server, userid);
		
		zero_signin_data();
		/* the settings view might be in the slider */
		mCallback.settings_class().redraw_settings();
		}	    	
   	}