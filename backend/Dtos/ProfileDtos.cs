namespace Backend.Dtos;

public sealed record NurseProfileDto(
    string FullName,
    string? PreferredName,
    string? NmcPin,
    string? RegistrationType,
    string? Employer,
    string? RoleBand,
    string? Email,
    string? Phone,
    string? Bio,
    DateTime? PinExpiryDate,
    DateTime? RevalidationCycleStart,
    DateTime? RevalidationCycleEnd,
    bool PushNotificationsEnabled,
    bool EmailRemindersEnabled,
    string? ReminderCadence
);

public sealed record NurseProfileUpdateRequest(
    string FullName,
    string? PreferredName,
    string? NmcPin,
    string? RegistrationType,
    string? Employer,
    string? RoleBand,
    string? Phone,
    string? Bio,
    DateTime? PinExpiryDate,
    DateTime? RevalidationCycleStart,
    DateTime? RevalidationCycleEnd,
    bool PushNotificationsEnabled,
    bool EmailRemindersEnabled,
    string? ReminderCadence
);
