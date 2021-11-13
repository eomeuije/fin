package com.example.fin;


import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class ReqBusRouteList{

    int routeId;
    int busCur = -1;
    int turIndex;
    TextView textView = null;
    BusRoute [] busRouteArr;
    String endPoint = "http://openapi.changwon.go.kr/rest/bis/BusLocation/";
    String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";
    ReqBusRouteList(int routeId, TextView textView, int busCur){
        this.routeId = routeId;
        this.textView = textView;
        this.busCur = busCur;
    }
    public void run(){
        XmlPullParser xpp;
        XmlPullParserFactory factory;
        try {
            URL url = new URL(endPoint + "?serviceKey=" + key + "&route=" + routeId); //문자열로 된 요청 url을 URL객체로 생성
            InputStream is = url.openStream();
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            int eventType = xpp.getEventType();
            xpp.setInput(new InputStreamReader(is, "UTF-8"));
            String tagName = null;
            int busRouteArrindex = 0;
            while(eventType != XmlPullParser.END_DOCUMENT){

                tagName = xpp.getName();
                if(tagName == null){
                }
                else if(tagName.equals("rowCount")){
                    xpp.next();
                    busRouteArr = new BusRoute[Integer.parseInt(xpp.getText())];
                    xpp.next();
                }
                else if(tagName.equals("row")){
                    BusRoute busRoute = new BusRoute();

                    while(true){
                        xpp.next();
                        tagName = xpp.getName();
                        if(tagName != null){
                            if(tagName.equals("row")) {
                                busRouteArr[busRouteArrindex++] = busRoute;
                                break;
                            }
                            switch(tagName) {
                                case "ROUTE_ID":{
                                    xpp.next();
                                    busRoute.ROUTE_ID = Integer.parseInt(xpp.getText());
                                    xpp.next();
                                    break;
                                }
                                case "ROUTE_NM":{
                                    xpp.next();
                                    busRoute.ROUTE_NM = xpp.getName();
                                    xpp.next();
                                    break;
                                }
                                case "STATION_ID":{
                                    xpp.next();
                                    busRoute.STATION_ID = Integer.parseInt(xpp.getText());
                                    xpp.next();
                                    break;
                                }
                                case "STATION_NM":{
                                    xpp.next();
                                    busRoute.STATION_NM = xpp.getText();
                                    xpp.next();
                                    break;
                                }
                                case "PLATE_NO":{
                                    xpp.next();
                                    busRoute.PLATE_NO = xpp.getText();
                                    xpp.next();
                                    break;
                                }
                                case "TUR":{
                                    xpp.next();
                                    busRoute.TUR = xpp.getText();
                                    if(busRoute.TUR.equals("T")){
                                        turIndex = busRouteArrindex;
                                    }
                                    xpp.next();
                                    break;
                                }
                            }
                        }
                    }
                }
                eventType = xpp.next();
            }
            String result;
            if(busCur <= turIndex){
                result = busRouteArr[0].STATION_NM + "→" + busRouteArr[turIndex].STATION_NM;
            }else{
                result = busRouteArr[turIndex].STATION_NM + "→" + busRouteArr[0].STATION_NM;
            }
            textView.setText(result);
        } catch (XmlPullParserException | IOException protocolException) {
            protocolException.printStackTrace();
        }
    }

}
class BusRoute{
    int ROUTE_ID;
    String ROUTE_NM;
    int STATION_ID;
    String STATION_NM;
    String PLATE_NO;
    String TUR;
}
//class BusRouteListHandler extends Handler{
//    TextView textView = null;
//    int busCur = -1;
//    BusRoute busRouteArr [];
//    BusRouteListHandler(TextView textView, int busCur){
//        this.busCur = busCur;
//        this.busRouteArr = busRouteArr;
//        this.textView = textView;
//    }
//    @Override
//    public void handleMessage(Message msg) {
//        if(textView != null){
//            if(msg.what <= -2){
//                return;
//            }
//            if(msg.what == -1){
//
//                return;
//            }else{
//
//                return;
//            }
//        }
//    }
//}