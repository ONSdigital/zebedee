package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.ResultMessage;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
public class Publish {

	/**
	 * Creates or updates collection details the endpoint <code>/Publish/[CollectionName]</code>
	 * <p>Marks a content item complete</p>
	 *
	 *
	 * @param request This should contain a X-Florence-Token header for the current session
	 * @param response <ul>
	 *                 <li>If collection does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
	 *                 <li>If user not authorised to publish:  {@link HttpStatus#UNAUTHORIZED_401}</li>
	 *                 <li>If collection is not complete:  {@link HttpStatus#CONFLICT_409}</li>
	 *                 <li>Complete fails for another reason:  {@link HttpStatus#BAD_REQUEST_400}</li>
	 * @return success true/false
	 * @throws IOException
	 */
	@POST
	public boolean publish(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		boolean result = false;

		// Locate and publish the collection:
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
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
