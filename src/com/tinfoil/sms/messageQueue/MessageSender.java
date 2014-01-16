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

package com.tinfoil.sms.messageQueue;

import com.bugsense.trace.BugSenseHandler;
import com.tinfoil.sms.dataStructures.Entry;
import com.tinfoil.sms.database.DBAccessor;
import com.tinfoil.sms.utility.SMSUtility;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

public class MessageSender implements Runnable{

	private boolean loopRunner = true;
	private Context c;
	private boolean empty = true;
	private Thread thread;
	private DBAccessor sender;
	private boolean signal = false;
	
	/**
	 * Start the thread to send messages.
	 * @param c The context the messages are sent from.
	 */
	public void startThread(Context c) {
		this.c = c;
		this.sender = new DBAccessor(c);
		empty = true;
		thread = new Thread(this);
		thread.start();
	}
	
	//@Override
	public void run() {
		
		Looper.prepare();
		
		/*
		 * Keep the thread running
		 * TODO change while(true) to use a semaphore so that the thread can be
		 * killed once the program has exited
		 */
		while(true)
		{
			Entry mes = null;
			
			/*
			 * TODO change the queue to wait until the broadcast receiver
			 * notifies that the message has been sent or that the message.
			 */

			/*
			 * Get the next element in the queue. If there is no more elements
			 * wait until notified that there are more in the queue
			 */
			while(empty && mes == null)
			{
				mes = sender.getFirstInQueue();

				if(mes != null)
				{
					break;
				}
				else if (mes == null && !loopRunner)
				{
					break;
				}
				
				synchronized(this){
					try {
						this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						BugSenseHandler.sendExceptionMessage("Type", "MessageSender Concurrency Issue", e);
					}
				}
			}
			
			if (mes == null && !loopRunner)
			{
				break;
			}

			/*
			 * Check that the signal has not changed to have no signal to send
			 * messages. If there is no service, wait till the service state
			 * changes to signal.
			 */
			while(!signal && loopRunner)
			{
				Log.v("Signal", "none");
				synchronized(this){
					try {
						this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						BugSenseHandler.sendExceptionMessage("Type", "MessageSender Concurrency Issue", e);
					}
				}
			}
			
			Log.v("Signal", "some");
			
			synchronized(this){
				empty = true;
			}
			
			/*
			 * Send the message 
			 */
			if(mes != null) {
				SMSUtility.sendMessage(this.sender, c, mes);
			}
		}
		loopRunner = true;
		empty = true;
	}
	
	/**
	 * Set whether the queue has been emptied and notifies all the threads to
	 * wake up. 
	 * @param setEmpty Whether the queue is empty or not.
	 */
	public void threadNotify(boolean setEmpty)
	{
		if(setEmpty)
		{
			empty = false;
		}
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Check whether the phone has signal.
	 * @return Whether the phone has signal or not.
	 */
	public synchronized boolean isSignal() {
		return signal;
	}

	/**
	 * Set whether the phone has signal. This should really only be used by the
	 * signalListener.
	 * @param signal Whether the phone has signal or not.
	 */
	public synchronized void setSignal(boolean signal) {
		this.signal = signal;
	}
	
    /**
     * The semaphore for keeping the thread running. This can be left as true
     * until the activity is no longer in use (onDestroy) where it can be set to
     * false.
     * @param runner Whether the thread should be kept running
     */
    public synchronized void setRunner(boolean runner) {
		this.loopRunner = runner;
		notifyAll();
	}
}
	