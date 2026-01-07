using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class SharePack
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string OwnerUserId { get; set; } = string.Empty;

    [Required]
    [MaxLength(128)]
    public string TokenHash { get; set; } = string.Empty;

    [MaxLength(200)]
    public string? NurseName { get; set; }

    [MaxLength(60)]
    public string? RegistrationType { get; set; }

    [MaxLength(200)]
    public string? Email { get; set; }

    [MaxLength(20)]
    public string? NmcPin { get; set; }

    public bool IncludeNmcPin { get; set; }

    public DateTime ExpiresAt { get; set; }

    public DateTime CreatedAt { get; set; }

    public List<SharePackItem> Items { get; set; } = new();
}
