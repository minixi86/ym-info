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

package net.ym910.yminfo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import net.ym910.yminfo.R;
import net.ym910.yminfo.activity.MainActivity;
import net.ym910.yminfo.utils.PrefUtils;

public class WidgetProvider extends AppWidgetProvider {
    public static final int STANDARD_BACKGROUND = 0x7c000000;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent svcIntent = new Intent(context, WidgetService.class);

            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            svcIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, PrefUtils.getInteger(appWidgetId + ".fontsize", 0));
            svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.widget);
            widget.setOnClickPendingIntent(R.id.feed_icon, PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0));
            widget.setPendingIntentTemplate(R.id.feedsListView, PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW), 0));

            widget.setRemoteAdapter(R.id.feedsListView, svcIntent);
            widget.setInt(R.id.feedsListView, "setBackgroundColor", PrefUtils.getInteger(appWidgetId + ".background", STANDARD_BACKGROUND));

            appWidgetManager.updateAppWidget(appWidgetId, widget);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
