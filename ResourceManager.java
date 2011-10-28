import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

public class ResourceManager extends Application {
		public ResourceManager() {
				setName("ResourceManager");
				setDescription("Interfaces between Adaptive Computing's MOAB and the front-end clients");
				setOwner("HP Labs Singapore");
				setAuthor("Raymond Tay");
		}

		public static void main(String[] args) throws Exception {
				Server rm = new Server(Protocol.HTTP, 8111);
				rm.setNext(new ResourceManager());
				rm.start();
		}

		@Override
		public Restlet createInboundRoot() {
			Router router = new Router(getContext());
			router.attach("http://localhost:8111/vpc/{project_id}/request/", VPCResource.class);
			return router;
		}
}
