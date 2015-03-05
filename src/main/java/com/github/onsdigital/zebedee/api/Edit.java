package com.github.onsdigital.zebedee.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Parameter;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.ChangeSet;
import com.github.onsdigital.zebedee.json.ChangeSetDescription;
import com.github.onsdigital.zebedee.json.Item;

@Api
public class Edit {

	@POST
	public boolean edit(HttpServletRequest request,
			HttpServletResponse response, Item item) throws IOException {
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

	@POST
	public void create(HttpServletRequest request,
			HttpServletResponse response,
			ChangeSetDescription changeSetDescription) throws IOException {
		changeSetDescription.name = StringUtils.trim(changeSetDescription.name);
		for (ChangeSet changeSet : Root.zebedee.getChangeSets()) {
			if (StringUtils.equals(changeSet.description.name,
					changeSetDescription.name)) {
				response.setStatus(HttpStatus.CONFLICT_409);
				return;
			}
		}
		ChangeSet.create(changeSetDescription.name, Root.zebedee);
	}

	public List<ChangeSetDescription> list() throws IOException {
		List<ChangeSetDescription> result = new ArrayList<>();

		List<ChangeSet> changeSets = Root.zebedee.getChangeSets();
		for (ChangeSet changeSet : changeSets) {
			result.add(changeSet.description);
		}

		return result;
	}

	public ChangeSet get(int index) throws IOException {
		ChangeSet result = null;

		List<ChangeSet> changeSets = Root.zebedee.getChangeSets();
		if (index < changeSets.size()) {
			result = changeSets.get(index);
		}

		return result;
	}
}
