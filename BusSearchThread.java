package com.example.fin;

import android.app.Dialog;

public class BusSearchThread extends Thread{

    Dialog dlg;
    String busNm;
    BusInfo [] busInfoArr;
    BusSearchThread(Dialog dlg, String busNM, BusInfo [] busInfoArr){
        this.dlg = dlg;
        this.busNm = busNM;
        this.busInfoArr = busInfoArr;
    }

    @Override
    public void run() {
        super.run();

    }
}
