package com.nolnoch.wifiauto;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TabHost;

public class ManualActivity extends Activity {
	//Class variables
	private static String streetvalue, cityvalue, statevalue, zipvalue, address;
	private static EditText streetfield, cityfield, statefield, zipfield;
	private static double coordLat;
	private static double coordLong;
	private AlertDialog.Builder alert;
	private static final int CONTACT_PICKER_RESULT=1020, MAP_RESULT=1040;
	private static InputMethodManager imm;
		
	//Manual acquisition of latitude and longitude.
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manualpage);
		streetfield = (EditText) findViewById(R.id.streetbox);
		cityfield = (EditText) findViewById(R.id.citybox);
		statefield = (EditText) findViewById(R.id.statebox);
		zipfield = (EditText) findViewById(R.id.zipbox);
		imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
	}
	
	//Import Street, City, State, and Zip from contacts.
	public void importContactInfo(View view) {
		clearFields();
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}
	
	public void verify(View view) {
		
		streetvalue = streetfield.getText().toString();
		cityvalue = cityfield.getText().toString();
		statevalue = statefield.getText().toString();
		zipvalue = zipfield.getText().toString();
		alert = new AlertDialog.Builder(ManualActivity.this).setTitle("Incomplete").setMessage("Street with City/State or Zip required.").setNeutralButton("Close", null);
		
		if ((!streetvalue.contentEquals("")) && ((!zipvalue.contentEquals("")) || ( (!cityvalue.contentEquals("") ) && (!statevalue.contentEquals("") )))) {
			addressLookup(); } else {alert.show(); }
	}
	
	public void addressLookup() {

		alert = new AlertDialog.Builder(ManualActivity.this).setTitle("Address Not Found").setMessage("Please use Automatic Tab.").setNeutralButton("Close", null);
		
		if (!zipvalue.contentEquals("")) {
			address = streetvalue + ", " + zipvalue;} else {
			address = streetvalue + ", " + cityvalue + ", " + statevalue; }
		
		Geocoder geodex = new Geocoder(getApplicationContext(), Locale.US);
		try {
			List<Address> coordlist = geodex.getFromLocationName(address, 1);
			if (coordlist != null) {
				coordLat = coordlist.get(0).getLatitude();
				coordLong = coordlist.get(0).getLongitude();
				Intent mapIntent = new Intent(ManualActivity.this, MapFrameActivity.class);
				mapIntent.putExtra("latitude", coordLat);
				mapIntent.putExtra("longitude", coordLong);
				startActivityForResult(mapIntent, MAP_RESULT);
			} else {
				alert.show();
			}
		} catch (IOException e) {
			new AlertDialog.Builder(ManualActivity.this).setTitle("Maps Not Found").setMessage("Install Google Maps or use Automatic Tab.").setNeutralButton("Close", null).show();
			Log.e("IOException", e.getMessage());
		}
	}
	
	public void clearFields() {
		EditText[] locFields = {streetfield, cityfield, statefield, zipfield};
		for (int i = 0; i < locFields.length; i++) {
			locFields[i].setText("");
		}
		streetvalue = cityvalue = statevalue = zipvalue = "";
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				try {
					//attempt fetch of address
					Cursor addrCur = null;
					Uri contactUri = intent.getData();
					String contactId = contactUri.getLastPathSegment();
					ContentResolver cr = getContentResolver();
					String addrWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
					String[] addrWhereParams = new String[]{contactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE}; 
					addrCur = cr.query(ContactsContract.Data.CONTENT_URI, null, addrWhere, addrWhereParams, null); 
					if (addrCur.moveToNext()) {
						streetvalue = addrCur.getString(
							addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
						cityvalue = addrCur.getString(
							addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
						statevalue = addrCur.getString(
							addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
						zipvalue = addrCur.getString(
							addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
					} else {
						Log.w("DEBUG", "No results found");
						streetvalue = cityvalue = statevalue = zipvalue = "";
					}
					if (addrCur != null)
						addrCur.close();
				} catch (Exception e) {
					//report a failed lookup
					alert = new AlertDialog.Builder(ManualActivity.this).setTitle("Error").setMessage("Contact address lookup failed.").setNeutralButton("Close", null);
					alert.show();
					Log.e("DEBUG", "Failed to fetch address", e);
				} finally {
					//check for and assign retrieved values
					EditText[] locFields = {streetfield, cityfield, statefield, zipfield};
					String[] locValues = {streetvalue, cityvalue, statevalue, zipvalue};
					int i, j = 0;
					for (i = 0; i < locValues.length; i++) {
						if (locValues[i] == null)
							locValues[i] = "";
						boolean isField = locValues[i].length() != 0;
						locFields[i].setText(isField ? locValues[i] : "");
						if (isField)
							j++;
					}
					if (j == 0) {
						alert = new AlertDialog.Builder(ManualActivity.this).setTitle("Empty").setMessage("Contact returned no address.").setNeutralButton("Close", null);
						alert.show();
					}
				}
				break;
			case MAP_RESULT:
				clearFields();
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
	public void onResume() {
		super.onResume();
		imm.hideSoftInputFromWindow(streetfield.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(zipfield.getWindowToken(), 0);
	}

}