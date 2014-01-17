package tv.tv9x9.player;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

public class shear extends Activity
	{
	Handler in_main_thread = new Handler();
	
	int animation_count = 0;
	
	@Override
	public void onCreate (Bundle savedInstanceState)
		{
		super.onCreate (savedInstanceState);

		requestWindowFeature (Window.FEATURE_NO_TITLE);
		setContentView (R.layout.shear);
		
		reset_rushmore();
		
		animate_trapezoid();
		}

	public void reset_rushmore()
		{
		Bitmap rushmore = BitmapFactory.decodeResource (getResources(), R.drawable.rushmore);
		
		TrapezoidView vTrapezoid = (TrapezoidView) findViewById (R.id.thumbnail);
		vTrapezoid.setMaxIndentation (-150f);
		vTrapezoid.setImageBitmap (rushmore);
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

	
	public class CustomAnim extends Animation
		{
	    private ImageView vThumb;
	    private float finalVal;
	    private float startVal;

	    public CustomAnim (ImageView layout, float finalVal)
	    	{
	        this.vThumb = layout;
	        this.finalVal = finalVal;
	        this.startVal = 0f;
	        Log.i ("vtest", "[anim] instantiate");
	    	}

	    @Override
	    protected void applyTransformation (float interpolatedTime, Transformation t)
	    	{
	    	Log.i ("vtest", "[anim] apply " + interpolatedTime);
	    	
	        // vThumb.setXYZ(interpolatedTime * (finalVal - startVal) + startVal);
	    	float deformation = interpolatedTime * (finalVal - startVal) + startVal;
			//Bitmap bm = b (deformation);
			//vThumb.setImageBitmap (bm);
	    	}

	    @Override
	    public boolean willChangeBounds()
	    	{
	        return true;
	    	} 
		}
	
	@SuppressLint("NewApi")
	public void animate_trapezoid()
		{
		TrapezoidView vTrapezoid = (TrapezoidView) findViewById (R.id.thumbnail);
		
	   	// ObjectAnimator anim1 = ObjectAnimator.ofFloat (vTrapezoid, "tallness", height_hack, 0f);	   	
	   	// anim1.setDuration (3000);
		
	   	ObjectAnimator anim2 = ObjectAnimator.ofFloat (vTrapezoid, "indentation", 0f, vTrapezoid.getMaxIndentation());	   	
		anim2.setDuration (2000);
		
		anim2.addListener (new AnimatorListener()
			{
            @Override
            public void onAnimationStart (Animator arg0) {}

            @Override
            public void onAnimationRepeat (Animator arg0) {}

            @Override
            public void onAnimationEnd (Animator arg0)
            	{
            	in_main_thread.post (new Runnable()
            		{
					@Override
					public void run()
						{
						if (++animation_count <= 50)
							{
			            	reset_rushmore();
							animate_trapezoid();
							}
						}
            		});
            	}

			@Override
			public void onAnimationCancel (Animator arg0) {}				
			});
		
		// anim1.start();
		
	   	ObjectAnimator anim3 = ObjectAnimator.ofFloat (vTrapezoid, "alpha", 1.0f, 0f);	  
		anim3.setDuration (2000);
		
		anim2.start();
		anim3.start();
		}
	
	@TargetApi(11)
	public static Bitmap OBSOLETE_b (Context ctx, float deform2, float height)
		{
		if (height <= 0f) height = 1;
		// empty
		// Bitmap  bitmap2 = Bitmap.createBitmap (200, 200, Bitmap.Config.ARGB_8888);
		
		// immutable bitmap
		//Bitmap bitmap2 = BitmapFactory.decodeResource (this.getResources(), R.drawable.unavailable);

		//mutable bitmap
		 BitmapFactory.Options opt = new BitmapFactory.Options();
		 opt.inMutable = true;
		 Bitmap bitmap2 = BitmapFactory.decodeResource (ctx.getResources(), R.drawable.rushmore, opt);
		 
		Canvas canvas2 = new Canvas (bitmap2);       
		//canvas2.drawColor(Color.WHITE);
		//Paint rectPaint2 = new Paint();
		//rectPaint2.setColor(Color.GREEN);
		//canvas2.drawRect(20, 20, 180, 180, rectPaint2);
		Matrix matrix2 = new Matrix();
		
		//float deform2 = 60f;
		
		// float[] src2 = new float[] { 0, 0, bitmap2.getWidth(), 0, bitmap2.getWidth(), bitmap2.getHeight(), 0, bitmap2.getHeight() };
		// float[] dst2 = new float[] { 0, 0, bitmap2.getWidth() - deform2, deform2, bitmap2.getWidth() - deform2, bitmap2.getHeight() - deform2, 0, bitmap2.getHeight() };
		
		float[] src2 = new float[] {
										0, 0,
										bitmap2.getWidth(), 0,
										bitmap2.getWidth(), bitmap2.getHeight(), 
										0, bitmap2.getHeight()
									};
		
		float[] old_dst2 = new float[] {
										0, 0,
										bitmap2.getWidth() - deform2, deform2, 
										bitmap2.getWidth() - deform2, bitmap2.getHeight() - deform2,
										0, bitmap2.getHeight()
									};

		float[] dst2_again = new float[] {
										0 + deform2, 0,
										bitmap2.getWidth() - deform2, 0, 
										bitmap2.getWidth(), bitmap2.getHeight(),
										0, bitmap2.getHeight()
									};		

		float[] dst2 = new float[] {
										0 + deform2, 0,
										bitmap2.getWidth() - deform2, 0, 
										bitmap2.getWidth(), height,
										0, height
									};
		
		matrix2.setPolyToPoly(src2, 0, dst2, 0, src2.length >> 1);
		Bitmap bMatrix2= Bitmap.createBitmap (bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), matrix2, true);
		return bMatrix2;
		}
	}

/*
Bitmap  bitmap2 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
Canvas canvas2 = new Canvas(bitmap2);       
canvas2.drawColor(Color.WHITE);
Paint rectPaint2 = new Paint();
rectPaint2.setColor(Color.GREEN);
canvas2.drawRect(20, 20, 180, 180, rectPaint2);
Matrix matrix2 = new Matrix();
float deform2 = 20f;
float[] src2 = new float[] { 0, 0, bitmap2.getWidth(), 0, bitmap2.getWidth(), bitmap2.getHeight(), 0, bitmap2.getHeight() };
float[] dst2 = new float[] { 0, 0, bitmap2.getWidth() - deform2, deform2, bitmap2.getWidth() - deform2, bitmap2.getHeight() - deform2, 0, bitmap2.getHeight() };
matrix2.setPolyToPoly(src2, 0, dst2, 0, src2.length >> 1);
Bitmap bMatrix2= Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), matrix2, true);

ImageView ivSecond = (ImageView) findViewById(R.id.ivSecond);
ivSecond.setImageBitmap(bMatrix2);
*/