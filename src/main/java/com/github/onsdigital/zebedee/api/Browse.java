package com.github.onsdigital.zebedee.api;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.ChangeSet;
import com.github.onsdigital.zebedee.json.DirectoryListing;

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
	public DirectoryListing browse(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		Path requestPath = Path.newInstance(request);
		List<String> segments = new ArrayList<>(requestPath.segments());

		// Remove the API name:
		if (segments.size() > 0) {
			segments.remove(0);
		}

		// Remove the changeset ID, if present:
		if (segments.size() > 0 && segments.get(0).matches("[0-9]+")) {
			segments.remove(0);
		}

		// Build the URI:
		StringBuilder uriBuilder = new StringBuilder();
		for (String segment : segments) {
			uriBuilder.append("/");
			uriBuilder.append(segment);
		}
		if (uriBuilder.length() == 0) {
			uriBuilder.append("/");
		}
		String uri = uriBuilder.toString();

		// Locate the path:
		java.nio.file.Path path;
		ChangeSet changeSet = ChangeSets.getChangeSet(request);
		if (changeSet != null) {
			path = changeSet.find(uri);
		} else {
			path = Root.zebedee.find(uri);
		}
		if (path == null) {
			response.setStatus(HttpStatus.NOT_FOUND_404);
			return null;
		}

		// Do the right thing:
		if (Files.isDirectory(path)) {
			return listDirectory();
		} else {
			returnFile();
			return null;
		}

		// Get the directory listing:
		DirectoryListing listing = new DirectoryListing();
		try (DirectoryStream<java.nio.file.Path> stream = Files
				.newDirectoryStream(path)) {
			for (java.nio.file.Path directory : stream) {
				// Recursively delete directories only:
				if (Files.isDirectory(directory)) {
					listing.folders.put(directory.getFileName().toString(),
							directory.toString());
				} else {
					listing.files.put(directory.getFileName().toString(),
							directory.toString());
				}
			}
		}
		Serialiser.getBuilder().setPrettyPrinting();
		return listing;
	}

	private DirectoryListing listDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	private void returnFile() {
		// TODO Auto-generated method stub

	}
}
