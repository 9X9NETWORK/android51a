package tv.tv9x9.player;

import android.util.Log;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public final class Streamy
	{
	TwitterStream twitterStream = null;
	
	public void log (String text)
		{
		Log.i ("vtest", "[Streamy] " + text);
		}	    
       
    public void t0 (final Callback cb)
		{
		Thread t = new Thread()
			{
			@Override
			public void run()
				{
				try {
					t1 (cb);
				} catch (TwitterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			};
		
		log ("t0: starting thread");
		t.start();
		}
    
    public void t1 (final Callback callback) throws TwitterException
    	{
    	log ("t1: inside thread");
    	ConfigurationBuilder cb = new ConfigurationBuilder();
    	 
    	
    	/*
    	 * API key	BT0HMhIBSUCGFKXU4EpkROe91
		 * API secret	TDzNVtLQ5bTEUdd0SJFnFLsdvmZZeCvQ9eaSL2vTiBZeEE3iGN
		 * Access token	2328058117-QrirJNYIff0UC0ddzcbg7vPyj3FIgIrP2D4uV6y
		 * Access token secret	eavQN5f38s4nqdW7hC1sAEbYDQB9sQ0A3f0WmI5EWF1Jh
    	 */
    	
        cb.setDebugEnabled (true)
              .setOAuthConsumerKey("BT0HMhIBSUCGFKXU4EpkROe91")
             .setOAuthConsumerSecret("TDzNVtLQ5bTEUdd0SJFnFLsdvmZZeCvQ9eaSL2vTiBZeEE3iGN")
               .setOAuthAccessToken("2328058117-QrirJNYIff0UC0ddzcbg7vPyj3FIgIrP2D4uV6y")
             .setOAuthAccessTokenSecret("eavQN5f38s4nqdW7hC1sAEbYDQB9sQ0A3f0WmI5EWF1Jh");
        
        	// TwitterFactory factory = new TwitterFactory (cb.build());
            // Twitter twitter = factory.getInstance();
            
            /*
            String message="\"A Visit to Transylvania\" by Euromaxx: Lifestyle Europe (DW) \n http://bit.ly/1cHB7MH";
            Status status = twitter.updateStatus(message);
            log ("Successfully updated status to " + status.getText());
            */
            
                twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
                StatusListener listener = new StatusListener() {
                    @Override
                    public void onStatus(Status status) {
                        log ("@" + status.getUser().getScreenName() + " - " + status.getText());
                        if (callback != null)
                        	callback.run_two_strings (status.getUser().getScreenName(), status.getText());
                    }

                    @Override
                    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                        log ("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
                    }

                    @Override
                    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                        log ("Got track limitation notice:" + numberOfLimitedStatuses);
                    }

                    @Override
                    public void onScrubGeo(long userId, long upToStatusId) {
                        log ("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
                    }

                    @Override
                    public void onStallWarning(StallWarning warning) {
                        log ("Got stall warning:" + warning);
                    }

                    @Override
                    public void onException(Exception ex) {
                        ex.printStackTrace();
                    }
                };
                twitterStream.addListener(listener);
                twitterStream.sample();
                
                
                UserStreamListener ulistener = new UserStreamListener() {
                    @Override
                    public void onStatus(Status status) {
                        System.out.println("onStatus @" + status.getUser().getScreenName() + " - " + status.getText());
                        if (callback != null)
                        	callback.run_two_strings (status.getUser().getScreenName(), status.getText());
                    }

                    @Override
                    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                        System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
                    }

                    @Override
                    public void onDeletionNotice(long directMessageId, long userId) {
                        System.out.println("Got a direct message deletion notice id:" + directMessageId);
                    }

                    @Override
                    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                        System.out.println("Got a track limitation notice:" + numberOfLimitedStatuses);
                    }

                    @Override
                    public void onScrubGeo(long userId, long upToStatusId) {
                        System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
                    }

                    @Override
                    public void onStallWarning(StallWarning warning) {
                        System.out.println("Got stall warning:" + warning);
                    }

                    @Override
                    public void onFriendList(long[] friendIds) {
                        System.out.print("onFriendList");
                        for (long friendId : friendIds) {
                            System.out.print(" " + friendId);
                        }
                        System.out.println();
                    }

                    @Override
                    public void onFavorite(User source, User target, Status favoritedStatus) {
                        System.out.println("onFavorite source:@"
                                + source.getScreenName() + " target:@"
                                + target.getScreenName() + " @"
                                + favoritedStatus.getUser().getScreenName() + " - "
                                + favoritedStatus.getText());
                    }

                    @Override
                    public void onUnfavorite(User source, User target, Status unfavoritedStatus) {
                        System.out.println("onUnFavorite source:@"
                                + source.getScreenName() + " target:@"
                                + target.getScreenName() + " @"
                                + unfavoritedStatus.getUser().getScreenName()
                                + " - " + unfavoritedStatus.getText());
                    }

                    @Override
                    public void onFollow(User source, User followedUser) {
                        System.out.println("onFollow source:@"
                                + source.getScreenName() + " target:@"
                                + followedUser.getScreenName());
                    }

                    @Override
                    public void onUnfollow(User source, User followedUser) {
                        System.out.println("onFollow source:@"
                                + source.getScreenName() + " target:@"
                                + followedUser.getScreenName());
                    }

                    @Override
                    public void onDirectMessage(DirectMessage directMessage) {
                        System.out.println("onDirectMessage text:"
                                + directMessage.getText());
                    }

                    @Override
                    public void onUserListMemberAddition(User addedMember, User listOwner, UserList list) {
                        System.out.println("onUserListMemberAddition added member:@"
                                + addedMember.getScreenName()
                                + " listOwner:@" + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list) {
                        System.out.println("onUserListMemberDeleted deleted member:@"
                                + deletedMember.getScreenName()
                                + " listOwner:@" + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserListSubscription(User subscriber, User listOwner, UserList list) {
                        System.out.println("onUserListSubscribed subscriber:@"
                                + subscriber.getScreenName()
                                + " listOwner:@" + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserListUnsubscription(User subscriber, User listOwner, UserList list) {
                        System.out.println("onUserListUnsubscribed subscriber:@"
                                + subscriber.getScreenName()
                                + " listOwner:@" + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserListCreation(User listOwner, UserList list) {
                        System.out.println("onUserListCreated  listOwner:@"
                                + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserListUpdate(User listOwner, UserList list) {
                        System.out.println("onUserListUpdated  listOwner:@"
                                + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserListDeletion(User listOwner, UserList list) {
                        System.out.println("onUserListDestroyed  listOwner:@"
                                + listOwner.getScreenName()
                                + " list:" + list.getName());
                    }

                    @Override
                    public void onUserProfileUpdate(User updatedUser) {
                        System.out.println("onUserProfileUpdated user:@" + updatedUser.getScreenName());
                    }

                    @Override
                    public void onBlock(User source, User blockedUser) {
                        System.out.println("onBlock source:@" + source.getScreenName()
                                + " target:@" + blockedUser.getScreenName());
                    }

                    @Override
                    public void onUnblock(User source, User unblockedUser) {
                        System.out.println("onUnblock source:@" + source.getScreenName()
                                + " target:@" + unblockedUser.getScreenName());
                    }

                    @Override
                    public void onException(Exception ex) {
                        ex.printStackTrace();
                        System.out.println("onException:" + ex.getMessage());
                    }
                };

                // twitterStream.addListener (ulistener);
                // twitterStream.user();
        }

    public void close()
    	{
    	if (twitterStream != null)
    		twitterStream.cleanUp();
    	}
	}