package com.griffin.solhome;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;

public final class CompanionServerTest {
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static final class Fixture implements AutoCloseable {
        AtomicLong clock=new AtomicLong(10000);AtomicInteger commands=new AtomicInteger();
        CompanionServer server;Thread worker;String host,token;
        Fixture()throws Exception{server=new CompanionServer(InetAddress.getByName("127.0.0.1"),0,"page".getBytes(StandardCharsets.UTF_8),(a,v)->{commands.incrementAndGet();return new CompanionServer.Reply(200,a+":"+v);},clock::get);host="127.0.0.1:"+server.port();worker=new Thread(server::serve);worker.start();}
        String raw(String request)throws Exception{try(Socket socket=new Socket("127.0.0.1",server.port())){socket.setSoTimeout(3000);socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));ByteArrayOutputStream result=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int n;while((n=socket.getInputStream().read(bytes))!=-1){result.write(bytes,0,n);String partial=result.toString("UTF-8");int end=partial.indexOf("\r\n\r\n");if(end>=0){int marker=partial.indexOf("Content-Length: ")+16;int length=Integer.parseInt(partial.substring(marker,partial.indexOf("\r\n",marker)));if(result.size()>=end+4+length)break;}}return result.toString("UTF-8");}}
        String post(String route,String body,String origin,String bearer)throws Exception{return raw("POST /api/"+route+" HTTP/1.1\r\nHost: "+host+"\r\nOrigin: "+origin+"\r\nContent-Type: application/x-www-form-urlencoded\r\n"+(bearer==null?"":"Authorization: Bearer "+bearer+"\r\n")+"Content-Length: "+body.getBytes(StandardCharsets.UTF_8).length+"\r\n\r\n"+body);}
        String post(String route,String body)throws Exception{return post(route,body,"http://"+host,token);}
        void pair()throws Exception{String result=post("pair","code="+server.pair());check(result.startsWith("HTTP/1.1 200"),"pair works");token=result.split("\r\n\r\n",2)[1];check(token.matches("[a-f0-9]{64}"),"256-bit token");}
        public void close()throws Exception{server.close();worker.join(4000);check(!worker.isAlive(),"socket closes worker");}
    }
    public static void main(String[] args)throws Exception{
        try(Fixture f=new Fixture()){
            check(f.raw("GET / HTTP/1.1\r\nHost: "+f.host+"\r\n\r\n").endsWith("page"),"page served");
            check(f.post("command","action=home").contains("401 Result"),"auth required");
            check(f.commands.get()==0,"unauthorized no callback");f.pair();
            check(f.post("command","action=search&value=Snow%20day").endsWith("search:Snow day"),"bounded text delivered");
            check(f.post("command","action=shell&value=id").contains("400 Result"),"no arbitrary command");
            check(f.post("command","action=home","http://evil.example",f.token).contains("403 Result"),"origin defense");
            check(f.raw("GET / HTTP/1.1\r\nHost: evil.example\r\n\r\n").contains("403 Result"),"host defense");
            check(f.post("command","action=search&value=bad%0Aline").contains("400 Result"),"control characters rejected");
            check(f.post("command","action=home&action=app").contains("400 Result"),"duplicate fields rejected");
            check(f.post("command","action=search&value="+"a".repeat(121)).contains("400 Result"),"text length bound");
            check(f.post("disconnect","action=disconnect").contains("404 Result"),"only fixed routes");
            check(f.post("command","action=disconnect").contains("200 Result"),"disconnect accepted");
            check(f.post("status","").contains("401 Result"),"disconnect revokes");
        }
        try(Fixture f=new Fixture()){
            String pin=f.server.pair();String wrong=pin.equals("000000")?"111111":"000000";
            for(int i=0;i<5;i++)check(f.post("pair","code="+wrong).contains("403 Result"),"incorrect PIN");
            check(f.post("pair","code="+pin).contains("403 Result"),"five attempts invalidate PIN");
            f.pair();String old=f.token;String newPin=f.server.pair();
            check(f.post("status","").contains("401 Result"),"new code revokes old phone");
            f.clock.addAndGet(CompanionServer.PAIR_MS+1);
            check(f.post("pair","code="+newPin).contains("403 Result"),"PIN expiry enforced");
            check(!old.isEmpty(),"initial token");
        }
        try(Fixture f=new Fixture()){
            f.pair();f.clock.addAndGet(CompanionServer.IDLE_MS+1);check(f.server.expired(),"idle expiry");
        }
        try(Fixture f=new Fixture()){
            f.pair();for(int i=0;i<CompanionServer.MAX_MS/600000;i++){f.clock.addAndGet(600000);if(!f.server.expired())check(f.post("status","").contains("200 Result"),"active sessions refresh idle");}
            f.clock.incrementAndGet();check(f.server.expired(),"absolute session bound");
        }
        try(Fixture f=new Fixture()){
            check(f.raw("GET /../../etc/passwd HTTP/1.1\r\nHost: "+f.host+"\r\n\r\n").contains("404 Result"),"no file serving");
            check(f.raw("POST /api/pair HTTP/1.1\r\nHost: "+f.host+"\r\nHost: "+f.host+"\r\n\r\n").contains("400 Result"),"ambiguous headers rejected");
            f.pair();for(int i=0;i<20;i++)f.post("status","");check(f.post("status","").contains("429 Result"),"rate bounded");
        }
        System.out.println("CompanionServerTest passed");
    }
}
