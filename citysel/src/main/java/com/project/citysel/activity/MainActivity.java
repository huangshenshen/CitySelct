package com.project.citysel.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.project.citysel.R;
import com.project.citysel.adapter.CityListAdapter;
import com.project.citysel.adapter.SearchResultAdapter;
import com.project.citysel.bean.City;
import com.project.citysel.dbhelper.AllCitySqliteOpenHelper;
import com.project.citysel.dbhelper.CitySqliteOpenHelper;
import com.project.citysel.utils.PingYinUtil;
import com.project.citysel.view.MyLetterView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends Activity {

	protected static final String TAG = "MainActivity";
	private MyLetterView myLetterView;//自定义的View
	private TextView tvDialog;//主界面显示字母的TextView
	private ListView lvCity;//进行城市列表展示
	private EditText etSearch;
	private ListView lvResult;//搜索结果列表展示
	private TextView tvNoResult;//搜索无结果时文字展示

	private List<City> allCityList;//所有的城市
	private List<City> hotCityList;//热门城市列表
	private List<City> searchCityList;//搜索城市列表
	private List<String> recentCityList;//最近访问城市列表

	public CitySqliteOpenHelper cityOpenHelper;//对保存了最近访问城市的数据库操作的帮助类
	public SQLiteDatabase cityDb;//保存最近访问城市的数据库
	public CityListAdapter cityListAdapter;
	public SearchResultAdapter searchResultAdapter;
	private boolean isScroll=false;
	private boolean mReady=false;
	private Handler handler;
	private OverlayThread overlayThread; //显示首字母对话框

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initData();
		setListener();

		//初始化所有城市列表
		initAllCityData();
		initRecentVisitCityData();//初始化最近访问的城市数据
		initHotCityData();//初始化热门城市
		setAdapter();//设置适配器
		mReady=true;
	}

	private void setAdapter() {
		cityListAdapter = new CityListAdapter(this,allCityList,hotCityList,recentCityList);
		searchResultAdapter=new SearchResultAdapter(this,searchCityList);
		lvCity.setAdapter(cityListAdapter);
		lvResult.setAdapter(searchResultAdapter);
	}

	private void initView() {
		myLetterView = (MyLetterView) findViewById(R.id.my_letterView);

		tvDialog = (TextView) findViewById(R.id.tv_dialog);
		myLetterView.setTextView(tvDialog);
		lvCity=(ListView) findViewById(R.id.lv_city);
		etSearch=(EditText) findViewById(R.id.et_search);
		lvResult=(ListView) findViewById(R.id.lv_result);
		tvNoResult=(TextView) findViewById(R.id.tv_noresult);
	}

	private void setListener() {
		//自定义myLetterView的一个监听
		myLetterView.setOnSlidingListener(new MyLetterView.OnSlidingListener() {

			@Override
			public void sliding(String s) {
				isScroll=false;
				if(cityListAdapter.alphaIndexer.get(s)!=null){
					//根据MyLetterView滑动到的数据获得ListView应该展示的位置
					int position = cityListAdapter.alphaIndexer.get(s);
					//将listView展示到相应的位置
					lvCity.setSelection(position);
				}
			}
		});

		etSearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s.toString()==null||"".equals(s.toString())){
					myLetterView.setVisibility(View.VISIBLE);
					lvCity.setVisibility(View.VISIBLE);
					lvResult.setVisibility(View.GONE);
					tvNoResult.setVisibility(View.GONE);
				}else{
					searchCityList.clear();
					myLetterView.setVisibility(View.GONE);
					lvCity.setVisibility(View.GONE);
					getResultCityList(s.toString());
					if (searchCityList.size() <= 0) {
						lvResult.setVisibility(View.GONE);
						tvNoResult.setVisibility(View.VISIBLE);
					} else {
						lvResult.setVisibility(View.VISIBLE);
						tvNoResult.setVisibility(View.GONE);
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
										  int after) {


			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

		lvCity.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == SCROLL_STATE_TOUCH_SCROLL
						|| scrollState == SCROLL_STATE_FLING) {
					isScroll = true;
				}

			}

			@SuppressLint("DefaultLocale") @Override
			public void onScroll(AbsListView view, int firstVisibleItem,
								 int visibleItemCount, int totalItemCount) {
				if (!isScroll) {
					return;
				}
				if (mReady) {
					String text;
					String name = allCityList.get(firstVisibleItem).getName();
					String pinyin = allCityList.get(firstVisibleItem).getPinyin();
					if (firstVisibleItem < 4) {
						text = name;
					} else {
						text = PingYinUtil.converterToFirstSpell(pinyin)
								.substring(0, 1).toUpperCase();
					}
					tvDialog.setText(text);
					tvDialog.setVisibility(View.VISIBLE);
					handler.removeCallbacks(overlayThread);
//					Toast.makeText(MainActivity.this,"测试",0).show();
//					 延迟一秒后执行，让中间显示的TextView为不可见
					handler.postDelayed(overlayThread,1000);
				}
			}
		});
	}

	private void initData() {
		cityOpenHelper=new CitySqliteOpenHelper(MainActivity.this);
		cityDb=cityOpenHelper.getWritableDatabase();
		allCityList=new ArrayList<City>();
		hotCityList=new ArrayList<City>();
		searchCityList=new ArrayList<City>();
		recentCityList=new ArrayList<String>();
		handler = new Handler();
		overlayThread = new OverlayThread();
	}

	private void initAllCityData() {

		City city = new City("定位", "0"); // 当前定位城市
		allCityList.add(city);
		city=new City("最近","1");
		allCityList.add(city);
		city=new City("热门","2");
		allCityList.add(city);
		city=new City("全部","3");
		allCityList.add(city);
		allCityList.addAll(getCityList());
	}

	@SuppressWarnings("unchecked")
	private ArrayList<City> getCityList() {
		SQLiteDatabase db;
		Cursor cursor = null;
		//获取assets目录下的数据库中的所有城市的openHelper
		AllCitySqliteOpenHelper op=new AllCitySqliteOpenHelper(MainActivity.this);
		ArrayList<City> cityList=new ArrayList<City>();
		try {
			op.createDataBase();
			db = op.getWritableDatabase();
			cursor= db.rawQuery("select * from city",null);

			while (cursor.moveToNext()) {
				String cityName=cursor.getString(cursor.getColumnIndex("name"));
				String cityPinyin=cursor.getString(cursor.getColumnIndex("pinyin"));
				City city=new City(cityName,cityPinyin);
				cityList.add(city);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			cursor.close();
		}
		Collections.sort(cityList, comparator);
		return cityList;

	}

	private void initRecentVisitCityData() {
		InsertCity("北京");
		InsertCity("上海");
		InsertCity("广州");
		SQLiteDatabase recentVisitDb = cityOpenHelper.getWritableDatabase();
		Cursor cursor = recentVisitDb.rawQuery("select * from recentcity order by date desc limit 0, 3", null);
		while (cursor.moveToNext()) {
			String recentVisitCityName=cursor.getString(cursor.getColumnIndex("name"));
			recentCityList.add(recentVisitCityName);
		}
		cursor.close();
		recentVisitDb.close();
	}

	private void initHotCityData() {
		City city=new City("北京","2");
		hotCityList.add(city);
		city=new City("上海","2");
		hotCityList.add(city);
		city=new City("广州","2");
		hotCityList.add(city);
		city=new City("南京","2");
		hotCityList.add(city);
		city=new City("合肥","2");
		hotCityList.add(city);
		city=new City("安徽","2");
		hotCityList.add(city);
		city=new City("砀山","2");
		hotCityList.add(city);
		city=new City("日本","2");
		hotCityList.add(city);
	}

	/**Binary
	 * 自定义的排序规则，按照A-Z进行排序
	 */
	@SuppressWarnings("rawtypes")
	Comparator comparator = new Comparator<City>() {
		@Override
		public int compare(City lhs, City rhs) {
			String a = lhs.getPinyin().substring(0, 1);
			String b = rhs.getPinyin().substring(0, 1);
			int flag = a.compareTo(b);
			if (flag == 0) {
				return a.compareTo(b);
			} else {
				return flag;
			}
		}
	};

	@SuppressWarnings("unchecked")
	private void getResultCityList(String keyword) {
		AllCitySqliteOpenHelper dbHelper = new AllCitySqliteOpenHelper(this);
		try {
			dbHelper.createDataBase();
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			Cursor cursor = db.rawQuery(
					"select * from city where name like \"%" + keyword
							+ "%\" or pinyin like \"%" + keyword + "%\"", null);
			City city;
			while (cursor.moveToNext()) {
				String cityName=cursor.getString(cursor.getColumnIndex("name"));
				String cityPinyin=cursor.getString(cursor.getColumnIndex("pinyin"));
				city = new City(cityName,cityPinyin);
				searchCityList.add(city);
			}
			cursor.close();
			db.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//将得到的集合按照自定义的comparator的规则进行排序
		Collections.sort(searchCityList, comparator);
	}

	// 设置显示字母的TextView为不可见
	private class OverlayThread implements Runnable {
		@Override
		public void run() {
			tvDialog.setVisibility(View.INVISIBLE);
		}
	}
	//插入数据到最近访问的城市
	public void InsertCity(String name) {
		SQLiteDatabase db = cityOpenHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("select * from recentcity where name = '"
				+ name + "'", null);
		if (cursor.getCount() > 0) { //
			db.delete("recentcity", "name = ?", new String[] { name });
		}
		db.execSQL("insert into recentcity(name, date) values('" + name + "', "
				+ System.currentTimeMillis() + ")");
		db.close();
	}
}
