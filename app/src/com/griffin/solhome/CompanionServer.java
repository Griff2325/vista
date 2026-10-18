package com.griffin.solhome;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.LongSupplier;

/** Bounded, single-worker LAN transport. No Android, filesystem routes, shell, CORS or logging. */
final class CompanionServer implements AutoCloseable {
    interface Commands { Reply run(String action,String value); }
    static final class Reply {
        final int status; final String message;
        Reply(int status,String message){this.status=status;this.message=message;}
    }
    static final long PAIR_MS=120000,IDLE_MS=1800000,MAX_MS=43200000;
    private final ServerSocket listener;
    private final byte[] page;
    private final Commands commands;
    private final SecureRandom random=new SecureRandom();
    private final LongSupplier clock;
    private final long started;
    private volatile boolean closed;
    private volatile Socket current;
    private String pin="",token="";
    private long pinUntil,lastUse,window;
    private int failures,requests;
    CompanionServer(InetAddress address,int port,byte[] page,Commands commands)throws IOException{
        this(address,port,page,commands,()->System.nanoTime()/1000000);
    }
    CompanionServer(InetAddress address,int port,byte[] page,Commands commands,LongSupplier clock)throws IOException{
        this.clock=clock;started=now();lastUse=started;this.page=page;this.commands=commands;listener=new ServerSocket();listener.setReuseAddress(true);
        listener.bind(new InetSocketAddress(address,port),4);listener.setSoTimeout(1000);
    }
    int port(){return listener.getLocalPort();}
    private long now(){return clock.getAsLong();}
    synchronized String pair(){
        if(closed)return "";
        token="";
        pin=String.format(Locale.US,"%06d",random.nextInt(1000000));pinUntil=now()+PAIR_MS;failures=0;lastUse=now();return pin;
    }
    synchronized String pairingStatus(){return !token.isEmpty()?"Phone connected":now()<pinUntil?"Code expires in "+((pinUntil-now()+999)/1000)+"s":"Code expired · generate a new code";}
    synchronized boolean expired(){long time=now();return time-lastUse>IDLE_MS||time-started>MAX_MS;}
    void serve(){
        try{while(!closed&&!expired())try{Socket socket=listener.accept();current=socket;try{socket.setSoTimeout(1500);handle(socket);}catch(Exception ignored){}finally{current=null;socket.close();}}catch(SocketTimeoutException idle){}
        }catch(IOException ignored){}finally{close();}
    }
    private void handle(Socket socket)throws IOException{
        long deadline=now()+2500;
        InputStream in=socket.getInputStream();String first=line(in,1024,deadline);String[] parts=first.split(" ");
        if(parts.length!=3||!parts[2].equals("HTTP/1.1")){send(socket,400,"Invalid request");return;}
        Map<String,String> headers=new HashMap<>();int size=first.length();
        while(true){String header=line(in,2048,deadline);size+=header.length()+2;if(size>8192){send(socket,431,"Headers too large");return;}if(header.isEmpty())break;
            int colon=header.indexOf(':');if(colon<1){send(socket,400,"Invalid headers");return;}String key=header.substring(0,colon).toLowerCase(Locale.ROOT);
            if(headers.put(key,header.substring(colon+1).trim())!=null){send(socket,400,"Duplicate header");return;}}
        String host=socket.getLocalAddress().getHostAddress()+":"+port();
        if(!host.equals(headers.get("host"))){send(socket,403,"Use the address shown on the TV");return;}
        if(expired()){send(socket,401,"Companion session expired");return;}
        String route=parts[1],method=parts[0];
        if(method.equals("GET")&&route.equals("/")){respond(socket,200,"text/html; charset=utf-8",page);return;}
        if(!route.equals("/api/pair")&&!route.equals("/api/command")&&!route.equals("/api/status")){send(socket,404,"Not found");return;}
        synchronized(this){long time=now();if(time-window>1000){window=time;requests=0;}if(++requests>18){send(socket,429,"Slow down");return;}}
        if(!method.equals("POST")||!("http://"+host).equals(headers.get("origin"))||!"application/x-www-form-urlencoded".equals(headers.get("content-type"))||headers.containsKey("transfer-encoding")){
            send(socket,403,"Use this TV's companion page");return;}
        int length;try{length=Integer.parseInt(headers.getOrDefault("content-length","-1"));}catch(NumberFormatException error){length=-1;}
        if(length<0||length>2048){send(socket,413,"Request too large");return;}
        byte[] body=new byte[length];for(int i=0;i<length;i++){if(now()>deadline)throw new IOException("Timeout");int b=in.read();if(b<0)throw new EOFException();body[i]=(byte)b;}
        Map<String,String> form=new HashMap<>();
        try{for(String pair:new String(body,StandardCharsets.UTF_8).split("&")){String[] kv=pair.split("=",2);if(kv.length==2&&form.put(URLDecoder.decode(kv[0],"UTF-8"),URLDecoder.decode(kv[1],"UTF-8"))!=null)throw new IllegalArgumentException();}}
        catch(IllegalArgumentException error){send(socket,400,"Invalid form");return;}
        if(route.equals("/api/pair")){
            synchronized(this){if(now()>pinUntil||failures>=5||pin.isEmpty()){send(socket,403,"Generate a new code on the TV");return;}
                if(!equal(pin,form.getOrDefault("code",""))){if(++failures>=5){pin="";pinUntil=0;}send(socket,403,"Code did not match");return;}
                byte[] bytes=new byte[32];random.nextBytes(bytes);StringBuilder value=new StringBuilder();for(byte b:bytes)value.append(String.format(Locale.US,"%02x",b&255));
                token=value.toString();pin="";pinUntil=0;lastUse=now();send(socket,200,token);return;}}
        synchronized(this){if(token.isEmpty()||!equal("Bearer "+token,headers.getOrDefault("authorization",""))){send(socket,401,"Pair with the TV again");return;}}
        Reply result;
        if(route.equals("/api/status"))result=commands.run("status","");
        else{
            String action=form.getOrDefault("action",""),value=form.getOrDefault("value","");
            if(!action.matches("home|key|search|app|panel|season|wildlife|spotify|disconnect")||value.length()>120||value.matches("(?s).*[\\p{Cntrl}].*")){send(socket,400,"Unsupported command");return;}
            result=commands.run(action,value);
            if(action.equals("disconnect"))synchronized(this){token="";}
        }
        // Revoked/expired requests never reach Commands. Only authenticated traffic keeps this alive.
        synchronized(this){lastUse=now();}send(socket,result.status,result.message);
    }
    static boolean equal(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    private String line(InputStream input,int limit,long deadline)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();while(bytes.size()<=limit){if(now()>deadline)throw new IOException("Timeout");int b=input.read();if(b<0)throw new EOFException();if(b==10){byte[] data=bytes.toByteArray();if(data.length<1||data[data.length-1]!=13)throw new IOException("Invalid newline");return new String(data,0,data.length-1,StandardCharsets.US_ASCII);}bytes.write(b);}throw new IOException("Line too long");
    }
    private void send(Socket socket,int status,String text)throws IOException{respond(socket,status,"text/plain; charset=utf-8",text.getBytes(StandardCharsets.UTF_8));}
    private void respond(Socket socket,int status,String type,byte[] bytes)throws IOException{
        String header="HTTP/1.1 "+status+" Result\r\nContent-Type: "+type+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; img-src data:; base-uri 'none'; form-action 'none'; frame-ancestors 'none'\r\n\r\n";
        OutputStream output=socket.getOutputStream();output.write(header.getBytes(StandardCharsets.US_ASCII));output.write(bytes);output.flush();
    }
    @Override public synchronized void close(){closed=true;pin="";token="";try{listener.close();}catch(IOException ignored){}Socket socket=current;if(socket!=null)try{socket.close();}catch(IOException ignored){}}
}
