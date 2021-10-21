class Globs1 {
    /*
    DESCRIPTION: Better heuristic than original backup 14
    Change: caveOneWt is a constant 1-delta. [originally 2+delta for backUpDist enabled]
    Profit: caves are explored later on. payoff is accurate
    Explanation: Why reZeroCavePipe = true is better -->
        zeroing caves and pipes are better in general, because:
        1) Encourages board exploration
        2) If not zeroed, may come back after a long time to find zero

    UNSOLVED:
        1) Two pacs enter a pipe from different sides [both do greedy for seen parts, since caveOneWt < 1]

    ADVANCED IDEAS:
        1) Develop a binary 0-1 modification for cave-pipe. UNSOLVED-1 will be solved.
        2) Since over time, certainty of pellet is reduced, reduce 1 to blindPelletWt in say, 10/15 turns.
     */


    public static final int INF = 50000;
    public static final int dangerDist = 4;  //MAX danger distance [-1 for non-speed]
    //public static final eatDist = dangerDist  //EXPERIMENTAL [Hard Coded] [NEED UPDATE]
    public static final int dangerOverSpeed = 1;  //MAX value of danger to avoid speeding
    public static final int switchDistance = 2;  //Switch only for inevitable risk
    public static final int fwdMove = 20;  //set to INF for counting all pellets //forward counting for pellets
    public static final int bwdMove = 5;  //backward counting for pellets [excl. dest. cell]
    public static final double blindPelletWt = 1.0/2;
    public static final int fwdPelletZero = fwdMove;  //set to INF for all pellets zero
    public static final int bwdPelletZero = bwdMove;
    public static final boolean destPelletZero = true;
    public static final boolean backUpDistEnable = true;  //computes backUpDist if true
    public static final boolean distPlusPriority = false;
    /* DOCS for distPlusPriority
        //only active if backUpDistEnable = true
        //if set to false, minDist breaks distance tie in greedy
        //if set to true, minDistPlus breaks distance tie in greedy
     */

    //debugging purposes
    public static int width;

    //pellet prediction parameters
    public static final double delta = 0.0001;  //delta tends to zero
    public static final double oppComeOutWt = delta/3;
    //cave predictions
    public static final double caveZeroWt = delta;
    public static final double caveOneWt = (backUpDistEnable? 1 : 1) - delta;  //->2 for backUpDist enabled (preferred)
    //->1 for backUpDist disabled (preferred)
    //pipe predictions
    public static final double pipeZeroWt = 1.0/10;
    public static final double pipeOneWt = 1 - pipeZeroWt;
    //public static final double pipeOneWt = 1 - delta;
    //If both ends of pipe contradict, re-evaluate as blindPelletWt
    //NEED UPDATE: need better algorithm for 0-1 clash in pipe.

    public static final boolean reZeroCavePipe = true;

    //opponent corridor[cave/pipe] pellet eating extrapolation
    public static final double oppXpolZeroWt = delta/2;  //static 0 for opponent in corridor
    //NEED UPDATE: better algo., using following constants
    public static final double oppFoodEatWt = 2.0 + 2*delta;  //if pellet is closer to you, you can eat [even killing pac(s)!]
    // NEED UPDATE: must enable for specific pacman(s) only]
    //public static final boolean oppXpolEat = true;  //eating algo. based on opponent pipe extrapolation
    //[NEED UPDATE: lots of work]
    //cave,pipe exploration weights  [super pellet prioritized with Globs.INF]
    //public static final double pipeSeeWt = 1.0/4;  //for joint cells [0 for inactive]
    public static final double pipeSeeWt = 0;  //for joint cells [0 for inactive]
    //public static final double CaveSeeWt = pipeSeeWt/2;  //for joint cells [0 for inactive]
    public static final double CaveSeeWt = 0;  //for joint cells [0 for inactive]
    public static final boolean seeWtOverOne = false;  //DOCS: if seeWt = 1, seeWt += (flag? +delta : -delta)

    public static final double superPelletWeight = Globs.INF;

    //myXYHistory parameters [resolve collision]
    public static final int xyHistorySize = 6;  //includes current position
    public static final int switchAfter = 2;  //based on same final position [NOT on collision. NEED UPDATE!]
    //switchAfter < xyHistorySize for consistent behaviour
}
