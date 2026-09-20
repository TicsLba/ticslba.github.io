using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace AulaControlTeacher;

public class ViewerWindow:Window
{
 readonly Device d; readonly string key;
 readonly Image img=new(){Stretch=Stretch.Uniform};
 readonly TextBlock state=new(){Foreground=new SolidColorBrush(Color.FromRgb(191,206,220)),FontWeight=FontWeights.SemiBold};
 readonly HttpClient http=new(){Timeout=TimeSpan.FromSeconds(2)};
 readonly DispatcherTimer timer=new(){Interval=TimeSpan.FromMilliseconds(850)};
 public ViewerWindow(Device x,string k){
  d=x;key=k;Title="Aula Móvil · "+d.Name;Width=940;Height=780;MinWidth=720;MinHeight=560;Background=new SolidColorBrush(Color.FromRgb(14,24,38));WindowStartupLocation=WindowStartupLocation.CenterOwner;
  var g=new Grid();g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(64)});g.RowDefinitions.Add(new RowDefinition());
  var h=new Grid{Margin=new Thickness(18,10,18,10)};h.ColumnDefinitions.Add(new ColumnDefinition());h.ColumnDefinitions.Add(new ColumnDefinition{Width=GridLength.Auto});
  var left=new StackPanel();left.Children.Add(new TextBlock{Text=d.Name,Foreground=Brushes.White,FontSize=18,FontWeight=FontWeights.Bold});left.Children.Add(new TextBlock{Text=$"{d.Person} · {d.RoleText}{(string.IsNullOrWhiteSpace(d.Course)?"":" · "+d.Course)}",Foreground=new SolidColorBrush(Color.FromRgb(169,185,201)),FontSize=12});h.Children.Add(left);Grid.SetColumn(state,1);state.VerticalAlignment=VerticalAlignment.Center;h.Children.Add(state);g.Children.Add(h);
  var b=new Border{Background=Brushes.Black,Margin=new Thickness(14),CornerRadius=new CornerRadius(14),BorderBrush=new SolidColorBrush(Color.FromRgb(44,62,80)),BorderThickness=new Thickness(1),Child=img};Grid.SetRow(b,1);g.Children.Add(b);Content=g;
  timer.Tick+=async(_,_)=>await Refresh();Loaded+=async(_,_)=>{timer.Start();await Refresh();};Closed+=(_,_)=>{timer.Stop();http.Dispose();};
 }
 async Task Refresh(){try{long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();using var q=new HttpRequestMessage(HttpMethod.Get,$"http://{d.Ip}:{d.ScreenPort}/screen.jpg");q.Headers.Add("X-Timestamp",ts.ToString());q.Headers.Add("X-Signature",AcCrypto.Hmac(key,$"SCREEN\n{ts}"));using var r=await http.SendAsync(q);if(!r.IsSuccessStatusCode){state.Text="Esperando señal…";return;}byte[] z=AcCrypto.Open(key,await r.Content.ReadAsByteArrayAsync());using var ms=new MemoryStream(z);var bi=new BitmapImage();bi.BeginInit();bi.CacheOption=BitmapCacheOption.OnLoad;bi.StreamSource=ms;bi.EndInit();bi.Freeze();img.Source=bi;state.Text="● EN VIVO · cifrado";}catch{state.Text="Reconectando…";}}
}

