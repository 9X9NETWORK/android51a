package tv.tv9x9.player;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
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

public class ChatLayer extends StandardFragment
	{
	metadata config = null;
	
    public class chatmessage
    	{	
    	String username;
    	String message;
    	chatmessage (String username, String message)
    		{
    		this.username = username;
    		this.message = message;
    		}
    	}

    List <chatmessage> chatlog = new ArrayList <chatmessage> ();
	
	Hashtable <String, String> user_id_mappings = new Hashtable <String, String> ();
	
    ChatAdapter chat_adapter = null;
    
    Thread chat_thread = null;
    
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
							irc.say ("#" + config.mso, message);
						return true;
						}
					}
				return false;
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
    
    			add_chat_message (from, message);
    			}
    		
    		@Override
    		public void say (String to, String message)
    			{
    			super.say (to, message);
    			add_chat_message ("u_" + config.userid, message);
    			clear_new_message();
    			}
    		};
    		
    	irc.open (config.chat_server, 6667, "52e49a7fc64b774a", "u_" + config.userid);
    	}
    
    public void add_chat_message (final String from, final String message)
    	{
		chatmessage cm = new chatmessage (from, message);
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
				id = id.replace ("u_", "");
			
    		new playerAPI (mCallback.get_main_thread(), config, "getUserNames?id=" + from)
    			{
				public void success (String[] lines)
					{
					String fields[] = lines[0].split ("\t");
					user_id_mappings.put (from, fields[1]);
					chat_adapter.notifyDataSetChanged();
					}
				public void failure (int code, String errtext)
					{
					}
				};
			}
    	}
    
    public void clear_new_message()
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
			
			TextView vSource = (TextView) rv.findViewById (R.id.message_source);
			if (vSource != null)
				vSource.setText (" on IRC");
			
			TextView vAgo = (TextView) rv.findViewById (R.id.message_ago);
			if (vAgo != null)
				{
				String ago = "";
				//if (soc.date.length() == "1409103232".length())
				//	ago = util.ageof (getActivity(), Long.parseLong (soc.date));
				vAgo.setText (ago);
				}

			return rv;
			}	   
		}
	}