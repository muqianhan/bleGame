package com.saints.Thread;


import android.app.Instrumentation;

public class InputThread extends Thread {
  private int code;
  public InputThread(int code){
    this.code=code;
  }
  public void run() {
    try {
      Instrumentation inst = new Instrumentation();
      inst.sendKeyDownUpSync(this.code);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}