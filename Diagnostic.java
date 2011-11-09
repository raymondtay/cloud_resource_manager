// JDK refs.
import java.util.concurrent.*;
import java.util.*;
import java.io.*;
import java.util.logging.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;

// Third party refs.
import org.custommonkey.xmlunit.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.resource.*;
import org.restlet.service.*;
import org.restlet.ext.xml.*;
import com.mongodb.*;

/**
 * A Restlet interfacing between MOAB and the Front-End
 * Primary purpose is to serve as the end-point for which
 * clients can find out the status of their virtual private 
 * cloud
 *
 * @see ResourceManager for the URI mappings
 * @author Raymond Tay
 * @version 1.0
 */
public class Diagnostic extends ServerResource {

	/**
	 * Gets the status of the vpc matching the project_id
	 * @param project_id 
	 * @return a XML encapsulating the statuses
	 */	
	@Get("xml")
	public Representation getVPCStatus() throws Exception {
		BasicDBObject project = null;
		StatusDocument doc = null; 
		String projectId = (String)getRequestAttributes().get("project_id");
		getLogger().log(Level.INFO, "REQ_VPC_STATUS => PROJECT ID:" + projectId);

		Mongo m = new Mongo();
		DB db = m.getDB("vpc");
		DBCollection col = db.getCollection("configuration");
		DBCursor cursor = col.find(new BasicDBObject("project_id", projectId));

		// get the vpc config file to prepare it for augmentation
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = fac.newDocumentBuilder();
		Document projectXml = null; 

		if (cursor.hasNext()) { // expecting to find just 1 document
			project = (BasicDBObject)cursor.next();

			projectXml = builder.parse(new InputSource(new StringReader( (String)project.get("config_file") ))); 

			ArrayList< HashMap<String,String> > webTier = (ArrayList< HashMap<String,String> >) project.get("moab_web_rsrv_id");			
			ArrayList< HashMap<String,String> > appTier = (ArrayList< HashMap<String,String> >) project.get("moab_app_rsrv_id");			
			ArrayList< HashMap<String,String> > dbTier = (ArrayList< HashMap<String,String> >) project.get("moab_db_rsrv_id");			

			/**
			 * What you're expecting to see in the database is that
			 * " moab_web_rsrv_id : { "hostname-1": "moab-id-1", "hostname-2": "moab-id-2" ...}
			 */
			doc = new StatusDocument();
			for(  HashMap<String,String> kvPair : webTier ) {
				Set<String> keys = kvPair.keySet();
				for( String key : keys )  {
					getLogger().log(Level.INFO, "Adding WEB task: " + key + ":" + kvPair.get(key));
					doc.append(getApplication().getTaskService().submit(new StatusTask(key, kvPair.get(key))));
				}
			}
			for(  HashMap<String,String> kvPair : appTier ) {
				Set<String> keys = kvPair.keySet();
				for( String key : keys )  {
					getLogger().log(Level.INFO, "Adding APP task: " + key + ":" + kvPair.get(key));
					doc.append(getApplication().getTaskService().submit(new StatusTask(key, kvPair.get(key))));
				}
			}
			for(  HashMap<String,String> kvPair : dbTier ) {
				Set<String> keys = kvPair.keySet();
				for( String key : keys )  {
					getLogger().log(Level.INFO, "Adding DB task: " + key + ":" + kvPair.get(key));
					doc.append(getApplication().getTaskService().submit(new StatusTask(key, kvPair.get(key))));
				}
			}
		}

		DomRepresentation domModel = new DomRepresentation();
		domModel.setDocument( DomInjector.makeInstance().build(projectXml, doc.build(), "status") );
		return domModel;
	}
}

/**
 * This class will handle the injection of new nodes into the 
 * DOM object and returns it
 */
class DomInjector {
	private static DomInjector o;
	private DomInjector() {}
	public static DomInjector makeInstance() {
		if (o == null) 
			o = new DomInjector();
		return o;
	}	

