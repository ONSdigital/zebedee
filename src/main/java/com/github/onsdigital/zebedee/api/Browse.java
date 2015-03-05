package com.github.onsdigital.zebedee.api;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;

@Api
public class Browse {

	/**
	 * Enables you to browse the site, or a specific changeset.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@GET
	public Map<String, String> browse(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Path path = Path.newInstance(request);
		for (String segment : path.segments()) {
			System.out.println(" - " + segment);
		}

		return null;
	}
}
