package tv.tv9x9.player;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

public class TabbedEditText extends EditText
	{
    public TabbedEditText (Context context)
    	{
        super (context, null);
    	}

    public TabbedEditText (Context context, AttributeSet attrs)
    	{
        super (context, attrs);
    	}

    public TabbedEditText (Context context, AttributeSet attrs, int defStyle)
    	{
        super (context, attrs, defStyle);
    	}

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event)
    	{
        if (keyCode == KeyEvent.KEYCODE_TAB)
        	{
        	Log.i ("vtest", "TAB FOCUS CHANGE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            focusSearch (FOCUS_DOWN).requestFocus();
            return true;
        	}
        return super.onKeyDown (keyCode, event);
    	}
    
    @Override
    public int getNextFocusDownId()
    	{
    	Log.i ("vtest", "getNextFocusDownId() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	if (getId() == R.id.email)
    		return R.id.password;
    	else if (getId() == R.id.newemail)
    		return R.id.newpassword;    	
    	else
    		return super.getNextFocusDownId();
    	}
    
    @Override
    public View focusSearch (int direction)
    	{
    	Log.i ("vtest", "focusSearch() !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	// Log.i ("vtest", "focusSearch() id=" + getId() + " (" + tvgrid.reflect_R_constant (getId()) + ") direction=" + direction);
    	if (direction == FOCUS_DOWN)
    		{
    		Log.i ("vtest", "FOCUS DOWN query");
        	if (getId() == R.id.email)
        		return findViewById (R.id.password);
        	else if (getId() == R.id.newemail)
        		return findViewById (R.id.newpassword);    
    		}
    	return super.focusSearch (direction);
    	}
	}
