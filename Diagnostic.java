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

		if (cursor.hasNext()) { // expecting to find just 1 document
			project = (BasicDBObject)cursor.next();

			ArrayList<String> webTier = (ArrayList<String>) project.get("moab_web_rsrv_id");			
			ArrayList<String> appTier = (ArrayList<String>) project.get("moab_app_rsrv_id");			
			ArrayList<String> dbTier = (ArrayList<String>) project.get("moab_db_rsrv_id");			

			ArrayList< Callable< Vector<String> > > tasks = new ArrayList< Callable< Vector<String> > >();

			for( String vpcId : webTier ) {
				getLogger().log(Level.INFO, "Adding WEB task: " + vpcId + tasks.size());
				tasks.add(new StatusTask(vpcId));
			}
			List<Future> results = getApplication().getTaskService().invokeAll(tasks);
			doc = new StatusDocument(results);
			tasks.clear(); // we're going to reuse this.

			for( String vpcId : appTier ) {
				getLogger().log(Level.INFO, "Adding APP task: " + vpcId + tasks.size());
				tasks.add(new StatusTask(vpcId));
			}
			results = getApplication().getTaskService().invokeAll(tasks);
			doc.append(results);
			tasks.clear(); // we're going to reuse this.

			for( String vpcId : dbTier ) {
				getLogger().log(Level.INFO, "Adding DB task: " + vpcId + tasks.size());
				tasks.add(new StatusTask(vpcId));
			}
			results = getApplication().getTaskService().invokeAll(tasks);
			doc.append(results);
			tasks.clear(); // we're not going to reuse this, but no harm i guess.
		}

		DomRepresentation domModel = new DomRepresentation();
		domModel.setDocument(doc.build());
		return domModel;
	}
}

class Status {
	private String vpcId;  // the MOAB id e.g. vpc.16
	private String status; // a string either "ok" or "failed"
	private String state; // some description indicating state

	public Status(StatusBuilder builder) {
		this.vpcId = builder.getVpcId();
		this.status = builder.getStatus();
		this.state = builder.getState();
	}

	public void setVpcId(String vpcId) { this.vpcId = vpcId; }
	public void setStatus(String status) { this.status = status; }
	public void setState(String state) { this.state = state;  }

	public String getVpcId() { return this.vpcId; }
	public String getStatus() { return this.status; }
	public String getState() { return this.state; }
}

class StatusBuilder {
	private String vpcId;  // the MOAB id e.g. vpc.16
	private String status; // a string either "ok" or "failed"
	private String state; // some description indicating state

	public StatusBuilder setVpcId(String vpcId) { this.vpcId = vpcId; return this; }
	public StatusBuilder setStatus(String status) { this.status = status; return this; }
	public StatusBuilder setState(String state) { this.state = state; return this; }

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
			Element child1 = doc.createElement("vpc");
			Text text1 = doc.createTextNode(vpcId);
			Element child2 = doc.createElement("status");
			Text text2 = doc.createTextNode(status);

			child1.appendChild(text1);
			child2.appendChild(text2);
			child.appendChild(child1);
			child.appendChild(child2);
			elems.add(child);
		}
	}

	public void append(List<Future> results) throws Exception {
		for(Future f : results ) {
			Element child = doc.createElement("machine") ;
			Vector<String> v = (Vector<String>) f.get(8, TimeUnit.SECONDS);
			String vpcId = v.get(0);
			String status = v.get(1);
			Element child1 = doc.createElement("vpc");
			Text text1 = doc.createTextNode(vpcId);
			Element child2 = doc.createElement("status");
			Text text2 = doc.createTextNode(status);

			child1.appendChild(text1);
			child2.appendChild(text2);
			child.appendChild(child1);
			child.appendChild(child2);
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
	protected StatusTask(String vpcId) {
		this.vpcId = vpcId; 
	}
	@Override
	public Vector<String> call() throws Exception {
		ProcessBuilder pb = new ProcessBuilder("./getvminfo.sh", vpcId);
		Process proc = pb.start();	

		BufferedInputStream bis = new BufferedInputStream(proc.getInputStream());
		byte[] data = new byte[256]; // TODO: bad idea to have static arrays.
		bis.read(data, 0, 255);
		String retdata = (new String(data)).trim(); // this trimming is necessary otherwise you'll get the "Content not allowed in trailing section."
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = fac.newDocumentBuilder();

		Document xmlDoc = builder.parse(new ByteArrayInputStream(retdata.getBytes()));  
		XPath xpath = XPathFactory.newInstance().newXPath();
		String status = (String) xpath.evaluate("//request/status/text()", xmlDoc, XPathConstants.STRING);
		Vector<String> v = new Vector<String>();
		v.add(0, vpcId);
		v.add(1, status);// either "ok" or "failed"
		return v;
	}
}

