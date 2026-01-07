using System.Security.Claims;
using System.Security.Cryptography;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using Backend.Data;
using Backend.Dtos;
using Backend.Models;
using Backend.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddApplicationInsightsTelemetry();

builder.Services.AddDbContext<AppDbContext>(options =>
{
    var config = builder.Configuration;
    var connectionString = config["Sql:ConnectionString"];
    if (string.IsNullOrWhiteSpace(connectionString))
    {
        var server = config["Sql:Server"];
        var database = config["Sql:Database"];
        var user = config["Sql:User"];
        var password = config["Sql:Password"];
        if (string.IsNullOrWhiteSpace(server) || string.IsNullOrWhiteSpace(database) || string.IsNullOrWhiteSpace(user) || string.IsNullOrWhiteSpace(password))
        {
            throw new InvalidOperationException("SQL configuration is missing. Provide Sql:ConnectionString or Sql:Server/Database/User/Password.");
        }

        connectionString = $"Server=tcp:{server},1433;Initial Catalog={database};Persist Security Info=False;User ID={user};Password={password};MultipleActiveResultSets=False;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;";
    }

    options.UseSqlServer(connectionString);
});

builder.Services.AddSingleton<BlobStorageService>();

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        var config = builder.Configuration.GetSection("AzureAd");
        var instance = config["Instance"] ?? "https://login.microsoftonline.com/";
        var tenantId = config["TenantId"] ?? throw new InvalidOperationException("AzureAd:TenantId missing.");
        var audience = config["Audience"] ?? throw new InvalidOperationException("AzureAd:Audience missing.");
        var normalizedAudience = audience.StartsWith("api://", StringComparison.OrdinalIgnoreCase)
            ? audience["api://".Length..]
            : $"api://{audience}";

        options.Authority = $"{instance}{tenantId}/v2.0";
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = $"{instance}{tenantId}/v2.0",
            ValidAudiences = new[] { audience, normalizedAudience }.Distinct(StringComparer.OrdinalIgnoreCase)
        };
    });

builder.Services.AddAuthorization();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
        policy.AllowAnyOrigin()
            .AllowAnyHeader()
            .AllowAnyMethod());
});

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();
app.UseCors();

app.MapGet("/me", (ClaimsPrincipal user) =>
{
    var userId = GetUserId(user);
    var name = user.FindFirstValue("name") ?? user.FindFirstValue(ClaimTypes.Name);
    return Results.Ok(new { userId, name });
}).RequireAuthorization();

app.MapGet("/profile", async (AppDbContext db, ClaimsPrincipal user) =>
{
    var userId = GetUserId(user);
    var profile = await db.NurseProfiles.FirstOrDefaultAsync(p => p.UserId == userId);
    var email = GetEmail(user);
    var displayName = user.FindFirstValue("name") ?? user.FindFirstValue(ClaimTypes.Name) ?? "Unknown";

    if (profile is null)
    {
        profile = new NurseProfile
        {
            UserId = userId,
            FullName = displayName,
            Email = email,
            UpdatedAt = DateTime.UtcNow
        };
        db.NurseProfiles.Add(profile);
        await db.SaveChangesAsync();
    }
    else if (!string.IsNullOrWhiteSpace(email) && !string.Equals(profile.Email, email, StringComparison.OrdinalIgnoreCase))
    {
        profile.Email = email;
        profile.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync();
    }

    return Results.Ok(new NurseProfileDto(
        profile.FullName,
        profile.PreferredName,
        profile.NmcPin,
        profile.RegistrationType,
        profile.Employer,
        profile.RoleBand,
        profile.Email,
        profile.Phone,
        profile.Bio));
}).RequireAuthorization();

