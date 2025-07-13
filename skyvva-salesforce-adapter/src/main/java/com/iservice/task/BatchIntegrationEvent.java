package com.iservice.task;

import com.sforce.soap.schemas._class.IServices.IIntegration;

public class BatchIntegrationEvent extends NormalIntegrationEvent {

	public BatchIntegrationEvent(long totalRecord,MapIntegrationInfo intInfo) {
		super(totalRecord,intInfo);
		
	}

	@Override
	protected IIntegration createIntegration() {
		
		IIntegration itg= super.createIntegration();
		
		itg.setMappingName("integrateBatch()");
		return itg;
	}

}
