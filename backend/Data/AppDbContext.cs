using Backend.Models;
using Microsoft.EntityFrameworkCore;

namespace Backend.Data;

public sealed class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<Competency> Competencies => Set<Competency>();
    public DbSet<Evidence> Evidence => Set<Evidence>();
    public DbSet<NurseProfile> NurseProfiles => Set<NurseProfile>();
    public DbSet<SharePack> SharePacks => Set<SharePack>();
    public DbSet<SharePackItem> SharePackItems => Set<SharePackItem>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<SharePackItem>()
            .HasKey(x => new { x.SharePackId, x.CompetencyId });

        modelBuilder.Entity<SharePackItem>()
            .HasOne(x => x.SharePack)
            .WithMany(x => x.Items)
            .HasForeignKey(x => x.SharePackId);

        modelBuilder.Entity<SharePackItem>()
            .HasOne(x => x.Competency)
            .WithMany(x => x.SharePackItems)
            .HasForeignKey(x => x.CompetencyId);

        modelBuilder.Entity<NurseProfile>()
            .HasKey(x => x.UserId);
    }
}
