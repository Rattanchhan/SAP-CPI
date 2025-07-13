package com.sforce.soap.schemas._class.IServices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.iservice.sforce.SFService;
import com.sforce.soap.SFConnectorConfig;
import com.sforce.soap.SalesForceHttpTransport;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.ExceptionCode;

public class IntegrationService {

	public static Namespace ENV = Namespace.getNamespace("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
	public static Namespace ISER=Namespace.getNamespace("iser","http://soap.sforce.com/schemas/class/IServices");
	public SFService service; 

	public IntegrationService(SFService service){
		this.service = service;
		ISER = Namespace.getNamespace("iser","http://soap.sforce.com/schemas/class/"+service.getPackage()+"IServices");
	}

	public IBean[][] search(final IBean[] filter) throws Exception{

		byte[] resp = doSend(new ICreateBody() {

			@Override
			public Element createBodyElement() {
				Element search = new Element("search",ISER);
				Element efilter = new Element("filter",ISER);
				search.addContent(efilter);
				if(filter!=null){
					for(IBean b:filter){
						Element oneFilter = new Element("oneFilter",ISER);
						efilter.addContent(oneFilter);
						Element name = new Element("name",ISER);
						name.setText(b.getName());
						Element val = new Element("value",ISER);
						val.setText(StringUtils.trimToEmpty(b.getValue()));
						oneFilter.addContent(name);
						oneFilter.addContent(val);
					}
				}

				return search;
			}
		});

		//FileOutputStream out = new  FileOutputStream("d:/test_resp.xml");
		//out.write(resp);
		//out.close();

		Document doc = xmlStreamToDom(new ByteArrayInputStream(resp));
		Element evnRoot = doc.getRootElement();
		Element body = evnRoot.getChild("Body", ENV);
		Element searchResponse = body.getChild("searchResponse",ISER);
		List<Element> records = searchResponse.getChildren();
		IBean[][] result = null;
		if(records!=null){
			result = new IBean[records.size()][];
			for(int i=0;i<records.size();i++){
				Element rec = records.get(i);
				List<Element> oneRecs = rec.getChildren();
				IBean[] row = new IBean[oneRecs.size()];
				for(int j=0;j<oneRecs.size();j++){
					Element oneRec = oneRecs.get(j);
					IBean b = new IBean();
					for(Element eb:oneRec.getChildren()){
						if(StringUtils.equalsIgnoreCase(eb.getName(), "name")){
							b.setName(eb.getText());
						}else{
							b.setValue(eb.getText());
						}
					}
					row[j]=b;
				}
				result[i]=row;
			}
		}
		return result;
	}

	public void integrate(final IIntegration integration) throws Exception {
		doSend(new ICreateBody() {

			@Override
			public Element createBodyElement() {
				Element integrate = new Element("integrate",ISER);
				Element eintegration = new Element("integration",ISER);
				integrate.addContent(eintegration);
				Element fromSystem = new Element("fromSystem",ISER);
				fromSystem.setText(integration.getFromSystem());
				eintegration.addContent(fromSystem);
				Element mappingName = new Element("mappingName",ISER);
				mappingName.setText(integration.getMappingName());
				eintegration.addContent(mappingName);
				Element targetObject = new Element("targetObject",ISER);
				targetObject.setText(integration.getTargetObject());
				eintegration.addContent(targetObject);
				for(IBean[] rec:integration.getRecords()){
					Element record=new Element("records",ISER);
					eintegration.addContent(record);
					for(IBean field:rec){
						Element oneRec = new Element("oneRecord",ISER);
						record.addContent(oneRec);
						Element name = new Element("name",ISER);
						name.setText(field.getName());
						Element val = new Element("value",ISER);
						val.setText(StringUtils.trimToEmpty(field.getValue()));
						oneRec.addContent(name);
						oneRec.addContent(val);
					}
				}

				return integrate;
			}
		});
	}

	protected byte[] doSend(ICreateBody createBody) throws Exception{
		Element evn = createEvelopeElement();
		createSessionHeader(evn);
		Document doc = new Document(evn);
		Element body = new Element("Body",ENV);
		evn.addContent(body);
		body.addContent(createBody.createBodyElement());
		URL targetURL = new URL(service.getEndPoint());
		HttpPost method = createHttpMethod(targetURL.getPath(), HttpPost.class);
		byte[] reqContent = docToByteArray(doc);

		//FileOutputStream out1 = new FileOutputStream("d:/test_req.xml");
		//out1.write(reqContent);
		//out1.close();

		HttpEntity entity =	EntityBuilder.create().setBinary(reqContent).gzipCompress().build();
		method.setEntity(entity );
		HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
		HttpClient client = SalesForceHttpTransport.createHttpClient(method, (SFConnectorConfig)service.getSFPartner().getConfig());
		HttpResponse resp = SalesForceHttpTransport.execute(client, target, method);

		HttpEntity r_entity = resp.getEntity();
		BufferedHttpEntity b_entity = new BufferedHttpEntity(r_entity);

		if(resp.getStatusLine().getStatusCode()!=200){
			ApiFault err= new ApiFault();
			err.setExceptionCode(ExceptionCode.UNKNOWN_EXCEPTION);
			String error = readFromStream(b_entity.getContent());
			List<String> errors = new ArrayList<String>();
			extract(error, "faultstring", errors);
			extract(error, "faultcode", errors);			
			if (errors.size() > 0) {
				error = "";
				for (String tmp : errors) {
					error += tmp + ". ";
				}
			}
			err.setExceptionMessage(error);
			throw err;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		b_entity.writeTo(out);
		byte[] content = out.toByteArray();
		return content;
	}

	protected <M extends HttpRequestBase> M createHttpMethod(String targetURL, Class<M> type) throws Exception {
		M method = type.getConstructor(String.class).newInstance(targetURL);
		method.setHeader("Content-Type", "text/xml");
		method.setHeader("Accept", "text/xml");
		method.setHeader("SOAPAction","\"\"");
		return method;
	}

	protected Element createEvelopeElement(){
		Element evn = new Element("Envelope",ENV);
		evn.addNamespaceDeclaration(ISER);
		return evn;
	}

	/**
	 * Reads the content of an InputStream into a String.
	 * @param is InputStream.
	 * @return Data read from the InputStream.
	 * @throws IOException IO Exception.
	 */
	protected String readFromStream(InputStream is) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(is, "UTF-8");
		try {
			int read;
			do {
				read = in.read(buffer, 0, buffer.length);
				if (read>0) {
					out.append(buffer, 0, read);
				}
			} while (read>=0);
		} finally {
			in.close();
		}
		return out.toString();
	}

	protected void extract(String error, String tag, List<String> errors) {
		int start, end = 0;
		while (true) {
			start = error.indexOf("<" + tag + ">", end);
			if (start == -1) {
				break;
			}
			end = error.indexOf("</" + tag + ">", start);
			if (end == -1) {
				break;
			}
			String tmp = error.substring(start + tag.length() + 2, end);
			tmp = tmp.replaceAll("&apos;", "'").replaceAll("&quot;", "\"");
			if (!errors.contains(tmp)) {
				errors.add(tmp);
			}
		}
	}

	protected Document xmlStreamToDom(InputStream is) throws Exception {
		SAXBuilder builder = new SAXBuilder();
		return builder.build(is);
	}

	/**
	 * Convert document(jdom) to byte array
	 *
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	protected byte[] docToByteArray(Document doc) throws IOException {
		Format format = Format.getPrettyFormat();
		format.setEncoding("UTF-8");
		XMLOutputter xmlOut = new XMLOutputter(format);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		xmlOut.output(doc, out);
		out.flush();
		out.close();
		return out.toByteArray();
	}

	protected Element createSessionHeader(Element soapenv) throws Exception{
		Element header = new Element("Header",ENV);
		Element sessionHeader = new Element("SessionHeader",ISER);
		Element sessionid = new Element("sessionId",ISER);
		sessionid.setText(service.getSFPartner().getConfig().getSessionId());
		sessionHeader.addContent(sessionid);
		header.addContent(sessionHeader);
		soapenv.addContent(header);
		return header;
	}

	public interface ICreateBody{
		public Element createBodyElement();
	}
}
