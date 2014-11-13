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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.state.*;
import com.ircclouds.irc.api.Callback;
import com.ircclouds.irc.api.domain.messages.*;
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.domain.messages.interfaces.*;

/*
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
*/

import javax.net.ssl.SSLSocketFactory;

public class ChatLayer extends StandardFragment
	{
	metadata config = null;
	
    public class chatmessage
    	{	
    	String username;
    	String message;
    	}
       
    Social soc = null;
    
    boolean soc_shim_added = false;
	
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
    
    public void start_social (final metadata config)
	    {
	    this.config = config;
	    
	    log ("start chat");

	    /*
        Configuration configuration = new Configuration.Builder()
	        .setName ("PircBotXUser")
	        .setServer (config.chat_server, config.chat_port)
	        .setSocketFactory (SSLSocketFactory.getDefault())
	        .addAutoJoinChannel ("#" + config.mso)
	        .addListener (new MyListener())
	        .buildConfiguration();
        
        PircBotX bot = new PircBotX (configuration);
        
        try
        	{
            bot.startBot();
            log ("irc connected");
        	}
        catch (Exception ex)
        	{
        	ex.printStackTrace();
        	}
        */
	    
	    Thread t = new Thread()
	    	{
	    	@Override
	    	public void run()
	    		{
	    		open_irc();
	    		}
	    	};
	    	
	    t.start();
		}
    
    public void open_irc()
    	{
        final IRCApi _api = new IRCApiImpl(true);
        _api.connect (getServerParams ("nick", Arrays.asList ("altNick1", "altNick2"), "IRC Api", "ident", config.chat_server, true), new Callback <IIRCState>()
        		{
                @Override
                public void onSuccess(final IIRCState aIRCState)
                	{
                    log ("connected");
    	    		_api.addListener (message_listener);
                	}

                @Override
                public void onFailure(Exception aErrorMessage)
                	{
                    log ("failure: " + aErrorMessage);
                	}
        		});
    	}
    
    public void close()
	    {
	    }
    
    IMessageListener message_listener = new IMessageListener()
    	{
    	public void onMessage (IMessage aMessage)
    		{
    		log ("message: " + aMessage);
    		}
    	};
    
    // public IRCServer(String aHostname, int aPort, String aPassword, Boolean aIsSSL)
    
    private IServerParameters getServerParams
    	(final String aNickname, final List <String> aAlternativeNicks, final String aRealname,
    						final String aIdent, final String aServerName, final Boolean aIsSSLServer)
    	{
    	return new IServerParameters()
		    {
            @Override
            public IRCServer getServer()
            	{
                // return new IRCServer (aServerName, aIsSSLServer);            	
            	return new IRCServer (config.chat_server, config.chat_port + 1, config.chat_server_pw, false);
            	}

            @Override
            public String getRealname()
            	{
                return aRealname;
            	}

            @Override
            public String getNickname()
            	{
                return aNickname;
            	}

            @Override
            public String getIdent()
            	{
                return aIdent;
            	}

            @Override
            public List <String> getAlternativeNicknames()
            	{
                return aAlternativeNicks;
            	}
		    };
    	}
		    /*
    public class MyListener extends ListenerAdapter
    	{
        @Override
        public void onGenericMessage (GenericMessageEvent event)
        	{
            if (event.getMessage().startsWith ("?helloworld"))
                    event.respond ("Hello world!");
        	}
        }
    */
    
    /*
    public class ChannelJoinListener extends VariousMessageListenerAdapter
    	{
            @Override
            public void onChannelJoin(ChanJoinMessage aMsg)
            	{
                log ("User " + aMsg.getSource().getNick() + " joined channel" + aMsg.getChannelName()); 
            	}
            
            @Override
            public void onChannelMessage (ChannelPrivMsg aMsg)
            	{
            	log ("ChannelPrivMsg #" + aMsg.asRaw());
            	}
    	}
    */
    
    
	}