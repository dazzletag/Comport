using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class RevalidationReflection
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public DateTime Date { get; set; }

    [Required]
    [MaxLength(2000)]
    public string WhatHappened { get; set; } = string.Empty;

    [Required]
    [MaxLength(2000)]
    public string WhatLearned { get; set; } = string.Empty;

    [Required]
    [MaxLength(2000)]
    public string HowChanged { get; set; } = string.Empty;

    [Required]
    [MaxLength(200)]
    public string CodeThemes { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
