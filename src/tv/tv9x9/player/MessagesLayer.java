package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MessagesLayer extends StandardFragment
	{
	boolean messages_initialized = false;
	
	AbsListView messages_view = null;
	MessagesAdapter messages_adapter = null;
	
	int messages_count = 0;
	
	metadata config = null;
	
    public interface OnMessagesListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
        public void launch_player (String channel_id, String channels[]);
        public void launch_player (String channel_id, String episode_id, String channels[]);
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
		}    
    
    OnMessagesListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.message_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnMessagesListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnMessagesListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "messages";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
   	
	public void messages_init (metadata config)
		{
		this.config = config;
		
		if (!messages_initialized)
			{
			messages_initialized = true;
			messages_view = (AbsListView) getView().findViewById (mCallback.is_tablet() ? R.id.message_list_tablet : R.id.message_list_phone);
			messages_adapter = new MessagesAdapter (getActivity(), new message[] {});
			messages_view.setAdapter (messages_adapter);
					
			if (mCallback.is_phone())
				{
				/* ListView supports footer but GridView does not */
	    		LayoutInflater inflater = getActivity().getLayoutInflater();
	    		View shim = inflater.inflate (R.layout.footer_shim, null);
	    		((ListView) messages_view).addFooterView (shim);
				}
			}
		}

	public void setup_messages_buttons()
		{
		View vLayer = getView().findViewById (R.id.messagelayer);		
		if (vLayer != null)
			vLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat this */
		        	log ("message layer ate my tap!");
		        	}
				});				
		}
	
	public class message
		{
		String mso;
		String title;
		String description;
		String channel;
		String episode;
		String timestamp;
		String logo;
		}

	public message parse_notification_file (String ts)
		{
		message m = new message();
		File configfile = new File (getActivity().getFilesDir(), "notifications/" + ts);
		try
			{
		    FileReader reader = new FileReader (configfile);
		    BufferedReader br = new BufferedReader (reader);
		    String line = br.readLine();    
		    while (line != null)
		    	{
		    	while (line != null)
		    		{
		    		String fields[] = line.split ("\t");
		    		if (fields.length >= 2)
		    			{
		    			log ("notification " + ts + " k: " + fields[0] + " v: " + fields[1]);
						if (fields[0].equals ("title"))
							m.title = fields[1];
						if (fields[0].equals ("mso"))
							m.mso = fields[1];    		
						if (fields[0].equals ("channel"))
							m.channel = fields[1];      
						if (fields[0].equals ("episode"))
							m.episode = fields[1];    
						if (fields[0].equals ("text"))
							m.description = fields[1];
						if (fields[0].equals ("logo"))
							m.logo = fields[1];
		    			}
		    		line = br.readLine();    	    		
		    		}
		    	}
		    reader.close();
		    m.timestamp = ts.replaceAll ("\\.seen", "");
		    if (m.title == null)
		    	m.title = getResources().getString (R.string.app_name);
			}
		catch (IOException ex)
			{
		    ex.printStackTrace();
		    return null;
			}
		return m;
		}	
	
	public Stack <message> messages_gather (boolean unseen_only)
		{
		File dir = new File (getActivity().getFilesDir(), "notifications");
		
		File[] files = dir.listFiles();
	
		/* to allow filtering out duplicates */
		HashSet <String> message_de_duper = new HashSet <String> ();
		
		if (files != null)
			{
			/* sort in the reverse of what we need, because of Stack */
			Arrays.sort (files);
			}
		
		final Stack <message> stack = new Stack <message> ();
		
		if (files != null)
	        for (File f: files)
	        	{
	        	log ("file: " + f);
	        	String parts[] = f.toString().split ("/");
	        	/* last component of the filepath is mostly a timestamp */
	        	String ts = parts [parts.length-1];
	        	if (ts.endsWith (".seen"))
	        		{
	        		if (unseen_only)
	        			continue;
	        		}
	        	else
	        		{
	        		if (!unseen_only)
	        			{
		        		f.renameTo (new File (f.toString() + ".seen"));
		        		ts = ts + ".seen";
	        			}
	        		}
	        	message m = parse_notification_file (ts);
	        	if (m != null && m.title != null)
	        		{
	        		String ident = m.title + "\t" + m.description + "\t" + m.channel + "\t" + m.episode;
	        		if (!message_de_duper.contains (ident))
	        			{
	        			stack.push (m);
	        			message_de_duper.add (ident);
	        			}
	        		else
	        			log ("a very similar notification already exists -- filtered out");
	        		}
	        	}
	
		return stack;
		}

	public void messages_display_content()
		{
		final Stack <message> stack = messages_gather (false);
		
		final Stack <String> channels = new Stack <String> ();
		final Stack <String> unknown_channels = new Stack <String> ();
	   
		/* for the UI badge */
		messages_count = 0;
		mCallback.redraw_menu();
		
	    int count = 0;
	    message messages[] = new message [stack.size()];
	    while (!stack.empty())
	    	{
	    	message m = stack.pop();
			if (m.channel != null)
				{
				channels.push (m.channel);
				String id = config.pool_meta (m.channel, "id");
				if (id == null || id.equals (""))
					unknown_channels.push (m.channel);
				}
	    	messages [count++] = m;
	    	}
	    
		log ("[notifications] total messages: " + stack.size());
		log ("[notifications] total channels: " + channels.size());
		log ("[notifications] total unknown channels: " + unknown_channels.size());
		
		View vNo = getView().findViewById (R.id.no_new_messages);
		vNo.setVisibility (messages == null || messages.length == 0 ? View.VISIBLE : View.GONE);
		messages_view.setVisibility (messages == null || messages.length == 0 ? View.GONE : View.VISIBLE);
	
		if (messages_adapter != null)
			{
			messages_adapter.set_content (messages);			
			messages_view.setOnItemClickListener (new OnItemClickListener()
				{
				public void onItemClick (AdapterView parent, View v, int position, long id)
					{
					message m = messages_adapter.get_item (position);
					if (m.channel != null)
						{
			            String fake_set[] = new String[] { m.channel };
			            if (m.episode != null)
			            	mCallback.launch_player (m.channel, m.episode, fake_set);
			            else
			            	mCallback.launch_player (m.channel, fake_set);
						}
					else
						mCallback.enable_home_layer();
					}
				});
			}
	
		final Runnable download_thumbs = new Runnable()
			{
			public void run()
				{
				if (channels.size() > 0)
					{
			        int count = 0;
			        String channel_list[] = new String [channels.size()];
			        while (!channels.empty())
			        	channel_list [count++] = channels.pop();
			    	
			        final Runnable update_notify_thumbs = new Runnable()
						{
						public void run()
							{
							messages_adapter.notifyDataSetChanged();
							}
						};
					
			        thumbnail.stack_thumbs (getActivity(), config, channel_list, -1, mCallback.get_main_thread(), update_notify_thumbs);
					}
				}
			};
		
		if (messages.length > 0)
			{
			if (unknown_channels.size() > 0)
				{
		        int uncount = 0;
		        String ids[] = new String [unknown_channels.size()];
		        while (!unknown_channels.empty())
		        	ids [uncount++] = unknown_channels.pop();
		        
		        String channel_ids = TextUtils.join (",", ids); 
		        
				new playerAPI (mCallback.get_main_thread(), config, "channelLineup?channel=" + channel_ids)
					{
					public void success (String[] chlines)
						{
						log ("load channel for notifications, lines received: " + chlines.length);
						config.parse_channel_info (chlines);
						download_thumbs.run();
						}
			
					public void failure (int code, String errtext)
						{
						log ("ERROR! " + errtext);
						}
					};
				}
			else
				download_thumbs.run();
			}
		}
	
	public class MessagesAdapter extends BaseAdapter
		{
		private Context context;
		private message messages[] = null;
		
		public MessagesAdapter (Context context, message messages[])
			{
			this.context = context;
			this.messages = messages;
			}
	
		public void set_content (message messages[])
			{
			this.messages = messages;
			notifyDataSetChanged();
			}
		
		public message get_item (int position)
			{
			return messages [position];
			}
			
		@Override
		public int getCount()
			{			
			// log ("getcount: " + messages.length);
			return messages == null ? 0 : messages.length;
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
					
			log ("messages getView: " + position + " (of " + getCount() + ")");
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (getActivity(), R.layout.message_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (messages [position].title);
			
			TextView vDesc = (TextView) rv.findViewById (R.id.desc);
			if (vDesc != null)
				vDesc.setText (messages [position].description);
	
			TextView vAgo = (TextView) rv.findViewById (R.id.message_ago);
			if (vAgo != null)
				{
				String ago = util.ageof (getActivity(), Long.parseLong (messages [position].timestamp) / 1000);
				vAgo.setText (ago);
				}
			
			boolean icon_found = false;
			String channel_id = messages [position].channel;
			
			ImageView vIcon = (ImageView) rv.findViewById (R.id.icon); 	
			if (vIcon != null)
				{
				if (channel_id != null)
					{
					log ("[notify] channel: " + channel_id);
					
					String filename = getActivity().getFilesDir() + "/" + config.api_server + "/cthumbs/" + channel_id + ".png";
						
					File f = new File (filename);
					if (f.exists())
						{
						Bitmap bitmap = BitmapFactory.decodeFile (filename);
						if (bitmap != null)
							{
							icon_found = true;
							vIcon.setImageBitmap (bitmap);
							}
						}
					else
						log ("channel icon for " + channel_id + " not found");
					}
				else
					{
					icon_found = true;
					vIcon.setImageResource (R.drawable.home_channel);
					}
				}
			
			/* default should not be home_channel though */
			if (!icon_found)
				vIcon.setImageResource (R.drawable.home_channel);		
			
			if (mCallback.is_tablet())
				{
				View vMessageBlock = rv.findViewById (R.id.message_block);
				if (vMessageBlock != null)
					{
					LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vMessageBlock.getLayoutParams(); 
					layout.rightMargin = mCallback.actual_pixels (4);
					layout.leftMargin = mCallback.actual_pixels (4);
					vMessageBlock.setLayoutParams (layout);
					}
				}
			return rv;
			}	
		}	
	}