app.MapPut("/profile", async (AppDbContext db, ClaimsPrincipal user, NurseProfileUpdateRequest request) =>
{
    var userId = GetUserId(user);
    var profile = await db.NurseProfiles.FirstOrDefaultAsync(p => p.UserId == userId);
    var email = GetEmail(user);
    var fullName = request.FullName?.Trim() ?? string.Empty;

    if (string.IsNullOrWhiteSpace(fullName))
    {
        return Results.BadRequest(new { message = "Full name is required." });
    }

    var normalizedPin = NormalizeNmcPin(request.NmcPin);
    if (normalizedPin == string.Empty)
    {
        return Results.BadRequest(new { message = "NMC PIN must be 2 letters followed by 6 digits." });
    }

    if (!IsValidRegistrationType(request.RegistrationType))
    {
        return Results.BadRequest(new { message = "Registration type must be RN Adult, RN Mental Health, RN Learning Disability, or RN Child." });
    }

    if (profile is null)
    {
        profile = new NurseProfile { UserId = userId };
        db.NurseProfiles.Add(profile);
    }

    profile.FullName = fullName;
    profile.PreferredName = string.IsNullOrWhiteSpace(request.PreferredName) ? null : request.PreferredName.Trim();
    profile.NmcPin = normalizedPin;
    profile.RegistrationType = NormalizeRegistrationType(request.RegistrationType);
    profile.Employer = string.IsNullOrWhiteSpace(request.Employer) ? null : request.Employer.Trim();
    profile.RoleBand = string.IsNullOrWhiteSpace(request.RoleBand) ? null : request.RoleBand.Trim();
    profile.Phone = string.IsNullOrWhiteSpace(request.Phone) ? null : request.Phone.Trim();
    profile.Bio = string.IsNullOrWhiteSpace(request.Bio) ? null : request.Bio.Trim();
    profile.Email = email;
    profile.UpdatedAt = DateTime.UtcNow;

    await db.SaveChangesAsync();

    return Results.Ok(new NurseProfileDto(
        profile.FullName,
        profile.PreferredName,
        profile.NmcPin,
        profile.RegistrationType,
        profile.Employer,
        profile.RoleBand,
        profile.Email,
        profile.Phone,
        profile.Bio));
}).RequireAuthorization();

app.MapGet("/competencies", async (AppDbContext db, ClaimsPrincipal user) =>
{
    var userId = GetUserId(user);
    var now = DateTime.UtcNow;

    var items = await db.Competencies
        .AsNoTracking()
        .Where(c => c.UserId == userId)
        .Select(c => new
        {
            c.Id,
            c.Title,
            c.Description,
            c.AchievedAt,
            c.ExpiresAt,
            c.Category,
            EvidenceCount = c.Evidence.Count
        })
        .ToListAsync();

    var result = items
        .Select(c => new CompetencySummaryDto(
            c.Id,
            c.Title,
            c.Description,
            c.AchievedAt,
            c.ExpiresAt,
            c.Category,
            ComputeStatus(c.ExpiresAt, now),
            c.EvidenceCount))
        .ToList();

    return Results.Ok(result);
}).RequireAuthorization();

app.MapPost("/competencies", async (AppDbContext db, ClaimsPrincipal user, CompetencyUpsertRequest request) =>
{
    var userId = GetUserId(user);
    var now = DateTime.UtcNow;
    var achievedAt = request.AchievedAt == default ? now : request.AchievedAt;
    var category = NormalizeCategory(request.Category);

    var title = request.Title?.Trim() ?? string.Empty;

    if (string.IsNullOrWhiteSpace(title))
    {
        return Results.BadRequest(new { message = "Title is required." });
    }

    if (!IsValidCategory(category))
    {
        return Results.BadRequest(new { message = "Category must be Mandatory, Clinical, or Specialist." });
    }

    if (request.ExpiresAt < achievedAt)
    {
        return Results.BadRequest(new { message = "Expiry date cannot be before achieved date." });
    }

    var competency = new Competency
    {
        Id = Guid.NewGuid(),
        Title = title,
        Description = request.Description,
        AchievedAt = achievedAt,
        ExpiresAt = request.ExpiresAt,
        Category = category,
        UserId = userId,
        CreatedAt = now,
        UpdatedAt = now
    };

    db.Competencies.Add(competency);
    await db.SaveChangesAsync();

    return Results.Created($"/competencies/{competency.Id}", new CompetencyDetailDto(
        competency.Id,
        competency.Title,
        competency.Description,
        competency.AchievedAt,
        competency.ExpiresAt,
        competency.Category,
        ComputeStatus(competency.ExpiresAt, now),
        Array.Empty<EvidenceDto>()));
}).RequireAuthorization();

app.MapGet("/competencies/{id:guid}", async (AppDbContext db, ClaimsPrincipal user, Guid id) =>
{
    var userId = GetUserId(user);
    var now = DateTime.UtcNow;

    var competency = await db.Competencies
        .AsNoTracking()
        .Include(c => c.Evidence)
        .FirstOrDefaultAsync(c => c.Id == id && c.UserId == userId);

    if (competency is null)
    {
        return Results.NotFound();
    }

    var evidence = competency.Evidence
        .OrderByDescending(e => e.UploadedAt)
        .Select(e => new EvidenceDto(e.Id, e.FileName, e.ContentType, e.Note, e.Size, e.UploadedAt))
        .ToList();

    return Results.Ok(new CompetencyDetailDto(
        competency.Id,
        competency.Title,
        competency.Description,
        competency.AchievedAt,
        competency.ExpiresAt,
        competency.Category,
        ComputeStatus(competency.ExpiresAt, now),
        evidence));
}).RequireAuthorization();

