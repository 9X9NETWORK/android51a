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
	
    public StoppableListView (Context context)
    	{
		super (context);
        this.isPagingEnabled = true;
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
        if (this.isPagingEnabled)
        	{
            return super.onTouchEvent (event);
        	}

        return false;
    	}

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
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
    
	int max_overscroll_distance = 120;

	public void set_refresh_function (Handler h, Runnable r)
		{	
		refresh_function = r;
		handler = h;
		}
	
	@Override
    protected boolean overScrollBy
            (int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) 
    	{
		Log.i ("vtest", "deltaY: " + deltaY);
		int max_y_overscroll = deltaY < 0 ? max_overscroll_distance : maxOverScrollY;
		
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
			}
		
        return super.overScrollBy (deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, max_y_overscroll, isTouchEvent);  
    	}
	}