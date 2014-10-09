package tv.tv9x9.player;

import android.content.Context;

import android.os.Bundle;
import android.util.Log;
import android.support.v7.media.MediaRouter.RouteInfo;

import java.io.IOException;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.GooglePlayServicesClient;

public class WrapChromecast implements PlasterCast.PlasterInterface
	{
	private PlasterCast.PlasterListener plasterListener = null;
	
	private CastDevice gcast_selected_device = null;
    private Cast.Listener gcast_listener = null;
    private GoogleApiClient gcast_api_client = null;
    private GoogleApiClient.ConnectionCallbacks gcast_connection_callbacks;

    private WrapChannel gcast_channel = null;
    private WrapConnectionFailedListener gcast_connection_failed_listener;
    
    private boolean gcast_application_started = false;
    private boolean gcast_waiting_for_reconnect = false;
    
    private String app_name = null;
    
	WrapChromecast()
		{
		log ("WrapChromecast start");
		}	
	
	public void log (String text)
		{
		Log.i ("vtest", "[chromecast] " + text);
		}
	
	public String categoryForCast (String app_name)
		{
		this.app_name = app_name;
		log ("categoryForCast: " + app_name);
		return CastMediaControlIntent.categoryForCast (app_name);
		}
	
	public void selectDevice (RouteInfo info)
		{
		if (info != null)
			gcast_selected_device = CastDevice.getFromBundle (info.getExtras());
		else
			gcast_selected_device = null;
		}	
	
	public void stopApplication()
		{
		log ("stopApplication");
		Cast.CastApi.stopApplication (gcast_api_client);
		}
	
	public void removeMessageCallbacks()
		{
		try
			{
			Cast.CastApi.removeMessageReceivedCallbacks (gcast_api_client, gcast_channel.getNamespace());
			}
		catch (IOException ex)
			{
			ex.printStackTrace();
			}
		}
	
	private class WrapConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks
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
	
	            if ((connectionHint != null) && connectionHint.getBoolean (Cast.EXTRA_APP_NO_LONGER_RUNNING))
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
    			Cast.CastApi.launchApplication (gcast_api_client, app_name, false).setResultCallback
    				(
    				new ResultCallback<Cast.ApplicationConnectionResult>()
    					{		                    		
    					@Override
    					public void onResult (ApplicationConnectionResult result)
    						{
		                    Status status = result.getStatus();
		                    log ("ApplicationConnectionResultCallback.onResult: statusCode " + status.getStatusCode());
		                    if (status.isSuccess())
		                    	{
		                        ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
		                        String sessionId = result.getSessionId();
		                        String applicationStatus = result.getApplicationStatus();
		                        boolean wasLaunched = result.getWasLaunched();
                                log ("CCX application name: " + applicationMetadata.getName() + ", status: "
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
		}
	
	private class WrapConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener
		{
        @Override
        public void onConnectionFailed (ConnectionResult result)
        	{
        	if (plasterListener != null)
        		plasterListener.onConnectionFailed();
        	}
		}

    class WrapChannel implements Cast.MessageReceivedCallback
    	{
        public String getNamespace()
	    	{
	    	if (plasterListener != null)
	    		return plasterListener.getNamespace();
	    	return null;
	    	}

        @Override
        public void onMessageReceived (CastDevice castDevice, String namespace, String message)
        	{
        	if (plasterListener != null)
        		plasterListener.onMessageReceived (namespace, message);
        	}
    	}
    
	public void createClient (Context ctx, PlasterCast.PlasterListener listener)
		{
		plasterListener = listener;
		
		if (gcast_connection_failed_listener == null)
			gcast_connection_failed_listener = new WrapConnectionFailedListener();
		
		if (gcast_connection_callbacks == null)
			gcast_connection_callbacks = new WrapConnectionCallbacks();
		
    	gcast_listener = new Cast.Listener()
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
        	
		Cast.CastOptions.Builder api_options_builder = Cast.CastOptions.builder (gcast_selected_device, gcast_listener);
		
        gcast_api_client = new GoogleApiClient
        		.Builder (ctx)
                .addApi (Cast.API, api_options_builder.build())
                .addConnectionCallbacks (gcast_connection_callbacks)
                .addOnConnectionFailedListener (gcast_connection_failed_listener)
                .build();
        
        gcast_api_client.connect();
		}
	
	public boolean setMessageReceivedCallbacks()
		{
		gcast_channel = new WrapChannel();
		try
			{
			Cast.CastApi.setMessageReceivedCallbacks (gcast_api_client, gcast_channel.getNamespace(), gcast_channel);
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
	            Cast.CastApi.sendMessage (gcast_api_client, gcast_channel.getNamespace(), message)
	                    .setResultCallback (new ResultCallback<Status>()
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