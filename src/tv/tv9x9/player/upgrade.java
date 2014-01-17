package tv.tv9x9.player;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class upgrade extends RelayActivity
	{
	String action = null;
	String version = null;
	
	public upgrade()
		{
		identity = "upgrade";
		}

	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);

		Intent wIntent = getIntent();
		if (wIntent != null)
			{
			Bundle b = wIntent.getExtras();
			if (b != null)
				{
				action = b.getString ("tv.9x9.action");
				version = b.getString ("tv.9x9.version");
				}
			}	
		
		setContentView (R.layout.upgrade);
		
		TextView vMessage = (TextView) findViewById (R.id.message);
		
		String txt_want_upgrade_9x9 = getResources().getString (R.string.wantupgrade9x9);
		String txt_want_upgrade_sys = getResources().getString (R.string.wantupgradesys);
		
		vMessage.setText (action.equals (Message9x9.EXTRA_DATA_9x9APK) ? txt_want_upgrade_9x9 : txt_want_upgrade_sys);

		set_mask();
		}

	public void set_mask()
		{
		FrameLayout vMask = (FrameLayout) findViewById (R.id.mask);
		
		DisplayMetrics dm = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics (dm);
	    
	    FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) vMask.getLayoutParams();
	    
	    layout.width = dm.widthPixels;
	    layout.height = dm.heightPixels;
	    
	    vMask.setLayoutParams (layout);
		}
	
	@Override
	public void onRelayActivityReady()
		{
		log ("ready");
		setup_buttons();
		}
	
	public void setup_buttons()
		{
		Button vUpgrade = (Button) findViewById (R.id.upgrade);
		vUpgrade.setVisibility (View.VISIBLE);
	    vUpgrade.setOnClickListener (new OnClickListener()
	    	{
	        @Override
	        public void onClick (View v)
	        	{
	    		Intent intent = new Intent (Message9x9.MESSAGE_UPGRADE);
	   			intent.putExtra (action, version);
	    		log ("sending broadcast: " + action + " :: " + version);
				String txt_requesting_upgrade = getResources().getString (R.string.requesting_upgrade);
	    		toast (txt_requesting_upgrade + " (version " + version + ")");
	    		sendBroadcast (intent);
	        	}
	    	});
		}	
	}