	/**
	 * Augments/Injects a new tag into original DOM based on the 
	 * results of another list of DOMs. Unfortunately, this is 
	 * going to be a O(n^2) operation
	 *
	 * @param toDom original DOM to inject new tags
	 * @param fromDOM a DOM that contains the results of the async tasks
	 * @return toDOM the original DOM inject with the new tags
	 *
	 **/
	public Document build(org.w3c.dom.Document toDom, org.w3c.dom.Document fromDom, String tagName) {
		// do nothing if there's nothing to derive from
		if (fromDom == null) return toDom;
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			org.w3c.dom.NodeList nodes = (org.w3c.dom.NodeList) xpath.evaluate("//machines/machine", toDom, XPathConstants.NODESET);
			xpath.reset();
			org.w3c.dom.NodeList nodes2 = (org.w3c.dom.NodeList) xpath.evaluate("/system/machine", fromDom, XPathConstants.NODESET);

			for( int i = 0 ; i< nodes.getLength(); ++i ) {
				Node n = nodes.item(i);
				String machineId = (String) xpath.evaluate("id/text()", n, XPathConstants.STRING);
				Node child = null;
				if ( (child = getMatchingChild(machineId, nodes2)) != null ) {
					// Get a copy of the node to be added and look for the <status>
					// node, retrieve its value and append to the appropriate node in 'toDom'	
					xpath.reset();
					n.appendChild( toDom.importNode( (Node)xpath.evaluate("//status", child, XPathConstants.NODE ), true) );
				}
				xpath.reset();
			}

		} catch (XPathExpressionException xpathe) { xpathe.printStackTrace(); } 
		return toDom;
	}


	private Node getMatchingChild(String stringToMatch, org.w3c.dom.NodeList nodes) {
		Node toreturn = null;
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();

			for( int i = 0 ; i< nodes.getLength(); ++i ) {
				Node n = nodes.item(i);
				String id = (String) xpath.evaluate("hostid/text()", n, XPathConstants.STRING);
				if (stringToMatch.equalsIgnoreCase(id)) {
					toreturn=n;
					break;
				}
				xpath.reset();
			}
		} catch (XPathExpressionException xpathe) { xpathe.printStackTrace(); }
		return toreturn;
	}
}

class Status {
	private String hostId; // basically the hostname or IP4 address
	private String vpcId;  // the MOAB id e.g. vpc.16
	private String status; // a string either "ok" or "failed"
	private String state; // some description indicating state

	public Status(StatusBuilder builder) {
		this.hostId = builder.getHostId();	
		this.vpcId = builder.getVpcId();
		this.status = builder.getStatus();
		this.state = builder.getState();
	}

	public void setHostId(String hostId) { this.hostId = hostId; }
	public void setVpcId(String vpcId) { this.vpcId = vpcId; }
	public void setStatus(String status) { this.status = status; }
	public void setState(String state) { this.state = state;  }

	public String getHostId() { return this.hostId; }
	public String getVpcId() { return this.vpcId; }
	public String getStatus() { return this.status; }
	public String getState() { return this.state; }
}

class StatusBuilder {
	private String hostId; // basically the hostname or IP4 address
	private String vpcId;  // the MOAB id e.g. vpc.16
	private String status; // a string either "ok" or "failed"
	private String state; // some description indicating state

	public StatusBuilder setHostId(String hostId) { this.hostId = hostId; return this; }
	public StatusBuilder setVpcId(String vpcId) { this.vpcId = vpcId; return this; }
	public StatusBuilder setStatus(String status) { this.status = status; return this; }
	public StatusBuilder setState(String state) { this.state = state; return this; }

	public String getHostId() { return this.hostId; }
	public String getVpcId() { return this.vpcId; }
	public String getStatus() { return this.status; }
	public String getState() { return this.state; }

	public Status build() { return new Status(this); }
}

class StatusDocument {
	private Document doc;
	private Element root;
	private Vector<Element> elems;

	/**
	 * Initialize a data structure to keep all the (vpc_id, status)
	 * pairs and the user keeps adding these pairs through the 
	 * call "append(...)" till the call to "build()", where a proper XML
	 * will be formatted and brought back.
	 *
	 * There is a chance that a timeout will occur because the server
	 * might be really too busy to return within the TIMEOUT stipulation
	 * of 8 seconds. In that case, increasing the timeout is not likely to help
	 * since the system's already too busy to response within 8s, what makes you
	 *
	 * @see build
	 * @see append
	 */
	StatusDocument(List<Future> results) throws Exception {
		// start building the document
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = fac.newDocumentBuilder();
		this.doc = docBuilder.newDocument();	
		this.root = doc.createElement("system") ;
		this.elems = new Vector<Element>();

		for(Future f : results ) {
			Element child = doc.createElement("machine") ;
			Vector<String> v = (Vector<String>) f.get(8, TimeUnit.SECONDS);
			String vpcId = v.get(0);
			String status = v.get(1);
			String hostId = v.get(2);
			Element child1 = doc.createElement("vpc");
			Text text1 = doc.createTextNode(vpcId);
			Element child2 = doc.createElement("status");
			Text text2 = doc.createTextNode(status);
			Element child3 = doc.createElement("hostid");
			Text text3 = doc.createTextNode(hostId);

			child1.appendChild(text1);
			child2.appendChild(text2);
			child3.appendChild(text3);
			child.appendChild(child1);
			child.appendChild(child2);
			child.appendChild(child3);
			elems.add(child);
		}
	}

