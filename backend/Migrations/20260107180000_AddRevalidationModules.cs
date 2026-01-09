using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Backend.Migrations
{
    /// <inheritdoc />
    public partial class AddRevalidationModules : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "PinExpiryDate",
                table: "NurseProfiles",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "RevalidationCycleStart",
                table: "NurseProfiles",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "RevalidationCycleEnd",
                table: "NurseProfiles",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "PushNotificationsEnabled",
                table: "NurseProfiles",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "EmailRemindersEnabled",
                table: "NurseProfiles",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<string>(
                name: "ReminderCadence",
                table: "NurseProfiles",
                type: "nvarchar(40)",
                maxLength: 40,
                nullable: true);

            migrationBuilder.CreateTable(
                name: "RevalidationPracticeHours",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    Date = table.Column<DateTime>(type: "datetime2", nullable: false),
                    Role = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    Setting = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    Hours = table.Column<decimal>(type: "decimal(18,2)", nullable: false),
                    Notes = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RevalidationPracticeHours", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "RevalidationCpdEntries",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    Date = table.Column<DateTime>(type: "datetime2", nullable: false),
                    Topic = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    Hours = table.Column<decimal>(type: "decimal(18,2)", nullable: false),
                    IsParticipatory = table.Column<bool>(type: "bit", nullable: false),
                    EvidenceFileName = table.Column<string>(type: "nvarchar(260)", maxLength: 260, nullable: true),
                    EvidenceContentType = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: true),
                    EvidenceBlobName = table.Column<string>(type: "nvarchar(400)", maxLength: 400, nullable: true),
                    EvidenceSize = table.Column<long>(type: "bigint", nullable: true),
                    EvidenceUploadedAt = table.Column<DateTime>(type: "datetime2", nullable: true),
                    Notes = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RevalidationCpdEntries", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "RevalidationFeedbackEntries",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    Date = table.Column<DateTime>(type: "datetime2", nullable: false),
                    Source = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: false),
                    Summary = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: false),
                    EvidenceFileName = table.Column<string>(type: "nvarchar(260)", maxLength: 260, nullable: true),
                    EvidenceContentType = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: true),
                    EvidenceBlobName = table.Column<string>(type: "nvarchar(400)", maxLength: 400, nullable: true),
                    EvidenceSize = table.Column<long>(type: "bigint", nullable: true),
                    EvidenceUploadedAt = table.Column<DateTime>(type: "datetime2", nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RevalidationFeedbackEntries", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "RevalidationReflections",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    Date = table.Column<DateTime>(type: "datetime2", nullable: false),
                    WhatHappened = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: false),
                    WhatLearned = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: false),
                    HowChanged = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: false),
                    CodeThemes = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RevalidationReflections", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "RevalidationDiscussions",
                columns: table => new
                {
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    ReflectiveDiscussionDate = table.Column<DateTime>(type: "datetime2", nullable: true),
                    ReflectiveRegistrantName = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: true),
                    ReflectiveRegistrantPin = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: true),
                    ConfirmationDate = table.Column<DateTime>(type: "datetime2", nullable: true),
                    ConfirmerName = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: true),
                    ConfirmerRole = table.Column<string>(type: "nvarchar(120)", maxLength: 120, nullable: true),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RevalidationDiscussions", x => x.UserId);
                });

            migrationBuilder.CreateTable(
                name: "RevalidationDeclarations",
                columns: table => new
                {
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    HealthAndCharacter = table.Column<bool>(type: "bit", nullable: false),
                    Indemnity = table.Column<bool>(type: "bit", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RevalidationDeclarations", x => x.UserId);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(name: "RevalidationPracticeHours");
            migrationBuilder.DropTable(name: "RevalidationCpdEntries");
            migrationBuilder.DropTable(name: "RevalidationFeedbackEntries");
            migrationBuilder.DropTable(name: "RevalidationReflections");
            migrationBuilder.DropTable(name: "RevalidationDiscussions");
            migrationBuilder.DropTable(name: "RevalidationDeclarations");

            migrationBuilder.DropColumn(name: "PinExpiryDate", table: "NurseProfiles");
            migrationBuilder.DropColumn(name: "RevalidationCycleStart", table: "NurseProfiles");
            migrationBuilder.DropColumn(name: "RevalidationCycleEnd", table: "NurseProfiles");
            migrationBuilder.DropColumn(name: "PushNotificationsEnabled", table: "NurseProfiles");
            migrationBuilder.DropColumn(name: "EmailRemindersEnabled", table: "NurseProfiles");
            migrationBuilder.DropColumn(name: "ReminderCadence", table: "NurseProfiles");
        }
    }
}
