package com.longinus.projcaritasand;

import java.util.List;






import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.Utils;
import com.longinus.projcaritasand.model.database.Campaign;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CampaignsArrayAdapter extends ArrayAdapter<Campaign> {
	Context context;
	List<Campaign> campaigns;

	public CampaignsArrayAdapter(Context context, int resource, List<Campaign> objects) {
		super(context, resource, objects);
		this.context = context;
		this.campaigns = objects;
	}

	public CampaignsArrayAdapter(Context context, int resource, int textViewResourceId, List<Campaign> objects) {
		super(context, resource, textViewResourceId, objects);
		this.context = context;
		this.campaigns = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.campaign_row, parent, false);
		}
		
		TextView txtCollectionTitle = (TextView) rowView.findViewById(R.id.txtCampaignTitle);
		TextView txtCollectionDate = (TextView) rowView.findViewById(R.id.txtCampaignDate);
		TextView txtCollectionUsed = (TextView) rowView.findViewById(R.id.txtCampaignUsed);
		ImageView imgCollectionAuthorized = (ImageView) rowView.findViewById(R.id.imgCampaignAuthorized);

		Campaign campaign = campaigns.get(position);
		txtCollectionTitle.setText(campaign.getName());
		txtCollectionDate.setText(Utils.dateToFormattedText(campaign.getStartDate()) + " - " + Utils.dateToFormattedText(campaign.getEndDate()));
		
		Campaign campaignInUse = SingletonModel.getInstance().getCampaignInUse();
		if (campaignInUse == null) {
			txtCollectionUsed.setVisibility(View.INVISIBLE);
		}else {
			txtCollectionUsed.setVisibility((campaign.equals(campaignInUse)) ? View.VISIBLE : View.INVISIBLE);
		}		
		
		imgCollectionAuthorized.setBackgroundResource((campaign.isSubscribed()) ? R.drawable.ic_action_authorized : R.drawable.ic_action_unauthorized);

		return rowView;
	}

}
