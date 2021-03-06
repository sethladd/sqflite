package com.tekartik.sqflite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static com.tekartik.sqflite.Constant.METHOD_CLOSE_DATABASE;
import static com.tekartik.sqflite.Constant.METHOD_DEBUG_MODE;
import static com.tekartik.sqflite.Constant.METHOD_EXECUTE;
import static com.tekartik.sqflite.Constant.METHOD_INSERT;
import static com.tekartik.sqflite.Constant.METHOD_OPEN_DATABASE;
import static com.tekartik.sqflite.Constant.METHOD_QUERY;
import static com.tekartik.sqflite.Constant.METHOD_UPDATE;
import static com.tekartik.sqflite.Constant.PARAM_ID;
import static com.tekartik.sqflite.Constant.PARAM_PATH;
import static com.tekartik.sqflite.Constant.PARAM_SQL;
import static com.tekartik.sqflite.Constant.PARAM_SQL_ARGUMENTS;

/**
 * SqflitePlugin Android implementation
 */
public class SqflitePlugin implements MethodCallHandler {

    private MethodChannel channel;

    static private boolean LOGV = false;

    private static class Database extends SQLiteOpenHelper {
        String path;
        private Database(Context context, String path) {
            super(context, path, null);
            this.path = path;
        }
    }

    static private String TAG = "Sqflite";

    private Context context;

    private final Object mapLocker = new Object();
    private int databaseId = 0; // incremental database id
    private Map<Integer, Database> databaseMap = new HashMap<>();

    private SqflitePlugin(Context context, MethodChannel channel) {
        this.context = context;
        this.channel = channel;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.tekartik.sqflite");
        channel.setMethodCallHandler(new SqflitePlugin(registrar.activity().getApplicationContext(), channel));
    }

    private Database getDatabase(int databaseId) {
        return databaseMap.get(databaseId);
    }

    private Database getDatabaseOrError(MethodCall call, Result result) {
        int databaseId = call.argument(PARAM_ID);
        Database database = getDatabase(databaseId);

        if (database != null) {
            return database;
        } else {
            result.error(call.method, "database " + databaseId + " not found", null);
            return null;
        }
    }

    private void errorException(MethodCall call, Result result, Exception exception) {
        result.error(call.method, "Native exception", exception);
    }

    String[] getSqlArguments(List<Object> rawArguments) {
        List<String> stringArguments = new ArrayList<>();
        if (rawArguments != null) {
            for (Object rawArgument : rawArguments) {
                if (rawArgument == null) {
                    stringArguments.add(null);
                } else {
                    stringArguments.add(rawArgument.toString());
                }
            }
        }
        return stringArguments.toArray(new String[0]);
    }

    private void onQueryCall(MethodCall call, Result result) {
        Database database = getDatabaseOrError(call, result);
        if (database == null) {
            return;
        }
        String sql = call.argument(PARAM_SQL);
        List<Object> arguments = call.argument(PARAM_SQL_ARGUMENTS);

        List<Map<String, Object>> results = new ArrayList<>();
        if (LOGV) {
            Log.d(TAG, sql + ((arguments == null || arguments.isEmpty()) ? "" : (" " + arguments)));
        }
        Cursor cursor = database.getReadableDatabase().rawQuery(sql, getSqlArguments(arguments));
        try {
            while (cursor.moveToNext()) {
                //ContentValues cv = new ContentValues();
                //DatabaseUtils.cursorRowToContentValues(cursor, cv);
                ContentValues cv = cursorRowToContentValues(cursor);
                Map<String, Object> map = contentValuesToMap(cv);
                if (LOGV) {
                    Log.d(TAG, map.toString());
                }
                results.add(map);
            }
            result.success(results);
        } catch (Exception e) {
            errorException(call, result, e);
        } finally {
            cursor.close();
        }
    }

