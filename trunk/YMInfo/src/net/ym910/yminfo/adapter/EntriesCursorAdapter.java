/**
 * YMInfo
 *
 * Copyright (c) 2012-2013 WeiQiang ym910.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * 
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package net.ym910.yminfo.adapter;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import net.ym910.yminfo.R;
import net.ym910.yminfo.Constants;
import net.ym910.yminfo.MainApplication;
import net.ym910.yminfo.provider.FeedData;
import net.ym910.yminfo.provider.FeedDataContentProvider;
import net.ym910.yminfo.provider.FeedData.EntryColumns;
import net.ym910.yminfo.provider.FeedData.FeedColumns;
import net.ym910.yminfo.utils.MD5Util;
import net.ym910.yminfo.utils.PrefUtils;
import net.ym910.yminfo.utils.UiUtils;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.LruCache;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.ImageOptions;
import java.util.concurrent.Executors;

public class EntriesCursorAdapter extends ResourceCursorAdapter {
	private int titleColumnPosition;

	private int dateColumn;
	private int isReadColumn;
	private int favoriteColumn;
	private int idColumn;
	private int feedIconColumn;
	private int feedNameColumn;
	private int linkColumn;
	private int abstractColumn;
	private int contentColumn;

	private final Uri uri;
	private final boolean showFeedInfo;

	private final ConcurrentHashMap<Long, Boolean> markedAsRead = new ConcurrentHashMap<Long, Boolean>();
	private final ConcurrentHashMap<Long, Boolean> markedAsUnread = new ConcurrentHashMap<Long, Boolean>();
	private final ConcurrentHashMap<Long, Boolean> favorited = new ConcurrentHashMap<Long, Boolean>();
	private final ConcurrentHashMap<Long, Boolean> unfavorited = new ConcurrentHashMap<Long, Boolean>();
	public static LruCache<String, Bitmap> mMemoryCache = new LruCache<String, Bitmap>(
			1024 * 10) {
		protected int sizeOf(String key, Bitmap bitmap) {
			return bitmap.getByteCount() / 1024;
		}
	};
	public static ExecutorService newCachedThreadPool = Executors
			.newFixedThreadPool(3);

	private ImageOptions options;

	private boolean picDisable;

	public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor,
			boolean showFeedInfo) {
		super(context, R.layout.entry_list_item, cursor, 0);
		this.uri = uri;
		this.showFeedInfo = showFeedInfo;
		reinit(cursor);
		options = new ImageOptions();
		options.round = 15;
		options.fileCache = true;
		options.memCache = false;
		options.targetWidth = 80;
		options.ratio = 1.0f / 1.0f;
		options.fallback = AQuery.GONE;
		picDisable = PrefUtils.getBoolean(PrefUtils.DISABLE_PICTURES, false);
	}

	public static int diff_in_date(Date d1, Date d2) {

		if (null == d1 || null == d2) {
			return -1;
		}
		return (int) (d1.getTime() - d2.getTime()) / 86400000;
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		final TextView textView = (TextView) view
				.findViewById(android.R.id.text1);
		textView.setText(cursor.getString(titleColumnPosition));

		final TextView feedSiteNameView = (TextView) view
				.findViewById(android.R.id.text2);
		final TextView datetimetv = (TextView) view
				.findViewById(R.id.datetimetv);

		final ImageView starImgView = (ImageView) view
				.findViewById(android.R.id.icon);
		final long id = cursor.getLong(idColumn);
		view.setTag(cursor.getString(linkColumn));
		final boolean favorite = !unfavorited.contains(id)
				&& (cursor.getInt(favoriteColumn) == 1 || favorited
						.contains(id));
		final CheckBox viewCheckBox = (CheckBox) view
				.findViewById(android.R.id.checkbox);

		starImgView
				.setImageResource(favorite ? R.drawable.dimmed_rating_important
						: R.drawable.dimmed_rating_not_important);
		starImgView.setTag(favorite ? Constants.TRUE : Constants.FALSE);
		starImgView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean newFavorite = !Constants.TRUE.equals(view.getTag());
				if (newFavorite) {
					view.setTag(Constants.TRUE);
					starImgView
							.setImageResource(R.drawable.dimmed_rating_important);
					favorited.put(id, Constants.BOOLEAN);
					unfavorited.remove(id);
				} else {
					view.setTag(Constants.FALSE);
					starImgView
							.setImageResource(R.drawable.dimmed_rating_not_important);
					unfavorited.put(id, Constants.BOOLEAN);
					favorited.remove(id);
				}

				ContentValues values = new ContentValues();
				values.put(EntryColumns.IS_FAVORITE, newFavorite ? 1 : 0);

				ContentResolver cr = MainApplication.getContext()
						.getContentResolver();
				Uri entryUri = ContentUris.withAppendedId(uri, id);
				if (cr.update(entryUri, values, null, null) > 0) {
					FeedDataContentProvider.notifyAllFromEntryUri(entryUri,
							false); // Receive New Favorite on MainActivity
				}
			}
		});

		Date date = new Date(cursor.getLong(dateColumn));
		if (date != null) {
			datetimetv.setText(Constants.DATE_FORMAT.format(date));
			ImageView today = (ImageView) view.findViewById(R.id.today);
			if (diff_in_date(new Date(), date) == 0)
			{
				today.setVisibility(View.VISIBLE);
			}
		}
		String abstractContent = cursor.getString(abstractColumn);
		if (UiUtils.empty(abstractContent)) {
			abstractContent = cursor.getString(contentColumn);
		}
		if (!UiUtils.empty(abstractContent)) {
			TextView abstractTxt = (TextView) view
					.findViewById(R.id.txtabstract);
			abstractTxt.setText(UiUtils.removeTagFromText(abstractContent, 60));
		}

		ImageView representImg = (ImageView) view
				.findViewById(R.id.representImg);
		
		if (picDisable)
		{
			representImg.setVisibility(View.GONE);
		} else {
			AQuery aQuery = new AQuery(representImg);
			if (!UiUtils.empty(abstractContent)) {
				String img = UiUtils.findFirstImgLink(abstractContent);
				if (!UiUtils.empty(abstractContent)) {
					aQuery.image(img, options);
				} else {
					representImg.setVisibility(View.GONE);
				}
			} else {
				representImg.setVisibility(View.GONE);
			}
		}
		
		boolean siteImgSet = false;
		boolean siteTitleSet = false;
		if (showFeedInfo && feedIconColumn > -1) {
			byte[] iconBytes = cursor.getBlob(feedIconColumn);

			if (iconBytes != null && iconBytes.length > 0) {
				String md5Hex = MD5Util.md5Hex(iconBytes);
				Bitmap cachedBM = null;
				if (md5Hex != null) {
					cachedBM = mMemoryCache.get(md5Hex);
					if (cachedBM == null) {
						cachedBM = BitmapFactory.decodeByteArray(iconBytes, 0,
								iconBytes.length);
						if (cachedBM != null) {
							int bitmapSizeInDip = UiUtils.dpToPixel(18);
							if (cachedBM.getHeight() != bitmapSizeInDip) {
								cachedBM = Bitmap
										.createScaledBitmap(cachedBM,
												bitmapSizeInDip,
												bitmapSizeInDip, false);
							}
							mMemoryCache.put(md5Hex, cachedBM);
						}
					}
				}
				if (cachedBM != null) {
					BitmapDrawable bitmapDrawable = new BitmapDrawable(
							context.getResources(), cachedBM);
					feedSiteNameView.setCompoundDrawablesWithIntrinsicBounds(
							bitmapDrawable, null, null, null);
					siteImgSet = true;
				} else {
					feedSiteNameView.setCompoundDrawablesWithIntrinsicBounds(
							null, null, null, null);
				}
			} else {
				feedSiteNameView.setCompoundDrawablesWithIntrinsicBounds(null,
						null, null, null);
			}
		}

		if (showFeedInfo && feedIconColumn > -1) {
			String feedName = cursor.getString(feedNameColumn);
			if (feedName != null) {
				feedSiteNameView.setText(new StringBuilder(feedName));
				siteTitleSet = true;
			} else {
				feedSiteNameView.setText("");
			}
		}

		if (!siteTitleSet && !siteImgSet) {
			feedSiteNameView.setVisibility(View.GONE);
		}

		viewCheckBox.setOnCheckedChangeListener(null);
		if (markedAsUnread.contains(id)
				|| (cursor.isNull(isReadColumn) && !markedAsRead.contains(id))) {
			feedSiteNameView.setEnabled(true);
			viewCheckBox.setChecked(false);
		} else {
			feedSiteNameView.setEnabled(false);
			viewCheckBox.setChecked(true);
		}

		viewCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					markAsRead(id);
					feedSiteNameView.setEnabled(false);
				} else {
					markAsUnread(id);
					feedSiteNameView.setEnabled(true);
				}
			}
		});
	}

	private void markAsRead(final long id) {
		markedAsRead.put(id, Constants.BOOLEAN);
		markedAsUnread.remove(id);

		newCachedThreadPool.submit(new Runnable() {

			@Override
			public void run() {
				ContentResolver cr = MainApplication.getContext()
						.getContentResolver();
				Uri entryUri = ContentUris.withAppendedId(uri, id);
				if (cr.update(entryUri, FeedData.getReadContentValues(), null,
						null) > 0) {
					FeedDataContentProvider.notifyAllFromEntryUri(entryUri,
							false);
				}
			}
		});
	}

	private void markAsUnread(final long id) {
		markedAsUnread.put(id, Constants.BOOLEAN);
		markedAsRead.remove(id);

		newCachedThreadPool.submit(new Runnable() {

			@Override
			public void run() {
				ContentResolver cr = MainApplication.getContext()
						.getContentResolver();
				Uri entryUri = ContentUris.withAppendedId(uri, id);
				if (cr.update(entryUri, FeedData.getUnreadContentValues(),
						null, null) > 0) {
					FeedDataContentProvider.notifyAllFromEntryUri(entryUri,
							false);
				}
			}
		});
	}

	@Override
	public void changeCursor(Cursor cursor) {
		reinit(cursor);
		super.changeCursor(cursor);
	}

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		reinit(newCursor);
		return super.swapCursor(newCursor);
	}

	@Override
	public void notifyDataSetChanged() {
		reinit(null);
		super.notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetInvalidated() {
		reinit(null);
		super.notifyDataSetInvalidated();
	}

	private void reinit(Cursor cursor) {
		markedAsRead.clear();
		markedAsUnread.clear();
		favorited.clear();
		unfavorited.clear();

		if (cursor != null) {
			titleColumnPosition = cursor.getColumnIndex(EntryColumns.TITLE);
			dateColumn = cursor.getColumnIndex(EntryColumns.DATE);
			isReadColumn = cursor.getColumnIndex(EntryColumns.IS_READ);
			favoriteColumn = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
			idColumn = cursor.getColumnIndex(EntryColumns._ID);
			linkColumn = cursor.getColumnIndex(EntryColumns.LINK);
			abstractColumn = cursor.getColumnIndex(EntryColumns.ABSTRACT);
			contentColumn = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
			if (showFeedInfo) {
				feedIconColumn = cursor.getColumnIndex(FeedColumns.ICON);
				feedNameColumn = cursor.getColumnIndex(FeedColumns.NAME);
			}
		}
	}
}
