namespace Backend.Models;

public sealed class SharePackItem
{
    public Guid SharePackId { get; set; }

    public SharePack? SharePack { get; set; }

    public Guid CompetencyId { get; set; }

    public Competency? Competency { get; set; }
}