app.MapPut("/competencies/{id:guid}", async (AppDbContext db, ClaimsPrincipal user, Guid id, CompetencyUpsertRequest request) =>
{
    var userId = GetUserId(user);
    var competency = await db.Competencies.FirstOrDefaultAsync(c => c.Id == id && c.UserId == userId);
    if (competency is null)
    {
        return Results.NotFound();
    }

    var title = request.Title?.Trim() ?? string.Empty;
    var achievedAt = request.AchievedAt == default ? competency.AchievedAt : request.AchievedAt;
    var category = NormalizeCategory(request.Category);

    if (string.IsNullOrWhiteSpace(title))
    {
        return Results.BadRequest(new { message = "Title is required." });
    }

    if (!IsValidCategory(category))
    {
        return Results.BadRequest(new { message = "Category must be Mandatory, Clinical, or Specialist." });
    }

    if (request.ExpiresAt < achievedAt)
    {
        return Results.BadRequest(new { message = "Expiry date cannot be before achieved date." });
    }

    competency.Title = title;
    competency.Description = request.Description;
    competency.AchievedAt = achievedAt;
    competency.ExpiresAt = request.ExpiresAt;
    competency.Category = category;
    competency.UpdatedAt = DateTime.UtcNow;

    await db.SaveChangesAsync();
    return Results.NoContent();
}).RequireAuthorization();

app.MapPost("/competencies/{id:guid}/evidence", async (AppDbContext db, BlobStorageService storage, ClaimsPrincipal user, Guid id, HttpRequest request, IConfiguration config, CancellationToken cancellationToken) =>
{
    if (!request.HasFormContentType)
    {
        return Results.BadRequest(new { message = "Expected multipart form data." });
    }

    var form = await request.ReadFormAsync(cancellationToken);
    var file = form.Files.GetFile("file");
    if (file is null)
    {
        return Results.BadRequest(new { message = "Missing file field named 'file'." });
    }

    var maxBytes = GetEvidenceMaxBytes(config);
    if (file.Length > maxBytes)
    {
        return Results.BadRequest(new { message = $"Evidence file exceeds limit of {maxBytes / (1024 * 1024)} MB." });
    }

    if (!IsAllowedEvidence(file.ContentType, file.FileName))
    {
        return Results.BadRequest(new { message = "Unsupported evidence type. Upload photos or PDF/DOC/DOCX files." });
    }

    var userId = GetUserId(user);
    var competency = await db.Competencies.FirstOrDefaultAsync(c => c.Id == id && c.UserId == userId, cancellationToken);
    if (competency is null)
    {
        return Results.NotFound();
    }

    await storage.EnsureContainerAsync(cancellationToken);

    var blobName = $"{userId}/{competency.Id}/{Guid.NewGuid()}_{file.FileName}";
    await using var stream = file.OpenReadStream();
    await storage.UploadAsync(blobName, stream, file.ContentType, cancellationToken);

    var note = form.TryGetValue("note", out var noteValues) ? noteValues.ToString() : null;
    note = string.IsNullOrWhiteSpace(note) ? null : note.Trim();
    if (note != null && note.Length > 400)
    {
        return Results.BadRequest(new { message = "Evidence note must be 400 characters or fewer." });
    }

    var evidence = new Evidence
    {
        Id = Guid.NewGuid(),
        CompetencyId = competency.Id,
        FileName = file.FileName,
        ContentType = file.ContentType,
        Note = note,
        BlobName = blobName,
        Size = file.Length,
        UploadedAt = DateTime.UtcNow
    };

    db.Evidence.Add(evidence);
    await db.SaveChangesAsync(cancellationToken);

    return Results.Ok(new EvidenceDto(evidence.Id, evidence.FileName, evidence.ContentType, evidence.Note, evidence.Size, evidence.UploadedAt));
}).RequireAuthorization();

app.MapGet("/competencies/{id:guid}/evidence/{evidenceId:guid}/download", async (AppDbContext db, BlobStorageService storage, ClaimsPrincipal user, Guid id, Guid evidenceId, CancellationToken cancellationToken) =>
{
    var userId = GetUserId(user);
    var competency = await db.Competencies
        .AsNoTracking()
        .FirstOrDefaultAsync(c => c.Id == id && c.UserId == userId, cancellationToken);

    if (competency is null)
    {
        return Results.NotFound();
    }

    var evidence = await db.Evidence
        .AsNoTracking()
        .FirstOrDefaultAsync(e => e.Id == evidenceId && e.CompetencyId == id, cancellationToken);

    if (evidence is null)
    {
        return Results.NotFound();
    }

    var stream = await storage.OpenReadAsync(evidence.BlobName, cancellationToken);
    return Results.File(stream, evidence.ContentType ?? "application/octet-stream", evidence.FileName);
}).RequireAuthorization();

