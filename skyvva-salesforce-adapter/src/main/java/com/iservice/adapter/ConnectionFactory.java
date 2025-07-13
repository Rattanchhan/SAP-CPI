package com.iservice.adapter;

import com.model.iservice.Adapter;

public class ConnectionFactory {
	private static ConnectionFactory factory;

	private ConnectionFactory() {

	}

	public static ConnectionFactory getInstance() {
		if (factory == null) {
			factory = new ConnectionFactory();
		}
		return factory;
	}

	public IDBConnection createConnection(Adapter ad) throws Exception {

		// new instance, reflection
		// adapter com.iservice.adapter.xxxAdapter
		Class<?> a = Class.forName("com.iservice.adapter." + ad.getConnType() + "Adapter");
		
		IDBConnection aIDBConnection = (IDBConnection) a.newInstance();
		aIDBConnection.setAdapter(ad);
		
		return aIDBConnection;
		
	}

   public IDBConnection createConnection(String adapter) throws Exception {
      // new instance, reflection
      // adapter com.iservice.adapter.xxxAdapter

      Class<?> a = Class.forName(adapter);
      
      IDBConnection aIDBConnection = (IDBConnection) a.newInstance();
    
      
      return aIDBConnection;
      
   }
}
