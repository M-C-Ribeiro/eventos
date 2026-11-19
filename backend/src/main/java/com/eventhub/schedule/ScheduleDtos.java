package com.eventhub.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record ScheduleItemRequest(
            @NotBlank(message = "Informe o título") @Size(max = 160) String title,
            @NotNull(message = "Informe a data") LocalDate date,
            @NotNull(message = "Informe a hora") @JsonFormat(pattern = "HH:mm") LocalTime time,
            @NotNull(message = "Informe a duração")
            @Min(value = 1, message = "A duração deve ser de pelo menos 1 minuto")
            @Max(value = 1440, message = "A duração não pode passar de 24 horas") Integer durationMinutes,
            @NotBlank(message = "Informe o palestrante") @Size(max = 120) String speaker,
            @Size(max = 160) String contact) {
    }

    public record ScheduleItemResponse(
            Long id,
            String title,
            LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime time,
            int durationMinutes,
            String speaker,
            String contact) {

        public static ScheduleItemResponse from(ScheduleItem s) {
            return new ScheduleItemResponse(s.getId(), s.getTitle(), s.getDate(), s.getTime(), s.getDurationMinutes(),
                    s.getSpeaker(), s.getContact());
        }
    }
}
