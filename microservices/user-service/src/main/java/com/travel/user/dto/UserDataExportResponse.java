package com.travel.user.dto;

import java.time.Instant;
import java.util.List;

public record UserDataExportResponse(
        UserProfileExport profile,
        List<BookingExport> bookings,
        List<FeedbackExport> feedbacks,
        List<PaymentExport> payments,
        List<ReportExport> reportsFiled,
        List<ReportExport> reportsReceived
) {
    public record UserProfileExport(
            String id,
            String email,
            String firstName,
            String lastName,
            String role,
            Instant createdAt
    ) {}

    public record BookingExport(
            String id,
            String tripId,
            String status,
            String paymentId,
            Instant createdAt
    ) {}

    public record FeedbackExport(
            String id,
            String tripId,
            int rating,
            String comment,
            Instant createdAt
    ) {}

    public record PaymentExport(
            String id,
            String bookingId,
            String status,
            String paymentMethod,
            Instant createdAt
    ) {}

    public record ReportExport(
            String id,
            String reportedId,
            String tripId,
            String reason,
            String status,
            Instant createdAt
    ) {}
}
