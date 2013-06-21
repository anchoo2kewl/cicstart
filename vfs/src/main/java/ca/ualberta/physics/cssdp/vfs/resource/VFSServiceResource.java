package ca.ualberta.physics.cssdp.vfs.resource;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import ca.ualberta.physics.cssdp.domain.ServiceInfo;
import ca.ualberta.physics.cssdp.domain.ServiceStats;
import ca.ualberta.physics.cssdp.domain.ServiceStats.ServiceName;
import ca.ualberta.physics.cssdp.resource.AbstractServiceResource;
import ca.ualberta.physics.cssdp.service.StatsService;
import ca.ualberta.physics.cssdp.vfs.InjectorHolder;

import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;

/*
 * Normally paths at class levels end with .json or .xml so the auto-api documentation
 * works properly.  This won't work but we are constrainted due to CANARIE's requirements
 * for this service.
 */
@Path("/service")
@Api(value = "/service", description = "Generic info about this service")
@Produces({ MediaType.APPLICATION_JSON })
public class VFSServiceResource extends AbstractServiceResource {

	@Inject
	private StatsService statsService;

	public VFSServiceResource() {
		InjectorHolder.inject(this);
	}

	@Override
	protected ServiceInfo buildInfo() {
		ServiceInfo info = new ServiceInfo();
		info.name = ServiceName.VFS;
		info.synopsis = "Long term persistent storage that can interact with other CICSTART services.  "
				+ "Useful for accessing input files required by Macros running on spawned VMs.  Also useful "
				+ "for storing output from Macros.";
		return info;
	}

	@Override
	protected ServiceStats buildStats() {
		return statsService.find(ServiceName.VFS).getPayload();
	}

	@Override
	protected URI getDocURI() throws URISyntaxException {
		return new URI("https://github.com/RoddiPotter/cicstart/wiki/VFS");
	}

}
