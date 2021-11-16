package com.example.fin;

import static android.content.Context.ALARM_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;
import static com.example.fin.MainActivity.swap;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;

class ReqBusList extends Thread{

    Dialog dlg;
    String endPoint = "http://openapi.changwon.go.kr/rest/bis/BusArrives/";
    String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";
    int stationId;
    BusInfo [] busArr;
    BusStopInfo [] busStopArr;
    BusRelative [] busRelativeArr;
    double stationLat;
    double stationLon;

//    public String searchStationNM(int id){
//        int top = busStopArr.length - 1;
//        int bot = 0;
//        int mid = 0;
//        while(bot <= top) {
//            mid = (bot + top) / 2;
//
//            if (busStopArr[mid].STATION_ID == id)
//                return busStopArr[mid].STATION_NM;
//            else if (busStopArr[mid].STATION_ID > id)
//                top = mid - 1;
//            else
//                bot = mid + 1;
//        }
//        return busStopArr[mid].STATION_NM;
//    }
    ReqBusList(int stationId, Dialog dlg, BusInfo [] busInfoArr, BusStopInfo [] busStopArr, double stationLat, double stationLon){
        this.stationId = stationId;
        this.dlg = dlg;
        this.busArr = busInfoArr;
        this.busStopArr = busStopArr;
        this.stationLat = stationLat;
        this.stationLon = stationLon;
    }
    @SuppressLint("ResourceType")
    public void run(){
        XmlPullParser xpp;
        XmlPullParserFactory factory;
        try {
            URL url = new URL(endPoint + "?serviceKey=" + key + "&station=" + stationId); //문자열로 된 요청 url을 URL객체로 생성
            InputStream is = url.openStream();
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            int eventType = xpp.getEventType();
            xpp.setInput(new InputStreamReader(is, "UTF-8"));
            LinearLayout.LayoutParams pm = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            String tagName = null;
            int busRelativeArrIndex = 0;
            String pastNM = "";
            while(eventType != XmlPullParser.END_DOCUMENT){

                //System.out.println(xpp.getText() + xpp.getName());
                tagName = xpp.getName();
                if(tagName == null){
                }
                else if(tagName.equals("rowCount")){
                    xpp.next();
                    busRelativeArr = new BusRelative[Integer.parseInt(xpp.getText())];
                    xpp.next();
                }
                else if(tagName.equals("row")){
                    Bus bus = new Bus();
                    Button button = new Button(dlg.getContext());
                    button.setLayoutParams(pm);

                    while(true){
                        xpp.next();
                        tagName = xpp.getName();
                        if(tagName != null){
                            if(tagName.equals("row")) {
                                if(pastNM.equals(bus.BUS_NM)){
                                    break;
                                }
                                pastNM = bus.BUS_NM;
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.bottomMargin = 5;
                                RelativeLayout relativeLayout = new RelativeLayout(dlg.getContext());
                                relativeLayout.setTag(bus);
                                relativeLayout.setLayoutParams(params);
                                // 버스 번호
                                TextView what = new TextView(dlg.getContext());
                                what.setTextSize(40);
                                what.setText(bus.BUS_NM);
                                what.setId(2);
                                switch (busArr[bus.index].ROUTE_COLOR){
                                    case 2:{
                                        what.setTextColor(Color.parseColor("#0055ff"));
                                        break;
                                    }
                                    case 3:
                                    case 1: {
                                        what.setTextColor(Color.parseColor("#00DD00"));
                                        break;
                                    }
                                    case 5:{
                                        what.setTextColor(Color.parseColor("#ff5500"));
                                        break;
                                    }
                                    case 6:{
                                        what.setTextColor(Color.parseColor("#ff0044"));
                                        break;
                                    }
                                    default:{
                                        what.setTextColor(Color.parseColor("#030303"));
                                        break;
                                    }
                                }
                                // 도착 시간
                                TextView when = new TextView(dlg.getContext());
                                when.setTextSize(25);
                                when.setTextColor(Color.parseColor("#131313"));
                                String wh;
                                if (bus.PREDICT_TRAV_TM == 0) {
                                    wh = "도착 정보 없음";
                                    when.setTextColor(Color.parseColor("#D3D3D3"));
                                    when.setTextSize(15);
                                }else if(bus.PREDICT_TRAV_TM / 60 == 0){
                                    wh = "곧 도착";
                                }else{
                                    wh = bus.PREDICT_TRAV_TM / 60 + "분 후 도착";
                                }
                                when.setText(wh);
                                // 출발 -> 종점
                                TextView how = new TextView(dlg.getContext());
                                how.setTextSize(14);
                                how.setTextColor(Color.parseColor("#838383"));
                                // 레이아웃 디자인
                                RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                );
                                param.addRule(RelativeLayout.ALIGN_PARENT_START, 1);
                                param.leftMargin = 15;
                                what.setLayoutParams(param);
                                param = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                );
                                param.addRule(RelativeLayout.ALIGN_BOTTOM, 2);
                                param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 2);
                                when.setLayoutParams(param);
                                param = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                );
                                param.addRule(RelativeLayout.BELOW, 2);
                                how.setLayoutParams(param);
                                relativeLayout.setOnClickListener(new View.OnClickListener() {
                                    @SuppressLint("MissingPermission")
                                    @Override
                                    public void onClick(View view) {
                                        Bus bus = (Bus)view.getTag();
                                        View dialogView = (View)View.inflate(view.getContext(), R.layout.dialog_alarm, null);
                                        AlertDialog.Builder dlg = new AlertDialog.Builder(view.getContext());
                                        dlg.setView(dialogView);
                                        dlg.setTitle("알람 설정");

                                        // 나가야 할 시간 계산
                                        LocationManager manager=(LocationManager) view.getContext().getSystemService(Context.LOCATION_SERVICE);
                                        MainActivity.GPSListener gpsListener = new MainActivity.GPSListener();
                                        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
                                        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
                                        Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                        LocationDistance ld = new LocationDistance();
                                        int time = (int) (ld.distance(lastLocation.getLatitude(), lastLocation.getLongitude(), stationLat, stationLon) / 5.0 * 1.8 * 3600);
                                        System.out.println(time);
                                        Calendar cal = Calendar.getInstance();
                                        cal.add(Calendar.SECOND, bus.PREDICT_TRAV_TM - time);
                                        TimePicker timePicker = (TimePicker)dialogView.findViewById(R.id.timePicker);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            timePicker.setHour(cal.get(Calendar.HOUR));
                                            timePicker.setMinute(cal.get(Calendar.MINUTE));
                                        }
                                        dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    cal.set(Calendar.HOUR, timePicker.getHour());
                                                    cal.set(Calendar.MINUTE, timePicker.getMinute());
                                                }else{
                                                    cal.set(Calendar.HOUR, timePicker.getCurrentHour());
                                                    cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                                                }
                                                // Receiver 설정
                                                Intent intent = new Intent(dlg.getContext(), AlarmReceiver.class);
                                                // state 값이 on 이면 알람시작, off 이면 중지
                                                intent.putExtra("state", "on");

                                                PendingIntent pendingIntent = PendingIntent.getBroadcast(dlg.getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                                                // 알람 설정
                                                AlarmManager alarmManager = (AlarmManager) dlg.getContext().getSystemService(ALARM_SERVICE);
                                                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
//                                                Toast.makeText(dialogView.getContext(),result, Toast.LENGTH_SHORT);
                                            }
                                        });
                                        dlg.setNegativeButton("취소", null);
                                        dlg.show();
                                    }
                                });

                                relativeLayout.addView(what);
                                relativeLayout.addView(when);
                                relativeLayout.addView(how);

                                ReqBusRouteList busRouteListthread = new ReqBusRouteList(bus.ROUTE_ID, how, bus.STATION_ORD);
                                busRouteListthread.run();
                                if(bus.PREDICT_TRAV_TM == 0){
                                    BusRelative busRelative = new BusRelative(relativeLayout, bus.PREDICT_TRAV_TM);
                                    busRelativeArr[busRelativeArrIndex++] = busRelative;
                                }else{
                                    Message message = handler.obtainMessage();
                                    message.obj = relativeLayout;
                                    message.what = 1;
                                    handler.sendMessage(message);
                                }
                                break;
                            }
                            switch(tagName) {
                                case "ROUTE_ID":{
                                    xpp.next();
                                    bus.ROUTE_ID=Integer.parseInt(xpp.getText());
                                    for(int i = 0; i < busArr.length; i++){
                                        if(busArr[i].ROUTE_ID == bus.ROUTE_ID){
                                            bus.index = i;
                                            bus.BUS_NM = busArr[i].ROUTE_NM;
                                            bus.STATION_CNT = busArr[i].STATION_CNT;
                                            break;
                                        }
                                    }
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
            handler.sendEmptyMessage(-1);
        } catch (XmlPullParserException | IOException protocolException) {
            protocolException.printStackTrace();
        }
    }
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            LinearLayout linearLayout = (LinearLayout) dlg.findViewById(R.id.busStopLinearLayout);
            if(msg.what != -1){
                RelativeLayout relativeLayout = (RelativeLayout)msg.obj;
                linearLayout.addView(relativeLayout);
                return;
            }
            for(int i = 0; !(busRelativeArr[i] == null); i++){
                linearLayout.addView(busRelativeArr[i].relativeLayout);
            }
        }
    };
    class Bus{
        int ROUTE_ID;       // 노선 ID
        String BUS_NM;          // 버스 번호
        short STATION_ORD;     // 노선에 대한 정류장 순번
        short PREDICT_TRAV_TM;  // 도착 예정 시간
        short LEFT_STATION;     // 남은 정류장 수
        int index;
        short STATION_CNT;      // 총 정류장 수
    }
}
class BusRelative{
    RelativeLayout relativeLayout;
    int id;
    BusRelative(RelativeLayout relativeLayout, int id){
        this.relativeLayout = relativeLayout;
        this.id = id;
    }
}
