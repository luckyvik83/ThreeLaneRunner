package com.sanai.runner;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class RunnerView extends View {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random rng = new Random(7);
    Bitmap boy, sister;
    int lane = 1, level = 1, score = 0, coins = 0;
    boolean jumping = false, alive = true, levelComplete = false;
    long jumpStart, last, levelStart;
    float speed = 0.42f, world = 0, downX, downY;
    ArrayList<Obj> objects = new ArrayList<>();

    static class Obj {
        float z; int lane; boolean coin;
        Obj(float z, int lane, boolean coin) { this.z = z; this.lane = lane; this.coin = coin; }
    }

    public RunnerView(Context c) {
        super(c);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        boy = BitmapFactory.decodeResource(getResources(), R.drawable.boy_back);
        sister = BitmapFactory.decodeResource(getResources(), R.drawable.sister_front);
        last = System.nanoTime();
        levelStart = System.currentTimeMillis();
        resetObjects();
        postInvalidateOnAnimation();
    }

    void resetObjects() {
        objects.clear();
        for (int i = 0; i < 18; i++) spawn(1.0f + i * .55f);
    }

    void spawn(float z) {
        int l = rng.nextInt(3);
        boolean c = rng.nextFloat() < .62f;
        objects.add(new Obj(z, l, c));
        if (c && rng.nextFloat() < .45f)
            objects.add(new Obj(z + .11f, (l + 1 + rng.nextInt(2)) % 3, true));
    }

    float roadY(float z) { return getHeight() * .32f + (1 - z) * getHeight() * .62f; }
    float laneX(int l, float z) {
        float center = getWidth() / 2f;
        float half = getWidth() * .42f * (1 - z * .72f);
        return center + (l - 1) * half;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float dt = Math.min(.04f, (System.nanoTime() - last) / 1e9f);
        last = System.nanoTime();

        drawBackground(c);
        drawRoad(c);
        if (alive && !levelComplete) update(dt);
        drawObjects(c);
        drawBoy(c);
        drawHud(c);
        if (!alive) drawGameOver(c);
        if (levelComplete) drawLevelComplete(c);
        postInvalidateOnAnimation();
    }

    void drawBackground(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, getHeight(),
                Color.rgb(105, 190, 238), Color.rgb(35, 105, 72), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), p);
        p.setShader(null);
        // distant skyline
        p.setColor(Color.argb(55, 255, 255, 255));
        for (int i = 0; i < 9; i++) {
            float x = i * getWidth() / 8f;
            float bh = 35 + (i % 4) * 28;
            c.drawRect(x, getHeight() * .30f - bh, x + getWidth() * .055f,
                    getHeight() * .30f, p);
        }
    }

    void drawRoad(Canvas c) {
        float w = getWidth(), h = getHeight();
        Path r = new Path();
        r.moveTo(w * .44f, h * .30f); r.lineTo(w * .56f, h * .30f);
        r.lineTo(w * .98f, h); r.lineTo(w * .02f, h); r.close();
        p.setColor(Color.rgb(48, 51, 57)); c.drawPath(r, p);

        p.setColor(Color.WHITE); p.setStrokeWidth(5);
        for (int i = 1; i < 3; i++) {
            Path q = new Path();
            q.moveTo(w * (.44f + i * .06f), h * .30f);
            q.lineTo(w * (i / 3f), h);
            c.drawPath(q, p);
        }
        p.setColor(Color.rgb(65, 145, 72));
        c.drawRect(0, h * .75f, w * .02f, h, p);
        c.drawRect(w * .98f, h * .75f, w, h, p);

        for (int i = 0; i < 9; i++) {
            float z = (i / 9f + world * .8f) % 1f;
            float y = roadY(z);
            p.setColor(Color.argb(90, 255, 255, 255));
            c.drawCircle(w * .05f, y, 3 + (1 - z) * 5, p);
            c.drawCircle(w * .95f, y, 3 + (1 - z) * 5, p);
        }
    }

    void update(float dt) {
        world = (world + dt * speed) % 1f;
        for (Obj o : objects) o.z -= dt * speed;
        if (objects.size() < 24) spawn(1.1f + rng.nextFloat() * .5f);

        Iterator<Obj> it = objects.iterator();
        while (it.hasNext()) {
            Obj o = it.next();
            if (o.z < -.1f) it.remove();
            else if (o.z < .055f && o.z > -.03f && o.lane == lane) {
                if (o.coin) { coins++; score += 10; o.z = -.2f; }
                else if (!jumping) { alive = false; }
            }
        }

        if (jumping && System.currentTimeMillis() - jumpStart > 650) jumping = false;
        score += (int)(dt * 10);
        speed = Math.min(.76f, .42f + level * .045f + score * .00005f);

        // A level lasts about 25 seconds. Reaching the end triggers the sister scene.
        if (System.currentTimeMillis() - levelStart >= 25000) levelComplete = true;
    }

    void drawObjects(Canvas c) {
        for (Obj o : objects) {
            if (o.z < 0 || o.z > 1) continue;
            float x = laneX(o.lane, o.z), y = roadY(o.z), s = 1 - o.z;
            if (o.coin) {
                float rr = 13 * s + 5;
                p.setColor(Color.rgb(255, 208, 35)); c.drawCircle(x, y - 25 * s, rr, p);
                p.setColor(Color.rgb(255, 244, 120)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
                c.drawCircle(x, y - 25 * s, rr, p); p.setStyle(Paint.Style.FILL);
                p.setColor(Color.rgb(225, 160, 20)); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(13 * s + 5); c.drawText("★", x, y - 20 * s, p); p.setTextAlign(Paint.Align.LEFT);
            } else {
                float bw = 48 * s + 14, bh = 40 * s + 12;
                p.setColor(Color.rgb(175, 105, 45)); c.drawRect(x - bw/2, y - bh, x + bw/2, y, p);
                p.setColor(Color.rgb(125, 70, 30)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
                c.drawRect(x - bw/2, y - bh, x + bw/2, y, p); p.setStyle(Paint.Style.FILL);
                c.drawLine(x-bw*.35f,y-bh*.75f,x+bw*.35f,y-bh*.2f,p);
                c.drawLine(x+bw*.35f,y-bh*.75f,x-bw*.35f,y-bh*.2f,p);
            }
        }
    }

    void drawBoy(Canvas c) {
        float base = getHeight() * .88f;
        float jump = jumping ? (float)Math.sin((System.currentTimeMillis()-jumpStart)/650.0*Math.PI) * getHeight()*.16f : 0;
        float bh = getHeight() * .30f;
        float bw = bh * boy.getWidth() / boy.getHeight();
        RectF dst = new RectF(getWidth()/2-bw/2, base-bh-jump, getWidth()/2+bw/2, base-jump);
        p.setAlpha(255); c.drawBitmap(boy, null, dst, p);
    }

    void drawHud(Canvas c) {
        p.setColor(Color.WHITE); p.setTextSize(26); p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("Уровень " + level, 24, 36, p);
        p.setTextSize(22); p.setTypeface(Typeface.DEFAULT);
        c.drawText("Очки: " + score, 24, 67, p);
        c.drawText("Монеты: " + coins, 24, 96, p);
        // progress bar
        float progress = Math.min(1f, (System.currentTimeMillis()-levelStart)/25000f);
        p.setColor(Color.argb(100,255,255,255)); c.drawRoundRect(24,112,getWidth()-24,124,8,8,p);
        p.setColor(Color.rgb(255,210,35)); c.drawRoundRect(24,112,24+(getWidth()-48)*progress,124,8,8,p);
        p.setColor(Color.WHITE); p.setTextSize(17); c.drawText("← → движение   ↑ прыжок", 24, getHeight()-24, p);
    }

    void drawLevelComplete(Canvas c) {
        p.setColor(Color.argb(205, 0, 0, 0)); c.drawRect(0,0,getWidth(),getHeight(),p);
        // sister portrait panel
        float panelW = getWidth() * .72f, panelH = getHeight() * .52f;
        float left = (getWidth()-panelW)/2f, top = getHeight()*.12f;
        p.setColor(Color.WHITE); c.drawRoundRect(left,top,left+panelW,top+panelH,28,28,p);
        RectF img = new RectF(left+14, top+14, left+panelW-14, top+panelH-82);
        c.drawBitmap(sister, null, img, p);
        p.setColor(Color.rgb(30,120,70)); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(30); c.drawText("Ромка, ты молодец!", getWidth()/2f, top+panelH-48, p);
        p.setTextSize(20); p.setTypeface(Typeface.DEFAULT);
        c.drawText("Проходи на следующий уровень!", getWidth()/2f, top+panelH-20, p);
        p.setColor(Color.rgb(45,180,70));
        float bw=getWidth()*.58f, bh=62, by=top+panelH+28, bx=(getWidth()-bw)/2f;
        c.drawRoundRect(bx,by,bx+bw,by+bh,31,31,p);
        p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(24);
        c.drawText("СЛЕДУЮЩИЙ УРОВЕНЬ",getWidth()/2f,by+40,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    void drawGameOver(Canvas c) {
        p.setColor(Color.argb(185,0,0,0)); c.drawRect(0,0,getWidth(),getHeight(),p);
        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(42); c.drawText("СТОЛКНОВЕНИЕ!",getWidth()/2,getHeight()*.43f,p);
        p.setTextSize(24); c.drawText("Уровень "+level+"   Счёт: "+score+"   Монеты: "+coins,getWidth()/2,getHeight()*.50f,p);
        p.setTextSize(21); p.setTypeface(Typeface.DEFAULT); c.drawText("Нажми, чтобы начать заново",getWidth()/2,getHeight()*.59f,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    void resetGame() {
        objects.clear(); lane=1; level=1; score=0; coins=0; speed=.42f; world=0; alive=true; jumping=false; levelComplete=false;
        levelStart=System.currentTimeMillis(); resetObjects();
    }

    void nextLevel() {
        level++; score += 100; speed = Math.min(.76f, .42f + level*.045f); levelStart=System.currentTimeMillis(); levelComplete=false; alive=true; jumping=false; lane=1; resetObjects();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction()==MotionEvent.ACTION_DOWN) { downX=e.getX(); downY=e.getY(); return true; }
        if (e.getAction()==MotionEvent.ACTION_UP) {
            float dx=e.getX()-downX, dy=e.getY()-downY;
            if (levelComplete) { nextLevel(); return true; }
            if (!alive) { resetGame(); return true; }
            if (Math.abs(dx)>Math.abs(dy) && Math.abs(dx)>50) {
                lane=Math.max(0,Math.min(2,lane+(dx>0?1:-1)));
            } else if (dy<-50 || (Math.abs(dx)<25 && Math.abs(dy)<25)) {
                jumping=true; jumpStart=System.currentTimeMillis();
            }
            return true;
        }
        return true;
    }
}
