using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class Evidence
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    public Guid CompetencyId { get; set; }

    public Competency? Competency { get; set; }

    [Required]
    [MaxLength(260)]
    public string FileName { get; set; } = string.Empty;

    [MaxLength(100)]
    public string? ContentType { get; set; }

    [Required]
    [MaxLength(400)]
    public string BlobName { get; set; } = string.Empty;

    public long Size { get; set; }

    public DateTime UploadedAt { get; set; }
}
