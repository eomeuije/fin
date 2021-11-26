package com.example.fin;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

class ReqBusThread extends Thread{

    Dialog dlg;
    String endPoint = "http://openapi.changwon.go.kr/rest/bis/BusArrives/";
    String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";
    int stationId;
    BusInfo [] busArr;
    BusStopInfo [] busStopArr;
    BusRelative [] busRelativeArr;
    double stationLat;
    double stationLon;

    ReqBusThread(int stationId, Dialog dlg, BusInfo [] busInfoArr, BusStopInfo [] busStopArr, double stationLat, double stationLon){
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
                                RelativeLayout relativeLayout = new RelativeLayout(dlg.getContext());
                                relativeLayout.setTag(bus);
                                params.bottomMargin = 1;
                                relativeLayout.setBackground(ContextCompat.getDrawable(dlg.getContext(), R.drawable.botbodclr));
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
                                ReqBusRouteList busRouteList = new ReqBusRouteList(bus.ROUTE_ID);
                                bus.busRouteArr = busRouteList.getBusRouteArr();
                                int turIndex;
                                for(turIndex = 0; !bus.busRouteArr[turIndex].TUR; turIndex++){}
                                String result;
                                if(bus.STATION_ORD <= turIndex){
                                    result = bus.busRouteArr[0].STATION_NM + "→" + bus.busRouteArr[turIndex].STATION_NM;
                                }else{
                                    result = bus.busRouteArr[turIndex].STATION_NM + "→" + bus.busRouteArr[0].STATION_NM;
                                }
                                TextView how = new TextView(dlg.getContext());
                                how.setText(result);
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
                                        CreateAlarmDialog.create(view, (Bus)view.getTag(), stationLat, stationLon);
                                    }
                                });

                                relativeLayout.addView(what);
                                relativeLayout.addView(when);
                                relativeLayout.addView(how);


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
                                    int low = 0;
                                    int high = busArr.length - 1;
                                    int mid;
                                    while(low <= high) {
                                        mid = (low + high) / 2;
                                        if (busArr[mid].ROUTE_ID == bus.ROUTE_ID) {
                                            bus.index = mid;
                                            bus.BUS_NM = busArr[mid].ROUTE_NM;
                                            bus.STATION_CNT = busArr[mid].STATION_CNT;
                                            break;
                                        }
                                        else if (busArr[mid].ROUTE_ID > bus.ROUTE_ID) {
                                            high = mid - 1;
                                        }
                                        else {
                                            low = mid + 1;
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
            is.close();
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
            for(int i = 0; i < busRelativeArr.length && !(busRelativeArr[i] == null); i++){
                linearLayout.addView(busRelativeArr[i].relativeLayout);
            }
        }
    };
}
class BusRelative{
    RelativeLayout relativeLayout;
    int id;
    BusRelative(RelativeLayout relativeLayout, int id){
        this.relativeLayout = relativeLayout;
        this.id = id;
    }
}
class Bus{
    int ROUTE_ID;       // 노선 ID
    String BUS_NM;          // 버스 번호
    short STATION_ORD;     // 노선에 대한 정류장 순번
    short PREDICT_TRAV_TM;  // 도착 예정 시간
    short LEFT_STATION;     // 남은 정류장 수
    int index;
    short STATION_CNT;      // 총 정류장 수
    BusRoute [] busRouteArr;
}