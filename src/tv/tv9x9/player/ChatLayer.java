package tv.tv9x9.player;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.tv9x9.player.SocialLayer.SocialAdapter;
import tv.tv9x9.player.SocialLayer.social;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import javax.net.ssl.SSLSocketFactory;

import android.view.WindowManager;

public class ChatLayer extends StandardFragment
	{
	metadata config = null;
	
    public class chatmessage
    	{	
    	String username;
    	String message;
    	long timestamp;
    	chatmessage (String username, String message, long timestamp)
    		{
    		this.username = username;
    		this.message = message;
    		if (timestamp == 0)
    			this.timestamp = System.currentTimeMillis() / 1000L;    		
    		else
        		this.timestamp = timestamp;
    		}
    	}

    List <chatmessage> chatlog = new ArrayList <chatmessage> ();
	
	Hashtable <String, String> user_id_mappings = new Hashtable <String, String> ();
	
    ChatAdapter chat_adapter = null;
    
    Thread chat_thread = null;
    
    Timer update_timer = null;
    UpdateTask update_task = null;
    
    public interface OnChatListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
        public void launch_player (String channel_id, String channels[]);
        public void launch_player (String channel_id, String episode_id, String channels[]);
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public int screen_width();
    	public void remember_location();
    	public void enable_signin_layer (Runnable callback);
		}    
    
    OnChatListener mCallback; 

    IrcRobot irc = null;
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.chat_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnChatListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnChatListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "chat";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
    
    public void start_chat (final metadata config)
	    {
	    this.config = config;
	  
	    log ("start chat");
	    
	    getActivity().getWindow().addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
	    chat_thread = new Thread()
	    	{
	    	@Override
	    	public void run()
	    		{
	    	    close();
	    		open_irc();
	    		}
	    	};
	    	
	    chat_thread.start();
	    
		chat_adapter = new ChatAdapter (getActivity(), chatlog);
		
		ListView vChat = (ListView) getView().findViewById (R.id.chat_list);
		
		vChat.setAdapter (chat_adapter);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View shim = inflater.inflate (R.layout.footer_shim_d9, null);
		vChat.addFooterView (shim);
		
		final EditText vSay = (EditText) getView().findViewById (R.id.say);
	    vSay.setOnKeyListener (new OnKeyListener()
		    {
			@Override
			public boolean onKey (View v, int keyCode, KeyEvent event)
				{
				if (event.getAction() == KeyEvent.ACTION_UP)
					{					
					if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
						{
						String message = vSay.getText().toString();
						vSay.clearFocus();						
						InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow (vSay.getWindowToken(), 0);
						if (irc != null)
							{
							if (config.userid == null || config.usertoken == null)								
								{
								final EditText vSay = (EditText) getView().findViewById (R.id.say);
								final String what_im_sayin = vSay.getText().toString();
								mCallback.enable_signin_layer (new Runnable()
									{
									@Override
									public void run()
										{
										if (irc.is_connected())
											{
											/* on tablet, signin is an overlay. We might possibly keep the connection */
											change_nick();
											}
										else
											start_chat (config);										
										vSay.setText (what_im_sayin);
										}
									});
								}
							else
								irc.say ("#" + config.mso, message);
							}	

						return true;
						}
					}
				return false;
				}						
		    });
	    
	    View vLayer = getView();
		vLayer.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{		        	
	        	/* eat this */
	        	}
			});

		update_saybox();
		
		if (update_timer != null)
			{
			update_timer.cancel();
			update_timer = null;
			}
		
		update_timer = new Timer();
		update_timer.scheduleAtFixedRate (new UpdateTask(), 10000, 10000);
		}

    public void update_saybox()
    	{		
		boolean not_logged_in = config.usertoken == null || config.userid == null;
		
		View vSayBox = getView().findViewById (R.id.saybox);
		vSayBox.setVisibility (not_logged_in ? View.GONE : View.VISIBLE);
		
		View vLoginBox = getView().findViewById (R.id.loginbox);
		vLoginBox.setVisibility (not_logged_in ? View.VISIBLE : View.GONE);	
		
		vLoginBox.setOnClickListener (new OnClickListener()
			{
	        @Override
	        public void onClick (View v)
	        	{		    
	        	if (config.usertoken != null && config.userid != null)
	        		{
	        		/* out of sync! should never happen. change_nick will resync nick and update saybox */
	        		change_nick();
	        		return;
	        		}
	        		
				mCallback.enable_signin_layer (new Runnable()
					{
					@Override
					public void run()
						{
						if (irc.is_connected())
							{
							/* on tablet, signin is an overlay. We might possibly keep the connection */
							change_nick();
							}
						else
							start_chat (config);										
						}
					});
	        	}
			});
    	}
    
    public void open_irc()
    	{
    	irc = new IrcRobot()
    		{
    		@Override
    		public void onLog (String text)
    			{	
    			ChatLayer.this.log (text);
    			}
    		
    		@Override
    		public void onConnected()
    			{    
    			log ("irc: connected");
    			register();
    			post ("JOIN #" + config.mso);
    			String botname = "FLIPrBot";
    			// botname = "soylent";    					
				irc.say (botname, "!getLastMsgs #" + config.mso);
    			}
    		
    		@Override
    		public void onDisconnected()
    			{    
    			log ("irc: disconnected");
    			}
    		
    		@Override
    		public void onError (String what, String details)
    			{    		
    			log ("irc: error " + what + ": " + details);
    			}

    		@Override
    		public void onMessage (String from, String message)
    			{
    			log ("irc: from(" + from + ") " + message);    			
    			if (from.contains ("!"))
    				{
    				String fields[] = from.split ("!");
    				from = fields [0];
    				from = from.replaceAll (":", "");
    				}
    			
    			message = message.replaceAll ("^:", "");
    
    			if (from.contains ("FLIPrBot"))
    				add_bot_message (message);
    			else
	    			add_chat_message (from, message, 0);
    			}
    		
    		@Override
    		public void say (String to, String message)
    			{
    			super.say (to, message);
    			if (to.startsWith ("#"))
    				add_chat_message (generate_nickname(), message, 0);
    			else
    				{
    				// add_chat_message (generate_nickname(), "-> *" + to + "* " + message, 0);
    				}
    			clear_new_message();
    			}
    		};
    	
    	irc.open (config.chat_server, 6667, "52e49a7fc64b774a", generate_nickname());
    	}
    
    public void change_nick()
    	{
    	if (irc != null)
    		irc.post ("NICK " + generate_nickname());
    	
    	mCallback.get_main_thread().post (new Runnable()
			{
			@Override
			public void run()
				{
		    	update_saybox();	
				}
			});
    	}
    
    public String generate_nickname()
    	{
    	if (config.userid == null || config.usertoken == null)
	    	{
			long now = System.currentTimeMillis();
			return "a_" + (now % 1000000);
	    	}
    	else
    		return "u_" + config.userid;
    	}
    
    public void add_chat_message (final String from, final String message, long timestamp)
    	{
    	if (timestamp == 0)
    		{
    		/* default to now */
    		timestamp = System.currentTimeMillis();
    		}
    	
		chatmessage cm = new chatmessage (from, message, timestamp);
		chatlog.add (cm);
		
		mCallback.get_main_thread().post (new Runnable()
			{
			@Override
			public void run()
				{
				if (chat_adapter != null)
					{
					chat_adapter.notifyDataSetChanged();
					ListView vChat = (ListView) getView().findViewById (R.id.chat_list);
					vChat.setSelection (chat_adapter.getCount() - 1);
					}
				}
			});
		
		if (user_id_mappings.get (from) == null)
			{
			String id = from;
			if (id.startsWith ("u_"))
				{
				id = id.replaceAll ("^u_", "");
			
	    		new playerAPI (mCallback.get_main_thread(), config, "getUserNames?id=" + id)
	    			{
					public void success (String[] lines)
						{
						if (lines.length > 0)
							{
							String fields[] = lines[0].split ("\t");
							if (fields.length > 1)
								{
								user_id_mappings.put (from, fields[1]);
								chat_adapter.notifyDataSetChanged();
								}
							}
						}
					public void failure (int code, String errtext)
						{
						}
					};
				}
			}
    	}
    
    public void add_bot_message (String text)
    	{
    	if (!text.startsWith ("{"))
    		{
    		log ("not a JSON message: " + text);
    		return;
    		}
    	
    	JSONObject json;		
		try {
			json = new JSONObject (text);
			}
		catch (JSONException e)
			{
			e.printStackTrace();
			return;
			}
		
		String j_nick = null;
		try
			{
			j_nick = json.getString ("nick");
			}
		catch (Exception ex)
			{
			log ("JSON: no \"nick\"");
			return;
			}
		
		String j_text = null;
		try
			{
			j_text = json.getString ("text");
			}
		catch (Exception ex)
			{
			log ("JSON: no \"text\"");
			return;
			}		

		String j_timestamp = null;
		try
			{
			j_timestamp = json.getString ("timestamp");
			}
		catch (Exception ex)
			{
			log ("JSON: no \"timestamp\"");
			return;
			}
		
		log ("ts: " + j_timestamp);
		long ts = Long.parseLong (j_timestamp) * 1000;
		
		add_chat_message (j_nick, j_text, ts);
    	}
    
    public void clear_new_message()
    	{
    	if (Looper.myLooper() != Looper.getMainLooper())
    		{
    		mCallback.get_main_thread().post (new Runnable()
    			{
    			@Override
    			public void run()
    				{
    				clear_new_message_inner();
    				}
    			});
    		}
    	else
    		clear_new_message_inner();
    	}
    
    public void clear_new_message_inner()
    	{
		final EditText vSay = (EditText) getView().findViewById (R.id.say);
		vSay.setText ("");
		vSay.clearFocus();						
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService (Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow (vSay.getWindowToken(), 0);
    	}
    
    public void close()
	    {
		if (irc != null)
			{
			irc.close();
			irc = null;
			}
		
		if (update_timer != null)
			{
			update_timer.cancel();
			update_timer = null;
			}
		
		mCallback.get_main_thread().post (new Runnable()
			{
			@Override
			public void run()
				{
			    getActivity().getWindow().clearFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				}
			});
	    }
    
	public class ChatAdapter extends BaseAdapter
		{
		List <chatmessage> chatlog = null;
		
		public ChatAdapter (Context context, List <chatmessage> chatlog)
			{
			this.chatlog = chatlog;
			}
	
		@Override
		public int getCount()
			{			
			log ("getcount: " + chatlog.size());
			return chatlog == null ? 0 : chatlog.size();
			}
		
		@Override
		public Object getItem (int position)
			{
			return position;
			}
	
		@Override
		public long getItemId (int position)
			{
			return position;
			}
		
		@Override
		public View getView (final int position, View convertView, ViewGroup parent)
			{
			LinearLayout rv = null;
					
			log ("social getView: " + position + " (of " + getCount() + ")");
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (getActivity(), R.layout.chat_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			if (position > chatlog.size())
				{
				log ("getView: position is " + position + " but only have " + chatlog.size() + " items!");
				return null;
				}
			
			chatmessage chat = chatlog.get (position);
			
			String mapped_from = user_id_mappings.get (chat.username);
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (mapped_from != null ? mapped_from : chat.username);

			TextView vText = (TextView) rv.findViewById (R.id.text);
			if (vText != null)
				vText.setText (chat.message);
			
			TextView vCounter = (TextView) rv.findViewById (R.id.soc_counter);
			if (vCounter != null)
				vCounter.setText ("#" + Integer.toString (chatlog.size() - position));
			
			String ago = util.ageof (getActivity(), chat.timestamp / 1000);
			
			TextView vSource = (TextView) rv.findViewById (R.id.message_source);
			if (vSource != null)
				vSource.setText (ago);
			
			/*
			TextView vAgo = (TextView) rv.findViewById (R.id.message_ago);
			if (vAgo != null)
				{
				String ago = "";
				//if (soc.date.length() == "1409103232".length())
				//	ago = util.ageof (getActivity(), Long.parseLong (soc.date));
				vAgo.setText (ago);
				}
			*/
			
			return rv;
			}	   
		}
	
	class UpdateTask extends TimerTask
		{  
		public void run()
	       	{
			mCallback.get_main_thread().post (new Runnable()
				{
				@Override
				public void run()
					{
					if (chat_adapter != null)
						chat_adapter.notifyDataSetChanged();
					}
				});
	       	}	       	
		}
	}