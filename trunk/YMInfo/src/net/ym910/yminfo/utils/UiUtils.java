package net.ym910.yminfo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ym910.yminfo.MainApplication;
import android.app.Activity;
import android.text.Html;
import android.util.TypedValue;

public class UiUtils {
	
	private static Pattern imgLinkPat = Pattern.compile("\"(http://.*?\\.(jpg|png).*?)\"");

	static public int dpToPixel(int dp) {
		return (int) TypedValue
				.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
						MainApplication.getContext().getResources()
								.getDisplayMetrics());
	}

	public static String removeTagFromText(String content, int limit) {
		String replaceAll = content.replaceAll("<.*?>", "").replaceAll("/r+|/n+|(\\s\\S)+", "");
		return Html.fromHtml(replaceAll.substring(0, replaceAll.length() > limit ? limit : replaceAll.length())).toString();
	}
	
	public static boolean empty(String em)
	{
		return em == null || em.trim().equals("");
	}

	public static String findFirstImgLink(String abstractContent) {
		Matcher matcher = imgLinkPat.matcher(abstractContent);
		return matcher.find() ? matcher.group(1) : null;
	}
}
