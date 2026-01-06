using Azure;
using Azure.Identity;
using Azure.Storage.Blobs;
using Azure.Storage.Blobs.Models;

namespace Backend.Services;

public sealed class BlobStorageService
{
    private readonly BlobContainerClient _container;

    public BlobStorageService(IConfiguration config)
    {
        var connectionString = config["Storage:ConnectionString"];
        var accountName = config["Storage:AccountName"];
        var containerName = config["Storage:ContainerName"] ?? "evidence";

        if (!string.IsNullOrWhiteSpace(connectionString))
        {
            _container = new BlobContainerClient(connectionString, containerName);
        }
        else
        {
            if (string.IsNullOrWhiteSpace(accountName))
            {
                throw new InvalidOperationException("Storage:AccountName must be set when not using a connection string.");
            }

            var endpoint = new Uri($"https://{accountName}.blob.core.windows.net/{containerName}");
            _container = new BlobContainerClient(endpoint, new DefaultAzureCredential());
        }
    }

    public async Task EnsureContainerAsync(CancellationToken cancellationToken)
    {
        await _container.CreateIfNotExistsAsync(PublicAccessType.None, cancellationToken: cancellationToken);
    }

    public async Task UploadAsync(string blobName, Stream content, string? contentType, CancellationToken cancellationToken)
    {
        var client = _container.GetBlobClient(blobName);
        var options = new BlobUploadOptions
        {
            HttpHeaders = new BlobHttpHeaders { ContentType = contentType }
        };

        await client.UploadAsync(content, options, cancellationToken);
    }

    public async Task<Stream> OpenReadAsync(string blobName, CancellationToken cancellationToken)
    {
        var client = _container.GetBlobClient(blobName);
        var response = await client.DownloadStreamingAsync(cancellationToken: cancellationToken);
        return response.Value.Content;
    }
}
