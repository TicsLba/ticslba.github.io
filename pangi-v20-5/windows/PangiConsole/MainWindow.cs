using Microsoft.Win32;
using System.Security.Cryptography;
using System.Text.Json;
using System.Collections.ObjectModel;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace PangiConsole;

public class MainWindow : Window
{
    static readonly SolidColorBrush Blue = new(Color.FromRgb(24,167,124));
    static readonly SolidColorBrush Navy = new(Color.FromRgb(17,24,32));
    static readonly SolidColorBrush Teal = new(Color.FromRgb(90,216,177));
    static readonly SolidColorBrush Gold = new(Color.FromRgb(242,184,75));
    static readonly SolidColorBrush Red = new(Color.FromRgb(231,101,101));
    static readonly SolidColorBrush Ink = new(Color.FromRgb(245,247,247));
    static readonly SolidColorBrush Muted = new(Color.FromRgb(168,184,185));
    static readonly SolidColorBrush Canvas = new(Color.FromRgb(17,24,32));
    static readonly SolidColorBrush Line = new(Color.FromRgb(45,61,68));
    static readonly SolidColorBrush SoftBlue = new(Color.FromRgb(18,56,68));
    static readonly SolidColorBrush SoftTeal = new(Color.FromRgb(19,63,54));
    static readonly SolidColorBrush SoftGold = new(Color.FromRgb(67,56,33));
    static readonly SolidColorBrush SoftRed = new(Color.FromRgb(74,45,48));

    readonly ObservableCollection<Device> devices = new();
    readonly ObservableCollection<DeviceUsageSummary> deviceReports = new();
    readonly ObservableCollection<UserUsageSummary> userReports = new();
    readonly ObservableCollection<RecoveryRow> recoveryRows = new();
    readonly Dictionary<string,DateTime> localSeen = new(StringComparer.OrdinalIgnoreCase);

