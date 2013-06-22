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
package com.tinfoil.sms;

import org.acra.*;
import org.acra.annotation.*;

import com.tinfoil.sms.R;

import android.app.Application;
import android.widget.Toast;

@ReportsCrashes(
        formKey = "",
        formUri = "http://code4peace.dyndns.org:5984/acra-tinfoilsms/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="reporter",
        formUriBasicAuthPassword="reportALLthecrashes987",
        logcatArguments = { "-t", "200", "-v", "time"},
        // Your usual ACRA configuration
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
        )

/*
@ReportsCrashes(
        formKey = "",
        formUri = "http://code4peace.dyndns.org:5984/acra-tinfoilsms/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="reporter",
        formUriBasicAuthPassword="reportALLthecrashes987",
        // Your usual ACRA configuration
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
        )
*/

/**
 * The main application class which is simply used to initialize ACRA, which is
 * used to report crashes and other bugs in the application.
 */
public class TinfoilSMS extends Application
{
    @Override
    public void onCreate()
    {
      super.onCreate();
      ACRA.init(this);
      Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
      //Double derp = null;
      //Toast.makeText(this, derp.toString(), Toast.LENGTH_LONG).show();
      /*try{
    	 
    	  
      }
      catch (Exception e)
      {
    	  
      }*/
    }
}