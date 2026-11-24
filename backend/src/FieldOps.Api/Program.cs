using FieldOps.Api.Data;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers().AddNewtonsoftJson();
builder.Services.AddScoped<AuthDataAccess>();
builder.Services.AddScoped<WorkOrdersDataAccess>();

var encodedSigningKey = builder.Configuration["Jwt:SigningKey"]
    ?? throw new InvalidOperationException(
        "Falta la configuración Jwt:SigningKey.");

var issuer = builder.Configuration["Jwt:Issuer"]
    ?? throw new InvalidOperationException("Falta Jwt:Issuer.");

var audience = builder.Configuration["Jwt:Audience"]
    ?? throw new InvalidOperationException("Falta Jwt:Audience.");

var accessTokenMinutes =
    builder.Configuration.GetValue<int>("Jwt:AccessTokenMinutes");

if (accessTokenMinutes <= 0)
{
    throw new InvalidOperationException(
        "Jwt:AccessTokenMinutes debe ser mayor que cero.");
}

var signingKey = new SymmetricSecurityKey(
    Convert.FromBase64String(encodedSigningKey));

if (signingKey.KeySize < 256)
{
    throw new InvalidOperationException(
        "La clave JWT debe tener al menos 256 bits.");
}

var signingCredentials = new SigningCredentials(
    signingKey,
    SecurityAlgorithms.HmacSha256);

builder.Services.AddSingleton(signingCredentials);

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = signingKey,
            ValidateIssuer = true,
            ValidIssuer = issuer,
            ValidateAudience = true,
            ValidAudience = audience,
            ValidateLifetime = true,
            RequireExpirationTime = true,
            ClockSkew = TimeSpan.Zero,
            NameClaimType = "username",
            RoleClaimType = "role"
        };
    });

builder.Services.AddAuthorization();
builder.Services.AddOpenApi();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();