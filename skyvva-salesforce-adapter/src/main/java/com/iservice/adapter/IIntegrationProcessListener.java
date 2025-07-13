package com.iservice.adapter;

public interface IIntegrationProcessListener {
public void onProgress(int numSent,long totalRec);
public boolean isStop();
}
