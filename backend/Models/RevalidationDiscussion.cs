using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class RevalidationDiscussion
{
    [Key]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    public DateTime? ReflectiveDiscussionDate { get; set; }

    [MaxLength(200)]
    public string? ReflectiveRegistrantName { get; set; }

    [MaxLength(20)]
    public string? ReflectiveRegistrantPin { get; set; }

    public DateTime? ConfirmationDate { get; set; }

    [MaxLength(200)]
    public string? ConfirmerName { get; set; }

    [MaxLength(120)]
    public string? ConfirmerRole { get; set; }

    public DateTime UpdatedAt { get; set; }
}
