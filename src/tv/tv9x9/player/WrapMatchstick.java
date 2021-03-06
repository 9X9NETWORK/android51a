package tv.tv9x9.player;

import android.os.Bundle;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import java.io.IOException;

import android.content.Context;

import tv.matchstick.flint.ApplicationMetadata;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.Flint;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.FlintMediaControlIntent;
import tv.matchstick.flint.FlintStatusCodes;
import tv.matchstick.flint.MediaInfo;
import tv.matchstick.flint.MediaMetadata;
import tv.matchstick.flint.MediaStatus;
import tv.matchstick.flint.RemoteMediaPlayer;
import tv.matchstick.flint.ResultCallback;
import tv.matchstick.flint.Status;

import tv.matchstick.flint.Flint.ApplicationConnectionResult;
import tv.matchstick.flint.RemoteMediaPlayer.MediaChannelResult;
import tv.matchstick.flint.images.WebImage;

class WrapMatchstick implements PlasterCast.PlasterInterface
	{
	private PlasterCast.PlasterListener plasterListener = null;
	
    private FlintDevice gcast_selected_device = null;
    private Flint.Listener gcast_listener;
    private FlintManager gcast_api_client;
    private FlintManager.ConnectionCallbacks gcast_connection_callbacks;
    
    private WrapChannel gcast_channel = null;
        
    private boolean gcast_application_started = false;
    private boolean gcast_waiting_for_reconnect = false;
    
    private String app_name = null;
    
    // private String RECV_URL = "http://9x9ui.s3.amazonaws.com/recv1.html";
    // private String TILDE_NAME = "~noop";
    
    private String RECV_URL = "http://dev6.9x9.tv/castm/";
    private String TILDE_NAME = "~castm";
    
	WrapMatchstick()
		{
		Flint.FlintApi.setApplicationId (TILDE_NAME);
		}			
	
	public void log (String text)
		{
		Log.i ("vtest", "[matchstick] " + text);
		}
	
	public String categoryForCast (String app_name)
		{
		// this.app_name = app_name;
		this.app_name = RECV_URL;
		String cat = FlintMediaControlIntent.categoryForFlint (TILDE_NAME);
		log ("CCX categoryForCast: " + cat);
		return cat;
		}
	
	public void selectDevice (RouteInfo info)
		{
		if (info != null)
			gcast_selected_device = FlintDevice.getFromBundle (info.getExtras());
		else
			gcast_selected_device = null;
		}
	
	public void stopApplication()
		{
		Flint.FlintApi.stopApplication (gcast_api_client);
		}
	
	public void removeMessageCallbacks()
		{
		try
			{
			Flint.FlintApi.removeMessageReceivedCallbacks (gcast_api_client, gcast_channel.getNamespace());
			}
		catch (IOException ex)
			{
			ex.printStackTrace();
			}
		}
	
	private class WrapConnectionCallbacks implements FlintManager.ConnectionCallbacks
		{
		@Override
    	public void onConnected (final Bundle connectionHint)
			{
    		if (gcast_api_client == null)
    			{
    			log ("we were disconnected, gcast_api_client is null");
    			return;
    			}
    		
    		if (!gcast_api_client.isConnected())
				{
    			log ("gcast_api_client says it is not connected");
    			return;
				}
    		
	        if (gcast_waiting_for_reconnect)
	        	{
	        	gcast_waiting_for_reconnect = false;
	
	            if ((connectionHint != null) && connectionHint.getBoolean (Flint.EXTRA_APP_NO_LONGER_RUNNING))
	            	{
	            	log ("receiver app is no longer running");
	                teardown();
	            	}
	            else
	            	{
	            	log ("reconnect: re-creating the custom message channel");
	                setMessageReceivedCallbacks();
	            	}
	            return;
	        	}
    		
    		try
				{
		        /* launch receiver app */
		        Flint.FlintApi.launchApplication (gcast_api_client, app_name, false).setResultCallback
		        	(
		            new ResultCallback <Flint.ApplicationConnectionResult> ()
		            	{	                    		
                        @Override
                        public void onResult (ApplicationConnectionResult result)
                        	{
                            Status status = result.getStatus();
                            log ("ApplicationConnectionResultCallback.onResult: statusCode " + status.getStatusCode());
                            if (status.isSuccess())
                            	{
                                ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                                // String sessionId = result.getSessionId();
                                String sessionId = "sessionId";
                                // String applicationName = applicationMetadata.getName();
                                String applicationName = "name";
                                String applicationStatus = result.getApplicationStatus();
                                boolean wasLaunched = result.getWasLaunched();
                                log ("CCX application name: " + applicationName + ", status: "
                                                + applicationStatus + ", sessionId: " + sessionId + ", wasLaunched: " + wasLaunched);
                                gcast_application_started = true;

                                if (!setMessageReceivedCallbacks())
                                	{
                        			/* don't call onConnected */
                                	return;
                                	}
               
                    			if (plasterListener != null)
                    				plasterListener.onConnected (connectionHint);
                            	}
                            else
                            	{
                                log ("CCX application could not launch");
                    			if (plasterListener != null)
                    				plasterListener.onApplicationCouldNotLaunch (status.getStatusCode());
                                teardown();
                            	}
                        	}
		            	}
		        	);
				} 
		    catch (Exception ex)
		    	{
		        log ("CCX failed to launch application");
		        ex.printStackTrace();
		    	}
			}
		
		@Override
		public void onConnectionSuspended (int cause)
			{
    	    gcast_waiting_for_reconnect = true;
			if (plasterListener != null)
				plasterListener.onConnectionSuspended (cause);
			}
		
		@Override
        public void onConnectionFailed (ConnectionResult result)
	    	{
	    	if (plasterListener != null)
	    		plasterListener.onConnectionFailed();
	    	}
		}

    class WrapChannel implements Flint.MessageReceivedCallback
    	{
        public String getNamespace()
        	{
        	if (plasterListener != null)
        		return plasterListener.getNamespace();
        	return null;
        	}

        @Override
        public void onMessageReceived (FlintDevice castDevice, String namespace, String message)
        	{
        	if (plasterListener != null)
        		plasterListener.onMessageReceived (namespace, message);
        	}
    	}
    
	public void createClient (Context ctx, PlasterCast.PlasterListener listener)
		{
		plasterListener = listener;

		if (gcast_connection_callbacks == null)
			gcast_connection_callbacks = new WrapConnectionCallbacks();
		
        gcast_listener = new Flint.Listener()
    		{
            @Override
            public void onApplicationDisconnected (int errorCode)
            	{
            	plasterListener.onApplicationDisconnected (errorCode);
            	}
            
            @Override
            public void onVolumeChanged()
            	{
            	plasterListener.onVolumeChanged();
            	}
        	};
        	
		Flint.FlintOptions.Builder api_options_builder = Flint.FlintOptions.builder (gcast_selected_device, gcast_listener);
		
        gcast_api_client = new FlintManager
        		.Builder (ctx)
        		.addApi (Flint.API, api_options_builder.build())
                .addConnectionCallbacks (gcast_connection_callbacks)
                .build();
        
        gcast_api_client.connect();
		}
	
	public boolean setMessageReceivedCallbacks()
		{
		gcast_channel = new WrapChannel();
		try
			{
			Flint.FlintApi.setMessageReceivedCallbacks (gcast_api_client, gcast_channel.getNamespace(), gcast_channel);
			return true;
			}
		catch (IOException ex)
			{
			ex.printStackTrace();
			return false;
			}
		}
	
	public void sendMessage (String message)
		{
	    if (gcast_api_client != null && gcast_channel != null)
	    	{
	        try
	        	{
	            Flint.FlintApi.sendMessage (gcast_api_client, gcast_channel.getNamespace(), message)
	                    .setResultCallback (new ResultCallback <Status> ()
	                    		{
	                        	@Override
	                        	public void onResult(Status result)
	                        		{
	                        		if (!result.isSuccess())
	                        			/* failed! */ ; 
	                        		}
	                    		});
	        	} 
	        catch (Exception ex)
	        	{
	            ex.printStackTrace();
	        	}
			}
		}
	
	public void teardown()
		{
	    if (gcast_api_client != null)
	    	{
	    	if (gcast_application_started)
           		{
	    		try
               		{
	    			stopApplication();
	    			if (gcast_channel != null)
                   		{
	    				removeMessageCallbacks();
	    				gcast_channel = null;
                   		}
               		}
	    		catch (Exception ex)
               		{
	    			ex.printStackTrace();
               		}
	    		gcast_application_started = false;
           		}
           
           try
	            {
	            if (gcast_api_client.isConnected())
	                gcast_api_client.disconnect();
	            }
           catch (Exception ex)
	            {
	            ex.printStackTrace();
	            }
           
           gcast_api_client = null;            
	       }       
       
        gcast_selected_device = null;
        gcast_waiting_for_reconnect = false;
		}
	}