app.MapPost("/sharepacks", async (AppDbContext db, ClaimsPrincipal user, SharePackCreateRequest request, HttpContext httpContext) =>
{
    if (request.CompetencyIds.Count == 0)
    {
        return Results.BadRequest(new { message = "Provide at least one competencyId." });
    }

    if (request.ExpiryDays <= 0 || request.ExpiryDays > 365)
    {
        return Results.BadRequest(new { message = "ExpiryDays must be between 1 and 365." });
    }

    var userId = GetUserId(user);
    var profile = await db.NurseProfiles.AsNoTracking().FirstOrDefaultAsync(p => p.UserId == userId);
    var nurseName = profile?.FullName ?? user.FindFirstValue("name") ?? user.FindFirstValue(ClaimTypes.Name);
    var registrationType = profile?.RegistrationType;
    var nmcPin = request.IncludeNmcPin ? profile?.NmcPin : null;

    var competencies = await db.Competencies
        .Where(c => c.UserId == userId && request.CompetencyIds.Contains(c.Id))
        .Select(c => c.Id)
        .ToListAsync();

    if (competencies.Count != request.CompetencyIds.Count)
    {
        return Results.BadRequest(new { message = "One or more competencies not found." });
    }

    var tokenBytes = RandomNumberGenerator.GetBytes(32);
    var token = ToUrlSafeToken(tokenBytes);
    var tokenHash = HashToken(token);

    var expiresAt = DateTime.UtcNow.AddDays(request.ExpiryDays);

    var sharePack = new SharePack
    {
        Id = Guid.NewGuid(),
        OwnerUserId = userId,
        TokenHash = tokenHash,
        NurseName = nurseName,
        RegistrationType = registrationType,
        NmcPin = nmcPin,
        IncludeNmcPin = request.IncludeNmcPin,
        ExpiresAt = expiresAt,
        CreatedAt = DateTime.UtcNow,
        Items = competencies.Select(id => new SharePackItem { CompetencyId = id }).ToList()
    };

    db.SharePacks.Add(sharePack);
    await db.SaveChangesAsync();

    var viewerBaseUrl = httpContext.RequestServices.GetRequiredService<IConfiguration>()["ShareViewer:BaseUrl"];
    var baseUrl = string.IsNullOrWhiteSpace(viewerBaseUrl)
        ? $"{httpContext.Request.Scheme}://{httpContext.Request.Host}"
        : viewerBaseUrl.TrimEnd('/');
    var shareUrl = $"{baseUrl}/?token={token}";

    return Results.Ok(new SharePackCreateResponse(token, expiresAt, shareUrl));
}).RequireAuthorization();

app.MapGet("/share/{token}", async (AppDbContext db, string token) =>
{
    var tokenHash = HashToken(token);
    var now = DateTime.UtcNow;

    var sharePack = await db.SharePacks
        .AsNoTracking()
        .Include(sp => sp.Items)
        .ThenInclude(item => item.Competency)
        .ThenInclude(c => c!.Evidence)
        .FirstOrDefaultAsync(sp => sp.TokenHash == tokenHash);

    if (sharePack is null || sharePack.ExpiresAt < now)
    {
        return Results.NotFound();
    }

    var competencies = sharePack.Items
        .Select(item => item.Competency)
        .Where(c => c is not null)
        .Select(c => new SharePackCompetencyDto(
            c!.Id,
            c.Title,
            c.Description,
            c.AchievedAt,
            c.ExpiresAt,
            c.Category,
            ComputeStatus(c.ExpiresAt, now),
            c.Evidence.OrderByDescending(e => e.UploadedAt)
                .Select(e => new SharePackEvidenceDto(e.Id, e.FileName, e.ContentType, e.Note, e.Size, e.UploadedAt))
                .ToList()))
        .ToList();

    var nmcPin = sharePack.IncludeNmcPin ? sharePack.NmcPin : null;
    return Results.Ok(new SharePackPublicDto(sharePack.ExpiresAt, sharePack.NurseName, sharePack.RegistrationType, nmcPin, competencies));
});

