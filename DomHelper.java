import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

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

// package will likely rest in the gcloud
// package gcloud.dom.util;


public class DomHelper {
		public enum TIER_TYPE { NONE, WEB, APP, DB };
		private Document xmlDoc = null; // This will be set and ready for use
		private TIER_TYPE tier = TIER_TYPE.NONE ; 

		private XPath xpath;

		// Each nodelist will be set in a lazy fashion and returned 
		private Vector<Machine> machinesWeb;
		private Vector<Machine> machinesApp;
		private Vector<Machine> machinesDB;

		private NodeList firewallWeb;
		private NodeList firewallApp;
		private NodeList firewallDB;

		private NodeList loadbalancerWeb;
		private NodeList loadbalancerApp;
		private NodeList loadbalancerDB;

		/**
		 * The constructor will attempt to parse the XML file
		 * and store it as a instance for further processing if 
		 * needed.
		 *
		 * beware that if a xml is malformed, then 
		 */
		public DomHelper(Document xmlDoc) {
			this.xpath  = XPathFactory.newInstance().newXPath();
			this.xmlDoc = xmlDoc;
		}

		/**
		 * Internal method. Returns list of nodes depending which "tier" it wants
		 */
		private NodeList mineFirewallData(TIER_TYPE type) {
			XPathExpression expr = null;
			NodeList l = null;
			try {	
				switch(type) {
					case WEB: 
						expr = xpath.compile("/system/compartment-web/firewall");
						l = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						break;
					case APP: 
						expr = xpath.compile("/system/compartment-app/firewall");
						l = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						break;
					case DB: 
						expr = xpath.compile("/system/compartment-db/firewall");
						l = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						break;
				}
			} catch (Exception e) {}
			return l;
		}

		/**
		 * Internal method. Returns list of nodes depending which "tier" it wants
		 */
		private Vector<Machine> mineResourceData(TIER_TYPE type) {
			XPathExpression expr = null;
			NodeList list = null;
			Vector<Machine> data = new Vector<Machine>();
			try {	
				switch (type) {
					case WEB: 
						expr = xpath.compile("/system/compartment-web/machines/machine");
						list = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						xpath.reset();
						break;
					case APP: 
						expr = xpath.compile("/system/compartment-app/machines/machine");
						list = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						xpath.reset();
						break;
					case DB: 
						expr = xpath.compile("/system/compartment-db/machines/machine");
						list = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						xpath.reset();
						break;
				}
			} catch (Exception e) { e.printStackTrace(); }

			// mine the returned data, stuff container with a bunch of "Machine" types as in the
			// XML
				
			for( int i = 0; i < list.getLength(); ++i ) {
				Node resourceNode = list.item(i);
				XPath localxpath = XPathFactory.newInstance().newXPath();
				Vector<Disk> disks = new Vector<Disk>();
				Vector<Application> apps = new Vector<Application>();

				try{	
					String machineId = (String) localxpath.evaluate("id/text()", resourceNode, XPathConstants.STRING);
					String machineType = (String) localxpath.evaluate("type/text()", resourceNode, XPathConstants.STRING);
					System.out.println("Machine: " + machineType);
					int cores = Integer.parseInt( (String) localxpath.evaluate("cpu-core/text()", resourceNode, XPathConstants.STRING) );
					System.out.println("Cores: " + cores);
					long memory = Long.parseLong( (String) localxpath.evaluate("memory/text()", resourceNode, XPathConstants.STRING) );
					System.out.println("Memory: " + memory);
					String os = (String) localxpath.evaluate("os/text()", resourceNode, XPathConstants.STRING);
					String ip = (String) localxpath.evaluate("ext-ip-address/text()", resourceNode, XPathConstants.STRING);
					NodeList diskNodes = (NodeList) localxpath.evaluate("disks/disk", resourceNode, XPathConstants.NODESET);
					NodeList applicationNodes = (NodeList) localxpath.evaluate("applications/application", resourceNode, XPathConstants.NODESET);

					for( int j = 0; j < diskNodes.getLength(); ++j ) {
						Node disk = diskNodes.item(j);

						String diskId = (String) localxpath.evaluate("id/text()", disk, XPathConstants.STRING);
						String diskType = (String) localxpath.evaluate("type/text()", disk, XPathConstants.STRING);
						long diskSize = Long.parseLong( (String) localxpath.evaluate("size/text()", disk, XPathConstants.STRING) );
						boolean isBootDisk = (Boolean) localxpath.evaluate("boot/text()", disk, XPathConstants.BOOLEAN); // nice thing about XPath is that if a node is absent, no errors are returned .. rather the default value for the converted type is returned. Hooray!

						// using the builder pattern, we construct the new disk and add it to the container
						Disk newdisk = new DiskBuilder().setId(diskId).setType(diskType).setSize(diskSize).setBootDisk(isBootDisk).build();
						disks.add(newdisk);
						System.out.println("DISK: " + diskType + "," + diskSize +", " + isBootDisk);
					}

					for( int j = 0; j < applicationNodes.getLength(); ++j ) {
						Node appNode = applicationNodes.item(j);

						String id = appNode.getTextContent() ;
						Application newapp = new ApplicationBuilder().setId(id).build();
						apps.add(newapp);
					}

					/**
					 * At this point, we should have discovered the elements of the xml vpc configuration file
					 * and we'll build up the structure for the resource and add it to the container and pass
					 * it back to the requester
					 */
					Machine newMachine = new MachineBuilder()
							.setId(machineId)
							.setExtIPAddr(ip)
							.setType(machineType)
							.setCores(cores)
							.setMemory(memory)
							.setDisks(disks)
							.setOS(os)
							.setApps(apps)
							.build();
					data.add(newMachine);
				} catch (XPathExpressionException e) {e.printStackTrace();}

			}

			return data;
		}
		/**
		 * Internal method. Returns list of nodes depending which "tier" it wants
		 */
		private NodeList mineLoadBalancerData(TIER_TYPE type) {
			XPathExpression expr = null;
			NodeList l = null;
			try {	
				switch (type) {
					case WEB: 
						expr = xpath.compile("/system/compartment-web/load-balancer");
						l = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						break;
					case APP: 
						expr = xpath.compile("/system/compartment-app/load-balancer");
						l = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						break;
					case DB: 
						expr = xpath.compile("/system/compartment-db/load-balancer");
						l = (NodeList)expr.evaluate(this.xmlDoc, XPathConstants.NODESET);
						break;
				}
			} catch (Exception e) {}
			return l;
		}

