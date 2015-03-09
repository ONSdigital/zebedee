package com.github.onsdigital.zebedee.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Parameter;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.Collection;
import com.github.onsdigital.zebedee.json.CollectionDescription;

@Api
public class Collections {

	@GET
	public Object get(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		Path path = Path.newInstance(request);
		int index = Parameter.getId(path);
		if (index < 0) {
			return list();
		} else {
			Collection result = getCollection(request);
			if (result == null) {
				response.setStatus(HttpStatus.NOT_FOUND_404);
			}
			return result;
		}
	}

	@POST
	public void create(HttpServletRequest request,
			HttpServletResponse response,
			CollectionDescription collectionDescription) throws IOException {
		collectionDescription.name = StringUtils.trim(collectionDescription.name);
		for (Collection collection : Root.zebedee.getCollections()) {
			if (StringUtils.equals(collection.description.name,
					collectionDescription.name)) {
				response.setStatus(HttpStatus.CONFLICT_409);
				return;
			}
		}
		Collection.create(collectionDescription.name, Root.zebedee);
	}

	List<CollectionDescription> list() throws IOException {
		List<CollectionDescription> result = new ArrayList<>();

		List<Collection> collections = Root.zebedee.getCollections();
		for (Collection collection : collections) {
			result.add(collection.description);
		}

		return result;
	}

	static Collection getCollection(HttpServletRequest request)
			throws IOException {
		Collection result = null;

		Path path = Path.newInstance(request);
		List<String> segments = path.segments();
		int index = -1;
		if (segments.size() > 1) {
			Parameter.toInt(segments.get(1));
		}
		for (String segment : path.segments()) {
			System.out.println(" - " + segment);
		}

		if (index >= 0) {
			List<Collection> collections = Root.zebedee.getCollections();
			if (index < collections.size()) {
				result = collections.get(index);
			}
		}

		return result;
	}
}
