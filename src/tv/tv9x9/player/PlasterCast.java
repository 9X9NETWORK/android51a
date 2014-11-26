package tv.tv9x9.player;

/* This is its own class instead of using WrapChromecast and WrapMatchstick alone because I
   intend to have logic in here to select one or the other based on which receiver is selected */

import android.content.Context;

import android.os.Bundle;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import java.io.IOException;

public class PlasterCast
	{
	boolean uses_matchstick = false;
	boolean uses_chromecast = true;
	
	WrapMatchstick matchstick = null;
	WrapChromecast chromecast = null;
	
	PlasterListener listener = null;
	
	public interface PlasterListener
		{
        public void onApplicationDisconnected (int errorCode);
        public void onVolumeChanged();
		public String getNamespace();
    	public void onConnected (Bundle connectionHint);
    	public void onApplicationCouldNotLaunch (int statusCode);
		public void onMessageReceived (String namespace, String message);
    	public void onConnectionSuspended (int cause);
		public void onConnectionFailed();
		public void onTeardown();
		}
	
	/* WrapChromecast and WrapMatchstick adhere to this interface */
	public interface PlasterInterface
		{
		public String categoryForCast (String app_name);
		public void selectDevice (RouteInfo info);
		public void stopApplication();
		public void createClient (Context ctx, PlasterCast.PlasterListener listener);
		public void sendMessage (String message);
		public void teardown();
		}
	
	PlasterInterface plaster = null;
	
	PlasterCast (PlasterListener listener)
		{
		this.listener = listener;
		if (uses_matchstick)
			{
			plaster = new WrapMatchstick();
			}
		else if (uses_chromecast)
			plaster = new WrapChromecast();
		}
	
	public String categoryForCast (String app_name)
		{
		if (plaster != null)
			return plaster.categoryForCast (app_name);
		else
			return null;
		}
	
	public void selectDevice (MediaRouter router, RouteInfo info)
		{
		if (plaster != null)
			plaster.selectDevice (info);
		}
	
	public void stopApplication()
		{
		if (plaster != null)
			plaster.stopApplication();
		}
	
	public void teardown()
		{
		if (plaster != null)
			plaster.teardown();
		listener.onTeardown();
		}
	
	public void createClient (Context ctx, PlasterListener listener)
		{
		if (plaster != null)
			plaster.createClient (ctx, listener);
		}
	
	public void sendMessage (String message)
		{
		if (plaster != null)
			plaster.sendMessage (message);
		}
	}