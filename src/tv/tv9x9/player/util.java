package tv.tv9x9.player;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;

import android.content.Context;
import android.util.Log;

public class util
	{
	/* borrowed following two functions */
	
	static final String EX = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    public static String encodeURIComponent (String input)
    	{
    	try
    		{
            int l = input.length();
            StringBuilder o = new StringBuilder(l * 3);
            for(int i=0; i < l; i++) {
                String e = input.substring(i,i+1);
                if(EX.indexOf(e)==-1) {
                    byte[] b = e.getBytes("utf-8");
                    o.append(getHex(b));
                    continue;
                }
                o.append(e);
            }
            return o.toString();
    		}
    	catch (Exception ex)
    		{
			ex.printStackTrace();
    		}
    	return null;
        }
     
    public static String getHex(byte buf[])
        {
            StringBuilder o = new StringBuilder(buf.length * 3);
            for(int i=0; i< buf.length; i++)
            {
                int n = (int) buf[i] & 0xff;
                o.append("%");
                if(n < 0x10)
                    o.append("0");
                o.append(Long.toString(n, 16).toUpperCase());
            }
            return o.toString();
        }
    
    public static String decodeURIComponent (String input)
	    {
		try
			{
			// Log.i ("vtest", "DECODE!!!! " + input + " -> " + URLDecoder.decode  (input.replace ("+", "%2B"), "UTF-8").replace ("%2B", "+"));
			// return URLDecoder.decode (input.replace ("+", "%2B"), "UTF-8").replace ("%2B", "+");
			String decoded = URLDecoder.decode (input);
			// decoded = decoded.replaceAll ("\\+", " "); /* <---- TEMPORARY! */
			return decoded;	
			}
		catch (Exception e)
			{
			return "";
			}
	    }
    
   	public static String seconds_to_string (int seconds)
		{
		if (seconds < 60)
			return (String.format ("0:%02d", seconds));
		else if (seconds < 3600)
			return (String.format ("%02d:%02d", (seconds / 60), seconds % 60));
		else
			{
			int hours = seconds / 3600;
			int minutes = (seconds - (hours * 3600)) / 60;
			return String.format ("%d:%02d:%02d", hours, minutes, seconds % 60);
			}
		}

   	public static int string_to_seconds (String time)
		{
   		String fields[] = time.split (":");
   		if (fields.length == 1)
   			return Integer.parseInt (fields [0]);
   		else if (fields.length == 2)
   			return 60 * Integer.parseInt (fields [0]) + Integer.parseInt (fields [1]);
   		else if (fields.length == 3)
   			return 3600 * Integer.parseInt (fields [0]) + 60 * Integer.parseInt (fields [1]) + Integer.parseInt (fields [2]);
   		else
   			return 0;
		}
   	
   	public static String ageof (long seconds)
   		{
   		long now = System.currentTimeMillis() / 1000;
   		
   		if (seconds > 0)
   			{
   			long minutes = (now - seconds) / 60;
   			
   			String ago_or_hence = minutes < 0 ? " hence" : " ago";
   			minutes = Math.abs (minutes);

   			String age = null;
   			
   			if (minutes > 59)
   				{
   		        int hours = (int) Math.floor ((minutes + 1) / 60);
   		        if (hours >= 24)
   		          {
   		          int days = (int) Math.floor ((hours + 1) / 24);
   		          if (days > 30)
   		          	{
   		        	int months = (int) Math.floor ((days + 1) / 30);
   		            if (months > 12)
   		            	{
   		            	int years = (int) Math.floor ((months + 1) / 12);
   		            	age = years + (years == 1 ? " year" : " years");
   		            	}
   		            else
   		                age = months + (months == 1 ? " month" : " months");
   		          	}
   		          else
   		        	  age = days + (days == 1 ? " day" : " days");
   		          }
   		        else
   		        	age = hours + (hours == 1 ? " hour" : " hours");
   				}
   			else
   				age = minutes + (minutes == 1 ? " minute" : " minutes");
   			
   			return age + ago_or_hence;
   			}
   		else
   			return "long ago";
   		}
   	
   	public static String md5 (String text)
   		{
		byte[] message = null;
		try
			{
			message = text.getBytes ("UTF-8");
			}
		catch (UnsupportedEncodingException e)
			{
			e.printStackTrace();
			return null;
			}

		MessageDigest md = null;
		try
			{
			md = MessageDigest.getInstance ("MD5");
			}
		catch (Exception ex)
			{
			ex.printStackTrace();
			}
		
		byte[] digest = md.digest (message);
		
		BigInteger bigint = new BigInteger (1,digest);
		String hash = bigint.toString (16);

		while (hash.length() < 32 )
		  hash = "0" + hash;
		  
		// Log.i ("vtest", "hash (" + text + ") -> " + hash);
   		return hash;
   		}
   	
	
	public static boolean is_a_tv (Context ctx)
		{
		if (ctx.getPackageManager().hasSystemFeature ("com.google.android.tv"))
			{
		    Log.i ("vtest", "Google TV detected");
		    return true;
			}
		else if (android.os.Build.MODEL.contains ("AK-MINI-PC"))
			{
			Log.i ("vtest", "AK-MINI-PC detected");
			return true;
			}
		else if (android.os.Build.MODEL.contains ("MK80"))
			{
			Log.i ("vtest", "MK80x detected");
			return true;
			}
		else if (android.os.Build.MODEL.contains ("rk30"))
			{
			Log.i ("vtest", "MK80x (rk30) detected");
			return true;
			}
		else if (android.os.Build.MODEL.contains ("IP_Cable"))
			{
			Log.i ("vtest", "IP_Cable Box detected");
			return true;
			}
		else if (android.os.Build.MODEL.contains ("CloudAlive"))
			{
			Log.i ("vtest", "CloudAlive detected");
			return true;
			}		
		return false;
		// return true;
		}
	}