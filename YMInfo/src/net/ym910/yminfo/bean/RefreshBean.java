package net.ym910.yminfo.bean;

import android.view.MenuItem;
import android.view.View;

public class RefreshBean {

	private MenuItem item;
	
	private View refreshActionView;

	public MenuItem getItem() {
		return item;
	}

	public void setItem(MenuItem item) {
		this.item = item;
	}

	public View getRefreshActionView() {
		return refreshActionView;
	}

	public void setRefreshActionView(View refreshActionView) {
		this.refreshActionView = refreshActionView;
	}

	public void clearRefresh() {
		if (refreshActionView != null)
		{
			refreshActionView.clearAnimation();
		}
		if (item != null)
		{
			item.setActionView(null);
		}
	}
}
