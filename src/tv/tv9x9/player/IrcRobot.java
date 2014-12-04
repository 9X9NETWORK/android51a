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

import tv.tv9x9.player.Social.PingTask;

abstract public class IrcRobot
	{
	String host;
	int port;
	String password;
	String nick;
	
	private Socket fd = null;
	private InputStream in;
	private OutputStream out;
	private BufferedReader con;
	
	private BufferedReader in_stream;
	private BufferedWriter out_stream;
	
	private boolean awaiting_close = false;
	
	IrcRobot()
		{		
		}
	
	public void log (String text)
		{
		onLog (text);
		}
		
	public void open (String host, int port, String password, String nick)
		{
		this.host = host;
		this.port = port;
		this.password = password;
		this.nick = nick;
		
		force_close();
	
		Thread t = new Thread()
			{
			public void run()
				{				
				open_connection();
				}
			};

		t.start();
		}
	
	public void reconnect()
		{		
		}
	
	public void close()
		{		
		log ("close");
		awaiting_close = true;
		force_close();
		}
	
	private boolean open_connection()
		{
		log ("opening connection to irc server: " + host + ":" + port);
		
		try
			{
			fd = new Socket (host, port);
			in = fd.getInputStream();
			out = fd.getOutputStream();
			in_stream = new BufferedReader (new InputStreamReader (fd.getInputStream()));
			out_stream = new BufferedWriter (new OutputStreamWriter (fd.getOutputStream()));
			}
		catch (ConnectException ex)
			{
			log (ex.getMessage());
			onError ("open", ex.getMessage());
			return false;
			}
		catch (IOException ex)
			{
			log (ex.getMessage());
			onError ("open", ex.getMessage());
			return false;
			}
		
		Thread t = new Thread()
			{
			public void run()
				{
				log ("irc message thread started");
				onConnected();
				while (process_socket());
				}
			};

		start_pinging();
			
		try
			{
			t.start();
			return true;
			}
		catch (Exception ex)
			{
			onError ("open", "error starting irc thread");
			ex.printStackTrace();
			}
		
		return false;
		}

	private boolean process_socket()
		{
		String line;
		
		try
			{
			while ((line = in_stream.readLine()) != null)
				{
				if (!line.matches ("^\\s*$"))
					{
					log ("** got line: " + line);
					// :soylent!~soylent@75.144.20.97 PRIVMSG u_1-1411 :HI
					String fields[] = line.split (" ", 4);
					if (fields[1].equals ("PRIVMSG"))
						{
						onMessage (fields[0], fields[3]);
						}
					else if (fields[0].equals ("PING"))
						{
						String response = "";
						for (int i = 1; i < fields.length; i++)
							response += " " + fields[i];	
						log ("responding to ping: " + response);
						post ("PONG" + response);
						}
					else if (fields[0].equals ("ERROR"))
						{
						String rest = "";
						for (int i = 1; i < fields.length; i++)
							rest += " " + fields[i];	
						onMessage ("IRC Server", rest);
						}
					else
						log ("unknown IRC message: " + line);
					}
				}
			if (!awaiting_close)
				onError ("eof", "eof");
			stop_pinging();
			onDisconnected();
			return false;
			}
		catch (SocketTimeoutException ex)
			{
			return true;
			}
		catch (IOException ex)
			{
			stop_pinging();
			if (!awaiting_close)
				{
				log (ex.getMessage());
				onError ("read", "read error");
				}
			else
				log ("socket read error while awaiting close, which is expected");
			onDisconnected();
			return false;
			}
		}
	
	private void start_pinging()
		{
		/*
		if (ping_timer == null)
			{
			ping_timer = new Timer();
			ping_timer.scheduleAtFixedRate (new PingTask(), 10000, 10000);
			}
		*/
		}
	
	private void stop_pinging()
		{		
		}
	
	private void force_close()
		{
		if (fd != null)
			{
			try
				{
				fd.close();
				}
			catch (Exception e)
				{
				}
			fd = null;
			}
		}
	
	public void register()
		{
		log ("register");
		if (password != null)
			post ("PASS " + password);
		post ("USER " + nick + " 0 * :" + nick);
		post ("NICK " + nick);
		}
	
	public void set_nickname (String nickname)
		{		
		}
	
	public void post (String text)
		{
		if (fd == null || fd.isClosed())
			{
			onError ("send", "connection closed");
			return;
			}
		else if (fd.isConnected() == false)
			{
			onError ("send", "connection disconnected");
			return;
			}
		try
			{
			fd.setSoTimeout (3000);
			log ("** post: " + text);
			byte[] b = (text + '\n' + '\r').getBytes ("UTF-8");
			out.write (b);
			}
		catch (IOException ex)
			{
			log (ex.getMessage());
			onError ("send", "write error");
			}
		}
	
	public void say (String to, String message)
		{
		post ("PRIVMSG " + to + " :" + message);
		}
	
	public boolean is_connected()
		{
		return (fd != null && fd.isClosed() == false && fd.isConnected() == true);
		}	
	
	abstract public void onLog (String text);
	
	abstract public void onError (String what, String details);
	
	abstract public void onConnected();
	
	abstract public void onDisconnected();
	
	abstract public void onMessage (String from, String message);
	}