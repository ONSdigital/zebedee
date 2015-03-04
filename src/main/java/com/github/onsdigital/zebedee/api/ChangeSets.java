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
import com.github.onsdigital.zebedee.ChangeSet;
import com.github.onsdigital.zebedee.ChangeSetDescription;

@Api
public class ChangeSets {

	@GET
	public Object get(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		Path path = Path.newInstance(request);
		int index = Parameter.getId(path);
		if (index < 0) {
			return list();
		} else {
			ChangeSet result = getChangeSet(request);
			if (result == null) {
				response.setStatus(HttpStatus.NOT_FOUND_404);
			}
			return result;
		}
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

	List<ChangeSetDescription> list() throws IOException {
		List<ChangeSetDescription> result = new ArrayList<>();

		List<ChangeSet> changeSets = Root.zebedee.getChangeSets();
		for (ChangeSet changeSet : changeSets) {
			result.add(changeSet.description);
		}

		return result;
	}

	static ChangeSet getChangeSet(HttpServletRequest request)
			throws IOException {
		ChangeSet result = null;

		Path path = Path.newInstance(request);
		int index = Parameter.getId(path);
		if (index >= 0) {
			List<ChangeSet> changeSets = Root.zebedee.getChangeSets();
			if (index < changeSets.size()) {
				result = changeSets.get(index);
			}
		}

		return result;
	}
}
