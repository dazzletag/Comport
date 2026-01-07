using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Backend.Migrations
{
    /// <inheritdoc />
    public partial class AddProfileEvidenceMetadata : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "AchievedAt",
                table: "Competencies",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "Category",
                table: "Competencies",
                type: "nvarchar(40)",
                maxLength: 40,
                nullable: false,
                defaultValue: "Mandatory");

            migrationBuilder.AddColumn<string>(
                name: "Note",
                table: "Evidence",
                type: "nvarchar(400)",
                maxLength: 400,
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "IncludeNmcPin",
                table: "SharePacks",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<string>(
                name: "NmcPin",
                table: "SharePacks",
                type: "nvarchar(20)",
                maxLength: 20,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "NurseName",
                table: "SharePacks",
                type: "nvarchar(200)",
                maxLength: 200,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "RegistrationType",
                table: "SharePacks",
                type: "nvarchar(60)",
                maxLength: 60,
                nullable: true);

            migrationBuilder.CreateTable(
                name: "NurseProfiles",
                columns: table => new
                {
                    UserId = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    FullName = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    PreferredName = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: true),
                    NmcPin = table.Column<string>(type: "nvarchar(20)", maxLength: 20, nullable: true),
                    RegistrationType = table.Column<string>(type: "nvarchar(60)", maxLength: 60, nullable: true),
                    Employer = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: true),
                    RoleBand = table.Column<string>(type: "nvarchar(80)", maxLength: 80, nullable: true),
                    Email = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: true),
                    Phone = table.Column<string>(type: "nvarchar(40)", maxLength: 40, nullable: true),
                    Bio = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_NurseProfiles", x => x.UserId);
                });

            migrationBuilder.Sql("UPDATE Competencies SET AchievedAt = ISNULL(AchievedAt, CreatedAt)");

            migrationBuilder.AlterColumn<DateTime>(
                name: "AchievedAt",
                table: "Competencies",
                type: "datetime2",
                nullable: false,
                oldClrType: typeof(DateTime),
                oldType: "datetime2",
                oldNullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "NurseProfiles");

            migrationBuilder.DropColumn(
                name: "AchievedAt",
                table: "Competencies");

            migrationBuilder.DropColumn(
                name: "Category",
                table: "Competencies");

            migrationBuilder.DropColumn(
                name: "Note",
                table: "Evidence");

            migrationBuilder.DropColumn(
                name: "IncludeNmcPin",
                table: "SharePacks");

            migrationBuilder.DropColumn(
                name: "NmcPin",
                table: "SharePacks");

            migrationBuilder.DropColumn(
                name: "NurseName",
                table: "SharePacks");

            migrationBuilder.DropColumn(
                name: "RegistrationType",
                table: "SharePacks");
        }
    }
}
