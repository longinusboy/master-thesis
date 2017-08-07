package com.longinus.projcaritasand;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.*;

import android.app.Application;

@ReportsCrashes(formKey = "",
	mailTo = "david_nemesis@msn.com",
	mode = ReportingInteractionMode.TOAST,
	resToastText = R.string.crash_toast_text)
public class MyApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		ACRA.init(this);
	}

}
