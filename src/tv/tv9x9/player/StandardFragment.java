package tv.tv9x9.player;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

public class StandardFragment extends Fragment 
	{
	String identity = "fragment";
	
	public void log (String text)
		{
		Log.i ("vtest", "[" + identity + "] " + text);
		}
	
    public void toast (String text)
    	{
	 	Toast.makeText (getActivity(), text, Toast.LENGTH_SHORT).show();
    	} 
	}
