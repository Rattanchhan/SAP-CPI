package com.iservice.adapter;

import java.util.List;

import com.sforce.soap.schemas._class.IServices.IBean;

public interface IResultsetMapper {
	
	IBean[][] map(List<List<IBean>> queryList);
}
