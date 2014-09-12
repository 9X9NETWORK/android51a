package tv.tv9x9.player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

public class AppsLayer extends StandardFragment
	{
	metadata config = null;
	
    public interface OnAppsListener
		{
    	public boolean is_tablet();
    	public boolean is_phone();
    	public void redraw_menu();
    	public void enable_home_layer();
        public void launch_player (String channel_id, String channels[]);
        public void launch_player (String channel_id, String episode_id, String channels[]);
    	public Handler get_main_thread();
    	public int actual_pixels (int dp);
    	public void track_event (String category, String action, String label, long value);
    	public void refresh_menu_adapter();
    	public void redraw_menu_if_created();
		}    
    
    OnAppsListener mCallback; 
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    	{
    	log ("onCreateView");
        return inflater.inflate (R.layout.apps_layer, container, false);
    	}  
    
    @Override
    public void onAttach (Activity activity)
    	{
        super.onAttach (activity);
        
        try
        	{
            mCallback = (OnAppsListener) activity;
        	} 
        catch (ClassCastException e)
        	{
            throw new ClassCastException (activity.toString() + " must implement OnAppsListener");
        	}
    	}
    
    @Override
    public void onStart()
    	{
    	super.onStart();
    	identity = "apps";
    	log ("onStart");
    	}
    
    @Override
    public void onResume()
    	{
    	super.onResume();
    	log ("onResume");
    	}
    
	public void setup_apps_buttons()
		{
		AbsListView vAppsList = (AbsListView) getView().findViewById (mCallback.is_phone() ? R.id.apps_list_phone : R.id.apps_list_tablet);				
		vAppsList.setOnItemClickListener (new OnItemClickListener()
			{
			public void onItemClick (AdapterView parent, View v, int position, long id)
				{
				log ("onItemClick: " + position);
				if (position < apps.length)
					{
					log ("app list click: " + position);
					launch_suggested_app (apps [position].title, apps [position].market_url);
					}
				}
			});
		
		View vAppsLayer = getView().findViewById (R.id.appslayer);
		if (vAppsLayer != null)
			vAppsLayer.setOnClickListener (new OnClickListener()
				{
		        @Override
		        public void onClick (View v)
		        	{
		        	/* eat these */
		        	log ("apps layer ate my tap!");
		        	}
				});		
		}
	
	public class app
		{
		String title;
		String description;
		String icon_url;
		String market_url;
		String basename;
		}
	
	app apps[] = null;
	app recommended_apps[] = null;
	
	AppsAdapter apps_adapter = null;
	
	public void init_apps (metadata config)
		{
		this.config = config;
		
		if (apps == null)
			{
			new playerAPI (mCallback.get_main_thread(), config, "relatedApps?os=android&sphere=" + config.region)
				{
				public void success (String[] lines)
					{
					int section = 0, count0 = 0, count1 = 0;
					for (int i = 0; i < lines.length; i++)
						{
						if (lines[i].equals ("--"))
							section++;
						else if (section == 0)
							count0++;
						else if (section == 1)
							count1++;
						}
							
					app new_recommended_apps[] = new app [count0];
					app new_apps[] = new app [count1];
	
					section = 0;
					count0 = 0;
					count1 = 0;
					
					Pattern pattern = Pattern.compile ("id=([^&]*)", Pattern.CASE_INSENSITIVE);
					
					for (int i = 0; i < lines.length; i++)
						{
						if (lines[i].equals ("--"))
							section++;
						else if (section == 0 || section == 1)
							{
							app a = new app();
							String fields[] = lines[i].split ("\t");
							a.title = fields.length > 0 ? fields[0] : "";
							a.description = fields.length > 1 ? fields[1] : "";
							a.icon_url = fields.length > 2 ? fields[2] : "";
							a.market_url = fields.length > 3 ? fields[3] : "";
							/* market://details?id=tv.ddtv.player9x9tv&hl=en */
							Matcher m = pattern.matcher (a.market_url);
							if (m.find())
								a.basename = m.group (1);
							if (section == 0)
								new_recommended_apps [count0++] = a;
							if (section == 1)
								new_apps [count1++] = a;
							log ("app: " + a.market_url);
							}
						}
					
					recommended_apps = new_recommended_apps;
					apps = new_apps;
		
					mCallback.redraw_menu_if_created();
					
					View vPhone = getView().findViewById (R.id.apps_list_phone);
					vPhone.setVisibility (mCallback.is_phone() ? View.VISIBLE : View.GONE);
					
					View vTablet = getView().findViewById (R.id.apps_list_tablet);
					vTablet.setVisibility (mCallback.is_phone() ? View.GONE : View.VISIBLE);
					
					AbsListView vAppsList = (AbsListView) getView().findViewById (mCallback.is_phone() ? R.id.apps_list_phone : R.id.apps_list_tablet);
					apps_adapter = new AppsAdapter (getActivity(), apps);
					vAppsList.setAdapter (apps_adapter);
					
					setup_apps_buttons();
					download_app_thumbs();
					}
				public void failure (int code, String errtext)
					{
					}
				};
			}
		else
			{
			if (apps_adapter != null)
				apps_adapter.notifyDataSetChanged();
			}
		}
	
	public app[] get_recommended_apps()
		{
		return recommended_apps;
		}
	
	public app[] get_known_apps()
		{
		return apps;
		}
	
	public void download_app_thumbs()
		{			
		Runnable apps_thumberino = new Runnable()
			{
			public void run()
				{
				if (apps_adapter != null)
					apps_adapter.notifyDataSetChanged();
				mCallback.refresh_menu_adapter();
				}
			};
	
		Stack <String> filenames = new Stack <String> ();
		Stack <String> urls = new Stack <String> ();
	
		for (app a: apps)
			{
			filenames.push (a.basename);
			urls.push (a.icon_url);
			}
		
		thumbnail.download_list_of_images (getActivity(), config, "apps", filenames, urls, true, mCallback.get_main_thread(), apps_thumberino);
		}
	
	public void launch_suggested_app (String name, String url)
		{
		if (url != null && !url.equals (""))
			{
			Intent intent = new Intent (Intent.ACTION_VIEW, Uri.parse (url));
			startActivity (intent);
			mCallback.track_event ("install", "toDownload-others", name, 0);
			}
		}
	
	public class AppsAdapter extends BaseAdapter
		{
		private Context context;
		private app apps[] = null;
		
		public AppsAdapter (Context context, app apps[])
			{
			this.context = context;
			this.apps = apps;
			}
	
		@Override
		public int getCount()
			{			
			log ("getcount: " + apps.length);
			return apps == null ? 0 : apps.length;
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
					
			log ("apps getView: " + position + " (of " + getCount() + ")");
			
			if (convertView == null)
				rv = (LinearLayout) View.inflate (getActivity(), R.layout.app_item, null);				
			else
				rv = (LinearLayout) convertView;
						
			if (rv == null)
				{
				log ("getView: [position " + position + "] rv is null!");
				return null;
				}
			
			TextView vTitle = (TextView) rv.findViewById (R.id.title);
			if (vTitle != null)
				vTitle.setText (apps [position].title);
			
			TextView vDesc = (TextView) rv.findViewById (R.id.desc);
			if (vDesc != null)
				{
				vDesc.setText (apps [position].description);
				vDesc.setMaxLines (mCallback.is_phone() ? 2 : 3);
				if (mCallback.is_tablet())
					vDesc.setLines (3);
				}
	
			boolean icon_found = false;
			
			ImageView vIcon = (ImageView) rv.findViewById (R.id.icon); 	
			if (vIcon != null)
				{
				String filename = getActivity().getFilesDir() + "/" + config.api_server + "/apps/" + apps [position].basename + ".png";
				
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
				
				if (mCallback.is_phone())
					{
					LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) vIcon.getLayoutParams();
					layout.height = layout.width = mCallback.actual_pixels (40 + 4);
					vIcon.setLayoutParams (layout);
					}
				}
			
			String txt_download = getResources().getString (R.string.download);
			String txt_coming_soon = getResources().getString (R.string.coming_soon);
			
			TextView vDownload = (TextView) rv.findViewById (R.id.download_button);
			if (vDownload != null)
				{
				if (apps [position].market_url == null || apps [position].market_url.equals(""))
					vDownload.setText (txt_coming_soon);
				else
					vDownload.setText (txt_download);
				}
			
			return rv;
			}	
		}
	}