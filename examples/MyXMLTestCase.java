

import java.io.File;
import java.io.FileReader;
import java.util.List;

import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.custommonkey.xmlunit.*;

public class MyXMLTestCase extends XMLTestCase {
		    public MyXMLTestCase(String name) {
					        super(name);
							    }
			    public void testForEquality() throws Exception {
						        String myControlXML = "<resource><cores>99</cores></resources>";
						        String myTestXML = "<resource><cores>9</cores></resources>";
							//    assertXMLEqual("comparing test xml to control xml", myControlXML, myTestXML);
							//     assertXMLNotEqual("test xml not similar to control xml", myControlXML, myTestXML);
							DetailedDiff d = new DetailedDiff(new Diff(myControlXML, myTestXML));
							List<Difference> all = d.getAllDifferences();
							for( Difference i : all ) {
									System.out.println("Diff value is -> " + i.getTestNodeDetail().getValue());
									System.out.println("Parent node is -> " + i.getTestNodeDetail().getNode().getParentNode().getNodeName());
							}
														    }
}
