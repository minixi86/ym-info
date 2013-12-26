/**
 * YMInfo
 *
 * Copyright (c) 2012-2013 WeiQiang ym910.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.ym910.yminfo.activity;

import net.ym910.yminfo.Constants;
import net.ym910.yminfo.MainApplication;
import net.ym910.yminfo.R;
import net.ym910.yminfo.adapter.DrawerAdapter;
import net.ym910.yminfo.bean.RefreshBean;
import net.ym910.yminfo.fragment.EntriesListFragment;
import net.ym910.yminfo.notify.MenuInfoer;
import net.ym910.yminfo.provider.FeedData;
import net.ym910.yminfo.service.FetcherService;
import net.ym910.yminfo.service.RefreshService;
import net.ym910.yminfo.utils.GeneralUtil;
import net.ym910.yminfo.utils.PrefUtils;
import net.ym910.yminfo.utils.UiUtils;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.CycleInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.util.AQUtility;
import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;
import com.umeng.socialize.controller.RequestType;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.media.UMImage;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;

public class MainActivity extends ProgressActivity  implements
LoaderManager.LoaderCallbacks<Cursor> {

	public static final String REFRESH_END = "yiming.info.refresh_end";

	public static final String STATUS_CHANGE = "yiming.info.statusChange";

	public static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";

	private final SharedPreferences.OnSharedPreferenceChangeListener isRefreshingListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (PrefUtils.IS_REFRESHING.equals(key)) {
				int isVis = PrefUtils.getBoolean(PrefUtils.IS_REFRESHING,
						false) ? View.VISIBLE : View.GONE;
				getProgressBar().setVisibility(isVis);
				getStatusBar().setVisibility(isVis);
			}
		}
	};

	private EntriesListFragment mEntriesFragment;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private DrawerAdapter mDrawerAdapter;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mTitle;
	private BitmapDrawable mIcon;
	private int mCurrentDrawerPos;
	private static final int LOADER_ID = 0;
	private RefreshBean refreshBean = new RefreshBean();

	private RefreshEndReceiver refreshEndReceiver;

	private TextView statusBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		refreshBean.setRefreshActionView((ImageView) getLayoutInflater()
				.inflate(R.layout.refresh_infinite, null));
		mEntriesFragment = (EntriesListFragment) getFragmentManager()
				.findFragmentById(R.id.fragment);
		mEntriesFragment.setRefreshActionView(refreshBean);

		mTitle = getTitle();

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				selectDrawerItem(position);
				mDrawerLayout.closeDrawer(mDrawerList);
			}
		});
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				refreshTitleAndIcon();
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(R.string.app_name);
				getActionBar().setIcon(R.drawable.icon);
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState != null) {
			mCurrentDrawerPos = savedInstanceState
					.getInt(STATE_CURRENT_DRAWER_POS);
		}
		getLoaderManager().initLoader(LOADER_ID, null, this);
		if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
			// starts the service independent to this activity
			startService(new Intent(this, RefreshService.class));
		} else {
			stopService(new Intent(this, RefreshService.class));
		}
		
		if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
			if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
				startService(new Intent(MainActivity.this, FetcherService.class)
						.setAction(FetcherService.ACTION_REFRESH_FEEDS));
			}
		}
		statusBar = (TextView)findViewById(R.id.statusbar);
		initMenus();
	}
	
	public TextView getStatusBar() {
		statusBar.setText(getResources().getString(R.string.loading));
		return statusBar;
	}

	private void refreshTitleAndIcon() {
		getActionBar().setTitle(mTitle);
		switch (mCurrentDrawerPos) {
		case 0:
			getActionBar().setTitle(R.string.all);
			getActionBar().setIcon(R.drawable.ic_statusbar_rss);
			break;
		case 1:
			getActionBar().setTitle(R.string.favorites);
			getActionBar().setIcon(R.drawable.dimmed_rating_important);
			break;
		default:
			getActionBar().setTitle(mTitle);
			if (mIcon != null) {
				getActionBar().setIcon(mIcon);
			}
			break;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		int isVis = PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false) ? View.VISIBLE
				: View.GONE;
		getProgressBar().setVisibility(isVis);
		getStatusBar().setVisibility(isVis);
		PrefUtils.registerOnPrefChangeListener(isRefreshingListener);

		if (Constants.NOTIF_MGR != null) {
			Constants.NOTIF_MGR.cancel(0);
		}
		// UMENG
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		PrefUtils.unregisterOnPrefChangeListener(isRefreshingListener);
		super.onPause();
		// UMENG
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mEntriesFragment.setMenuVisibility(false);
		} else {
			mEntriesFragment.setMenuVisibility(true);
		}
		return super.onCreateOptionsMenu(menu);
	}

	private class RefreshEndReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			if (MainActivity.REFRESH_END.equals(intent.getAction()))
			{
				refreshBean.clearRefresh();
			} else if (MainActivity.STATUS_CHANGE.equals(intent.getAction()))
			{
				Bundle extras = intent.getExtras();
				if (extras != null)
				{
					Object status = extras.get(MainActivity.STATUS_CHANGE);
					if (status != null)
					{
						statusBar.setText(status.toString());
					}
				}
			}
		}
	}

	@Override
	protected void onStart() {
		refreshEndReceiver = new RefreshEndReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(REFRESH_END);
		filter.addAction(STATUS_CHANGE);
		registerReceiver(refreshEndReceiver, filter);
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(refreshEndReceiver);
		AQUtility.cleanCacheAsync(this);
		super.onDestroy();
	}

	private long exitTime = 0;

	private FeedbackAgent agent;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {

			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), R.string.logoutpromot,
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void selectDrawerItem(int position) {
		mCurrentDrawerPos = position;
		mIcon = null;

		Uri newUri;
		boolean showFeedInfo = true;

		switch (position) {
		case 0:
			newUri = FeedData.EntryColumns.CONTENT_URI;
			break;
		case 1:
			newUri = FeedData.EntryColumns.FAVORITES_CONTENT_URI;
			break;
		default:
			long feedOrGroupId = mDrawerAdapter.getItemId(position);
			if (mDrawerAdapter.isItemAGroup(position)) {
				newUri = FeedData.EntryColumns
						.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
			} else {
				byte[] iconBytes = mDrawerAdapter.getItemIcon(position);
				if (iconBytes != null && iconBytes.length > 0) {
					int bitmapSizeInDip = UiUtils.dpToPixel(24);
					Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0,
							iconBytes.length);
					if (bitmap != null) {
						if (bitmap.getHeight() != bitmapSizeInDip) {
							bitmap = Bitmap.createScaledBitmap(bitmap,
									bitmapSizeInDip, bitmapSizeInDip, false);
						}

						mIcon = new BitmapDrawable(getResources(), bitmap);
					}
				}

				newUri = FeedData.EntryColumns
						.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
				showFeedInfo = false;
			}
			mTitle = mDrawerAdapter.getItemName(position);
			break;
		}

		if (!newUri.equals(mEntriesFragment.getUri())) {
			mEntriesFragment.setData(newUri, showFeedInfo);
		}

		mDrawerList.setItemChecked(position, true);

		// First open => we open the drawer for you
		if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
			PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false);
			mDrawerLayout.postDelayed(new Runnable() {
				@Override
				public void run() {
					
					mDrawerLayout.openDrawer(mDrawerList);
				}
			}, 500);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	// ################################################################################################

	private void initMenus() {

		editFeed = (ImageButton) findViewById(R.id.picture_tiankong);

		refreshAll = (ImageButton) findViewById(R.id.music);

		setting = (ImageButton) findViewById(R.id.place);

		feedback = (ImageButton) findViewById(R.id.sleep);

		checkUpdate = (ImageButton) findViewById(R.id.thought);

		shareWithOthers = (ImageButton) findViewById(R.id.with);

		plusImage = (ImageView) findViewById(R.id.myplus);

		// UMENG
		agent = new FeedbackAgent(this);
		agent.sync();
		UmengUpdateAgent.setDeltaUpdate(true);
		UmengUpdateAgent.setUpdateOnlyWifi(true);
		UmengUpdateAgent.update(this);
		mController.setShareContent(getResources().getString(R.string.promote));
		mController.setShareMedia(new UMImage(this, R.drawable.icon));
	}

	// UMENG
	final UMSocialService mController = UMServiceFactory.getUMSocialService(
			"com.umeng.share", RequestType.SOCIAL);

	ImageButton editFeed, refreshAll, setting, feedback, checkUpdate,
			shareWithOthers;
	ImageView plusImage;

	boolean sign = false;

	static final int anicatinoTime = 100;

	private RotateAnimation showRotateAnimation;
	
	public void myanimation(View view) {
		if (showRotateAnimation == null || showRotateAnimation.hasEnded())
		{
			showRotateAnimation = showRotateAnimation(sign);
			if (!sign) {
				sign = true;
				outAnimation();
			} else {
				sign = false;
				inAnimation();
			}
		}
	}

	// 放出
	public void outAnimation() {
		{
			ViewPropertyAnimator animate = editFeed.animate();
			animate.setInterpolator(new OvershootInterpolator());
			animate.setDuration(anicatinoTime * 1 + 240);
			animate.translationXBy(-200).translationYBy(0).start();
		}

		{
			ViewPropertyAnimator animate = refreshAll.animate();
			animate.setInterpolator(new OvershootInterpolator());
			animate.setDuration(anicatinoTime * 1 + 180);
			animate.setStartDelay(20);
			animate.translationXBy(-190).translationYBy(-60).start();
		}

		{
			ViewPropertyAnimator animate = setting.animate();
			animate.setInterpolator(new OvershootInterpolator());
			animate.setDuration(anicatinoTime * 1 + 120);
			animate.setStartDelay(40);
			animate.translationXBy(-170).translationYBy(-120).start();
		}

		{
			ViewPropertyAnimator animate = feedback.animate();
			animate.setInterpolator(new OvershootInterpolator());
			animate.setDuration(anicatinoTime * 1 + 80);
			animate.setStartDelay(60);
			animate.translationXBy(-120).translationYBy(-165).start();
		}

		{
			ViewPropertyAnimator animate = checkUpdate.animate();
			animate.setInterpolator(new OvershootInterpolator());
			animate.setDuration(anicatinoTime * 1 + 40);
			animate.setStartDelay(80);
			animate.translationXBy(-60).translationYBy(-190).start();
		}

		{
			ViewPropertyAnimator animate = shareWithOthers.animate();
			animate.setInterpolator(new OvershootInterpolator());
			animate.setDuration(anicatinoTime * 1);
			animate.setStartDelay(100);
			animate.translationXBy(0).translationYBy(-200).start();
		}
	}

	// 收回
	public void inAnimation() {
		{
			ViewPropertyAnimator animate = editFeed.animate();
			animate.setDuration(anicatinoTime * 1);
			animate.translationXBy(200).translationYBy(0).start();
		}

		{
			ViewPropertyAnimator animate = refreshAll.animate();
			animate.setDuration(anicatinoTime * 1);
			animate.setStartDelay(30);
			animate.translationXBy(190).translationYBy(60).start();
		}

		{
			ViewPropertyAnimator animate = setting.animate();
			animate.setDuration(anicatinoTime * 1);
			animate.setStartDelay(60);
			animate.translationXBy(170).translationYBy(120).start();
		}

		{
			ViewPropertyAnimator animate = feedback.animate();
			animate.setDuration(anicatinoTime * 1);
			animate.setStartDelay(90);
			animate.translationXBy(120).translationYBy(165).start();
		}

		{
			ViewPropertyAnimator animate = checkUpdate.animate();
			animate.setDuration(anicatinoTime * 1);
			animate.setStartDelay(120);
			animate.translationXBy(60).translationYBy(190).start();
		}

		{
			ViewPropertyAnimator animate = shareWithOthers.animate();
			animate.setDuration(anicatinoTime * 1);
			animate.setStartDelay(150);
			animate.translationXBy(0).translationYBy(200).start();
		}
	}
	
	public void shakeAnimation(View v, int CycleTimes, final Runnable run) {
		Animation translateAnimation = new TranslateAnimation(0, 0, 0, 5);
		translateAnimation.setInterpolator(new CycleInterpolator(CycleTimes));
		translateAnimation.setDuration(1000);
		translateAnimation.setAnimationListener(new AnimationListener() {
			public void onAnimationStart(Animation animation) {
			}
			public void onAnimationRepeat(Animation animation) {
			}
			public void onAnimationEnd(Animation arg0) {
				run.run();
			}
		});
		v.startAnimation(translateAnimation);
	}

	public void itemClick(View itemid) {
		if (itemid == editFeed) {
			shakeAnimation(itemid, 3, new Runnable() {
				public void run() {
					startActivity(new Intent(MainActivity.this, FeedsListActivity.class));
					overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
				}
			});
		} else if (itemid == refreshAll) {
			MenuInfoer.instance().refreshAll(this);
			if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
				shakeAnimation(itemid, 3, new Runnable() {
					public void run() {
						MainApplication
						.getContext()
						.startService(
								new Intent(MainApplication.getContext(),
										FetcherService.class)
										.setAction(FetcherService.ACTION_REFRESH_FEEDS));
					}
				});
			}
		} else if (itemid == setting) {
			shakeAnimation(itemid, 3, new Runnable() {
				public void run() {
					startActivity(new Intent(MainActivity.this, GeneralPrefsActivity.class));
					overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
				}
			});
		} else if (itemid == feedback) {
			shakeAnimation(itemid, 3, new Runnable() {
				public void run() {
					agent.startFeedbackActivity();					
				}
			});
		} else if (itemid == checkUpdate) {
			shakeAnimation(itemid, 3, new Runnable() {
				public void run() {
					UmengUpdateListener a = new UmengUpdateListener() {

						@Override
						public void onUpdateReturned(int updateStatus,
								UpdateResponse updateInfo) {

							switch (updateStatus) {
							case 0: // has update
								UmengUpdateAgent.showUpdateDialog(MainActivity.this,
										updateInfo);
								break;
							case 1: // has no update
								Toast.makeText(MainActivity.this, R.string.noupdate,
										Toast.LENGTH_SHORT).show();
								break;
							case 2: // none wifi
								Toast.makeText(MainActivity.this, R.string.onlyupdatewifi, Toast.LENGTH_SHORT)
										.show();
								break;
							case 3: // time out
								Toast.makeText(MainActivity.this, R.string.updatetimeout,
										Toast.LENGTH_SHORT).show();
								break;
							}
							UmengUpdateAgent.setUpdateListener(null);
						}
					};
					UmengUpdateAgent.setUpdateListener(a);
					UmengUpdateAgent.forceUpdate(MainActivity.this);			
				}
			});
		} else if (itemid == shareWithOthers) {
			shakeAnimation(itemid, 3, new Runnable() {
				public void run() {
					if ("zh".equals(GeneralUtil.getLan()))
					{
						mController.openShare(MainActivity.this, false);
					} else { 
						startActivity(Intent.createChooser(
								new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_favorites_title))
								.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_with_other)).setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)));
					}
				}
			});
		}
	}

	public RotateAnimation showRotateAnimation(boolean sign) {

		final float centerX = plusImage.getWidth() / 2.0f;
		final float centerY = plusImage.getHeight() / 2.0f;
		RotateAnimation rotateAnimation = null;
		if (!sign) {
			rotateAnimation = new RotateAnimation(0, 405, centerX, centerY);
		} else {
			rotateAnimation = new RotateAnimation(405, 0, centerX, centerY);
		}
		rotateAnimation.setDuration(anicatinoTime + 220);
		rotateAnimation.setFillAfter(true);
		plusImage.startAnimation(rotateAnimation);
		return rotateAnimation;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		CursorLoader cursorLoader = new CursorLoader(this,
				FeedData.FeedColumns.GROUPED_FEEDS_CONTENT_URI, new String[] {
						FeedData.FeedColumns._ID,
						FeedData.FeedColumns.URL,
						FeedData.FeedColumns.NAME,
						FeedData.FeedColumns.IS_GROUP,
						FeedData.FeedColumns.GROUP_ID,
						FeedData.FeedColumns.ICON,
						FeedData.FeedColumns.LAST_UPDATE,
						FeedData.FeedColumns.ERROR,
						"(SELECT COUNT(*) FROM "
								+ FeedData.EntryColumns.TABLE_NAME + " WHERE "
								+ FeedData.EntryColumns.IS_READ
								+ " IS NULL AND "
								+ FeedData.EntryColumns.FEED_ID + "="
								+ FeedData.FeedColumns.TABLE_NAME + "."
								+ FeedData.FeedColumns._ID + ")",
						"(SELECT COUNT(*) FROM "
								+ FeedData.EntryColumns.TABLE_NAME + " WHERE "
								+ FeedData.EntryColumns.IS_READ + " IS NULL)",
						"(SELECT COUNT(*) FROM "
								+ FeedData.EntryColumns.TABLE_NAME + " WHERE "
								+ FeedData.EntryColumns.IS_FAVORITE
								+ Constants.DB_IS_TRUE + ")" }, null, null,
				null);
		cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

		
		if (mDrawerAdapter != null) {
			mDrawerAdapter.setCursor(cursor);
		} else {
			mDrawerAdapter = new DrawerAdapter(this, cursor);
			mDrawerList.setAdapter(mDrawerAdapter);
			
			// We don't have any menu yet, we need to display it
			mDrawerList.post(new Runnable() {
				@Override
				public void run() {
					selectDrawerItem(mCurrentDrawerPos);
					refreshTitleAndIcon();
				}
			});
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}
}
