package com.example.fin;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class BusSearchThread extends Thread {

    LinearLayout baseLayout;
    int busId;
    BusStopInfo [] busStopArr;
    BusInfo [] busInfoArr;

    BusSearchThread(LinearLayout linearLayout, int busId, BusStopInfo [] busStopArr, BusInfo [] busInfoArr) {
        this.baseLayout = linearLayout;
        this.busId = busId;
        this.busStopArr = busStopArr;
        this.busInfoArr = busInfoArr;
    }

    @Override
    public void run() {
        super.run();

        ReqBusRouteList busRouteList = new ReqBusRouteList(busId);
        BusRoute[] busRouteArr = busRouteList.getBusRouteArr();
        Message message = handler.obtainMessage();
        message.obj = busRouteArr;
        message.what = 1;
        handler.sendMessage(message);
    }

    final Handler handler = new Handler() {
        @SuppressLint("ResourceType")
        public void handleMessage(Message msg) {
            BusRoute [] busRouteArr = (BusRoute[]) msg.obj;
            for(int i = 0; i < busRouteArr.length; i++){
                RelativeLayout relativeLayout = new RelativeLayout(baseLayout.getContext());
                RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                relativeLayout.setLayoutParams(param);
                param.bottomMargin = 1;
                relativeLayout.setBackground(ContextCompat.getDrawable(baseLayout.getContext(), R.drawable.botbodclr));
                // 현재 위치를 표시할 버스 아이콘
                TextView icon = new TextView(baseLayout.getContext());
                param = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                param.addRule(RelativeLayout.ALIGN_PARENT_START);
                icon.setLayoutParams(param);
                icon.setTextSize(40);
                icon.setId(2);
                icon.setCompoundDrawablesWithIntrinsicBounds(R.drawable.busicon, 0, 0, 0);
                if(busRouteArr[i].PLATE_NO.equals("null")){
                    icon.setVisibility(TextView.INVISIBLE);
                }
                // 정류장 이름
                TextView what = new TextView(baseLayout.getContext());
                param = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                param.addRule(RelativeLayout.END_OF, 2);
                param.addRule(RelativeLayout.ALIGN_BASELINE, 2);
                param.leftMargin = 10;
                what.setLayoutParams(param);
                what.setTextSize(25);
                what.setTextColor(Color.parseColor("#131313"));
                what.setText(busRouteArr[i].STATION_NM);
                // 회차지 정보
                TextView tur = new TextView(baseLayout.getContext());
                param = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                param.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                tur.setLayoutParams(param);
                tur.setTextSize(20);
                tur.setText("회차지");
                tur.setTextColor(Color.parseColor("#131313"));
                tur.setId(3);
                if(!busRouteArr[i].TUR){
                    tur.setVisibility(TextView.INVISIBLE);
                }
                // 정류장 ID
                TextView idView = new TextView(baseLayout.getContext());
                param = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                param.addRule(RelativeLayout.BELOW, 3);
                idView.setLayoutParams(param);
                idView.setTextSize(15);
                idView.setText(Integer.toString(busRouteArr[i].STATION_ID));


                relativeLayout.addView(icon);
                relativeLayout.addView(what);
                relativeLayout.addView(tur);
                relativeLayout.addView(idView);
                relativeLayout.setTag(busRouteArr[i]);
                relativeLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BusRoute busRoute = (BusRoute) view.getTag();
                        BusSearchStopThread bSST = new BusSearchStopThread(busRoute.STATION_ID, busRoute.ROUTE_ID, view, busRouteArr, busInfoArr);
                        bSST.setDaemon(true);
                        bSST.start();
                    }
                });
                baseLayout.addView(relativeLayout);
            }
        }
    };
    class BusSearchStopThread extends Thread{
        int stationId;
        int routeId;
        View view;
        BusRoute [] busRouteArr;
        BusInfo [] busInfoArr;

        BusSearchStopThread(int stationId, int routeId, View view, BusRoute [] busRouteArr, BusInfo [] busInfoArr){
            this.stationId = stationId;
            this.routeId = routeId;
            this.view = view;
            this.busRouteArr = busRouteArr;
            this.busInfoArr = busInfoArr;
        }

        @Override
        public void run() {
            super.run();
            Bus bus = ReqBusClassSearch.getBus(stationId, routeId, busInfoArr);
            Message message = handlerTime.obtainMessage();
            if (bus != null) {
                bus.busRouteArr = busRouteArr;
            }
            message.obj = bus;
            message.what = 1;
            handlerTime.sendMessage(message);
        }
        final Handler handlerTime = new Handler(){
            @SuppressLint("MissingPermission")
            @Override
            public void handleMessage(@NonNull Message msg) {
                int low = 0;
                if(busStopArr == null){
                    return;
                }
                int high = busStopArr.length - 1;
                int mid = 0;
                while(low <= high) {
                    mid = (low + high) / 2;
                    if (busStopArr[mid].STATION_ID == stationId) {
                        break;
                    }
                    else if (busStopArr[mid].STATION_ID > stationId) {
                        high = mid - 1;
                    }
                    else {
                        low = mid + 1;
                    }
                }
                CreateAlarmDialog.create(view, (Bus)msg.obj, busStopArr[mid].LOCAL_Y, busStopArr[mid].LOCAL_X);
            }
        };
    }
    static class ReqBusClassSearch{
        static String endPoint = "http://openapi.changwon.go.kr/rest/bis/BusArrives/";
        static String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";

        public static Bus getBus(int stationId, int routeId, BusInfo [] busArr){
            XmlPullParser xpp;
            XmlPullParserFactory factory;
            try {
                URL url = new URL(endPoint + "?serviceKey=" + key + "&station=" + stationId); //문자열로 된 요청 url을 URL객체로 생성
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
                        Boolean isThis = false;
                        while(true){
                            xpp.next();
                            tagName = xpp.getName();
                            if(tagName != null){
                                if(tagName.equals("row")) {
                                    if(isThis){
                                        return bus;
                                    }
                                    break;
                                }
                                switch(tagName) {
                                    case "ROUTE_ID":{
                                        xpp.next();
                                        bus.ROUTE_ID=Integer.parseInt(xpp.getText());
                                        if(routeId == bus.ROUTE_ID){
                                            isThis = true;
                                        }
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
            } catch (XmlPullParserException | IOException protocolException) {
                protocolException.printStackTrace();
            }
            return null;
        }
    }
}
