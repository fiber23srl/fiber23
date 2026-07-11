package it.fiber23.cloud;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
    static final String PREFS = "fiber23";
    static final String KEY_TOKEN = "token";
    static final String KEY_API_URL = "api_url";
    static final String KEY_LAST_SYNC = "last_sync_ms";
    static final String KEY_LAST_MESSAGE = "last_sync_message";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, "");
        if (token == null || token.isEmpty()) {
            return Result.failure();
        }
        ApiClient api = new ApiClient(prefs.getString(KEY_API_URL, ApiClient.DEFAULT_API_URL), token);
        long lastSyncMs = prefs.getLong(KEY_LAST_SYNC, 0L);
        long newest = lastSyncMs;
        int uploaded = 0;
        try {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            Uri collection = MediaStore.Files.getContentUri("external");
            String[] projection = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED
            };
            String selection = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?) AND " + MediaStore.Files.FileColumns.DATE_ADDED + ">?";
            String[] args = {
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                    String.valueOf(lastSyncMs / 1000L)
            };
            try (Cursor cursor = resolver.query(collection, projection, selection, args, MediaStore.Files.FileColumns.DATE_ADDED + " ASC")) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
                    int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String name = cursor.getString(nameCol);
                        String mime = cursor.getString(mimeCol);
                        long dateMs = cursor.getLong(dateCol) * 1000L;
                        Uri uri = ContentUris.withAppendedId(collection, id);
                        api.upload(resolver, uri, name, mime, true);
                        uploaded++;
                        if (dateMs > newest) newest = dateMs;
                    }
                }
            }
            prefs.edit()
                    .putLong(KEY_LAST_SYNC, newest == 0L ? System.currentTimeMillis() : newest)
                    .putString(KEY_LAST_MESSAGE, uploaded + " foto/video sincronizzati")
                    .apply();
            return Result.success();
        } catch (Exception e) {
            prefs.edit().putString(KEY_LAST_MESSAGE, "Errore sync: " + e.getMessage()).apply();
            return Result.retry();
        }
    }
}