	StatusDocument() throws Exception {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = fac.newDocumentBuilder();
		this.doc = docBuilder.newDocument();	
		this.root = doc.createElement("system") ;
		this.elems = new Vector<Element>();
	}

	public void append(Future f) throws Exception {
		Element child = doc.createElement("machine") ;
		Vector<String> v = (Vector<String>) f.get(8, TimeUnit.SECONDS);
		String vpcId = v.get(0);
		String status = v.get(1);
		String hostId = v.get(2);
		Element child1 = doc.createElement("vpc");
		Text text1 = doc.createTextNode(vpcId);
		Element child2 = doc.createElement("status");
		Text text2 = doc.createTextNode(status);
		Element child3 = doc.createElement("hostid");
		Text text3 = doc.createTextNode(hostId);

		child1.appendChild(text1);
		child2.appendChild(text2);
		child3.appendChild(text3);
		child.appendChild(child1);
		child.appendChild(child2);
		child.appendChild(child3);	
		elems.add(child);
	}

	public void append(List<Future> results) throws Exception {
		for(Future f : results ) {
			Element child = doc.createElement("machine") ;
			Vector<String> v = (Vector<String>) f.get(8, TimeUnit.SECONDS);
			String vpcId = v.get(0);
			String status = v.get(1);
			String hostId = v.get(2);
			Element child1 = doc.createElement("vpc");
			Text text1 = doc.createTextNode(vpcId);
			Element child2 = doc.createElement("status");
			Text text2 = doc.createTextNode(status);
			Element child3 = doc.createElement("hostid");
			Text text3 = doc.createTextNode(hostId);

			child1.appendChild(text1);
			child2.appendChild(text2);
			child3.appendChild(text3);
			child.appendChild(child1);
			child.appendChild(child2);
			child.appendChild(child3);	
			elems.add(child);
		}
	}

	public org.w3c.dom.Document build() {
		for( Element ele : elems ) 
			this.root.appendChild(ele);
		this.doc.appendChild(this.root);

		return this.doc;	
	}
}

/**
 * Responsible for reaching out to the external 
 * program (possibly on a separate thread) and requesting
 * it executes and returns a message
 *
 * In the implementation, there's something weird going on related
 * to the size of the internal buffer though the consolation is
 * that each thread maintains its IO buffer though making them 
 * anything large >= 1024 bytes means that i get truncated output
 *
 * @author Raymond 
 * @version 1.0
 * @see java.util.concurrent.Callable<V>
 */
class StatusTask implements Callable< Vector<String> > {
	private String vpcId;	
	private String hostId;
	protected StatusTask(String hostId, String vpcId) {
		this.vpcId = vpcId; 
		this.hostId = hostId; 
	}
	@Override
	public Vector<String> call() throws Exception {
		ProcessBuilder pb = new ProcessBuilder("./getvmstatus.sh", " -n " + vpcId);
		Process proc = pb.start();	

		BufferedInputStream bis = new BufferedInputStream(proc.getInputStream());
		byte[] data = new byte[256]; // TODO: bad idea to have static arrays.
		bis.read(data, 0, 255);
		String retdata = (new String(data)).trim(); // this trimming is necessary otherwise you'll get the "Content not allowed in trailing section."
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = fac.newDocumentBuilder();

		Document xmlDoc = builder.parse(new InputSource(new StringReader(retdata)));  
		XPath xpath = XPathFactory.newInstance().newXPath();
		String status = (String) xpath.evaluate("//request/status/text()", xmlDoc, XPathConstants.STRING);
		Vector<String> v = new Vector<String>();
		v.add(0, vpcId);
		v.add(1, status);// either "ok" or "failed"
		v.add(2, hostId);
		return v;
	}
}

