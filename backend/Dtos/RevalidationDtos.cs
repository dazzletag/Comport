namespace Backend.Dtos;

public sealed record RevalidationSummaryDto(
    DateTime? PinExpiryDate,
    DateTime? CycleStart,
    DateTime? CycleEnd,
    decimal PracticeHoursTotal,
    decimal PracticeHoursTarget,
    decimal CpdTotal,
    decimal CpdParticipatoryTotal,
    decimal CpdTarget,
    decimal CpdParticipatoryTarget,
    int FeedbackCount,
    int FeedbackTarget,
    int ReflectionCount,
    int ReflectionTarget,
    bool AtRisk
);

public sealed record PracticeHourDto(
    Guid Id,
    DateTime Date,
    string? Role,
    string? Setting,
    decimal Hours,
    string? Notes
);

public sealed record PracticeHourUpsertRequest(
    DateTime Date,
    string? Role,
    string? Setting,
    decimal Hours,
    string? Notes
);

public sealed record CpdEntryDto(
    Guid Id,
    DateTime Date,
    string Topic,
    decimal Hours,
    bool IsParticipatory,
    string? EvidenceFileName,
    string? EvidenceContentType,
    long? EvidenceSize,
    DateTime? EvidenceUploadedAt,
    string? Notes
);

public sealed record CpdEntryUpsertRequest(
    DateTime Date,
    string Topic,
    decimal Hours,
    bool IsParticipatory,
    string? Notes
);

public sealed record FeedbackDto(
    Guid Id,
    DateTime Date,
    string Source,
    string Summary,
    string? EvidenceFileName,
    string? EvidenceContentType,
    long? EvidenceSize,
    DateTime? EvidenceUploadedAt
);

public sealed record FeedbackUpsertRequest(
    DateTime Date,
    string Source,
    string Summary
);

public sealed record ReflectionDto(
    Guid Id,
    DateTime Date,
    string WhatHappened,
    string WhatLearned,
    string HowChanged,
    string CodeThemes
);

public sealed record ReflectionUpsertRequest(
    DateTime Date,
    string WhatHappened,
    string WhatLearned,
    string HowChanged,
    string CodeThemes
);

public sealed record DiscussionDto(
    DateTime? ReflectiveDiscussionDate,
    string? ReflectiveRegistrantName,
    string? ReflectiveRegistrantPin,
    DateTime? ConfirmationDate,
    string? ConfirmerName,
    string? ConfirmerRole
);

public sealed record DiscussionUpdateRequest(
    DateTime? ReflectiveDiscussionDate,
    string? ReflectiveRegistrantName,
    string? ReflectiveRegistrantPin,
    DateTime? ConfirmationDate,
    string? ConfirmerName,
    string? ConfirmerRole
);

public sealed record DeclarationDto(
    bool HealthAndCharacter,
    bool Indemnity
);

public sealed record DeclarationUpdateRequest(
    bool HealthAndCharacter,
    bool Indemnity
);
