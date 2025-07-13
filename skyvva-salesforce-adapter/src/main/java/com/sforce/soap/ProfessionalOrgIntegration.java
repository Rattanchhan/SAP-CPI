package com.sforce.soap;

//import org.json.simple.JSONObject;

import com.iservice.sforce.ISFService;
import com.iservice.sforce.SFService;
import com.iservice.task.GenericSFTask;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.soap.schemas._class.IServices.IBean;
import com.sforce.soap.schemas._class.IServices.IIntegration;

public class ProfessionalOrgIntegration {

	private SFService sfService;

	int SCRIPT_LIMIT = 29000;

	public ProfessionalOrgIntegration(SFService sfService) {
		this.sfService = sfService;
	}

	public void integrate(IIntegration integration)
			throws Exception {
/*
		try {

			/* NEW VERSION			 
			 	{
                          "targetObject" : "Upsert Accounts",
                          "records" : [ {
                            "oneRecord" : [ {
                              "value" : "Munich0",
                              "name" : "ACCOUNTNAME"
                            }, {
                              "value" : "Streetname0",
                              "name" : "ACCOUNTSTREET1"
                            } ]
                          }, {
                            "oneRecord" : [ {
                              "value" : "Munich1",
                              "name" : "ACCOUNTNAME"
                            }, {
                              "value" : "Streetname1",
                              "name" : "ACCOUNTSTREET1"
                            } ]
                          }, {
                            "oneRecord" : [ {
                              "value" : "Munich2",
                              "name" : "ACCOUNTNAME"
                            }, {
                              "value" : "Streetname2",
                              "name" : "ACCOUNTSTREET1"
                            } ]
                          } ],
                          "mappingName" : null,
                          "fromSystem" : "a0I900000035ae9EAA"
                        }			 
			 

			PartnerConnection sfServiceBinding = sfService.getSFPartner();			

			String body = "{"
					+ "\"targetObject\" : \""+JSONObject.escape(integration.getTargetObject())+"\",";
			body = body + "\"records\" : [ "; //start records
			for(Integer a=0; a<integration.getRecords().length; a++) {
				//start one record
				IBean[] r =  integration.getRecords()[a];
				body = body + "{\"oneRecord\" : [";
				for(Integer b = 0; b<r.length; b++) {
					IBean bean = r[b]; 
					body = body + "{\"value\" : \""+JSONObject.escape(bean.getValue())
					+"\",\"name\" : \""+JSONObject.escape(bean.getName())+"\"}";
					if(b<r.length-1) {
						body = body + ",";
					}
				}
				body = body + "]}"; //end one record

				if(a<integration.getRecords().length-1) {
					body = body + ",";
				}						
			}
			
			body = body + "],"; //end records  

			body = body + "\"mappingName\" : "+(integration.getMappingName()!=null 
					? "\""+JSONObject.escape(integration.getMappingName())+"\"" : null) + ",";               

			body = body + "\"fromSystem\" : \""+integration.getFromSystem()+"\""+"}"; //end iintegration
			
			String attachmentName = "SKYVVA_AGENT_INTEGRATE_" + integration.getFromSystem() + "_" + System.currentTimeMillis();
			createAttachmentPE(attachmentName, integration.getFromSystem(), body, sfServiceBinding);			

		} catch (Exception ex) {
			LOGGER.error(">integratePE> ERROR> "+ex, ex);
			throw new Exception("Error: " + GenericSFTask.getExceptionDetail(ex));
		}	
		*/
	}

	private String getNamespace(){
		return sfService.getQueryPackage();
	}

	public IBean[][] search(IBean[] filter) throws Exception {
		return null;
/*
		try {

			//20150202 No filter, return
			if(filter==null || filter.length==0) {
				LOGGER.trace(">searchPE>no filter available");
				return null;

			}

			String attachmentName = getAttachmentName(filter);

			filter = addFilter(filter, attachmentName);

			// Search IBean[][] from Attachment and finally delete the attachment
			PartnerConnection sfServiceBinding = sfService.getSFPartner();

			// Search ParentId (integratonId or firstAdapterID)
			String parentId = null;
			// Search adapter, get the first adapter to be parentId
			if(filter[0].getName().equalsIgnoreCase(ISFService.OPERATION_SEARCH_ADAPTER)) {
				String adapterType = null;
				String ids = null;
				if(filter[1].getName().equalsIgnoreCase("type")) adapterType = filter[1].getValue();
				if(filter[2].getName().equalsIgnoreCase("ids")) ids = filter[2].getValue();

				//Must be equivalent to the IServices.search()-> searchAdapter() -> sql
				String sql="select Id from "+getNamespace() + "Adapter__c ";
				String wh="";
				String[] idsL;
				if(adapterType!=null && adapterType.trim()!="")wh+=" "+getNamespace()+"Type__c='"+adapterType+"' AND ";
				if(ids!=null && ids.trim().length()>15){
					idsL=ids.split(",");
					String inIds = "";
					for(String id : idsL) {
						inIds+="'"+id+"',";
					}
					if(inIds.endsWith(","))inIds=inIds.substring(0,inIds.length()-1);
					wh+=" Id In ("+inIds+") AND ";
				}
				if(wh.length()>0){
					wh=wh.substring(0,wh.length()-4);
					sql+=" Where "+wh;
				}
				sql+=" ORDER BY Name LIMIT 1";

				QueryResult qr = sfServiceBinding.query(sql);
				if(qr!=null && qr.isDone() && qr.getSize()>0) {
					SObject obj = qr.getRecords()[0];
					parentId= obj.getId();
				}
			}
			//Other cases
			else {
				for(IBean b : filter) {
					if(b.getName().equalsIgnoreCase(ISFService.INTEGRATION_C)) {
						parentId = b.getValue();
						break;
					}
				}
			}

			/*
			 * NEW VERSION
			

			//If the parentId is NULL -> that means no any adapter for the search filter yet
			if(parentId==null) return null;

			/*
	            {
	                "oneFilter" : [ {
	                  "value" : "SEARCH_ADAPTER",
	                  "name" : "SEARCH_ADAPTER"
	                }, {
	                  "value" : "ODBC",
	                  "name" : "type"
	                } ]
	            }
			 	

			String filterStr = "{\"oneFilter\" : [";
			if(filter!=null && filter.length>0) {
				for(Integer f=0; f<filter.length; f++) {
					IBean b = filter[f];
					filterStr+="{\"value\" : \""+JSONObject.escape(b.getValue())
					+"\",\"name\" : \""+JSONObject.escape(b.getName())+"\"}";
					if(f<filter.length-1) {
						filterStr+=",";
					}
				}
			}
			filterStr+="]}";

			attachmentName = "SKYVVA_AGENT_SEARCH_" + attachmentName;
			createAttachmentPE(attachmentName, parentId, filterStr, sfServiceBinding);			
			return searchAttachment(attachmentName, parentId, sfServiceBinding);

		} catch (Exception ex) {
			LOGGER.error(">searchPE> ERROR> "+ex, ex);
			throw new Exception("Error: " + GenericSFTask.getExceptionDetail(ex));
		}
*/
	}

