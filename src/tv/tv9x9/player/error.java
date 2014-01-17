package tv.tv9x9.player;

public class error
	{
	public static String pretty (String error)
		{
		error = error.replaceAll ("^ERROR:", "");
		if (error.startsWith ("350"))
			return "Oops! Something just went wrong trying to subscribe or unsubscribe. Please try again later. (Error 350 -- general)";
		if (error.startsWith ("351"))
			return "Oops! Something just went wrong trying to subscribe or unsubscribe. Please try again later. (Error 351 -- duplicate)";
		if (error.startsWith ("355"))
			return "Oops! Something just went wrong trying to subscribe or unsubscribe. Please try again later. (Error 355 -- occupied)";
		return error;
		}
	}

/*
50 THIS FEATURE IS TEMPORARILY DISABLED (feature is not available, not likely happens in production)
This feature is currently disabled for upgrade.  Please try again later. (Error 50) 
100 INPUT_ERROR (any api parameter has error other than the following)
Oops! Something went wrong. Please restart the app and try again later. (Error 100)
101 INPUT_MISSING (any api parameter is missing)
Oops! Something went wrong. Please restart the app and try again later. (Error 101)
102 INPUT_BAD (any api parameter has invalid format)
Oops! Something went wrong. Please restart the app and try again later. (Error 102)
200 USER_ERROR (any user-related error other than the followings)  
Oops! Something went wrong. Please restart the app and try again later. (Error 200)
201 USER_LOGIN_FAILED (login fail)
The sign-in information you've provided is incorrect.  Please check your email and/or password and try again. (Error 201)
202 USER_EMAIL_TAKEN (signup with a taken email)
That email has already been taken, please use another email address. (Error 202)
203 USER_INVALID (user does not exist)
This user does not exist.  Please sign in with another account. (Error 203)
204 USER_TOKEN_TAKEN (a token has associated with an account)
This account is already in use.  Please try signing in/up with another account. (Error 204)
205 USER_PERMISSION_ERROR (user does not have permission to do certain things)
This action cannot be performed.  Please try again later. (Error 205)
206 ACCOUNT_INVALID (either user or device does not exist)
This account doesn't exist.  Please sign in again. (Error 206)
300 CHANNEL_ERROR (any channel related error other than the followings)
Oops! Something went wrong. Please try another channel. (Error 300)
301 CHANNEL_URL_INVALID (youtube url invalid)
This YouTube channel is invalid.  Please try another channel. (Error 301)
302 CHANNEL_INVALID (channel id does not exist)
This channel does not exist.  Please try another channel. (Error 302)
320 PROGRAM_ERROR (any program/episode error other than the followings)
We encountered an error trying to play this video.  Please pick another video and try again. (Error 320)
321 PROGRAM_INVALID (program id does not exist)
This video doesn't exist.  Please pick another video. (Error 321)
350 SUBSCRIPTION_ERROR (any subscription error other than the following)
Oops! Something just went wrong.  Please try again later. (Error 350)
351 SUBSCRIPTION_DUPLICATE_CHANNEL (channel already subscribed)
This channel has already been followed.  Please pick another channel to follow. (Error 351)
355 SUBSCRIPTION_POS_OCCUPIED (grid position is occupied, won't happen on youtube connect account)
That grid position has already been taken.  Please choose another position. (Error 355)
900 DATABASE_ERROR (any db error other than the following)
Something just went wrong.  Please restart the app and try again. (Error 900)
901 DATABASE_TIMEOUT (query timeout)
Server timeout.  Please try again later. (Error 901)
903 DATABASE_READONLY (database in read only mode)
We're currently conducting routine maintenance.  During this time some functions are disabled temporarily.  Please try again later.  (Error 903)
*/