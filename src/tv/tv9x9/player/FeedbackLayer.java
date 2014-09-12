package tv.tv9x9.player;

import tv.tv9x9.player.main.toplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class FeedbackLayer extends StandardFragment
	{
	metadata config = null;
	    
    boolean feedback_initialized = false;
    
    enum feedbacks { CONTACT, SUGGESTION, BUG };   
    
    feedbacks current_feedback = feedbacks.CONTACT;    
    toplayer layer_before_feedback = toplayer.HOME;
    
    public interface OnFeedbackListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void alert (String text);
    	public void toast_by_resource (int id);
    	public void toggle_menu();
    	public void toggle_menu	(final Callback cb);
    	public void set_layer (toplayer layer);
		}    
    
    OnFeedbackListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.feedback_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnFeedbackListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnFeedbackListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "feedback";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
    
    public void feedback_init (metadata config, toplayer current_layer)
    	{
    	this.config = config;
    	
    	if (!feedback_initialized)
			{
			View vRemove = getView().findViewById (mCallback.is_tablet() ? R.id.feedback_radio_phone : R.id.feedback_radio_tablet);
	    	if (vRemove != null)
				((ViewManager) vRemove.getParent()).removeView (vRemove);
			feedback_initialized = true;
			}
		
		current_feedback = feedbacks.CONTACT;
		
		if (current_layer != toplayer.FEEDBACK)
			layer_before_feedback = current_layer;
	    }
    
    public void setup_feedback_buttons()
		{
		ImageView vAppIcon = (ImageView) getView().findViewById (R.id.feedback_app_icon);  
		if (vAppIcon != null)
			{
			String app_icon = getResources().getString (R.string.logo);
			if (app_icon != null)
				{
				int app_icon_id = getResources().getIdentifier (app_icon, "drawable", getActivity().getPackageName());
				vAppIcon.setImageResource (app_icon_id);
				}
			}
		
		if (config.email != null)
			{
			TextView vEmail = (TextView) getView().findViewById (R.id.feedback_email);
			vEmail.setText (config.email);
			}
		
		View vBack = getView().findViewById (R.id.feedback_back);
		vBack.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	feedback_close();
	        	}
			});
		
		View vContact = getView().findViewById (R.id.feedback_radio_contact);
		if (vContact != null)
			vContact.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	current_feedback = feedbacks.CONTACT;
	        	redraw_feedback();
	        	}
			});	
	
		View vSuggestion = getView().findViewById (R.id.feedback_radio_suggestion);
		if (vSuggestion != null)
			vSuggestion.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	current_feedback = feedbacks.SUGGESTION;
		        	redraw_feedback();
		        	}
				});	
		
		View vBug = getView().findViewById (R.id.feedback_radio_bug);
		if (vBug != null)
			vBug.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	current_feedback = feedbacks.BUG;
		        	redraw_feedback();
		        	}
				});	
		
		View vSend = getView().findViewById (R.id.feedback_send);
		if (vSend != null)
			vSend.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	log ("feedback send");
		        	send_feedback();
		        	}
				});	
		
		View vFeedback = getView().findViewById (R.id.feedbacklayer);		
		if (vFeedback != null)
			vFeedback.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("feedback layer ate my tap!");
		        	}
				});		
		}
	
	public void redraw_feedback()
		{
		ImageView vContact = (ImageView) getView().findViewById (R.id.feedback_radio_contact_image);
		vContact.setImageResource (current_feedback == feedbacks.CONTACT ? R.drawable.btn_radio_on : R.drawable.btn_radio_off);
		ImageView vSuggestion = (ImageView) getView().findViewById (R.id.feedback_radio_suggestion_image);
		vSuggestion.setImageResource (current_feedback == feedbacks.SUGGESTION ? R.drawable.btn_radio_on : R.drawable.btn_radio_off);
		ImageView vBug = (ImageView) getView().findViewById (R.id.feedback_radio_bug_image);
		vBug.setImageResource (current_feedback == feedbacks.BUG ? R.drawable.btn_radio_on : R.drawable.btn_radio_off);
		}
	
	public void send_feedback()
		{    	
		TextView vMessage = (TextView) getView().findViewById (R.id.feedback_text);
		String message = vMessage.getText().toString();
		
		if (message.equals (""))
			{
			mCallback.alert ("Please provide a message!");
			return;
			}
		
		TextView vEmail = (TextView) getView().findViewById (R.id.feedback_email);
		String email = vEmail.getText().toString();
		
		String type = current_feedback.toString().toLowerCase();
		
		String email_encoded = util.encodeURIComponent (email);
		String message_encoded = util.encodeURIComponent (message);
		
		String version_code = "unknown";	
		try
			{
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo (getActivity().getPackageName(), 0);
			version_code = pInfo.versionName;
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}	
		
		String query = "userReport?key=email,description,os,version&value=" + email_encoded + "," + message_encoded + ",android," + version_code + "&type=" + type;
		new playerAPI (mCallback.get_main_thread(), config, query)
			{
			public void success (String[] lines)
				{
				log ("userReport sent successfully");
				mCallback.toast_by_resource (R.string.feedback_thanks);
				feedback_close();
				}		
			public void failure (int code, String errtext)
				{
				mCallback.alert ("ERROR! " + errtext);
				}
			};	
		}
	
	public void feedback_close()
	    {
		mCallback.toggle_menu (new Callback()
			{
			public void run()
				{
		    	mCallback.set_layer (layer_before_feedback);
		    	mCallback.toggle_menu();
				}
			});
	    }    
	}