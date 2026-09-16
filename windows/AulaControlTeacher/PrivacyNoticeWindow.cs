using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace AulaControlTeacher;

public class PrivacyNoticeWindow:Window
{
 static readonly Brush Navy=new SolidColorBrush(Color.FromRgb(22,50,79));
 static readonly Brush Blue=new SolidColorBrush(Color.FromRgb(53,104,242));
 static readonly Brush Teal=new SolidColorBrush(Color.FromRgb(36,167,142));
 static readonly Brush Muted=new SolidColorBrush(Color.FromRgb(102,112,133));
 static readonly Brush Bg=new SolidColorBrush(Color.FromRgb(244,247,251));
 static readonly Brush Line=new SolidColorBrush(Color.FromRgb(223,230,239));
 public static string FlagFile{get{var d=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"AulaControl");Directory.CreateDirectory(d);return Path.Combine(d,"privacy-notice-tablet-escolar-2.0.ok");}}
 public static bool Seen=>File.Exists(FlagFile);
 public PrivacyNoticeWindow(){Title="Tablet Escolar · Privacidad y seguridad";Width=760;Height=720;MinWidth=680;MinHeight=620;WindowStartupLocation=WindowStartupLocation.CenterScreen;ResizeMode=ResizeMode.CanResize;Background=Bg;FontFamily=new FontFamily("Segoe UI");Build();}
 TextBlock T(string s,double z,bool bold=false,Brush? c=null)=>new(){Text=s,FontSize=z,FontWeight=bold?FontWeights.SemiBold:FontWeights.Normal,Foreground=c??Brushes.Black,TextWrapping=TextWrapping.Wrap,LineHeight=z*1.45};
 Border Card(UIElement c)=>new(){Background=Brushes.White,CornerRadius=new CornerRadius(18),Padding=new Thickness(24),BorderBrush=Line,BorderThickness=new Thickness(1),Child=c};
 void Add(StackPanel p,string h,string b){var x=T(h,15,true,Navy);x.Margin=new Thickness(0,10,0,2);p.Children.Add(x);p.Children.Add(T(b,14,false,Muted));}
 void Build(){
  var sc=new ScrollViewer{VerticalScrollBarVisibility=ScrollBarVisibility.Auto};var root=new StackPanel{Margin=new Thickness(30)};sc.Content=root;
  var brand=new StackPanel{Orientation=Orientation.Horizontal};
  brand.Children.Add(new Border{Width=52,Height=52,CornerRadius=new CornerRadius(15),Background=Blue,Child=new TextBlock{Text="TE",Foreground=Brushes.White,FontSize=19,FontWeight=FontWeights.Bold,HorizontalAlignment=HorizontalAlignment.Center,VerticalAlignment=VerticalAlignment.Center}});
  var names=new StackPanel{Margin=new Thickness(14,0,0,0),VerticalAlignment=VerticalAlignment.Center};names.Children.Add(T("Tablet Escolar",30,true,Navy));names.Children.Add(T("Privacidad y seguridad de aula",16,true,Blue));brand.Children.Add(names);root.Children.Add(brand);
  var intro=T("La consola docente y las tablets administradas trabajan dentro de la red institucional para apoyar la gestión del aula sin convertir la supervisión en vigilancia histórica.",15,false,Muted);intro.Margin=new Thickness(0,18,0,14);root.Children.Add(intro);
  var p=new StackPanel();p.Children.Add(T("Principios de funcionamiento",23,true,Navy));
  Add(p,"✓ Supervisión en tiempo real","La consola puede mostrar la pantalla de una sesión activa mientras ésta se encuentra en uso. No crea grabaciones históricas por defecto.");
  Add(p,"✓ Sin captura de credenciales","No registra contraseñas personales, PIN, tokens de autenticación ni pulsaciones de teclado.");
  Add(p,"✓ Sesiones temporales","Estudiantes y profesores utilizan usuarios Android temporales para reducir la permanencia de cuentas, cookies y datos personales entre usuarios.");
  Add(p,"✓ Identidad mínima","Nombre, rol y curso se utilizan sólo para identificar la sesión activa en la consola del docente.");
  Add(p,"✓ Gestión institucional","La consola puede enviar mensajes, abrir enlaces, activar modo atención y cerrar sesiones en tablets administradas por el establecimiento.");
  Add(p,"✓ Clave técnica protegida","La clave técnica de la consola se almacena protegida por Windows y se utiliza para autenticar y cifrar la comunicación local.");
  var note=T("Tablet Escolar está diseñado para supervisión en tiempo real, no vigilancia histórica.",14,true,Teal);note.Margin=new Thickness(0,16,0,0);p.Children.Add(note);root.Children.Add(Card(p));
  var b=new Button{Content="Entendido · abrir consola",Background=Blue,Foreground=Brushes.White,FontSize=15,FontWeight=FontWeights.SemiBold,Padding=new Thickness(18,12,18,12),BorderThickness=new Thickness(0),Margin=new Thickness(0,18,0,6),HorizontalAlignment=HorizontalAlignment.Stretch};
  b.Click+=(_,_)=>{File.WriteAllText(FlagFile,"Tablet Escolar 2.0 privacy notice acknowledged");DialogResult=true;};root.Children.Add(b);
  var dev=T("Tablet Escolar 2.0 · Desarrollado por Eduardo Pérez González",12,false,Muted);dev.TextAlignment=TextAlignment.Center;dev.Margin=new Thickness(0,8,0,12);root.Children.Add(dev);Content=sc;
 }
}
