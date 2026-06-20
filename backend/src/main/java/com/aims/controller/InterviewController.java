package com.aims.controller;

import com.aims.dto.common.ApiResponse;
import com.aims.dto.common.PageResponse;
import com.aims.dto.interview.InterviewCalendarResponse;
import com.aims.dto.interview.InterviewRequest;
import com.aims.dto.interview.InterviewRoundRequest;
import com.aims.dto.interview.InterviewRoundResponse;
import com.aims.dto.interview.RescheduleInterviewRequest;
import com.aims.dto.interview.InterviewResponse;
import com.aims.entity.enums.InterviewStatus;
import com.aims.service.ExportService;
import com.aims.service.InterviewRoundService;
import com.aims.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Tag(name = "Interview Management")
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewRoundService interviewRoundService;
    private final ExportService exportService;

    @GetMapping
    @Operation(summary = "Get all interviews")
    public ResponseEntity<ApiResponse<PageResponse<InterviewResponse>>> getAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "") String interviewer,
            @RequestParam(required = false) InterviewStatus status,
            @RequestParam(required = false) String profile,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "interviewDate"));
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getAll(search, date, interviewer, status, profile, pageable)));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<InterviewCalendarResponse>>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) InterviewStatus status) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getCalendar(start, end, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponse>> create(@Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Interview scheduled", interviewService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> update(@PathVariable Long id, @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.update(id, request)));
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<InterviewResponse>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleInterviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.reschedule(id, request)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.cancel(id, body.get("notes"))));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<InterviewResponse>> complete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.complete(id, body.get("feedback"))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        interviewService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Interview deleted", null));
    }

    @GetMapping("/{id}/rounds")
    public ResponseEntity<ApiResponse<List<InterviewRoundResponse>>> getRounds(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(interviewRoundService.getRounds(id)));
    }

    @PostMapping("/{id}/rounds/init")
    public ResponseEntity<ApiResponse<List<InterviewRoundResponse>>> initRounds(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(interviewRoundService.initializeRounds(id)));
    }

    @PostMapping("/{id}/cv")
    public ResponseEntity<ApiResponse<InterviewResponse>> uploadCv(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.uploadCv(id, file)));
    }

    @PatchMapping("/{id}/rounds/{roundNumber}")
    public ResponseEntity<ApiResponse<InterviewRoundResponse>> updateRound(
            @PathVariable Long id,
            @PathVariable Integer roundNumber,
            @RequestBody InterviewRoundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewRoundService.updateRound(id, roundNumber, request)));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() throws Exception {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=interviews.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(exportService.exportInterviewsToCsv());
    }
}
