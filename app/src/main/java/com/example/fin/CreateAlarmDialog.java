package com.example.fin;

import static android.content.Context.ALARM_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;

public class CreateAlarmDialog {
    @SuppressLint("MissingPermission")
    public static void create(View view, Double stationLat, Double stationLon){
        Bus bus = (Bus)view.getTag();
        View dialogView = (View)View.inflate(view.getContext(), R.layout.dialog_alarm, null);
        AlertDialog.Builder dlg = new AlertDialog.Builder(view.getContext());
        dlg.setView(dialogView);
        TabHost tabHost = (TabHost) dialogView.findViewById(R.id.tabHost);
        tabHost.setup();


        TabHost.TabSpec tabSpecArtist = tabHost.newTabSpec("dayAlarm").setIndicator("요일 알람");
        tabSpecArtist.setContent(R.id.dayAlarm);
        tabHost.addTab(tabSpecArtist);

        TabHost.TabSpec tabSpecSong = tabHost.newTabSpec("temporalAlarm").setIndicator("한번 알람");
        tabSpecSong.setContent(R.id.temporalAlarm);
        tabHost.addTab(tabSpecSong);

        tabHost.setCurrentTab(1);

        // 나가야 할 시간 계산
        LocationManager manager=(LocationManager) view.getContext().getSystemService(Context.LOCATION_SERVICE);
        MainActivity.GPSListener gpsListener = new MainActivity.GPSListener();
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
        Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LocationDistance ld = new LocationDistance();
        int time = (int) (ld.distance(lastLocation.getLatitude(), lastLocation.getLongitude(), stationLat, stationLon) / 5.0 * 1.8 * 3600);
        time = bus.PREDICT_TRAV_TM - time;
        if(time < 0){       // 0보다 작으면 그 다음 버스나 이미 지난 버스의 위치로 배차간격을 계산해 시간을 예상한다.
            TextView danger = (TextView) dialogView.findViewById(R.id.dangerMessage);
            danger.setText("※주의 시간이 부정확할 수 있습니다.");

            time += BusRoute.getBusTerm(bus.busRouteArr, bus.STATION_ORD);
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, time);
        TimePicker timePicker = (TimePicker)dialogView.findViewById(R.id.timePicker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(cal.get(Calendar.HOUR));
            timePicker.setMinute(cal.get(Calendar.MINUTE));
        }
        // 확인 버튼 클릭시
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
                Toast.makeText(dialogView.getContext(), cal.getTime() + "\n알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        dlg.setNegativeButton("취소", null);
        dlg.show();
    }
}
