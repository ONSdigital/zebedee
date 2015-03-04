package com.github.onsdigital.zebedee.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.ChangeSet;

@Api
public class Publish {

	@POST
	public boolean publish(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		boolean result = false;

		// Locate and publish the change set:
		ChangeSet changeSet = ChangeSets.getChangeSet(request);
		if (changeSet != null) {
			result = Root.zebedee.publish(changeSet);
		}

		// Change the status code if necessary:
		if (!result) {
			response.setStatus(HttpStatus.BAD_REQUEST_400);
		}

		return result;
	}

}
