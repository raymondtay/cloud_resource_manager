import com.mongodb.*;
import java.util.*;
import java.io.*;
import org.custommonkey.xmlunit.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

public class DBDriver {

		public static void main(String[] args) throws Exception {
			Mongo m = new Mongo();
			DB db = m.getDB("vpc");

			Set<String> col = db.getCollectionNames();
			for(String s: col) {
					System.out.println(s);
			}

			DBCollection collection = db.getCollection("configuration");
			collection.drop();
			BasicDBObject rec = new BasicDBObject();
			rec.put("project_id", "BCA_internal_001");
			rec.put("config_file", "<configuration> <projectId>345</projectId> <compartmentWeb> <zone>MAZ</zone> <firewall> <rule> <sourceAddr>*</sourceAddr> <destinationAddr>*</destinationAddr>          <destinationPort>80</destinationPort> <protocol>TCP</protocol> <direction>IN</direction> </rule> <rule> <sourceAddr>202.193.223.1</sourceAddr> <destinationAddr>*</destinationAddr> <destinationPort>22</destinationPort> <protocol>TCP</protocol> <direction>IN</direction> </rule> </firewall>   <loadBalancer> </loadBalancer>          <resources> <resource> <type>VM</type> <cores>2</cores> <memory>4</memory> <disks> <disk> <type>1</type> <size>160</size> <boot>true</boot> </disk> <disk> <type>2</type> <size>80</size> </disk> </disks> <os>WIN2008R2</os> <applications> <application> <id>IIS75</id> </application> </applications> </resource> </resources> </compartmentWeb> <compartmentApp> <zone>HAZ</zone> <firewall> <rule> <sourceAddr>*</sourceAddr> <destinationAddr>*</destinationAddr> <destinationPort>3361</destinationPort> <protocol>TCP</protocol> <direction>IN</direction> </rule> </firewall> <resources> <resource> </resource> </resources> </compartmentApp>               <compartmentDB> <zone>HAZ</zone> <resources> <resource> <type>PM</type> <cores>12</cores> <memory>32</memory> <disks> <disk> <type>1</type> <size>160</size>            <boot>true</boot> </disk> <disk> <type>2</type> <size>2048</size> </disk> </disks> <os>RHEL6</os> <applications> <application> <id>ORACLE11G</id> </application> </applications> </resource> </resources> </compartmentDB> </configuration>");
			rec.put("moab_web_rsrv_id", "0");
			rec.put("moab_app_rsrv_id", "0");
			rec.put("moab_db_rsrv_id", "0");
			collection.insert(rec);

			// find the inserted document from the collection 'configuration'
			DBCursor cursor = collection.find(new BasicDBObject("project_id", "BCA_internal_001"));
			BasicDBObject oldRec = (BasicDBObject)cursor.next();

			String orgXML = (String)oldRec.get("config_file");
			String newXML = "<configuration> <projectId>34512313231</projectId> <compartmentWeb> <zone>MAZ</zone> <firewall> <rule> <sourceAddr>*</sourceAddr> <destinationAddr>*</destinationAddr>          <destinationPort>80</destinationPort> <protocol>TCP</protocol> <direction>IN</direction> </rule> <rule> <sourceAddr>202.193.223.1</sourceAddr> <destinationAddr>*</destinationAddr> <destinationPort>22</destinationPort> <protocol>TCP</protocol> <direction>IN</direction> </rule> </firewall>   <loadBalancer> </loadBalancer>          <resources> <resource> <type>VM</type> <cores>2</cores> <memory>4</memory> <disks> <disk> <type>1</type> <size>160</size> <boot>true</boot> </disk> <disk> <type>2</type> <size>80</size> </disk> </disks> <os>WIN2008R2</os> <applications> <application> <id>IIS75</id> </application> <application><id>Oracle</id></application></applications> </resource> </resources> </compartmentWeb> <compartmentApp> <zone>HAZ</zone> <firewall> <rule> <sourceAddr>*</sourceAddr> <destinationAddr>*</destinationAddr> <destinationPort>3361</destinationPort> <protocol>TCP</protocol> <direction>IN</direction> </rule> </firewall> <resources> <resource> </resource> </resources> </compartmentApp>               <compartmentDB> <zone>HAZ</zone> <resources> <resource> <type>PM</type> <cores>12</cores> <memory>32</memory> <disks> <disk> <type>1</type> <size>160</size>            <boot>true</boot> </disk> <disk> <type>2</type> <size>2048</size> </disk> </disks> <os>RHEL6</os> <applications> <application> <id>ORACLE11G</id> </application> </applications> </resource> </resources> </compartmentDB> </configuration>";
             DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
             DocumentBuilder builder = fac.newDocumentBuilder();
  
             Document orgXml = builder.parse(new ByteArrayInputStream( orgXML.getBytes() ));
             Document newXml = builder.parse(new ByteArrayInputStream( newXML.getBytes() ));
 
             DetailedDiff diff = new DetailedDiff(new Diff( orgXML, newXML));
             List<Difference> all = diff.getAllDifferences();
             for( Difference d : all ) {
                 XPath xpath = XPathFactory.newInstance().newXPath();
                 Node node = (Node) xpath.evaluate(d.getTestNodeDetail().getXpathLocation(), newXml, XPathConstants.NODE);
                 System.out.println(node.getNodeName() + ", " + node.getTextContent() + ", " + d.getTestNodeDetail().getXpathLocation());
            }

			BasicDBObject newRec = new BasicDBObject();
			newRec.put("project_id", "BCA_internal_001");
			newRec.put("config_file", "NON XML DATA");
			newRec.put("moab_web_rsrv_id", "0");
			newRec.put("moab_app_rsrv_id", "0");
			newRec.put("moab_db_rsrv_id", "0");
			
			collection.update(oldRec, newRec);
			
		}

}


