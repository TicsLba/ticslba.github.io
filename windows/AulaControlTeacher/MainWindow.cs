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
    static readonly SolidColorBrush Blue = new(Color.FromRgb(49,89,255));
    static readonly SolidColorBrush Navy = new(Color.FromRgb(23,43,77));
    static readonly SolidColorBrush Teal = new(Color.FromRgb(32,199,164));
    static readonly SolidColorBrush Gold = new(Color.FromRgb(245,180,61));
    static readonly SolidColorBrush Red = new(Color.FromRgb(202,61,71));
    static readonly SolidColorBrush Ink = new(Color.FromRgb(23,32,51));
    static readonly SolidColorBrush Muted = new(Color.FromRgb(102,112,133));
    static readonly SolidColorBrush Canvas = new(Color.FromRgb(244,247,251));
    static readonly SolidColorBrush Line = new(Color.FromRgb(221,228,239));
    static readonly SolidColorBrush SoftBlue = new(Color.FromRgb(232,237,255));
    static readonly SolidColorBrush SoftTeal = new(Color.FromRgb(229,250,245));

    readonly ObservableCollection<Device> devices = new();
    readonly DataGrid grid = new()
    {
        SelectionMode = DataGridSelectionMode.Extended,
        AutoGenerateColumns = false,
        IsReadOnly = true,
        CanUserAddRows = false,
        RowHeight = 46,
        HeadersVisibility = DataGridHeadersVisibility.Column,
        GridLinesVisibility = DataGridGridLinesVisibility.Horizontal,
        HorizontalGridLinesBrush = Line,
        Background = Brushes.White,
        BorderThickness = new Thickness(0)
    };
    readonly WrapPanel wall = new() { Margin = new Thickness(8) };
    readonly Dictionary<string, Image> wallImages = new();
    readonly Dictionary<string, TextBlock> wallStatus = new();
    readonly TextBlock footer = new() { Foreground = Muted, VerticalAlignment = VerticalAlignment.Center };
    readonly TextBlock statOnline = new(), statLive = new(), statStudents = new(), statTeachers = new(), statAlerts = new();
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
        Title = "Tablet Escolar · Consola docente";
        Width = 1500;
        Height = 900;
        MinWidth = 1120;
        MinHeight = 700;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        Background = Canvas;
        FontFamily = new FontFamily("Segoe UI");
        try { Icon = BitmapFrame.Create(new Uri("pack://application:,,,/TabletEscolar.ico")); } catch { }
        settings = Settings.Load();
        BuildUi();
        timer.Tick += async (_, _) => { RefreshStats(); await RefreshWall(); };
        timer.Start();
        Loaded += (_, _) => Configure();
        Closed += (_, _) => { discovery?.Dispose(); timer.Stop(); http.Dispose(); };
    }

    Border Card(UIElement child, double pad = 16) => new()
    {
        Background = Brushes.White,
        CornerRadius = new CornerRadius(18),
        Padding = new Thickness(pad),
        BorderBrush = Line,
        BorderThickness = new Thickness(1),
        Child = child
    };

    Button Action(string text, RoutedEventHandler click, Brush? fill = null, Brush? foreground = null)
    {
        var b = new Button
        {
            Content = text,
            Padding = new Thickness(14,9,14,9),
            Margin = new Thickness(4),
            Background = fill ?? Brushes.White,
            Foreground = foreground ?? (fill == null ? Ink : Brushes.White),
            BorderThickness = new Thickness(fill == null ? 1 : 0),
            BorderBrush = Line,
            FontWeight = FontWeights.SemiBold,
            Cursor = System.Windows.Input.Cursors.Hand
        };
        b.Click += click;
        return b;
    }

    Border Stat(string title, string caption, TextBlock value, Brush accent)
    {
        value.FontSize = 29; value.FontWeight = FontWeights.Bold; value.Foreground = Ink; value.Text = "0";
        var p = new StackPanel();
        p.Children.Add(new TextBlock { Text = title, Foreground = accent, FontWeight = FontWeights.Bold, FontSize = 11 });
        p.Children.Add(value);
        p.Children.Add(new TextBlock { Text = caption, Foreground = Muted, FontSize = 11 });
        var c = Card(p, 13); c.Margin = new Thickness(4); return c;
    }

    UIElement BrandMark()
    {
        var box = new Grid { Width = 58, Height = 58, Background = Blue };
        box.Clip = new RectangleGeometry(new Rect(0,0,58,58), 15, 15);
        var tablet = new Border
        {
            Width = 29, Height = 38, CornerRadius = new CornerRadius(5),
            BorderBrush = Brushes.White, BorderThickness = new Thickness(3),
            HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
        };
        var dot = new EllipseGeometry(new Point(29,45), 2, 2);
        box.Children.Add(tablet);
        return box;
    }

    void BuildUi()
    {
        var root = new Grid();
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(100) });
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(100) });
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(62) });
        root.RowDefinitions.Add(new RowDefinition());
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(38) });

        var head = new Border { Background = Navy, Padding = new Thickness(24,14,24,14) };
        var hg = new Grid(); hg.ColumnDefinitions.Add(new ColumnDefinition()); hg.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var left = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        left.Children.Add(BrandMark());
        var words = new StackPanel { Margin = new Thickness(15,0,0,0), VerticalAlignment = VerticalAlignment.Center };
        words.Children.Add(new TextBlock { Text = "Tablet Escolar", Foreground = Brushes.White, FontSize = 30, FontWeight = FontWeights.Bold });
        words.Children.Add(new TextBlock { Text = "Consola docente · supervisión en vivo · gestión de aula", Foreground = new SolidColorBrush(Color.FromRgb(201,213,238)), FontSize = 13 });
        left.Children.Add(words); hg.Children.Add(left);
        var right = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        right.Children.Add(new Border { Background = Blue, CornerRadius = new CornerRadius(12), Padding = new Thickness(12,7,12,7), Margin = new Thickness(0,0,9,0), Child = new TextBlock { Text = "2.0 · Classroom", Foreground = Brushes.White, FontWeight = FontWeights.SemiBold } });
        right.Children.Add(Action("Configuración", (_, _) => ConfigDialog(), Gold, Navy));
        Grid.SetColumn(right, 1); hg.Children.Add(right); head.Child = hg; root.Children.Add(head);

        var stats = new Grid { Margin = new Thickness(18,7,18,3) };
        for (int i=0;i<5;i++) stats.ColumnDefinitions.Add(new ColumnDefinition());
        var a = Stat("EN LÍNEA", "tablets visibles", statOnline, Blue); stats.Children.Add(a);
        var b = Stat("EN VIVO", "pantallas supervisadas", statLive, Teal); Grid.SetColumn(b,1); stats.Children.Add(b);
        var c = Stat("ESTUDIANTES", "sesiones activas", statStudents, Blue); Grid.SetColumn(c,2); stats.Children.Add(c);
        var d = Stat("PROFESORES", "sesiones activas", statTeachers, Navy); Grid.SetColumn(d,3); stats.Children.Add(d);
        var e = Stat("REVISAR", "sesiones sin pantalla", statAlerts, Gold); Grid.SetColumn(e,4); stats.Children.Add(e);
        Grid.SetRow(stats,1); root.Children.Add(stats);

        var bar = new WrapPanel { Margin = new Thickness(18,3,18,4) };
        bar.Children.Add(Action("Seleccionar en línea", (_,_) => SelectOnline()));
        bar.Children.Add(Action("Mosaico", (_,_) => OpenMosaic(), SoftBlue, Navy));
        bar.Children.Add(Action("Mensaje", async (_,_) => await PromptSend("MESSAGE","Enviar mensaje")));
        bar.Children.Add(Action("Abrir enlace", async (_,_) => await PromptSend("OPEN_URL","Abrir enlace")));
        bar.Children.Add(Action("Abrir app", async (_,_) => await PromptSend("LAUNCH_APP","Abrir aplicación")));
        bar.Children.Add(Action("Atención", async (_,_) => await PromptSend("ATTENTION_ON","Modo atención"), Navy));
        bar.Children.Add(Action("Liberar atención", async (_,_) => await Send("ATTENTION_OFF"), Teal));
        bar.Children.Add(Action("Cerrar sesión", async (_,_) => await Send("FORCE_LOGOUT"), Red));
        Grid.SetRow(bar,2); root.Children.Add(bar);

        var tabs = new TabControl { Margin = new Thickness(20,3,20,10), Background = Canvas, BorderThickness = new Thickness(0) };
        tabs.Items.Add(new TabItem { Header = "  Aula en vivo  ", Content = Card(new ScrollViewer { Content = wall, VerticalScrollBarVisibility = ScrollBarVisibility.Auto }, 4) });
        grid.ItemsSource = devices;
        AddCol("Estado","Status",105); AddCol("Tablet","Name",145); AddCol("Usuario","Person",185); AddCol("Rol","RoleText",100); AddCol("Curso","CourseText",105);
        AddCol("Supervisión","Supervision",130); AddCol("Gestión","Protection",125); AddCol("Batería","Battery",70,"{0}%"); AddCol("Modelo","Model",175); AddCol("Android","Android",75); AddCol("IP","Ip",120);
        tabs.Items.Add(new TabItem { Header = "  Dispositivos  ", Content = Card(grid,0) });
        Grid.SetRow(tabs,3); root.Children.Add(tabs);

        var foot = new Border { Background = new SolidColorBrush(Color.FromRgb(235,240,248)), Padding = new Thickness(18,8,18,8), Child = footer };
        Grid.SetRow(foot,4); root.Children.Add(foot); Content = root;
    }

    void AddCol(string h,string b,double w,string? fmt=null)
    {
        var x = new DataGridTextColumn { Header = h, Binding = new Binding(b), Width = w };
        if(fmt!=null) ((Binding)x.Binding).StringFormat = fmt; grid.Columns.Add(x);
    }

    void Configure()
    {
        if(string.IsNullOrWhiteSpace(settings.Key))
        {
            var k = Ask("Configuración inicial","Ingresa la misma clave técnica configurada en las tablets.",true);
            if(string.IsNullOrWhiteSpace(k) || k.Length < 10) { footer.Text = "Configura una clave técnica para comenzar."; return; }
            settings.Key = k; settings.Save();
        }
        Start();
    }

    void Start()
    {
        discovery?.Dispose(); devices.Clear(); wall.Children.Clear(); wallImages.Clear(); wallStatus.Clear(); wallSignature = "";
        registry = new Registry(settings.Key); foreach(var x in registry.Load()) devices.Add(x);
        commands = new Commands(settings.Key); discovery = new Discovery(settings.Key); discovery.Seen += Incoming;
        try { discovery.Start(); footer.Text = "● Consola activa · tráfico cifrado · supervisión en tiempo real, no vigilancia histórica"; }
        catch(Exception e) { footer.Text = "No se pudo abrir UDP 45888: " + e.Message; }
        RefreshStats(); _ = RefreshWall();
    }

    void Incoming(Device n) => Dispatcher.Invoke(() =>
    {
        var d = devices.FirstOrDefault(x => x.Id == n.Id);
        if(d == null) { devices.Add(n); if(!string.IsNullOrWhiteSpace(n.User)) SessionLog.Change(n.Id,"",n.User,n.Course); }
        else
        {
            string old = d.User;
            d.DeviceName=n.DeviceName; d.User=n.User; d.Course=n.Course; d.Role=n.Role; d.Model=n.Model; d.Android=n.Android; d.Ip=n.Ip;
            d.CommandPort=n.CommandPort; d.ScreenPort=n.ScreenPort; d.Battery=n.Battery; d.Screen=n.Screen; d.Managed=n.Managed; d.FrameAgeMs=n.FrameAgeMs; d.Seen=DateTime.Now; d.Identity();
            if(old != d.User) SessionLog.Change(d.Id,old,d.User,d.Course);
        }
        registry?.Save(devices); RefreshStats(); _ = RefreshWall();
    });

    void RefreshStats()
    {
        foreach(var d in devices) d.Computed();
        var on = devices.Where(x => x.IsOnline).ToList();
        statOnline.Text=on.Count.ToString(); statLive.Text=on.Count(x=>!string.IsNullOrWhiteSpace(x.User)&&x.Screen).ToString();
        statStudents.Text=on.Count(x=>x.Role=="student").ToString(); statTeachers.Text=on.Count(x=>x.Role=="teacher").ToString();
        statAlerts.Text=on.Count(x=>!string.IsNullOrWhiteSpace(x.User)&&!x.Screen).ToString();
    }

    string WallSignature() => string.Join("|",devices.OrderBy(x=>x.Id).Select(x=>$"{x.Id}:{x.User}:{x.Role}:{x.Course}:{x.IsOnline}:{x.Screen}:{x.Managed}"));

    void RebuildWall()
    {
        wall.Children.Clear(); wallImages.Clear(); wallStatus.Clear();
        foreach(var d in devices.OrderByDescending(x=>x.IsOnline).ThenBy(x=>x.Name))
        {
            var g = new Grid(); g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(48)}); g.RowDefinitions.Add(new RowDefinition()); g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(60)});
            var h = new Grid{Margin=new Thickness(12,8,12,6)}; h.ColumnDefinitions.Add(new ColumnDefinition()); h.ColumnDefinitions.Add(new ColumnDefinition{Width=GridLength.Auto});
            h.Children.Add(new TextBlock{Text=d.Name,Foreground=Ink,FontSize=15,FontWeight=FontWeights.Bold,VerticalAlignment=VerticalAlignment.Center});
            var badge = new Border{Background=d.Managed?SoftTeal:new SolidColorBrush(Color.FromRgb(255,246,224)),CornerRadius=new CornerRadius(10),Padding=new Thickness(8,4,8,4),Child=new TextBlock{Text=d.Managed?"Gestionada":"Revisar",Foreground=d.Managed?Teal:Gold,FontSize=11,FontWeight=FontWeights.SemiBold}};
            Grid.SetColumn(badge,1); h.Children.Add(badge); g.Children.Add(h);
            var ib = new Border{Background=new SolidColorBrush(Color.FromRgb(15,23,42)),CornerRadius=new CornerRadius(10),Margin=new Thickness(8,0,8,0)}; var im = new Image{Stretch=Stretch.Uniform}; ib.Child=im; Grid.SetRow(ib,1); g.Children.Add(ib); wallImages[d.Id]=im;
            var st = new TextBlock{Foreground=Muted,FontWeight=FontWeights.SemiBold,Margin=new Thickness(12,7,12,7),TextWrapping=TextWrapping.Wrap}; st.Text=WallText(d); Grid.SetRow(st,2); g.Children.Add(st); wallStatus[d.Id]=st;
            var card = Card(g,0); card.Width=326; card.Height=270; card.Margin=new Thickness(7); card.Cursor=System.Windows.Input.Cursors.Hand;
            card.MouseLeftButtonDown += (_,_) => { if(d.IsOnline&&d.Screen&&!string.IsNullOrWhiteSpace(d.User)) new ViewerWindow(d,settings.Key){Owner=this}.Show(); };
            wall.Children.Add(card);
        }
        if(devices.Count==0) wall.Children.Add(new TextBlock{Text="Esperando tablets en la red institucional…",Foreground=Muted,FontSize=18,Margin=new Thickness(25)});
    }

    string WallText(Device d) => !d.IsOnline ? "○ Sin conexión" : string.IsNullOrWhiteSpace(d.User) ? "○ Disponible · esperando identificación" : d.Screen ? $"● EN VIVO · {d.Person}\n{d.RoleText}{(string.IsNullOrWhiteSpace(d.Course)?"":" · "+d.Course)}" : $"● {d.Person} · sesión sin pantalla";

    async Task RefreshWall()
    {
        if(wallBusy) return; wallBusy=true;
        try { var s=WallSignature(); if(s!=wallSignature){wallSignature=s;RebuildWall();} await Task.WhenAll(devices.Where(d=>d.IsOnline).Select(FetchFrame)); }
        finally { wallBusy=false; }
    }

    async Task FetchFrame(Device d)
    {
        if(!wallStatus.TryGetValue(d.Id,out var st)||!wallImages.TryGetValue(d.Id,out var im)) return;
        if(!d.IsOnline){st.Text="○ Sin conexión";im.Source=null;return;}
        if(string.IsNullOrWhiteSpace(d.User)){st.Text="○ Disponible · esperando identificación";st.Foreground=Blue;im.Source=null;return;}
        if(!d.Screen){st.Text=$"● {d.Person} · BLOQUEADA: sin supervisión";st.Foreground=Gold;im.Source=null;return;}
        try
        {
            long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(); using var q=new HttpRequestMessage(HttpMethod.Get,$"http://{d.Ip}:{d.ScreenPort}/screen.jpg");
            q.Headers.Add("X-Timestamp",ts.ToString()); q.Headers.Add("X-Signature",AcCrypto.Hmac(settings.Key,$"SCREEN\n{ts}")); var r=await http.SendAsync(q);
            if(!r.IsSuccessStatusCode){st.Text=$"● {d.Person} · esperando cuadro…";st.Foreground=Gold;return;}
            byte[] z=AcCrypto.Open(settings.Key,await r.Content.ReadAsByteArrayAsync()); using var ms=new MemoryStream(z); var bi=new BitmapImage(); bi.BeginInit(); bi.CacheOption=BitmapCacheOption.OnLoad; bi.DecodePixelWidth=305; bi.StreamSource=ms; bi.EndInit(); bi.Freeze(); im.Source=bi;
            st.Text=$"● EN VIVO · {d.Person}\n{d.RoleText}{(string.IsNullOrWhiteSpace(d.Course)?"":" · "+d.Course)}"; st.Foreground=Teal;
        }
        catch { st.Text=$"● {d.Person} · reconectando…"; st.Foreground=Gold; }
    }

    List<Device> Selected() => grid.SelectedItems.Cast<Device>().ToList();

    async Task Send(string action,string value="")
    {
        var s=Selected(); if(s.Count==0){MessageBox.Show("Selecciona al menos una tablet en la pestaña Dispositivos.","Tablet Escolar");return;}
        var rs=await Task.WhenAll(s.Where(x=>x.IsOnline).Select(async d=>(d,await commands!.Send(d,action,value))));
        foreach(var x in rs) if(!x.Item2.Item1) footer.Text=$"{x.d.Name}: {x.Item2.Item2}";
    }

    async Task PromptSend(string action,string title)
    {
        string help = action=="MESSAGE"?"Mensaje breve que verá el usuario":action=="OPEN_URL"?"URL completa (https://...)":action=="LAUNCH_APP"?"Nombre de paquete Android, por ejemplo com.google.android.youtube":"Mensaje que ocupará la pantalla durante Atención";
        var x=Ask(title,help,false); if(!string.IsNullOrWhiteSpace(x)) await Send(action,x);
    }

    void SelectOnline(){grid.SelectedItems.Clear();foreach(var d in devices.Where(x=>x.IsOnline))grid.SelectedItems.Add(d);}

    void OpenMosaic()
    {
        var chosen=Selected().Where(x=>x.IsOnline&&x.Screen&&!string.IsNullOrWhiteSpace(x.User)).ToList();
        if(chosen.Count==0) chosen=devices.Where(x=>x.IsOnline&&x.Screen&&!string.IsNullOrWhiteSpace(x.User)).ToList();
        if(chosen.Count==0){MessageBox.Show("No hay pantallas supervisadas disponibles.","Tablet Escolar");return;}
        new MosaicWindow(chosen,settings.Key){Owner=this}.Show();
    }

    void ConfigDialog(){var k=Ask("Configuración","Clave técnica del establecimiento",true,settings.Key);if(!string.IsNullOrWhiteSpace(k)&&k.Length>=10){settings.Key=k;settings.Save();Start();}}

    string? Ask(string title,string help,bool password,string initial="")
    {
        var w=new Window{Title=title,Width=540,Height=285,ResizeMode=ResizeMode.NoResize,WindowStartupLocation=WindowStartupLocation.CenterOwner,Owner=this,Background=Canvas,FontFamily=new FontFamily("Segoe UI")};
        var p=new StackPanel{Margin=new Thickness(28)}; p.Children.Add(new TextBlock{Text=title,FontSize=24,FontWeight=FontWeights.Bold,Foreground=Navy}); p.Children.Add(new TextBlock{Text=help,Margin=new Thickness(0,7,0,14),TextWrapping=TextWrapping.Wrap,Foreground=Muted});
        Control input; if(password){var q=new PasswordBox{FontSize=16,Padding=new Thickness(12),Password=initial,Background=Brushes.White};input=q;}else{var q=new TextBox{FontSize=16,Padding=new Thickness(12),Text=initial,Background=Brushes.White};input=q;} p.Children.Add(input);
        var row=new StackPanel{Orientation=Orientation.Horizontal,HorizontalAlignment=HorizontalAlignment.Right,Margin=new Thickness(0,18,0,0)}; string? result=null;
        row.Children.Add(Action("Cancelar",(_,_)=>w.Close())); row.Children.Add(Action("Aceptar",(_,_)=>{result=input is PasswordBox pb?pb.Password:((TextBox)input).Text;w.DialogResult=true;},Blue)); p.Children.Add(row); w.Content=p; w.ShowDialog(); return result;
    }
}
