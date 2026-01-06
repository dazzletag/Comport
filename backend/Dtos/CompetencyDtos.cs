namespace Backend.Dtos;

public sealed record EvidenceDto(
    Guid Id,
    string FileName,
    string? ContentType,
    long Size,
    DateTime UploadedAt
);

public sealed record CompetencySummaryDto(
    Guid Id,
    string Title,
    string? Description,
    DateTime ExpiresAt,
    string Status,
    int EvidenceCount
);

public sealed record CompetencyDetailDto(
    Guid Id,
    string Title,
    string? Description,
    DateTime ExpiresAt,
    string Status,
    IReadOnlyList<EvidenceDto> Evidence
);

public sealed record CompetencyUpsertRequest(
    string Title,
    string? Description,
    DateTime ExpiresAt
);