app.MapGet("/share/{token}/download/{evidenceId:guid}", async (AppDbContext db, BlobStorageService storage, string token, Guid evidenceId, CancellationToken cancellationToken) =>
{
    var tokenHash = HashToken(token);
    var now = DateTime.UtcNow;

    var sharePack = await db.SharePacks
        .AsNoTracking()
        .Include(sp => sp.Items)
        .FirstOrDefaultAsync(sp => sp.TokenHash == tokenHash, cancellationToken);

    if (sharePack is null || sharePack.ExpiresAt < now)
    {
        return Results.NotFound();
    }

    var competencyIds = sharePack.Items.Select(i => i.CompetencyId).ToList();
    var evidence = await db.Evidence
        .AsNoTracking()
        .Include(e => e.Competency)
        .FirstOrDefaultAsync(e => e.Id == evidenceId && competencyIds.Contains(e.CompetencyId), cancellationToken);

    if (evidence is null)
    {
        return Results.NotFound();
    }

    var stream = await storage.OpenReadAsync(evidence.BlobName, cancellationToken);
    return Results.File(stream, evidence.ContentType ?? "application/octet-stream", evidence.FileName);
});

app.Run();

static string GetUserId(ClaimsPrincipal user)
{
    return user.FindFirstValue("oid")
        ?? user.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? user.FindFirstValue("sub")
        ?? throw new InvalidOperationException("User id claim not found.");
}

static string? GetEmail(ClaimsPrincipal user)
{
    return user.FindFirstValue("preferred_username")
        ?? user.FindFirstValue("upn")
        ?? user.FindFirstValue(ClaimTypes.Email)
        ?? user.FindFirstValue("email");
}

static string ComputeStatus(DateTime expiresAt, DateTime now)
{
    if (expiresAt < now)
    {
        return "Expired";
    }

    if (expiresAt <= now.AddDays(30))
    {
        return "ExpiringSoon";
    }

    return "Valid";
}

static string NormalizeCategory(string? category)
{
    return string.IsNullOrWhiteSpace(category) ? "Mandatory" : category.Trim();
}

static bool IsValidCategory(string? category)
{
    if (string.IsNullOrWhiteSpace(category))
    {
        return false;
    }

    return category.Equals("Mandatory", StringComparison.OrdinalIgnoreCase)
        || category.Equals("Clinical", StringComparison.OrdinalIgnoreCase)
        || category.Equals("Specialist", StringComparison.OrdinalIgnoreCase);
}

static string? NormalizeRegistrationType(string? registrationType)
{
    return string.IsNullOrWhiteSpace(registrationType) ? null : registrationType.Trim();
}

static bool IsValidRegistrationType(string? registrationType)
{
    if (string.IsNullOrWhiteSpace(registrationType))
    {
        return false;
    }

    return registrationType.Equals("RN Adult", StringComparison.OrdinalIgnoreCase)
        || registrationType.Equals("RN Mental Health", StringComparison.OrdinalIgnoreCase)
        || registrationType.Equals("RN Learning Disability", StringComparison.OrdinalIgnoreCase)
        || registrationType.Equals("RN Child", StringComparison.OrdinalIgnoreCase);
}

static string? NormalizeNmcPin(string? nmcPin)
{
    if (string.IsNullOrWhiteSpace(nmcPin))
    {
        return string.Empty;
    }

    var normalized = nmcPin.Trim().ToUpperInvariant();
    return Regex.IsMatch(normalized, "^[A-Z]{2}\\d{6}$") ? normalized : string.Empty;
}

static long GetEvidenceMaxBytes(IConfiguration config)
{
    var maxBytes = config.GetValue<long?>("Evidence:MaxBytes");
    return maxBytes.HasValue && maxBytes.Value > 0 ? maxBytes.Value : 10 * 1024 * 1024;
}

static bool IsAllowedEvidence(string? contentType, string fileName)
{
    if (!string.IsNullOrWhiteSpace(contentType))
    {
        if (contentType.StartsWith("image/", StringComparison.OrdinalIgnoreCase))
        {
            return true;
        }

        return contentType.Equals("application/pdf", StringComparison.OrdinalIgnoreCase)
            || contentType.Equals("application/msword", StringComparison.OrdinalIgnoreCase)
            || contentType.Equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", StringComparison.OrdinalIgnoreCase);
    }

    var extension = Path.GetExtension(fileName)?.ToLowerInvariant();
    return extension is ".jpg" or ".jpeg" or ".png" or ".heic" or ".pdf" or ".doc" or ".docx";
}

static string ToUrlSafeToken(byte[] bytes)
{
    return Convert.ToBase64String(bytes)
        .Replace('+', '-')
        .Replace('/', '_')
        .TrimEnd('=');
}

static string HashToken(string token)
{
    var bytes = SHA256.HashData(System.Text.Encoding.UTF8.GetBytes(token));
    return Convert.ToHexString(bytes);
}
