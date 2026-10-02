package com.keni.doctorappointment.appointments.dto;

import lombok.Value;

/** Request to cancel an appointment. */
@Value
public class CancelAppointmentRequest {

	String reason;

}