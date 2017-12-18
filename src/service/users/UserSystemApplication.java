package service.users;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.routing.Router;
import org.restlet.service.CorsService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import service.events.Event;

public class UserSystemApplication extends Application{
	
	private static DatabaseHandler database = new DatabaseHandler();
	private static Map<String, UserInfo> users = new HashMap<String, UserInfo>();
	private static Gson gson = new Gson();
	
	public Router createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/login", new Restlet() {
			
			public void handle(Request request, Response response) {
				if(request.getMethod() == Method.POST) {
					try {
						String input = request.getEntityAsText();
						input = input.replace("username=", "#");
						input = input.replace("&password=", "#");
						input = input.replace("}", "");
						String[] substr = input.split("#");
						UserInfo profile = database.getUserProfile(substr[1], substr[2]);
						if(profile != null) {
							users.put(profile.getName(), profile);
							String link = request.getResourceRef().getPath() + "/users/" + profile.getName();
							response.setEntity("{\"user\" : \"" + profile.getName()
											+ "\",\"link\" : \"" + link + "\"}" , MediaType.APPLICATION_JSON);
							response.setStatus(Status.SUCCESS_OK);
						}
						else {
							response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
						}
					}
					catch(Exception e) {
						response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					}
				}
				else {
					response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			}
		});
		
		router.attach("/login/users/{user_name}", new Restlet() {
			
			public void handle(Request request, Response response) {
				if(request.getMethod() == Method.GET) {
					String userName = (String) request.getAttributes().get("user_name");
					UserInfo profile = users.get(userName);
					if (profile != null) {
						response.setEntity(gson.toJson(profile), MediaType.APPLICATION_JSON);
						response.setStatus(Status.SUCCESS_OK);
					}
					else {
						response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					}
				}
				else {
					response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			}
		});
		
		router.attach("/register", new Restlet() {
			
			public void handle(Request request, Response response) {
				if(request.getMethod() == Method.POST) {
					String input = request.getEntityAsText();
					try {
						JsonParser parser = new JsonParser();
						JsonObject json = (JsonObject) parser.parse(input);
						String username = json.get("username").getAsString();
						String password = json.get("password").getAsString();
						boolean success = database.insertNewUser(username, password);
						if(success) {
							response.setStatus(Status.SUCCESS_OK);
						}
						else {
							response.setStatus(Status.CLIENT_ERROR_CONFLICT);
						}
					} catch (Exception e) {
						response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					}
					
				}
				else {
					response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			}
		});
		
		router.attach("/add/event/{user_name}", new Restlet() {
			public void handle(Request request, Response response) {
				if(request.getMethod() == Method.POST) {
					String userName = (String) request.getAttributes().get("user_name");
					String req = request.getEntityAsText();
					Event theEvent = gson.fromJson(req, Event.class);
					UserInfo profile = users.get(userName);
					if (profile == null) {
						response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					}
					else {
						database.insertEvent(profile.getID(), gson.toJson(theEvent));
					}
					
				}
				else {
					response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			}
		});

		router.attach("/get/event/{user_name}", new Restlet(){
			public void handle(Request request, Response response){
				if(request.getMethod() == Method.GET){
					String userName = (String) request.getAttributes().get("user_name");
					UserInfo user = users.get(userName);
					Event result = database.getLastEvent(user);
					if (result == null){
						response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					}
					else{
						response.setStatus(Status.SUCCESS_OK);
					}
				}
				else{
					response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			}
		});
		return router;
	}
	
	public static void main(String[] args) throws Exception {
		
		//Add cors to allow browsers to accept the response
		CorsService corsService = new CorsService();
		corsService.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
		corsService.setAllowedCredentials(true);
		UserSystemApplication app = new UserSystemApplication();
		app.getServices().add(corsService);
		
		Component component = new Component();
	    component.getServers().add(Protocol.HTTP, 9001);
	    component.getClients().add(Protocol.HTTP);
	    component.getDefaultHost().attach("", app);
	    component.start();
	}

}