		/**
		 * Internal method. Returns list of nodes depending which "tier" it wants
		 */
		private String mineZoneData(TIER_TYPE type) {
			XPathExpression expr = null;
			String s = null;
			try {	
				switch (type) {
					case WEB: 
						expr = xpath.compile("/system/compartment-web/zone/text()");
						s = (String)expr.evaluate(this.xmlDoc, XPathConstants.STRING);
						break;
					case APP: 
						expr = xpath.compile("/system/compartment-app/zone/text()");
						s = (String)expr.evaluate(this.xmlDoc, XPathConstants.STRING);
						break;
					case DB: 
						expr = xpath.compile("/system/compartment-db/zone/text()");
						s = (String)expr.evaluate(this.xmlDoc, XPathConstants.STRING);
						break;
				}
			} catch (Exception e) {}
			return s;
		}



		/**
		 * Returns a DOM where the root node is tagged by "firewall"
		 */
		public NodeList getFirewallData(TIER_TYPE type) {
			switch (type) {
				case WEB: if (this.firewallWeb != null) return this.firewallWeb;else { this.firewallWeb = mineFirewallData(TIER_TYPE.WEB); return this.firewallWeb; } 	
				case APP: if (this.firewallApp != null) return this.firewallApp;else { this.firewallApp = mineFirewallData(TIER_TYPE.APP); return this.firewallApp; }
				case DB : if (this.firewallDB != null) return this.firewallDB;  else { this.firewallDB = mineFirewallData(TIER_TYPE.DB); return this.firewallDB; }
			    default: return null;	
			}
		}
		/**
		 * Returns a DOM where the root node is tagged by "resources"
		 */
		public Vector<Machine> getMachineData(TIER_TYPE type) {
			switch (type) {
				case WEB: if (this.machinesWeb != null) return this.machinesWeb;else { this.machinesWeb = mineResourceData(TIER_TYPE.WEB); return this.machinesWeb; } 	
				case APP: if (this.machinesApp != null) return this.machinesApp;else { this.machinesApp = mineResourceData(TIER_TYPE.APP); return this.machinesApp; }
				case DB : if (this.machinesDB != null) return this.machinesDB;  else { this.machinesDB = mineResourceData(TIER_TYPE.DB); return this.machinesDB; }
			    default: return null;	
			}
		}

