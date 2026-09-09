package com.sanai.runner;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class RunnerView extends View {
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Random rng=new Random(7);
  Bitmap boy; float px,py; int lane=1; boolean jumping=false, alive=true; long jumpStart;
  float speed=0.42f, world=0; int score=0, coins=0; float downX,downY;
  ArrayList<Obj> objects=new ArrayList<>(); long last;
  static class Obj { float z; int lane; boolean coin; Obj(float z,int l,boolean c){this.z=z;lane=l;coin=c;} }
  public RunnerView(Context c){super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); boy=BitmapFactory.decodeResource(getResources(), R.drawable.boy_back); last=System.nanoTime();
    for(int i=0;i<18;i++) spawn(1.0f+i*.55f); postInvalidateOnAnimation(); }
  void spawn(float z){ int l=rng.nextInt(3); boolean c=rng.nextFloat()<.62f; objects.add(new Obj(z,l,c)); if(c && rng.nextFloat()<.45f) objects.add(new Obj(z+.11f,(l+1+rng.nextInt(2))%3,true)); }
  float roadY(float z){ return getHeight()*.32f + (1-z)*getHeight()*.62f; }
  float laneX(int l,float z){ float center=getWidth()/2f; float half=getWidth()*.42f*(1-z*.72f); return center+(l-1)*half; }
  @Override protected void onDraw(Canvas c){super.onDraw(c); float dt=Math.min(.04f,(System.nanoTime()-last)/1e9f); last=System.nanoTime();
    p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0,0,0,getHeight(),Color.rgb(115,190,235),Color.rgb(40,95,60),Shader.TileMode.CLAMP)); c.drawRect(0,0,getWidth(),getHeight(),p); p.setShader(null);
    drawRoad(c); update(dt); drawObjects(c); drawBoy(c); drawHud(c); if(!alive) drawGameOver(c); postInvalidateOnAnimation(); }
  void drawRoad(Canvas c){ float w=getWidth(),h=getHeight(); Path r=new Path(); r.moveTo(w*.44f,h*.30f);r.lineTo(w*.56f,h*.30f);r.lineTo(w*.98f,h);r.lineTo(w*.02f,h);r.close(); p.setColor(Color.rgb(55,55,60));c.drawPath(r,p);
    p.setColor(Color.WHITE);p.setStrokeWidth(5); for(int i=1;i<3;i++){Path q=new Path();q.moveTo(w*(.44f+i*.06f),h*.30f);q.lineTo(w*(i/3f),h);c.drawPath(q,p);}
    p.setColor(Color.rgb(70,150,75)); c.drawRect(0,h*.75f,w*.02f,h,p); c.drawRect(w*.98f,h*.75f,w,h,p);
    for(int i=0;i<9;i++){float z=(i/9f+world*.8f)%1f;float y=roadY(z);p.setColor(Color.argb(90,255,255,255));c.drawCircle(getWidth()*.05f,y,3+(1-z)*5,p);c.drawCircle(getWidth()*.95f,y,3+(1-z)*5,p);}
  }
  void update(float dt){ if(!alive)return; world=(world+dt*speed)%1f; for(Obj o:objects)o.z-=dt*speed; if(objects.size()<24)spawn(1.1f+rng.nextFloat()*.5f);
    Iterator<Obj> it=objects.iterator(); while(it.hasNext()){Obj o=it.next(); if(o.z<-.1f)it.remove(); else if(o.z<.055f && o.z>-.03f && o.lane==lane){ if(o.coin){coins++;score+=10;o.z=-.2f;} else if(!jumping){alive=false;}}}
    if(jumping && System.currentTimeMillis()-jumpStart>650) jumping=false; score+=(int)(dt*10); speed=Math.min(.72f,.42f+score*.00008f);
  }
  void drawObjects(Canvas c){for(Obj o:objects){if(o.z<0||o.z>1)continue;float x=laneX(o.lane,o.z),y=roadY(o.z);float s=(1-o.z); if(o.coin){p.setColor(Color.rgb(255,210,35));c.drawCircle(x,y-25*s,13*s+5,p);p.setColor(Color.rgb(255,245,120));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(x,y-25*s,13*s+5,p);p.setStyle(Paint.Style.FILL);}else{float bw=48*s+14,bh=40*s+12;p.setColor(Color.rgb(175,105,45));c.drawRect(x-bw/2,y-bh,x+bw/2,y,p);p.setColor(Color.rgb(125,70,30));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawRect(x-bw/2,y-bh,x+bw/2,y,p);p.setStyle(Paint.Style.FILL);}}
  }
  void drawBoy(Canvas c){float base=getHeight()*.86f; float jump=jumping?(float)Math.sin((System.currentTimeMillis()-jumpStart)/650.0*Math.PI)*getHeight()*.16f:0; float bh=getHeight()*.30f;float bw=bh*boy.getWidth()/boy.getHeight(); RectF dst=new RectF(getWidth()/2-bw/2,base-bh-jump,getWidth()/2+bw/2,base-jump); p.setAlpha(255);c.drawBitmap(boy,null,dst,p);}
  void drawHud(Canvas c){p.setColor(Color.WHITE);p.setTextSize(28);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("Очки: "+score,24,45,p);c.drawText("🪙 "+coins,24,80,p);p.setTextSize(17);p.setTypeface(Typeface.DEFAULT);c.drawText("← → движение   ↑ прыжок",24,getHeight()-24,p);}
  void drawGameOver(Canvas c){p.setColor(Color.argb(175,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(44);c.drawText("СТОЛКНОВЕНИЕ!",getWidth()/2,getHeight()*.43f,p);p.setTextSize(25);c.drawText("Счёт: "+score+"   Монеты: "+coins,getWidth()/2,getHeight()*.50f,p);p.setTextSize(22);c.drawText("Нажми, чтобы начать заново",getWidth()/2,getHeight()*.59f,p);p.setTextAlign(Paint.Align.LEFT);}
  void reset(){objects.clear();lane=1;score=0;coins=0;speed=.42f;world=0;alive=true;jumping=false;for(int i=0;i<18;i++)spawn(1+i*.55f);}
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;} if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX,dy=e.getY()-downY;if(!alive){reset();return true;} if(Math.abs(dx)>Math.abs(dy)&&Math.abs(dx)>50){lane=Math.max(0,Math.min(2,lane+(dx>0?1:-1)));} else if(dy<-50){jumping=true;jumpStart=System.currentTimeMillis();} else if(Math.abs(dx)<25&&Math.abs(dy)<25){jumping=true;jumpStart=System.currentTimeMillis();}return true;}return true;}
}
