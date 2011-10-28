import java.io.*;
import java.util.*;

public class UseProcessBuilder {
		public static void main(String[] args) throws IOException {
				Vector<String> s = new Vector<String>();
				String[] as = "-c 1 -m 512 -bd 1,160 -dd 1,120 -dd 1,1024 -o rhel5x64 -a oracle11g -a tomcat6 -z HA -st 19:30:00_27/10/2011 -d 01:00:00:00".split(" ");
				s.add("./createvm.sh");
				for( int i = 0; i < as.length ; ++i) 
						s.add(as[i]);

				String aStr = "hello+there+";
				System.out.println(aStr.substring(0, aStr.length()-1));
				ProcessBuilder pb = new ProcessBuilder("./createvm.sh", " -c 1 -m 512 -bd 1,160 -dd 1,120 -dd 1,1024 -o rhel5x64 -a oracle11g -a tomcat6 -z HA -st 19:30:00_27/10/2011 -d 01:00:00:00");
				Process p = pb.start();

				byte[] d = new byte[256];
				BufferedInputStream bis = new BufferedInputStream(p.getInputStream());
				bis.read(d);

				System.out.println(new String(d));
		}
}
