using System.Collections.Concurrent;
using System.Globalization;

namespace AulaControlTeacher;

public sealed record RecoverySnapshot(
    string DeviceId,
    double? Latitude,
    double? Longitude,
    double? AccuracyM,
    DateTime? CapturedAt,
    bool Lost
)
{
    public bool HasLocation => Latitude.HasValue && Longitude.HasValue;
    public string StateText => Lost ? "MODO PÉRDIDA" : "Normal";
    public string LocationText => !HasLocation ? "Sin ubicación" : $"{Latitude:0.000000}, {Longitude:0.000000}";
    public string AccuracyText => AccuracyM.HasValue && AccuracyM.Value >= 0 ? $"±{AccuracyM:0} m" : "—";
    public string CapturedText => CapturedAt.HasValue ? CapturedAt.Value.ToString("dd-MM-yyyy HH:mm:ss") : "—";
    public string MapUrl => HasLocation ? $"https://www.google.com/maps?q={Latitude.Value.ToString(CultureInfo.InvariantCulture)},{Longitude.Value.ToString(CultureInfo.InvariantCulture)}" : "";
}

public static class LiveRecovery
{
    static readonly ConcurrentDictionary<string,RecoverySnapshot> data = new(StringComparer.OrdinalIgnoreCase);

    public static void Update(string id,string lat,string lon,string accuracy,long locationTs,bool lost)
    {
        double? la = double.TryParse(lat,NumberStyles.Float,CultureInfo.InvariantCulture,out var a) ? a : null;
        double? lo = double.TryParse(lon,NumberStyles.Float,CultureInfo.InvariantCulture,out var o) ? o : null;
        double? ac = double.TryParse(accuracy,NumberStyles.Float,CultureInfo.InvariantCulture,out var c) ? c : null;
        DateTime? at = locationTs > 0 ? DateTimeOffset.FromUnixTimeMilliseconds(locationTs).LocalDateTime : null;
        data[id] = new RecoverySnapshot(id,la,lo,ac,at,lost);
    }

    public static RecoverySnapshot Get(string id) => data.TryGetValue(id,out var x) ? x : new RecoverySnapshot(id,null,null,null,null,false);
}

public sealed class RecoveryRow
{
    public string DeviceId { get; init; } = "";
    public string DeviceName { get; init; } = "";
    public string Status { get; init; } = "";
    public string Person { get; init; } = "";
    public int Battery { get; init; }
    public string Recovery { get; init; } = "";
    public string Location { get; init; } = "";
    public string Accuracy { get; init; } = "";
    public string LocationTime { get; init; } = "";
    public string MapUrl { get; init; } = "";
}
