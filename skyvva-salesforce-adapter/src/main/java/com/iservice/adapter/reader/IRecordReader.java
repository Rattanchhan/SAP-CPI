package com.iservice.adapter.reader;
public interface IRecordReader {
	public boolean doIntegration(String criteria) throws Exception;
	public boolean doTestIntegration(String criteria) throws Exception ;
	public QueryResult doTestQuery(String criteria, boolean isTestQuery) throws Exception;
}