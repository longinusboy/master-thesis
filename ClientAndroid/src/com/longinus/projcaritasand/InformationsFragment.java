package com.longinus.projcaritasand;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.longinus.projcaritasand.model.SingletonModel;

public class InformationsFragment extends Fragment {
	private Context context;
	private boolean mustRefresh;
	private Thread refreshThread;

	public InformationsFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_informations, container, false);
		
		TextView deviceId = (TextView) view.findViewById(R.id.tv_myid);
		TextView tvRegistriesSent = (TextView) view.findViewById(R.id.tv_registries_sent);
		TextView tvRegistriesAwaiting = (TextView) view.findViewById(R.id.tv_registries_awaiting);
		
		TextView tvLastAccess = (TextView) view.findViewById(R.id.tv_last_server_access);
		TextView tvLastUpdate = (TextView) view.findViewById(R.id.tv_last_server_update);

		deviceId.setText(SingletonModel.getInstance().getDeviceID());
		tvRegistriesSent.setText(String.valueOf(SingletonModel.getInstance().getNumberRegistriesSent()));
		tvRegistriesAwaiting.setText(String.valueOf(SingletonModel.getInstance().getNumberRegistriesAwaiting()));
		
		long lastAccess = SingletonModel.getInstance().getLastServerAccess();
		long lastUpdate = SingletonModel.getInstance().getLastServerUpdate();
		
		if (lastAccess>-1) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			tvLastAccess.setText(sdf.format(new Date(lastAccess)));
		}else {
			tvLastAccess.setText("-");
		}
		
		if (lastUpdate>-1) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			tvLastUpdate.setText(sdf.format(new Date(lastUpdate)));
		}else {
			tvLastUpdate.setText("-");
		}
		
		Button buttonForce = (Button) view.findViewById(R.id.btn_send_data);
		buttonForce.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (context instanceof IMessages) {
					((IMessages)context).onMessage("FORCE_SEND", null);
				}
			}
		});
		
		Button buttonExportDB = (Button) view.findViewById(R.id.export_db);
		buttonExportDB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SingletonModel.getInstance().exportDB();
			}
		});
		
		mustRefresh = true;
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		refreshThread = new Thread(new Runnable() {
			public void run() {
				do {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
					}
					try {
						updateDates();
					} catch (Exception e) {
						Log.e(this.getClass().getName(), "refreshThread", e);
					}				
				} while (mustRefresh);
			}
		});
		refreshThread.start();
	}

	@Override
	public void onPause() {
		mustRefresh = false;
		super.onPause();
	}

	private void updateDates() {
		((Activity)context).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					TextView tvRegistriesSent = (TextView) getView().findViewById(R.id.tv_registries_sent);
					TextView tvRegistriesAwaiting = (TextView) getView().findViewById(R.id.tv_registries_awaiting);
					
					tvRegistriesSent.setText(String.valueOf(SingletonModel.getInstance().getNumberRegistriesSent()));
					tvRegistriesAwaiting.setText(String.valueOf(SingletonModel.getInstance().getNumberRegistriesAwaiting()));
					
					TextView tvLastAccess = (TextView) getView().findViewById(R.id.tv_last_server_access);
					TextView tvLastUpdate = (TextView) getView().findViewById(R.id.tv_last_server_update);
					
					long lastAccess = SingletonModel.getInstance().getLastServerAccess();
					long lastUpdate = SingletonModel.getInstance().getLastServerUpdate();
					
					if (lastAccess>-1) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						tvLastAccess.setText(sdf.format(new Date(lastAccess)));
					}else {
						tvLastAccess.setText("-");
					}
					
					if (lastUpdate>-1) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						tvLastUpdate.setText(sdf.format(new Date(lastUpdate)));
					}else {
						tvLastUpdate.setText("-");
					}
				} catch (Exception e) {
				}				
			}
		});		
	}
}
