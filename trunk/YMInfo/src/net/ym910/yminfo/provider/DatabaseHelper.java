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

package net.ym910.yminfo.provider;

import java.io.File;
import java.util.Locale;

import net.ym910.yminfo.MainApplication;
import net.ym910.yminfo.R;
import net.ym910.yminfo.activity.MainActivity;
import net.ym910.yminfo.adapter.EntriesCursorAdapter;
import net.ym910.yminfo.parser.OPML;
import net.ym910.yminfo.provider.FeedData.EntryColumns;
import net.ym910.yminfo.provider.FeedData.FeedColumns;
import net.ym910.yminfo.provider.FeedData.FilterColumns;
import net.ym910.yminfo.provider.FeedData.TaskColumns;
import net.ym910.yminfo.service.FetcherService;
import net.ym910.yminfo.utils.PrefUtils;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "YMInfo.db";
    private static final int DATABASE_VERSION = 4;

    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD = " ADD ";

    private final Handler mHandler;

    public DatabaseHelper(Handler handler, Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mHandler = handler;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTable(FeedColumns.TABLE_NAME, FeedColumns.COLUMNS));
        database.execSQL(createTable(FilterColumns.TABLE_NAME, FilterColumns.COLUMNS));
        database.execSQL(createTable(EntryColumns.TABLE_NAME, EntryColumns.COLUMNS));
        database.execSQL(createTable(TaskColumns.TABLE_NAME, TaskColumns.COLUMNS));

        // Check if we need to import the backup
        File backupFile = new File(OPML.BACKUP_OPML);
        final boolean hasBackup = backupFile.exists();
        mHandler.post(new Runnable() { // In order to it after the database is created
            @Override
            public void run() {
            	EntriesCursorAdapter.newCachedThreadPool.submit(new Runnable() {
					public void run() {
                        try {
                            if (hasBackup) {
                                // Perform an automated import of the backup
                                OPML.importFromFile(OPML.BACKUP_OPML);
                            } else {
                                // No database and no backup, automatically add the default feeds
								int defaultFeeds = getDefaultFeeds();
								if (defaultFeeds != -1)
								{
									Context context = MainApplication.getContext();
									OPML.importFromFile(context.getResources().openRawResource(defaultFeeds));
									
									if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
										context.startService(new Intent(context, FetcherService.class)
										.setAction(FetcherService.ACTION_REFRESH_FEEDS));
									}
								}
                            }
                        } catch (Exception ignored) {
                        }
					}

					private int getDefaultFeeds() {
						try {
							return (Integer) R.raw.class.getDeclaredField("default_feeds_" + Locale.getDefault().getLanguage()).get(R.raw.class);
						} catch (Exception e) {
							return -1;
						}
					}
				});
            }
        });
    }

    public void exportToOPML() {
        try {
            OPML.exportToFile(OPML.BACKUP_OPML);
        } catch (Exception ignored) {
        }
    }

    private String createTable(String tableName, String[][] columns) {
        if (tableName == null || columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
        } else {
            StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");

            stringBuilder.append(tableName);
            stringBuilder.append(" (");
            for (int n = 0, i = columns.length; n < i; n++) {
                if (n > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(columns[n][0]).append(' ').append(columns[n][1]);
            }
            return stringBuilder.append(");").toString();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            executeCatchedSQL(
                    database,
                    ALTER_TABLE + FeedColumns.TABLE_NAME + ADD + FeedColumns.REAL_LAST_UPDATE + ' ' + FeedData.TYPE_DATE_TIME);
        }
        if (oldVersion < 3) {
            executeCatchedSQL(
                    database,
                    ALTER_TABLE + FeedColumns.TABLE_NAME + ADD + FeedColumns.RETRIEVE_FULLTEXT + ' ' + FeedData.TYPE_BOOLEAN);
        }
        if (oldVersion < 4) {
            executeCatchedSQL(database, createTable(TaskColumns.TABLE_NAME, TaskColumns.COLUMNS));
            // Remove old yminfo directory (now useless)
            try {
                deleteFileOrDir(new File(Environment.getExternalStorageDirectory() + "/YMInfo/"));
            } catch (Exception ignored) {
            }
        }
    }

    private void executeCatchedSQL(SQLiteDatabase database, String query) {
        try {
            database.execSQL(query);
        } catch (Exception ignored) {
        }
    }

    private void deleteFileOrDir(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteFileOrDir(child);

        fileOrDirectory.delete();
    }
}
