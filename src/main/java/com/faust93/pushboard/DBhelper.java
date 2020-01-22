package com.faust93.pushboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by faust on 16.04.14.
 */
public class DBhelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "pbMessages";

    private static final String TABLE_MESSAGES = "messages";

    private static final String KEY_ID = "_id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_MESSAGE = "message";

    public DBhelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MSG_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_TYPE + " INTEGER,"
                + KEY_SUBJECT + " TEXT," + KEY_MESSAGE + " TEXT" + ")";
        db.execSQL(CREATE_MSG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    public List<pbMessage> getAllMessages() {
        List<pbMessage> msgList = new ArrayList<pbMessage>();
        String query = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()) {
            do {
                pbMessage msg = new pbMessage();
                msg.setID(Integer.parseInt(cursor.getString(0)));
                msg.setType(Integer.parseInt(cursor.getString(1)));
                msg.setSubject(cursor.getString(2));
                msg.setMessage(cursor.getString(3));
                msgList.add(msg);
            } while(cursor.moveToNext());
        }
        return msgList;
    }

    public List<pbMessage> getAllMessagesWithSubj(String subj) {
        List<pbMessage> msgList = new ArrayList<pbMessage>();
        String query = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_SUBJECT + "=" + "\"" + subj + "\"";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()) {
            do {
                pbMessage msg = new pbMessage();
                msg.setID(Integer.parseInt(cursor.getString(0)));
                msg.setType(Integer.parseInt(cursor.getString(1)));
                msg.setSubject(cursor.getString(2));
                msg.setMessage(cursor.getString(3));
                msgList.add(msg);
            } while(cursor.moveToNext());
        }
        return msgList;
    }

    public pbMessage getMessage(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, new String[] { KEY_ID, KEY_TYPE, KEY_SUBJECT, KEY_MESSAGE },
                KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null);
        if(cursor != null)
            cursor.moveToFirst();
        pbMessage msg = new pbMessage(Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(1)), cursor.getString(2), cursor.getString(3));
        return msg;
    }

    public long addMessage(pbMessage msg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_TYPE, msg.getType());
        cv.put(KEY_SUBJECT, msg.getSubject());
        cv.put(KEY_MESSAGE, msg.getMessage());
        long id = db.insert(TABLE_MESSAGES, null, cv);
        db.close();
        return id;
    }

    public int deleteMessagesWithSubj(String subj) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES, KEY_SUBJECT + "=?", new String[] { subj });
    }

    public int deleteAllMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES, null, null);
    }

    public int deleteMessage(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES, KEY_ID + "=?", new String[] { String.valueOf(id) });
    }
}
