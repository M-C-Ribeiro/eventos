package com.eventhub.meeting;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public final class MeetingDtos {

    private MeetingDtos() {
    }

    public record MeetingRequest(
            @NotBlank(message = "Informe o título") @Size(max = 160) String title,
            @NotNull(message = "Informe a data") LocalDate date,
            @NotNull(message = "Informe a hora") @JsonFormat(pattern = "HH:mm") LocalTime time,
            @NotBlank(message = "Informe o local") @Size(max = 255) String location) {
    }

    /** {@code presentCount} de {@code teamSize}: presentes entre os membros atuais da equipe. */
    public record MeetingResponse(
            Long id,
            String title,
            LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime time,
            String location,
            long presentCount,
            long teamSize) {

        public static MeetingResponse from(PrepMeeting m, long presentCount, long teamSize) {
            return new MeetingResponse(m.getId(), m.getTitle(), m.getDate(), m.getTime(), m.getLocation(),
                    presentCount, teamSize);
        }
    }

    /**
     * Uma linha da lista de presença. {@code inTeam} é falso para quem tinha registro de presença
     * mas saiu da equipe depois: o histórico é mantido.
     */
    public record AttendanceRow(
            Long userId,
            String userName,
            String functionName,
            boolean present,
            String justification,
            boolean inTeam) {
    }

    /** Marca presente/ausente. A justificativa só é aceita para ausentes; ao marcar presente ela é apagada. */
    public record AttendanceRequest(
            @NotNull(message = "Informe se a pessoa está presente") Boolean present,
            @Size(max = 500, message = "A justificativa pode ter até 500 caracteres") String justification) {
    }
}
