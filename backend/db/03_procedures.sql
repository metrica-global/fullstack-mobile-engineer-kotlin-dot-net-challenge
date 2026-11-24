USE FieldOps;
GO

CREATE OR ALTER PROCEDURE dbo.sp_Auth_Login
    @Username VARCHAR(50),
    @Password VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        Id,
        Username,
        FullName,
        Role
    FROM dbo.Users
    WHERE
        Username = @Username
        AND Password = @Password
        AND IsActive = 1;
END;
GO

CREATE OR ALTER PROCEDURE dbo.sp_WorkOrders_List
    @TechnicianId INT = NULL,
    @Status VARCHAR(20) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        Id,
        Code,
        Title,
        Description,
        CustomerName,
        Address,
        Priority,
        Status,
        TechnicianId,
        ScheduledAt,
        UpdatedAt
    FROM dbo.WorkOrders
WHERE
    (@TechnicianId IS NULL OR TechnicianId = @TechnicianId)
    AND (@Status IS NULL OR Status = @Status)
ORDER BY ScheduledAt;
END;
GO

CREATE OR ALTER PROCEDURE dbo.sp_WorkOrders_UpdateStatus
    @WorkOrderId INT,
    @TechnicianId INT,
    @Status VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.WorkOrders
    SET
        Status = @Status,
        UpdatedAt = GETDATE()
    WHERE
        Id = @WorkOrderId
        AND TechnicianId = @TechnicianId;

    SELECT @@ROWCOUNT AS UpdatedRows;
END;
GO

-- Guarda el hash del refresh token después de un login válido
CREATE OR ALTER PROCEDURE dbo.sp_Auth_CreateRefreshToken
    @UserId INT,
    @TokenHash BINARY(32),
    @ExpiresAt DATETIME2(0)
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.RefreshTokens (UserId, TokenHash, ExpiresAt)
    VALUES (@UserId, @TokenHash, @ExpiresAt);
END;
GO

-- Maneja el token 
CREATE OR ALTER PROCEDURE dbo.sp_Auth_RotateRefreshToken
    @CurrentTokenHash BINARY(32),
    @NewTokenHash BINARY(32),
    @NewExpiresAt DATETIME2(0)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @ConsumedUsers TABLE (UserId INT NOT NULL);

    BEGIN TRY
        BEGIN TRANSACTION;

        UPDATE refreshToken
        SET RevokedAt = SYSUTCDATETIME()
        OUTPUT inserted.UserId INTO @ConsumedUsers (UserId)
        FROM dbo.RefreshTokens AS refreshToken
        INNER JOIN dbo.Users AS userAccount
            ON userAccount.Id = refreshToken.UserId
            AND userAccount.IsActive = 1
        WHERE refreshToken.TokenHash = @CurrentTokenHash
            AND refreshToken.RevokedAt IS NULL
            AND refreshToken.ExpiresAt > SYSUTCDATETIME();

        INSERT INTO dbo.RefreshTokens (UserId, TokenHash, ExpiresAt)
        SELECT UserId, @NewTokenHash, @NewExpiresAt
        FROM @ConsumedUsers;

        SELECT
            userAccount.Id,
            userAccount.Username,
            userAccount.FullName,
            userAccount.Role
        FROM dbo.Users AS userAccount
        INNER JOIN @ConsumedUsers AS consumedUser
            ON consumedUser.UserId = userAccount.Id
        WHERE userAccount.IsActive = 1;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH
END;
GO