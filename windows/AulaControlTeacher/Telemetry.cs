using System.Globalization;
using System.Text;

namespace AulaControlTeacher;

public sealed record TelemetrySample(
    DateTime Timestamp,
    string DeviceId,
    bool SessionActive,
    string Role,
    string Course,
    int Battery,
    bool Managed
);

public sealed class DeviceUsageSummary
{
    public string DeviceId { get; init; } = "";
    public string DeviceName { get; init; } = "";
    public int Samples { get; init; }
    public int Sessions { get; init; }
    public double ApproxHours { get; init; }
    public double StudentHours { get; init; }
    public double TeacherHours { get; init; }
    public int BatteryAverage { get; init; }
    public int BatteryMinimum { get; init; }
    public DateTime LastSeen { get; init; }
    public string LastSeenText => LastSeen == default ? "—" : LastSeen.ToString("dd-MM-yyyy HH:mm");
    public string UsageText => $"{ApproxHours:0.0} h";
    public string StudentText => $"{StudentHours:0.0} h";
    public string TeacherText => $"{TeacherHours:0.0} h";
}

public sealed class TelemetryStore
{
    readonly object gate = new();
    readonly Dictionary<string, DateTime> lastPersist = new();
    readonly Dictionary<string, bool> lastSessionState = new();
    readonly Dictionary<string, int> sessionStarts = new();
    readonly string dir;

    public TelemetryStore()
    {
        dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TabletEscolar", "telemetry");
        Directory.CreateDirectory(dir);
    }

    string FileFor(DateTime t) => Path.Combine(dir, $"usage-{t:yyyy-MM}.csv");

    public void Observe(Device d)
    {
        var now = DateTime.Now;
        lock (gate)
        {
            bool active = !string.IsNullOrWhiteSpace(d.User);
            if (lastSessionState.TryGetValue(d.Id, out var oldActive) && !oldActive && active)
                sessionStarts[d.Id] = sessionStarts.GetValueOrDefault(d.Id) + 1;
            else if (!lastSessionState.ContainsKey(d.Id) && active)
                sessionStarts[d.Id] = sessionStarts.GetValueOrDefault(d.Id) + 1;
            lastSessionState[d.Id] = active;

            if (lastPersist.TryGetValue(d.Id, out var last) && now - last < TimeSpan.FromSeconds(55))
                return;

            lastPersist[d.Id] = now;
            var path = FileFor(now);
            if (!System.IO.File.Exists(path))
                System.IO.File.WriteAllText(path, "timestamp,device_id,session_active,role,course,battery,managed\r\n", Encoding.UTF8);

            string row = string.Join(',',
                Csv(now.ToString("O", CultureInfo.InvariantCulture)),
                Csv(d.Id),
                active ? "1" : "0",
                Csv(d.Role ?? ""),
                Csv(d.Course ?? ""),
                d.Battery.ToString(CultureInfo.InvariantCulture),
                d.Managed ? "1" : "0") + "\r\n";
            System.IO.File.AppendAllText(path, row, Encoding.UTF8);
        }
    }

