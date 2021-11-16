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
    BusRoute [] busRouteArr;
    String endPoint = "http://openapi.changwon.go.kr/rest/bis/BusLocation/";
    String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";
    ReqBusRouteList(int routeId){
        this.routeId = routeId;
    }
    public BusRoute[] getBusRouteArr(){
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
                                    if(xpp.getText().equals("T")){
                                        busRoute.TUR = true;
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

        } catch (XmlPullParserException | IOException protocolException) {
            protocolException.printStackTrace();
        }
        return busRouteArr;
    }

}
class BusRoute{
    int ROUTE_ID;       //버스 ID
    String ROUTE_NM;    //버스 번호
    int STATION_ID;     //정류장 ID
    String STATION_NM;  //정류장명
    String PLATE_NO;    //차량 번호
    boolean TUR = false;//회차지 ture or false
}