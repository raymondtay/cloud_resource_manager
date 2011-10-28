import  org.antlr.stringtemplate.*;

public class UseStringTemplate {
		public static void main(String[] args) {
		StringTemplate c = new StringTemplate(" $hello$ , my name is $name$");
				c.setAttribute("hello", "Hallo");
				c.setAttribute("name", "raymond");
				System.out.println(c.toString());
				c.reset();
				c.setAttribute("hello", "Hallor2");
				c.setAttribute("name", "raymondr2");
				System.out.println(c.toString());

		}
}


