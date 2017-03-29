package com.project.citysel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.project.citysel.R;
import com.project.citysel.bean.City;

import java.util.List;

public class SearchResultAdapter extends BaseAdapter {
	
	private List<City> mSearchList;
	private Context mContext;
	private LayoutInflater mInflater;
	
	public SearchResultAdapter(Context context,List<City> searchList){
		this.mSearchList=searchList;
		this.mContext=context;
		mInflater=LayoutInflater.from(mContext);
	}

	@Override
	public int getCount() {
		return mSearchList.size();
	}

	@Override
	public Object getItem(int position) {
		return mSearchList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder=null;
		if(convertView==null){
			viewHolder=new ViewHolder();
			convertView=mInflater.inflate(R.layout.item_search_list,null);
			viewHolder.tvCityName=(TextView) convertView.findViewById(R.id.tv_city_name);
			convertView.setTag(viewHolder);
		}else{
			viewHolder =(ViewHolder) convertView.getTag();
		}
		
		viewHolder.tvCityName.setText(mSearchList.get(position).getName());
		viewHolder.tvCityName.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Toast.makeText(mContext,mSearchList.get(position).getName(),0).show();
			}
		});
		
		return convertView;
	}
	
	class ViewHolder{
		LinearLayout ll_item;
		TextView tvCityName;
	}

}
