using System.ComponentModel.DataAnnotations;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using FieldOps.Api.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;

namespace FieldOps.Api.Controllers
{
    [ApiController]
    [Route("api/auth")]
    public class AuthController : ControllerBase
    {
        private readonly AuthDataAccess _authDataAccess;
        private readonly SigningCredentials _signingCredentials;
        private readonly string _issuer;
        private readonly string _audience;
        private readonly int _accessTokenMinutes;
        private readonly int _refreshTokenDays;

        public AuthController(
            AuthDataAccess authDataAccess,
            SigningCredentials signingCredentials,
            IConfiguration configuration)
        {
            _authDataAccess = authDataAccess;
            _signingCredentials = signingCredentials;
            _issuer = configuration["Jwt:Issuer"]
                ?? throw new InvalidOperationException("Falta Jwt:Issuer.");
            _audience = configuration["Jwt:Audience"]
                ?? throw new InvalidOperationException("Falta Jwt:Audience.");
            _accessTokenMinutes =
                configuration.GetValue<int>("Jwt:AccessTokenMinutes");
            _refreshTokenDays =
                configuration.GetValue<int>("Jwt:RefreshTokenDays");

            if (_accessTokenMinutes <= 0 || _refreshTokenDays <= 0)
            {
                throw new InvalidOperationException(
                    "Las duraciones de los tokens deben ser mayores que cero.");
            }
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login(
            [FromBody] LoginRequest request,
            CancellationToken cancellationToken)
        {
            var user = await _authDataAccess.LoginAsync(
                request.Username,
                request.Password,
                cancellationToken);

            if (user is null)
            {
                return Unauthorized(new
                {
                    ok = false,
                    message = "Usuario o clave incorrectos"
                });
            }

            var refreshToken = GenerateRefreshToken();

            await _authDataAccess.CreateRefreshTokenAsync(
                user.Id,
                HashRefreshToken(refreshToken),
                DateTime.UtcNow.AddDays(_refreshTokenDays),
                cancellationToken);

            return Ok(new
            {
                ok = true,
                token = CreateAccessToken(user),
                refreshToken,
                user = new[]
                {
                    new
                    {
                        id = user.Id,
                        username = user.Username,
                        fullName = user.FullName,
                        role = user.Role
                    }
                }
            });
        }

        [HttpPost("refresh")]
        public async Task<IActionResult> Refresh(
            [FromBody] RefreshTokenRequest request,
            CancellationToken cancellationToken)
        {
            var newRefreshToken = GenerateRefreshToken();

            var user = await _authDataAccess.RotateRefreshTokenAsync(
                HashRefreshToken(request.RefreshToken),
                HashRefreshToken(newRefreshToken),
                DateTime.UtcNow.AddDays(_refreshTokenDays),
                cancellationToken);

            if (user is null)
            {
                return Unauthorized(new
                {
                    ok = false,
                    message = "Refresh token inválido o vencido"
                });
            }

            return Ok(new
            {
                ok = true,
                token = CreateAccessToken(user),
                refreshToken = newRefreshToken
            });
        }

        private string CreateAccessToken(UserLoginResult user)
        {
            var now = DateTime.UtcNow;
            var claims = new[]
            {
                new Claim("userId", user.Id.ToString()),
                new Claim("username", user.Username),
                new Claim("role", user.Role)
            };

            var jwt = new JwtSecurityToken(
                issuer: _issuer,
                audience: _audience,
                claims: claims,
                notBefore: now,
                expires: now.AddMinutes(_accessTokenMinutes),
                signingCredentials: _signingCredentials);

            return new JwtSecurityTokenHandler().WriteToken(jwt);
        }

        private static string GenerateRefreshToken()
        {
            return Convert.ToBase64String(
                RandomNumberGenerator.GetBytes(32));
        }

        private static byte[] HashRefreshToken(string refreshToken)
        {
            return SHA256.HashData(Encoding.UTF8.GetBytes(refreshToken));
        }
    }

    public class LoginRequest
    {
        public string Username { get; set; }
        public string Password { get; set; }
    }

    /// <summary>Datos para solicitar nuevos tokens.</summary>
    public sealed record RefreshTokenRequest
    {
        [Required]
        public string RefreshToken { get; init; } = string.Empty;
    }
}