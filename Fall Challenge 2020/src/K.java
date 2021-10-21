class K {
    /*
     * Constants class
     */

    public static final int itemTypes = 4;
    public static int spells;
    public static int storage = 10;

    /**
     * For n = scale, i = itemTypes:<br>
     * <b>options = [ (n+i) <sub style="font-size:12px">C</sub> i ] - 1</b>
     */
    public static int upScale = 2;
    public static int downScale = 3;
    public static int maxScale = Math.max(upScale,downScale);

    public static int inf = 9999999;

    /*
     * Control variables
     */
    public static int comboDepth = 10;
    public static int moveDepth = comboDepth*2;  //at most 1 rest per spell
    /*
     * downMoveStart:
     * lower bound for downscale search's moveCount
     * dependent on spell [initialized after spell]
     */
    public static int downMoveStart;

}
