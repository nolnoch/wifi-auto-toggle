package com.nolnoch.wifiauto;

import java.util.List;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;

public class WifiAutoToggleActivity extends TabActivity {
    /** Called when the activity is first created. */
	
	//Anchor Wifi and GPS service connections.
	public static WifiManager wifiManager;
    public static LocationManager locationManager;
    private static ConnectivityManager conManager;
    private static NetworkInfo wifiCon, cellCon;
    private static TextView textview;
    private static ComponentName component;
    
    //Licensing infrastructure declarations
    private static final String BASE64_PUBLIC_KEY = "REDACTED";
    private static final byte[] SALT =
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int RETRY = 0x04, MARKET = 0x08, APP_ERROR = 0x16;
    private static MyLicenseCheckerCallback mLicenseCheckerCallback;
	private static LicenseChecker mChecker;
	private static String deviceId;
    
	private Handler mHandler;
	private AlertDialog.Builder licenseDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Reusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, AutomaticActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("automatic").setIndicator("Automatic",
                res.getDrawable(R.drawable.ic_tab_auto))
                      .setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        intent = new Intent().setClass(this, ManualActivity.class);
        spec = tabHost.newTabSpec("manual").setIndicator("Manual",
                res.getDrawable(R.drawable.ic_tab_manual))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, ManageActivity.class);
        spec = tabHost.newTabSpec("manage").setIndicator("Manage",
                res.getDrawable(R.drawable.ic_tab_manage))
                      .setContent(intent);
        tabHost.addTab(spec);

       	tabHost.setCurrentTab(0);
       	
       	Context pkg = getApplicationContext();
		component = new ComponentName(pkg.getPackageName(), CustomReceiver.class.getName());
       	
       	wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		mHandler = new Handler();
		
		//Prepare to Check License against Google Play servers
		runCheckLicense();
        
    }
    
    protected void runCheckLicense() {
    	conManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
    	wifiCon = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	cellCon = conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    	
    	//TODO increase security of deviceId
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
     	//Construct the LicenseCheckerCallback. The library calls this when done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();

        // Construct the LicenseChecker with a Policy.
        mChecker = new LicenseChecker(
            this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
            BASE64_PUBLIC_KEY
            );
        
        doCheck();
    }
    
    protected void serviceStatus() {
		textview = (TextView) findViewById(R.id.GPSstatus);		
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        	textview.setText("GPS ON");
        	textview.setTextColor(0xAA33FF33);
        } else {
        	textview.setText("GPS OFF");
        	textview.setTextColor(0xAAFF3333);
        }
        textview = (TextView) findViewById(R.id.WIFIstatus);
        if (wifiManager.isWifiEnabled()) {
        	textview.setText("WiFi ON");
        	textview.setTextColor(0xAA33FF33);
        } else {
        	textview.setText("WiFi OFF");
        	textview.setTextColor(0xAAFF3333);
        }
        textview = (TextView) findViewById(R.id.servicestatus);
        Context pkg = getApplicationContext();
		int io = pkg.getPackageManager().getComponentEnabledSetting(component);
		if (io == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
			textview.setText("Service ON");
			textview.setTextColor(0xFF808080);
		} else if ((io == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) || (io == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)) {
			textview.setText("Service OFF");
			textview.setTextColor(0x99808080);
		}
        
    }
    
    //TextView-click method to launch settings menu
  	public void changeSettings(View view) {
  		//Catch-all menu for all devices
  		Intent settingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
  		Intent tmpIntent;
  		
  		//Assign specifically requested menu for devices that have them 
  		switch (view.getId()) {
  		case R.id.GPSstatus:
  			tmpIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
  			if (isCallable(tmpIntent))
  				settingsIntent = tmpIntent;
  			break;
  		case R.id.WIFIstatus:
  			tmpIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
  			if (isCallable(tmpIntent))
  				settingsIntent = tmpIntent;
  			break;
  		default:			
  			break;
  		}
  		
  		//Launch menu
  		try {
  			startActivity(settingsIntent);
  		} catch (ActivityNotFoundException e) {
  			new AlertDialog.Builder(WifiAutoToggleActivity.this).setTitle("Menu Not Found").setMessage("Could not launch chosen Settings menu.").setNeutralButton("Close", null).show();
  			Log.e("DEBUG", e.getMessage());
  		}
  	}
  	
  	private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
  	    
  		public void allow(int reason) {
  	        if (isFinishing()) {
  	            // Don't update UI if Activity is finishing.
  	            return;
  	        }
  	        // Should allow user access.
  	        // Do nothing apparent.  Or really, do nothing at all.
  	    }

  	    public void dontAllow(int reason) {
  	        if (isFinishing()) {
  	            // Don't update UI if Activity is finishing.
  	            return;
  	        }  	        
  	        if (reason == Policy.RETRY) {
  	            // If the reason received from the policy is RETRY, it was probably
  	            // due to a loss of connection with the service, so we should give the
  	            // user a chance to retry. So show a dialog to retry.
  	            displayResult(RETRY);
  	        } else {
  	            // Otherwise, the user is not licensed to use this app.
  	            // Your response should always inform the user that the application
  	            // is not licensed, but your behavior at that point can vary. You might
  	            // provide the user a limited access version of your app or you can
  	            // take them to Google Play to purchase the app.
  	            displayResult(MARKET);
  	        }
  	    }
  	    
  	    public void applicationError(int errorCode) {
  	    	switch(errorCode) {
  	    	case ERROR_INVALID_PACKAGE_NAME:
  	    	case ERROR_NON_MATCHING_UID:
  	    	case ERROR_NOT_MARKET_MANAGED:
  	    	case ERROR_CHECK_IN_PROGRESS:
  	    	case ERROR_INVALID_PUBLIC_KEY:
  	    	case ERROR_MISSING_PERMISSION:
  	    	default:
  	    		displayResult(APP_ERROR);
  	    		Log.w("DEBUG", "License Application Error Code " + errorCode);
  	    		break;
  	    	}
  	    }
  	    
  	    private void displayResult(final int result) {
  	    	mHandler.post(new Runnable() {
  	    		public void run() {
  	    			licenseDialog = new AlertDialog.Builder(WifiAutoToggleActivity.this).setCancelable(false)
  	    				.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							WifiAutoToggleActivity.this.finish();
							
						}
					});
  	    			
  	    			switch(result) {
  	    			case RETRY:
  	    				licenseDialog.setTitle("License Check Error")
  	    					.setMessage("License check incomplete due to network problems.\n\nCheck mobile or WiFi connectivity and try again.")
  	    					.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface dialog, int which) {
									doCheck();
									
								}
							});
  	    				break;
  	    			case MARKET:
  	    				licenseDialog.setTitle("License Invalid")
  	    					.setMessage("Unauthorized use of this application not permitted.\n\nPlease purchase from Google Play (only USD $.99!).")
  	    					.setPositiveButton("Google Play", new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface dialog, int which) {
									Uri uri = Uri.parse("market://details?id=com.nolnoch.wifiauto");
									Intent intent = new Intent (Intent.ACTION_VIEW, uri); 
									startActivity(intent);
									WifiAutoToggleActivity.this.finish();
									
								}
							});
  	    				break;
  	    			case APP_ERROR:
  	    				licenseDialog.setTitle("License Check Error")
  	    					.setMessage("Application Error\n\nCheck LogCat for more information.")
  	    					.setPositiveButton("OK", null);
  	    				break;
  	    			default:
  	    				licenseDialog.setTitle("Unkown License Error")
	    					.setMessage("You may continue to use this application, but please email support@market.nolnoch.net with a bug report.")
	    					.setPositiveButton("Continue", null);
  	    				break;
  	    			}
  	    			
  	    			licenseDialog.show();
  	    		}
  	    	});
  	    }
  	    
  	}
  	
  	protected void doCheck() {
  		if (wifiCon.isConnected() || cellCon.isConnected())
  			mChecker.checkAccess(mLicenseCheckerCallback);
		else {
			licenseDialog = new AlertDialog.Builder(WifiAutoToggleActivity.this).setCancelable(false)
				.setTitle("No Network Available")
				.setMessage("This app uses Google Play licensing.\n\nPlease enable Cellular data transfer or connect to a WiFi network before trying again.")
				.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Intent settingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
						try {
				  			startActivity(settingsIntent);
				  		} catch (ActivityNotFoundException e) {
				  			new AlertDialog.Builder(WifiAutoToggleActivity.this).setTitle("Menu Not Found").setMessage("Could not launch chosen Settings menu.").setNeutralButton("Close", null).show();
				  			Log.e("DEBUG", e.getMessage());
				  		}
						WifiAutoToggleActivity.this.finish();
						
					}
				})
				.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						WifiAutoToggleActivity.this.finish();
						
					}
				});
			licenseDialog.show();
		}
  	}
  	
  	private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 
            PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
  	}
    
    @Override
    public void onResume() {
    	super.onResume();
    	serviceStatus();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	mChecker.onDestroy();
    }
}
