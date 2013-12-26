package net.ym910.yminfo.activity;

import net.ym910.yminfo.R;
import net.ym910.yminfo.utils.PrefUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewPropertyAnimator;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SplashScreen extends Activity{

	// Splash screen timer
	public static int SPLASH_TIME_OUT = 2500;

	private ImageView iv;

	private TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		iv = (ImageView)findViewById(R.id.logoimg);
		tv = (TextView)findViewById(R.id.logotxt);
		
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				iv.clearAnimation();
				
				Intent i = new Intent(SplashScreen.this, MainActivity.class);
				startActivity(i);
				overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
				finish();
			}
		}, SPLASH_TIME_OUT);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				ViewPropertyAnimator animate = iv.animate();
				animate.setDuration(SPLASH_TIME_OUT / 2);
				animate.translationXBy(0).translationYBy(-130).start();
				
				tv.animate().alpha(1.0f).setDuration(SPLASH_TIME_OUT / 2).start();
			}
		}, SPLASH_TIME_OUT / 2);
		
		checkNetWorkAndInitial();
	}

	protected void checkNetWorkAndInitial() {
		if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true))
		{
			try {
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
				if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED && networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
					PrefUtils.putBoolean(PrefUtils.DISABLE_PICTURES, true);
					Toast.makeText(this, R.string.no_wifi_difault_false,
							Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
			}
		}
	}
}
