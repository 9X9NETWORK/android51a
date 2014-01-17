package tv.tv9x9.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class rctest extends RelayActivity
	{
	public rctest()
		{
		identity = "rctest";
		}
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.rctest);
		set_mask();
		}
	
	@Override
	public void onRelayActivityReady()
		{
		setup_buttons();
		}

	public void setup_buttons()
		{
		OnClickListener clicker = new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{
	        	switch (v.getId())
	        		{
		        	case R.id.broadcast_set_screen:
		            	send_broadcast (Message9x9.MESSAGE_SET, Message9x9.EXTRA_DATA_SET_SCREEN);
		            	break;
		            	
		        	case R.id.broadcast_set_net:
			        	send_broadcast (Message9x9.MESSAGE_SET, Message9x9.EXTRA_DATA_SET_NET);
			        	break;
			        	
		        	case R.id.broadcast_switch_home:
			        	send_broadcast (Message9x9.MESSAGE_SWITCH, Message9x9.EXTRA_DATA_SWITCH_HOME);
			        	break;
			        	
		        	case R.id.broadcast_clear_cache:
			        	send_broadcast (Message9x9.MESSAGE_CLEAR, Message9x9.EXTRA_DATA_CLEAR_CACHE);
			        	break;
			        	
		        	case R.id.broadcast_info_ready:
			        	send_broadcast (Message9x9.MESSAGE_INFO, Message9x9.EXTRA_DATA_APK_READY);
			        	break;			        	
	        		}
	
	        	}
			};
		
	    int clickables[] = new int[] 
	    	{
	    	R.id.broadcast_set_screen,
	    	R.id.broadcast_set_net,
	    	R.id.broadcast_switch_home,
	    	R.id.broadcast_clear_cache,
	    	R.id.broadcast_info_ready
	    	};

	    for (int clickable: clickables)
	    	{
	    	View v = findViewById (clickable);
	    	v.setOnClickListener (clicker);
	    	}	
		}
	
	public void send_broadcast (String type, String subtype)
		{
		Intent intent = new Intent (type);
		intent.putExtra (subtype, 0);
		this.sendBroadcast (intent);
		log ("SENT BROADCAST :: type: " + type + " subtype: " + subtype);
		}
	}