package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.util.Log;

public class switchboard extends Service
	{
	private metadata config = null;

	boolean use_relay = true;
	
	Socket connfd = null;
	InputStream in;
	OutputStream out;
	BufferedReader con;
	
	BufferedReader in_stream;
	BufferedWriter out_stream;
	
	Callback read_callback = null;
	Callback error_callback = null;
	String controlling_activity = "";
	
	Timer ping_timer = null;

	Thread relay_thread = null;
	
	boolean awaiting_close = false;
	
	private final IBinder mBinder = new LocalBinder ();

	@Override
	public void onCreate()
		{
		config = new metadata();
		}

	@Override
	public void onDestroy()
		{
		Log.i ("vtest", "[switchboard] DESTROY");
		}

	public class LocalBinder extends Binder
		{
		switchboard getService()
			{
			Log.i ("vtest", "switchboard getService()");
			return switchboard.this;
			}
		}

	@Override
	public IBinder onBind (Intent intent)
		{
		return mBinder;
		}

	public metadata get_metadata (String whence)
		{
		Log.i ("vtest", "switchboard get_metadata() " + whence);
		return config;
		}

	public void set_callbacks (final String whence, final Callback onRead, final Callback onError)
		{
		controlling_activity = whence;
		read_callback = onRead;
		error_callback = onError;
		}
		
	public void unset_callbacks (String whence)
		{
		if (controlling_activity.equals (whence))
			{
			read_callback = null;
			error_callback = null;
			}
		}
	
	public void open_relay (final String whence, final Callback onConnected, final Callback onRead, final Callback onError)
		{
		if (!use_relay)
			{
			Log.i ("vtest", "open_relay: application configured not to use relay");
			return;
			}
		
		Log.i ("vtest", "open relay, whence: " + whence);
				
		if (connfd != null)
			{
			try
				{
				connfd.close();
				connfd = null;
				}
			catch (Exception e)
				{
				}
			}
		
		awaiting_close = false;
		
		controlling_activity = whence;
		read_callback = onRead;
		error_callback = onError;

		Thread t = new Thread()
			{
			public void run()
				{
				Log.i ("vtest", "opening connection to relay: " + config.relay_server + ":" + config.relay_port);
				
				try
					{
					connfd = new Socket (config.relay_server, config.relay_port);
					in = connfd.getInputStream();
					out = connfd.getOutputStream();
					in_stream = new BufferedReader (new InputStreamReader (connfd.getInputStream()));
					out_stream = new BufferedWriter (new OutputStreamWriter (connfd.getOutputStream()));
					}
				catch (ConnectException ex)
					{
					Log.i ("vtest", ex.getMessage());
					if (error_callback != null)
						error_callback.run_string ("open");
					return;
					}
				catch (IOException ex)
					{
					Log.i ("vtest", ex.getMessage());
					if (error_callback != null)
						error_callback.run_string ("open");
					return;
					}
				
				relay_thread = new Thread()
					{
					public void run()
						{
						Log.i ("vtest", "relay thread started");
						if (onConnected != null)
							onConnected.run_string ("");
						while (process_socket());
						}
					};

				if (ping_timer == null)
					{
					ping_timer = new Timer();
					ping_timer.scheduleAtFixedRate (new PingTask(), 10000, 10000);
					}
				
				try
					{
					relay_thread.start();
					}
				catch (Exception ex)
					{
					Log.i ("vtest", "Error starting relay thread");
					ex.printStackTrace();
					}
				}
			};
				
		t.start();
		}
		
	public boolean process_socket()
		{
		String line;
		
		try
			{
			while ((line = in_stream.readLine()) != null)
				{
				Log.i ("vtest", "** got line: " + line);
				if (read_callback != null)
					read_callback.run_string (line);
				else
					Log.i ("vtest", "no callback, ignoring");
				}
			config.renderer = null;
			if (!awaiting_close && error_callback != null)
				error_callback.run_string ("eof");
			else
				Log.i ("vtest", "socket eof: no callback, ignoring");
			stop_pinging();
			return false;
			}
		catch (SocketTimeoutException ex)
			{
			// Log.i ("vtest", "[relay] socket read timeout");
			return true;
			}
		catch (IOException ex)
			{
			config.renderer = null;
			stop_pinging();
			if (!awaiting_close)
				{
				Log.i ("vtest", ex.getMessage());
				if (error_callback != null)
					error_callback.run_string ("read error");
				else
					Log.i ("vtest", "socket read error: no callback, ignoring");
				}
			else
				Log.i ("vtest", "socket read error while awaiting close, which is expected");
			return false;
			}
		}
	
	public void close_relay()
		{
		Log.i ("vtest", "close relay");
		
		awaiting_close = true;
		
		read_callback = null;
		error_callback = null;
		
		config.renderer = null;
		
		if (!use_relay)
			return;
		
		try 
			{
			if (connfd != null)
				connfd.close();
			}
		catch (Exception ex)
			{
			}
		}
	
	public boolean is_connected()
		{
		// Log.i ("vtest", "is connected: closed? " + connfd.isClosed());
		// Log.i ("vtest", "is connected: connected? " + connfd.isConnected());
		return (connfd != null && connfd.isClosed() == false && connfd.isConnected() == true);
		}
	
	public void relay_post (String s)
		{
		if (!use_relay)
			{
			if (!s.contains("REPORT TICK") && !s.contains ("PING"))
				Log.i ("vtest", "** relay post: " + s + " [note: relay is off]");
			return;
			}
		else
			{
			if (!s.contains("REPORT TICK") && !s.contains ("PING"))
				Log.i ("vtest", "** relay post: " + s);
			}
		
		if (connfd == null || connfd.isClosed())
			{
			if (error_callback != null)
				error_callback.run_string ("closed");
			else
				; // Log.i ("vtest", "socket closed: no error callback"); // noisy
			return;
			}
		else if (connfd.isConnected() == false)
			{
			if (error_callback != null)
				error_callback.run_string ("disconnected");
			else
				Log.i ("vtest", "socket disconnected: no error callback");
			return;
			}
		try
			{
			connfd.setSoTimeout (3000);
			byte[] b = (s + '\n').getBytes ("UTF-8");
			out.write (b);
			}
		catch (IOException ex)
			{
			Log.i ("vtest", ex.getMessage());
			if (error_callback != null)
				error_callback.run_string ("write error");
			else
				Log.i ("vtest", "socket write error: no error callback");
			}
		}
	
	long most_recent_ping = 0L;
	
	class PingTask extends TimerTask
  		{  
		public void run()
       		{
			if (is_connected())
				{
				long now = System.currentTimeMillis() / 1000L;
				if (now - most_recent_ping > 9)
					{
					most_recent_ping = now;
					relay_post ("PING");
					}
				}
			else
				stop_pinging();
       		}
  		}
	
	public void stop_pinging()
		{
		if (ping_timer != null)
			{
			Log.i ("vtest", "not connected, canceling timer");
			/* even though there is a null check above, this will still sometimes fail -- race */
			try { ping_timer.cancel(); } catch (Exception ex) {};
			ping_timer = null;
			}
		}
	}