package com.eventhub.meeting;

import com.eventhub.meeting.MeetingDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Permissões por evento são checadas no PrepMeetingService. */
@RestController
@RequestMapping("/api/events/{eventId}/meetings")
public class PrepMeetingController {

    private final PrepMeetingService service;

    public PrepMeetingController(PrepMeetingService service) {
        this.service = service;
    }

    @GetMapping
    public List<MeetingResponse> list(@PathVariable Long eventId) {
        return service.list(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse create(@PathVariable Long eventId, @Valid @RequestBody MeetingRequest request) {
        return service.create(eventId, request);
    }

    @PutMapping("/{meetingId}")
    public MeetingResponse update(@PathVariable Long eventId, @PathVariable Long meetingId,
                                  @Valid @RequestBody MeetingRequest request) {
        return service.update(eventId, meetingId, request);
    }

    @DeleteMapping("/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long eventId, @PathVariable Long meetingId) {
        service.delete(eventId, meetingId);
    }

    @GetMapping("/{meetingId}/attendance")
    public List<AttendanceRow> attendance(@PathVariable Long eventId, @PathVariable Long meetingId) {
        return service.attendance(eventId, meetingId);
    }

    @PutMapping("/{meetingId}/attendance/{userId}")
    public AttendanceRow mark(@PathVariable Long eventId, @PathVariable Long meetingId, @PathVariable Long userId,
                              @Valid @RequestBody AttendanceRequest request) {
        return service.mark(eventId, meetingId, userId, request);
    }
}
