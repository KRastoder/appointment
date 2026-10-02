package com.keni.doctorappointment.doctors.availability.dto;

import java.time.OffsetDateTime;

import com.keni.doctorappointment.doctors.availability.DoctorTimeOff;

public record DoctorTimeOffResponse(
		Long id,
		OffsetDateTime startAt,
		OffsetDateTime endAt,
		String reason) {

	public static DoctorTimeOffResponse from(DoctorTimeOff t) {
		return new DoctorTimeOffResponse(t.getId(), t.getStartAt(), t.getEndAt(), t.getReason());
	}

}