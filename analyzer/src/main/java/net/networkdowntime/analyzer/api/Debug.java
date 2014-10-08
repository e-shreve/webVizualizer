package net.networkdowntime.analyzer.api;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
@Path("/debug")
public class Debug {

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getIt() {
		String retval = "Debug Page<br><br>";
		for (Method method : this.getClass().getMethods()) {
			retval += "<a href=\"/api/debug/" + method.getName() + "\">" + method.getName() + "</a><br>";
		}
		return retval;
	}

	@GET
	@Path("/path")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, Object> path() {
		HashMap<String, Object> response = new HashMap<String, Object>();
		response.put("path", System.getProperty("user.dir"));
		return response;
	}

	@GET
	@Path("/echoString")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public HashMap<String, Object> echoString(@QueryParam("input") String input) {
		HashMap<String, Object> response = new HashMap<String, Object>();
		response.put("output", input);
		return response;
	}

	@GET
	@Path("/getVoid")
	@Produces(MediaType.APPLICATION_JSON)
	public void getVoid(@Context final HttpServletResponse response) {
		response.setStatus(Response.Status.NOT_FOUND.ordinal());
	}

	@GET
	@Path("/getStringArr")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getStringArr() {
		List<String> retval = new ArrayList<String>();
		retval.add("String1");
		retval.add("String2");
		retval.add("String3");
		return retval;
	}
	
	@GET
	@Path("/getString")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getString() {
		return "String";
	}

	@GET
	@Path("/getHashMap")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, String> getHashMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");
		return map;
	}

}