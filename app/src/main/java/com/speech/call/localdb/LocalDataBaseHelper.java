/*
 * Copyright 2019 ~ https://github.com/braver-tool
 */

package com.speech.call.localdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.speech.call.ContactModel;
import com.speech.call.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import static com.speech.call.AppUtils.IS_CONTACT_SORTED;

public class LocalDataBaseHelper extends SQLiteOpenHelper {
    public static final String CONTACT_NAME = "ContactName";
    public static final String CONTACT_NUMBER = "ContactNumber";
    public static final String CONTACT_ID = "ContactId";
    public static final String DATABASE_NAME = "ContactSqlite.db";
    public static final int DATABASE_VERSION = 1;
    public static final String CONTACT_TABLE_NAME = "ContactTable";


    public LocalDataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ContactTable(ContactId INTEGER PRIMARY KEY AUTOINCREMENT,ContactName TEXT,ContactNumber TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int n, int n2) {
        if (n < n2) {
            db.execSQL("DROP TABLE IF EXISTS ContactTable");
        }
    }

    public void insertContactDetailsToDb(PreferencesManager preferencesManager, List<ContactModel> list) {
        SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
        if (list.size() > 0) {
            sQLiteDatabase.execSQL("DELETE FROM ContactTable");
            for (int i = 0; i < list.size(); ++i) {
                try {
                    ContentValues contentValues = new ContentValues();
                    String phNumber = list.get(i).getContactNumber();
                    String cName = list.get(i).getContactName();
                    phNumber = phNumber.replace("+91", "");
                    phNumber = phNumber.replace("\\s", "");
                    phNumber = phNumber.replace(" ", "");
                    phNumber = phNumber.trim();
                    cName = cName.replace(" ", "");
                    cName = cName.trim();
                    contentValues.put(CONTACT_NAME, cName);
                    contentValues.put(CONTACT_NUMBER, phNumber);
                    //contentValues.put(CONTACT_ID, ((ContactModel) list.get(i)).getContactId());
                    sQLiteDatabase.insert(CONTACT_TABLE_NAME, null, contentValues);
                    //sQLiteDatabase.close();
                } catch (Exception e) {
                    Log.d("##insertContactDetails", "----------------->" + e.getMessage());
                }
            }
            if (!preferencesManager.getBooleanValue(IS_CONTACT_SORTED)) {
                preferencesManager.setBooleanValue(IS_CONTACT_SORTED, true);
                getSortedContactList(preferencesManager);
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void getSortedContactList(PreferencesManager preferencesManager) {
        List<ContactModel> ContactModelList = new ArrayList<>();
        //String selectQuery = "SELECT * from ContactTable WHERE ContactName like 'Pitha ji%' GROUP BY ContactNumber LIMIT 1";
        String selectQuery = "SELECT * FROM ContactTable GROUP BY ContactNumber ORDER BY ContactName ASC";
        try (Cursor cursor = this.getReadableDatabase().rawQuery(selectQuery, null)) {
            while (cursor.moveToNext()) {
                try {
                    ContactModel ContactModel = new ContactModel();
                    ContactModel.setContactId(cursor.getString(cursor.getColumnIndex(CONTACT_ID)));
                    ContactModel.setContactName(cursor.getString(cursor.getColumnIndex(CONTACT_NAME)));
                    ContactModel.setContactNumber(cursor.getString(cursor.getColumnIndex(CONTACT_NUMBER)));
                    ContactModelList.add(ContactModel);
                } catch (Exception exception) {
                    Log.e("##getSortedContactList", "------->" + exception.getMessage());
                }
            }
        }
        if (ContactModelList.size() > 0) {
            insertContactDetailsToDb(preferencesManager, ContactModelList);
        }
    }

    public ContactModel getMatchedContact(String contactName) {
        ContactModel contactModel = new ContactModel();
        String selectQuery = "SELECT * from ContactTable WHERE ContactName like '" + contactName + "%' GROUP BY ContactNumber LIMIT 1";
        try (Cursor cursor = this.getReadableDatabase().rawQuery(selectQuery, null)) {
            while (cursor.moveToNext()) {
                try {
                    contactModel.setContactId(cursor.getString(cursor.getColumnIndex(CONTACT_ID)));
                    contactModel.setContactName(cursor.getString(cursor.getColumnIndex(CONTACT_NAME)));
                    contactModel.setContactNumber(cursor.getString(cursor.getColumnIndex(CONTACT_NUMBER)));
                } catch (Exception exception) {
                    Log.e("##getMatchedContact", "------->" + exception.getMessage());
                }
            }
        }
        return contactModel;
    }


    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}

