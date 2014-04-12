package tv.tv9x9.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.util.Log;

public class bitmappery
	{
	public static Bitmap getRoundedCornerBitmap (Bitmap bitmap, int square)
		{
		if (bitmap == null)
			{
			Log.i ("vtest", "bitmap for " + square + " is null");
			return null;
			}
		Bitmap output = Bitmap.createBitmap (square, square, Config.ARGB_8888);
		Canvas canvas = new Canvas (output);
	
		final int color = 0xff424242;
		final Paint paint = new Paint ();
		// final Rect rect = new Rect (0, 0, bitmap.getWidth(),
		// bitmap.getHeight());
	
		final Rect src_rect;
		final Rect dst_rect = new Rect (0, 0, square, square);
	
		final RectF rectF = new RectF (dst_rect);
		final float roundPx = 5;
	
		if (bitmap.getWidth () > bitmap.getHeight ())
			{
			int offset = (bitmap.getWidth () - bitmap.getHeight ()) / 2;
			src_rect = new Rect (offset, 0, bitmap.getHeight (), bitmap.getHeight ());
			}
		else
			{
			int offset = (bitmap.getHeight () - bitmap.getWidth ()) / 2;
			src_rect = new Rect (0, offset, bitmap.getWidth (), bitmap.getWidth ());
			}
	
		paint.setAntiAlias (true);
		canvas.drawARGB (0, 0, 0, 0);
	
		paint.setColor (color);
		canvas.drawRoundRect (rectF, roundPx, roundPx, paint);
	
		paint.setXfermode (new PorterDuffXfermode (Mode.SRC_IN));
		canvas.drawBitmap (bitmap, src_rect, dst_rect, paint);
	
		return output;
		}
	
	public static Bitmap getCroppedBitmap (Bitmap bitmap, int height, int width, boolean gravity_to_left)
		{
		if (bitmap == null)
			{
			Log.i ("vtest", "[bitmappery] bitmap is null");
			return null;
			}
		if (width <= 0 || height <= 0)
			{
			Log.i ("vtest", "[bitmappery] bad size, width=" + width + ", height=" + height);
			return null;
			}
		
		Bitmap output = Bitmap.createBitmap (width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas (output);
	
		final int color = 0xff424242;
		final Paint paint = new Paint ();
	
		final Rect src_rect;
		final Rect dst_rect = new Rect (0, 0, width, height);
	
		final RectF rectF = new RectF (dst_rect);
	
		int right = bitmap.getWidth() / 10;
		if (gravity_to_left)
			src_rect = new Rect (0, 0, right, bitmap.getHeight());
		else
			src_rect = new Rect (bitmap.getWidth() - width, 0, bitmap.getWidth(), bitmap.getHeight());
		paint.setAntiAlias (true);
		canvas.drawARGB (0, 0, 0, 0);
	
		paint.setColor (color);
		canvas.drawRect (rectF, paint);
	
		paint.setXfermode (new PorterDuffXfermode (Mode.SRC_IN));
		canvas.drawBitmap (bitmap, src_rect, dst_rect, paint);
	
		return output;
		}	
	
	public static Bitmap cropped_episode_bitmap (Bitmap bitmap, int height, int width)
		{
		if (bitmap == null)
			{
			Log.i ("vtest", "[bitmappery] bitmap is null");
			return null;
			}
		if (width <= 0 || height <= 0)
			{
			Log.i ("vtest", "[bitmappery] bad size, width=" + width + ", height=" + height);
			return null;
			}
		
		Bitmap output = Bitmap.createBitmap (width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas (output);
	
		final int color = 0xff424242;
		final Paint paint = new Paint ();
	
		int midtop  = (bitmap.getHeight() - height) / 2;
		int midleft = (bitmap.getWidth()  - width)  / 2;
		
		final Rect src_rect = new Rect (midleft, midtop, midleft + width, midtop + height);
		final Rect dst_rect = new Rect (0, 0, width, height);
	
		final RectF rectF = new RectF (dst_rect);
	
		paint.setAntiAlias (true);
		canvas.drawARGB (0, 0, 0, 0);
	
		paint.setColor (color);
		canvas.drawRect (rectF, paint);
	
		paint.setXfermode (new PorterDuffXfermode (Mode.SRC_IN));
		canvas.drawBitmap (bitmap, src_rect, dst_rect, paint);
	
		return output;
		}
	
	public static Bitmap getRoundedTopTwoCorners (Bitmap bitmap)
		{
		if (bitmap == null)
			{
			Log.i ("vtest", "bitmap is null");
			return null;
			}
		Bitmap output = Bitmap.createBitmap (bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas (output);
	
		final int color = 0xff424242;
		final Paint paint = new Paint();
		// final Rect rect = new Rect (0, 0, bitmap.getWidth(),
		// bitmap.getHeight());
	
		final Rect src_rect = new Rect (0, 0, bitmap.getWidth(), bitmap.getHeight());
		final Rect dst_rect = new Rect (0, 0, bitmap.getWidth(), bitmap.getHeight());
	
		final RectF rectF = new RectF (dst_rect);
		final float roundPx = 5;
	
		if (bitmap.getWidth() > bitmap.getHeight())
			{
			// int offset = (bitmap.getWidth () - bitmap.getHeight ()) / 2;
			// src_rect = new Rect (offset, 0, bitmap.getHeight (), bitmap.getHeight ());
			}
		else
			{
			// int offset = (bitmap.getHeight () - bitmap.getWidth ()) / 2;
			// src_rect = new Rect (0, offset, bitmap.getWidth (), bitmap.getWidth ());
			}
	
		paint.setAntiAlias (true);
		canvas.drawARGB (0, 0, 0, 0);
	
		paint.setColor (color);
		canvas.drawRoundRect (rectF, roundPx, roundPx, paint);
	
		paint.setXfermode (new PorterDuffXfermode (Mode.SRC_IN));
		canvas.drawBitmap (bitmap, src_rect, dst_rect, paint);
	
		return output;
		}
	
	public static Bitmap generate_triple_thumbnail (String channel_id, int thumb_width, int thumb_height, int margin_px, String f1, String f2, String f3)
		{
		Bitmap output = Bitmap.createBitmap (3 * thumb_width + 2 * margin_px, thumb_height, Config.ARGB_8888);
		Canvas canvas = new Canvas (output);
		
		canvas.drawARGB (0, 0, 0, 0);
		
		// final Paint paint = new Paint();
		
		if (f1 != null)
			{
			Log.i ("vtest", "[TRIPLE] ch=" + channel_id + " f1=" + f1);
			Bitmap bm1 = BitmapFactory.decodeFile (f1);
			if (bm1 != null)
				{
				int left = 0;
				int right = left + thumb_width;
				final Rect src_rect = new Rect (0, 0, bm1.getWidth(), bm1.getHeight());
				final Rect dst_rect = new Rect (left, 0, right, thumb_height);
				canvas.drawBitmap (bm1, src_rect, dst_rect, null);
				}
			}
		
		if (f2 != null)
			{
			Log.i ("vtest", "[TRIPLE] ch=" + channel_id + " f2=" + f2);
			Bitmap bm2 = BitmapFactory.decodeFile (f2);
			if (bm2 != null)
				{
				int left = thumb_width + margin_px;
				int right = left + thumb_width;
				final Rect src_rect = new Rect (0, 0, bm2.getWidth(), bm2.getHeight());
				final Rect dst_rect = new Rect (left, 0, right, thumb_height);
				canvas.drawBitmap (bm2, src_rect, dst_rect, null);
				}
			}		
		
		if (f3 != null)
			{
			Log.i ("vtest", "[TRIPLE] ch=" + channel_id + " f3=" + f3);
			Bitmap bm3 = BitmapFactory.decodeFile (f3);
			if (bm3 != null)
				{
				int left = thumb_width + margin_px + thumb_width + margin_px;
				int right = left + thumb_width;
				final Rect src_rect = new Rect (0, 0, bm3.getWidth(), bm3.getHeight());
				final Rect dst_rect = new Rect (left, 0, right, thumb_height);
				canvas.drawBitmap (bm3, src_rect, dst_rect, null);
				}
			}			
		return output;
		}
	}