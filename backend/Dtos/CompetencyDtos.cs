namespace Backend.Dtos;

public sealed record EvidenceDto(
    Guid Id,
    string FileName,
    string? ContentType,
    string? Note,
    long Size,
    DateTime UploadedAt
);

public sealed record CompetencySummaryDto(
    Guid Id,
    string Title,
    string? Description,
    DateTime AchievedAt,
    DateTime ExpiresAt,
    string Category,
    string Status,
    int EvidenceCount
);

public sealed record CompetencyDetailDto(
    Guid Id,
    string Title,
    string? Description,
    DateTime AchievedAt,
    DateTime ExpiresAt,
    string Category,
    string Status,
    IReadOnlyList<EvidenceDto> Evidence
);

public sealed record CompetencyUpsertRequest(
    string Title,
    string? Description,
    DateTime AchievedAt,
    DateTime ExpiresAt,
    string Category
);
