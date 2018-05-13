package tickets.interfaces;

import java.text.ParseException;

import tickets.api.dto.AddEvent;
import tickets.api.dto.EditEvent;
import tickets.api.dto.EventOrgRequest;
import tickets.api.dto.HallRequest;
import tickets.api.dto.HallScheme;
import tickets.api.dto.OrgHallRequest;
import tickets.api.dto.OrgTypeRequest;
import tickets.api.dto.RegisterOrganiser;
import tickets.api.dto.ShortEventInfo;
import tickets.api.dto.ShortHallInfo;
import tickets.api.dto.SuccessResponse;
import tickets.api.dto.VisibleRequest;

public interface IOrganiser {
	SuccessResponse register (RegisterOrganiser organiser);
	Iterable<ShortEventInfo> getEventsByDate(String email);
	Iterable<ShortEventInfo> getEventsByHall(OrgHallRequest orgHallRequest);
	Iterable<ShortEventInfo> getEventsByType(OrgTypeRequest orgTypeRequest);
	EditEvent editEvent(EventOrgRequest eventOrgRequest);
	boolean addEvent(AddEvent event) throws ParseException, Exception;
	boolean hideEvent(VisibleRequest visibleRequest);
	boolean deleteEvent(String eventId);
	RegisterOrganiser getProfile(String email);
	Iterable<ShortHallInfo> getHalls(String email);
	String addHall(HallRequest hallRequest);
	HallScheme getHallScheme(String hallId);
}
