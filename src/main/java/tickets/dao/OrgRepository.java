package tickets.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Repository;

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
import tickets.entities.objects.Event;
import tickets.entities.objects.EventSeat;
import tickets.entities.objects.Hall;
import tickets.entities.objects.License;
import tickets.entities.users.Client;
import tickets.entities.users.Organiser;
import tickets.interfaces.IOrganiser;
@Repository
public class OrgRepository implements IOrganiser {
	@PersistenceContext
	EntityManager em;
	@Override
	@Transactional
	public SuccessResponse register(RegisterOrganiser organiser) {
		SuccessResponse response = new SuccessResponse();
		Organiser newOrganiser = new Organiser(organiser);
		Client possibleClient = em.find(Client.class, organiser.getEmail());
		Organiser possibleOrganiser = em.find(Organiser.class, organiser.getEmail());
		License license = em.find(License.class, organiser.getLicense());
		if (license == null || license.getEmail()!= organiser.getEmail()) {
			response.setSuccess(false);
			response.setResponse("Wrong license");
			return response;
		}
		if (possibleClient == null && possibleOrganiser == null) {
			try {
				em.persist(newOrganiser);
			} catch (Exception e) {
				response.setResponse("Database error");
				response.setSuccess(false);
				return response;
			}
		} else {
			response.setSuccess(false);
			response.setResponse("There is already a user with this email");
			return response;
		}
		em.remove(license);
		response.setSuccess(true);
		response.setResponse(newOrganiser.getEmail());
		return response;
	}

	@Override
	public Iterable<ShortEventInfo> getEventsByDate(String email) {
		Organiser organiser =  em.find(Organiser.class, email);
		Set<Event> orgEvents = organiser.getEvents();
		Set<ShortEventInfo> response =  new HashSet<>();
		for (Event event : orgEvents) {
			ShortEventInfo eventInfo =  new ShortEventInfo(event);
			response.add(eventInfo);
		}
		return sortByDate(response);
	}

	private Iterable<ShortEventInfo> sortByDate(Set<ShortEventInfo> response) {
		List<ShortEventInfo> events = new ArrayList<>(response);
		events.sort(new Comparator<ShortEventInfo>() {

			@Override
			public int compare(ShortEventInfo o1, ShortEventInfo o2) {
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				Date date1 = new Date();
				Date date2 = new Date();
				try {
					date1 = formatter.parse(o1.getDate());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				try {
					date2 = formatter.parse(o2.getDate());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				return date1.compareTo(date2);
			}
		});
		return events;
	}

	@Override
	public Iterable<ShortEventInfo> getEventsByHall(OrgHallRequest orgHallRequest) {
		Organiser organiser =  em.find(Organiser.class, orgHallRequest.getEmail());
		Set<Event> orgEvents = organiser.getEvents();
		Set<ShortEventInfo> response =  new HashSet<>();
		for (Event event : orgEvents) {
			if (event.getHall().getHallId().toString().equals(orgHallRequest.getHallId())) {
				ShortEventInfo eventInfo =  new ShortEventInfo(event);
				response.add(eventInfo);
			}
		}
		return sortByDate(response);
	}

	@Override
	public Iterable<ShortEventInfo> getEventsByType(OrgTypeRequest orgTypeRequest) {
		Organiser organiser =  em.find(Organiser.class, orgTypeRequest.getEmail());
		Set<Event> orgEvents = organiser.getEvents();
		Set<ShortEventInfo> response =  new HashSet<>();
		for (Event event : orgEvents) {
			if (event.getType().equals(orgTypeRequest.getType())) {
				ShortEventInfo eventInfo =  new ShortEventInfo(event);
				response.add(eventInfo);
			}
		}
		return sortByDate(response);
	}

	@Override
	@Transactional
	public EditEvent editEvent(EventOrgRequest eventOrgRequest) {
		return new EditEvent(em.find(Event.class, Integer.parseInt(eventOrgRequest.getEventId())), eventOrgRequest.getEmail());
	}

	@Override
	@Transactional
	public boolean addEvent(AddEvent request) throws Exception {
		Hall hall = em.find(Hall.class, Integer.parseInt(request.getHallId()));
		Organiser org = em.find(Organiser.class, request.getEmail());
		Event event = new Event(request, hall, org);
		try {
			em.persist(event);
		} catch (Exception e) {
			return false;
		}
		Set<Event> orgEvents = org.getEvents();
		orgEvents.add(event);
		org.setEvents(orgEvents);
		em.merge(org);
		return true;
	}

	@Override
	@Transactional
	public boolean hideEvent(VisibleRequest visibleRequest) {
		Event event = em.find(Event.class, Integer.parseInt(visibleRequest.getEventId()));
		event.setIsHidden(visibleRequest.getIsHidden());
		em.merge(event);
		return true;
	}

	@Override
	@Transactional
	public boolean deleteEvent(String eventId) {
		Event event = em.find(Event.class, Integer.parseInt(eventId));
		event.setIsDeleted(true);
		try {
			em.merge(event);
			String text = "We regret to announce that the event " + event.getTitle() + " has been cancelled. Please contact the organiser " + event.getOrg().getCompanyName() + " for refund.";
			for (EventSeat eventSeat : event.getSeats()) {
				if (eventSeat.getTicket().getBuyer()!= null) {
					EmailSender sender = new EmailSender(eventSeat.getTicket().getBuyer().getEmail());
					sender.sendEmailWithText(text);
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public RegisterOrganiser getProfile(String email) {
		return new RegisterOrganiser(em.find(Organiser.class, email));
	}

	@Override
	public Iterable<ShortHallInfo> getHalls(String email) {
		Set<Hall> halls = em.find(Organiser.class, email).getHalls();
		Set<ShortHallInfo> hallInfos =  new HashSet<>();
		for (Hall hall : halls) {
			hallInfos.add(new ShortHallInfo(hall));
		}
		return hallInfos;
	}

	@Override
	@Transactional
	public String addHall(HallRequest hallRequest) {
		Organiser organiser = em.find(Organiser.class, hallRequest.getEmail());
		Set<Hall> halls = organiser.getHalls();
		Hall hall = new Hall(hallRequest, organiser);
		try {
			em.persist(hall);
		} catch (Exception e) {
			return "Database error";
		}
		halls.add(hall);
		organiser.setHalls(halls);
		try {
			em.merge(organiser);
			return hall.getHallId().toString();
		} catch (Exception e) {
			return "Database error";
		}
		
		
	}

	@Override
	public HallScheme getHallScheme(String hallId) {
		Hall hall = em.find(Hall.class, Integer.parseInt(hallId));
		return new HallScheme(hall);
	}

}
