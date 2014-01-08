/** 
 * Copyright (C) 2013 Jonathan Gillett, Joseph Heron
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

package com.tinfoil.sms.settings;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;

import com.tinfoil.sms.dataStructures.Message;
import com.tinfoil.sms.dataStructures.Number;
import com.tinfoil.sms.dataStructures.TrustedContact;
import com.tinfoil.sms.loader.Loader;
import com.tinfoil.sms.utility.SMSUtility;

public class ImportContactLoader extends Loader{

    private Context context;
	private Handler handler;
    private ArrayList<TrustedContact> tc;
    private ArrayList<Boolean> inDb;
    private boolean clicked;   
    private boolean stop;
    private boolean doNothing;
    
    private SharedPreferences sharedPrefs;

    /**
     * Create the object and start the thread 
     * @param context The activity context
     * @param update Whether the load is an update or not.
     * @param handler The Handler that takes care of UI setup after the thread
     * has finished
     */
    public ImportContactLoader(Context context, boolean doNothing, 
    		ArrayList<Boolean> inDb, ArrayList<TrustedContact> tc, Handler handler)
    {
    	super(context);
    	this.context = context;
    	this.handler = handler;
    	this.doNothing = doNothing;
    	this.clicked = false;
    	this.inDb = inDb;
    	this.tc = tc;
    	start();
    }	


	@Override
	public void execution() {
		/*
    	 * Note throughout this thread checks are made to a variable 'stop'.
    	 * This variable identifies if the user has pressed the back button. If
    	 * they have the thread will break each loop until it is at the end of
    	 * run method and then will the dialog will be dismissed and the user
    	 * will go back to the previous activity.
    	 * This allows the user to interrupt the import contacts loading thread
    	 * so that if the user does not actually want to wait for the all the
    	 * contacts to be found then it will terminate the search. This is
    	 * because the method of reading in the contacts from the native's
    	 * database can be quite time consuming. This time increases as the
    	 * number of contacts increases, of course this also has to do with the
    	 * users phone.
    	 */
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    	 
		if (!doNothing)
		{
	        if (!this.clicked)
	        {
	            tc = new ArrayList<TrustedContact>();
	            ArrayList<Number> number;
	            String name;
	            String id;
	
	            final Uri mContacts = ContactsContract.Contacts.CONTENT_URI;
	            final Cursor cur = context.getContentResolver().query(mContacts, new String[] 
	            		{ Contacts._ID, Contacts.DISPLAY_NAME, Contacts.HAS_PHONE_NUMBER }, 
	                    null, null, Contacts.DISPLAY_NAME);
	
	            this.inDb = new ArrayList<Boolean>();
	
	            if (cur != null && cur.moveToFirst()) {
	                do {
	                	
	                	//Check if the thread has been stopped
	                	if(getStop())
	                	{
	                		break;
	                	}
	                	
	                    number = new ArrayList<Number>();
	                    name = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));
	                    id = cur.getString(cur.getColumnIndex(Contacts._ID));
	
	                    if (cur.getString(cur.getColumnIndex(Contacts.HAS_PHONE_NUMBER)).equalsIgnoreCase("1"))
	                    {
	                    	Cursor mCur = null;
	                        final Cursor pCur = context.getContentResolver().query(Phone.CONTENT_URI,
	                                new String[] { Phone.NUMBER, Phone.TYPE }, Phone.CONTACT_ID + " = ?",
	                                new String[] { id }, null);
	
	                        if (pCur != null && pCur.moveToFirst())
	                        {
	                            do
	                            {
	                            	//Check if the thread has been stopped
	                            	if(getStop())
	                            	{
	                            		break;
	                            	}
	                            	
	                                final String numb = pCur.getString(pCur.getColumnIndex(Phone.NUMBER));
	                                final int type = pCur.getInt(pCur.getColumnIndex(Phone.TYPE));
	                                final Uri uriSMSURI = Uri.parse("content://sms/");
	
	                                // Ensure that the number retrieved is not null
	                                if(numb != null)
	                                {
	                                	number.add(new Number(SMSUtility.format(numb), type));
	                                
	
		                                //This now takes into account the different formats of the numbers. 
		                                mCur = context.getContentResolver().query(uriSMSURI, new String[]
		                                { "body", "date", "type" }, "address = ? or address = ? or address = ?",
		                                        new String[] { SMSUtility.format(numb),
		                                                "+1" + SMSUtility.format(numb),
		                                                "1" + SMSUtility.format(numb) },
		                                        "date DESC LIMIT " +
                                                Integer.valueOf(sharedPrefs.getString
                                                (QuickPrefsActivity.MESSAGE_LIMIT_SETTING_KEY,
                                                String.valueOf(SMSUtility.LIMIT))));
		                                if (mCur != null && mCur.moveToFirst())
		                                {
		                                    do
		                                    {
		                                    	//Check if the thread has been stopped
		                                    	if(getStop())
		                                    	{
		                                    		break;
		                                    	}
		                                    	
		                                        //Toast.makeText(this, ContactRetriever.millisToDate(mCur.getLong(mCur.getColumnIndex("date"))), Toast.LENGTH_LONG);
		                                        final Message myM = new Message(mCur.getString(mCur.getColumnIndex("body")),
		                                                mCur.getLong(mCur.getColumnIndex("date")), mCur.getInt(mCur.getColumnIndex("type")));
		                                        number.get(number.size() - 1).addMessage(myM);
		                                        
		                                        //Check if the thread has been stopped
		                                    	if(getStop())
		                                    	{
		                                    		break;
		                                    	}
		                                    } while (mCur.moveToNext());
		                                    
		                                }
	                                }
	
	                                //Check if the thread has been stopped
	                            	if(getStop())
	                            	{
	                            		break;
	                            	}
	                                
	                            } while (pCur.moveToNext());
	                        }
	                        if(mCur != null)
	                        {
	                        	mCur.close();
	                        }                        
	                        pCur.close();
	                    }
	
	                    //Check if the thread has been stopped
	                	if(getStop())
	                	{
	                		break;
	                	}
	                	
	                    /*
	                     * Added a check to see if the number array is empty
	                     * if a contact has no number they can not be texted
	                     * therefore there is no point allowing them to be
	                     * added.
	                     */
	                    if (number != null && !number.isEmpty() &&
	                            !loader.inDatabase(number))
	                    {
	                        tc.add(new TrustedContact(name, number));
	                        this.inDb.add(false);
	                    }
	
	                    
	                    number = null;
	                } while (cur.moveToNext());
	            }
	            // cur.close();
	
	            final Uri uriSMSURI = Uri.parse("content://sms/conversations/");
	            final Cursor convCur = context.getContentResolver().query(uriSMSURI,
	                    new String[] { "thread_id" }, null,
	                    null, "date DESC");
	            
	            Cursor nCur = null;
	            Cursor sCur = null;
	
	            Number newNumber = null;
	
	            //Check if the thread has been stopped
	            while (convCur != null && convCur.moveToNext() && !getStop())
	            {
	                id = convCur.getString(convCur.getColumnIndex("thread_id"));
	
	                nCur = context.getContentResolver().query(Uri.parse("content://sms/inbox"),
	                        new String[] { "body", "address", "date", "type" }, "thread_id = ?",
	                        new String[] { id }, "date DESC LIMIT " +
	                                Integer.valueOf(sharedPrefs.getString
	                                        (QuickPrefsActivity.MESSAGE_LIMIT_SETTING_KEY, String.valueOf(SMSUtility.LIMIT))));
	
	                if (nCur != null && nCur.moveToFirst())
	                {
	                    newNumber = new Number(SMSUtility.format(
	                            nCur.getString(nCur.getColumnIndex("address"))));
	                    do
	                    {
	                    	//Check if the thread has been stopped
	                    	if(getStop())
	                    	{
	                    		break;
	                    	}
	                        
	                        newNumber.addMessage(new Message(nCur.getString(nCur.getColumnIndex("body")),
	                                nCur.getLong(nCur.getColumnIndex("date")), nCur.getInt(nCur.getColumnIndex("type"))));
	                        //newNumber.setDate(nCur.getLong(nCur.getColumnIndex("date")));
	                    } while (nCur.moveToNext());
	                }
	
	                sCur = context.getContentResolver().query(Uri.parse("content://sms/sent"),
	                        new String[] { "body", "address", "date", "type" }, "thread_id = ?",
	                        new String[] { id }, "date DESC LIMIT " +
	                        Integer.valueOf(sharedPrefs.getString
	                        (QuickPrefsActivity.MESSAGE_LIMIT_SETTING_KEY, String.valueOf(SMSUtility.LIMIT))));
	
	                if (sCur != null && sCur.moveToFirst())
	                {
	                    if (newNumber == null)
	                    {
	                        newNumber = new Number(SMSUtility.format(
	                                sCur.getString(sCur.getColumnIndex("address"))));
	                    }
	
	                    do
	                    {
	                    	//Check if the thread has been stopped
	                        if(getStop())
	                    	{
	                    		break;
	                    	}
	                        newNumber.addMessage(new Message(sCur.getString(sCur.getColumnIndex("body")),
	                                sCur.getLong(sCur.getColumnIndex("date")), sCur.getInt(sCur.getColumnIndex("type"))));
	                        //newNumber.setDate(nCur.getLong(nCur.getColumnIndex("date")));
	                    } while (sCur.moveToNext());
	                }
	                if (newNumber != null && !TrustedContact.isNumberUsed(tc, newNumber.getNumber())
	                        && !loader.inDatabase(newNumber.getNumber()) && newNumber.getNumber() != null)
	                {
	                    tc.add(new TrustedContact(newNumber));
	                    this.inDb.add(false);
	                }
	            }
	            if(nCur != null)
	            {
	            	nCur.close();
	            }
	            if(sCur != null)
	            {
	            	sCur.close();
	            }
	            convCur.close();
	        }
	        else
	        {
	            for (int i = 0; i < this.tc.size(); i++)
	            {
	                if (this.inDb.get(i))
	                {
	                    loader.addRow(tc.get(i));
	                }
	            }
	            
	            android.os.Message msg = new android.os.Message();
	        	Bundle b = new Bundle();
	        	msg.setData(b);
	        	msg.what = ImportContacts.FINISH;
	        	
	        	handler.sendMessage(msg);
	        }
	        
	        if(!getStop())
	        {
	        	android.os.Message msg = new android.os.Message();
	        	Bundle b = new Bundle();
	        	b.putSerializable(ImportContacts.TRUSTED_CONTACTS, (Serializable) tc);
	        	b.putSerializable(ImportContacts.IN_DATABASE, (Serializable) inDb);
	        	msg.setData(b);
	        	msg.what = ImportContacts.LOAD;
	        	
	        	handler.sendMessage(msg);
	        }
	        else
	        {
	        	setStop(false);
	        	android.os.Message msg = new android.os.Message();
	        	Bundle b = new Bundle();
	        	msg.setData(b);
	        	msg.what = ImportContacts.FINISH;
	        	
	        	handler.sendMessage(msg);
	        }
		}
		else
		{
			doNothing = false;
		}
	}
    
    /**
     * Get the stop flag
     * @return The stop flag
     */
    public synchronized boolean getStop()
    {
    	return this.stop;
    }
    
    /**
     * Set the stop flag to to true
     */
    public synchronized void setClicked(boolean clicked) 
    {
    	this.clicked = clicked;
    }
    
    /**
     * Get the stop flag
     * @return The stop flag
     */
    public synchronized boolean getClicked()
    {
    	return this.clicked;
    }    
    
    /**
     * Set the stop flag.
	 * @param stop Whether the thread should be stopped or not.
	 */
    public synchronized void setStop(boolean stop)
	{
    	this.stop = stop;
	}
}
