package com.keni.doctorappointment.doctors.availability.dto;

import java.time.LocalTime;

import com.keni.doctorappointment.doctors.availability.DoctorAvailability;

public record DoctorAvailabilityResponse(
		Long id,
		int dayOfWeek,
		LocalTime startTime,
		LocalTime endTime) {

	public static DoctorAvailabilityResponse from(DoctorAvailability a) {
		return new DoctorAvailabilityResponse(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime());
	}

}