package cl.antumapu.aulacontrol;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

final class NetUtil {
    private NetUtil(){}
    static String ip(){try{Enumeration<NetworkInterface> n=NetworkInterface.getNetworkInterfaces();while(n.hasMoreElements()){NetworkInterface ni=n.nextElement();if(!ni.isUp()||ni.isLoopback())continue;Enumeration<InetAddress> a=ni.getInetAddresses();while(a.hasMoreElements()){InetAddress x=a.nextElement();if(x instanceof Inet4Address&&x.isSiteLocalAddress())return x.getHostAddress();}}}catch(Exception ignored){}return "0.0.0.0";}
    static List<InetAddress> broadcasts(){LinkedHashSet<InetAddress> out=new LinkedHashSet<>();try{out.add(InetAddress.getByName("255.255.255.255"));}catch(Exception ignored){}try{Enumeration<NetworkInterface> n=NetworkInterface.getNetworkInterfaces();while(n.hasMoreElements()){NetworkInterface ni=n.nextElement();if(!ni.isUp()||ni.isLoopback())continue;for(java.net.InterfaceAddress a:ni.getInterfaceAddresses())if(a.getBroadcast()!=null)out.add(a.getBroadcast());}}catch(Exception ignored){}return new ArrayList<>(out);}
}
