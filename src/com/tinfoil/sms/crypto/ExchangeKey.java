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

package com.tinfoil.sms.crypto;

import java.security.Security;
import java.util.ArrayList;

import org.strippedcastle.jce.provider.BouncyCastleProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tinfoil.sms.R;
import com.tinfoil.sms.dataStructures.ContactParent;
import com.tinfoil.sms.dataStructures.Number;
import com.tinfoil.sms.dataStructures.TrustedContact;
import com.tinfoil.sms.database.DBAccessor;
import com.tinfoil.sms.settings.EditNumber;
import com.tinfoil.sms.utility.OnKeyExchangeResolvedListener;
import com.tinfoil.sms.utility.SMSUtility;

/**
 * A class that creates a thread to manage a user's exchange of keys with
 * contacts. It will go through each contact that the user wishes to exchange
 * keys with and queue a key exchange message. This thread only handles the
 * first phase of the key exchange. The second phase is handled in the
 * MessagerReceiver.
 */
public class ExchangeKey implements Runnable {

    //public static ProgressDialog keyDialog;
    private ArrayList<String> untrusted;
    private ArrayList<String> trusted;
    private ArrayList<ContactParent> contacts;
    private Number number;
    
    private Activity activity;
    private TrustedContact trustedContact;
    
    private OnKeyExchangeResolvedListener listener;
    
    private boolean multiNumber = false;
    private DBAccessor dba;
    

