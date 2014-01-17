package tv.tv9x9.player;

import com.google.cast.MessageStream;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * An abstract class which encapsulates control and game logic for sending and receiving messages 
 * during a TicTacToe game.
 */
public abstract class CastMessageStream extends MessageStream
	{
    private static final String TAG = CastMessageStream.class.getSimpleName();

    private static final String GAME_NAMESPACE = "tv.9x9.cast";

    // Receivable event types
    private static final String KEY_EVENT = "event";
    private static final String KEY_JOINED = "joined";
    private static final String KEY_MOVED = "moved";
    private static final String KEY_ERROR = "error";

    // Commands
    private static final String KEY_COMMAND = "command";
    private static final String KEY_JOIN = "join";

    private static final String KEY_COLUMN = "column";
    private static final String KEY_GAME_OVER = "game_over";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_NAME = "name";
    private static final String KEY_OPPONENT = "opponent";
    private static final String KEY_PLAYER = "player";
    private static final String KEY_ROW = "row";

     /**
     * Constructs a new GameMessageStream with GAME_NAMESPACE as the namespace used by 
     * the superclass.
     */
    protected CastMessageStream()
    	{
        super (GAME_NAMESPACE);
    	}

    /**
     * Performs some action upon a player joining the game.
     * 
     * @param playerSymbol either X or O
     * @param opponentName the name of the player who just joined an existing game, or the opponent
     */
    protected abstract void onGameJoined(String playerSymbol, String opponentName);

    /**
     * Performs some action upon a game error.
     * 
     * @param errorMessage the string description of the error
     */
    protected abstract void onGameError(String errorMessage);

    /**
     * Attempts to connect to an existing session of the game by sending a join command.
     * 
     * @param name the name of the player that is joining
     */
    public final void join(String name) {
        try {
            Log.d(TAG, "join: " + name);
            JSONObject payload = new JSONObject();
            payload.put(KEY_COMMAND, KEY_JOIN);
            payload.put(KEY_NAME, name);
            sendMessage(payload);
        } catch (JSONException e) {
            Log.e(TAG, "Cannot create object to join a game", e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to send a join message", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Message Stream is not attached", e);
        }
    }


    public void play_video_id (String video_id)
    	{
        JSONObject payload = new JSONObject();
        try
        	{
        	payload.put ("command", "play");
			payload.put ("video", video_id);
	        sendMessage (payload);
        	}
        catch (Exception e)
        	{
			e.printStackTrace();
        	}
    	}
    
    public void send_simple_json (String command)
    	{
    	JSONObject payload = new JSONObject();
    	try { payload.put ("type", command); } catch (Exception ex) { ex.printStackTrace(); }
    	try { sendMessage (payload); } catch (Exception ex) { ex.printStackTrace(); }
    	}
    
    public void onChromecastPausePlay (boolean is_playing)
    	{    	
    	}
    
    public void onChromecastPosition (String channel, String episode, int position, int duration)
    	{    	
    	}
    
    public void onChromecastEpisodeChanged (String channel, String episode)
    	{    	
    	}
    
    public void pause()
    	{    	
    	send_simple_json ("pause");
    	}

    public void resume()
		{    	
		send_simple_json ("resume");
		}
    
    public void stop()
		{    	
		send_simple_json ("stop");
		}
    
    public void play (metadata config, String channel_id, String episode_id, String arena[])
		{
    	JSONObject json = assemble_play_command_json (config, channel_id, episode_id, arena);
    	try { sendMessage (json); } catch (Exception ex) { ex.printStackTrace(); }
		}
	    
    static JSONObject assemble_play_command_json (metadata config, String channel_id, String episode_id, String arena[])
	    {
  	    JSONObject payload = new JSONObject();
	    JSONObject data = new JSONObject();
	    JSONArray channel_arena = new JSONArray();
	    JSONObject mso = new JSONObject();
	    try
	    	{
	        if (arena != null)
	        	{
    	        for (String arena_channel_id: arena)
    	        	{
    	        	if (arena_channel_id != null && !channel_id.equals (""))
	    	        	{
	    	        	String name = config.pool_meta (arena_channel_id, "name");
	    	        	String desc = config.pool_meta (arena_channel_id, "desc");
	    	        	String thumb = config.pool_meta (arena_channel_id, "thumb");
	    	        	String nature = config.pool_meta (arena_channel_id, "nature");
	    	        	String extra = config.pool_meta (arena_channel_id, "extra");
	    	        	
	    	        	JSONObject channel_structure = new JSONObject();
	    	        	
	    	        	channel_structure.put ("youtubeId", extra);
	    	        	channel_structure.put ("contentType", nature);
	    	        	int program_count = config.programs_in_real_channel (arena_channel_id);
	    	        	if (program_count > 0)
	    	        		channel_structure.put ("programCount", program_count);		    	        	
	    	        	channel_structure.put ("thumb", thumb);		    	        	
	    	        	channel_structure.put ("description", desc);
	    	        	channel_structure.put ("name", name);
	    	        	channel_structure.put ("id", arena_channel_id);
	    	        	
	    	        	channel_arena.put (channel_structure);
	    	        	}
    	        	}
    	        data.put ("channelArena", channel_arena);
	        	}
	        else
	        	Log.i ("vtest", "json: no channel arena");
	        
	        mso.put ("name", config.mso);
	        mso.put ("title", config.mso_title != null ? config.mso_title : config.app_name);
	        mso.put ("supported-region", config.supported_region);
	        mso.put ("region",  config.region);
	        mso.put ("preferredLangCode",  config.mso_preferred_lang_code);
	        	        
			data.put ("channelId", channel_id);
			data.put ("episodeId", episode_id);
			data.put ("mso", mso);
	    	payload.put ("data", data);
	    	payload.put ("type", "play");
	        Log.i ("vtest", "JSON: " + payload.toString());    	        
	    	}
	    catch (Exception ex)
	    	{
	    	ex.printStackTrace();
	    	}
	    
	    return payload;
		}    
    
    /**
     * Processes all JSON messages received from the receiver device and performs the appropriate 
     * action for the message. Recognizable messages are of the form:
     * 
     * <ul>
     * <li> KEY_JOINED: a player joined the current game
     * <li> KEY_MOVED: a player made a move
     * <li> KEY_ENDGAME: the game has ended in one of the END_STATE_* states
     * <li> KEY_ERROR: a game error has occurred
     * <li> KEY_BOARD_LAYOUT_RESPONSE: the board has been laid out in some new configuration
     * </ul>
     * 
     * <p>No other messages are recognized.
     */
    
    // onMessageReceived:
    //  {"data":{"position":491.65500000000003,"state":"playing","duration":74000,"episode":"9j52mq_dl_A","channel":"30070"},"event":"progress"}
    
    // onMessageReceived: 
    //  {"data":{"position":74164.534,"state":"ended","duration":74164.534,"episode":"9j52mq_dl_A","channel":"30070"},"event":"progress"}

    String current_episode = null;
    String current_channel = null;
    
    @Override
    public void onMessageReceived (JSONObject message) {
        try {
            Log.d(TAG, "onMessageReceived: " + message);
            if (message.has(KEY_EVENT))
            	{
                String event = message.getString(KEY_EVENT);
                if (event.equals("progress"))
                	{
                	JSONObject data = message.getJSONObject ("data");
                	String state = data.getString ("state");
                	if (state.equals ("playing"))
                		onChromecastPausePlay (true);
                	else if (state.equals ("paused"))
                		onChromecastPausePlay (false);
                	String position = data.getString ("position");
                	position = position.replaceAll ("\\.\\d+$", "");
                	String duration = data.getString ("duration");
                	duration = duration.replaceAll ("\\.\\d+$", "");                	
                	String episode = data.getString ("episode");
                	String channel = data.getString ("channel");
                	onChromecastPosition (channel, episode, Integer.parseInt (position), Integer.parseInt (duration));
                	
                	if (current_episode == null || !current_episode.equals (episode))
                		{
                		current_episode = episode;
                		onChromecastEpisodeChanged (channel, episode);
                		}
                	}
                }
        	}
        catch (Exception ex)
        	{
        	ex.printStackTrace();
        	}
    }

}