    public List<DeviceUsageSummary> Summaries(IEnumerable<Device> devices, TimeSpan period)
    {
        var since = DateTime.Now - period;
        var names = devices.ToDictionary(x => x.Id, x => x.Name);
        var samples = ReadSince(since);
        var result = new List<DeviceUsageSummary>();

        foreach (var g in samples.GroupBy(x => x.DeviceId))
        {
            var ordered = g.OrderBy(x => x.Timestamp).ToList();
            int sessions = 0;
            bool previous = false;
            bool first = true;
            foreach (var s in ordered)
            {
                if (s.SessionActive && (first || !previous)) sessions++;
                previous = s.SessionActive;
                first = false;
            }

            // Cada muestra representa aproximadamente un minuto de presencia del dispositivo.
            double allHours = ordered.Count / 60.0;
            double student = ordered.Count(x => x.SessionActive && x.Role == "student") / 60.0;
            double teacher = ordered.Count(x => x.SessionActive && x.Role == "teacher") / 60.0;
            var batteries = ordered.Where(x => x.Battery >= 0).Select(x => x.Battery).ToList();

            result.Add(new DeviceUsageSummary
            {
                DeviceId = g.Key,
                DeviceName = names.GetValueOrDefault(g.Key, g.Key),
                Samples = ordered.Count,
                Sessions = sessions,
                ApproxHours = allHours,
                StudentHours = student,
                TeacherHours = teacher,
                BatteryAverage = batteries.Count == 0 ? -1 : (int)Math.Round(batteries.Average()),
                BatteryMinimum = batteries.Count == 0 ? -1 : batteries.Min(),
                LastSeen = ordered.Last().Timestamp
            });
        }

        foreach (var d in devices)
            if (result.All(x => x.DeviceId != d.Id))
                result.Add(new DeviceUsageSummary { DeviceId = d.Id, DeviceName = d.Name, LastSeen = d.Seen });

        return result.OrderByDescending(x => x.LastSeen).ThenBy(x => x.DeviceName).ToList();
    }

    public List<TelemetrySample> ReadSince(DateTime since)
    {
        var list = new List<TelemetrySample>();
        lock (gate)
        {
            foreach (var file in Directory.EnumerateFiles(dir, "usage-*.csv"))
            {
                foreach (var line in System.IO.File.ReadLines(file).Skip(1))
                {
                    var p = ParseCsv(line);
                    if (p.Count < 7) continue;
                    if (!DateTime.TryParse(p[0], null, DateTimeStyles.RoundtripKind, out var ts) || ts < since) continue;
                    list.Add(new TelemetrySample(
                        ts,
                        p[1],
                        p[2] == "1",
                        p[3],
                        p[4],
                        int.TryParse(p[5], out var b) ? b : -1,
                        p[6] == "1"));
                }
            }
        }
        return list;
    }

    public string ExportCsv(string deviceId, TimeSpan period)
    {
        var since = DateTime.Now - period;
        var rows = ReadSince(since).Where(x => x.DeviceId == deviceId).OrderBy(x => x.Timestamp).ToList();
        var outDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "Tablet Escolar", "Informes");
        Directory.CreateDirectory(outDir);
        var path = Path.Combine(outDir, $"{Safe(deviceId)}_{DateTime.Now:yyyyMMdd_HHmm}.csv");
        var sb = new StringBuilder("timestamp,device_id,session_active,role,course,battery,managed\r\n");
        foreach (var x in rows)
            sb.Append(Csv(x.Timestamp.ToString("O"))).Append(',').Append(Csv(x.DeviceId)).Append(',')
              .Append(x.SessionActive ? "1" : "0").Append(',').Append(Csv(x.Role)).Append(',')
              .Append(Csv(x.Course)).Append(',').Append(x.Battery).Append(',').Append(x.Managed ? "1" : "0").Append("\r\n");
        System.IO.File.WriteAllText(path, sb.ToString(), Encoding.UTF8);
        return path;
    }

    static string Csv(string value) => "\"" + (value ?? "").Replace("\"", "\"\"") + "\"";
    static string Safe(string value) => string.Concat(value.Select(c => Path.GetInvalidFileNameChars().Contains(c) ? '_' : c));

    static List<string> ParseCsv(string line)
    {
        var r = new List<string>();
        var sb = new StringBuilder();
        bool q = false;
        for (int i = 0; i < line.Length; i++)
        {
            char c = line[i];
            if (c == '"')
            {
                if (q && i + 1 < line.Length && line[i + 1] == '"') { sb.Append('"'); i++; }
                else q = !q;
            }
            else if (c == ',' && !q) { r.Add(sb.ToString()); sb.Clear(); }
            else sb.Append(c);
        }
        r.Add(sb.ToString());
        return r;
    }
}
