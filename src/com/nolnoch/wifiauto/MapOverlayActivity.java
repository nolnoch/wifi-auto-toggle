package com.nolnoch.wifiauto;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MapOverlayActivity extends ItemizedOverlay<OverlayItem> {
	
	private ArrayList<OverlayItem> mapoverlay = new ArrayList<OverlayItem>();
	
	public MapOverlayActivity(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}
	
	public void addOverlay(OverlayItem overlay) {
	    mapoverlay.add(overlay);
	    populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mapoverlay.get(i);
	}
	
	@Override
	public int size() {
		return mapoverlay.size();
	}
}
