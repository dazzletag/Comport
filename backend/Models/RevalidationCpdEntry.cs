using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class RevalidationCpdEntry
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public DateTime Date { get; set; }

    [Required]
    [MaxLength(200)]
    public string Topic { get; set; } = string.Empty;

    public decimal Hours { get; set; }

    public bool IsParticipatory { get; set; }

    [MaxLength(260)]
    public string? EvidenceFileName { get; set; }

    [MaxLength(100)]
    public string? EvidenceContentType { get; set; }

    [MaxLength(400)]
    public string? EvidenceBlobName { get; set; }

    public long? EvidenceSize { get; set; }

    public DateTime? EvidenceUploadedAt { get; set; }

    [MaxLength(500)]
    public string? Notes { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