    private void onInsertCall(MethodCall call, Result result) {
        Database database = executeOrError(call, result);
        if (database == null) {
            return;
        }
        String sql = "SELECT last_insert_rowid()";
        //if (LOGV) {
        //    Log.d(TAG, sql);
        //}
        Cursor cursor = database.getWritableDatabase().rawQuery(sql, null);
        try {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                if (LOGV) {
                    Log.d(TAG, "inserted " + id);
                }
                result.success(id);
                return;
            } else {
                Log.e(TAG, "Fail to read inserted it");
            }
            result.success(null);
        } catch (Exception e) {
            errorException(call, result, e);
        } finally {
            cursor.close();
        }
    }

    private Database executeOrError(MethodCall call, Result result) {
        Database database = getDatabaseOrError(call, result);
        if (database == null) {
            return null;
        }
        String sql = call.argument(PARAM_SQL);
        List<Object> arguments = call.argument(PARAM_SQL_ARGUMENTS);
        if (LOGV) {
            Log.d(TAG, sql + ((arguments == null || arguments.isEmpty()) ? "" : (" " + arguments)));
        }
        try {
            database.getWritableDatabase().execSQL(sql, getSqlArguments(arguments));
        } catch (SQLException exception) {
            result.error(call.method, sql, exception);
        }
        return database;
    }


    private void onExecuteCall(MethodCall call, Result result) {
        Database database = executeOrError(call, result);
        if (database == null) {
            return;
        }
        result.success(null);
    }

    private void onUpdateCall(MethodCall call, Result result) {
        Database database = executeOrError(call, result);
        if (database == null) {
            return;
        }
        SQLiteDatabase db = database.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT changes()", null);
        try {
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                final int changed = cursor.getInt(0);
                if (LOGV) {
                    Log.d(TAG, "changed " + changed);
                }
                result.success(changed);
                return;
            } else {
                Log.e(TAG, "fail to read changes for Update/Delete");
            }
            result.success(null);
        } catch (Exception e) {
            errorException(call, result, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void onOpenDatabaseCall(MethodCall call, Result result) {
        String path = call.argument(PARAM_PATH);
        //int version = call.argument(PARAM_VERSION);
        File file = new File(path);
        File directory = new File(file.getParent());
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (LOGV) {
            Log.d(TAG, "opening " + path);
        }
        Database database = new Database(context, path);
        // force opening
        database.getReadableDatabase();
        //SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(path, null, 0);
        int databaseId;
        synchronized (mapLocker) {
            databaseId = ++this.databaseId;
            databaseMap.put(databaseId, database);
        }
        result.success(databaseId);
    }

    private void onCloseDatabaseCall(MethodCall call, Result result) {
        Database database = getDatabaseOrError(call, result);
        if (database == null) {
            return;
        }
        if (LOGV) {
            Log.d(TAG, "closing " + database.path);
        }
        database.close();
        synchronized (mapLocker) {
            databaseMap.remove(databaseId);
        }
        result.success(null);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            // quick testing
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;

            case METHOD_DEBUG_MODE: {
                LOGV = true;
                result.success(null);
                break;
            }
            case METHOD_CLOSE_DATABASE: {
                onCloseDatabaseCall(call, result);
                break;
            }
            case METHOD_QUERY: {
                onQueryCall(call, result);
                break;
            }
            case METHOD_INSERT: {
                onInsertCall(call, result);
                break;
            }
            case METHOD_UPDATE: {
                onUpdateCall(call, result);
                break;
            }
            case METHOD_EXECUTE: {
                onExecuteCall(call, result);

                break;
            }
            case METHOD_OPEN_DATABASE: {
                onOpenDatabaseCall(call, result);
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    private static ContentValues cursorRowToContentValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(columns[i]);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columns[i], cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(columns[i], cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columns[i], cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(columns[i], cursor.getBlob(i));
                    break;
            }
        }
        return values;
    }

    private Map<String, Object> contentValuesToMap(ContentValues cv) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : cv.valueSet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /*
    private ContentValues contentValuesFromMap(Map<String, Object> values) {
        ContentValues contentValues = new ContentValues();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (value instanceof String) {
                contentValues.put(key, (String) value);
            } else if (value instanceof Integer) {
                contentValues.put(key, (Integer) value);
            } else if (value instanceof Long) {
                contentValues.put(key, (Long) value);
            } else if (value == null) {
                contentValues.putNull(key);
            } else {
                throw new IllegalArgumentException("object of type " + value.getClass() + " value " + value + " not supported");
            }
        }
        return contentValues;
    }
    */
}
