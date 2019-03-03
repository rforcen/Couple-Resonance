package com.voicesync.coupleresonance;

import twitter.TwitterApp;
import twitter.TwitterApp.TwDialogListener;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class TwitterMgr {
	private TwitterApp mTwitter;
	private static Context context;
	
	public TwitterMgr(Context _context, String TW_consumerkey, String TW_consumerSecret) {	
		context=_context;
		mTwitter 	= new TwitterApp(context, TW_consumerkey, TW_consumerSecret);
		mTwitter.setListener(mTwLoginDialogListener);
	}
	public void postToTwitter(final String review) {
		if (review == null | review.length()==0) return;
		new Thread() {
			@Override public void run() {
				int what = 0;
				
				try {
					mTwitter.updateStatus(review);
				} catch (Exception e) {
					what = 1;
				}
				
				mtwHandler.sendMessage(mtwHandler.obtainMessage(what));
			}
		}.start();
	}
	
	static private Handler mtwHandler = new Handler() {
		@Override public void handleMessage(Message msg) {
			mess ( (msg.what == 0) ? "Posted to Twitter" : "Post to Twitter failed" );
		}
	};
	private final TwDialogListener mTwLoginDialogListener = new TwDialogListener() {
		@Override public void onComplete(String value) {
			String username = mTwitter.getUsername();
			username		= (username.equals("")) ? "No Name" : username;
			mess("Connected to Twitter as " + username);
		}
		@Override public void onError(String value) {
			mess("Twitter connection failed:"+value);
		}
	};
	private static void mess(String s) {
		Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
	}
}
