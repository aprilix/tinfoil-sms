/** 
 * Copyright (C) 2011 Tinfoilhat
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

package com.tinfoil.sms;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;

/**
 * Creates a database that is read and write and provides methods to facilitate the reading and writing to the database. 
 */
public class DBAccessor {
	
	public static final String KEY_NAME = "name";
	public static final String KEY_NUMBER = "number";
	public static final String KEY_KEY = "key";
	public static final String KEY_VERIFIED = "verified";
	
	private static Pattern p = Pattern.compile("^[+]1.{10}");
	private SQLiteDatabase db;
	private SQLitehelper contactDatabase;
	private ContentResolver cr;
	
	/**
	 * Creates a database that is read and write
	 * @param c	: Context, where the database is available
	 */
	public DBAccessor (Context c)
	{
		contactDatabase = new SQLitehelper(c);
		db = contactDatabase.getWritableDatabase();
		cr = c.getContentResolver();
	}
	
	/**
	 * Checks if a contact already has the given number
	 * @param number : String, a phone number
	 * @return : boolean
	 * true if their is a conflict
	 * false if there is not a conflict
	 */
	public boolean conflict (String number)
	{
		TrustedContact tc = getRow(number);
		if (tc == null)
		{
			return false;
		}
		return true;
		
	}

	/**
	 * Adds a row to the contacts table, trusted_contact
	 * @param name : String the name of the contact
	 * @param number : String the number for the contact
	 * @param key : String the contact's public key, null if not received
	 * @param verified : int whether the user's public key has been given to the contact, 0 if not sent
	 */
	public void addRow (String name, String number, String key, int verified)
	{
		//Check if name, number or key contain any ';'
		//if (!conflict(number))
		//{
			ContentValues cv = new ContentValues();
				
			//add given values to a row
	        cv.put(KEY_NAME, name);
	        cv.put(KEY_NUMBER, number);
	        cv.put(KEY_KEY, key);
	        cv.put(KEY_VERIFIED, verified);
	
	        //Insert the row into the database
	        open();
	        db.insert(SQLitehelper.TABLE_NAME, null, cv);
	        close();
		//}
		
	}
	
	/**
	 * Adds a row to the contacts table, trusted_contact
	 * @param tc : TrustedContact contains all the required information for the contact
	 */
	public void addRow (TrustedContact tc)
	{
		//Check if name, number or key contain any ';'
		//if (!conflict(tc.getNumber()))
		//{
			ContentValues cv = new ContentValues();
			
			//add given values to a row
	        cv.put(KEY_NAME, tc.getName());
	        cv.put(KEY_NUMBER, tc.getNumber());
	        cv.put(KEY_KEY, tc.getKey());
	        cv.put(KEY_VERIFIED, tc.getVerified());
	
	        //Insert the row into the database
	        open();
	        db.insert(SQLitehelper.TABLE_NAME, null, cv);
	        close();
		//}
        
	}
	
	/**
	 * **Note Still a working project
	 * This will be used to sync the contacts in tinfoil-sms's
	 * database with the contacts in the native database.
	 * @param name : String, the name of the contact to be added
	 * @param number : String, the number of the contact to be added
	 * @return : String
	 */
	public String nativeContact (String name, String number)
	{
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, 
		null, null, null, null);
		//ContactsContract.Contacts.DISPLAY_NAME +" = " + name,
		while (cur.moveToNext())
		{
			String id = cur.getString(
					cur.getColumnIndex(ContactsContract.Contacts._ID));
			String found_name = cur.getString(
		            cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			if (found_name.equalsIgnoreCase(name))
			{
		        Cursor pCur = cr.query(
		 	 		    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
		 	 		    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", 
		 	 		    new String[]{id}, null);
		        if (pCur.moveToNext())
		        {
		        	String tempNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		        	if (tempNumber.equalsIgnoreCase(number))
		        	{
		        		return "Contact is already in dba";
		        	}
		        	else
		        	{
		        		return "Found " + found_name + " X" + tempNumber + "X " + number;
		        	}
		        }
		        else
		        {
		        	return "Found " + found_name;
		        }
			
			}
			else 
			{
				break;
			}
		}
		
		//Need to use Content Provider to add stuff to android's db
		
		return "Found Nothing!";
	}
	
