package com.voicesync.coupleresonance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;
import ogl.GLModel;
import ogl.Merkaba;
import twitter.TwitterApp;
import twitter.TwitterApp.TwDialogListener;
import com.voicesync.graph.DrawGauge;
import com.voicesync.graph.GraphRadial;
import com.voicesync.signal.AsyncListener;
import com.voicesync.signal.Conf;
import com.voicesync.signal.MStimer;
import com.voicesync.signal.Resonancer;
import com.voicesync.signal.Signal;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.*;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.model.*;

public class MainActivity extends Activity {

	static final String HashFB="$1$7ag6i7.N$HUfg0DgbvNmuJepfWq3YZ."; 						// FB integration constants md5pass
	static final String FB_App_ID="538805346155219";
	static final String FB_App_Secret="fec7e3b41deb3bfa64e85a7663bcaa78";
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
//			Arrays.asList("publish_actions, user_photos, publish_checkins, publish_stream");
//	 Arrays.asList("photo_upload, publish_stream, video_upload, share_item, installed, user_photos, status_update, create_note, publish_actions");
	static final String TW_consumerkey= "N4ksyGSuRw1UpodmMRVrTg";	// "21294529-sKMri6KPpsifzMvTYuNqJatspjdpFI6YSc0UbrQ"; //						// Twitter integration
	static final String TW_consumerSecret="ua8kdBXlIKA3pG8GSpT8Q3iI9qPcuKYgnuWU7Vvk"; // "M1uegiGddYSqQAI3hHdbJPqjUWklgUHaRZulX4PYM"; //
	static final String TW_Request_token_URL="https://api.twitter.com/oauth/request_token";
	static final String TW_Authorize_URL="https://api.twitter.com/oauth/authorize";
	static final String TW_Access_token_URL="https://api.twitter.com/oauth/access_token";

	protected String TAG="CR";

	TextView	tvResult, tvTimer, tvFBUserMale, tvFBUserFemale;
	ImageButton ibMale, ibFemale, ibFaceBook, ibTwitter;
	ImageView	ivMale, ivFemale;

	GraphRadial grMale, grFemale;
	DrawGauge	drGauge;
	Merkaba 	merkaba;

	boolean recMale, recFemale, ft;
	Resonancer resonance=new Resonancer();
	double[]fm, ff;
	double res, resEq;
	private int nHit;

	private Handler handler=new Handler();
	MStimer timer=new MStimer();
	static Activity act;
	Conf.sex gender;
	private String strResult="";

	private TwitterMgr twMgr;

	OnLongClickListener lclistener=new OnLongClickListener(){
		@Override public boolean onLongClick(View v) { 					reset();	return true;	}};

