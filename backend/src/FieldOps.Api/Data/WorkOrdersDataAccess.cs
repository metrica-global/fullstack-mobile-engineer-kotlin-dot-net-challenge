#nullable enable

using System.Data;
using Microsoft.Data.SqlClient;

namespace FieldOps.Api.Data;

public sealed class WorkOrdersDataAccess
{
    private readonly string _connectionString;

    public WorkOrdersDataAccess(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("FieldOps")
            ?? throw new InvalidOperationException(
                "Falta la cadena de conexión 'FieldOps'.");
    }

    public async Task<IReadOnlyList<WorkOrderRecord>> GetOrdersAsync(
        int? technicianId,
        string? status,
        CancellationToken cancellationToken)
    {
        var orders = new List<WorkOrderRecord>();

        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);

        await using var command =
            new SqlCommand("dbo.sp_WorkOrders_List", connection);

        command.CommandType = CommandType.StoredProcedure;

        var technicianIdParameter =
            command.Parameters.Add("@TechnicianId", SqlDbType.Int);

        technicianIdParameter.Value =
            (object?)technicianId ?? DBNull.Value;

        var statusParameter = command.Parameters.Add(
            "@Status",
            SqlDbType.VarChar,
            20);
        statusParameter.Value = (object?)status ?? DBNull.Value;

        await using var reader =
            await command.ExecuteReaderAsync(cancellationToken);

        while (await reader.ReadAsync(cancellationToken))
        {
            orders.Add(new WorkOrderRecord(
                reader.GetInt32(reader.GetOrdinal("Id")),
                reader.GetString(reader.GetOrdinal("Code")),
                reader.GetString(reader.GetOrdinal("Title")),
                reader.IsDBNull(reader.GetOrdinal("Description"))
                    ? null
                    : reader.GetString(reader.GetOrdinal("Description")),
                reader.GetString(reader.GetOrdinal("CustomerName")),
                reader.GetString(reader.GetOrdinal("Address")),
                reader.GetString(reader.GetOrdinal("Priority")),
                reader.GetString(reader.GetOrdinal("Status")),
                reader.GetInt32(reader.GetOrdinal("TechnicianId")),
                reader.GetDateTime(reader.GetOrdinal("ScheduledAt")),
                reader.IsDBNull(reader.GetOrdinal("UpdatedAt"))
                    ? null
                    : reader.GetDateTime(reader.GetOrdinal("UpdatedAt"))));
        }

        return orders;
    }

    public async Task<bool> UpdateStatusAsync(
    int workOrderId,
    int technicianId,
    string status,
    CancellationToken cancellationToken)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);

        await using var command =
            new SqlCommand("dbo.sp_WorkOrders_UpdateStatus", connection);

        command.CommandType = CommandType.StoredProcedure;

        command.Parameters.Add(
            "@WorkOrderId",
            SqlDbType.Int).Value = workOrderId;

        command.Parameters.Add(
            "@TechnicianId",
            SqlDbType.Int).Value = technicianId;

        command.Parameters.Add(
            "@Status",
            SqlDbType.VarChar,
            20).Value = status;

        var result = await command.ExecuteScalarAsync(cancellationToken);

        return result is int updatedRows && updatedRows > 0;
    }
}

public sealed record WorkOrderRecord(
    int Id,
    string Code,
    string Title,
    string? Description,
    string CustomerName,
    string Address,
    string Priority,
    string Status,
    int TechnicianId,
    DateTime ScheduledAt,
    DateTime? UpdatedAt);