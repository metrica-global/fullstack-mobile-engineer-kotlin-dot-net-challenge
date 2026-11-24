#nullable enable

using Microsoft.Data.SqlClient;
using System.Data;

namespace FieldOps.Api.Data;

public sealed class AuthDataAccess
{
    private readonly string _connectionString;

    public AuthDataAccess(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("FieldOps")
            ?? throw new InvalidOperationException(
                "Falta la cadena de conexión 'FieldOps'.");
    }

    public async Task<UserLoginResult?> LoginAsync(
        string username,
        string password,
        CancellationToken cancellationToken)
    {
        await using var connection =
            new SqlConnection(_connectionString);

        await connection.OpenAsync(cancellationToken);

        //llama al proc almacenado
        await using var command =
            new SqlCommand("dbo.sp_Auth_Login", connection);

        command.CommandType = System.Data.CommandType.StoredProcedure;

        command.Parameters.Add(
            "@Username",
            SqlDbType.VarChar,
            50).Value = username;

        command.Parameters.Add(
            "@Password",
            SqlDbType.VarChar,
            100).Value = password;

        await using var reader =
            await command.ExecuteReaderAsync(cancellationToken);

        if (!await reader.ReadAsync(cancellationToken))
        {
            return null;
        }

        return new UserLoginResult(
            reader.GetInt32(reader.GetOrdinal("Id")),
            reader.GetString(reader.GetOrdinal("Username")),
            reader.GetString(reader.GetOrdinal("FullName")),
            reader.GetString(reader.GetOrdinal("Role")));
    }

    public async Task CreateRefreshTokenAsync(
    int userId,
    byte[] tokenHash,
    DateTime expiresAt,
    CancellationToken cancellationToken)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);

        await using var command =
            new SqlCommand("dbo.sp_Auth_CreateRefreshToken", connection);

        command.CommandType = CommandType.StoredProcedure;
        command.Parameters.Add("@UserId", SqlDbType.Int).Value = userId;
        command.Parameters.Add("@TokenHash", SqlDbType.Binary, 32).Value = tokenHash;

        var expiresAtParameter =
            command.Parameters.Add("@ExpiresAt", SqlDbType.DateTime2);
        expiresAtParameter.Scale = 0;
        expiresAtParameter.Value = expiresAt;

        await command.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<UserLoginResult?> RotateRefreshTokenAsync(
        byte[] currentTokenHash,
        byte[] newTokenHash,
        DateTime newExpiresAt,
        CancellationToken cancellationToken)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);

        await using var command =
            new SqlCommand("dbo.sp_Auth_RotateRefreshToken", connection);

        command.CommandType = CommandType.StoredProcedure;
        command.Parameters.Add(
            "@CurrentTokenHash",
            SqlDbType.Binary,
            32).Value = currentTokenHash;
        command.Parameters.Add(
            "@NewTokenHash",
            SqlDbType.Binary,
            32).Value = newTokenHash;

        var expiresAtParameter =
            command.Parameters.Add("@NewExpiresAt", SqlDbType.DateTime2);
        expiresAtParameter.Scale = 0;
        expiresAtParameter.Value = newExpiresAt;

        await using var reader =
            await command.ExecuteReaderAsync(cancellationToken);

        if (!await reader.ReadAsync(cancellationToken))
        {
            return null;
        }

        return new UserLoginResult(
            reader.GetInt32(reader.GetOrdinal("Id")),
            reader.GetString(reader.GetOrdinal("Username")),
            reader.GetString(reader.GetOrdinal("FullName")),
            reader.GetString(reader.GetOrdinal("Role")));
    }
}

public sealed record UserLoginResult(
    int Id,
    string Username,
    string FullName,
    string Role);