using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;

namespace PangiConsole;

sealed class ApkPushServer : IDisposable
{
    readonly string file;
    readonly TcpListener listener;
    readonly CancellationTokenSource cts=new();
    readonly Task loop;
    public int Port { get; }

    public ApkPushServer(string filePath)
    {
        file=filePath;
        listener=new TcpListener(IPAddress.Any,0);
        listener.Start();
        Port=((IPEndPoint)listener.LocalEndpoint).Port;
        loop=Task.Run(ServeLoop);
    }

    async Task ServeLoop()
    {
        while(!cts.IsCancellationRequested)
        {
            try
            {
                var client=await listener.AcceptTcpClientAsync(cts.Token);
                _=Task.Run(()=>Serve(client),cts.Token);
            }
            catch(OperationCanceledException){break;}
            catch{if(cts.IsCancellationRequested)break;}
        }
    }

    async Task Serve(TcpClient client)
    {
        using(client)
        {
            try
            {
                using var stream=client.GetStream();
                using var reader=new StreamReader(stream,Encoding.ASCII,false,4096,true);
                var request=await reader.ReadLineAsync();
                if(string.IsNullOrWhiteSpace(request)||!request.StartsWith("GET ",StringComparison.OrdinalIgnoreCase))return;
                string? line;while(!string.IsNullOrEmpty(line=await reader.ReadLineAsync())){}
                var fi=new FileInfo(file);
                var header=$"HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: {fi.Length}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n";
                var hb=Encoding.ASCII.GetBytes(header);
                await stream.WriteAsync(hb);
                using var fs=File.OpenRead(file);
                await fs.CopyToAsync(stream);
                await stream.FlushAsync();
            }
            catch{}
        }
    }

    public static string LocalAddressFor(string remoteIp)
    {
        try
        {
            using var s=new Socket(AddressFamily.InterNetwork,SocketType.Dgram,ProtocolType.Udp);
            s.Connect(remoteIp,9);
            return ((IPEndPoint)s.LocalEndPoint!).Address.ToString();
        }
        catch
        {
            return Dns.GetHostEntry(Dns.GetHostName()).AddressList.FirstOrDefault(x=>x.AddressFamily==AddressFamily.InterNetwork)?.ToString()??"127.0.0.1";
        }
    }

    public void Dispose()
    {
        try{cts.Cancel();}catch{}
        try{listener.Stop();}catch{}
        cts.Dispose();
    }
}