    /* Register spongycastle as the most preferred security provider */
    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }
    
    /**
     * Used by the ManageContactsActivity to set up the key exchange thread
     * 
     * @param contacts The list of contacts
     */
    public void startThread(Activity activity, final ArrayList<ContactParent> contacts)
    {
        this.activity = activity;
        this.contacts = contacts;
        this.trusted = null;
        this.untrusted = null;
        
        multiNumber = true;
        dba = new DBAccessor(activity);
        /*
         * Start the thread from the constructor
         */
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Used to setup the a key exchange for a single contact or to untrust a
     * single contact.
     * @param trusted The number of the contact to send a key exchange to,
     * do not sent a key exchange if null
     * @param untrusted The number of the contact to un-trust, no contact is
     * untrusted. 
     */
    public void startThread(Activity activity, final String trusted, final String untrusted)
    {
        this.activity = activity;
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
        multiNumber = false;
        dba = new DBAccessor(activity);
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
                this.number = dba.getNumber(this.untrusted.get(i));
                this.number.clearPublicKey();
                
                //set the initiator flag to false
                this.number.setInitiator(false);
                dba.updateKey(this.number);
            }
        }

        //TODO give a status update for each person sent and received
        /*
         * Start Key exchanges 1 by 1, messages are prepared and then placed in
         * the messaging queue.
         */       
        boolean invalid = false;
        if (this.trusted != null)
        {
            for (int i = 0; i < this.trusted.size(); i++)
            {
                number = dba.getNumber(trusted.get(i));
                
                if(number != null)
                {
	                if (SMSUtility.checksharedSecret(number.getSharedInfo1()) &&
	                		SMSUtility.checksharedSecret(number.getSharedInfo2()))
	                {
                		Log.v("S1", number.getSharedInfo1());
                		Log.v("S2", number.getSharedInfo2());
	                	
		                /*
		                 * Set the initiator flag since this user is starting the key exchange.
		                 */
		                number.setInitiator(true);					
		                                
		                dba.updateInitiator(number);
		                
		                String keyExchangeMessage = KeyExchange.sign(number,
		                		dba, SMSUtility.user);
		                
		                dba.addMessageToQueue(number.getNumber(), keyExchangeMessage, true);
	                }
	                else
	                {
	                	invalid = true;
	                	//Toast.makeText(c, "Invalid shared secrets", Toast.LENGTH_LONG).show();
	                	Log.v("Shared Secret", "Invalid shared secrets");
	                }
                }
                else
                {
                	//TODO give error message
                }
            }
        }
        
        if (invalid)
        {
        	trustedContact = dba.getRow(number.getNumber());
        	
        	/*
        	 * Get the shared secrets from the user.
        	 */
        	activity.runOnUiThread(new Runnable() {
        	    public void run() {
        	    	
        	    	if(!multiNumber)
        	    	{
        	    		//Toast.makeText(activity, "Shared secrets must be set prior to key exchange", Toast.LENGTH_LONG).show();
        	    		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        	    		LinearLayout linearLayout = new LinearLayout(activity);
        	    		linearLayout.setOrientation(LinearLayout.VERTICAL);

        	    		final EditText sharedSecret1 = new EditText(activity);
        	    		sharedSecret1.setHint(R.string.shared_secret_hint_1);
        	    		sharedSecret1.setMaxLines(EditNumber.SHARED_INFO_MAX);
        	    		sharedSecret1.setInputType(InputType.TYPE_CLASS_TEXT);
        	    		linearLayout.addView(sharedSecret1);

        	    		final EditText sharedSecret2 = new EditText(activity);
        	    		sharedSecret2.setHint(R.string.shared_secret_hint_2);
        	    		sharedSecret2.setMaxLines(EditNumber.SHARED_INFO_MAX);
        	    		sharedSecret2.setInputType(InputType.TYPE_CLASS_TEXT);
        	    		linearLayout.addView(sharedSecret2);
        	    		
        	    		builder.setMessage(activity.getString(R.string.set_shared_secrets)
        	    				+ " " + trustedContact.getName() + ", " + number.getNumber())
        	    		       .setCancelable(true)
        	    		       .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
        	    		    	   @Override
        	    		    	   public void onClick(DialogInterface dialog, int id) {
        	    		                //Save the shared secrets
        	    		    		   String s1 = sharedSecret1.getText().toString();
        	    		    		   String s2 = sharedSecret2.getText().toString();
        	    		    		   if(s1 != null && s2 != null &&
        	    		    				   s1.length() >= EditNumber.SHARED_INFO_MIN &&
        	    		    				   s2.length() >= EditNumber.SHARED_INFO_MIN)
        	    		    		   {
        	    		    			   //Toast.makeText(activity, "Valid secrets", Toast.LENGTH_LONG).show();
        	    		    			   number.setSharedInfo1(s1);
        	    		    			   number.setSharedInfo2(s2);
        	    		    			   dba.updateNumberRow(number, number.getNumber(), number.getId());
        	    		    			   number.setInitiator(true);					
       	                                
        	    			               dba.updateInitiator(number);
        	    			                
        	    			               String keyExchangeMessage = KeyExchange.sign(number,
        	    			            		   dba, SMSUtility.user);
        	    			                
        	    			               dba.addMessageToQueue(number.getNumber(), keyExchangeMessage, true);
        	    		    		   }
        	    		    		   else
        	    		    		   {
        	    		    			   Toast.makeText(activity, R.string.invalid_secrets, Toast.LENGTH_LONG).show();
        	    		    		   }
        	    		           }})
        	    		       .setOnCancelListener(new OnCancelListener(){

									@Override
									public void onCancel(DialogInterface arg0) {
										//Cancel the key exchange
										Toast.makeText(activity, R.string.key_exchange_cancelled, Toast.LENGTH_LONG).show();
									}
        	    		    	   
        	    		       })
        	    		       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        	    		    	   @Override
        	    		    	   public void onClick(DialogInterface arg0, int arg1) {
        	    		    			//Cancel the key exchange
        	    		    		   Toast.makeText(activity, R.string.key_exchange_cancelled, Toast.LENGTH_LONG).show();
        	    		    	   }});
        	    		AlertDialog alert = builder.create();
        	    		
        	    		alert.setView(linearLayout);
        	    		alert.show();
        	    	}
        	    	else
        	    	{
        	    		Toast.makeText(activity, R.string.unsuccessful_key_exchanges, Toast.LENGTH_LONG).show();
        	    	}
        	    }
        	});
        }

        if((trusted == null || trusted.size() == 0) && listener != null)
        {
        	Log.v("onKeyExchangeResolved", "TRUE");
        	listener.onKeyExchangeResolved();
        }
        //Dismisses the load dialog since the load is finished
        //keyDialog.dismiss();
    }
    
    public void setOnFinishedTaskListener(OnKeyExchangeResolvedListener listener)
    {
    	this.listener = listener;
    }

    public void removeOnFinishedTaskListener()
    {
    	this.listener = null;
    }
}
