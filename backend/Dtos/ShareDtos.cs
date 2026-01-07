namespace Backend.Dtos;

public sealed record SharePackCreateRequest(
    int ExpiryDays,
    IReadOnlyList<Guid> CompetencyIds,
    bool IncludeNmcPin
);

public sealed record SharePackCreateResponse(
    string Token,
    DateTime ExpiresAt,
    string ShareUrl
);

public sealed record SharePackEvidenceDto(
    Guid Id,
    string FileName,
    string? ContentType,
    string? Note,
    long Size,
    DateTime UploadedAt
);

public sealed record SharePackCompetencyDto(
    Guid Id,
    string Title,
    string? Description,
    DateTime AchievedAt,
    DateTime ExpiresAt,
    string Category,
    string Status,
    IReadOnlyList<SharePackEvidenceDto> Evidence
);

public sealed record SharePackPublicDto(
    DateTime ExpiresAt,
    string? NurseName,
    string? RegistrationType,
    string? NmcPin,
    IReadOnlyList<SharePackCompetencyDto> Competencies
);
