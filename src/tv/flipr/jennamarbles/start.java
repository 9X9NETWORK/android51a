package tv.flipr.jennamarbles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class start extends Activity
	{
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);
		launch_start();
		}
	
	@Override
	protected void onStart()
		{
		super.onStart();
		}

	@Override
	protected void onStop()
		{
		super.onStop();
		}

	@Override
	protected void onDestroy ()
		{
		super.onDestroy();
		}
	
	@Override
	protected void onResume()
		{
		super.onResume();
		}	
	
	public void launch_start()
		{
		Log.i ("vtest", "SpinninRec Activity, launching start");
		Intent wIntent = new Intent (this, tv.tv9x9.player.start.class);
		startActivity (wIntent);
		}
	}