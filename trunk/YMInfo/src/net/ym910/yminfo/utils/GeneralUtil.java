package net.ym910.yminfo.utils;

import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import net.ym910.yminfo.MainApplication;
import net.ym910.yminfo.activity.MainActivity;
import android.content.Intent;

public class GeneralUtil {
	public static boolean isEmpty(String e) {
		return e == null || "".equals(e.trim());
	}

	public static String checkRSSAddress(String ori) {
		try {
			URL url = new URL(ori);
			URLConnection openConnection = url.openConnection();
			String contentType = openConnection.getContentType();
			if (contentType != null && contentType.indexOf("xml") > 0) {
				return ori;
			}

			if ("".equals(url.getPath()) || "/".equals(url.getPath())) {
				String newurl = ori;

				if (ori.endsWith("/")) {
					newurl += "feed";
				} else {
					newurl += "/feed";
				}

				url = new URL(newurl);
				openConnection = url.openConnection();
				contentType = openConnection.getContentType();
				if (contentType != null && contentType.indexOf("xml") > 0) {
					return newurl;
				}

				newurl = ori;
				if (ori.endsWith("/")) {
					newurl += "?feed=rss2";
				} else {
					newurl += "/feed=rss2";
				}
				url = new URL(newurl);
				openConnection = url.openConnection();
				contentType = openConnection.getContentType();
				if (contentType != null && contentType.indexOf("xml") > 0) {
					return newurl;
				}

			}
		} catch (Exception e) {
		}
		return ori;
	}

	public static void statusChange(String s) {
		if (!isEmpty(s)) {
			Intent myIntent = new Intent();
			myIntent.setAction(MainActivity.STATUS_CHANGE);
			myIntent.putExtra(MainActivity.STATUS_CHANGE, s);
			MainApplication.getContext().sendBroadcast(myIntent);
		}
	}

	public static String getString(int id) {
		try {
			return MainApplication.getContext().getResources().getString(id);
		} catch (Exception e) {
			return "";
		}
	}

	public static String getLan() {
		return Locale.getDefault().getLanguage();
	}
}
