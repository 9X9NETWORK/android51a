package tv.tv9x9.player;

import android.os.Handler;
import android.util.Log;

public class playerAPI implements futil.Notifier
	{
	public void success (String[] lines)
		{
		}
	
	public void failure (int code, String errtext)
		{
		}
	
	public playerAPI (final Handler handler, metadata m, String cmd)
		{
		player_api (handler, m.api_server, m, cmd);
		}
	
	public playerAPI (final Handler handler, final String api_server, metadata m, String cmd)
		{
		player_api (handler, api_server, m, cmd);
		}

	public void player_api (final Handler handler, final String api_server, metadata m, String cmd)
		{
		final String final_cmd = add_mso (m, cmd);
		
		Log.i ("vtest", "[PlayerAPI " + api_server + "] " + cmd);
	
		Thread t = new Thread ()
			{
			public void run ()
				{
				try
					{
					futil.get_webfile_notify (handler, api_server, final_cmd, playerAPI.this);
					}
				catch (Exception ex)
					{
					Log.i ("vtest", "exception in thread");
					ex.printStackTrace();
					}				
				}
			};
	
		t.start ();
		}
	
	public String add_mso (metadata m, String cmd)
		{	
		String ret = cmd;

		if (m.mso != null)
			{
			if (ret.contains ("?"))
				ret += "&mso=" + m.mso;
			else
				ret += "?mso=" + m.mso;
			}
		
		if (m.region.equals ("tw") || m.region.equals ("zh"))
			{
			if (ret.contains ("?"))
				ret += "&lang=zh";
			else
				ret += "?lang=zh";
			}
		
		if (ret.contains ("?"))
			ret += "&v=40";
		else
			ret += "?v=40";
		
		return ret;
		}
	}