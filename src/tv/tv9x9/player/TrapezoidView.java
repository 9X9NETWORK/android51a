package tv.tv9x9.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class TrapezoidView extends ImageView
	{
	private Bitmap original = null;
	
	private float indentation = 0f;
	private float tallness = 0f;
	private float original_width = 0;
	private float original_height = 0;
	private float max_indentation = 0f;
	
	private Context ctx = null;
	
	public TrapezoidView (Context context)
		{
		super (context);
		ctx = context;
		}
	
	public TrapezoidView (Context context, AttributeSet attrs)
		{
		super (context, attrs);
		ctx = context;
		}
	
	public TrapezoidView (Context context, AttributeSet attrs, int defStyle)
		{
		super (context, attrs, defStyle);
		ctx = context;
		}
	
	public void setMaxIndentation (float new_max_indentation)
		{
		max_indentation = new_max_indentation;
		}
	
	public float getMaxIndentation()
		{
		return max_indentation;
		}
	
	public void setTallness (float new_tallness)
		{
		tallness = new_tallness;
		Log.i ("vtest", "[trapezoid] set tallness: " + new_tallness);
		// ViewGroup parentView = (ViewGroup) TrapezoidView.this.getParent();
		
		FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) getLayoutParams();
		
		layout.height = (int) new_tallness;
		layout.width = (int) original_width;
		
		setLayoutParams (layout);
		
		super.setMaxHeight ((int) tallness);
		}
	
	public float getTallness()
		{
		return tallness;
		}
	
	public void setIndentation (float new_indentation)
		{
		Log.i ("vtest", "set indentation: " + new_indentation);
		
		float ratio = Math.abs ((max_indentation - new_indentation) / max_indentation);
		
		float new_height = original_height * ratio;
		if (new_height <= 2f) new_height = 2f;
		
		Log.i ("vtest", "animation ratio: " + ratio + ", original height: " + original_height + ", new height: " + new_height);
		
		indentation = new_indentation;
		if (indentation == 0f)
			super.setImageBitmap (original);
		else
			{
			Bitmap b2 = trapezoidal_bitmap (ctx, indentation, new_height);
			if (b2 != null)
				super.setImageBitmap (b2);
			else
				super.setImageBitmap (original);
			}
		}
	
	public float getIndentation()
		{
		Log.i ("vtest", "get indentation: " + indentation);
		return indentation;
		}
	
	@Override
	public void setImageBitmap (Bitmap bm)
		{
		FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) getLayoutParams();
		
		// layout.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		// layout.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		
		layout.height = android.view.ViewGroup.LayoutParams.FILL_PARENT;
		layout.width = android.view.ViewGroup.LayoutParams.FILL_PARENT;
		
		setLayoutParams (layout);
		
		original = bm;
		super.setImageBitmap (bm);
		
		original_width = bm.getWidth();
		original_height = bm.getHeight();
		}
	
	@TargetApi(11)
	public Bitmap trapezoidal_bitmap (Context ctx, float deform2, float height)
		{
		if (height <= 1f) height = 1f;

		/* mutable bitmap required */
		// BitmapFactory.Options opt = new BitmapFactory.Options();
		// opt.inMutable = true;
		// Bitmap bitmap2 = BitmapFactory.decodeResource (ctx.getResources(), R.drawable.rushmore, opt);
		Bitmap bitmap2 = original.copy (original.getConfig(), true);
		
		Matrix matrix2 = new Matrix();
		
		float[] src2 = new float[] {
										0, 0,
										bitmap2.getWidth(), 0,
										bitmap2.getWidth(), bitmap2.getHeight(), 
										0, bitmap2.getHeight()
									};
		
		float[] dst2 = new float[] {
										0 + deform2, 0,
										bitmap2.getWidth() - deform2, 0, 
										bitmap2.getWidth(), height,
										0, height
									};
		
		matrix2.setPolyToPoly (src2, 0, dst2, 0, src2.length >> 1);
		
		Bitmap bMatrix2= Bitmap.createBitmap (bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), matrix2, true);
		return bMatrix2;
		}
	}