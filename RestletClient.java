import java.io.*;

import org.restlet.resource.*;
import org.restlet.representation.*;
import org.restlet.ext.xml.*;

public class RestletClient {
		public static void main(String []args) throws IOException {
				ClientResource client = new ClientResource("http://localhost:8111/vpc/BCA_internal_002/request");
				DomRepresentation dom = new DomRepresentation();
				StringWriter sw = new StringWriter();
				sw.write("<configuration><projectId>345</projectId><compartmentWeb><zone>MAZ</zone><firewall><rule><sourceAddr>*</sourceAddr><destinationAddr>*</destinationAddr><destinationPort>80</destinationPort><protocol>TCP</protocol><direction>IN</direction></rule><rule><sourceAddr>202.193.223.1</sourceAddr><destinationAddr>*</destinationAddr><destinationPort>22</destinationPort><protocol>TCP</protocol><direction>IN</direction></rule></firewall><loadBalancer></loadBalancer><resources><resource><type>VM</type><cores>2</cores><memory>4</memory><disks><disk><type>1</type><size>160</size><boot>true</boot></disk><disk><type>2</type><size>80</size></disk></disks><os>WIN2008R2</os><applications><application><id>IIS75</id></application></applications></resource></resources></compartmentWeb><compartmentApp><zone>HAZ</zone><firewall><rule><sourceAddr>*</sourceAddr><destinationAddr>*</destinationAddr><destinationPort>3361</destinationPort><protocol>TCP</protocol><direction>IN</direction></rule></firewall><resources><resource></resource></resources></compartmentApp><compartmentDB><zone>HAZ</zone><resources><resource><type>PM</type><cores>12</cores><memory>32</memory><disks><disk><type>1</type><size>160</size><boot>true</boot></disk><disk><type>2</type><size>2048</size></disk></disks><os>RHEL6</os><applications><application><id>ORACLE11G</id></application></applications></resource></resources></compartmentDB></configuration>");
				dom.write(sw);
				Representation rep = client.post(dom);
		}
}

