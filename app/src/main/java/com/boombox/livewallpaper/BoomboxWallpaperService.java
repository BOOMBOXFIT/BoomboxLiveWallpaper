package com.boombox.livewallpaper;

import android.app.Service;
import android.graphics.*;
import android.hardware.*;
import android.os.*;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class BoomboxWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine(){ return new LuxuryEngine(); }

    private class LuxuryEngine extends Engine implements SensorEventListener {
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path plaquePath=new Path();
        private boolean visible=false;
        private int w=1,h=1;
        private float tiltX=0f,tiltY=0f,homeOffset=.5f;
        private long visibleSince=0L;
        private SensorManager sm; private Sensor sensor;
        private final Runnable runner=()->drawFrame();

        @Override public void onCreate(SurfaceHolder sh){
            super.onCreate(sh);
            setTouchEventsEnabled(false);
            sm=(SensorManager)getSystemService(Service.SENSOR_SERVICE);
            if(sm!=null){
                sensor=sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                if(sensor==null) sensor=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            }
        }
        @Override public void onVisibilityChanged(boolean v){
            visible=v;
            if(v){ visibleSince=System.currentTimeMillis(); registerSensors(); handler.removeCallbacks(runner); handler.post(runner); }
            else { unregisterSensors(); handler.removeCallbacks(runner); }
        }
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height){
            w=Math.max(1,width); h=Math.max(1,height); super.onSurfaceChanged(holder,format,width,height); drawFrame();
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder holder){ visible=false; unregisterSensors(); handler.removeCallbacks(runner); super.onSurfaceDestroyed(holder); }
        @Override public void onDestroy(){ unregisterSensors(); handler.removeCallbacks(runner); super.onDestroy(); }
        @Override public void onOffsetsChanged(float xo,float yo,float xs,float ys,int xp,int yp){ homeOffset=xo; }
        private void registerSensors(){ if(sm!=null&&sensor!=null) sm.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME); }
        private void unregisterSensors(){ if(sm!=null) sm.unregisterListener(this); }
        @Override public void onAccuracyChanged(Sensor s,int a){}
        @Override public void onSensorChanged(SensorEvent e){
            float[] r=new float[9],o=new float[3]; SensorManager.getRotationMatrixFromVector(r,e.values); SensorManager.getOrientation(r,o);
            tiltX+=(clamp(o[2],-.55f,.55f)-tiltX)*.065f; tiltY+=(clamp(o[1],-.5f,.5f)-tiltY)*.065f;
        }

        private void drawFrame(){
            if(!visible)return; Canvas c=null; SurfaceHolder sh=getSurfaceHolder();
            try{ c=sh.lockCanvas(); if(c!=null){ w=Math.max(1,c.getWidth());h=Math.max(1,c.getHeight()); render(c,System.currentTimeMillis()); } }
            finally{ if(c!=null) sh.unlockCanvasAndPost(c); }
            handler.removeCallbacks(runner); if(visible) handler.postDelayed(runner,33L);
        }

        private void render(Canvas c,long now){
            float unlock=Math.min(1f,(now-visibleSince)/720f); float settle=1f-(float)Math.pow(1f-unlock,3f);
            c.drawColor(Color.rgb(2,4,8));
            c.save();
            float zoom=1.025f-.018f*settle; c.scale(zoom,zoom,w*.5f,h*.5f);
            drawBackdrop(c,now,1.0f);
            drawTopPanel(c,now,0.76f);
            drawLeftPanels(c,now,0.62f);
            drawRightPanel(c,now,0.50f);
            drawBottomPanel(c,now,0.42f);
            drawPlaque(c,now,0.28f);
            c.restore();
            drawGlobalVignette(c);
        }

        private float px(float depth){ return (homeOffset-.5f)*w*.045f*depth + tiltX*w*.025f*depth; }
        private float py(float depth){ return tiltY*h*.018f*depth; }

        private void drawBackdrop(Canvas c,long now,float depth){
            float x=px(depth),y=py(depth);
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0,y,w,h+y,new int[]{Color.rgb(8,11,18),Color.rgb(2,5,10),Color.rgb(0,0,2)},null,Shader.TileMode.CLAMP));
            c.drawRect(-40+x,-40+y,w+40+x,h+40+y,p); p.setShader(null);
            p.setShader(new RadialGradient(w*.72f+x,h*.40f+y,w*.85f,new int[]{Color.argb(45,15,48,95),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
        }

        private void drawTopPanel(Canvas c,long now,float d){
            float x=px(d),y=py(d); Path q=new Path();
            q.moveTo(-w*.08f+x,-h*.03f+y); q.lineTo(w*.87f+x,-h*.03f+y); q.lineTo(w*.48f+x,h*.17f+y); q.lineTo(-w*.08f+x,h*.07f+y); q.close();
            fillPanel(c,q,Color.rgb(27,30,37),Color.rgb(5,8,13));
            glowLine(c,-w*.02f+x,h*.065f+y,w*.48f+x,h*.17f+y,now,1.0f);
            glowLine(c,w*.48f+x,h*.17f+y,w*.87f+x,-h*.03f+y,now,.7f);
        }
        private void drawLeftPanels(Canvas c,long now,float d){
            float x=px(d),y=py(d); Path q=new Path();
            q.moveTo(-w*.08f+x,h*.24f+y); q.lineTo(w*.22f+x,h*.20f+y); q.lineTo(w*.48f+x,h*.30f+y); q.lineTo(w*.34f+x,h*.52f+y); q.lineTo(-w*.08f+x,h*.44f+y); q.close();
            fillPanel(c,q,Color.rgb(17,22,30),Color.rgb(2,5,10));
            glowLine(c,w*.22f+x,h*.20f+y,w*.48f+x,h*.30f+y,now,.75f);
            glowLine(c,-w*.02f+x,h*.42f+y,w*.34f+x,h*.52f+y,now,.55f);

            Path q2=new Path(); q2.moveTo(-w*.06f+x,h*.50f+y); q2.lineTo(w*.34f+x,h*.44f+y); q2.lineTo(w*.43f+x,h*.62f+y); q2.lineTo(-w*.06f+x,h*.72f+y); q2.close();
            fillPanel(c,q2,Color.rgb(20,24,31),Color.rgb(3,6,10)); glowLine(c,-w*.02f+x,h*.51f+y,w*.34f+x,h*.45f+y,now,.48f);
        }
        private void drawRightPanel(Canvas c,long now,float d){
            float x=px(d),y=py(d); Path q=new Path();
            q.moveTo(w*.73f+x,h*.16f+y); q.lineTo(w*1.05f+x,h*.11f+y); q.lineTo(w*1.05f+x,h*.63f+y); q.lineTo(w*.83f+x,h*.55f+y); q.close();
            fillPanel(c,q,Color.rgb(18,23,31),Color.rgb(2,4,8)); glowLine(c,w*.82f+x,h*.55f+y,w*1.04f+x,h*.63f+y,now,.55f);
        }
        private void drawBottomPanel(Canvas c,long now,float d){
            float x=px(d),y=py(d); Path q=new Path();
            q.moveTo(-w*.04f+x,h*.77f+y); q.lineTo(w*.16f+x,h*.69f+y); q.lineTo(w*.63f+x,h*.82f+y); q.lineTo(w*.48f+x,h*1.04f+y); q.lineTo(-w*.04f+x,h*1.04f+y); q.close();
            fillPanel(c,q,Color.rgb(29,32,38),Color.rgb(3,5,9)); glowLine(c,w*.16f+x,h*.69f+y,w*.63f+x,h*.82f+y,now,.65f);
        }

        private void drawPlaque(Canvas c,long now,float d){
            float x=px(d),y=py(d); plaquePath.reset();
            plaquePath.moveTo(w*.55f+x,h*.22f+y); plaquePath.lineTo(w*.88f+x,h*.20f+y); plaquePath.lineTo(w*.72f+x,h*.84f+y); plaquePath.lineTo(w*.32f+x,h*.76f+y); plaquePath.close();
            p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(w*.35f,h*.22f,w*.82f,h*.82f,new int[]{Color.rgb(50,52,57),Color.rgb(23,25,30),Color.rgb(9,11,15)},null,Shader.TileMode.CLAMP)); c.drawPath(plaquePath,p); p.setShader(null);
            p.setColor(Color.argb(115,255,255,255));p.setStrokeWidth(Math.max(1f,w*.0015f));p.setStyle(Paint.Style.STROKE); c.drawLine(w*.55f+x,h*.22f+y,w*.88f+x,h*.20f+y,p);
            glowLine(c,w*.88f+x,h*.20f+y,w*.72f+x,h*.84f+y,now,1.0f);
            glowLine(c,w*.32f+x,h*.76f+y,w*.72f+x,h*.84f+y,now,.45f);
            drawEngravedText(c,x,y);
            drawPlaqueSheen(c,now);
        }

        private void drawEngravedText(Canvas c,float x,float y){
            String s="BOOMBOX"; float cx=w*.605f+x, start=h*.30f+y, step=h*.068f; float size=Math.min(w*.105f,h*.060f);
            p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL)); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(size); p.setStyle(Paint.Style.FILL);
            for(int i=0;i<s.length();i++){
                float yy=start+i*step;
                p.setColor(Color.argb(115,210,215,222)); c.drawText(String.valueOf(s.charAt(i)),cx-1.6f,yy-1.8f,p);
                p.setColor(Color.rgb(5,6,9)); c.drawText(String.valueOf(s.charAt(i)),cx+1.8f,yy+2.1f,p);
                p.setColor(Color.rgb(13,14,18)); c.drawText(String.valueOf(s.charAt(i)),cx,yy,p);
            }
        }

        private void drawPlaqueSheen(Canvas c,long now){
            float cycle=(now%7600L)/7600f; float center=-.35f+1.7f*cycle;
            c.save(); c.clipPath(plaquePath);
            float sx=w*center;
            p.setShader(new LinearGradient(sx-w*.12f,0,sx+w*.18f,h,new int[]{Color.TRANSPARENT,Color.argb(13,255,255,255),Color.argb(27,145,190,255),Color.argb(9,255,255,255),Color.TRANSPARENT},new float[]{0f,.35f,.5f,.67f,1f},Shader.TileMode.CLAMP)); p.setStyle(Paint.Style.FILL); c.drawRect(0,0,w,h,p); p.setShader(null); c.restore();
        }

        private void fillPanel(Canvas c,Path q,int hi,int lo){ p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0,0,w,h,new int[]{hi,lo},null,Shader.TileMode.CLAMP)); c.drawPath(q,p); p.setShader(null); }
        private void glowLine(Canvas c,float x1,float y1,float x2,float y2,long now,float strength){
            float breath=.72f+.28f*(float)Math.sin(now/2800.0); int a=(int)(80*strength*breath);
            p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(Math.max(7f,w*.010f)); p.setColor(Color.argb(Math.max(8,a/4),0,75,255)); c.drawLine(x1,y1,x2,y2,p);
            p.setStrokeWidth(Math.max(3f,w*.004f)); p.setColor(Color.argb(Math.max(15,a),0,135,255)); c.drawLine(x1,y1,x2,y2,p);
            p.setStrokeWidth(Math.max(1.2f,w*.0016f)); p.setColor(Color.argb(Math.min(220,a+80),100,210,255)); c.drawLine(x1,y1,x2,y2,p);
        }
        private void drawGlobalVignette(Canvas c){
            p.setStyle(Paint.Style.FILL); float r=(float)Math.hypot(w*.5f,h*.5f); p.setShader(new RadialGradient(w*.5f,h*.48f,r,new int[]{Color.TRANSPARENT,Color.argb(22,0,0,0),Color.argb(135,0,0,0)},new float[]{.34f,.73f,1f},Shader.TileMode.CLAMP)); c.drawRect(new RectF(0,0,w,h),p); p.setShader(null);
        }
        private float clamp(float v,float lo,float hi){ return Math.max(lo,Math.min(hi,v)); }
    }
}
