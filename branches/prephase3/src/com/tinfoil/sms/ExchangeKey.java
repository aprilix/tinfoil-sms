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

import android.app.ProgressDialog;
import android.content.Context;

public class ExchangeKey implements Runnable {

    private Context c; //Currently not used but IS needed because messages will be sent from this thread
    public static ProgressDialog keyDialog;
    private ArrayList<String> untrusted;
    private ArrayList<String> trusted;
    private ArrayList<ContactParent> contacts;
    private Number number;

    /**
     * A constructor used by the ManageContactsActivity to set up the key
     * exchange thread
     * 
     * @param c
     *            The context of the activity
     * @param contacts
     *            The list of contacts
     */
    public void startThread(final Context c, final ArrayList<ContactParent> contacts)
    {
        this.c = c;
        this.contacts = contacts;
        this.trusted = null;
        this.untrusted = null;

        /*
         * Start the thread from the constructor
         */
        Thread thread = new Thread(this);
        thread.start();
    }

    public void startThread(final Context c, final String trusted, final String untrusted)
    {
        this.c = c;
        this.trusted = new ArrayList<String>();
        this.untrusted = new ArrayList<String>();
        
        if(trusted != null)
        {
        	this.trusted.add(trusted);
        }
        
        if(untrusted != null)
        {
        	this.untrusted.add(untrusted);
        }
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {

        /* 
         * Used by ManageContacts Activity to determine from the 
         * contacts that have been selected need to exchange keys or
         * stop sending secure messages
         */
        if (this.trusted == null && this.untrusted == null)
        {
            this.trusted = new ArrayList<String>();
            this.untrusted = new ArrayList<String>();

            for (int i = 0; i < this.contacts.size(); i++)
            {
                for (int j = 0; j < this.contacts.get(i).getNumbers().size(); j++)
                {
                    if (this.contacts.get(i).getNumber(j).isSelected())
                    {
                        if (!this.contacts.get(i).getNumber(j).isTrusted())
                        {
                            this.trusted.add(this.contacts.get(i).getNumber(j).getNumber());
                        }
                        else
                        {
                            this.untrusted.add(this.contacts.get(i).getNumber(j).getNumber());
                        }
                    }
                }
            }
        }

        /*
         * This is actually how removing contacts from trusted should look since it is just a
         * deletion of keys. We don't care if the contact will now fail to decrypt messages that
         * is the user's problem
         */
        if (this.untrusted != null)
        {
            for (int i = 0; i < this.untrusted.size(); i++)
            {
                //untrusted.get(i).clearPublicKey();
                this.number = MessageService.dba.getRow(this.untrusted.get(i)).getNumber(this.untrusted.get(i));
                this.number.clearPublicKey();
                MessageService.dba.updateKey(this.number);
            }
        }

        //TODO update to actually use proper key exchange (via sms)
        //Start Key exchanges 1 by 1, using the user specified time out.
        if (this.trusted != null)
        {
            for (int i = 0; i < this.trusted.size(); i++)
            {
                this.number = MessageService.dba.getRow(this.trusted.get(i)).getNumber(this.trusted.get(i));
                this.number.setPublicKey();
                MessageService.dba.updateKey(this.number);
            }
        }

        //Dismisses the load dialog since the load is finished
        keyDialog.dismiss();
    }

}