public class MosaicWindow:Window
{
 readonly List<Device> ds;readonly string key;readonly HttpClient http=new(){Timeout=TimeSpan.FromSeconds(2)};readonly DispatcherTimer timer=new(){Interval=TimeSpan.FromMilliseconds(1300)};readonly Dictionary<string,Image> imgs=new();readonly Dictionary<string,TextBlock> states=new();bool busy;
 public MosaicWindow(List<Device> d,string k){
  ds=d;key=k;Title="Aula Móvil · Mosaico del aula";Width=1320;Height=840;MinWidth=900;MinHeight=640;Background=new SolidColorBrush(Color.FromRgb(244,247,251));WindowStartupLocation=WindowStartupLocation.CenterOwner;
  var root=new Grid();root.RowDefinitions.Add(new RowDefinition{Height=new GridLength(72)});root.RowDefinitions.Add(new RowDefinition());
  var head=new Border{Background=new SolidColorBrush(Color.FromRgb(22,50,79)),Padding=new Thickness(20,13,20,13)};var hs=new StackPanel();hs.Children.Add(new TextBlock{Text="Mosaico del aula",Foreground=Brushes.White,FontSize=23,FontWeight=FontWeights.Bold});hs.Children.Add(new TextBlock{Text=$"{ds.Count} pantallas seleccionadas · supervisión en tiempo real",Foreground=new SolidColorBrush(Color.FromRgb(205,217,230)),FontSize=12});head.Child=hs;root.Children.Add(head);
  var wrap=new WrapPanel{Margin=new Thickness(10)};var scroll=new ScrollViewer{Content=wrap,VerticalScrollBarVisibility=ScrollBarVisibility.Auto};Grid.SetRow(scroll,1);root.Children.Add(scroll);Content=root;
  foreach(var x in ds){var border=new Border{Width=390,Height=300,Margin=new Thickness(8),Background=Brushes.White,CornerRadius=new CornerRadius(14),BorderBrush=new SolidColorBrush(Color.FromRgb(223,230,239)),BorderThickness=new Thickness(1)};var g=new Grid();g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(52)});g.RowDefinitions.Add(new RowDefinition());g.RowDefinitions.Add(new RowDefinition{Height=new GridLength(36)});var title=new TextBlock{Text=x.Name,Foreground=new SolidColorBrush(Color.FromRgb(23,32,51)),FontWeight=FontWeights.Bold,FontSize=14,Margin=new Thickness(12,10,12,0)};g.Children.Add(title);var imBox=new Border{Background=new SolidColorBrush(Color.FromRgb(14,24,38)),Margin=new Thickness(8,0,8,0),CornerRadius=new CornerRadius(10)};var im=new Image{Stretch=Stretch.Uniform};imBox.Child=im;Grid.SetRow(imBox,1);g.Children.Add(imBox);var s=new TextBlock{Text="Conectando…",Foreground=new SolidColorBrush(Color.FromRgb(102,112,133)),Margin=new Thickness(12,7,12,7),FontSize=11};Grid.SetRow(s,2);g.Children.Add(s);border.Child=g;wrap.Children.Add(border);imgs[x.Id]=im;states[x.Id]=s;border.Cursor=System.Windows.Input.Cursors.Hand;border.MouseLeftButtonDown+=(_,_)=>new ViewerWindow(x,key){Owner=this}.Show();}
  timer.Tick+=async(_,_)=>await Refresh();Loaded+=async(_,_)=>{timer.Start();await Refresh();};Closed+=(_,_)=>{timer.Stop();http.Dispose();};
 }
 async Task Refresh(){if(busy)return;busy=true;try{await Task.WhenAll(ds.Select(Fetch));}finally{busy=false;}}
 async Task Fetch(Device d){try{long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();using var q=new HttpRequestMessage(HttpMethod.Get,$"http://{d.Ip}:{d.ScreenPort}/screen.jpg");q.Headers.Add("X-Timestamp",ts.ToString());q.Headers.Add("X-Signature",AcCrypto.Hmac(key,$"SCREEN\n{ts}"));using var r=await http.SendAsync(q);if(!r.IsSuccessStatusCode){states[d.Id].Text="Esperando señal…";return;}byte[] z=AcCrypto.Open(key,await r.Content.ReadAsByteArrayAsync());using var ms=new MemoryStream(z);var bi=new BitmapImage();bi.BeginInit();bi.CacheOption=BitmapCacheOption.OnLoad;bi.DecodePixelWidth=360;bi.StreamSource=ms;bi.EndInit();bi.Freeze();imgs[d.Id].Source=bi;states[d.Id].Text=$"● En vivo · {d.Person}";}catch{states[d.Id].Text="Sin señal · reintentando";}}
}
