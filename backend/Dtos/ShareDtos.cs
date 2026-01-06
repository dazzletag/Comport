namespace Backend.Dtos;

public sealed record SharePackCreateRequest(
    int ExpiryDays,
    IReadOnlyList<Guid> CompetencyIds
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
    long Size,
    DateTime UploadedAt
);

public sealed record SharePackCompetencyDto(
    Guid Id,
    string Title,
    string? Description,
    DateTime ExpiresAt,
    string Status,
    IReadOnlyList<SharePackEvidenceDto> Evidence
);

public sealed record SharePackPublicDto(
    DateTime ExpiresAt,
    IReadOnlyList<SharePackCompetencyDto> Competencies
);
