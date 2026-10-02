package com.keni.doctorappointment.doctors.availability;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.doctors.availability.dto.DoctorAvailabilityRequest;
import com.keni.doctorappointment.doctors.availability.dto.DoctorAvailabilityResponse;
import com.keni.doctorappointment.doctors.availability.dto.DoctorTimeOffRequest;
import com.keni.doctorappointment.doctors.availability.dto.DoctorTimeOffResponse;
import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Doctor working hours and time-off management.
 *
 * <p>Only the doctor himself (own schedule) or an ADMIN may read/modify.</p>
 *
 * <p>Weekly hours are stored as wall-clock times in the clinic's zone, which
 * comes from the application {@link Clock} ({@code TimeConfig}) rather than from
 * {@link ZoneId#systemDefault()}. A per-doctor zone is not modelled yet; pinning
 * the clock's zone is what makes that possible without touching this code.
 * Tests can therefore move time and the zone deterministically instead of
 * waiting for a real day to pass.</p>
 */
@Service
@RequiredArgsConstructor
public class DoctorAvailabilityService {

	private final DoctorAvailabilityRepository availabilityRepository;

	private final DoctorTimeOffRepository timeOffRepository;

	private final DoctorRepository doctorRepository;

	/**
	 * Weekly hours are local to the clinic, so the zone comes from the clock
	 * rather than from {@link ZoneId#systemDefault()} (see {@code TimeConfig}).
	 */
	private final Clock clock;

	// --- availability ----------------------------------------------------

	@Transactional(readOnly = true)
	public List<DoctorAvailabilityResponse> getMine(AppUserPrincipal principal) {
		assertIsDoctorOrAdmin(principal);
		return availabilityRepository.findByDoctorIdOrderByDayOfWeekStartTime(principal.id())
			.stream().map(DoctorAvailabilityResponse::from).toList();
	}

	@Transactional
	public DoctorAvailabilityResponse add(AppUserPrincipal principal, DoctorAvailabilityRequest req) {
		assertIsDoctorOrAdmin(principal);
		if (!req.endTime().isAfter(req.startTime())) {
			throw new IllegalArgumentException("endTime must be after startTime");
		}
		Doctor doctor = doctorRepository.getReferenceById(principal.id());
		DoctorAvailability a = new DoctorAvailability(doctor, req.dayOfWeek(), req.startTime(), req.endTime());
		return DoctorAvailabilityResponse.from(availabilityRepository.save(a));
	}

	@Transactional
	public void delete(AppUserPrincipal principal, Long availabilityId) {
		assertIsDoctorOrAdmin(principal);
		DoctorAvailability a = availabilityRepository.findById(availabilityId)
			.orElseThrow(() -> new NoSuchElementException("Availability %d not found".formatted(availabilityId)));
		if (!a.getDoctor().getId().equals(principal.id()) && !principal.isAdmin()) {
			throw new AccessDeniedException("Not your availability slot");
		}
		availabilityRepository.delete(a);
	}

	// --- time off --------------------------------------------------------

	@Transactional(readOnly = true)
	public List<DoctorTimeOffResponse> getTimeOff(AppUserPrincipal principal) {
		assertIsDoctorOrAdmin(principal);
		return timeOffRepository.findByDoctor_IdOrderByStartAt(principal.id())
			.stream().map(DoctorTimeOffResponse::from).toList();
	}

	@Transactional
	public DoctorTimeOffResponse addTimeOff(AppUserPrincipal principal, DoctorTimeOffRequest req) {
		assertIsDoctorOrAdmin(principal);
		if (!req.endAt().isAfter(req.startAt())) {
			throw new IllegalArgumentException("endAt must be after startAt");
		}
		Doctor doctor = doctorRepository.getReferenceById(principal.id());
		DoctorTimeOff t = new DoctorTimeOff(doctor, req.startAt(), req.endAt(), req.reason());
		return DoctorTimeOffResponse.from(timeOffRepository.save(t));
	}

	@Transactional
	public void deleteTimeOff(AppUserPrincipal principal, Long timeOffId) {
		assertIsDoctorOrAdmin(principal);
		DoctorTimeOff t = timeOffRepository.findById(timeOffId)
			.orElseThrow(() -> new NoSuchElementException("Time-off %d not found".formatted(timeOffId)));
		if (!t.getDoctor().getId().equals(principal.id()) && !principal.isAdmin()) {
			throw new AccessDeniedException("Not your time-off entry");
		}
		timeOffRepository.delete(t);
	}

	// --- availability checking (used by booking) ------------------------

	/**
	 * Checks if a doctor is available at the given window.
	 *
	 * <p>Returns {@code true} iff</p>
	 * <ul>
	 *   <li>the window fits into at least one weekly availability slot, and</li>
	 *   <li>the window does not intersect any time-off period.</li>
	 * </ul>
	 */
	public boolean isAvailable(Doctor doctor, OffsetDateTime start, OffsetDateTime end) {
		if (start.isAfter(end) || start.equals(end)) {
			return false;
		}

		ZoneId zone = clock.getZone();
		DayOfWeek dow = start.atZoneSameInstant(zone).getDayOfWeek();
		LocalTime startLocal = start.atZoneSameInstant(zone).toLocalTime();
		LocalTime endLocal = end.atZoneSameInstant(zone).toLocalTime();

		// 1. Weekly availability
		boolean withinWeekly = availabilityRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), dow.getValue())
			.stream()
			.anyMatch(a -> !startLocal.isBefore(a.getStartTime()) && !endLocal.isAfter(a.getEndTime()));
		if (!withinWeekly) {
			return false;
		}

		// 2. Time off
		boolean blocked = timeOffRepository.findByDoctor_IdOrderByStartAt(doctor.getId())
			.stream()
			.anyMatch(t -> start.isBefore(t.getEndAt()) && end.isAfter(t.getStartAt()));
		if (blocked) {
			return false;
		}

		return true;
	}

	// --- helpers ---------------------------------------------------------

	private void assertIsDoctorOrAdmin(AppUserPrincipal principal) {
		// The filter chain already rejects anonymous callers; the null check is
		// defence in depth so a future routing change answers 403 instead of
		// failing with a NullPointerException.
		if (principal == null || (!principal.isDoctor() && !principal.isAdmin())) {
			throw new AccessDeniedException("Only doctors or ADMINs may manage availability");
		}
	}

}