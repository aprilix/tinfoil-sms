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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ManageContactsActivity extends Activity {
	private final String KEY = "12345";
	private final int VERIFY = 2;
	private ListView listView;
	private ArrayList<TrustedContact> tc;
    /** Called when the activity is first created. */
	//private ListView mContactList;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact);
               
        String[] names;
        
        //Linking the ListView object to the appropriate listview from the xml file.
        listView = (ListView)findViewById(R.id.listView1);
        
        tc  = Prephase2Activity.dba.getAllRows();
        //Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();
        if (tc != null)
        {
	        //The string that is displayed for each item on the list 
	        names = new String[tc.size()];
	        for (int i = 0; i < tc.size(); i++)
	        {
	        	names[i] = tc.get(i).getName();
	        }

	        
	        //populates listview with the declared strings, an option is also given for it to be multiple choice (check boxes), or single list (radio buttons) 
	        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, names));

	        //Not setting focus on a particular list item, (focus is then left to default at the top of the page)
	        listView.setItemsCanFocus(false);

	        //Set the mode to single or multiple choice, (should match top choice)
	        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	        initList();
        }
        else 
        {
        	names = new String[1];
        	names[0] = "Add a Contact";
        	        
	        //populates listview with the declared strings, an option is also given for it to be multiple choice (check boxes), or single list (radio buttons) 
	        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));

	        //Not setting focus on a particular list item, (focus is then left to default at the top of the page)
	        listView.setItemsCanFocus(false);
        }
       
        //Create what happens when you click on a button
        listView.setOnItemClickListener(new OnItemClickListener()
		{
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
        		//Note: the key does not actually affect the encryption currently
        		//In order to send a encrypted message a contact must have a key
        		
        		if (tc != null)
        		{
        			if (Prephase2Activity.dba.isTrustedContact(tc.get(position).getNumber()))
	        		{
        				change(position, false);
	        			Toast.makeText(getApplicationContext(), "Contact removed from\nTrusted Contacts", Toast.LENGTH_SHORT).show();
	        		}
	        		else
	        		{
	        			change(position, true);
	        			Toast.makeText(getApplicationContext(), "Contact added from\nTrusted Contacts", Toast.LENGTH_SHORT).show();
	        		}
        		}
        		else
        		{
        			//Go to add contact
        			startActivity(new Intent(getBaseContext(), AddContact.class));
        			finish();
        		}

        	}});
        
	}
	
	
	
	private void remove(int position)
	{
		tc.get(position).setKey(null);
		tc.get(position).setVerified(0);
	}
	private void add(int position)
	{
		tc.get(position).setKey(KEY);
		tc.get(position).setVerified(VERIFY);
	}
	
	private void change(int position, boolean add)
	{
		if (add)
		{
			add(position);
		}
		else
		{
			remove(position);
		}
		Prephase2Activity.dba.removeRow(tc.get(position).getNumber());
		Prephase2Activity.dba.addRow(tc.get(position));
		
	}
	
	private void initList()
	{
		for (int i = 0; i < tc.size();i++)
		{				
			if (Prephase2Activity.dba.isTrustedContact(tc.get(i).getNumber()))
    		{
				listView.setItemChecked(i, true);
    		}
    		else
    		{
    			listView.setItemChecked(i, false);
    		}
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.manage_contacts_menu, menu);
		return true;
	}

	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.all:
			if (tc!=null)
			{
				for (int i = 0; i < tc.size();i++)
				{
					listView.setItemChecked(i, true);
					change(i, true);
				}
			}
			return true;
		case R.id.remove:
			if (tc!=null)
			{
				for (int i = 0; i < tc.size();i++)
				{
					listView.setItemChecked(i, false);
					change(i, false);
				}
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

}
