package com.longinus.projcaritasand;

import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.database.Campaign;

public class CampaignsFragment extends Fragment {
	private Context context;

	public CampaignsFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_campaigns, container, false);
		
		ListView listView = (ListView) view.findViewById(R.id.listView_campaigns);
		List<Campaign> campaigns = SingletonModel.getInstance().getCampaigns();
		
		CampaignsArrayAdapter arrayAdapter = new CampaignsArrayAdapter(view.getContext(), R.layout.campaign_row, campaigns);
		listView.addHeaderView(View.inflate(view.getContext(), R.layout.campaigns_header, null), null, false);
		listView.setAdapter(arrayAdapter);
		
		TextView emptyText = (TextView) view.findViewById(R.id.empty_campaigns);
		listView.setEmptyView(emptyText);
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (context instanceof IMessages) {
					((IMessages)context).onMessage("CAMPAIGN_CLICKED", Integer.valueOf(position-1));
				}
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
