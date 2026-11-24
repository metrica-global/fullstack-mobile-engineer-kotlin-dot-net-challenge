#nullable enable

using Microsoft.AspNetCore.Mvc;
using System.ComponentModel.DataAnnotations;
using FieldOps.Api.Data;
using Microsoft.AspNetCore.Authorization;

namespace FieldOps.Api.Controllers
{
    [Authorize(Roles = "TECHNICIAN,SUPERVISOR")]
    [ApiController]
    [Route("api/workorders")]
    public class WorkOrdersController : ControllerBase
    {
        private readonly WorkOrdersDataAccess _workOrdersDataAccess;

        public WorkOrdersController(
            WorkOrdersDataAccess workOrdersDataAccess)
        {
            _workOrdersDataAccess = workOrdersDataAccess;
        }

        // GET api/workorders?status=PENDING
        [HttpGet]
        public async Task<ActionResult<IReadOnlyList<WorkOrderRecord>>> GetOrders(
            [FromQuery] string? status,
            CancellationToken cancellationToken)
        {
            int? technicianId;

            if (User.IsInRole("SUPERVISOR"))
            {
                technicianId = null;
            }
            else if (int.TryParse(
                User.FindFirst("userId")?.Value,
                out var authenticatedTechnicianId))
            {
                technicianId = authenticatedTechnicianId;
            }
            else
            {
                return Forbid();
            }

            var orders = await _workOrdersDataAccess.GetOrdersAsync(
                technicianId,
                status,
                cancellationToken);

            return Ok(orders);
        }


        // PUT api/workorders/5/status
        [Authorize(Roles = "TECHNICIAN")]
        [HttpPut("{id:int}/status")]
        public async Task<IActionResult> UpdateStatus(
            int id,
            [FromBody] UpdateStatusRequest request,
            CancellationToken cancellationToken)
        {
            if (!int.TryParse(User.FindFirst("userId")?.Value, out var technicianId))
            {
                return Forbid();
            }

            var updated = await _workOrdersDataAccess.UpdateStatusAsync(
                id,
                technicianId,
                request.Status,
                cancellationToken);

            if (!updated)
            {
                return NotFound();
            }

            return Ok(new { ok = true });
        }

    }

    /// <summary>Datos para cambiar el estado de una orden.</summary>
    public sealed record UpdateStatusRequest
    {
        [Required]
        [RegularExpression("^(PENDING|IN_PROGRESS|COMPLETED)$")]
        public string Status { get; init; } = string.Empty;
    }
}
