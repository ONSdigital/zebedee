package com.github.onsdigital.zebedee.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.Collection;

@Api
public class Publish {

	@POST
	public boolean publish(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		boolean result = false;

		// Locate and publish the collection:
		Collection collection = Collections.getCollection(request);
		if (collection != null) {
			result = Root.zebedee.publish(collection);
		}

		// Change the status code if necessary:
		if (!result) {
			response.setStatus(HttpStatus.BAD_REQUEST_400);
		}

		return result;
	}

}
