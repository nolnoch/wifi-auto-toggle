package com.nolnoch.wifiauto;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ProximityMachine extends Service {
	private static LocationManager locationManager;
	private static WifiManager wifiManager;
	private static Location finalLocation;
	private static LocationListener locationListener;
	private static NetworkInfo cellCon, wifiCon;
	private static final int TWO_MINUTES = 120000;
	private static final int MODE_NETWORK = 1, MODE_GPS = 2, PRELIMINARY = 1,
			FINAL = 2, ALTERNATE = 0;
	private static final int MODE_ALL = MODE_NETWORK | MODE_GPS, MODE_NONE = 0;
	private static final long ONE_SECOND = 1000;
	private static float currAccuracy;
	private static int c, tick, callStatus, whichRound, gpsCount = 0;
	private static boolean wifiEnabled, gpsEnabled, wifiConnected, cellAvailable,
			ticktock, mustDoWifiCheck;
	private SQLAdapter sqlAdapter;
	private Cursor sqlCursor;
	public static int setFinalDelta = 120, setTimeout = 10;
	
	// Start with buffered radius of smaller maximum cell size estimate and prepare
	// for worse accuracy in location update calls.
	private static final int PRELIM_DELTA = 2000;
	
	private CountDownTimer timeout = new CountDownTimer(ONE_SECOND*setTimeout, ONE_SECOND) {
		public void onTick(long remainder) {}

	    public void onFinish() {
	    	switch (whichRound) {
	    	case ALTERNATE:
	    		ConnectivityManager conManager = (ConnectivityManager) ProximityMachine.this.getSystemService(CONNECTIVITY_SERVICE);
	    		wifiCon = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    		if (!wifiCon.isConnectedOrConnecting())
	    	 		wifiManager.setWifiEnabled(false);
	    	 	stopSelf();
	    		break;
	    	case PRELIMINARY:
	    		endListener();
	    		ticktock = false;
	    		if (finalLocation != null) {
	    			currAccuracy = finalLocation.hasAccuracy() ? finalLocation.getAccuracy() : 0;
	    			mustDoWifiCheck = currAccuracy > PRELIM_DELTA || !gpsEnabled;
	    			process(finalLocation);
	    		} else
	    			stopSelf();
	    		break;
	    	case FINAL:
	    		endListener();
	    		ticktock = false;
	    		if (finalLocation != null) {
	    			mustDoWifiCheck = gpsCount < 1;
    				process(finalLocation);
	    		} else
	    			stopSelf();
	    		break;
	    	default:
	    		Log.w("DEBUG", "Unkown case for CountDownTimer onFinish()");
	    		stopSelf();
	    		break;
			}
	     }
	  };
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		
		wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
		locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
		TelephonyManager teleManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
		ConnectivityManager conManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
		wifiCon = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		cellCon = conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		finalLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		gpsEnabled = (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		wifiEnabled = (wifiManager.isWifiEnabled());
		wifiConnected = wifiCon.isConnectedOrConnecting();
		cellAvailable = cellCon.isAvailable();
		callStatus = teleManager.getCallState();
		ticktock = mustDoWifiCheck = false;
		
		sqlAdapter = new SQLAdapter(this);
		sqlAdapter.open();
		sqlCursor = sqlAdapter.fetchAllLocations();    	
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int id) {
		
		switch(callStatus) {
		case TelephonyManager.CALL_STATE_OFFHOOK:
		case TelephonyManager.CALL_STATE_RINGING:
			stopSelf();
			break;
		case TelephonyManager.CALL_STATE_IDLE:
		default:
			break;
		}
		
		c = sqlCursor.getCount();
		
		if ( (c <= 0) || (wifiConnected) || (!gpsEnabled && !wifiEnabled && !cellAvailable) ) {
			stopSelf();
			return START_NOT_STICKY;
		}
		
		locationListener = new LocationListener() {
    		public void onLocationChanged(Location location) {
    			if (location.getProvider().equals(LocationManager.GPS_PROVIDER))
    				gpsCount++;
    			if (isBetterLocation(location, finalLocation))
    	    		finalLocation = location;
    			currAccuracy = finalLocation.hasAccuracy() ? finalLocation.getAccuracy() : 0;
    			tick++;
    			if (tick >= whichRound+1) {
    				timeout.cancel();
    				endListener();
    				ticktock = false;
    				mustDoWifiCheck = (whichRound == FINAL && ((gpsEnabled && gpsCount < 1) || (currAccuracy > 2*setFinalDelta))) ||
    						(whichRound == PRELIMINARY && currAccuracy > PRELIM_DELTA) || !gpsEnabled;
    				process(finalLocation);
    			}
    		}
    		public void onStatusChanged(String provider, int status, Bundle extras) {
    			if (status == LocationProvider.OUT_OF_SERVICE)
    				stopSelf();
    			if (wifiConnected)
    				stopSelf();
    		}
    		public void onProviderEnabled(String provider) {}
    		public void onProviderDisabled(String provider) {
    			stopSelf();
    		}
    	};
    	
    	if (wifiEnabled || cellAvailable)
    		findLocation(MODE_NETWORK, PRELIMINARY);
    	else if (gpsEnabled)
    		findLocation(MODE_GPS, FINAL);
    	else
    		stopSelf();
    		
    	return START_NOT_STICKY;
	}
	
	private void findLocation(int mode, int round) {		
		whichRound = round;
    	tick = gpsCount = 0;
    	ticktock = true;
		
		if ((mode & MODE_NETWORK) > 0)
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    	if ((mode & MODE_GPS) > 0)
    		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    	
    	if (round == 0) {
    		wifiManager.setWifiEnabled(true);
    		setTimeout = 12;
    	} else
    		setTimeout = round*5;
    	
    	timeout.start();
	}
	
	private void process(Location currentLocation) {
		
		int io = 0;
		int processDelta = whichRound > 1 ? setFinalDelta : PRELIM_DELTA;
		
		
		for (int i = 0; i < c; i++) {
			sqlCursor.moveToPosition(i);
			int s = sqlCursor.getInt(sqlCursor.getColumnIndexOrThrow(SQLAdapter.KEY_STATUS));
			if (s == 1) {
				Location storedLocation = new Location("");
				storedLocation.setLatitude(sqlCursor.getDouble(sqlCursor.getColumnIndexOrThrow(SQLAdapter.KEY_LATITUDE)));
				storedLocation.setLongitude(sqlCursor.getDouble(sqlCursor.getColumnIndexOrThrow(SQLAdapter.KEY_LONGITUDE)));
				float delta = storedLocation.distanceTo(currentLocation);
				if (delta <= processDelta)
					io++;
			}
		}
		if (whichRound == FINAL && mustDoWifiCheck) {
			findLocation(MODE_NONE, ALTERNATE);
		} else if (io == 0) {
			wifiManager.setWifiEnabled(false);
			stopSelf();
		} else if (io * whichRound > io) {
			wifiManager.setWifiEnabled(true);
			stopSelf();
		} else {
			if (mustDoWifiCheck)
				findLocation(MODE_NONE, ALTERNATE);
			else
				findLocation(MODE_ALL, FINAL);
		}				
	}
	
	private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    private void endListener() {
    	locationManager.removeUpdates(locationListener);
    	ticktock = false;
		tick = 0;
    }
        
    private void terminate() {
    	sqlCursor.close();
    	sqlAdapter.close();
    	if (ticktock) {
    		timeout.cancel();
    		endListener();
    	}
    }
    
    @Override
    public void onDestroy() {
    	terminate();
    }

}
