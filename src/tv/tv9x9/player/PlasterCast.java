package tv.tv9x9.player;

import android.content.Context;

import android.os.Bundle;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.MediaRouteSelector;
import android.util.Log;

public class PlasterCast
	{
	boolean uses_matchstick = true;
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
			matchstick = new WrapMatchstick();
		if (uses_chromecast)
			chromecast = new WrapChromecast();		
		}
	
	public void log (String text)
		{
		Log.i ("vtest", "[plasterCast] " + text);
		}
	
	public MediaRouteSelector makeSelector (String app_name)
		{
		MediaRouteSelector.Builder selectorBuilder = new MediaRouteSelector.Builder();
		addCategoriesForCast (selectorBuilder, app_name);
		return selectorBuilder.build();
		}
	
	public void addCategoriesForCast (MediaRouteSelector.Builder selectorBuilder, String app_name)
		{
		if (selectorBuilder != null)
			{
			if (uses_matchstick && matchstick != null)
				{
				log ("adding category for MatchStick");
				selectorBuilder.addControlCategory (matchstick.categoryForCast (app_name));
				}
			if (uses_chromecast && chromecast != null)
				{
				if (app_name != null)
					{
					log ("adding category for Chromecast");
					selectorBuilder.addControlCategory (chromecast.categoryForCast (app_name));
					}
				else
					log ("won't add category for Chromecast, since app_name is null");
				}
			}
		else
			log ("** selectorBuilder is null!");
		}
	
	public void selectDevice (MediaRouter router, RouteInfo info)
		{
		if (info != null)
			{
			String devinfo = info.getDescription();
			if (devinfo != null && devinfo.contains ("MatchStick"))
				plaster = matchstick;
			else
				plaster = chromecast;
			if (plaster != null)
				plaster.selectDevice (info);
			}
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
		plaster = null;
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