		@Override protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			init();
		}
		private void init() {
			act=this;
			tvResult=(TextView)findViewById(R.id.tvResult);
			tvTimer=(TextView)findViewById(R.id.tvTimer);
			tvFBUserMale=(TextView)findViewById(R.id.tvFBUserMale);
			tvFBUserFemale=(TextView)findViewById(R.id.tvFBUserFemale);

			ibMale	=(ImageButton)findViewById(R.id.ibMale);
			ibMale.setOnClickListener(new OnClickListener(){
				@Override public void onClick(View v) { 			switchRecording(Conf.sex.male); 			}});

			ibFemale=(ImageButton)findViewById(R.id.ibFemale);
			ibFemale.setOnClickListener(new OnClickListener(){
				@Override public void onClick(View v) { 			switchRecording(Conf.sex.female);		}});

			ibFaceBook=(ImageButton)findViewById(R.id.ibFaceBook);
			ibFaceBook.setOnClickListener(new OnClickListener(){
				@Override public void onClick(View v) {				onFaceBookClick();				}});

			ibTwitter=(ImageButton)findViewById(R.id.ibTwitter);
			ibTwitter.setOnClickListener(new OnClickListener(){
				@Override public void onClick(View v) {				onTwitterClick();				}});

			ibMale.setOnLongClickListener(lclistener);		ibFemale.setOnLongClickListener(lclistener);

			ivMale=(ImageView)findViewById(R.id.ivMale);
			ivMale.setOnClickListener(new OnClickListener(){
				@Override public void onClick(View v) {				getSubjectPhoto(Conf.sex.male);			}});
			ivFemale=(ImageView)findViewById(R.id.ivFemale);
			ivFemale.setOnClickListener(new OnClickListener(){
				@Override public void onClick(View v) {				getSubjectPhoto(Conf.sex.female);		}});


			recMale=recFemale=false;

			grMale=(GraphRadial)findViewById(R.id.radMale);
			grFemale=(GraphRadial)findViewById(R.id.radFemale);
			drGauge=(DrawGauge)findViewById(R.id.drawGauge1);
			merkaba=new Merkaba((GLModel)findViewById(R.id.gLModel1));

			resonance.setParams(Conf.sampleRate, Conf.nFFT);

			setStrictMode(); 

			twMgr=new TwitterMgr( this, TW_consumerkey, TW_consumerSecret );
		}
		protected void onTwitterClick() {
			twMgr.postToTwitter(strResult);			
		}
		@TargetApi(Build.VERSION_CODES.HONEYCOMB) private void setStrictMode() { 		// strict network mode for newer aOS versions
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				StrictMode.setThreadPolicy(policy); 
			}
		}
		protected void onFaceBookClick() {
			Signal.stopAll(); 

			Session.openActiveSession(this, true, new Session.StatusCallback() { // start Facebook Login
				@Override public void call(Session session, SessionState state, Exception exception) { // callback when session changes state
					if (session.isOpened()) {
						Request.executeMeRequestAsync(session, new Request.GraphUserCallback() { // make request to the /me API
							@Override public void onCompleted(GraphUser user, Response response) { // callback after Graph API response with user object
								if (user != null) {
									updateName(user, response);
									updatePhoto(user, response);
									if (strResult.length()!=0) fbPost();
								}
							}
							private void updateName(GraphUser user, Response response) {
								(isMale(response) ? tvFBUserMale : tvFBUserFemale).setText( user.getName() );
							}
							private void updatePhoto(GraphUser user, Response response) {
								Bitmap bmp=getUserBitmap(user.getId());
								if (bmp!=null) {
									if (isMale(response)) ivMale.setImageBitmap(bmp);
									else ivFemale.setImageBitmap(bmp);
								}
							}
							private boolean isMale(Response response) {
								String fbGender="";
								try {
									fbGender = response.getGraphObject().getInnerJSONObject().getString("gender");
								} catch (JSONException e) {}
								return fbGender.startsWith("m");
							}
						});
					}
				}
			});
		}

		private Bitmap getUserBitmap(String id) {
			Bitmap bmp=null;
			try {
				URL img_value = new URL("http://graph.facebook.com/"+id+"/picture?type=large");
				bmp =  BitmapFactory.decodeStream(img_value.openConnection().getInputStream());
			} catch (MalformedURLException e) { e.printStackTrace(); } catch (IOException e) {
				bmp=null; e.printStackTrace();
			}
			return bmp;			
		}
		private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
			for (String string : subset) 
				if (!superset.contains(string)) return false;
			return true;
		}
		private void fbPost() {
			Session session = Session.getActiveSession();

			if (session != null){
				List<String> permissions = session.getPermissions();// Check for publish permissions    
				if ( ! isSubsetOf(PERMISSIONS, permissions)) {
					Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS);
					session.requestNewPublishPermissions(newPermissionsRequest);
					return;
				}

				Bundle postParams = new Bundle();
				postParams.putString("name", "Couple Resonance");
				postParams.putString("caption", "match couples using voice resonance");
				postParams.putString("description", "results obtained:\n" + strResult);
				//				postParams.putString("link", "http://www.quantum-life.com");  // creates a link post
				postParams.putString("picture", "http://www.quantum-life.com/images/logo.png");
				
				// post a nitmap, requires modif. permissions that don't match with query... endless loop
//				Bitmap bmp=((BitmapDrawable)ivMale.getDrawable()).getBitmap();
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();              
//		        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);              
//		        byte[]data = baos.toByteArray(); 
//				postParams.putByteArray ("picture", data );

				Request.Callback callback= new Request.Callback() {
					public void onCompleted(Response response) {
						JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
						String postId = null;
						try {
							postId = graphResponse.getString("id");
						} catch (JSONException e) {
							Log.i(TAG, "JSON error "+ e.getMessage());
						}
						FacebookRequestError error = response.getError();
						if (error != null) {
							mess(error.getErrorMessage());
						} else {
							mess("posted message to fb wall");
						}
					}
				};
				Request request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);
				RequestAsyncTask task = new RequestAsyncTask(request);
				task.execute();
			}
		}
		protected void reset() {
			recMale=recFemale=false;
			drGauge.setBallPos(0);
			tvResult.setText("");
			Signal.reset();
			grMale.refresh();
			grFemale.refresh();		
			strResult="";
		}
		protected void switchRecording(Conf.sex sx) {
			if (Signal.rec.isRecording)
				stopRecording(sx);
			else
				startRecording(sx);
		}
		private void startRecording(Conf.sex sx) {
			Signal.reset();
			addListeners(sx);
			Signal.setSex(sx);
			Signal.rec.startRecording();
			merkaba.startAnimation();
		}
		private void stopRecording(Conf.sex sex) {
			Signal.rec.stopRecording();
			merkaba.stopAnimation();
			switch (sex) {
			case male:		Signal.saveMale(); 		recMale=true;   break;
			case female:	Signal.saveFemale();	recFemale=true;	break;
			}
			if (recMale & recFemale) dispResonance();
		}
		private void dispResonance() {
			int hr; double rr, diff;
			fm=resonance.randVect(Conf.nFFT); ff=resonance.randVect(Conf.nFFT);
			rr		= resonance.calcResonance(fm, ff);		hr=resonance.getnHit();
			res		= resonance.calcResonance(Signal.getFftMale(), Signal.getFftFemale());		nHit=resonance.getnHit();
			diff	= resonance.calcDifference(Signal.getFftMale(), Signal.getFftFemale());
			drGauge.setBallPos(resonance.scaleRes());
			tvResult.setText(strResult=String.format("Resonance: %.0f, Emotional compat. :%d", res, nHit));
		}
		private void addListeners(final Conf.sex sex) { // sequence of listener called when rec chuck is ready: fft, graphs
			timer.start();
			Signal.resetListener();
			Signal.addListener(); // recFFT
			Signal.addListener(new AsyncListener() {
				@Override public void onDataReady() {
					handler.post(new Runnable() {
						@Override public void run() { 
							tvTimer.setText(timer.getSS());
							if (timer.getSec()<=1) tvResult.setText(String.format("recording - %s'S - voice", sex.name().toUpperCase(Locale.ENGLISH)));
						}
					});
				}
			});
			switch (sex) {
			case male:		Signal.addListener(grMale	.getListener()); break;
			case female:	Signal.addListener(grFemale	.getListener()); break;
			}
		}
		public static Bitmap createResizedBitmap(String path) { return  createResizedBitmap(path, 50); }
		public static Bitmap createResizedBitmap(String path, int kbs) { // create Bitmap of ks size in KB
			int imageMaxSize = kbs*1024; // kbs -> bytes

			BitmapFactory.Options opt = new BitmapFactory.Options();// Decode image size
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, opt);

			int scale = 1; // find scale
			while ((opt.outWidth * opt.outHeight) * (1 / Math.pow(scale, 2)) > imageMaxSize) 	scale++;

			Bitmap b = null;
			if (scale > 1) {
				scale--;
				// scale to max possible inSampleSize that still yields an image larger than target
				opt = new BitmapFactory.Options();
				opt.inSampleSize = scale;
				b = BitmapFactory.decodeFile(path, opt);

				int height = b.getHeight(), width = b.getWidth();// resize to desired dimensions
				double y = Math.sqrt(imageMaxSize / (((double) width) / height)), x = (y / height) * width;

				Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,  (int) y, true);
				b.recycle();
				b = scaledBitmap;

				System.gc();
			} else {
				b = BitmapFactory.decodeFile(path);
			}
			return b;
		}
		@TargetApi(Build.VERSION_CODES.HONEYCOMB) private static Cursor createCursorHoneycomb(Activity act, Uri imageUri) {
			String[] projection = { MediaStore.Images.Media.DATA  };
			Cursor cursor = act.getContentResolver().query(imageUri, projection, null, null, null);
			return cursor;
		}
		@SuppressWarnings("deprecation")
		@TargetApi(Build.VERSION_CODES.FROYO) 	private static Cursor createCursorFroyo(Activity act, Uri imageUri) {
			String[] projection = { MediaStore.Images.Media.DATA   };
			Cursor cursor = act.managedQuery(imageUri, projection, null, null, null);
			return cursor;
		}
		@SuppressWarnings("deprecation")
		public static String getImagePathFromUri(Activity act, Uri imageUri) {
			Cursor cursor = null;
			String imagePath = null;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				cursor = createCursorHoneycomb(act, imageUri);
			} else {
				cursor = createCursorFroyo(act, imageUri);
			}

			// if image is loaded from gallery
			if (cursor != null) {
				act.startManagingCursor(cursor);

				int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

				cursor.moveToFirst();
				imagePath = cursor.getString(columnIndex);
			}
			// if image is loaded from file manager
			else {
				imagePath = imageUri.getPath();
			}

			return imagePath;
		}
		private static void mess(String s) {
			Toast.makeText(act, s, Toast.LENGTH_SHORT).show();
		}
		final int SELECT_PICTURE_RESULT_CODE=3;		// get image from gallery
		protected void getSubjectPhoto(Conf.sex gender) {
			this.gender=gender;
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent,"Select member photo"), SELECT_PICTURE_RESULT_CODE);
		}
		protected String getName(String path) {
			int pos = path.lastIndexOf(".");
			if (pos > 0) path = path.substring(0, pos);
			return path;
		}
		@Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if (resultCode == Activity.RESULT_OK)   {
				switch (requestCode)   {
				case SELECT_PICTURE_RESULT_CODE:
					try {
						(gender == Conf.sex.male ? ivMale : ivFemale).setImageBitmap(createResizedBitmap(getImagePathFromUri(this,data.getData())));
						(gender == Conf.sex.male ? tvFBUserMale : tvFBUserFemale).setText( getName( new File( getImagePathFromUri(this,data.getData()) ).getName() ) );
					} catch (NullPointerException e) {
						mess("can't load requested image, notice that online images are not currently supported");
					}
					break;
				default:
					Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data); break;
				}
			} 
		}
		@Override public boolean onCreateOptionsMenu(Menu menu) {
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}
		@Override protected void onResume() { super.onResume();	} 
		@Override protected void onPause()   { 
			super.onPause();
			Signal.stopAll(); 
		}
		@Override protected void onStop()   { // ap stop, maybe not called
			super.onStop();
			Signal.stopAll(); 
		}
}
