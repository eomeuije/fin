package com.example.fin;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;

public class WeekAlarmReceiver extends BroadcastReceiver {
    String fName = "AlarmList.csv";
    int time = 0;
    Context context;
    Intent intent;

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
        String[] alarmList;
        try {
            InputStream inFs = context.openFileInput(fName);
            byte[] txt = new byte[50000];
            inFs.read(txt);
            String str = (new String(txt)).trim();
            alarmList = str.split("\n");
            inFs.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String id = intent.getStringExtra("id");
        String[] alarmInfo = CreateAlarmDialog.getLine(id, alarmList);
        // 알람이 삭제 됐을 때 바로 종료
        if(alarmInfo == null){
            return;
        }
        // 지금 요일이 알람 요일인지 확인, 아니면 종료
        Calendar cal = Calendar.getInstance();
        if(alarmInfo[cal.get(Calendar.DAY_OF_WEEK) + 6].equals("false")){
            return;
        }
        LocationDistance ld;
        double myLat, myLon;
        try {
            // 내 위치 가져오기
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            MainActivity.GPSListener gpsListener = new MainActivity.GPSListener();
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
            Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            ld = new LocationDistance();

            myLat = lastLocation.getLatitude();
            myLon = lastLocation.getLongitude();
        }catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // 내 위치와 정류장 위치가 2km 이상 차이날때 알람 종료
        if(ld.distance(myLat, myLon, Double.parseDouble(alarmInfo[3]), Double.parseDouble(alarmInfo[4])) >= 2){
            return;
        }
        time = (int) (ld.distance(myLat, myLon,
                Double.parseDouble(alarmInfo[3]), Double.parseDouble(alarmInfo[4])) / 5.0 * 1.8 * 3600);
        ReqBusThread thread = new ReqBusThread(Integer.parseInt(alarmInfo[5]), Integer.parseInt(alarmInfo[6]));
        thread.setDaemon(true);
        thread.start();
    }
    class ReqBusThread extends Thread{

        String endPoint = "http://openapi.changwon.go.kr/rest/bis/ArriveInfo/";
        String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";
        int stationId;
        int routeId;

        ReqBusThread(int stationId, int routeId){
            this.stationId = stationId;
            this.routeId = routeId;
        }
        @SuppressLint("ResourceType")
        public void run(){
            XmlPullParser xpp;
            XmlPullParserFactory factory;
            try {
                URL url = new URL(endPoint + "?serviceKey=" + key + "&station=" + stationId + "&route=" + routeId); //문자열로 된 요청 url을 URL객체로 생성
                InputStream is = url.openStream();
                factory = XmlPullParserFactory.newInstance();
                xpp = factory.newPullParser();
                int eventType = xpp.getEventType();
                xpp.setInput(new InputStreamReader(is, "UTF-8"));

                String tagName = null;
                while(eventType != XmlPullParser.END_DOCUMENT){

                    tagName = xpp.getName();
                    if(tagName == null){
                    }
                    else if(tagName.equals("row")){
                        Bus bus = new Bus();

                        while(true){
                            xpp.next();
                            tagName = xpp.getName();
                            if(tagName != null){
                                if(tagName.equals("row")) {
                                    Message message = handler.obtainMessage();
                                    message.obj = bus;
                                    message.what = 1;
                                    handler.sendMessage(message);
                                    break;
                                }
                                switch(tagName) {
                                    case "ROUTE_ID":{
                                        xpp.next();
                                        bus.ROUTE_ID=Integer.parseInt(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "STATION_ORD":{
                                        xpp.next();
                                        bus.STATION_ORD= Short.parseShort(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "PREDICT_TRAV_TM":{
                                        xpp.next();
                                        bus.PREDICT_TRAV_TM=Short.parseShort(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "LEFT_STATION":{
                                        xpp.next();
                                        bus.LEFT_STATION=Short.parseShort(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    eventType = xpp.next();
                }
                is.close();
            } catch (XmlPullParserException | IOException protocolException) {
                protocolException.printStackTrace();
            }
        }
        class ReqBRL extends Thread{
            Bus bus;
            ReqBRL(Bus bus){
                this.bus = bus;
            }
            @Override
            public void run() {
                super.run();
                ReqBusRouteList reqBRL = new ReqBusRouteList(bus.ROUTE_ID);
                bus.busRouteArr = reqBRL.getBusRouteArr();

                Message message = handler.obtainMessage();
                message.obj = bus;
                message.what = 2;
                handler.sendMessage(message);
            }
        }
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                Bus bus = (Bus)msg.obj;
                if(msg.what == 1){
                    time = bus.PREDICT_TRAV_TM - time;
                    if(time < 0){       // 0보다 작으면 노선 실시간 정보를 가져와 배차시간 예상
                        ReqBRL reqBRL = new ReqBRL(bus);
                        reqBRL.setDaemon(true);
                        reqBRL.start();
                        return;
                    }
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.SECOND, time);
                    //알람 설정
                    CreateAlarmDialog.startAlarm(context, cal
                            , intent.getStringExtra("stationNM"), intent.getStringExtra("busNM"));
                }
                else if(msg.what == 2){
                    time += BusRoute.getBusTerm(bus.busRouteArr, bus.STATION_ORD);
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.SECOND, time);
                    // 알람 설정
                    CreateAlarmDialog.startAlarm(context, cal
                            , intent.getStringExtra("stationNM"), intent.getStringExtra("busNM"));
                }
            }
        };
    }
}
