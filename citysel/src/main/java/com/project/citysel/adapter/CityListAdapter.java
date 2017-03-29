package com.project.citysel.adapter;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.project.citysel.R;
import com.project.citysel.bean.City;
import com.project.citysel.view.MyGridView;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;


public class CityListAdapter extends BaseAdapter {

	private Context mContext;
	private List<City> mAllCityList;
	private List<City> mHotCityList;
	private List<String> mRecentCityList;
	public HashMap<String, Integer> alphaIndexer;// 存放存在的汉语拼音首字母和与之对应的列表位置
	private String[] sections;// 存放存在的汉语拼音首字母
	private LocationClient myLocationClient;
	private String currentCity;//当前城市
	private MyLocationListener myLocationListener;
	private boolean isNeedRefresh;//当前定位的城市是否需要刷新
	private TextView tvCurrentLocateCity;
	private ProgressBar pbLocate;
	private TextView tvLocate;
	private final int VIEW_TYPE = 5;//view的类型个数

	public CityListAdapter(Context context, List<City> allCityList,
						   List<City> hotCityList, List<String> recentCityList) {
		this.mContext = context;
		this.mAllCityList = allCityList;
		this.mHotCityList = hotCityList;
		this.mRecentCityList=recentCityList;

		alphaIndexer = new HashMap<String, Integer>();
		sections = new String[allCityList.size()];

		//这里的主要目的是将listview中要显示字母的条目保存下来，方便在滑动时获得位置，alphaIndexer在Acitivity有调用
		for (int i = 0; i < mAllCityList.size(); i++) {
			// 当前汉语拼音首字母
			String currentStr = getAlpha(mAllCityList.get(i).getPinyin());
			// 上一个汉语拼音首字母，如果不存在为" "
			String previewStr = (i - 1) >= 0 ? getAlpha(mAllCityList.get(i - 1).getPinyin()) : " ";
			if (!previewStr.equals(currentStr)) {
				String name = getAlpha(mAllCityList.get(i).getPinyin());
				alphaIndexer.put(name, i);
				sections[i] = name;
			}
		}
		isNeedRefresh=true;
		initLocation();
	}

	@Override
	public int getViewTypeCount() {

		return VIEW_TYPE;
	}

	@Override
	public int getItemViewType(int position) {
		return position < 4 ? position : 4;
	}

	@Override
	public int getCount() {
		return mAllCityList.size();
	}

