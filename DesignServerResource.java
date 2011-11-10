// JDK refs.
import java.util.*;
import java.util.concurrent.*;
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
 * to handle the requests w.r.t when the VPC designer is in
 * 'design' mode.
 * @see ResourceManager for the URI mappings
 * @author Raymond Tay
 * @version 1.0
 */
public class DesignServerResource extends ServerResource {

	/**
	 * Stores the design on the database collection name "design"
	 * in the db "vpc".
	 *
	 * @param project_id - the project's id
	 * @param design - The XML document representing the current design
	 * @return none
	 */
	@Post
	public void storeDesign(Representation design) throws Exception {
		BasicDBObject project = null;
		Document projectXml = null;
		String id = (String) getRequestAttributes().get("project_id");

		getLogger().log(Level.INFO, "REQ_VPC_STORE_DESIGN_CONFIG => PROJECT ID:" + id);
		
		Mongo m = new Mongo();
		DB db = m.getDB("vpc");
		DBCollection col = db.getCollection("design");
		DBCursor cursor = col.find(new BasicDBObject("project_id", id));

		DomRepresentation dom = new DomRepresentation(design);
		projectXml = dom.getDocument();

		String vpcconfig = getStringFromDOM(projectXml);

		if (cursor.hasNext()) {
			project = (BasicDBObject) cursor.next();
			BasicDBObject newRecord = new BasicDBObject();
			newRecord.put("project_id", id);
			newRecord.put("config_file", projectXml);
			col.update(project, newRecord);
		} else {
			project = new BasicDBObject();
			project.put("project_id", id);
			project.put("config_file", vpcconfig);
			col.insert(project);
		}	
	}

	/**
	 * Retrieves the XML configuration from the collection name
	 * "design" in the db "vpc".
	 *
	 * @param project_id - project's id
	 * @return a XML representing the document
	 */
	@Get("xml")
	public Representation getDesignConfig() throws Exception {
		// extract the data from the database (MongoDB) and return it 
		// as a XML 
		BasicDBObject project = null;
		Document projectXml = null;

		String id = (String)getRequestAttributes().get("project_id");
		getLogger().log(Level.INFO, "REQ_VPC_DESIGN_CONFIG => PROJECT ID:" + id);

		Mongo m = new Mongo();
		DB db = m.getDB("vpc");
		DBCollection col = db.getCollection("design");
		DBCursor cursor = col.find(new BasicDBObject("project_id", id));

		if (cursor.hasNext()) { // expecting to find just 1 document
			project = (BasicDBObject)cursor.next();
			
			// get the XML file
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = fac.newDocumentBuilder();
			projectXml = builder.parse(new InputSource(new StringReader( (String)project.get("config_file") )));
		} else {
			// there's an error here likely, the client should know
			// with a more descriptive message since we're in the
			// pre-production stage
			getResponse().setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND, "The passed in project_id=" + id + " does not exist" + 
							" OR you must have missed the project_id you've passed in u twit");
			getLogger().log(Level.WARNING, "REQ_VPC_DESIGN_CONFIG => PROJECT ID:" + id + " does not exist in the database");
		}	

		// At this point, the resetlet should return a valid XML doc
		// to the requester
		DomRepresentation domModel = new DomRepresentation();
		domModel.setDocument(projectXml);

		return domModel;
	}

	/**
	 * Activates the designed VPC for provisioning
	 * 
	 * @param project_id - project's id
	 * @return a HTTP 200 
	 */
	@Put
	public void activateDesign() throws Exception {
		// extract the data from the database (MongoDB) and return it 
		// as a XML 
		BasicDBObject project = null;
		Document projectXml = null;

		String id = (String)getRequestAttributes().get("project_id");
		getLogger().log(Level.INFO, "REQ_VPC_ACTVATE_CONFIG => PROJECT ID:" + id);

		Mongo m = new Mongo();
		DB db = m.getDB("vpc");
		DBCollection col = db.getCollection("design");
		DBCursor cursor = col.find(new BasicDBObject("project_id", id));

		if (cursor.hasNext()) { // expecting to find just 1 document
			project = (BasicDBObject)cursor.next();
			
			// get the XML file
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = fac.newDocumentBuilder();
			projectXml = builder.parse(new InputSource(new StringReader( (String)project.get("config_file") )));

			ClientResource provision = new ClientResource("http://localhost:8111/vpc/"+id+"/request/");
			provision.post(projectXml, MediaType.APPLICATION_ALL_XML);	
			getLogger().log(Level.INFO, "REQ_VPC_ACTVATE_CONFIG => Provision request sent for PROJECT ID:" + id);
		} 
	}		
	/**
	 * This private implementation is from stackoverflow.com
	 * and i claim no credit for it. In case you wanna know the
	 * context, here's the link 
	 * http://stackoverflow.com/questions/315517/is-there-a-more-elegant-way-to-convert-an-xml-document-to-a-string-in-java-than
	 * @param doc - a XML comformant Document object
	 * @return A string representation of the XML object
	 */
	private String getStringFromDOM(org.w3c.dom.Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory fac = TransformerFactory.newInstance();
			javax.xml.transform.Transformer transformer = fac.newTransformer();
			transformer.transform(domSource, result);
			writer.flush();
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return "";
		}
	}

}
