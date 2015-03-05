package com.github.onsdigital.zebedee.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.ChangeSet;
import com.github.onsdigital.zebedee.json.Item;

@Api
public class Create {

	/**
	 * Creates a new item.
	 * 
	 * @param request
	 * @param response
	 * @param item
	 *            The URI of the item to create.
	 * @return If the item was successfully created, true.
	 * @throws IOException
	 */
	@POST
	public boolean create(HttpServletRequest request,
			HttpServletResponse response, Item item) throws IOException {
		boolean result;

		// Locate the change set:
		ChangeSet changeSet = ChangeSets.getChangeSet(request);
		if (changeSet == null) {
			response.setStatus(HttpStatus.NOT_FOUND_404);
			result = false;
		}

		// Open the item for editing:
		result = changeSet.edit(item.uri);
		if (!result) {
			response.setStatus(HttpStatus.BAD_REQUEST_400);
		}

		return result;
	}
}
