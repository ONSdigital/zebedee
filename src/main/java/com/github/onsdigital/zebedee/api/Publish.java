package com.github.onsdigital.zebedee.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Parameter;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.ChangeSet;

@Api
public class Publish {

	@POST
	public boolean publish(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		boolean result = false;

		// Locate and publish the release:
		Path path = Path.newInstance(request);
		int index = Parameter.getId(path);
		List<ChangeSet> changeSets = Root.zebedee.getChangeSets();
		if (index >= 0 && index < changeSets.size()) {
			result = Root.zebedee.publish(changeSets.get(index));
		}

		// Change the status code if necessary:
		if (!result) {
			response.setStatus(HttpStatus.BAD_REQUEST_400);
		}

		return result;
	}

}
