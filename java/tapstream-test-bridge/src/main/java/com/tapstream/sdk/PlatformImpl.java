package com.tapstream.sdk;

import com.tapstream.sdk.wordofmouth.Reward;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

class PlatformImpl implements Platform {
	public Response response = new Response(200, null, null);
	public Set<String> savedFiredList = null;

	public PlatformImpl() {
	}

	public ThreadFactory makeWorkerThreadFactory() {
		return new WorkerThread.Factory();
	}

	public String loadUuid() {
		return "00000000-0000-0000-0000-000000000000";
	}

	public Set<String> loadFiredEvents() {
		return new HashSet<String>();
	}

	public void saveFiredEvents(Set<String> firedEvents) {
		savedFiredList = new HashSet<String>(firedEvents);
	}

	public String getResolution() {
		return "480x960";
	}

	public String getManufacturer() {
		return "TestManfacturer";
	}

	public String getModel() {
		return "TestModel";
	}

	public String getOs() {
		return "AndroidTestOs";
	}

	public String getLocale() {
		return "en_US";
	}

	public String getWifiMac() {
		return "00:00:00:00:00:00";
	}

	public String getDeviceId() {
		return "000000000000000";
	}

	public String getAndroidId() {
		return "1111111111111111";
	}

	public String getAppName() {
		return "Test App";
	}
	
	public String getAppVersion() {
		return "1.0.0.0";
	}

	public String getPackageName() {
		return "com.test.TestApp";
	}

	public Response request(String url, String data, String method) {
		return response;
	}
	
	public String getReferrer() {
		return "utm_campaign=blah&utm_source=google";
	}
	
	public String getAdvertisingId() {
		return "00000000-0000-0000-0000-000000000000";
	}

	public Boolean getLimitAdTracking() {
		return false;
	}

	@Override
	public Integer getCountForReward(Reward reward) { return 0; }

	@Override
	public void consumeReward(Reward reward) {  }
}
