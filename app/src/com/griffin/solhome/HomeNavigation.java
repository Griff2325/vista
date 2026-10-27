package com.griffin.solhome;

/** Remote focus and reveal policy for the clipped two-column Home tile grid. */
final class HomeNavigation {
    static final int UP=0,DOWN=1,LEFT=2,RIGHT=3;
    static final int PRIME_VIDEO=18,FIRE_TV_HOME=19;
    static final int[] GRID={0,1,2,3,4,5,PRIME_VIDEO,11,12,6,7,8,9,10,FIRE_TV_HOME};
    static final int STEP=150,MAX_SCROLL=Math.max(0,((GRID.length+1)/2-4)*STEP);
    static boolean sound(int before,int after,long now,long last){return before!=after&&now-last>=100;}
    static int favoriteIndex(int id){return id==PRIME_VIDEO?6:id>=0&&id<6?id:-1;}
    static int position(int id){for(int i=0;i<GRID.length;i++)if(GRID[i]==id)return i;return -1;}
    static int move(int id,int direction,int lastGrid){
        int p=position(id);
        if(p>=0){
            if(direction==UP)return p>=2?GRID[p-2]:id;
            if(direction==DOWN)return p/2<(GRID.length-1)/2?GRID[Math.min(p+2,GRID.length-1)]:13+p%2;
            if(direction==LEFT)return p%2==1?GRID[p-1]:id;
            return p%2==0&&p+1<GRID.length?GRID[p+1]:15;
        }
        if(id==13||id==14){if(direction==UP)return id==13?FIRE_TV_HOME:10;if(direction==DOWN)return id+2;if(direction==RIGHT)return id==13?14:15;return id==14?13:id;}
        if(direction==UP)return position(lastGrid)>=0?lastGrid:0;
        if(direction==LEFT)return id==15?(position(lastGrid)>=0?lastGrid:0):id-1;
        if(direction==RIGHT)return Math.min(17,id+1);
        return id;
    }
    static int reveal(int id,int current){
        current=Math.max(0,Math.min(MAX_SCROLL,current));int p=position(id);if(p<0)return current;
        int row=p/2,first=current/STEP;return row<first?row*STEP:row>first+3?(row-3)*STEP:current;
    }
    static int shortcutHit(float x,float y){
        if(y<896||y>=926)return -1;
        for(int i=0;i<2;i++)if(x>=108+i*233&&x<308+i*233)return 13+i;
        return -1;
    }
    static int hit(float x,float y,int scroll){
        if(y<280||y>=884)return -1;int row=(int)Math.floor((y-286+scroll)/STEP),col=x>=328?1:0;
        if(row<0||row>(GRID.length-1)/2)return -1;int p=row*2+col;
        float left=108+col*233,width=200;
        return p<GRID.length&&x>=left&&x<left+width?GRID[p]:-1;
    }
}
