package tv.tv9x9.player;

import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class futil
	{
	public interface Notifier
		{
		public void success (String[] lines);

		public void failure (int code, String errtext);
		}

	public static String write_file (Context ctx, String filename, String data)
		{
		try
			{
			Log.i ("fl", "Saving file: " + filename);
			OutputStreamWriter out = new OutputStreamWriter (ctx.openFileOutput (filename, 0));
			out.write (data);
			out.close ();
			}
		catch (Throwable t)
			{
			Log.i ("fl", "write_file ERROR: " + t.toString ());
			return "ERROR:" + t.toString ();
			}

		return "";
		}

	public static String append_to_file (Context ctx, String filename, String data)
		{
		return "";
		}

	public static String read_file (Context ctx, String filename)
		{
		String ret = "";

		try
			{
			InputStream in = ctx.openFileInput (filename);
			if (in != null)
				{
				int size = in.available ();
				byte[] buf = new byte[size];
				in.read (buf);
				ret = new String (buf);
				Log.i ("fl", "read_file \"" + filename + "\" successful, " + Integer.toString (size) + " bytes");
				in.close ();
				}
			}
		catch (Throwable t)
			{
			Log.i ("fl", "read_file \"" + filename + "\" ERROR: " + t.toString ());
			ret = "ERROR:" + t.toString ();
			}
		return ret;
		}

	public static String get_any_webfile (Handler handler, String url, String header_keys[], String header_values[])
		{
		Log.i ("fl", "get_any_webfile: " + url);
	
		HttpParams parameters = new BasicHttpParams();
		HttpConnectionParams.setSocketBufferSize (parameters, 16384);		
		
		//HttpClient client = new DefaultHttpClient ();
		HttpClient client = new DefaultHttpClient (parameters);
		
		HttpGet request = new HttpGet (url);
		
		if (header_keys != null)
			{
			for (int i = 0; i < header_keys.length; i++)
				request.addHeader (header_keys[i], header_values[i]);
			}
		
		String answer;
		ResponseHandler<String> responseHandler = new BasicResponseHandler ();
	
		try
			{
			answer = client.execute (request, responseHandler);
			}
		catch (Exception ex)
			{
			// 'org.apache.http.client.HttpResponseException: Not Found'
			answer = "ERROR:" + ex.toString ();
			}
	
		return answer;
		}

	public static String http_post (String url, String header_keys[], String header_values[])
		{
		Log.i ("fl", "get_any_webfile: " + url);	
		
		HttpClient client = new DefaultHttpClient ();
		
		HttpPost request = new HttpPost (url);
		
		if (header_keys != null)
			{
			for (int i = 0; i < header_keys.length; i++)
				request.addHeader (header_keys[i], header_values[i]);
			}
		
		String answer;
		ResponseHandler<String> responseHandler = new BasicResponseHandler ();
	
		try
			{
			answer = client.execute (request, responseHandler);
			}
		catch (Exception ex)
			{
			// 'org.apache.http.client.HttpResponseException: Not Found'
			answer = "ERROR:" + ex.toString ();
			}
	
		return answer;
		}
	
	public static String get_webfile (Handler handler, String host, String filename)
		{
		Log.i ("fl", "[" + host + "] Requesting: " + filename);

		/* temporary */
		String player_api = filename.contains ("pdr_process") ? "hello" : "playerAPI";
		
		HttpClient client = new DefaultHttpClient ();
		HttpGet request = new HttpGet ("http://" + host + "/" + player_api + "/" + filename);
		
		String answer;
		ResponseHandler<String> responseHandler = new BasicResponseHandler ();

		try
			{
			answer = client.execute (request, responseHandler);
			}
		catch (Exception ex)
			{
			// 'org.apache.http.client.HttpResponseException: Not Found'
			answer = "ERROR:" + ex.toString ();
			}

		return answer;
		}

	public static void get_webfile_notify (Handler handler, String host, final String filename, final Notifier event)
		{
		final String result = get_webfile (handler, host, filename);
		
		if (result == null)
			{
			Log.i ("vtest", "get webfile notify: failure -- null returned for: " + filename);
			handler.post (new Runnable()
				{
				@Override
				public void run()
					{
					event.failure (1, "NULL");
					}
				});
			}

		final String[] lines = result.split ("[\n\r]+");
		if (lines[0].startsWith ("0\t"))
			{
			final String shifted[] = (lines.length >= 2) ? new String [lines.length - 2] : new String [0];
			
			if (lines.length >= 2)
				System.arraycopy (lines, 2, shifted, 0, lines.length - 2);
			
			try
				{
				handler.post (new Runnable()
					{
					@Override
					public void run()
						{
						event.success (shifted);
						}
					});
				}
			catch (Exception ex)
				{
				ex.printStackTrace();
				}
			}
		else
			{
			// Log.i ("vtest", "FAIL '" + filename + "', RAW TEXT: " + result.substring (0, 200));
			Log.i ("vtest", "FAIL '" + filename + "', RAW TEXT: " + result);
			handler.post (new Runnable()
				{
				@Override
				public void run()
					{
					event.failure (1, result);
					}
				});
			}
		}

	public static String get_universal_webfile (String filename)
		{
		Log.i ("fl", "Requesting: " + filename);

		HttpClient client = new DefaultHttpClient ();
		HttpGet request = new HttpGet (filename);

		String answer;
		ResponseHandler <String> responseHandler = new BasicResponseHandler ();

		try
			{
			answer = client.execute (request, responseHandler);
			}
		catch (Exception ex)
			{
			// 'org.apache.http.client.HttpResponseException: Not
			// Found'
			answer = "ERROR:" + ex.toString ();
			}

		return answer;
		}
	}