using System.Windows;
namespace PangiConsole;
public partial class App:Application{
 protected override void OnStartup(StartupEventArgs e){
  base.OnStartup(e);
  if(!PrivacyNoticeWindow.Seen){var p=new PrivacyNoticeWindow();p.ShowDialog();}
  MainWindow=new TeacherConsoleWindow();
  MainWindow.Show();
 }
}
