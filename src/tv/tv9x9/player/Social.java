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

import android.util.Log;

public class Social
	{
	metadata config = null;
	
	Socket connfd = null;
	InputStream in;
	OutputStream out;
	BufferedReader con;
	
	BufferedReader in_stream;
	BufferedWriter out_stream;
	
	Callback read_callback = null;
	Callback error_callback = null;
	
	Thread social_thread = null;
	
	Timer ping_timer = null;
		
	boolean awaiting_close = false;
	
	Social (metadata config)
		{
		this.config = config;
		}
	
	public void log (String text)
		{
		Log.i ("vtest", "[Social] " + text);
		}	 
	
	public void open (final String whence, final Callback onConnected, final Callback onRead, final Callback onError)
		{
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
		
		Thread t = new Thread()
			{
			public void run()
				{
				log ("opening connection to social server: " + config.social_server + ":" + config.social_port);
				
				try
					{
					connfd = new Socket (config.social_server, config.social_port);
					in = connfd.getInputStream();
					out = connfd.getOutputStream();
					in_stream = new BufferedReader (new InputStreamReader (connfd.getInputStream()));
					out_stream = new BufferedWriter (new OutputStreamWriter (connfd.getOutputStream()));
					}
				catch (ConnectException ex)
					{
					log (ex.getMessage());
					if (error_callback != null)
						error_callback.run_string ("open");
					return;
					}
				catch (IOException ex)
					{
					log (ex.getMessage());
					if (error_callback != null)
						error_callback.run_string ("open");
					return;
					}
				
				social_thread = new Thread()
					{
					public void run()
						{
						log ("social thread started");
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
					social_thread.start();
					}
				catch (Exception ex)
					{
					log ("Error starting social thread");
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
				log ("** got line: " + line);
				if (read_callback != null)
					read_callback.run_string (line);
				else
					log ("no callback, ignoring");
				}
			config.renderer = null;
			if (!awaiting_close && error_callback != null)
				error_callback.run_string ("eof");
			else
				log ("socket eof: no callback, ignoring");
			stop_pinging();
			return false;
			}
		catch (SocketTimeoutException ex)
			{
			return true;
			}
		catch (IOException ex)
			{
			config.renderer = null;
			stop_pinging();
			if (!awaiting_close)
				{
				log (ex.getMessage());
				if (error_callback != null)
					error_callback.run_string ("read error");
				else
					log ("socket read error: no callback, ignoring");
				}
			else
				log ("socket read error while awaiting close, which is expected");
			return false;
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
					post ("PING");
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
			log ("not connected, canceling timer");
			/* even though there is a null check above, this will still sometimes fail -- race */
			try { ping_timer.cancel(); } catch (Exception ex) {};
			ping_timer = null;
			}
		}	
	
	public void post (String s)
		{
		if (connfd == null || connfd.isClosed())
			{
			if (error_callback != null)
				error_callback.run_string ("closed");
			return;
			}
		else if (connfd.isConnected() == false)
			{
			if (error_callback != null)
				error_callback.run_string ("disconnected");
			else
				log ("socket disconnected: no error callback");
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
			log (ex.getMessage());
			if (error_callback != null)
				error_callback.run_string ("write error");
			else
				log ("socket write error: no error callback");
			}
		}
	
	public void close()
		{
		log ("close");
		
		awaiting_close = true;
		
		read_callback = null;
		error_callback = null;
	
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
		return (connfd != null && connfd.isClosed() == false && connfd.isConnected() == true);
		}	
	}