		/**
		 * Returns a DOM where the root node is tagged by "loadbalancer"
		 */
		public NodeList getLoadBalancerData(TIER_TYPE type) {
			switch (type) {
				case WEB: if (this.loadbalancerWeb != null) return this.loadbalancerWeb;else { this.loadbalancerWeb = mineLoadBalancerData(TIER_TYPE.WEB); return this.loadbalancerWeb; } 	
				case APP: if (this.loadbalancerApp != null) return this.loadbalancerApp;else { this.loadbalancerApp = mineLoadBalancerData(TIER_TYPE.APP); return this.loadbalancerApp; }
				case DB : if (this.loadbalancerDB != null) return this.loadbalancerDB;  else { this.loadbalancerDB = mineLoadBalancerData(TIER_TYPE.DB); return this.loadbalancerDB; }
			    default: return null;	
			}
		}
		/**
		 * Returns a DOM where the root node is tagged by "zone"
		 */
		public String getZoneData(TIER_TYPE type) {
				String zone = "";
				switch (type) {
						case WEB: 
						case APP:
						case DB: zone = mineZoneData(type);
						default: break;
				}
				return zone;
		}
}

/**
 * The classes below is derived from the XML configuration file
 * so you should refer to that. Each resource type will 
 * have its accompany builder object (ref. Builder Pattern)
 * since bloody Java doesn't have default parameter values ...
 * admittedly, you can argue its elaborate but its the only way i can
 * do 
 * Disk d = new DiskBuilder().setType("1").setSize(160).setBootDisk(true).build();
 */

class Disk {
	private String type;
	private long size;
	private boolean bootDisk;
	private String diskId;

	/**
	 * Builds the Java object based on the info in
	 * <disk>
	 *  <id>disk-0</id> // <- attribute added on 31 oct'2011
	 * 	<type>1</type>
	 * 	<size>3</size>
	 * 	<boot>5</boot>
	 * </disk>
	 */
	public Disk(DiskBuilder diskBuilder) {
		this.type     = diskBuilder.getType();
		this.size     = diskBuilder.getSize();
		this.bootDisk = diskBuilder.getBootDisk();
		this.diskId   = diskBuilder.getDiskId();
	}
	public String getType() {return this.type;}
	public long getSize() {return this.size;}
	public boolean getBootDisk() {return this.bootDisk;}
	public String getDiskId() { return this.diskId; }

	public void setType(String type) {
		this.type = type;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public void setBootDisk(boolean bootDisk) {
		this.bootDisk = bootDisk;
	}
	public void setId(String diskId) {
		this.diskId = diskId;
	}
}
class DiskBuilder {
	private String type;
	private long size;
	private boolean bootDisk;
	private String diskId;
	
	public String getType() {return this.type;}
	public long getSize() {return this.size;}
	public boolean getBootDisk() {return this.bootDisk;}
	public String getDiskId() { return this.diskId; }

	public DiskBuilder setType(String type) {
		this.type = type;
		return this;
	}
	public DiskBuilder setSize(long size) {
		this.size = size;
		return this;
	}
	public DiskBuilder setBootDisk(boolean bootDisk) {
		this.bootDisk = bootDisk;
		return this;
	}
	public DiskBuilder setId(String diskId) {
		this.diskId = diskId;
		return this;
	}
	public Disk build() {
		return new Disk(this);
	}	
}

class Application{
	private String id;
	public String getId() { return this.id; }
	public void setId(String id) { this.id = id; }

