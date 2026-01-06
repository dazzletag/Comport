using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class Competency
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(2000)]
    public string? Description { get; set; }

    public DateTime ExpiresAt { get; set; }

    [Required]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }

    public List<Evidence> Evidence { get; set; } = new();

    public List<SharePackItem> SharePackItems { get; set; } = new();
}
