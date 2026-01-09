using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class RevalidationDeclaration
{
    [Key]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public bool HealthAndCharacter { get; set; }

    public bool Indemnity { get; set; }

    public DateTime UpdatedAt { get; set; }
}
