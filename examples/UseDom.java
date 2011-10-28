import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.*;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Vector;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class UseDom {
		public static void main(String[] args) throws Exception {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = fac.newDocumentBuilder();
			Document xmlDoc = builder.parse(new File("config.xml"));	

			DomHelper domH = new DomHelper(xmlDoc);
			Vector<Resource> resources = domH.getResourceData(DomHelper.TIER_TYPE.WEB);
			domH.getFirewallData(DomHelper.TIER_TYPE.WEB);
			System.out.println(resources.size());
		}
}

