using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class RevalidationFeedback
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public DateTime Date { get; set; }

    [Required]
    [MaxLength(60)]
    public string Source { get; set; } = string.Empty;

    [Required]
    [MaxLength(2000)]
    public string Summary { get; set; } = string.Empty;

    [MaxLength(260)]
    public string? EvidenceFileName { get; set; }

    [MaxLength(100)]
    public string? EvidenceContentType { get; set; }

    [MaxLength(400)]
    public string? EvidenceBlobName { get; set; }

    public long? EvidenceSize { get; set; }

    public DateTime? EvidenceUploadedAt { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
