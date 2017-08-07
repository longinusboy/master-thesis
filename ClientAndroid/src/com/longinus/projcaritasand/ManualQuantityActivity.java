package com.longinus.projcaritasand;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.longinus.projcaritasand.model.SingletonModel;

public class ManualQuantityActivity extends Activity {
	private String type = "";
	private int weight = 0;
	private String unit = "";
	private int realWeight = 0;
	private NumberPicker numberPicker;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String localType = intent.getStringExtra("type");
		if(localType!=null)
			type = localType;
		int localWeight = intent.getIntExtra("weight", -1);
		if(localWeight>-1)
			weight = localWeight;
		String localUnit = intent.getStringExtra("unit");
		if(localUnit!=null)
			unit = localUnit;
		int localRealWeight = intent.getIntExtra("realWeight", -1);
		if (localRealWeight > -1)
			realWeight = localRealWeight;
		String displayedUnit = intent.getStringExtra("displayedUnit");
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.fragment_quantity);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setLevelTitle(type, displayedUnit);
		
		numberPicker = (NumberPicker) findViewById(R.id.numberPickerManual);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		int titleId = getResources().getIdentifier("action_bar_title", "id", "android");

		// titleTxv : member reference to the action bar title textview
		// Setting Marquee here
		TextView titleTxv = (TextView) findViewById(titleId);
		titleTxv.setEllipsize(TextUtils.TruncateAt.START);
		titleTxv.setMarqueeRepeatLimit(-1);
		titleTxv.setFocusable(true);
		titleTxv.setFocusableInTouchMode(true);
		titleTxv.setHorizontallyScrolling(true);
		titleTxv.setFreezesText(true);
		titleTxv.setSingleLine(true);
		
		super.onResume();
	}
	
	public void onFinalizeClick(View view) {		
		int currentValue = numberPicker.getValue();
		
		Log.d("ManualQuantityActivity-onFinalizeClick", "type "+type+" weight "+weight+" unit "+unit+" num "+currentValue);
		SingletonModel.getInstance().addSimpleDonationProduct(type, weight, unit, realWeight, currentValue);
		Intent result = new Intent();
		setResult(RESULT_OK, result);
		finish();
	}
	
	public void setLevelTitle(String type, String measuredUnit) {
		String title = SingletonModel.getInstance().getLevelTitle(type, measuredUnit);
		
		try {
			title = Html.fromHtml(title).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		final String decodedTitle = title;
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				getActionBar().setTitle("Intro. Manual"+((decodedTitle!=null)?(" > " + decodedTitle):""));
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
