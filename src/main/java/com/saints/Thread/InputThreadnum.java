package com.saints.Thread;


import android.app.Instrumentation;

public class InputThreadnum extends Thread {
  private int code;
  public InputThreadnum(int code){
    this.code=code;
  }
  public void run() {
    try {
      Instrumentation inst = new Instrumentation();
      inst.sendCharacterSync(this.code);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
