package com.iservice.adapter;

import java.util.ArrayList;
import java.util.List;

import com.sforce.soap.schemas._class.IServices.IBean;

//Map the query result obtained from Source DB to IBean[][]
public class ResultsetMapper implements IResultsetMapper {

	public IBean[][] map(List<List<IBean>> queryList) {
		// in case no data in the query list
		if (queryList==null || queryList.size()==0) {			
			return null;
		}
		IBean[][] ibeans = new IBean[queryList.size()][];
		for (int i = 0; i < queryList.size(); i++) {
			//20110727
			//List<IBean> temp   =(List<IBean>) queryList.get(i);
			List<IBean> listCol=(List<IBean>) queryList.get(i);
			ibeans[i]=new IBean[listCol.size()];
			for(int col=0;col<listCol.size();col++){
				IBean bean=new IBean();
				bean.setName(listCol.get(col).getName());
				bean.setValue(listCol.get(col).getValue());
				ibeans[i][col] = bean;
			}
		}
		return ibeans;
	}

	public List<List<IBean>> map(IBean[][] beans){
		// in case no data in the beans
		if (beans == null || beans.length == 0) {
			return null;
		}
		List<List<IBean>> lstBeans = new ArrayList<List<IBean>>();
		for(int i=0; i<beans.length; i++){
			IBean[] aRecord = beans[i];			
			List<IBean> aLstRecord = new ArrayList<IBean>();
			for (int j=0; j<aRecord.length; j++){
				String name = aRecord[j].getName();
				String value = aRecord[j].getValue();
				aLstRecord.add(new IBean(name, value));
			}
			lstBeans.add(aLstRecord);
		}
		return lstBeans;
	}
	
}
