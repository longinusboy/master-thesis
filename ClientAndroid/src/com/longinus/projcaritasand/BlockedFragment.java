package com.longinus.projcaritasand;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.longinus.projcaritasand.model.SingletonModel;

public class BlockedFragment extends Fragment {
	private Context context;
	private String phoneNumber;
	private String emailAddr;
	
	public BlockedFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_blocked, container, false);
	
		TextView deviceId = (TextView) view.findViewById(R.id.tv_myid);
		TextView emailTv = (TextView) view.findViewById(R.id.tv_email);
		TextView callTv = (TextView) view.findViewById(R.id.tv_telephone);
		
		deviceId.setText(SingletonModel.getInstance().getDeviceID());
		
		phoneNumber = context.getString(R.string.help_number);
		emailAddr = context.getString(R.string.help_email);
		
		emailTv.setText(emailAddr);
		callTv.setText(phoneNumber);

		ImageButton imgButtonCall = (ImageButton) view.findViewById(R.id.imgbtn_call);
		ImageButton imgButtonEmail = (ImageButton) view.findViewById(R.id.imgbtn_email);
		imgButtonCall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent callIntent = new Intent(Intent.ACTION_DIAL);
				callIntent.setData(Uri.parse("tel:"+phoneNumber));
				startActivity(callIntent);
			}
		});
		imgButtonEmail.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

				/* Fill it with Data */
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailAddr});
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Informação de "+SingletonModel.getInstance().getDeviceID());
				
				/* Send it off to the Activity-Chooser */
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));
			}
		});
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}

}
