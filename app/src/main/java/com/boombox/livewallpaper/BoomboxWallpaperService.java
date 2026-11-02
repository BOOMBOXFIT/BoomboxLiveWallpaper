package com.boombox.livewallpaper;

import android.app.Service;
import android.graphics.*;
import android.hardware.*;
import android.os.*;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import java.util.*;

public class BoomboxWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine(){ return new EngineImpl(); }

    private class EngineImpl extends Engine implements SensorEventListener {
        final Handler h = new Handler(Looper.getMainLooper());
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random(260226L);
        final List<Particle> particles = new ArrayList<>();
        final List<Ripple> ripples = new ArrayList<>();
        boolean visible=false;
        int w=1,hgt=1;
        float tiltX=0,tiltY=0,homeOffset=.5f;
        long last=System.nanoTime();
        SensorManager sm; Sensor sensor;
        final Runnable runner=()->drawFrame();

        @Override public void onCreate(SurfaceHolder sh){
            super.onCreate(sh);
            setTouchEventsEnabled(true);
            sm=(SensorManager)getSystemService(Service.SENSOR_SERVICE);
            if(sm!=null){
                sensor=sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                if(sensor==null) sensor=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            }
            for(int i=0;i<78;i++) particles.add(new Particle(rnd.nextFloat(),rnd.nextFloat(),.25f+rnd.nextFloat(),rnd.nextFloat()*6.28f));
        }
        @Override public void onVisibilityChanged(boolean v){
            visible=v;
            if(v){ last=System.nanoTime(); if(sm!=null&&sensor!=null) sm.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME); h.post(runner);}
            else { if(sm!=null) sm.unregisterListener(this); h.removeCallbacks(runner); }
        }
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height){
            w=Math.max(1,width);hgt=Math.max(1,height);super.onSurfaceChanged(holder,format,width,height);
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder holder){
            visible=false;if(sm!=null)sm.unregisterListener(this);h.removeCallbacks(runner);super.onSurfaceDestroyed(holder);
        }
        @Override public void onOffsetsChanged(float xo,float yo,float xs,float ys,int xp,int yp){homeOffset=xo;}
        @Override public void onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){
                long now=System.currentTimeMillis();
                if(ripples.isEmpty()||now-ripples.get(ripples.size()-1).born>90){
                    ripples.add(new Ripple(e.getX(),e.getY(),now));
                    if(ripples.size()>12)ripples.remove(0);
                }
            }
            super.onTouchEvent(e);
        }
        @Override public void onSensorChanged(SensorEvent e){
            float[] m=new float[9],o=new float[3];
            SensorManager.getRotationMatrixFromVector(m,e.values);
            SensorManager.getOrientation(m,o);
            tiltX+=(o[2]-tiltX)*.08f; tiltY+=(o[1]-tiltY)*.08f;
            tiltX=Math.max(-.65f,Math.min(.65f,tiltX)); tiltY=Math.max(-.65f,Math.min(.65f,tiltY));
        }
        @Override public void onAccuracyChanged(Sensor s,int a){}

        void drawFrame(){
            if(!visible)return;
            Canvas c=null; SurfaceHolder sh=getSurfaceHolder();
            try{
                c=sh.lockCanvas();
                if(c!=null){
                    w=Math.max(1,c.getWidth());hgt=Math.max(1,c.getHeight());
                    long n=System.nanoTime(); float dt=Math.min(.05f,(n-last)/1_000_000_000f); last=n;
                    render(c,dt,System.currentTimeMillis());
                }
            }finally{if(c!=null)sh.unlockCanvasAndPost(c);}
            h.removeCallbacks(runner); if(visible)h.postDelayed(runner,33);
        }

        void render(Canvas c,float dt,long ms){
            drawBackground(c,ms);
            drawTunnel(c,ms);
            drawParticles(c,dt,ms);
            drawHeadphones(c,ms);
            drawLogo(c,ms);
            drawVisualizer(c,ms);
            drawRipples(c,ms);
            drawVignette(c);
        }

        void drawBackground(Canvas c,long ms){
            c.drawColor(Color.rgb(2,2,8));
            float px=(homeOffset-.5f)*w*.06f+tiltX*w*.025f;
            float py=tiltY*hgt*.018f;
            p.setShader(new RadialGradient(w*.5f+px,hgt*.52f+py,w*.75f,
                new int[]{Color.rgb(24,0,38),Color.rgb(0,7,28),Color.rgb(0,0,4)},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,hgt,p);p.setShader(null);
        }

        void drawTunnel(Canvas c,long ms){
            float t=ms/1000f; float px=tiltX*w*.018f;
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(2,w*.004f));
            for(int i=0;i<7;i++){
                float f=i/7f;
                float top=hgt*(.08f+.055f*i);
                float side=w*(.035f+.055f*i);
                int alpha=70-i*7;
                p.setShader(new LinearGradient(side,0,w-side,0,
                    new int[]{Color.argb(alpha,255,25,80),Color.argb(alpha,185,30,255),Color.argb(alpha,20,150,255)},null,Shader.TileMode.CLAMP));
                RectF r=new RectF(side+px*(1-f),top,w-side+px*(1-f),hgt*(.79f-.04f*i));
                c.drawRoundRect(r,w*.12f,w*.12f,p);
            }
            p.setShader(null);p.setStyle(Paint.Style.FILL);
        }

        void drawParticles(Canvas c,float dt,long ms){
            float t=ms/1000f;
            for(int i=0;i<particles.size();i++){
                Particle q=particles.get(i);
                q.y-=dt*(.006f+q.speed*.009f);q.x+=dt*(float)Math.sin(t*.45f+q.phase)*.002f;
                if(q.y<-.03f){q.y=1.03f;q.x=rnd.nextFloat();}
                float x=q.x*w+tiltX*w*.02f*q.speed,y=q.y*hgt+tiltY*hgt*.01f*q.speed;
                int a=(int)(45+70*(.5+.5*Math.sin(t*(1.1+q.speed)+q.phase)));
                p.setColor(i%2==0?Color.argb(a,255,50,190):Color.argb(a,65,145,255));
                c.drawCircle(x,y,1.2f+q.speed*1.8f,p);
            }
        }

        void drawHeadphones(Canvas c,long ms){
            float cx=w*.5f,cy=hgt*.47f,rad=w*.265f;
            p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(w*.035f);
            p.setShader(new LinearGradient(cx-rad,cy,cx+rad,cy,
                new int[]{Color.rgb(255,35,90),Color.rgb(210,45,255),Color.rgb(30,145,255)},null,Shader.TileMode.CLAMP));
            RectF arc=new RectF(cx-rad,cy-rad,cx+rad,cy+rad);
            c.drawArc(arc,205,130,false,p);
            p.setStrokeWidth(w*.065f);c.drawLine(cx-rad*.85f,cy+rad*.2f,cx-rad*.93f,cy+rad*.58f,p);c.drawLine(cx+rad*.85f,cy+rad*.2f,cx+rad*.93f,cy+rad*.58f,p);
            p.setShader(null);p.setStyle(Paint.Style.FILL);
        }

        void drawLogo(Canvas c,long ms){
            float t=ms/1000f,pulse=.5f+.5f*(float)Math.sin(t*1.7f);
            float cx=w*.5f,cy=hgt*.54f;
            float r=w*(.225f+.012f*pulse);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w*.009f);
            p.setShader(new SweepGradient(cx,cy,new int[]{Color.RED,Color.MAGENTA,Color.BLUE,Color.CYAN,Color.RED},null));
            c.drawCircle(cx,cy,r,p);p.setShader(null);

            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
            float textSize=w*.15f;p.setTextSize(textSize);
            p.setShadowLayer(w*.03f,0,0,Color.MAGENTA);
            p.setShader(new LinearGradient(cx-w*.32f,cy,cx+w*.32f,cy,
                    new int[]{Color.rgb(255,55,70),Color.WHITE,Color.rgb(50,155,255)},null,Shader.TileMode.CLAMP));
            c.drawText("BOOMBOX",cx,cy+textSize*.33f,p);
            p.clearShadowLayer();p.setShader(null);

            p.setStrokeWidth(w*.018f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(28,28,38));
            c.drawLine(cx-w*.37f,cy+w*.06f,cx-w*.29f,cy+w*.06f,p);c.drawLine(cx+w*.29f,cy+w*.06f,cx+w*.37f,cy+w*.06f,p);
            p.setColor(Color.rgb(255,45,100));c.drawCircle(cx-w*.385f,cy+w*.06f,w*.04f,p);
            p.setColor(Color.rgb(55,145,255));c.drawCircle(cx+w*.385f,cy+w*.06f,w*.04f,p);
        }

        void drawVisualizer(Canvas c,long ms){
            float t=ms/1000f,cx=w*.5f,base=hgt*.63f;
            int bars=17;float sp=w*.015f,max=hgt*.05f;
            p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(3,w*.005f));
            for(int i=0;i<bars;i++){
                float n=(i-8)/8f,env=1-.55f*Math.abs(n),osc=.3f+.7f*Math.abs((float)Math.sin(t*2.2f+i*.71f));
                float hh=max*env*osc,x=cx+(i-8)*sp;
                int rr=(int)(255-(190*((n+1)/2))),bb=(int)(185+70*((n+1)/2));
                p.setColor(Color.argb(175,rr,55,bb));c.drawLine(x,base-hh/2,x,base+hh/2,p);
            }
        }

        void drawRipples(Canvas c,long ms){
            Iterator<Ripple> it=ripples.iterator();
            while(it.hasNext()){
                Ripple r=it.next();float age=(ms-r.born)/1000f;
                if(age>1.15f){it.remove();continue;}
                float q=1-(float)Math.pow(1-Math.min(1,age/1.15f),2.5);
                float radius=8+q*Math.max(w,hgt)*.24f;int a=(int)(200*(1-age/1.15f));
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.004f));p.setColor(Color.argb(a,255,55,225));c.drawCircle(r.x,r.y,radius,p);
                p.setStrokeWidth(Math.max(1.5f,w*.002f));p.setColor(Color.argb(a,70,155,255));c.drawCircle(r.x,r.y,radius*.72f,p);
                p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(Math.min(220,a),255,125,245));c.drawCircle(r.x,r.y,Math.max(2,10*(1-age/1.15f)),p);
            }
        }

        void drawVignette(Canvas c){
            float radius=(float)Math.hypot(w*.5f,hgt*.5f);
            p.setShader(new RadialGradient(w*.5f,hgt*.5f,radius,
                    new int[]{Color.TRANSPARENT,Color.argb(20,0,0,0),Color.argb(135,0,0,0)},
                    new float[]{.35f,.72f,1},Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,hgt,p);p.setShader(null);
        }

        class Ripple{final float x,y;final long born;Ripple(float x,float y,long b){this.x=x;this.y=y;born=b;}}
        class Particle{float x,y;final float speed,phase;Particle(float x,float y,float s,float p){this.x=x;this.y=y;speed=s;phase=p;}}
    }
}