	/**
	 * Builds the Java object based on the info in the XML
	 * <application>
	 * 	IIS7
	 * </application>
	 */
	public Application(ApplicationBuilder builder) {
		this.id = builder.getId();
	}
}

class ApplicationBuilder {
	private String id;
	public String getId() { return this.id; }
	public ApplicationBuilder setId(String id) { this.id = id; return this;}
	public Application build() { return new Application(this); }
}

class Machine {
	private String id;
	private String type;
	private int	processingCores; // integer should be fine for the forseeable future unless sizeof(cpus in cloud) > 2^32
	private long memory; // not really realistic since its possible that we'll have more than 2^64
	private Vector<Disk> disks;
	private String os;	// operating system name
	private Vector<Application> applications;	
	private String externalIP;

	/**
	 * Builds the Java object based on the info in the XML
	 * <machines>
	 * 	<machine>
	 * 	    <id>vm-0</id>
	 * 		<type>VM</type>
	 * 		<cpu-core>3</cpu-core>
	 * 		<memory>5</memory>
	 * 		<ext-ip-address>202.146.255.1</ext-ip-address>
	 *		<disks>
	 *		 	<disk>
	 * 				<type>1</type>
	 * 				<size>1</size>
	 * 				<boot>1</boot>
	 * 			</disk>
	 * 			...
	 * 		</disks>
	 * 		<os>LINUX</os>
	 * 		<applications>
	 * 			<application>
	 * 				IIS
	 * 			</application>
	 * 			<application>
	 * 				Oracle 11G
	 * 			</application>
	 * 			...
	 * 		</applications>
	 * 	</machine>
	 * </machines>
	 */
	public Machine(MachineBuilder builder) {
		this.id = builder.getId();
		this.type = builder.getType();
		this.processingCores = builder.getCores();
		this.memory = builder.getMemory();
		this.disks = builder.getDisks();
		this.os = builder.getOS();
		this.applications = builder.getApps();
		this.externalIP = builder.getExtIPAddr();
	}
	public String getId() { return this.id; }
	public String getType() { return this.type; }
	public int getCores() { return this.processingCores; }
	public long getMemory() { return this.memory; }
	public Vector<Disk> getDisks() { return this.disks; }
	public String getOS() { return this.os ; }
	public Vector<Application> getApps() { return this.applications; }
	public String getExtIPAddr() { return this.externalIP; }

	public void setType(String type) { this.type = type;}
	public void setId(String id) { this.id = id; }
	public void setCores(int processingCores) { this.processingCores = processingCores; }
	public void setMemory(long memory) { this.memory = memory; }
	public void setDisks(Vector<Disk> disks) { this.disks = disks; }
	public void setOS(String os) { this.os = os; }
	public void setApps(Vector<Application> applications) { this.applications = applications; }
	public void setExtIPAddr(String ip) { this.externalIP = ip; }
}
class MachineBuilder {
	private String id;
	private String type;
	private int	processingCores; // integer should be fine for the forseeable future unless sizeof(cpus in cloud) > 2^32
	private long memory; // not really realistic since its possible that we'll have more than 2^64
	private Vector<Disk> disks;
	private String os;	// operating system name
	private Vector<Application> applications;	
	private String externalIP;

	public String getId() { return this.id; }
	public String getType() { return this.type; }
	public int getCores() { return this.processingCores; }
	public long getMemory() { return this.memory; }
	public Vector<Disk> getDisks() { return this.disks; }
	public String getOS() { return this.os ; }
	public Vector<Application> getApps() { return this.applications; }
	public String getExtIPAddr() { return this.externalIP; }

	public MachineBuilder setId(String id) { this.id = id; return this; }
	public MachineBuilder setType(String type) { this.type = type; return this; }
	public MachineBuilder setCores(int processingCores) { this.processingCores = processingCores; return this; }
	public MachineBuilder setMemory(long memory) { this.memory = memory; return this; }
	public MachineBuilder setDisks(Vector<Disk> disks) { this.disks = disks; return this; }
	public MachineBuilder setOS(String os) { this.os = os; return this; }
	public MachineBuilder setApps(Vector<Application> applications) { this.applications = applications; return this; }
	public MachineBuilder setExtIPAddr(String ip) { this.externalIP = ip; return this; }

	public Machine build() { return new Machine(this); }
}

