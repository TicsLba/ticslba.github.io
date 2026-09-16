using System.Text.Json;

namespace AulaControlTeacher;

public sealed class RelaySettings
{
    public string Url { get; set; } = "";
    static string Dir => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"TabletEscolar");
    static string File => Path.Combine(Dir,"relay.json");

    public static RelaySettings Load(){try{return System.IO.File.Exists(File)?JsonSerializer.Deserialize<RelaySettings>(System.IO.File.ReadAllText(File))??new():new();}catch{return new();}}
    public void Save(){try{Directory.CreateDirectory(Dir);System.IO.File.WriteAllText(File,JsonSerializer.Serialize(this));}catch{}}
    public bool Enabled => Uri.TryCreate(Url,UriKind.Absolute,out var u) && u.Scheme.Equals("https",StringComparison.OrdinalIgnoreCase);
}
