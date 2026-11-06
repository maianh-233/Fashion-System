package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.ShiftDtos.Availability;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.AvailabilityRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Colleague;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Entry;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.PeerResponse;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Swap;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.SwapCreate;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lịch làm cá nhân: mọi nhân viên đăng nhập, chỉ thao tác trên dữ liệu của chính mình. */
@RestController
@RequestMapping("/api/shifts")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ShiftController {
    private final AttendanceAccess access;
    private final ShiftScheduleService schedules;
    private final ShiftSwapService swaps;

    public ShiftController(AttendanceAccess access, ShiftScheduleService schedules, ShiftSwapService swaps) {
        this.access = access;
        this.schedules = schedules;
        this.swaps = swaps;
    }

    @GetMapping("/mine")
    public List<Entry> mine(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedules.mine(access.currentUserId(), from, to);
    }

    @GetMapping("/availability")
    public List<Availability> availability(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedules.availability(access.currentUserId(), from, to);
    }

    @PutMapping("/availability")
    public Availability setAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return schedules.setAvailability(access.currentUserId(), request);
    }

    @DeleteMapping("/availability/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAvailability(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        schedules.clearAvailability(access.currentUserId(), date);
    }

    @GetMapping("/{scheduleId}/colleagues")
    public List<Colleague> colleagues(@PathVariable UUID scheduleId) {
        return swaps.colleagues(access.currentUserId(), scheduleId);
    }

    @GetMapping("/swaps")
    public ShiftDtos.MySwaps mySwaps() {
        UUID userId = access.currentUserId();
        return new ShiftDtos.MySwaps(userId, swaps.mine(userId));
    }

    @PostMapping("/swaps")
    @ResponseStatus(HttpStatus.CREATED)
    public Swap createSwap(@Valid @RequestBody SwapCreate request) {
        return swaps.create(access.currentUserId(), request);
    }

    @PostMapping("/swaps/{id}/accept")
    public Swap accept(@PathVariable UUID id, @Valid @RequestBody(required = false) PeerResponse response) {
        return swaps.accept(access.currentUserId(), id, response);
    }

    @PostMapping("/swaps/{id}/decline")
    public Swap decline(@PathVariable UUID id, @Valid @RequestBody(required = false) PeerResponse response) {
        return swaps.decline(access.currentUserId(), id, response);
    }

    @PostMapping("/swaps/{id}/cancel")
    public Swap cancel(@PathVariable UUID id) {
        return swaps.cancel(access.currentUserId(), id);
    }
}
