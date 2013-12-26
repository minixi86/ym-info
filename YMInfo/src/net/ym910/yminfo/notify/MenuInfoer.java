package net.ym910.yminfo.notify;

import net.ym910.yminfo.R;
import net.ym910.yminfo.utils.PrefUtils;
import android.content.Context;
import android.widget.Toast;

public class MenuInfoer {

	private static MenuInfoer instance = new MenuInfoer();

	private MenuInfoer() {

	}

	public static final String hiddenRead = "hiddenRead";
	public static final String refreshAll = "refreshAll";
	public static final String refreshCurrent = "refreshCurrent";

	public static MenuInfoer instance() {
		return instance;
	}

	public void hiddenRead(Context c) {
		if (PrefUtils.getBoolean(hiddenRead, true)) {
			PrefUtils.putBoolean(hiddenRead, false);
			Toast.makeText(c, R.string.hiddenRead, Toast.LENGTH_SHORT).show();
		}
	}

	public void refreshAll(Context c) {
		if (PrefUtils.getBoolean(refreshAll, true)) {
			PrefUtils.putBoolean(refreshAll, false);
			Toast.makeText(c, R.string.refreshAll, Toast.LENGTH_SHORT).show();
		}
	}

	public void refreshCurrent(Context c) {
		if (PrefUtils.getBoolean(refreshCurrent, true)) {
			PrefUtils.putBoolean(refreshCurrent, false);
			Toast.makeText(c, R.string.refreshCurrent, Toast.LENGTH_SHORT).show();
		}
	}
}