    readonly DataGrid deviceGrid = GridBase();
    readonly DataGrid deviceReportGrid = GridBase();
    readonly DataGrid userReportGrid = GridBase();
    readonly DataGrid recoveryGrid = GridBase();
    readonly WrapPanel wall = new() { Margin = new Thickness(8) };
    readonly Dictionary<string,Image> wallImages = new();
    readonly Dictionary<string,TextBlock> wallStatus = new();
    readonly TextBlock footer = new() { Foreground = Muted, VerticalAlignment = VerticalAlignment.Center };
    readonly TextBlock statOnline = new(), statLive = new(), statStudents = new(), statTeachers = new(), statAlerts = new();
    readonly TextBlock reportCaption = new() { Foreground = Muted };
    readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(1400) };
    readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(2.5) };
    readonly TelemetryStore telemetry = new();

    Settings settings;
    RelaySettings relaySettings;
    Discovery? discovery;
    Commands? commands;
    Registry? registry;
    RemoteClient? remote;
    string wallSignature = "";
    bool wallBusy,remoteBusy;
    TimeSpan reportPeriod = TimeSpan.FromDays(30);
    int ticks;

    public MainWindow()
    {
        Title = "PANGI · Centro de gestión";
        Width = 1560;
        Height = 920;
        MinWidth = 1180;
        MinHeight = 720;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        Background = Canvas;
        FontFamily = new FontFamily("Segoe UI");
        settings = Settings.Load();
        relaySettings = RelaySettings.Load();
        BuildUi();
        timer.Tick += async (_,_) =>
        {
            RefreshStats();
            await RefreshWall();
            ticks++;
            if(ticks % 3 == 0) RefreshRecovery();
            if(ticks % 8 == 0) await SyncRemote();
            if(ticks % 20 == 0) RefreshReports();
        };
        timer.Start();
        Loaded += (_,_) => Configure();
        Closed += (_,_) => { discovery?.Dispose(); remote?.Dispose(); timer.Stop(); http.Dispose(); };
    }

    static DataGrid GridBase() => new()
    {
        SelectionMode = DataGridSelectionMode.Extended,
        AutoGenerateColumns = false,
        IsReadOnly = true,
        CanUserAddRows = false,
        RowHeight = 46,
        HeadersVisibility = DataGridHeadersVisibility.Column,
        GridLinesVisibility = DataGridGridLinesVisibility.Horizontal,
        Background = new SolidColorBrush(Color.FromRgb(25,35,43)),
        BorderThickness = new Thickness(0)
    };

    Border Card(UIElement child,double pad=16) => new()
    {
        Background = new SolidColorBrush(Color.FromRgb(25,35,43)),
        CornerRadius = new CornerRadius(18),
        Padding = new Thickness(pad),
        BorderBrush = Line,
        BorderThickness = new Thickness(1),
        Child = child
    };

    Button Action(string text,RoutedEventHandler click,Brush? fill=null,Brush? foreground=null)
    {
        var b = new Button
        {
            Content = text,
            Padding = new Thickness(14,9,14,9),
            Margin = new Thickness(4),
            Background = fill ?? new SolidColorBrush(Color.FromRgb(34,45,53)),
            Foreground = foreground ?? (fill == null ? Ink : Brushes.White),
            BorderThickness = new Thickness(fill == null ? 1 : 0),
            BorderBrush = Line,
            FontWeight = FontWeights.SemiBold,
            Cursor = System.Windows.Input.Cursors.Hand
        };
        b.Click += click;
        return b;
    }

    TextBlock TitleText(string s,double z=24) => new(){Text=s,FontSize=z,FontWeight=FontWeights.Bold,Foreground=Navy};
    TextBlock Hint(string s) => new(){Text=s,Foreground=Muted,TextWrapping=TextWrapping.Wrap,Margin=new Thickness(0,5,0,10)};

    Border Stat(string title,string caption,TextBlock value,Brush accent)
    {
        value.FontSize=29;value.FontWeight=FontWeights.Bold;value.Foreground=Ink;value.Text="0";
        var p=new StackPanel();
        p.Children.Add(new TextBlock{Text=title,Foreground=accent,FontWeight=FontWeights.Bold,FontSize=11});
        p.Children.Add(value);
        p.Children.Add(new TextBlock{Text=caption,Foreground=Muted,FontSize=11});
        var c=Card(p,13);c.Margin=new Thickness(4);return c;
    }

    UIElement BrandMark()
    {
        var box=new Border{Width=58,Height=58,CornerRadius=new CornerRadius(16),Background=Blue};
        var g=new Grid();
        var tablet=new Border{Width=29,Height=38,CornerRadius=new CornerRadius(5),BorderBrush=Brushes.White,BorderThickness=new Thickness(3),HorizontalAlignment=HorizontalAlignment.Center,VerticalAlignment=VerticalAlignment.Center};
        var dot=new Border{Width=4,Height=4,CornerRadius=new CornerRadius(2),Background=Brushes.White,HorizontalAlignment=HorizontalAlignment.Center,VerticalAlignment=VerticalAlignment.Bottom,Margin=new Thickness(0,0,0,7)};
        g.Children.Add(tablet);g.Children.Add(dot);box.Child=g;return box;
    }

    void BuildUi()
    {
        var root=new Grid();
        root.RowDefinitions.Add(new RowDefinition{Height=new GridLength(100)});
        root.RowDefinitions.Add(new RowDefinition{Height=new GridLength(100)});
        root.RowDefinitions.Add(new RowDefinition{Height=new GridLength(62)});
        root.RowDefinitions.Add(new RowDefinition());
        root.RowDefinitions.Add(new RowDefinition{Height=new GridLength(38)});

        var head=new Border{Background=Navy,Padding=new Thickness(24,14,24,14)};
        var hg=new Grid();hg.ColumnDefinitions.Add(new ColumnDefinition());hg.ColumnDefinitions.Add(new ColumnDefinition{Width=GridLength.Auto});
        var left=new StackPanel{Orientation=Orientation.Horizontal,VerticalAlignment=VerticalAlignment.Center};left.Children.Add(BrandMark());
        var words=new StackPanel{Margin=new Thickness(15,0,0,0),VerticalAlignment=VerticalAlignment.Center};
        words.Children.Add(new TextBlock{Text="PANGI V20.5",Foreground=Brushes.White,FontSize=30,FontWeight=FontWeights.Bold});
        words.Children.Add(new TextBlock{Text="Aula · dispositivos · responsables · informes · recuperación",Foreground=new SolidColorBrush(Color.FromRgb(201,213,238)),FontSize=13});
        left.Children.Add(words);hg.Children.Add(left);
        var right=new StackPanel{Orientation=Orientation.Horizontal,VerticalAlignment=VerticalAlignment.Center};
        right.Children.Add(new Border{Background=Blue,CornerRadius=new CornerRadius(12),Padding=new Thickness(12,7,12,7),Margin=new Thickness(0,0,9,0),Child=new TextBlock{Text="V20.5 · Observa · Protege · Responde",Foreground=Brushes.White,FontWeight=FontWeights.SemiBold}});
        right.Children.Add(Action("Configuración",(_,_)=>ConfigDialog(),Gold,Navy));Grid.SetColumn(right,1);hg.Children.Add(right);head.Child=hg;root.Children.Add(head);

        var stats=new Grid{Margin=new Thickness(18,7,18,3)};for(int i=0;i<5;i++)stats.ColumnDefinitions.Add(new ColumnDefinition());
        var a=Stat("EN LÍNEA","tablets visibles",statOnline,Blue);stats.Children.Add(a);
        var b=Stat("EN VIVO","pantallas supervisadas",statLive,Teal);Grid.SetColumn(b,1);stats.Children.Add(b);
        var c=Stat("ESTUDIANTES","sesiones activas",statStudents,Blue);Grid.SetColumn(c,2);stats.Children.Add(c);
        var d=Stat("PROFESORES","sesiones activas",statTeachers,Navy);Grid.SetColumn(d,3);stats.Children.Add(d);
        var e=Stat("REVISAR","sin pantalla / recuperación",statAlerts,Gold);Grid.SetColumn(e,4);stats.Children.Add(e);Grid.SetRow(stats,1);root.Children.Add(stats);

        var bar=new WrapPanel{Margin=new Thickness(18,3,18,4)};
        bar.Children.Add(Action("Seleccionar en línea",(_,_)=>SelectOnline()));
        bar.Children.Add(Action("Mosaico",(_,_)=>OpenMosaic(),SoftBlue,Navy));
        bar.Children.Add(Action("Mensaje",async(_,_)=>await PromptSend("MESSAGE","Enviar mensaje")));
        bar.Children.Add(Action("Abrir enlace",async(_,_)=>await PromptSend("OPEN_URL","Abrir enlace")));
        bar.Children.Add(Action("Abrir app",async(_,_)=>await PromptSend("LAUNCH_APP","Abrir aplicación")));
        bar.Children.Add(Action("Solicitar supervisión",async(_,_)=>await SendSelected("REQUEST_SCREEN"),SoftTeal,Navy));
        bar.Children.Add(Action("Refresco",async(_,_)=>await SetFps(),SoftTeal,Navy));
        bar.Children.Add(Action("Inactividad",async(_,_)=>await SetIdleSelected(),SoftBlue,Navy));
        bar.Children.Add(Action("Volumen",async(_,_)=>await SetVolumeSelected(),SoftBlue,Navy));
        bar.Children.Add(Action("Instalar APK",async(_,_)=>await InstallApkSelected(),SoftGold,Navy));
        bar.Children.Add(Action("Enviar archivo",async(_,_)=>await SendFileSelected(),SoftBlue,Navy));
        bar.Children.Add(Action("Desinstalar app",async(_,_)=>await PromptSend("UNINSTALL_PACKAGE","Desinstalar aplicación")));
        bar.Children.Add(Action("Bloquear",async(_,_)=>await SendSelected("LOCK_NOW"),Navy));
        bar.Children.Add(Action("Reiniciar",async(_,_)=>await SendSelected("REBOOT"),Red));
        bar.Children.Add(Action("Atención",async(_,_)=>await PromptSend("ATTENTION_ON","Modo atención"),Navy));
        bar.Children.Add(Action("Liberar atención",async(_,_)=>await SendSelected("ATTENTION_OFF"),Teal));
        bar.Children.Add(Action("Cerrar sesión",async(_,_)=>await SendSelected("FORCE_LOGOUT"),Red));
        bar.Children.Add(Action("Vincular relay",async(_,_)=>await PushRelayToSelected(),SoftGold,Navy));
        Grid.SetRow(bar,2);root.Children.Add(bar);

        var tabs=new TabControl{Margin=new Thickness(20,3,20,10),Background=Canvas,BorderThickness=new Thickness(0)};
        tabs.Items.Add(new TabItem{Header="  Aula  ",Content=BuildClassroomTab()});
        tabs.Items.Add(new TabItem{Header="  Dispositivos  ",Content=BuildDevicesTab()});
        tabs.Items.Add(new TabItem{Header="  Informes  ",Content=BuildReportsTab()});
        tabs.Items.Add(new TabItem{Header="  Recuperación  ",Content=BuildRecoveryTab()});
        Grid.SetRow(tabs,3);root.Children.Add(tabs);

        var foot=new Border{Background=new SolidColorBrush(Color.FromRgb(235,240,248)),Padding=new Thickness(18,8,18,8),Child=footer};Grid.SetRow(foot,4);root.Children.Add(foot);Content=root;
    }

    UIElement BuildClassroomTab()
    {
        var panel=new Grid();
        panel.RowDefinitions.Add(new RowDefinition{Height=GridLength.Auto});panel.RowDefinitions.Add(new RowDefinition());
        var intro=new StackPanel{Margin=new Thickness(6,8,6,8)};intro.Children.Add(TitleText("Aula en vivo",22));intro.Children.Add(Hint("Supervisión en tiempo real por LAN o Relay HTTPS. Sólo se conserva el último cuadro remoto; no se graban sesiones."));panel.Children.Add(intro);
        var scroll=new ScrollViewer{Content=wall,VerticalScrollBarVisibility=ScrollBarVisibility.Auto};Grid.SetRow(scroll,1);panel.Children.Add(Card(scroll,4));return panel;
    }

    UIElement BuildDevicesTab()
    {
        deviceGrid.ItemsSource=devices;
        AddCol(deviceGrid,"Estado","Status",110);AddCol(deviceGrid,"Tablet","Name",150);AddCol(deviceGrid,"Responsable","Person",190);AddCol(deviceGrid,"Rol","RoleText",100);AddCol(deviceGrid,"Curso","CourseText",110);
        AddCol(deviceGrid,"Supervisión","Supervision",135);AddCol(deviceGrid,"Gestión","Protection",130);AddCol(deviceGrid,"Batería","Battery",75,"{0}%");AddCol(deviceGrid,"Modelo","Model",180);AddCol(deviceGrid,"Android","Android",75);AddCol(deviceGrid,"IP","Ip",120);
        var panel=new Grid();panel.RowDefinitions.Add(new RowDefinition{Height=GridLength.Auto});panel.RowDefinitions.Add(new RowDefinition());
        var top=new StackPanel{Margin=new Thickness(6,8,6,8)};top.Children.Add(TitleText("Inventario operativo",22));top.Children.Add(Hint("Identidad permanente del dispositivo, sesión responsable actual y estado técnico. Los equipos remotos aparecen cuando el relay HTTPS está configurado."));panel.Children.Add(top);Grid.SetRow(deviceGrid,1);panel.Children.Add(Card(deviceGrid,0));return panel;
    }

    UIElement BuildReportsTab()
    {
        deviceReportGrid.ItemsSource=deviceReports;userReportGrid.ItemsSource=userReports;
        AddCol(deviceReportGrid,"Tablet","DeviceName",170);AddCol(deviceReportGrid,"ID","DeviceId",140);AddCol(deviceReportGrid,"Uso","UsageText",90);AddCol(deviceReportGrid,"Sesiones","Sessions",85);AddCol(deviceReportGrid,"Usuarios","DistinctUsers",85);AddCol(deviceReportGrid,"Estudiante","StudentText",95);AddCol(deviceReportGrid,"Profesor","TeacherText",95);AddCol(deviceReportGrid,"Bat. prom.","BatteryAverage",85);AddCol(deviceReportGrid,"Bat. mín.","BatteryMinimum",85);AddCol(deviceReportGrid,"Última actividad","LastSeenText",145);
        AddCol(userReportGrid,"Responsable","User",220);AddCol(userReportGrid,"Rol","Role",100);AddCol(userReportGrid,"Curso","Course",120);AddCol(userReportGrid,"Uso","UsageText",95);AddCol(userReportGrid,"Sesiones","Sessions",85);AddCol(userReportGrid,"Tablets","DevicesUsed",80);AddCol(userReportGrid,"Primera actividad","FirstSeenText",145);AddCol(userReportGrid,"Última actividad","LastSeenText",145);

        var root=new Grid();root.RowDefinitions.Add(new RowDefinition{Height=GridLength.Auto});root.RowDefinitions.Add(new RowDefinition());
        var top=new Grid{Margin=new Thickness(6,8,6,8)};top.ColumnDefinitions.Add(new ColumnDefinition());top.ColumnDefinitions.Add(new ColumnDefinition{Width=GridLength.Auto});
        var title=new StackPanel();title.Children.Add(TitleText("Informes de uso y responsabilidad",22));reportCaption.Text="Últimos 30 días · una muestra aprox. por minuto mientras la consola o relay reciben la tablet";title.Children.Add(reportCaption);top.Children.Add(title);
        var period=new StackPanel{Orientation=Orientation.Horizontal,VerticalAlignment=VerticalAlignment.Center};period.Children.Add(Action("7 días",(_,_)=>SetReportPeriod(7)));period.Children.Add(Action("30 días",(_,_)=>SetReportPeriod(30),Blue));period.Children.Add(Action("90 días",(_,_)=>SetReportPeriod(90)));Grid.SetColumn(period,1);top.Children.Add(period);root.Children.Add(top);
        var sub=new TabControl{Background=Canvas,BorderThickness=new Thickness(0)};
        var dp=new Grid();dp.RowDefinitions.Add(new RowDefinition());dp.RowDefinitions.Add(new RowDefinition{Height=new GridLength(54)});dp.Children.Add(Card(deviceReportGrid,0));var db=new StackPanel{Orientation=Orientation.Horizontal,HorizontalAlignment=HorizontalAlignment.Right};db.Children.Add(Action("Actualizar",(_,_)=>RefreshReports()));db.Children.Add(Action("Exportar tablet seleccionada",(_,_)=>ExportDeviceReport(),Blue));Grid.SetRow(db,1);dp.Children.Add(db);sub.Items.Add(new TabItem{Header="  Por dispositivo  ",Content=dp});
        var up=new Grid();up.RowDefinitions.Add(new RowDefinition());up.RowDefinitions.Add(new RowDefinition{Height=new GridLength(54)});up.Children.Add(Card(userReportGrid,0));var ub=new StackPanel{Orientation=Orientation.Horizontal,HorizontalAlignment=HorizontalAlignment.Right};ub.Children.Add(Action("Actualizar",(_,_)=>RefreshReports()));ub.Children.Add(Action("Exportar responsable seleccionado",(_,_)=>ExportUserReport(),Blue));Grid.SetRow(ub,1);up.Children.Add(ub);sub.Items.Add(new TabItem{Header="  Por responsable  ",Content=up});
        Grid.SetRow(sub,1);root.Children.Add(sub);return root;
    }

    UIElement BuildRecoveryTab()
    {
        recoveryGrid.ItemsSource=recoveryRows;
        AddCol(recoveryGrid,"Tablet","DeviceName",160);AddCol(recoveryGrid,"Estado","Status",105);AddCol(recoveryGrid,"Responsable","Person",190);AddCol(recoveryGrid,"Batería","Battery",75,"{0}%");AddCol(recoveryGrid,"Recuperación","Recovery",120);AddCol(recoveryGrid,"Ubicación","Location",190);AddCol(recoveryGrid,"Precisión","Accuracy",85);AddCol(recoveryGrid,"Capturada","LocationTime",150);
        var root=new Grid();root.RowDefinitions.Add(new RowDefinition{Height=GridLength.Auto});root.RowDefinitions.Add(new RowDefinition());root.RowDefinitions.Add(new RowDefinition{Height=new GridLength(58)});
        var intro=new StackPanel{Margin=new Thickness(6,8,6,8)};intro.Children.Add(TitleText("Ubicación y recuperación",22));intro.Children.Add(Hint("Seguimiento del dispositivo institucional. La ubicación muestra fecha y precisión. Con relay HTTPS configurado, los comandos pueden quedar pendientes aunque la tablet esté en otra red."));root.Children.Add(intro);
        Grid.SetRow(recoveryGrid,1);root.Children.Add(Card(recoveryGrid,0));
        var bar=new WrapPanel{HorizontalAlignment=HorizontalAlignment.Right,Margin=new Thickness(0,4,0,0)};
        bar.Children.Add(Action("Solicitar ubicación",async(_,_)=>await RecoveryAction("REQUEST_LOCATION")));
        bar.Children.Add(Action("Ver en mapa",(_,_)=>OpenMap(),SoftBlue,Navy));
        bar.Children.Add(Action("Bloquear ahora",async(_,_)=>await RecoveryAction("LOCK_NOW"),Navy));
        bar.Children.Add(Action("Hacer sonar",async(_,_)=>await RecoveryAction("RING_ON"),SoftGold,Navy));
        bar.Children.Add(Action("Detener sonido",async(_,_)=>await RecoveryAction("RING_OFF")));
        bar.Children.Add(Action("Activar modo pérdida",async(_,_)=>await EnableLostMode(),Red));
        bar.Children.Add(Action("Marcar recuperada",async(_,_)=>await RecoveryAction("LOST_MODE_OFF"),Teal));
        Grid.SetRow(bar,2);root.Children.Add(bar);return root;
    }

    void AddCol(DataGrid g,string h,string b,double w,string? fmt=null)
    {
        var x=new DataGridTextColumn{Header=h,Binding=new Binding(b),Width=w};if(fmt!=null)((Binding)x.Binding).StringFormat=fmt;g.Columns.Add(x);
    }

    void Configure()
    {
        if(string.IsNullOrWhiteSpace(settings.Key))
        {
            var k=Ask("Configuración inicial","Ingresa la misma clave técnica configurada en las tablets.",true);
            if(string.IsNullOrWhiteSpace(k)||k.Length<10){footer.Text="Configura una clave técnica para comenzar.";return;}
            settings.Key=k;settings.Save();
        }
        Start();
    }

    void Start()
    {
        discovery?.Dispose();remote?.Dispose();devices.Clear();localSeen.Clear();wall.Children.Clear();wallImages.Clear();wallStatus.Clear();wallSignature="";
        registry=new Registry(settings.Key);foreach(var x in registry.Load())devices.Add(x);
        commands=new Commands(settings.Key);discovery=new Discovery(settings.Key);discovery.Seen+=Incoming;
        remote=relaySettings.Enabled?new RemoteClient(settings.Key,relaySettings.Url):null;
        try{discovery.Start();footer.Text=relaySettings.Enabled?"● Consola activa · LAN + relay HTTPS · informes por responsable · ubicación y recuperación":"● Consola activa · LAN · configura Relay HTTPS para gestión entre redes";}
        catch(Exception e){footer.Text="No se pudo abrir UDP 45888: "+e.Message;}
        RefreshStats();RefreshReports();RefreshRecovery();_=RefreshWall();_=SyncRemote();
    }

    void Incoming(Device n)=>Dispatcher.Invoke(()=>
    {
        localSeen[n.Id]=DateTime.Now;
        var d=devices.FirstOrDefault(x=>x.Id==n.Id);
        if(d==null){devices.Add(n);d=n;if(!string.IsNullOrWhiteSpace(n.User))SessionLog.Change(n.Id,"",n.User,n.Course);}
        else
        {
            string old=d.User;
            d.DeviceName=n.DeviceName;d.User=n.User;d.Course=n.Course;d.Role=n.Role;d.Model=n.Model;d.Android=n.Android;d.Ip=n.Ip;d.CommandPort=n.CommandPort;d.ScreenPort=n.ScreenPort;d.Battery=n.Battery;d.Screen=n.Screen;d.Managed=n.Managed;d.FrameAgeMs=n.FrameAgeMs;d.Seen=DateTime.Now;d.Identity();
            if(old!=d.User)SessionLog.Change(d.Id,old,d.User,d.Course);
        }
        telemetry.Observe(d);registry?.Save(devices);RefreshStats();RefreshRecovery();_=RefreshWall();
    });

    bool IsLocal(Device d)=>localSeen.TryGetValue(d.Id,out var t)&&DateTime.Now-t<TimeSpan.FromSeconds(10)&&!string.IsNullOrWhiteSpace(d.Ip);

    async Task SyncRemote()
    {
        if(remoteBusy||remote==null||!remote.Enabled)return;remoteBusy=true;
        try
        {
            var list=await remote.FetchDevicesAsync();
            if(list.Count==0)return;
            await Dispatcher.InvokeAsync(()=>
            {
                foreach(var x in list)
                {
                    var d=devices.FirstOrDefault(q=>q.Id==x.DeviceId);
                    if(d==null){d=new Device{Id=x.DeviceId};devices.Add(d);}
                    bool local=IsLocal(d);
                    string old=d.User;
                    d.DeviceName=x.DeviceName;d.User=x.User;d.Course=x.Course;d.Role=x.Role;d.Model=x.Model;d.Android=x.Android;d.Battery=x.Battery;d.Managed=x.Managed;
                    d.AppVersion=x.AppVersion;d.Charging=x.Charging;d.Wifi=x.Wifi;d.StorageFreeMb=x.StorageFreeMb;d.StorageTotalMb=x.StorageTotalMb;
                    if(!local){d.Screen=x.Screen;d.FrameAgeMs=x.Screen?0:-1;d.Seen=x.Seen;}
                    d.Identity();
                    if(old!=d.User)SessionLog.Change(d.Id,old,d.User,d.Course);
                    LiveRecovery.Update(x.DeviceId,x.Lat,x.Lon,x.Accuracy,x.LocationTs,x.Lost);
                    telemetry.Observe(d);
                }
                registry?.Save(devices);RefreshStats();RefreshRecovery();
            });
        }
        finally{remoteBusy=false;}
    }

    void RefreshStats()
    {
        foreach(var d in devices)d.Computed();var on=devices.Where(x=>x.IsOnline).ToList();
        statOnline.Text=on.Count.ToString();statLive.Text=on.Count(x=>!string.IsNullOrWhiteSpace(x.User)&&x.Screen).ToString();statStudents.Text=on.Count(x=>x.Role=="student").ToString();statTeachers.Text=on.Count(x=>x.Role=="teacher").ToString();
        statAlerts.Text=devices.Count(x=>LiveRecovery.Get(x.Id).Lost||(x.IsOnline&&!string.IsNullOrWhiteSpace(x.User)&&!x.Screen)).ToString();
    }

    void SetReportPeriod(int days){reportPeriod=TimeSpan.FromDays(days);reportCaption.Text=$"Últimos {days} días · seguimiento por tablet y responsable";RefreshReports();}
    void RefreshReports()
    {
        deviceReports.Clear();foreach(var x in telemetry.Summaries(devices,reportPeriod))deviceReports.Add(x);
        userReports.Clear();foreach(var x in telemetry.SummariesByUser(reportPeriod))userReports.Add(x);
    }

    void ExportDeviceReport()
    {
        if(deviceReportGrid.SelectedItem is not DeviceUsageSummary x){MessageBox.Show("Selecciona una tablet del informe.","PANGI");return;}
        try{var p=telemetry.ExportCsv(x.DeviceId,reportPeriod);footer.Text="Informe exportado: "+p;MessageBox.Show("Informe guardado en:\n"+p,"PANGI");}catch(Exception e){MessageBox.Show(e.Message,"No se pudo exportar");}
    }
    void ExportUserReport()
    {
        if(userReportGrid.SelectedItem is not UserUsageSummary x){MessageBox.Show("Selecciona un responsable del informe.","PANGI");return;}
        try{var p=telemetry.ExportUserCsv(x.User,reportPeriod);footer.Text="Informe exportado: "+p;MessageBox.Show("Informe guardado en:\n"+p,"PANGI");}catch(Exception e){MessageBox.Show(e.Message,"No se pudo exportar");}
    }

    void RefreshRecovery()
    {
        var selected=(recoveryGrid.SelectedItem as RecoveryRow)?.DeviceId;
        recoveryRows.Clear();
        foreach(var d in devices.OrderByDescending(x=>LiveRecovery.Get(x.Id).Lost).ThenByDescending(x=>x.IsOnline).ThenBy(x=>x.Name))
        {
            var r=LiveRecovery.Get(d.Id);
            recoveryRows.Add(new RecoveryRow{DeviceId=d.Id,DeviceName=d.Name,Status=d.Status,Person=d.Person,Battery=d.Battery,Recovery=r.StateText,Location=r.LocationText,Accuracy=r.AccuracyText,LocationTime=r.CapturedText,MapUrl=r.MapUrl});
        }
        if(selected!=null)recoveryGrid.SelectedItem=recoveryRows.FirstOrDefault(x=>x.DeviceId==selected);
    }

    Device? RecoveryDevice(){if(recoveryGrid.SelectedItem is not RecoveryRow row)return null;return devices.FirstOrDefault(x=>x.Id==row.DeviceId);}

    async Task<(bool,string)> SendSmart(Device d,string action,string value="")
    {
        if(IsLocal(d)&&commands!=null)return await commands.Send(d,action,value);
        if(remote!=null&&remote.Enabled)return await remote.SendCommandAsync(d.Id,action,value);
        return(false,"Tablet fuera de LAN y relay no configurado");
    }

    async Task RecoveryAction(string action)
    {
        var d=RecoveryDevice();if(d==null){MessageBox.Show("Selecciona una tablet.","PANGI");return;}
        var r=await SendSmart(d,action);footer.Text=r.Item1?$"{d.Name}: comando {action} enviado":$"{d.Name}: {r.Item2}";
        if(action=="REQUEST_LOCATION"){await Task.Delay(1400);await SyncRemote();}RefreshRecovery();
    }
    async Task EnableLostMode()
    {
        var d=RecoveryDevice();if(d==null){MessageBox.Show("Selecciona una tablet.","PANGI");return;}
        if(MessageBox.Show($"¿Activar Modo pérdida en {d.Name}?\n\nEl equipo mostrará una pantalla institucional de recuperación, reforzará ubicación cuando sea posible y se bloqueará.","PANGI",MessageBoxButton.YesNo,MessageBoxImage.Warning)!=MessageBoxResult.Yes)return;
        await RecoveryAction("LOST_MODE_ON");
    }
    void OpenMap()
    {
        if(recoveryGrid.SelectedItem is not RecoveryRow row||string.IsNullOrWhiteSpace(row.MapUrl)){MessageBox.Show("La tablet todavía no tiene una ubicación disponible.","PANGI");return;}
        try{System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(row.MapUrl){UseShellExecute=true});}catch(Exception e){MessageBox.Show(e.Message,"No se pudo abrir el mapa");}
    }

    string WallSignature()=>string.Join("|",devices.OrderBy(x=>x.Id).Select(x=>$"{x.Id}:{x.User}:{x.Role}:{x.Course}:{x.IsOnline}:{x.Screen}:{x.Managed}:{LiveRecovery.Get(x.Id).Lost}:{IsLocal(x)}"));
    void RebuildWall()
    {
        wall.Children.Clear();wallImages.Clear();wallStatus.Clear();
        foreach(var d in devices.OrderByDescending(x=>x.IsOnline).ThenBy(x=>x.Name))
        {
            var lost=LiveRecovery.Get(d.Id).Lost;
            var g=new Grid();g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(48)});g.RowDefinitions.Add(new RowDefinition());g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(60)});
            var h=new Grid{Margin=new Thickness(12,8,12,6)};h.ColumnDefinitions.Add(new ColumnDefinition());h.ColumnDefinitions.Add(new ColumnDefinition{Width=GridLength.Auto});h.Children.Add(new TextBlock{Text=d.Name,Foreground=Ink,FontSize=15,FontWeight=FontWeights.Bold,VerticalAlignment=VerticalAlignment.Center});
            var badge=new Border{Background=lost?SoftRed:d.Managed?SoftTeal:SoftGold,CornerRadius=new CornerRadius(10),Padding=new Thickness(8,4,8,4),Child=new TextBlock{Text=lost?"PÉRDIDA":IsLocal(d)?"LAN":"REMOTA",Foreground=lost?Red:IsLocal(d)?Teal:Blue,FontSize=11,FontWeight=FontWeights.SemiBold}};Grid.SetColumn(badge,1);h.Children.Add(badge);g.Children.Add(h);
            var ib=new Border{Background=new SolidColorBrush(Color.FromRgb(15,23,42)),CornerRadius=new CornerRadius(10),Margin=new Thickness(8,0,8,0)};var im=new Image{Stretch=Stretch.Uniform};ib.Child=im;Grid.SetRow(ib,1);g.Children.Add(ib);wallImages[d.Id]=im;
            var st=new TextBlock{Foreground=Muted,FontWeight=FontWeights.SemiBold,Margin=new Thickness(12,7,12,7),TextWrapping=TextWrapping.Wrap};st.Text=WallText(d);Grid.SetRow(st,2);g.Children.Add(st);wallStatus[d.Id]=st;
            var card=Card(g,0);card.Width=326;card.Height=270;card.Margin=new Thickness(7);card.Cursor=System.Windows.Input.Cursors.Hand;card.MouseLeftButtonDown+=(_,_)=>{if(d.Screen&&!string.IsNullOrWhiteSpace(d.User))new ViewerWindow(d,settings.Key,relaySettings.Url){Owner=this}.Show();};wall.Children.Add(card);
        }
        if(devices.Count==0)wall.Children.Add(new TextBlock{Text="Esperando tablets por LAN o relay institucional…",Foreground=Muted,FontSize=18,Margin=new Thickness(25)});
    }
    string WallText(Device d)=>LiveRecovery.Get(d.Id).Lost?$"● MODO PÉRDIDA · {LiveRecovery.Get(d.Id).LocationText}":!d.IsOnline?"○ Sin conexión":!IsLocal(d)?$"● REMOTA · {d.Person}\núltimo reporte por relay":string.IsNullOrWhiteSpace(d.User)?"○ Disponible · esperando identificación":d.Screen?$"● EN VIVO · {d.Person}\n{d.RoleText}{(string.IsNullOrWhiteSpace(d.Course)?"":" · "+d.Course)}":$"● {d.Person} · sesión sin pantalla";

    async Task RefreshWall(){if(wallBusy)return;wallBusy=true;try{var s=WallSignature();if(s!=wallSignature){wallSignature=s;RebuildWall();}await Task.WhenAll(devices.Where(d=>d.IsOnline&&d.Screen&&!LiveRecovery.Get(d.Id).Lost).Select(FetchFrame));}finally{wallBusy=false;}}
    async Task FetchFrame(Device d)
    {
        if(!wallStatus.TryGetValue(d.Id,out var st)||!wallImages.TryGetValue(d.Id,out var im))return;if(!d.IsOnline){st.Text="○ Sin conexión";im.Source=null;return;}if(string.IsNullOrWhiteSpace(d.User)){st.Text="○ Disponible · esperando identificación";st.Foreground=Blue;im.Source=null;return;}if(!d.Screen){st.Text=$"● {d.Person} · BLOQUEADA: sin supervisión";st.Foreground=Gold;im.Source=null;return;}
        try{
            byte[]? z=null;
            if(IsLocal(d)){
                long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();using var q=new HttpRequestMessage(HttpMethod.Get,$"http://{d.Ip}:{d.ScreenPort}/screen.jpg");q.Headers.Add("X-Timestamp",ts.ToString());q.Headers.Add("X-Signature",AcCrypto.Hmac(settings.Key,$"SCREEN\n{ts}"));using var resp=await http.SendAsync(q);if(resp.IsSuccessStatusCode)z=AcCrypto.Open(settings.Key,await resp.Content.ReadAsByteArrayAsync());
            }else if(remote!=null&&remote.Enabled)z=await remote.FetchScreenAsync(d.Id);
            if(z==null||z.Length==0){st.Text=$"● {d.Person} · esperando cuadro…";st.Foreground=Gold;return;}
            using var ms=new MemoryStream(z);var bi=new BitmapImage();bi.BeginInit();bi.CacheOption=BitmapCacheOption.OnLoad;bi.DecodePixelWidth=305;bi.StreamSource=ms;bi.EndInit();bi.Freeze();im.Source=bi;st.Text=$"● EN VIVO · {d.Person}\n{d.RoleText}{(string.IsNullOrWhiteSpace(d.Course)?"":" · "+d.Course)}";st.Foreground=Teal;
        }catch{st.Text=$"● {d.Person} · reconectando…";st.Foreground=Gold;}
    }

    List<Device> Selected()=>deviceGrid.SelectedItems.Cast<Device>().ToList();
    async Task SendSelected(string action,string value="")
    {
        var s=Selected();if(s.Count==0){MessageBox.Show("Selecciona al menos una tablet en Dispositivos.","PANGI");return;}
        foreach(var d in s){var r=await SendSmart(d,action,value);if(!r.Item1)footer.Text=$"{d.Name}: {r.Item2}";}
    }
    async Task PromptSend(string action,string title)
    {
        string help=action=="MESSAGE"?"Mensaje breve que verá el usuario":action=="OPEN_URL"?"URL completa (https://...)":action=="LAUNCH_APP"?"Nombre de paquete Android, por ejemplo com.google.android.youtube":action=="UNINSTALL_PACKAGE"?"Paquete Android que se eliminará (solo Device Owner)":"Mensaje que ocupará la pantalla durante Atención";var x=Ask(title,help,false);if(!string.IsNullOrWhiteSpace(x))await SendSelected(action,x);
    }

    async Task SetVolumeSelected(){
        var x=Ask("Volumen","Porcentaje de volumen multimedia entre 0 y 100.",false,"50");if(!int.TryParse(x,out var v)||v<0||v>100){MessageBox.Show("Usa un número entre 0 y 100.","PANGI");return;}await SendSelected("SET_VOLUME",v.ToString());
    }
    async Task SetIdleSelected(){
        var role=Ask("Inactividad","Escribe student o teacher.",false,"student");if(role!="student"&&role!="teacher"){MessageBox.Show("Usa student o teacher.","PANGI");return;}
        var x=Ask("Inactividad","Minutos antes del cierre automático (1–180).",false,role=="student"?"10":"30");if(!int.TryParse(x,out var m)||m<1||m>180){MessageBox.Show("Usa entre 1 y 180 minutos.","PANGI");return;}
        await SendSelected("SET_IDLE",JsonSerializer.Serialize(new{role,minutes=m}));
    }
    async Task InstallApkSelected()
    {
        var s=Selected();
        if(s.Count==0){MessageBox.Show("Selecciona al menos una tablet.","PANGI");return;}
        var dlg=new OpenFileDialog{Filter="Aplicación Android (*.apk)|*.apk",Title="Seleccionar APK"};
        if(dlg.ShowDialog(this)!=true)return;
        string sha=Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(dlg.FileName))).ToLowerInvariant();
        int sent=0;ApkPushServer? lanServer=null;
        var local=s.Where(IsLocal).ToList();var away=s.Where(x=>!IsLocal(x)).ToList();

        if(local.Count>0){
            lanServer=new ApkPushServer(dlg.FileName);
            foreach(var d in local){
                string host=ApkPushServer.LocalAddressFor(d.Ip);
                string payload=JsonSerializer.Serialize(new{url=$"http://{host}:{lanServer.Port}/app.apk",sha256=sha});
                var rr=await commands!.Send(d,"INSTALL_APK",payload);if(rr.Item1)sent++;else footer.Text=$"{d.Name}: {rr.Item2}";
            }
        }

        if(away.Count>0){
            if(remote==null||!remote.Enabled)MessageBox.Show($"{away.Count} tablet(s) están fuera de LAN y no hay Relay HTTPS configurado.","PANGI");
            else{
                var upload=await remote.UploadFileAsync(dlg.FileName);
                if(!upload.Item1)MessageBox.Show("No se pudo subir el APK al Relay: "+upload.Item2,"PANGI");
                else foreach(var d in away){
                    string payload=JsonSerializer.Serialize(new{url=upload.Item3,sha256=upload.Item2});
                    var rr=await remote.SendCommandAsync(d.Id,"INSTALL_APK",payload);if(rr.Item1)sent++;else footer.Text=$"{d.Name}: {rr.Item2}";
                }
            }
        }
        if(lanServer!=null)_=Task.Run(async()=>{await Task.Delay(TimeSpan.FromMinutes(3));lanServer.Dispose();});
        MessageBox.Show($"Instalación enviada a {sent} tablet(s).\n\nSHA-256: {sha}\nLa instalación sólo se ejecuta en el usuario Device Owner.","PANGI");
    }

    async Task SendFileSelected()
    {
        var s=Selected();if(s.Count==0){MessageBox.Show("Selecciona al menos una tablet.","PANGI");return;}
        var dlg=new OpenFileDialog{Filter="Todos los archivos (*.*)|*.*",Title="Enviar archivo institucional"};if(dlg.ShowDialog(this)!=true)return;
        string sha=Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(dlg.FileName))).ToLowerInvariant();string name=Path.GetFileName(dlg.FileName);int sent=0;ApkPushServer? lanServer=null;
        var local=s.Where(IsLocal).ToList();var away=s.Where(x=>!IsLocal(x)).ToList();
        if(local.Count>0){
            lanServer=new ApkPushServer(dlg.FileName);
            foreach(var d in local){string host=ApkPushServer.LocalAddressFor(d.Ip);string payload=JsonSerializer.Serialize(new{url=$"http://{host}:{lanServer.Port}/file",sha256=sha,name});var rr=await commands!.Send(d,"DOWNLOAD_FILE",payload);if(rr.Item1)sent++;}
        }
        if(away.Count>0&&remote!=null&&remote.Enabled){
            var upload=await remote.UploadFileAsync(dlg.FileName);
            if(upload.Item1)foreach(var d in away){string payload=JsonSerializer.Serialize(new{url=upload.Item3,sha256=upload.Item2,name});var rr=await remote.SendCommandAsync(d.Id,"DOWNLOAD_FILE",payload);if(rr.Item1)sent++;}
            else footer.Text="No se pudo subir archivo al Relay: "+upload.Item2;
        }else if(away.Count>0)footer.Text=$"{away.Count} tablet(s) remotas requieren Relay HTTPS.";
        if(lanServer!=null)_=Task.Run(async()=>{await Task.Delay(TimeSpan.FromMinutes(3));lanServer.Dispose();});
        MessageBox.Show($"Archivo enviado a {sent} tablet(s). Se guardará en Descargas/PANGI dentro de la sesión.","PANGI");
    }

    async Task SetFps()
    {
        var x=Ask("Frecuencia de pantalla","Elige 0.5, 1, 2, 4 o 6 FPS. Usa 1–2 FPS para mosaicos grandes y 4–6 FPS para una tablet individual.",false,"2");
        if(string.IsNullOrWhiteSpace(x))return;
        if(!double.TryParse(x.Replace(',', '.'),System.Globalization.NumberStyles.Float,System.Globalization.CultureInfo.InvariantCulture,out var fps)
           || !(Math.Abs(fps-.5)<.01||Math.Abs(fps-1)<.01||Math.Abs(fps-2)<.01||Math.Abs(fps-4)<.01||Math.Abs(fps-6)<.01))
        {MessageBox.Show("Valores permitidos: 0.5, 1, 2, 4 o 6 FPS.","PANGI");return;}
        await SendSelected("SET_FPS",fps.ToString(System.Globalization.CultureInfo.InvariantCulture));
    }

    async Task PushRelayToSelected()
    {
        if(!relaySettings.Enabled){MessageBox.Show("Primero configura una URL HTTPS de Relay desde Configuración.","PANGI");return;}
        var s=Selected().Where(IsLocal).ToList();if(s.Count==0){MessageBox.Show("Selecciona tablets visibles por LAN para vincular el relay.","PANGI");return;}
        foreach(var d in s){var r=await commands!.Send(d,"SET_RELAY_URL",relaySettings.Url);if(!r.Item1){footer.Text=$"{d.Name}: {r.Item2}";return;}}
        MessageBox.Show($"Relay configurado en {s.Count} tablet(s). Las futuras sesiones temporales heredarán esta configuración.","PANGI");
    }
    void SelectOnline(){deviceGrid.SelectedItems.Clear();foreach(var d in devices.Where(x=>x.IsOnline))deviceGrid.SelectedItems.Add(d);}
    void OpenMosaic(){var chosen=Selected().Where(x=>x.IsOnline&&x.Screen&&!string.IsNullOrWhiteSpace(x.User)).ToList();if(chosen.Count==0)chosen=devices.Where(x=>x.IsOnline&&x.Screen&&!string.IsNullOrWhiteSpace(x.User)).ToList();if(chosen.Count==0){MessageBox.Show("No hay pantallas supervisadas disponibles.","PANGI");return;}new MosaicWindow(chosen,settings.Key,relaySettings.Url){Owner=this}.Show();}

    void ConfigDialog()
    {
        var k=Ask("Configuración","Clave técnica del establecimiento",true,settings.Key);if(string.IsNullOrWhiteSpace(k)||k.Length<10)return;
        var ru=Ask("Relay remoto","URL HTTPS del relay de PANGI. Déjala vacía para trabajar sólo por LAN.",false,relaySettings.Url)??relaySettings.Url;
        if(!string.IsNullOrWhiteSpace(ru)&&(!Uri.TryCreate(ru,UriKind.Absolute,out var u)||!u.Scheme.Equals("https",StringComparison.OrdinalIgnoreCase))){MessageBox.Show("El relay debe usar una URL HTTPS válida.","PANGI");return;}
        settings.Key=k;settings.Save();relaySettings.Url=ru.Trim();relaySettings.Save();Start();
    }

    string? Ask(string title,string help,bool password,string initial="")
    {
        var w=new Window{Title=title,Width=540,Height=285,ResizeMode=ResizeMode.NoResize,WindowStartupLocation=WindowStartupLocation.CenterOwner,Owner=this,Background=Canvas,FontFamily=new FontFamily("Segoe UI")};var p=new StackPanel{Margin=new Thickness(28)};p.Children.Add(TitleText(title));p.Children.Add(Hint(help));Control input;if(password){var q=new PasswordBox{FontSize=16,Padding=new Thickness(12),Password=initial,Background=Brushes.White};input=q;}else{var q=new TextBox{FontSize=16,Padding=new Thickness(12),Text=initial,Background=Brushes.White};input=q;}p.Children.Add(input);var row=new StackPanel{Orientation=Orientation.Horizontal,HorizontalAlignment=HorizontalAlignment.Right,Margin=new Thickness(0,18,0,0)};string? result=null;row.Children.Add(Action("Cancelar",(_,_)=>w.Close()));row.Children.Add(Action("Aceptar",(_,_)=>{result=input is PasswordBox pb?pb.Password:((TextBox)input).Text;w.DialogResult=true;},Blue));p.Children.Add(row);w.Content=p;w.ShowDialog();return result;
    }
}
