package com.keni.doctorappointment.doctors.availability;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.doctors.availability.dto.DoctorAvailabilityRequest;
import com.keni.doctorappointment.doctors.availability.dto.DoctorAvailabilityResponse;
import com.keni.doctorappointment.doctors.availability.dto.DoctorTimeOffRequest;
import com.keni.doctorappointment.doctors.availability.dto.DoctorTimeOffResponse;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Doctor availability and time-off endpoints.
 *
 * <pre>
 * GET    /api/doctors/me/availability         own weekly schedule
 * POST   /api/doctors/me/availability         add a slot
 * DELETE /api/doctors/me/availability/{id}    remove a slot
 *
 * GET    /api/doctors/me/time-off             own time-off entries
 * POST   /api/doctors/me/time-off             add time-off
 * DELETE /api/doctors/me/time-off/{id}        remove time-off
 * </pre>
 */
@RestController
@RequestMapping("/api/doctors/me")
@RequiredArgsConstructor
public class DoctorAvailabilityController {

	private final DoctorAvailabilityService availabilityService;

	@GetMapping("/availability")
	public List<DoctorAvailabilityResponse> getAvailability(
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return availabilityService.getMine(principal);
	}

	@PostMapping("/availability")
	public DoctorAvailabilityResponse addAvailability(
			@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody DoctorAvailabilityRequest request) {
		return availabilityService.add(principal, request);
	}

	@DeleteMapping("/availability/{id}")
	public void deleteAvailability(
			@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable Long id) {
		availabilityService.delete(principal, id);
	}

	@GetMapping("/time-off")
	public List<DoctorTimeOffResponse> getTimeOff(
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return availabilityService.getTimeOff(principal);
	}

	@PostMapping("/time-off")
	public DoctorTimeOffResponse addTimeOff(
			@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody DoctorTimeOffRequest request) {
		return availabilityService.addTimeOff(principal, request);
	}

	@DeleteMapping("/time-off/{id}")
	public void deleteTimeOff(
			@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable Long id) {
		availabilityService.deleteTimeOff(principal, id);
	}

}