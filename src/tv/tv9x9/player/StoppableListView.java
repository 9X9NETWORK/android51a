package tv.tv9x9.player;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListView;

/* public domain -- http://stackoverflow.com/questions/7814017/disable-viewpager */

public class StoppableListView extends ListView
	{
	private Handler handler = null;
	private Runnable refresh_function = null;
	private boolean refresh_in_progress = false;
	private boolean request_refresh = false;
	
	Callback finger_is_down_function = null;
	
    public StoppableListView (Context context)
    	{
		super (context);
        this.isPagingEnabled = true;
    	}

    public void log (String text)
    	{
    	Log.i ("vtest", "[ListView] " + text);
    	}
    
	private boolean isPagingEnabled;

    public StoppableListView (Context context, AttributeSet attrs)
    	{
        super (context, attrs);
        this.isPagingEnabled = true;
    	}

    @Override
    public boolean onTouchEvent (MotionEvent event)
    	{
    	int action = event.getAction();
    	
    	if (action == MotionEvent.ACTION_UP)
    		{
    		log ("onTouchEvent ACTION UP");
    		if (request_refresh)
    			{
    			request_refresh = false;
    			refresh_in_progress = true;
				Log.i ("vtest", "REFRESH!");
				handler.post (new Runnable()
					{
					@Override
					public void run()
						{
						refresh_function.run();
						refresh_in_progress = false;
						}
					});
    			}
    		}
    	
        if (this.isPagingEnabled)
        	{
            return super.onTouchEvent (event);
        	}

        return false;
    	}

    @Override
    public boolean onInterceptTouchEvent (MotionEvent event)
    	{
        if (this.isPagingEnabled)
        	{
            return super.onInterceptTouchEvent (event);
        	}

        return false;
    	}

    public void setPagingEnabled (boolean b)
    	{
        this.isPagingEnabled = b;
    	}
    
	int max_overscroll_distance = 200;

	public void set_refresh_function (Handler h, Runnable r)
		{	
		refresh_function = r;
		handler = h;
		}
	
	public void set_finger_is_down_function (Callback c)
		{
		finger_is_down_function = c;
		}
	
	@Override
    protected boolean overScrollBy
            (int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) 
    	{		
		int max_y_overscroll = deltaY < 0 ? max_overscroll_distance : maxOverScrollY;
				
		if (finger_is_down_function != null && !finger_is_down_function.return_boolean())
			max_y_overscroll = maxOverScrollY;
		
		log ("deltaY: " + deltaY + ", max_y_overscroll: " + max_y_overscroll);
		
		if (scrollY < (-max_y_overscroll / 2))
			{
			if (handler != null && refresh_function != null)
				request_refresh = true;
			}
		
		/*
		if (scrollY < (-max_y_overscroll / 2))
			{
			if (!refresh_in_progress)
				{			
				if (handler != null && refresh_function != null)
					{
					refresh_in_progress = true;
					Log.i ("vtest", "REFRESH!");
					handler.post (new Runnable()
						{
						@Override
						public void run()
							{
							refresh_function.run();
							refresh_in_progress = false;
							}
						});
					}
				}
			else
				log ("refresh in progress");
			}
		*/
		
        return super.overScrollBy (deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, max_y_overscroll, isTouchEvent);  
    	}
	}