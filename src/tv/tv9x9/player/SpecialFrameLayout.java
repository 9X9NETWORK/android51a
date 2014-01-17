package tv.tv9x9.player;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class SpecialFrameLayout extends FrameLayout
	{
	public SpecialFrameLayout (Context context, AttributeSet attrs, int defStyle)
		{
		super (context, attrs, defStyle);
		}

	public SpecialFrameLayout (Context context, AttributeSet attrs)
		{
		super (context, attrs);
		}

	public SpecialFrameLayout (Context context)
		{
		super (context);
		}
	
	Callback onVideoResize = null;
	
	private static GestureDetector gdet;
	
	public void registerGestureDetector (GestureDetector gd)
		{
		gdet = gd;
		}
	
	public boolean __dispatchTouchEvent (MotionEvent ev)
		{
		super.dispatchTouchEvent (ev);
		Log.i ("vtest", "!!! SpecialFrameLayout touch event");
		return gdet.onTouchEvent (ev);
		}
	
	 @Override
	 public boolean onTouchEvent (MotionEvent event)
	    {
		Log.i ("vtest", "onTouchEvent: action=" + event.getAction());
	    if (event.getAction() == MotionEvent.ACTION_DOWN)
	        {
	    	Log.i ("vtest", "ACTION DOWN, x=" + event.getX() + ", y=" + event.getY());
	        }
	    else if (event.getAction() == MotionEvent.ACTION_UP)
        	{
	    	Log.i ("vtest", "ACTION UP, x=" + event.getX() + ", y=" + event.getY());
        	}
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
	        {
	    	Log.i ("vtest", "ACTION MOVE, x=" + event.getX() + ", y=" + event.getY());
	        }
        super.onTouchEvent (event);
        return true;
	    }
	 
	@Override
	protected void onSizeChanged (int xNew, int yNew, int xOld, int yOld)
		{        
		Log.i ("vtest", "[video] onSizeChanged: xNew=" + xNew + ", yNew=" + yNew);
		
		if (onVideoResize != null)
			onVideoResize.run_two_integers (xNew, yNew);
		
	    super.onSizeChanged (xNew, yNew, xOld, yOld);
		}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom)
		{        
		// Log.i ("vtest", "[video] onLayout: xNew=" + (right-left) + ", yNew=" + (bottom-top)); // noisy
		
		if (onVideoResize != null)
			onVideoResize.run_two_integers (right-left, bottom-top);
		
	    super.onLayout (changed, left, top, right, bottom);
		}
	
	public void setOnVideoResize (Callback callback)
		{
		onVideoResize = callback;
		}
	}