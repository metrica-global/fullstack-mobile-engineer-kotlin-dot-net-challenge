-- FieldOps - esquema de base de datos
-- Se ejecuta automaticamente al levantar docker compose (servicio db-init).

IF DB_ID(N'FieldOps') IS NULL
    CREATE DATABASE FieldOps;
GO

USE FieldOps;
GO

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users
    (
        Id        INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Username  VARCHAR(50)   NOT NULL,
        Password  VARCHAR(100)  NOT NULL,
        FullName  NVARCHAR(120) NOT NULL,
        Role      VARCHAR(20)   NOT NULL DEFAULT ('TECHNICIAN'),
        IsActive  BIT           NOT NULL DEFAULT (1),
        CreatedAt DATETIME      NOT NULL DEFAULT (GETDATE())
    );
END
GO
 
-- nuevo
IF OBJECT_ID(N'dbo.RefreshTokens', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RefreshTokens
    (
        Id        INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserId    INT NOT NULL,
        TokenHash BINARY(32) NOT NULL,
        CreatedAt DATETIME2(0) NOT NULL
            CONSTRAINT DF_RefreshTokens_CreatedAt
            DEFAULT (SYSUTCDATETIME()),
        ExpiresAt DATETIME2(0) NOT NULL,
        RevokedAt DATETIME2(0) NULL,

        CONSTRAINT FK_RefreshTokens_Users
            FOREIGN KEY (UserId) REFERENCES dbo.Users (Id),

        CONSTRAINT UQ_RefreshTokens_TokenHash
            UNIQUE (TokenHash)
    );
END
GO

IF OBJECT_ID(N'dbo.WorkOrders', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.WorkOrders
    (
        Id           INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Code         VARCHAR(20)   NOT NULL,
        Title        NVARCHAR(150) NOT NULL,
        Description  NVARCHAR(500) NULL,
        CustomerName NVARCHAR(120) NOT NULL,
        Address      NVARCHAR(200) NOT NULL,
        Priority     VARCHAR(10)   NOT NULL,   -- LOW | MEDIUM | HIGH
        Status       VARCHAR(20)   NOT NULL,   -- PENDING | IN_PROGRESS | COMPLETED
        TechnicianId INT           NOT NULL REFERENCES dbo.Users (Id),
        ScheduledAt  DATETIME      NOT NULL,
        UpdatedAt    DATETIME      NULL
    );
END
GO


