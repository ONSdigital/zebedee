package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.ResultMessage;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
public class Publish {

	/**
	 *
	 * @param request
	 * @param response <ul>
	 *                 <li>If publish succeeds: {@link HttpStatus#OK_200}</li>
	 *                 <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
	 *                 <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
	 *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
	 *                 <li>If the collection is not approved:  {@link HttpStatus#CONFLICT_409}</li>
	 *                 </ul>
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws BadRequestException
	 * @throws UnauthorizedException
	 * @throws ConflictException
	 */
	@POST
	public boolean publish(HttpServletRequest request, HttpServletResponse response)
			throws IOException, NotFoundException, BadRequestException, UnauthorizedException, ConflictException {

		com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
		Session session = Root.zebedee.sessions.get(request);
		return Root.zebedee.collections.publish(collection, session);

	}

}