	private void createAttachmentPE(String attName, String parentId, String body, PartnerConnection binding) throws Exception {
		SObject[] sobjs = new SObject[1];
		SObject sobj = new SObject();
		sobj.setType("Attachment");

		// convert to base64 data to avoid StringException BLOB is not a valid UTF-8 string at SFDC side
		// body = com.Ostermiller.util.Base64.encodeToString(body.getBytes("UTF-8")); //for Asis call
		sobj.setField("Body", body.getBytes("UTF-8"));
		sobj.setField("ContentType", "application/json");
		sobj.setField("ParentId", parentId);
		sobj.setField("Name", attName);

		sobjs[0] = sobj;
		SaveResult[] sr = binding.create(sobjs);
		StringBuffer strErrors = new StringBuffer();
		if(sr != null) {		
			for(int i=0; i<sr.length; i++){
				if(!sr[i].isSuccess() && sr[i].getErrors()!=null){
					for(int j=0; j<sr[i].getErrors().length; j++){
						strErrors.append(sr[i].getId() + " : " + sr[i].getErrors()[j].getMessage() + "\n");
					}
				}
			}
		}
		if(strErrors.length()>0) {
			throw new Exception(strErrors.toString());
		}
	}

	private IBean[][] searchAttachment(String attachmentName, String parentId, PartnerConnection binding) throws Exception {
/*
		String xmlBody = null;
		String attachmentId = null;

		try {

			if(binding==null) {
				binding = sfService.getSFPartner();

			}

			if(attachmentName!=null) {
				String sql = "SELECT Id, Body from Attachment where ParentId!=null and ParentId='"+parentId+
						"' and Name='"+attachmentName+"'";

				QueryResult qr = binding.query(sql);

				if(qr!=null && qr.isDone() && qr.getSize()>0) {

					SObject so = qr.getRecords()[0];
					xmlBody = com.Ostermiller.util.Base64.decode((String) so.getField("Body"));
					attachmentId = so.getId();
				}

				if(xmlBody!=null) {
					XStream xstream = new XStream(new DomDriver());
					xstream.alias("integration", IIntegration.class);
					xstream.alias("item", IBean.class);
					IIntegration i = (IIntegration)xstream.fromXML(xmlBody);
					return i.getRecords();
				}
			}

		} catch (Exception ex) {
			LOGGER.error(">searchAtt> ERROR> "+ex, ex);
			throw new Exception("Error: " + GenericSFTask.getExceptionDetail(ex));
		}
		finally {

			if(attachmentId!=null && binding!=null) {
				try {
					String[] arr = new String[1];
					arr[0] = attachmentId;
					binding.delete(arr);
				} catch (Exception e) {
					LOGGER.error(">searchAtt>deleteAtt>ERROR>"+e.getMessage(),e);
				}
			}
		}
		*/
		return null;
	}

	private String getAttachmentName(IBean[] filter) {
		if(filter!=null && filter.length>0){
			return System.currentTimeMillis() + "_" + filter[0].getName();
		}
		return null;
	}
	
	private IBean[] addFilter(IBean[] filter, String attachmentName) {
		if (filter != null) {
			Integer n = filter.length + 1;
			IBean[] temp = new IBean[n];
			System.arraycopy(filter, 0, temp, 0, filter.length);
			temp[n - 1] = new IBean(ISFService.ATTACHMENT_NAME, attachmentName);
			filter = temp;
			temp = null;
		}
		return filter;
	}

	public static String escapeSingleQuotes(String str) {
		if(str!=null){
			str = str.trim();
			str = str.replaceAll("'", "\\\\'");
		}
		return str; 
	}

	public SFService getSfService() {
		return sfService;
	}

	public void setSfService(SFService sfService) {
		this.sfService = sfService;
	}


}
