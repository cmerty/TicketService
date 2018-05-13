package tickets.dao;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import tickets.api.dto.AddOrganiser;
import tickets.api.dto.BanRequest;
import tickets.entities.objects.Event;
import tickets.entities.objects.EventSeat;
import tickets.entities.objects.Hall;
import tickets.entities.objects.License;
import tickets.entities.objects.Seat;
import tickets.entities.users.Organiser;
import tickets.interfaces.IAdmin;

@Repository
public class AdminRepository implements IAdmin {
	@PersistenceContext
	EntityManager em;

	@Override
	@Transactional
	public boolean addOrganiser(AddOrganiser organiser) {
		Organiser newOrganiser = new Organiser(organiser);
		if (!em.contains(newOrganiser)) {
			try {
				em.persist(newOrganiser);
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<AddOrganiser> getOrganisers() {
		Query query = em.createQuery("SELECT s FROM Organiser s");
		if (query.getResultList() != null) {
			return query.getResultList();
		} else {
			return new HashSet<>();
		}
	}

	@Override
	@Transactional
	public boolean deleteOrganiser(String email) {
		if (em.find(Organiser.class, email) != null) {
			em.remove(em.find(Organiser.class, email));
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public boolean banOrganiser(BanRequest banRequest) {
		Organiser organiser = em.find(Organiser.class, banRequest.getEmail());
		if (organiser != null) {
			organiser.setIsBanned(banRequest.getIsBanned());
			em.merge(organiser);
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public boolean addLicense(String email) {
		String code = RandomStringUtils.randomAlphanumeric(10);
		License license = new License(code, email);
		if (!em.contains(license)) {
			try {
				em.persist(license);
				EmailSender sender =  new EmailSender(email);
				sender.sendEmailWithText("Your license: " + code);
				return true;
			} catch (Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	@Scheduled(cron = "0 0 12 * * *")
	@Override
	@Transactional
	@Modifying
	public boolean cleanDatabase() {
		Query query1 = em.createQuery("SELECT e FROM Event e WHERE DATE_PART('day', current_timestamp - e.date) >= 31");
		List<Event> events =  query1.getResultList();
		if(!events.isEmpty()) {
			for (Event event : events) {
				Query query2 = em.createQuery("DELETE FROM EventSeat e WHERE event_event_id = ?1");
				query2.setParameter(1, event.getEventId());
				query2.executeUpdate();
			}
			Query query = em.createQuery("DELETE FROM Event e WHERE DATE_PART('day', current_timestamp - e.date) >= 31");
			try {
				query.executeUpdate();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}else {
			return false;
		}
		
	}
	
	@Transactional
	@Override
	public boolean deleteTicket(Long ticketId) {
		try {
			em.remove(ticketId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	@Transactional
	@Override
	public boolean falseHall(Integer width, Integer height) {
		Hall hall = new Hall("Concert Hall", "Ashkelon", "Bar Kochba", "209", "Amazing concert hall", width.toString(), height.toString());
		List<Seat> seats =  new ArrayList<>();
		for (Integer i = 1; i <= width; i++) {
			for (Integer j = 1; j <= height; j++) {
				Seat seat = new Seat(hall, j.toString(), i.toString(), j.toString(), i.toString(), "seat");
				seats.add(seat);
			}
		}
		hall.setSeats(seats);
		try {
			em.persist(hall);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	@Transactional
	public boolean falseEvent(String artist, String title, String city, Date date, String time, String type,
			String description, String imageUrl, Integer hallId, Integer allTickets, String priceRange) throws Exception {
		Hall hall = em.find(Hall.class, hallId);
		Event event =  new Event(artist, title, city, date, time, type, description, imageUrl, hall, allTickets, priceRange);
		List<EventSeat> eventSeats = new ArrayList<>();
		for (Seat seat : hall.getSeats()) {
			Random rand = new Random();
			int number = rand.nextInt(3)+1;
			Color color1 = null;
			switch (number) {
			case 1:
				color1 = Color.decode("#e1f0cd");
				break;
			case 2:
				color1 = Color.decode("#fbc4cb");
				break;
			case 3:
				color1 = Color.decode("#faf2d2");
				break;
			}
			Integer color = color1.getRGB();
			String price = Integer.toString(number*100);
			EventSeat e =  new EventSeat(event, hall, seat, price, color);
			eventSeats.add(e);
		}
		event.setSeats(eventSeats);
		try {
			em.persist(event);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
