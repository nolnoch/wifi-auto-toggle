package com.nolnoch.wifiauto;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ManageActivity extends ListActivity {
	private SQLAdapter sqlAdapter;
	private Cursor sqlCursor;
	private static ComponentName component;
	private static Button buttService;
	private static TextView textview;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.managepage);
		sqlAdapter = new SQLAdapter(this);
		sqlAdapter.open();
		sqlCursor = sqlAdapter.fetchAllLocations();
		startManagingCursor(sqlCursor);
		
		Context pkg = getApplicationContext();
		component = new ComponentName(pkg.getPackageName(), CustomReceiver.class.getName());
		buttService = (Button) findViewById(R.id.buttservice);
	  
	}
	  
	public void fillData() {
	
		// Populate ListView from database
		String[] from = new String[] {SQLAdapter.KEY_LABEL};
		int[] to = new int[] {R.id.text1};
	        
		// Now create an array adapter and set it to display using our row
		MyCursorAdapter labels = new MyCursorAdapter(this, R.layout.list_item_play, sqlCursor, from, to);
		setListAdapter(labels);
		
		//Empower service button
		Context pkg = getApplicationContext();
		int io = pkg.getPackageManager().getComponentEnabledSetting(component);
		int c = sqlCursor.getCount();
		
		if (c <= 0) {
			buttService.setEnabled(false);
			if (io == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
				pkg.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				io = pkg.getPackageManager().getComponentEnabledSetting(component); }
		} else if (c > 0) {
			buttService.setEnabled(true);
		}
		
		if (io == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
			buttService.setText("Stop Service");
		} else if ((io == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) || (io == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)) {
			buttService.setText("Start Service");
		}
	     
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor c = sqlCursor;
        c.moveToPosition(position);
        Intent i = new Intent(this, MapFrameActivity.class);
        i.putExtra(SQLAdapter.KEY_ROWID, id);
        i.putExtra(SQLAdapter.KEY_LABEL, c.getString(
                c.getColumnIndexOrThrow(SQLAdapter.KEY_LABEL)));
        i.putExtra(SQLAdapter.KEY_LATITUDE, c.getDouble(
                c.getColumnIndexOrThrow(SQLAdapter.KEY_LATITUDE)));
        i.putExtra(SQLAdapter.KEY_LONGITUDE, c.getDouble(
                c.getColumnIndexOrThrow(SQLAdapter.KEY_LONGITUDE)));
        i.putExtra(SQLAdapter.KEY_STATUS, c.getInt(
                c.getColumnIndexOrThrow(SQLAdapter.KEY_STATUS)));
        startActivity(i);
    }
	
	private class MyCursorAdapter extends SimpleCursorAdapter {

        public MyCursorAdapter(Context context, int layoutId, Cursor cur, String[] from, int[] to) {
            super(context, layoutId, cur, from, to);
        }

        @Override
        public void bindView(View view, Context context, Cursor cur) {
            CheckedTextView checkTv = (CheckedTextView) view;
            checkTv.setText(cur.getString(cur.getColumnIndex(SQLAdapter.KEY_LABEL)));
            checkTv.setChecked((cur.getInt(cur.getColumnIndex(SQLAdapter.KEY_STATUS)))!=0);
        }
    }
	
	public void serviceClick(View view) {
		Context pkg = getApplicationContext();
		int io = pkg.getPackageManager().getComponentEnabledSetting(component);
		textview = (TextView) getParent().findViewById(R.id.servicestatus);
		if (io == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
			pkg.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
			buttService.setText("Start Service");
			textview.setText("Service OFF");
			textview.setTextColor(0x99808080);
		} else if ((io == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) || (io == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)) {
			pkg.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
			buttService.setText("Stop Service");
			textview.setText("Service ON");
			textview.setTextColor(0xFF808080);
		}
	}
	
	@Override
	protected void onPause() {
		if (isFinishing())
			sqlAdapter.close();
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
		fillData();
	}
	@Override
	protected void onDestroy() {
		sqlAdapter.close();
		super.onDestroy();
	}
}