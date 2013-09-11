package ca.ualberta.physics.cssdp.domain;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import ca.ualberta.physics.cssdp.service.StatsService;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * A struct to hold service info for CANARIE service registry
 * 
 * @author rpotter
 * 
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class ServiceInfo {

	private StatsService.ServiceName name;
	private String synopsis;
	private String version = "1.0";
	private String institution = "University of Alberta, Department of Physics, Space Physics";
	// ISO8601
	private String releaseTime = ISODateTimeFormat.dateTimeNoMillis()
			.withZoneUTC().print(new DateTime(2013, 06, 30, 01, 01));

	public StatsService.ServiceName getName() {
		return name;
	}

	public void setName(StatsService.ServiceName name) {
		this.name = name;
	}

	public String getSynopsis() {
		return synopsis;
	}

	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getInstitution() {
		return institution;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public String getReleaseTime() {
		return releaseTime;
	}

	public void setReleaseTime(String releaseTime) {
		this.releaseTime = releaseTime;
	}

}
