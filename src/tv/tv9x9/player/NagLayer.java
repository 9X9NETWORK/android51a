package tv.tv9x9.player;

import com.facebook.widget.LoginButton;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NagLayer extends StandardFragment
	{
	metadata config = null;
		
	NagSlider nag_slider = null;
	
    public interface OnNagListener
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
		}    
    
    OnNagListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.nag_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnNagListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnNagListener");
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
        nag_slider = new NagSlider();
        StoppableViewPager vHomePager = (StoppableViewPager) getView().findViewById (R.id.nagpager);
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
			log ("instantiate: " + position);
			
			final Swapnag sh = new Swapnag (position);			
	
			int layout_resource = 0;
			switch (position)
				{
				case 0: layout_resource = R.layout.nag1; break;
				case 1: layout_resource = R.layout.nag2; break;
				case 2: layout_resource = R.layout.nag3; break;				
				}
			
			LinearLayout page = (LinearLayout) View.inflate (getActivity(), layout_resource, null);
			sh.page = page;
			
			((StoppableViewPager) container).addView (page, 0);
			
			String signin_logo = getResources().getString (R.string.signin_logo);
			if (signin_logo != null)
				{
				int logo_id = getResources().getIdentifier (signin_logo, "drawable", getActivity().getPackageName());
				
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
			        	mCallback.enable_home_layer();
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
						mCallback.track_event ("signIn", "signInWithEmail-enforce", "signInWithEmail-enforce", 0);
		        		mCallback.enable_signin_layer (new Runnable()
			    			{
			        		@Override
			        		public void run()
				        		{
			        			mCallback.enable_home_layer();
				        		}
			    			});
			        	}
					});	
				}
			
			/* disable facebook buttons if there is no facebook app on this device */
			if (!mCallback.has_facebook())
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
				mCallback.fezbuk2 (page);
			
			return sh;
			}
		
		@Override
		public void destroyItem (ViewGroup container, int position, Object object)
			{
			log ("destroy: " + position);
			Swapnag sh = (Swapnag) object;
			((StoppableViewPager) container).removeView (sh.page);
			}
		
		@Override
		public void setPrimaryItem (ViewGroup container, int position, Object object)
			{
			Swapnag sh = (Swapnag) object;
			ImageView vDot1 = (ImageView) getView().findViewById (R.id.nag_dot1);
			ImageView vDot2 = (ImageView) getView().findViewById (R.id.nag_dot2);
			ImageView vDot3 = (ImageView) getView().findViewById (R.id.nag_dot3);			
			vDot1.setImageResource (position == 0 ? R.drawable.walkthrough_dot_active : R.drawable.walkthrough_dot);
			vDot2.setImageResource (position == 1 ? R.drawable.walkthrough_dot_active : R.drawable.walkthrough_dot);
			vDot3.setImageResource (position == 2 ? R.drawable.walkthrough_dot_active : R.drawable.walkthrough_dot);			
			}
		}   
	}