    /**
     * Open the database to be used
     */
	public void open()
	{
		db = contactDatabase.getWritableDatabase();
	}
	
	/**
	 * Close the database
	 */
	public void close()
	{
		contactDatabase.close();
		db.close();
	}
	
	/**
	 * Access the information stored in the database of a contact who has a certain number
	 * with the columns: name, number, key, verified.
	 * @param number : String the number of the contact to retrieve 
	 * @return TrustedContact, the row of data.
	 */
	public TrustedContact getRow(String number)
	{		
		open();
		Cursor cur = db.query(SQLitehelper.TABLE_NAME, new String[] {KEY_NAME, KEY_NUMBER, KEY_KEY, KEY_VERIFIED},
				"number = "+ number, null, null, null, null);
		
		if (cur.moveToFirst())
        {
			TrustedContact tc = new TrustedContact (cur.getString(0), cur.getString(1), cur.getString(2), cur.getInt(3));
			close();
			return tc;
        }
		close();
		return null;
	}
	
	/**
	 * Get all of the rows in the database with the columns
	 * name, number, key, verified.	
	 * @return : ArrayList<TrustedContact>, a list of all the
	 * contacts in the database
	 */
	public ArrayList<TrustedContact> getAllRows()
	{		
		open();
		Cursor cur = db.query(SQLitehelper.TABLE_NAME, new String[] {KEY_NAME, KEY_NUMBER, KEY_KEY, KEY_VERIFIED},
				null, null, null, null, "id");
		
		ArrayList<TrustedContact> tc = new ArrayList<TrustedContact>();

		if (cur.moveToFirst())
        {
			do
			{
				tc.add(new TrustedContact (cur.getString(0), cur.getString(1), cur.getString(2), cur.getInt(3)));
			}while (cur.moveToNext());
			close();
			return tc;
        }
		close();
		return null;
	}
	
	/**
	 * Update all of the values in a row
	 * @param tc : Trusted Contact, the new values for the row
	 * @param number : the number of the contact in the database
	 */
	public void updateRow (TrustedContact tc, String number)
	{
		open();
		removeRow(number);
		addRow(tc);
		close();
	}
		
	/**
	 * Deletes the rows with the given number
	 * @param number : String, the number of the contact to be deleted
	 */
	public void removeRow(String number)
	{
		open();
		db.delete(SQLitehelper.TABLE_NAME, "number = " +number, null);
		close();
	}
	
	//public void addTrusted()

	/**
	 * Checks if the given number is a trusted contact's number
	 * @param number : String, the number of the potential trusted contact
	 * @return : boolean
	 * true, if the contact is found in the database and is in the trusted state.
	 * false, if the contact is not found in the database or is not the trusted state.
	 * 
	 * A contact is in the trusted state if they have a key (!= null) and
	 * they have send their public key the contact (verified = 2)
	 */
	public boolean isTrustedContact (String number)
	{
		TrustedContact tc = getRow(number);
		
		if (tc == null)
		{
			tc = getRow(format(number));
		}
		if (tc != null)
		{
			if (!tc.isKeyNull() && tc.getVerified() == 2)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * **NOTE: This class could be moved to a static method.
	 * Removes the preceding '1' or '+1' for the given number
	 * @param number : String, the number of the contact 
	 * @return : String, the number without the preceding '1' or '+1'
	 */
	public static String format(String number)
	{
		if (number.matches("^1.{10}"))
		{
			return number.substring(1);
		}
		else if (number.matches(p.pattern())) 
		{
			return number.substring(2);
		}
		return number;
	}
}
