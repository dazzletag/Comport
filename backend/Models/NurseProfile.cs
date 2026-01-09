using System.ComponentModel.DataAnnotations;

namespace Backend.Models;

public sealed class NurseProfile
{
    [Key]
    [MaxLength(100)]
    public string UserId { get; set; } = string.Empty;

    [Required]
    [MaxLength(200)]
    public string FullName { get; set; } = string.Empty;

    [MaxLength(200)]
    public string? PreferredName { get; set; }

    [MaxLength(20)]
    public string? NmcPin { get; set; }

    [MaxLength(60)]
    public string? RegistrationType { get; set; }

    [MaxLength(200)]
    public string? Employer { get; set; }

    [MaxLength(80)]
    public string? RoleBand { get; set; }

    [MaxLength(200)]
    public string? Email { get; set; }

    [MaxLength(40)]
    public string? Phone { get; set; }

    [MaxLength(500)]
    public string? Bio { get; set; }

    public DateTime? PinExpiryDate { get; set; }

    public DateTime? RevalidationCycleStart { get; set; }

    public DateTime? RevalidationCycleEnd { get; set; }

    public bool PushNotificationsEnabled { get; set; }

    public bool EmailRemindersEnabled { get; set; }

    [MaxLength(40)]
    public string? ReminderCadence { get; set; }

    public DateTime UpdatedAt { get; set; }
}
