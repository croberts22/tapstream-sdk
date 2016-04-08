package com.tapstream.sdk;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import com.tapstream.sdk.wordofmouth.Reward;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;


class PlatformImpl implements Platform {
	private static final String FIRED_EVENTS_KEY = "TapstreamSDKFiredEvents";
	private static final String UUID_KEY = "TapstreamSDKUUID";
	private static final String WOM_REWARDS_KEY = "TapstreamWOMRewards";

	private Context context;

	public PlatformImpl(Context context) {
		this.context = context;
	}

	public ThreadFactory makeWorkerThreadFactory() {
		return new WorkerThread.Factory();
	}

	public String loadUuid() {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(UUID_KEY, 0);
		String uuid = prefs.getString("uuid", null);
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("uuid", uuid);
			editor.commit();
		}
		return uuid;
	}

	public Set<String> loadFiredEvents() {
		SharedPreferences settings = context.getApplicationContext().getSharedPreferences(FIRED_EVENTS_KEY, 0);
		Map<String, ?> fired = settings.getAll();
		return new HashSet<String>(fired.keySet());
	}

	public void saveFiredEvents(Set<String> firedEvents) {
		SharedPreferences settings = context.getApplicationContext().getSharedPreferences(FIRED_EVENTS_KEY, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		for (String name : firedEvents) {
			editor.putString(name, "");
		}
		editor.commit();
	}

	public String getResolution() {
		WindowManager wm = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		return String.format(Locale.US, "%dx%d", metrics.widthPixels, metrics.heightPixels);
	}

	public String getManufacturer() {
		try {
			return Build.MANUFACTURER;
		} catch (Exception e) {
			return null;
		}
	}

	public String getModel() {
		return Build.MODEL;
	}

	public String getOs() {
		return String.format(Locale.US, "Android %s", Build.VERSION.RELEASE);
	}

	public String getLocale() {
		return Locale.getDefault().toString();
	}

	public String getAppName() {
		PackageManager pm = context.getPackageManager();
		String appName;
		try {
			ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), 0);
			CharSequence cs = pm.getApplicationLabel(ai);
			appName = cs.toString();
		} catch (NameNotFoundException e) {
			appName = context.getPackageName();
		}
		return appName;
	}
	
	public String getAppVersion() {
		PackageManager pm = context.getPackageManager();
		try {
			return pm.getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	}

	public String getPackageName() {
		return context.getPackageName();
	}

	public Response request(String url, String data, String method) {
		WorkerThread th = (WorkerThread) Thread.currentThread();

		HttpRequestBase req;
		if(method == "POST") {
			req = new HttpPost(url);
			if(data != null) {
				StringEntity se = null;
				try {
					se = new StringEntity(data);	
				} catch (UnsupportedEncodingException e) {
					return new Response(-1, e.toString(), null);
				}
				se.setContentType("application/x-www-form-urlencoded");
				((HttpPost)req).setEntity(se);
			}
		} else {
			req = new HttpGet(url);
		}
		req.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

		HttpResponse response = null;
		try {
			response = th.client.execute(req);
		} catch (Exception e) {
			return new Response(-1, e.toString(), null);
		}

		StatusLine statusLine = response.getStatusLine();
		String responseData = null;
		try {
			InputStream is = response.getEntity().getContent();
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				StringBuilder sb = new StringBuilder();
				String line;
				while((line = br.readLine()) != null) {
					sb.append(line);
				}
				responseData = sb.toString();
			} finally {
				is.close();
			}
		} catch (Exception e) {
			return new Response(-1, e.toString(), null);
		}

		if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
			return new Response(200, null, responseData);
		}
		return new Response(statusLine.getStatusCode(), statusLine.getReasonPhrase(), null);
	}
	
	public String getReferrer() {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(UUID_KEY, Context.MODE_PRIVATE);
		return prefs.getString("referrer", null);
	}
	
	public String getAdvertisingId() {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(UUID_KEY, Context.MODE_PRIVATE);
		return prefs.getString("advertisingId", null);
	}

	public Boolean getLimitAdTracking() {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(UUID_KEY, Context.MODE_PRIVATE);
		return prefs.contains("limitAdTracking") ? prefs.getBoolean("limitAdTracking", false) : null;
	}

	@Override
	public Integer getCountForReward(Reward reward){
		synchronized (this) {
			SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(WOM_REWARDS_KEY, Context.MODE_PRIVATE);
			return prefs.getInt(Integer.toString(reward.getOfferId()), 0);
		}
	}

	@Override
	public void consumeReward(Reward reward){
		String key = String.format("%d", reward.getOfferId());
		synchronized (this) {
			SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(WOM_REWARDS_KEY, Context.MODE_PRIVATE);

			prefs.edit()
				.putInt(key, prefs.getInt(key, 0) + 1)
				.apply();
		}
	}
}
