using System.Collections.ObjectModel;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace AulaControlTeacher;

public class MainWindow : Window
{
    const string LOGO = "https://ia803101.us.archive.org/6/items/logos_20211117/color%20transparente.png";
    static readonly SolidColorBrush Green = new(Color.FromRgb(15, 90, 60));
    static readonly SolidColorBrush Dark = new(Color.FromRgb(8, 56, 38));
    static readonly SolidColorBrush Orange = new(Color.FromRgb(243, 107, 33));
    static readonly SolidColorBrush Ink = new(Color.FromRgb(28, 43, 36));
    static readonly SolidColorBrush Muted = new(Color.FromRgb(94, 111, 103));
    static readonly SolidColorBrush Canvas = new(Color.FromRgb(244, 248, 246));

    readonly ObservableCollection<Device> devices = new();
    readonly DataGrid grid = new() { SelectionMode = DataGridSelectionMode.Extended, AutoGenerateColumns = false, IsReadOnly = true, CanUserAddRows = false, RowHeight = 50, HeadersVisibility = DataGridHeadersVisibility.Column, GridLinesVisibility = DataGridGridLinesVisibility.Horizontal, Background = Brushes.White, BorderThickness = new Thickness(0) };
    readonly WrapPanel wall = new() { Margin = new Thickness(10) };
    readonly Dictionary<string, Image> wallImages = new();
    readonly Dictionary<string, TextBlock> wallStatus = new();
    readonly TextBlock footer = new() { Foreground = Muted };
    readonly TextBlock statOnline = new(), statSupervised = new(), statAvailable = new(), statAlerts = new();
    readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(1400) };
    readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(2.5) };

    Settings settings;
    Discovery? discovery;
    Commands? commands;
    Registry? registry;
    string wallSignature = "";
    bool wallBusy;

    public MainWindow()
    {
        Title = "AulaControl · Consola docente";
        Width = 1480; Height = 870; MinWidth = 1100; MinHeight = 680;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        Background = Canvas;
        FontFamily = new FontFamily("Segoe UI");
        settings = Settings.Load();
        BuildUi();
        timer.Tick += async (_, _) => { RefreshStats(); await RefreshWall(); };
        timer.Start();
        Loaded += (_, _) => Configure();
        Closed += (_, _) => { discovery?.Dispose(); timer.Stop(); http.Dispose(); };
    }

    Button B(string s, RoutedEventHandler h, SolidColorBrush? bg = null)
    {
        var b = new Button { Content = s, Padding = new Thickness(14, 9, 14, 9), Margin = new Thickness(4), Background = bg ?? Brushes.White, Foreground = bg == null ? Ink : Brushes.White, BorderThickness = new Thickness(bg == null ? 1 : 0), BorderBrush = new SolidColorBrush(Color.FromRgb(216, 225, 220)), FontWeight = FontWeights.SemiBold, Cursor = System.Windows.Input.Cursors.Hand };
        b.Click += h; return b;
    }

    Border Card(UIElement child, double pad = 16) => new() { Background = Brushes.White, CornerRadius = new CornerRadius(16), Padding = new Thickness(pad), BorderBrush = new SolidColorBrush(Color.FromRgb(226, 233, 229)), BorderThickness = new Thickness(1), Child = child };

    Border Stat(string title, string caption, TextBlock value, SolidColorBrush accent)
    {
        value.FontSize = 30; value.FontWeight = FontWeights.Bold; value.Foreground = Ink; value.Text = "0";
        var s = new StackPanel();
        s.Children.Add(new TextBlock { Text = title, Foreground = accent, FontWeight = FontWeights.Bold, FontSize = 12 });
        s.Children.Add(value);
        s.Children.Add(new TextBlock { Text = caption, Foreground = Muted, FontSize = 12 });
        var c = Card(s, 14); c.Margin = new Thickness(5); return c;
    }

    Image Logo()
    {
        var i = new Image { Width = 72, Height = 72, Stretch = Stretch.Uniform, Margin = new Thickness(0, 0, 15, 0) };
        try { i.Source = new BitmapImage(new Uri(LOGO, UriKind.Absolute)); } catch { }
        return i;
    }

    void BuildUi()
    {
        var root = new Grid();
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(112) });
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(104) });
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(64) });
        root.RowDefinitions.Add(new RowDefinition());
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(38) });

        var head = new Border { Background = Dark, Padding = new Thickness(26, 16, 26, 16) };
        var hg = new Grid(); hg.ColumnDefinitions.Add(new ColumnDefinition()); hg.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var left = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        left.Children.Add(new Border { Background = Brushes.White, CornerRadius = new CornerRadius(16), Padding = new Thickness(8), Child = Logo() });
        var titles = new StackPanel { VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(16, 0, 0, 0) };
        titles.Children.Add(new TextBlock { Text = "AulaControl", Foreground = Brushes.White, FontSize = 31, FontWeight = FontWeights.Bold });
        titles.Children.Add(new TextBlock { Text = "Consola docente · Supervisión continua", Foreground = new SolidColorBrush(Color.FromRgb(204, 231, 218)), FontSize = 14 });
        left.Children.Add(titles); hg.Children.Add(left);
        var right = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        right.Children.Add(new Border { Background = new SolidColorBrush(Color.FromRgb(19, 108, 73)), CornerRadius = new CornerRadius(12), Padding = new Thickness(12, 7, 12, 7), Margin = new Thickness(0, 0, 10, 0), Child = new TextBlock { Text = "AulaControl 1.1.1 Strict", Foreground = Brushes.White, FontWeight = FontWeights.SemiBold } });
        right.Children.Add(B("⚙  Configuración", (_, _) => ConfigDialog(), Orange)); Grid.SetColumn(right, 1); hg.Children.Add(right); head.Child = hg; root.Children.Add(head);
        root.Children.Add(new Border { Background = Orange, Height = 4, VerticalAlignment = VerticalAlignment.Bottom });

        var stats = new Grid { Margin = new Thickness(20, 8, 20, 4) }; for (int i = 0; i < 4; i++) stats.ColumnDefinitions.Add(new ColumnDefinition());
        var s1 = Stat("EN LÍNEA", "dispositivos visibles", statOnline, Green); stats.Children.Add(s1);
        var s2 = Stat("EN VIVO", "sesiones transmitiendo", statSupervised, Green); Grid.SetColumn(s2, 1); stats.Children.Add(s2);
        var s3 = Stat("DISPONIBLES", "sin estudiante", statAvailable, new SolidColorBrush(Color.FromRgb(52, 99, 168))); Grid.SetColumn(s3, 2); stats.Children.Add(s3);
        var s4 = Stat("BLOQUEADAS", "sin transmisión válida", statAlerts, Orange); Grid.SetColumn(s4, 3); stats.Children.Add(s4);
        Grid.SetRow(stats, 1); root.Children.Add(stats);

        var barPanel = new WrapPanel { Margin = new Thickness(20, 6, 20, 6) };
        barPanel.Children.Add(B("✓  Seleccionar en línea", (_, _) => SelectOnline()));
        barPanel.Children.Add(B("✉  Mensaje", async (_, _) => await PromptSend("MESSAGE", "Mensaje al estudiante")));
        barPanel.Children.Add(B("↗  Abrir enlace", async (_, _) => await PromptSend("OPEN_URL", "URL completa (https://…)")));
        barPanel.Children.Add(B("⏏  Cerrar sesión", async (_, _) => await Send("FORCE_LOGOUT"), Orange));
        barPanel.Children.Add(B("↻  Probar conexión", async (_, _) => await Send("PING")));
        barPanel.Children.Add(B("Limpiar offline", (_, _) => Clean()));
        Grid.SetRow(barPanel, 2); root.Children.Add(barPanel);

        var tabs = new TabControl { Margin = new Thickness(22, 4, 22, 12), Background = Canvas, BorderThickness = new Thickness(0) };
        var liveTab = new TabItem { Header = "  Pantallas en vivo  " };
        liveTab.Content = Card(new ScrollViewer { Content = wall, VerticalScrollBarVisibility = ScrollBarVisibility.Auto }, 4);
        tabs.Items.Add(liveTab);

        grid.ItemsSource = devices;
        AddCol("Estado", "Status", 110); AddCol("Dispositivo", "Name", 150); AddCol("Estudiante / usuario", "Person", 210); AddCol("Curso", "CourseText", 115); AddCol("Supervisión", "Supervision", 135); AddCol("Gestión", "Protection", 135); AddCol("Batería", "Battery", 75, "{0}%"); AddCol("Modelo", "Model", 190); AddCol("Android", "Android", 75); AddCol("IP", "Ip", 120); AddCol("Último aviso", "SeenText", 95);
        var devicesTab = new TabItem { Header = "  Dispositivos  ", Content = Card(grid, 0) }; tabs.Items.Add(devicesTab);
        Grid.SetRow(tabs, 3); root.Children.Add(tabs);

        var foot = new Border { Background = new SolidColorBrush(Color.FromRgb(229, 237, 233)), Padding = new Thickness(18, 9, 18, 9), Child = footer }; Grid.SetRow(foot, 4); root.Children.Add(foot);
        Content = root;
    }

    void AddCol(string h, string b, double w, string? fmt = null)
    {
        var x = new DataGridTextColumn { Header = h, Binding = new Binding(b), Width = w };
        if (fmt != null) ((Binding)x.Binding).StringFormat = fmt; grid.Columns.Add(x);
    }

    void Configure()
    {
        if (string.IsNullOrWhiteSpace(settings.Key))
        {
            var k = Ask("Configuración inicial", "Clave técnica del aula / establecimiento", true);
            if (string.IsNullOrWhiteSpace(k) || k.Length < 10) { footer.Text = "Configura una clave técnica para comenzar."; return; }
            settings.Key = k; settings.Save();
        }
        Start();
    }

    void Start()
    {
        discovery?.Dispose(); devices.Clear(); wall.Children.Clear(); wallImages.Clear(); wallStatus.Clear(); wallSignature = "";
        registry = new Registry(settings.Key); foreach (var x in registry.Load()) devices.Add(x);
        commands = new Commands(settings.Key); discovery = new Discovery(settings.Key); discovery.Seen += Incoming;
        try { discovery.Start(); footer.Text = "● Consola activa · las sesiones válidas aparecerán automáticamente en Pantallas en vivo"; }
        catch (Exception e) { footer.Text = "No se pudo abrir UDP 45888: " + e.Message; }
        RefreshStats(); _ = RefreshWall();
    }

    void Incoming(Device n) => Dispatcher.Invoke(() =>
    {
        var d = devices.FirstOrDefault(x => x.Id == n.Id);
        if (d == null) { devices.Add(n); if (!string.IsNullOrWhiteSpace(n.User)) SessionLog.Change(n.Id, "", n.User, n.Course); }
        else
        {
            string old = d.User; d.DeviceName = n.DeviceName; d.User = n.User; d.Course = n.Course; d.Model = n.Model; d.Android = n.Android; d.Ip = n.Ip; d.CommandPort = n.CommandPort; d.ScreenPort = n.ScreenPort; d.Battery = n.Battery; d.Screen = n.Screen; d.Managed = n.Managed; d.FrameAgeMs = n.FrameAgeMs; d.Seen = DateTime.Now; d.Identity(); if (old != d.User) SessionLog.Change(d.Id, old, d.User, d.Course);
        }
        registry?.Save(devices); RefreshStats(); _ = RefreshWall();
    });

    void RefreshStats()
    {
        foreach (var d in devices) d.Computed();
        var on = devices.Where(x => x.IsOnline).ToList();
        statOnline.Text = on.Count.ToString();
        statSupervised.Text = on.Count(x => !string.IsNullOrWhiteSpace(x.User) && x.Screen).ToString();
        statAvailable.Text = on.Count(x => string.IsNullOrWhiteSpace(x.User)).ToString();
        statAlerts.Text = on.Count(x => !string.IsNullOrWhiteSpace(x.User) && !x.Screen).ToString();
    }

    string WallSignature()
    {
        return string.Join("|", devices.OrderBy(x => x.Id).Select(x => $"{x.Id}:{x.User}:{x.Course}:{x.IsOnline}:{x.Screen}:{x.Managed}"));
    }

    void RebuildWall()
    {
        wall.Children.Clear(); wallImages.Clear(); wallStatus.Clear();
        foreach (var d in devices.OrderByDescending(x => x.IsOnline).ThenBy(x => x.Name))
        {
            var g = new Grid(); g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(46) }); g.RowDefinitions.Add(new RowDefinition()); g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(48) });
            var head = new Grid { Margin = new Thickness(12, 8, 12, 6) }; head.ColumnDefinitions.Add(new ColumnDefinition()); head.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            head.Children.Add(new TextBlock { Text = d.Name, Foreground = Ink, FontSize = 15, FontWeight = FontWeights.Bold, VerticalAlignment = VerticalAlignment.Center });
            var badge = new TextBlock { Text = d.Managed ? "🔒" : "⚠", Foreground = d.Managed ? Green : Orange, FontSize = 15, VerticalAlignment = VerticalAlignment.Center }; Grid.SetColumn(badge, 1); head.Children.Add(badge); g.Children.Add(head);
            var imageBorder = new Border { Background = new SolidColorBrush(Color.FromRgb(10, 18, 15)), CornerRadius = new CornerRadius(10), Margin = new Thickness(8, 0, 8, 0) };
            var image = new Image { Stretch = Stretch.Uniform }; imageBorder.Child = image; Grid.SetRow(imageBorder, 1); g.Children.Add(imageBorder); wallImages[d.Id] = image;
            var status = new TextBlock { Foreground = d.IsOnline ? (!string.IsNullOrWhiteSpace(d.User) && d.Screen ? Green : Orange) : Muted, FontWeight = FontWeights.SemiBold, Margin = new Thickness(12, 8, 12, 8), TextTrimming = TextTrimming.CharacterEllipsis };
            status.Text = !d.IsOnline ? "○ Sin conexión" : string.IsNullOrWhiteSpace(d.User) ? "○ Disponible · sin sesión" : d.Screen ? $"● {d.Person} · conectando imagen…" : $"● {d.Person} · sesión bloqueada";
            Grid.SetRow(status, 2); g.Children.Add(status); wallStatus[d.Id] = status;
            var card = Card(g, 0); card.Width = 318; card.Height = 250; card.Margin = new Thickness(7); card.Cursor = System.Windows.Input.Cursors.Hand; card.MouseLeftButtonDown += (_, _) => { if (d.IsOnline && d.Screen && !string.IsNullOrWhiteSpace(d.User)) new ViewerWindow(d, settings.Key) { Owner = this }.Show(); };
            wall.Children.Add(card);
        }
        if (devices.Count == 0) wall.Children.Add(new TextBlock { Text = "Esperando tablets AulaControl en la red…", Foreground = Muted, FontSize = 18, Margin = new Thickness(25) });
    }

    async Task RefreshWall()
    {
        if (wallBusy) return; wallBusy = true;
        try
        {
            var sig = WallSignature(); if (sig != wallSignature) { wallSignature = sig; RebuildWall(); }
            await Task.WhenAll(devices.Where(d => d.IsOnline).Select(FetchFrame));
        }
        finally { wallBusy = false; }
    }

    async Task FetchFrame(Device d)
    {
        if (!wallStatus.TryGetValue(d.Id, out var st) || !wallImages.TryGetValue(d.Id, out var im)) return;
        if (!d.IsOnline) { st.Text = "○ Sin conexión"; im.Source = null; return; }
        if (string.IsNullOrWhiteSpace(d.User)) { st.Text = "○ Disponible · sin sesión"; st.Foreground = new SolidColorBrush(Color.FromRgb(52, 99, 168)); im.Source = null; return; }
        if (!d.Screen) { st.Text = $"● {d.Person} · BLOQUEADA: sin supervisión"; st.Foreground = Orange; im.Source = null; return; }
        try
        {
            long ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            using var q = new HttpRequestMessage(HttpMethod.Get, $"http://{d.Ip}:{d.ScreenPort}/screen.jpg");
            q.Headers.Add("X-Timestamp", ts.ToString()); q.Headers.Add("X-Signature", AcCrypto.Hmac(settings.Key, $"SCREEN\n{ts}"));
            var r = await http.SendAsync(q); if (!r.IsSuccessStatusCode) { st.Text = $"● {d.Person} · esperando cuadro…"; st.Foreground = Orange; return; }
            byte[] z = await r.Content.ReadAsByteArrayAsync(); using var ms = new MemoryStream(z); var bi = new BitmapImage(); bi.BeginInit(); bi.CacheOption = BitmapCacheOption.OnLoad; bi.DecodePixelWidth = 300; bi.StreamSource = ms; bi.EndInit(); bi.Freeze(); im.Source = bi;
            st.Text = $"● EN VIVO · {d.Person}{(string.IsNullOrWhiteSpace(d.Course) ? "" : " · " + d.Course)}"; st.Foreground = Green;
        }
        catch { st.Text = $"● {d.Person} · reconectando…"; st.Foreground = Orange; }
    }

    List<Device> Sel() => grid.SelectedItems.Cast<Device>().ToList();
    async Task Send(string a, string v = "")
    {
        var s = Sel(); if (s.Count == 0) { MessageBox.Show("Selecciona al menos una tablet en la pestaña Dispositivos.", "AulaControl"); return; }
        var rs = await Task.WhenAll(s.Where(x => x.IsOnline).Select(async d => (d, await commands!.Send(d, a, v))));
        foreach (var x in rs) if (!x.Item2.Item1) footer.Text = $"{x.d.Name}: {x.Item2.Item2}";
    }
    async Task PromptSend(string a, string title) { var x = Ask(title, a == "MESSAGE" ? "Escribe el mensaje que recibirá el estudiante" : "Escribe la URL completa", false); if (!string.IsNullOrWhiteSpace(x)) await Send(a, x); }
    void SelectOnline() { grid.SelectedItems.Clear(); foreach (var d in devices.Where(x => x.IsOnline)) grid.SelectedItems.Add(d); }
    void Clean() { foreach (var d in devices.Where(x => !x.IsOnline).ToList()) devices.Remove(d); registry?.Save(devices); wallSignature = ""; RefreshStats(); _ = RefreshWall(); }
    void ConfigDialog() { var k = Ask("Configuración", "Clave técnica del aula / establecimiento", true, settings.Key); if (!string.IsNullOrWhiteSpace(k) && k.Length >= 10) { settings.Key = k; settings.Save(); Start(); } }

    string? Ask(string title, string help, bool password, string initial = "")
    {
        var w = new Window { Title = title, Width = 540, Height = 280, ResizeMode = ResizeMode.NoResize, WindowStartupLocation = WindowStartupLocation.CenterOwner, Owner = this, Background = Canvas };
        var p = new StackPanel { Margin = new Thickness(28) }; p.Children.Add(new TextBlock { Text = title, FontSize = 24, FontWeight = FontWeights.Bold, Foreground = Dark }); p.Children.Add(new TextBlock { Text = help, Margin = new Thickness(0, 7, 0, 14), TextWrapping = TextWrapping.Wrap, Foreground = Muted });
        Control input; if (password) { var q = new PasswordBox { FontSize = 17, Padding = new Thickness(12), Password = initial, Background = Brushes.White }; input = q; } else { var q = new TextBox { FontSize = 17, Padding = new Thickness(12), Text = initial, Background = Brushes.White }; input = q; }
        p.Children.Add(input); var ok = B("Aceptar", (_, _) => w.DialogResult = true, Green); ok.Margin = new Thickness(0, 16, 0, 0); p.Children.Add(ok); w.Content = p; if (w.ShowDialog() != true) return null; return password ? ((PasswordBox)input).Password : ((TextBox)input).Text.Trim();
    }
}
