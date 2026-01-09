using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class RevalidationPracticeHour
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public DateTime Date { get; set; }

    [MaxLength(120)]
    public string? Role { get; set; }

    [MaxLength(120)]
    public string? Setting { get; set; }

    public decimal Hours { get; set; }

    [MaxLength(500)]
    public string? Notes { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
