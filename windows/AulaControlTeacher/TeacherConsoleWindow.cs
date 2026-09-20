using System.Collections.ObjectModel;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace AulaControlTeacher;

public sealed class TeacherConsoleWindow : Window
{
    static readonly SolidColorBrush Navy = Brush("#202020");
    static readonly SolidColorBrush Navy2 = Brush("#2B2B2B");
    static readonly SolidColorBrush Blue = Brush("#0067C0");
    static readonly SolidColorBrush Teal = Brush("#0078D4");
    static readonly SolidColorBrush Amber = Brush("#FFB900");
    static readonly SolidColorBrush Red = Brush("#D13438");
    static readonly SolidColorBrush Ink = Brush("#1F1F1F");
    static readonly SolidColorBrush Muted = Brush("#605E5C");
    static readonly SolidColorBrush Surface = Brushes.White;
    static readonly SolidColorBrush CanvasBrush = Brush("#F3F3F3");
    static readonly SolidColorBrush Line = Brush("#D1D1D1");
    static readonly SolidColorBrush SoftBlue = Brush("#EEF3FF");
    static readonly SolidColorBrush SoftTeal = Brush("#EAF8F5");
    static readonly SolidColorBrush SoftAmber = Brush("#FFF6E7");

    readonly ObservableCollection<Device> devices = new();
    readonly HashSet<string> selected = new();
    readonly WrapPanel wall = new() { Margin = new Thickness(4) };
    readonly Dictionary<string, Image> wallImages = new();
    readonly Dictionary<string, TextBlock> wallStatus = new();
    readonly Dictionary<string, CheckBox> wallChecks = new();
    readonly TextBlock footer = new() { Foreground = Muted, FontSize = 12 };
    readonly TextBlock statOnline = StatValue();
    readonly TextBlock statLive = StatValue();
    readonly TextBlock statStudents = StatValue();
    readonly TextBlock statTeachers = StatValue();
    readonly TextBlock statAlerts = StatValue();
    readonly TextBox search = new() { MinWidth = 230, FontSize = 14, Padding = new Thickness(12, 9, 12, 9), BorderThickness = new Thickness(1), BorderBrush = Line, Background = Brushes.White };
    readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(1300) };
    readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(2.5) };

    Settings settings;
    Discovery? discovery;
    Commands? commands;
    Registry? registry;
    bool wallBusy;
    string wallSignature = "";
    string filter = "all";

    public TeacherConsoleWindow()
    {
        Title = "Aula Móvil · Consola Docente";
        Width = 1500;
        Height = 900;
        MinWidth = 1120;
        MinHeight = 720;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        Background = CanvasBrush;
        FontFamily = new FontFamily("Segoe UI");

        settings = Settings.Load();
        Content = BuildUi();
        search.TextChanged += (_, _) => { wallSignature = ""; RebuildWall(); };
        timer.Tick += async (_, _) => { RefreshStats(); await RefreshWall(); };
        Loaded += (_, _) => Configure();
        Closed += (_, _) => { discovery?.Dispose(); timer.Stop(); http.Dispose(); };
    }

    static SolidColorBrush Brush(string hex) => new((Color)ColorConverter.ConvertFromString(hex));
    static TextBlock StatValue() => new() { FontSize = 28, FontWeight = FontWeights.Bold, Foreground = Ink, Text = "0" };

    Border Card(UIElement child, double pad = 16, double radius = 18) => new()
    {
        Background = Surface,
        BorderBrush = Line,
        BorderThickness = new Thickness(1),
        CornerRadius = new CornerRadius(Math.Min(radius, 10)),
        Padding = new Thickness(pad),
        Child = child
    };

    TextBlock Txt(string text, double size = 14, Brush? color = null, FontWeight? weight = null) => new()
    {
        Text = text,
        FontSize = size,
        Foreground = color ?? Ink,
        FontWeight = weight ?? FontWeights.Normal,
        TextWrapping = TextWrapping.Wrap
    };

    Button ActionButton(string text, RoutedEventHandler onClick, SolidColorBrush? fill = null, bool compact = false)
    {
        var b = new Button
        {
            Content = text,
            Padding = compact ? new Thickness(10, 7, 10, 7) : new Thickness(14, 10, 14, 10),
            Margin = new Thickness(4, 0, 4, 0),
            Background = fill ?? Brushes.White,
            Foreground = fill == null ? Ink : Brushes.White,
            BorderBrush = fill == null ? Line : fill,
            BorderThickness = new Thickness(1),
            FontWeight = FontWeights.SemiBold,
            Cursor = System.Windows.Input.Cursors.Hand,
            MinHeight = compact ? 34 : 40
        };
        b.Click += onClick;
        return b;
    }

    UIElement BuildUi()
    {
        var root = new Grid();
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(86) });
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(96) });
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(70) });
        root.RowDefinitions.Add(new RowDefinition());
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(40) });

        var header = BuildHeader();
        Grid.SetRow(header, 0);
        root.Children.Add(header);

        var stats = BuildStats();
        Grid.SetRow(stats, 1);
        root.Children.Add(stats);

        var toolbar = BuildToolbar();
        Grid.SetRow(toolbar, 2);
        root.Children.Add(toolbar);

        var workspace = BuildWorkspace();
        Grid.SetRow(workspace, 3);
        root.Children.Add(workspace);

        var foot = new Border { Background = Brush("#EAF0F7"), Padding = new Thickness(18, 10, 18, 10), Child = footer };
        Grid.SetRow(foot, 4);
        root.Children.Add(foot);

        return root;
    }

    UIElement BuildHeader()
    {
        var bar = new Border { Background = Navy, Padding = new Thickness(22, 14, 22, 14) };
        var g = new Grid();
        g.ColumnDefinitions.Add(new ColumnDefinition());
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

        var left = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        var mark = new Border { Width = 52, Height = 52, CornerRadius = new CornerRadius(15), Background = Blue, Child = new TextBlock { Text = "AM", Foreground = Brushes.White, FontWeight = FontWeights.Bold, FontSize = 19, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center } };
        left.Children.Add(mark);
        var names = new StackPanel { Margin = new Thickness(14, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center };
        names.Children.Add(Txt("Aula Móvil", 26, Brushes.White, FontWeights.Bold));
        names.Children.Add(Txt("Consola docente · supervisión y gestión de aula", 13, Brush("#CFD9E6")));
        left.Children.Add(names);
        g.Children.Add(left);

        var right = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        right.Children.Add(new Border { Background = Navy2, CornerRadius = new CornerRadius(12), Padding = new Thickness(11, 7, 11, 7), Margin = new Thickness(0, 0, 8, 0), Child = Txt("7.0 · Aula administrada", 12, Brushes.White, FontWeights.SemiBold) });
        right.Children.Add(ActionButton("Configuración", (_, _) => ConfigDialog(), Amber, true));
        Grid.SetColumn(right, 1);
        g.Children.Add(right);
        bar.Child = g;
        return bar;
    }

    UIElement BuildStats()
    {
        var grid = new Grid { Margin = new Thickness(18, 8, 18, 5) };
        for (int i = 0; i < 5; i++) grid.ColumnDefinitions.Add(new ColumnDefinition());
        AddStat(grid, 0, "EN LÍNEA", "tablets visibles", statOnline, Blue, SoftBlue);
        AddStat(grid, 1, "EN VIVO", "pantallas activas", statLive, Teal, SoftTeal);
        AddStat(grid, 2, "ESTUDIANTES", "sesiones activas", statStudents, Blue, SoftBlue);
        AddStat(grid, 3, "PROFESORES", "sesiones activas", statTeachers, Navy2, Brush("#EDF2F7"));
        AddStat(grid, 4, "ATENCIÓN", "requieren revisión", statAlerts, Amber, SoftAmber);
        return grid;
    }

    void AddStat(Grid grid, int col, string title, string caption, TextBlock value, SolidColorBrush accent, SolidColorBrush soft)
    {
        var stack = new StackPanel();
        stack.Children.Add(Txt(title, 11, accent, FontWeights.Bold));
        stack.Children.Add(value);
        stack.Children.Add(Txt(caption, 11, Muted));
        var card = Card(stack, 13, 15);
        card.Margin = new Thickness(4, 0, 4, 0);
        card.Background = soft;
        Grid.SetColumn(card, col);
        grid.Children.Add(card);
    }

    UIElement BuildToolbar()
    {
        var g = new Grid { Margin = new Thickness(18, 6, 18, 8) };
        g.ColumnDefinitions.Add(new ColumnDefinition());
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

        var actions = new WrapPanel { VerticalAlignment = VerticalAlignment.Center };
        actions.Children.Add(ActionButton("Seleccionar visibles", (_, _) => SelectVisible(), null, true));
        actions.Children.Add(ActionButton("Mensaje", async (_, _) => await PromptSend("MESSAGE", "Mensaje al curso"), Blue, true));
        actions.Children.Add(ActionButton("Abrir enlace", async (_, _) => await PromptSend("OPEN_URL", "Abrir enlace"), null, true));
        actions.Children.Add(ActionButton("Modo atención", async (_, _) => await PromptSend("ATTENTION_ON", "Modo atención"), Navy2, true));
        actions.Children.Add(ActionButton("Liberar atención", async (_, _) => await Send("ATTENTION_OFF"), Teal, true));
        actions.Children.Add(ActionButton("Cerrar sesión", async (_, _) => await Send("FORCE_LOGOUT"), Red, true));
        actions.Children.Add(ActionButton("Mosaico", (_, _) => OpenMosaic(), null, true));
        g.Children.Add(actions);

        var filters = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        search.ToolTip = "Buscar por dispositivo, usuario o curso";
        search.Text = "";
        filters.Children.Add(search);
        filters.Children.Add(ActionButton("Todos", (_, _) => SetFilter("all"), null, true));
        filters.Children.Add(ActionButton("Alumnos", (_, _) => SetFilter("student"), null, true));
        filters.Children.Add(ActionButton("Profesores", (_, _) => SetFilter("teacher"), null, true));
        filters.Children.Add(ActionButton("Alertas", (_, _) => SetFilter("alerts"), null, true));
        Grid.SetColumn(filters, 1);
        g.Children.Add(filters);
        return g;
    }

    UIElement BuildWorkspace()
    {
        var area = new Grid { Margin = new Thickness(18, 0, 18, 10) };
        area.ColumnDefinitions.Add(new ColumnDefinition());
        area.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(280) });

        var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled, Content = wall };
        var wallCard = Card(scroll, 8, 18);
        area.Children.Add(wallCard);

        var panel = new StackPanel();
        panel.Children.Add(Txt("Control rápido", 18, Ink, FontWeights.Bold));
        var help = Txt("Selecciona una o más tablets desde las tarjetas para aplicar acciones al grupo.", 12, Muted);
        help.Margin = new Thickness(0, 5, 0, 12);
        panel.Children.Add(help);
        panel.Children.Add(Section("SELECCIÓN", "Las acciones se ejecutan sólo en las tablets marcadas."));
        panel.Children.Add(Section("PRIVACIDAD", "Supervisión en tiempo real, no vigilancia histórica. No se almacenan contraseñas ni grabaciones por defecto."));
        panel.Children.Add(Section("ESTADO", "Una sesión sin pantalla activa aparece como alerta para el docente."));
        panel.Children.Add(ActionButton("Comprobar conexión", async (_, _) => await Send("PING"), null));
        panel.Children.Add(ActionButton("Deseleccionar todo", (_, _) => ClearSelection(), null));
        var side = Card(panel, 18, 18);
        side.Margin = new Thickness(10, 0, 0, 0);
        Grid.SetColumn(side, 1);
        area.Children.Add(side);
        return area;
    }

    Border Section(string title, string body)
    {
        var s = new StackPanel();
        s.Children.Add(Txt(title, 11, Navy2, FontWeights.Bold));
        var b = Txt(body, 12, Muted);
        b.Margin = new Thickness(0, 4, 0, 0);
        s.Children.Add(b);
        var card = Card(s, 12, 12);
        card.Background = Brush("#F8FAFC");
        card.Margin = new Thickness(0, 0, 0, 9);
        return card;
    }

    void Configure()
    {
        if (string.IsNullOrWhiteSpace(settings.Key))
        {
            var k = Ask("Configuración inicial", "Ingresa la clave técnica del establecimiento. Debe coincidir con la configurada en las tablets.", true);
            if (string.IsNullOrWhiteSpace(k) || k.Length < 10)
            {
                footer.Text = "Configura una clave técnica para comenzar.";
                return;
            }
            settings.Key = k;
            settings.Save();
        }
        StartNetwork();
    }

    void StartNetwork()
    {
        discovery?.Dispose();
        devices.Clear();
        selected.Clear();
        wall.Children.Clear();
        wallImages.Clear();
        wallStatus.Clear();
        wallChecks.Clear();
        wallSignature = "";
        registry = new Registry(settings.Key);
        foreach (var d in registry.Load()) devices.Add(d);
        commands = new Commands(settings.Key);
        discovery = new Discovery(settings.Key);
        discovery.Seen += Incoming;
        try
        {
            discovery.Start();
            footer.Text = "● Consola activa · esperando tablets en la red institucional";
        }
        catch (Exception e)
        {
            footer.Text = "No se pudo iniciar la escucha de red: " + e.Message;
        }
        timer.Start();
        RefreshStats();
        RebuildWall();
        _ = RefreshWall();
    }

    void Incoming(Device incoming) => Dispatcher.Invoke(() =>
    {
        var d = devices.FirstOrDefault(x => x.Id == incoming.Id);
        if (d == null)
        {
            devices.Add(incoming);
            d = incoming;
        }
        else
        {
            d.DeviceName = incoming.DeviceName;
            d.User = incoming.User;
            d.Course = incoming.Course;
            d.Role = incoming.Role;
            d.Model = incoming.Model;
            d.Android = incoming.Android;
            d.Ip = incoming.Ip;
            d.CommandPort = incoming.CommandPort;
            d.ScreenPort = incoming.ScreenPort;
            d.Battery = incoming.Battery;
            d.Screen = incoming.Screen;
            d.Managed = incoming.Managed;
            d.FrameAgeMs = incoming.FrameAgeMs;
            d.Seen = DateTime.Now;
            d.Identity();
        }
        registry?.Save(devices);
        RefreshStats();
        _ = RefreshWall();
    });

    void RefreshStats()
    {
        foreach (var d in devices) d.Computed();
        var online = devices.Where(x => x.IsOnline).ToList();
        statOnline.Text = online.Count.ToString();
        statLive.Text = online.Count(x => !string.IsNullOrWhiteSpace(x.User) && x.Screen && x.FrameAgeMs >= 0 && x.FrameAgeMs < 7000).ToString();
        statStudents.Text = online.Count(x => x.Role == "student").ToString();
        statTeachers.Text = online.Count(x => x.Role == "teacher").ToString();
        statAlerts.Text = online.Count(x => !string.IsNullOrWhiteSpace(x.User) && (!x.Screen || x.FrameAgeMs < 0 || x.FrameAgeMs >= 7000)).ToString();
    }

    IEnumerable<Device> VisibleDevices()
    {
        var q = devices.AsEnumerable();
        if (filter == "student") q = q.Where(x => x.Role == "student");
        else if (filter == "teacher") q = q.Where(x => x.Role == "teacher");
        else if (filter == "alerts") q = q.Where(x => x.IsOnline && !string.IsNullOrWhiteSpace(x.User) && (!x.Screen || x.FrameAgeMs < 0 || x.FrameAgeMs >= 7000));

        var term = search.Text.Trim();
        if (!string.IsNullOrWhiteSpace(term))
            q = q.Where(x => (x.Name + " " + x.Person + " " + x.CourseText).Contains(term, StringComparison.OrdinalIgnoreCase));

        return q.OrderByDescending(x => x.IsOnline).ThenBy(x => x.Name);
    }

    string Signature() => filter + "|" + search.Text.Trim() + "|" + string.Join("|", devices.OrderBy(x => x.Id).Select(x => $"{x.Id}:{x.User}:{x.Role}:{x.Course}:{x.IsOnline}:{x.Screen}:{x.Managed}:{x.FrameAgeMs / 3000}"));

    void RebuildWall()
    {
        wall.Children.Clear();
        wallImages.Clear();
        wallStatus.Clear();
        wallChecks.Clear();

        var list = VisibleDevices().ToList();
        foreach (var d in list)
        {
            var outer = new Grid();
            outer.RowDefinitions.Add(new RowDefinition { Height = new GridLength(46) });
            outer.RowDefinitions.Add(new RowDefinition { Height = new GridLength(150) });
            outer.RowDefinitions.Add(new RowDefinition { Height = new GridLength(72) });

            var top = new Grid { Margin = new Thickness(12, 7, 12, 5) };
            top.ColumnDefinitions.Add(new ColumnDefinition());
            top.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            var name = Txt(d.Name, 14, Ink, FontWeights.Bold);
            name.VerticalAlignment = VerticalAlignment.Center;
            top.Children.Add(name);
            var check = new CheckBox { IsChecked = selected.Contains(d.Id), VerticalAlignment = VerticalAlignment.Center, ToolTip = "Seleccionar tablet" };
            check.Checked += (_, _) => selected.Add(d.Id);
            check.Unchecked += (_, _) => selected.Remove(d.Id);
            Grid.SetColumn(check, 1);
            top.Children.Add(check);
            wallChecks[d.Id] = check;
            outer.Children.Add(top);

            var preview = new Border { Background = Brush("#0E1726"), CornerRadius = new CornerRadius(10), Margin = new Thickness(8, 0, 8, 0) };
            var image = new Image { Stretch = Stretch.Uniform };
            preview.Child = image;
            Grid.SetRow(preview, 1);
            outer.Children.Add(preview);
            wallImages[d.Id] = image;

            var bottom = new Grid { Margin = new Thickness(12, 8, 12, 8) };
            bottom.RowDefinitions.Add(new RowDefinition());
            bottom.RowDefinitions.Add(new RowDefinition());
            var person = Txt(d.Person, 13, d.IsOnline ? Ink : Muted, FontWeights.SemiBold);
            bottom.Children.Add(person);
            var status = Txt(WallText(d), 11, StatusColor(d));
            Grid.SetRow(status, 1);
            bottom.Children.Add(status);
            wallStatus[d.Id] = status;
            Grid.SetRow(bottom, 2);
            outer.Children.Add(bottom);

            var card = Card(outer, 0, 16);
            card.Width = 300;
            card.Height = 280;
            card.Margin = new Thickness(6);
            card.Cursor = System.Windows.Input.Cursors.Hand;
            card.MouseLeftButtonDown += (_, e) =>
            {
                if (e.OriginalSource is CheckBox) return;
                if (d.IsOnline && d.Screen && !string.IsNullOrWhiteSpace(d.User)) new ViewerWindow(d, settings.Key) { Owner = this }.Show();
                else check.IsChecked = !(check.IsChecked ?? false);
            };
            wall.Children.Add(card);
        }

        if (list.Count == 0)
        {
            var empty = new StackPanel { Margin = new Thickness(28) };
            empty.Children.Add(Txt("Sin tablets para mostrar", 22, Ink, FontWeights.Bold));
            empty.Children.Add(Txt("Verifica que las tablets estén en la misma red, con la misma clave técnica y con Aula Móvil activo.", 13, Muted));
            wall.Children.Add(empty);
        }
    }

    SolidColorBrush StatusColor(Device d)
    {
        if (!d.IsOnline) return Muted;
        if (string.IsNullOrWhiteSpace(d.User)) return Blue;
        if (d.Screen && d.FrameAgeMs >= 0 && d.FrameAgeMs < 7000) return Teal;
        return Amber;
    }

    string WallText(Device d)
    {
        if (!d.IsOnline) return "○ Sin conexión";
        if (string.IsNullOrWhiteSpace(d.User)) return "○ Disponible · sin sesión";
        if (d.Screen && d.FrameAgeMs >= 0 && d.FrameAgeMs < 7000)
            return $"● En vivo · {d.RoleText}{(string.IsNullOrWhiteSpace(d.Course) ? "" : " · " + d.Course)}";
        return "● Sesión activa · esperando supervisión";
    }

    async Task RefreshWall()
    {
        if (wallBusy) return;
        wallBusy = true;
        try
        {
            var sig = Signature();
            if (sig != wallSignature)
            {
                wallSignature = sig;
                RebuildWall();
            }
            await Task.WhenAll(devices.Where(d => d.IsOnline).Select(FetchFrame));
        }
        finally { wallBusy = false; }
    }

    async Task FetchFrame(Device d)
    {
        if (!wallImages.TryGetValue(d.Id, out var image) || !wallStatus.TryGetValue(d.Id, out var status)) return;
        if (!d.IsOnline) { image.Source = null; status.Text = "○ Sin conexión"; status.Foreground = Muted; return; }
        if (string.IsNullOrWhiteSpace(d.User)) { image.Source = null; status.Text = "○ Disponible · sin sesión"; status.Foreground = Blue; return; }
        if (!d.Screen) { image.Source = null; status.Text = "● Sesión activa · sin pantalla"; status.Foreground = Amber; return; }

        try
        {
            long ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            using var req = new HttpRequestMessage(HttpMethod.Get, $"http://{d.Ip}:{d.ScreenPort}/screen.jpg");
            req.Headers.Add("X-Timestamp", ts.ToString());
            req.Headers.Add("X-Signature", AcCrypto.Hmac(settings.Key, $"SCREEN\n{ts}"));
            using var response = await http.SendAsync(req);
            if (!response.IsSuccessStatusCode) { status.Text = "● Esperando cuadro…"; status.Foreground = Amber; return; }
            var plain = AcCrypto.Open(settings.Key, await response.Content.ReadAsByteArrayAsync());
            using var ms = new MemoryStream(plain);
            var bi = new BitmapImage();
            bi.BeginInit();
            bi.CacheOption = BitmapCacheOption.OnLoad;
            bi.DecodePixelWidth = 285;
            bi.StreamSource = ms;
            bi.EndInit();
            bi.Freeze();
            image.Source = bi;
            status.Text = $"● En vivo · {d.RoleText}{(string.IsNullOrWhiteSpace(d.Course) ? "" : " · " + d.Course)}";
            status.Foreground = Teal;
        }
        catch
        {
            status.Text = "● Reconectando…";
            status.Foreground = Amber;
        }
    }

    List<Device> SelectedDevices() => devices.Where(x => selected.Contains(x.Id)).ToList();

    async Task Send(string action, string value = "")
    {
        var list = SelectedDevices();
        if (list.Count == 0)
        {
            MessageBox.Show("Selecciona al menos una tablet.", "Aula Móvil", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        if (commands == null) return;
        var results = await Task.WhenAll(list.Where(x => x.IsOnline).Select(async d => (d, result: await commands.Send(d, action, value))));
        var failed = results.Where(x => !x.result.Item1).ToList();
        footer.Text = failed.Count == 0 ? $"Acción enviada a {results.Length} tablet(s)." : $"{failed.Count} tablet(s) no respondieron.";
    }

    async Task PromptSend(string action, string title)
    {
        string help = action == "MESSAGE" ? "Escribe el mensaje que verán los usuarios seleccionados." : action == "OPEN_URL" ? "Escribe una URL completa, por ejemplo https://..." : "Escribe el mensaje que ocupará la pantalla durante el modo atención.";
        var value = Ask(title, help, false);
        if (!string.IsNullOrWhiteSpace(value)) await Send(action, value);
    }

    void OpenMosaic()
    {
        var list = SelectedDevices().Where(x => x.IsOnline && x.Screen && !string.IsNullOrWhiteSpace(x.User)).ToList();
        if (list.Count == 0)
        {
            MessageBox.Show("Selecciona tablets que tengan una sesión supervisada activa.", "Aula Móvil", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        new MosaicWindow(list, settings.Key) { Owner = this }.Show();
    }

    void SelectVisible()
    {
        foreach (var d in VisibleDevices().Where(x => x.IsOnline)) selected.Add(d.Id);
        foreach (var kv in wallChecks) kv.Value.IsChecked = selected.Contains(kv.Key);
    }

    void ClearSelection()
    {
        selected.Clear();
        foreach (var c in wallChecks.Values) c.IsChecked = false;
    }

    void SetFilter(string value)
    {
        filter = value;
        wallSignature = "";
        RebuildWall();
    }

    void ConfigDialog()
    {
        var k = Ask("Configuración", "Clave técnica del establecimiento", true, settings.Key);
        if (!string.IsNullOrWhiteSpace(k) && k.Length >= 10)
        {
            settings.Key = k;
            settings.Save();
            StartNetwork();
        }
    }

    string? Ask(string title, string help, bool password, string initial = "")
    {
        var w = new Window { Title = title, Width = 520, Height = 275, ResizeMode = ResizeMode.NoResize, WindowStartupLocation = WindowStartupLocation.CenterOwner, Owner = this, Background = CanvasBrush };
        var p = new StackPanel { Margin = new Thickness(26) };
        p.Children.Add(Txt(title, 23, Navy, FontWeights.Bold));
        var h = Txt(help, 13, Muted); h.Margin = new Thickness(0, 7, 0, 14); p.Children.Add(h);
        Control input;
        if (password)
        {
            var q = new PasswordBox { FontSize = 16, Padding = new Thickness(12), Password = initial, BorderBrush = Line, BorderThickness = new Thickness(1) };
            input = q;
        }
        else
        {
            var q = new TextBox { FontSize = 16, Padding = new Thickness(12), Text = initial, TextWrapping = TextWrapping.Wrap, AcceptsReturn = true, Height = 70, BorderBrush = Line, BorderThickness = new Thickness(1) };
            input = q;
        }
        p.Children.Add(input);
        var row = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(0, 16, 0, 0) };
        var cancel = ActionButton("Cancelar", (_, _) => w.DialogResult = false, null, true);
        var ok = ActionButton("Aceptar", (_, _) => w.DialogResult = true, Blue, true);
        row.Children.Add(cancel); row.Children.Add(ok); p.Children.Add(row); w.Content = p;
        if (w.ShowDialog() != true) return null;
        return input is PasswordBox pb ? pb.Password : ((TextBox)input).Text;
    }
}