	@Override
	public Object getItem(int position) {
		return mAllCityList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		int viewType = getItemViewType(position);
		if (viewType == 0) {//view类型为0，也就是：当前定位城市的布局
			convertView = View.inflate(mContext, R.layout.item_location_city,
					null);
			tvLocate=(TextView) convertView.findViewById(R.id.tv_locate);
			tvCurrentLocateCity=(TextView) convertView.findViewById(R.id.tv_current_locate_city);
			pbLocate = (ProgressBar) convertView.findViewById(R.id.pb_loacte);

			if(!isNeedRefresh){
				tvLocate.setText("当前定位城市");
				tvCurrentLocateCity.setVisibility(View.VISIBLE);
				tvCurrentLocateCity.setText(currentCity);
				pbLocate.setVisibility(View.GONE);
			}else{
				myLocationClient.start();
			}

			tvCurrentLocateCity.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pbLocate.setVisibility(View.VISIBLE);
					tvLocate.setText("正在定位");
					tvCurrentLocateCity.setVisibility(View.GONE);
					myLocationClient.start();
				}
			});

		} else if (viewType == 1) {//最近访问城市
			convertView = View.inflate(mContext,R.layout.item_recent_visit_city, null);
			TextView tvRecentVisitCity=(TextView) convertView.findViewById(R.id.tv_recent_visit_city);
			tvRecentVisitCity.setText("最近访问城市");
			MyGridView gvRecentVisitCity = (MyGridView) convertView.findViewById(R.id.gv_recent_visit_city);
			gvRecentVisitCity.setAdapter(new RecentVisitCityAdapter(mContext,mRecentCityList));

		} else if (viewType == 2) {//热门城市
			convertView = View.inflate(mContext,R.layout.item_recent_visit_city, null);
			TextView tvRecentVisitCity=(TextView) convertView.findViewById(R.id.tv_recent_visit_city);
			tvRecentVisitCity.setText("热门城市");
			MyGridView gvRecentVisitCity = (MyGridView) convertView.findViewById(R.id.gv_recent_visit_city);
			gvRecentVisitCity.setAdapter(new HotCityAdapter(mContext,mHotCityList));
		} else if (viewType == 3) {//全部城市，仅展示“全部城市这四个字”
			convertView = View.inflate(mContext,R.layout.item_all_city_textview, null);
		} else {//数据库中所有的城市的名字展示
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = View.inflate(mContext, R.layout.item_city_list,null);
				viewHolder.tvAlpha = (TextView) convertView.findViewById(R.id.tv_alpha);
				viewHolder.tvCityName = (TextView) convertView.findViewById(R.id.tv_city_name);
				viewHolder.llMain=(LinearLayout) convertView.findViewById(R.id.ll_main);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			if (position >= 1) {
				viewHolder.tvCityName.setText(mAllCityList.get(position).getName());
				viewHolder.llMain.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Toast.makeText(mContext,mAllCityList.get(position).getName(),0).show();
					}
				});
				String currentStr = getAlpha(mAllCityList.get(position).getPinyin());
				String previewStr = (position - 1) >= 0 ? getAlpha(mAllCityList
						.get(position - 1).getPinyin()) : " ";
				//如果当前的条目的城市名字的拼音的首字母和其前一条条目的城市的名字的拼音的首字母不相同，则将布局中的展示字母的TextView展示出来
				if (!previewStr.equals(currentStr)) {
					viewHolder.tvAlpha.setVisibility(View.VISIBLE);
					viewHolder.tvAlpha.setText(currentStr);
				} else {
					viewHolder.tvAlpha.setVisibility(View.GONE);
				}
			}

		}

		return convertView;
	}

	// 获得汉语拼音首字母
	private String getAlpha(String str) {
		if (str == null) {
			return "#";
		}
		if (str.trim().length() == 0) {
			return "#";
		}
		char c = str.trim().substring(0, 1).charAt(0);
		// 正则表达式，判断首字母是否是英文字母
		Pattern pattern = Pattern.compile("^[A-Za-z]+$");
		if (pattern.matcher(c + "").matches()) {
			return (c + "").toUpperCase();
		} else if (str.equals("0")) {
			return "定位";
		} else if (str.equals("1")) {
			return "最近";
		} else if (str.equals("2")) {
			return "热门";
		} else if (str.equals("3")) {
			return "全部";
		} else {
			return "#";
		}
	}

	class ViewHolder {
		TextView tvAlpha;
		TextView tvCityName;
		LinearLayout llMain;
	}

	public void initLocation() {
		myLocationClient = new LocationClient(mContext);
		myLocationListener=new MyLocationListener();
		myLocationClient.registerLocationListener(myLocationListener);
		// 设置定位参数
		LocationClientOption option = new LocationClientOption();
		option.setCoorType("bd09ll"); // 设置坐标类型
		option.setScanSpan(10000); // 10分钟扫描1次
		// 需要地址信息，设置为其他任何值（string类型，且不能为null）时，都表示无地址信息。
		option.setAddrType("all");
		// 设置是否返回POI的电话和地址等详细信息。默认值为false，即不返回POI的电话和地址信息。
		option.setPoiExtraInfo(true);
		// 设置产品线名称。强烈建议您使用自定义的产品线名称，方便我们以后为您提供更高效准确的定位服务。
		option.setProdName("通过GPS定位我当前的位置");
		// 禁用启用缓存定位数据
		option.disableCache(true);
		// 设置最多可返回的POI个数，默认值为3。由于POI查询比较耗费流量，设置最多返回的POI个数，以便节省流量。
		option.setPoiNumber(3);
		// 设置定位方式的优先级。
		// 当gps可用，而且获取了定位结果时，不再发起网络请求，直接返回给用户坐标。这个选项适合希望得到准确坐标位置的用户。如果gps不可用，再发起网络请求，进行定位。
		option.setPriority(LocationClientOption.GpsFirst);
		myLocationClient.setLocOption(option);
		myLocationClient.start();
	}

	public class MyLocationListener implements BDLocationListener{

		@Override
		public void onReceiveLocation(BDLocation arg0) {

			isNeedRefresh=false;
			if(arg0.getCity()==null){
				//定位失败
				tvLocate.setText("未定位到城市,请选择");
				tvCurrentLocateCity.setVisibility(View.VISIBLE);
				tvCurrentLocateCity.setText("重新选择");
				pbLocate.setVisibility(View.GONE);
				return;
			}else{
				//定位成功
				currentCity=arg0.getCity().substring(0,arg0.getCity().length()-1);
				tvLocate.setText("当前定位城市");
				tvCurrentLocateCity.setVisibility(View.VISIBLE);
				tvCurrentLocateCity.setText(currentCity);
				myLocationClient.stop();
				pbLocate.setVisibility(View.GONE);
			}
		}

		@Override
		public void onReceivePoi(BDLocation arg0) {

		}

	}

}
