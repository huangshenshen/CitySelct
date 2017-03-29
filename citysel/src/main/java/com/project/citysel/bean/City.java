package com.project.citysel.bean;

public class City {
	
	private String name;
	private String pinyin;
	
	public City(String name, String pinyin) {
		super();
		this.name = name;
		this.pinyin = pinyin;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPinyin() {
		return pinyin;
	}
	public void setPinyin(String pinyin) {
		this.pinyin = pinyin;
	}

}
