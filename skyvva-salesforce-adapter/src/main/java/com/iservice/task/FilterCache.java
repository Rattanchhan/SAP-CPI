package com.iservice.task;

import java.io.Serializable;

import com.sforce.soap.schemas._class.IServices.IBean;

public class FilterCache implements Serializable {
   
   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public long TIMESTART = 0;
   public IBean[][] resources = null;
   
   public FilterCache() {}

   public IBean[][] getResources() {
      return resources;
   }

   public void setResources(IBean[][] resources) {
      this.resources = resources;
   }

   public long getTIMESTART() {
      return TIMESTART;
   }

   public void setTIMESTART(long timestart) {
      TIMESTART = timestart;
   }
   
   
   
}
