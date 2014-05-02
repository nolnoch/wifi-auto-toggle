package com.nolnoch.wifiauto;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MapFrameActivity extends MapActivity {
	private static double lat;
	private static double lon;
	private static int i;
	private static Button b;
	private static TextView tv;
	private AlertDialog.Builder alert;
	private SQLAdapter sqlAdapter;
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent().hasExtra(SQLAdapter.KEY_LABEL)) {
			setContentView(R.layout.editdialog);
			format();}
		else {setContentView(R.layout.mapdialog); }
		
		MapView mapbox = (MapView) findViewById(R.id.mapbox);
		mapbox.setBuiltInZoomControls(true);
		
		List<Overlay> mapOverlays = mapbox.getOverlays();
		Drawable drawable = this.getResources().getDrawable(android.R.drawable.ic_notification_overlay);
		MapOverlayActivity overlayMarker = new MapOverlayActivity(drawable);
		
		lat = getIntent().getDoubleExtra(SQLAdapter.KEY_LATITUDE, 0);
		lon = getIntent().getDoubleExtra(SQLAdapter.KEY_LONGITUDE, 0);
		
		GeoPoint point = new GeoPoint((int)(lat*1E6), (int)(lon*1E6));
		OverlayItem markPoint = new OverlayItem(point, "", "");
		
		overlayMarker.addOverlay(markPoint);
		mapOverlays.add(overlayMarker);
		
		MapController mcontrol = mapbox.getController();
		mcontrol.animateTo(point);
		mcontrol.setZoom(18);
		
		sqlAdapter = new SQLAdapter(this);
		sqlAdapter.open();
		
	}
	
	public void format() {
		i = getIntent().getIntExtra("status", 1);
		buttSwitch();
		
		tv = (TextView) findViewById(R.id.showlabel);
		String s = "Name: " + getIntent().getStringExtra("label");
		tv.setText(s);
		tv = (TextView) findViewById(R.id.showlat);
		double d = getIntent().getDoubleExtra("latitude", 0);
		s = "Latitude: " + Double.toString(d);
		tv.setText(s);
		tv = (TextView) findViewById(R.id.showlon);
		d = getIntent().getDoubleExtra("longitude", 0);
		s = "Longitude: " + Double.toString(d);
		tv.setText(s);
	}
	
	public void buttSwitch() {
		tv = (TextView) findViewById(R.id.showstatus);
		b = (Button) findViewById(R.id.buttstatus);
		if (i == 0) {
			b.setText(getString(R.string.button_enable));
			tv.setText("Status: Disabled"); }
		else {b.setText(getString(R.string.button_disable));
			tv.setText("Status: Enabled"); }
	}
	
	public void yesClick(View view) {
		alert = new AlertDialog.Builder(MapFrameActivity.this);
		alert.setTitle("Success").setMessage("Enter a name for this location.");

		final EditText input = new EditText(MapFrameActivity.this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String name = input.getText().toString();
			sqlAdapter.createLocation(name, lat, lon, 1);
			terminate();
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
			  terminate();
		  }
		});

		alert.show();
	}
	
	public void noClick(View view) {
		alert = new AlertDialog.Builder(MapFrameActivity.this);
		alert.setTitle("Sorry").setMessage("Please try again or use alternate method.");
		alert.setNeutralButton("Close", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish(); }});
		alert.show();
	}
	
	public void editClick(View view) {
		alert = new AlertDialog.Builder(MapFrameActivity.this);
		alert.setTitle("Rename").setMessage("Enter a new name for this location.");

		final EditText input = new EditText(MapFrameActivity.this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			long locId = getIntent().getLongExtra(SQLAdapter.KEY_ROWID, 0);
			String name = input.getText().toString();
			sqlAdapter.updateLocation(locId, name, lat, lon, 1);
			terminate();
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
			  terminate();
		  }
		});

		alert.show();
	}
	
	public void statusClick(View view) {
		long locId = getIntent().getLongExtra(SQLAdapter.KEY_ROWID, 0);
		String name = getIntent().getStringExtra(SQLAdapter.KEY_LABEL);
		if (i == 0) {
			sqlAdapter.updateLocation(locId, name, lat, lon, 1);
			i = 1; }
		else {
			sqlAdapter.updateLocation(locId, name, lat, lon, 0);
			i = 0; }
		buttSwitch();
	}
	
	public void deleteClick(View view) {
		long locId = getIntent().getLongExtra(SQLAdapter.KEY_ROWID, 0);
		sqlAdapter.deleteLocation(locId);
		terminate();
	}
	
	public void closeClick(View view) {
		terminate();
	}
	
	public void terminate() {
		setResult(RESULT_OK);
		finish();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (isFinishing())
			sqlAdapter.close();
	}
	@Override
	protected void onDestroy() {
		sqlAdapter.close();
		super.onDestroy();
	}
}
