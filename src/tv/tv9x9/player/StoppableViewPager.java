package tv.tv9x9.player;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/* public domain -- http://stackoverflow.com/questions/7814017/disable-viewpager */

public class StoppableViewPager extends android.support.v4.view.ViewPager
	{
    public StoppableViewPager (Context context)
    	{
		super (context);
        this.isPagingEnabled = true;
    	}

	private boolean isPagingEnabled;

    public StoppableViewPager (Context context, AttributeSet attrs)
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
}