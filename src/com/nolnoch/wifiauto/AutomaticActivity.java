package com.nolnoch.wifiauto;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;

public class AutomaticActivity extends Activity {
	//Anchor Wifi and GPS service connections.
	public static WifiManager wifiManager;
	public static LocationManager locationManager;
    
    public static Location finalLocation;
    public static LocationListener locationListener; 
	public static int tick = 0;
	private static final int TWO_MINUTES = 120000;
	private static final int AUTO_RESULT = 1060;
	private static final long ONE_SECOND = 1000;
	private static double coordLat;
	private static double coordLong;
	private static boolean ticktock = false;
	
	private static ProgressDialog searching;
	
	private CountDownTimer timeout = new CountDownTimer(ONE_SECOND*40, ONE_SECOND) {
	     public void onTick(long remainder) {}

	     public void onFinish() {
	    	endListener();
	    	ticktock = false;
	    	if (finalLocation != null) {
				process(); }
			else {Toast.makeText(getApplicationContext(), "Network timeout. Please try again.", Toast.LENGTH_SHORT).show(); }
	     }
	  };
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.autopage);
        
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
    	finalLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	
	}
	
	//Define method called by Button click.
    public void locateMe(View view) {
    	
    	searching = ProgressDialog.show(AutomaticActivity.this, "", 
                "Searching...", true, false);
    	
    	locationListener = new LocationListener() {
    		public void onLocationChanged(Location location) {
    			if (isBetterLocation(location, finalLocation)) {
    	    		finalLocation = location; }
    			tick++;
    			if (tick >= 3) {
    				timeout.cancel();
    				endListener();
    				if (finalLocation != null) {
    					process(); }
    				else {
    					Toast.makeText(getApplicationContext(), "No location found.", Toast.LENGTH_SHORT).show(); }
    			}
    		}
    		public void onStatusChanged(String provider, int status, Bundle extras) {}
    		public void onProviderEnabled(String provider) {}
    		public void onProviderDisabled(String provider) {}
    	};
    	    	
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    	  
    	timeout.start();
    	ticktock = true;
    }
    
    /** Determines whether one Location reading is better than the current Location fix
      * @param location  The new Location that you want to evaluate
      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
      */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
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
    
    public void endListener() {
    	locationManager.removeUpdates(locationListener);
    	searching.dismiss();
		tick = 0;
    }
    
    public void process() {
    	coordLat = finalLocation.getLatitude();
		coordLong = finalLocation.getLongitude();
		
		Intent intent = new Intent(AutomaticActivity.this, MapFrameActivity.class);
		intent.putExtra("latitude", coordLat);
		intent.putExtra("longitude", coordLong);
		startActivityForResult(intent, AUTO_RESULT);
    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case AUTO_RESULT:
				TabHost tabHost =  (TabHost) getParent().findViewById(android.R.id.tabhost);
	            tabHost.setCurrentTab(2);
				break;
			default:
				Log.w("DEBUG", "Activity not prepared to handle request");
				break;
			}
		} else {
			Log.w("DEBUG", "Activity result not OK");
		}
	}
    
    @Override
	protected void onPause() {
    	if (ticktock) {
    		timeout.cancel();
    		endListener(); }
    	super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}
}