using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace AulaControlTeacher;

public class PrivacyNoticeWindow:Window
{
 static readonly Brush Navy=new SolidColorBrush(Color.FromRgb(23,43,77));
 static readonly Brush Blue=new SolidColorBrush(Color.FromRgb(49,89,255));
 static readonly Brush Teal=new SolidColorBrush(Color.FromRgb(32,199,164));
 static readonly Brush Muted=new SolidColorBrush(Color.FromRgb(102,112,133));
 static readonly Brush Bg=new SolidColorBrush(Color.FromRgb(244,247,251));
 static readonly Brush Line=new SolidColorBrush(Color.FromRgb(221,228,239));
 public static string FlagFile{get{var d=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"AulaMovil");Directory.CreateDirectory(d);return Path.Combine(d,"privacy-notice-2.1.ok");}}
 public static bool Seen=>File.Exists(FlagFile);
 public PrivacyNoticeWindow(){Title="Aula Móvil · Privacidad, responsabilidad y seguridad";Width=780;Height=760;MinWidth=680;MinHeight=640;WindowStartupLocation=WindowStartupLocation.CenterScreen;ResizeMode=ResizeMode.CanResize;Background=Bg;FontFamily=new FontFamily("Segoe UI");Build();}
 TextBlock T(string s,double z,bool bold=false,Brush? c=null)=>new(){Text=s,FontSize=z,FontWeight=bold?FontWeights.SemiBold:FontWeights.Normal,Foreground=c??Brushes.Black,TextWrapping=TextWrapping.Wrap,LineHeight=z*1.45};
 Border Card(UIElement c)=>new(){Background=Brushes.White,CornerRadius=new CornerRadius(18),Padding=new Thickness(24),BorderBrush=Line,BorderThickness=new Thickness(1),Child=c};
 void Add(StackPanel p,string h,string b){var x=T(h,15,true,Navy);x.Margin=new Thickness(0,10,0,2);p.Children.Add(x);p.Children.Add(T(b,14,false,Muted));}
 void Build(){
  var sc=new ScrollViewer{VerticalScrollBarVisibility=ScrollBarVisibility.Auto};var root=new StackPanel{Margin=new Thickness(30)};sc.Content=root;
  var brand=new StackPanel{Orientation=Orientation.Horizontal};
  brand.Children.Add(new Border{Width=52,Height=52,CornerRadius=new CornerRadius(15),Background=Blue,Child=new TextBlock{Text="TE",Foreground=Brushes.White,FontSize=19,FontWeight=FontWeights.Bold,HorizontalAlignment=HorizontalAlignment.Center,VerticalAlignment=VerticalAlignment.Center}});
  var names=new StackPanel{Margin=new Thickness(14,0,0,0),VerticalAlignment=VerticalAlignment.Center};names.Children.Add(T("Aula Móvil",30,true,Navy));names.Children.Add(T("Gestión, informes y recuperación",16,true,Blue));brand.Children.Add(names);root.Children.Add(brand);
  var intro=T("Aula Móvil administra equipos institucionales, identifica al responsable de cada sesión y genera informes de uso. La supervisión de pantalla sigue siendo en tiempo real y no crea grabaciones históricas por defecto.",15,false,Muted);intro.Margin=new Thickness(0,18,0,14);root.Children.Add(intro);
  var p=new StackPanel();p.Children.Add(T("Qué registra la versión 2.1",23,true,Navy));
  Add(p,"✓ Responsabilidad de uso","Registra nombre declarado, rol, curso cuando corresponda, tablet utilizada, fecha/hora y duración aproximada para construir trazabilidad e informes por responsable y por dispositivo.");
  Add(p,"✓ Telemetría técnica","Puede conservar batería, estado de administración, última conexión y métricas agregadas necesarias para operación y mantenimiento.");
  Add(p,"✓ Uso de aplicaciones, de forma agregada","Cuando Android permite acceso de uso, el sistema puede contabilizar tiempo por aplicación. No necesita guardar búsquedas, páginas web, documentos abiertos ni contenido escrito.");
  Add(p,"✓ Ubicación institucional","Cuando la política del equipo lo habilita, puede registrar la ubicación de la tablet con fecha y precisión. En Modo pérdida puede aumentar la frecuencia de actualización para facilitar recuperación del equipo.");
  Add(p,"✓ Supervisión en tiempo real","La consola puede mostrar la pantalla de una sesión activa. No guarda grabaciones ni capturas históricas por defecto.");
  Add(p,"✓ Sin captura de credenciales","No registra contraseñas personales, PIN, tokens de autenticación ni pulsaciones de teclado.");
  Add(p,"✓ Sesiones temporales","Estudiantes y profesores utilizan usuarios Android temporales para reducir la permanencia de cuentas, cookies y datos personales entre responsables sucesivos.");
  Add(p,"✓ Comunicación protegida","La clave técnica se protege en Windows y se utiliza para autenticar y cifrar telemetría y comandos. El relay remoto recibe payloads cifrados y no la pantalla en vivo.");
  var note=T("Principio operativo: trazabilidad del uso institucional sin keylogging, sin captura de credenciales y sin vigilancia histórica de pantalla.",14,true,Teal);note.Margin=new Thickness(0,16,0,0);p.Children.Add(note);root.Children.Add(Card(p));
  var b=new Button{Content="Entendido · abrir consola",Background=Blue,Foreground=Brushes.White,FontSize=15,FontWeight=FontWeights.SemiBold,Padding=new Thickness(18,12,18,12),BorderThickness=new Thickness(0),Margin=new Thickness(0,18,0,6),HorizontalAlignment=HorizontalAlignment.Stretch};
  b.Click+=(_,_)=>{File.WriteAllText(FlagFile,"Aula Móvil 2.1 privacy notice acknowledged");DialogResult=true;};root.Children.Add(b);
  var dev=T("Aula Móvil 2.1 · Gestión institucional de tablets",12,false,Muted);dev.TextAlignment=TextAlignment.Center;dev.Margin=new Thickness(0,8,0,12);root.Children.Add(dev);Content=sc;
 }
}
