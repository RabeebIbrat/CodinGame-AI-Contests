import java.util.*;

class Globs {
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
    public static final double caveOneWt = 0.5 + Math.random();  //->2 for backUpDist enabled (preferred)
    //->1 for backUpDist disabled (preferred)
    //pipe predictions
    public static final double pipeZeroWt = 0.0828417818880451;
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

enum Type {
    Rock("ROCK"), Paper("PAPER"), Scissors("SCISSORS"), Dead("DEAD");

    String name;

    Type(String name) {
        this.name = name;
    }

    public boolean lt(Type b) {
        return (this == Rock && b == Paper) || (this == Paper && b == Scissors) || (this == Scissors && b == Rock);
    }

    public boolean eq(Type b) {
        return (this == b);
    }

    public boolean gt(Type b) {
        return !lt(b) && !eq(b);
    }

    public Type greater() throws Exception {  //returns the greater type
        switch(this) {
            case Rock:
                return Paper;
            case Paper:
                return Scissors;
            case Scissors:
                return Rock;
            case Dead:
                throw new DeadException();  //coping with rules of silver league
            default:
                return null;
        }
    }
}

class DeadException extends Exception{};

class Coordinate {
    public int x;
    public int y;

    public Coordinate() {  //gives default invalid values
        x = -1;
        y = -1;
    }

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Coordinate(int cell, int width, boolean transform) {
        Coordinate loc = Ops.decode(cell, width);
        this.x = loc.x;
        this.y = loc.y;
    }

    public boolean isInvalid() {
        return x==-1 && y==-1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return x == that.x &&
                y == that.y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}

class Path {
    int[][] dist;
    int[][] next;
    int[][] last;

    public Path(int[][] dist, int[][] next, int[][] last) {
        this.dist = dist;
        this.next = next;
        this.last = last;
    }
}

class Board {
    int width;
    int height;
    int[][] rawBoard;

    public Board(int width, int height, int[][] rawBoard) {
        this.width = width;
        this.height = height;
        this.rawBoard = rawBoard;
    }
}

class Root {
    int root;
    int next;

    public Root(int root, int next) {
        this.root = root;
        this.next = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Root root1 = (Root) o;
        return root == root1.root &&
                next == root1.next;
    }

    @Override
    public int hashCode() {
        return Objects.hash(root, next);
    }

    @Override
    public String toString() {
        return "{" + Ops.decode(root,Globs.width) + "->" + Ops.decode(next,Globs.width) + '}';
    }
}

class Ops {
    public static int encode(int x, int y, int width) {
        return y*width + x;
    }

    public static int encode(Coordinate loc, int width) {
        return encode(loc.x, loc.y, width);
    }

    public static Coordinate decode(int cell, int width) {
        return new Coordinate(cell%width, cell/width);
    }

    public static ArrayList<Integer> getAdjacentCells(int cell, Board board) {
        Coordinate loc = decode(cell, board.width);
        if(board.rawBoard[loc.x][loc.y] == 1) {
            return new ArrayList<>();
        }
        ArrayList<Integer> adjacent = new ArrayList<>();
        int x2,y2;
        //left,right
        y2 = loc.y;
        x2 = (loc.x-1);
        if(x2 < 0) x2 += board.width;
        if(board.rawBoard[x2][y2] == 0)  adjacent.add(encode(x2,y2,board.width));
        x2 = (loc.x+1) % board.width;
        if(board.rawBoard[x2][y2] == 0)  adjacent.add(encode(x2,y2,board.width));
        //up,down
        x2 = loc.x;
        y2 = (loc.y-1);
        if(y2 < 0) y2 += board.height;
        if(board.rawBoard[x2][y2] == 0)  adjacent.add(encode(x2,y2,board.width));
        y2 = (loc.y+1) % board.height;
        if(board.rawBoard[x2][y2] == 0)  adjacent.add(encode(x2,y2,board.width));
        return adjacent;
    }

    public static Queue<Integer> getVisibleCells(int cell, Board board) {
        Coordinate loc = decode(cell, board.width);
        if(board.rawBoard[loc.x][loc.y] == 1) {
            return new LinkedList<>();
        }
        Queue<Integer> visible = new LinkedList<>();
        visible.add(encode(loc.x,loc.y,board.width));
        int x2,y2;
        //left
        boolean xWrap = false;
        y2 = loc.y;
        x2 = loc.x;
        while(true) {
            x2 = (x2 - 1);
            if (x2 < 0) x2 += board.width;
            if(x2 == loc.x) {
                xWrap = true;
                break;  //BREAK for wraparound
            }
            if (board.rawBoard[x2][y2] == 0) visible.add(encode(x2, y2, board.width));
            else break;  //BREAK of line of sight for wall
        }
        //right
        if(!xWrap) {
            x2 = loc.x;
            while (true) {
                x2 = (x2 + 1) % board.width;
                if (board.rawBoard[x2][y2] == 0) visible.add(encode(x2, y2, board.width));
                else break;  //BREAK of line of sight for wall
            }
        }
        //up
        boolean yWrap = false;
        x2 = loc.x;
        y2 = loc.y;
        while(true) {
            y2 = (y2 - 1);
            if (y2 < 0) y2 += board.height;
            if(y2 == loc.y) {
                yWrap = true;
                break;  //BREAK for wraparound
            }
            if (board.rawBoard[x2][y2] == 0) visible.add(encode(x2, y2, board.width));
            else break;  //BREAK of line of sight for wall
        }
        //down
        y2 = loc.y;
        if(!yWrap) {
            while (true) {
                y2 = (y2 + 1) % board.height;
                if (board.rawBoard[x2][y2] == 0) visible.add(encode(x2, y2, board.width));
                else break;  //BREAK of line of sight for wall
            }
        }

        return visible;
    }

    public static int generateRandom(int min, int max) {
        if(max < min) {
            System.err.println("Error in random number generation.");
            int temp = max;
            max = min;
            min = temp;
        }
        int range = max - min + 1;
        return ((int)(Math.random()*range)) + min;
    }

    public static Type getType(String type) throws Exception {
        switch (type) {
            case "ROCK":
                return Type.Rock;
            case "PAPER":
                return Type.Paper;
            case "SCISSORS":
                return Type.Scissors;
            case "DEAD":
                throw new DeadException();  //coping with rules of silver league
                //return Type.Dead;
            default:
                System.err.println("Invalid string found in \"Ops.getType\" function");
                return null;
        }
    }

    //new greedy for fwdMove and bWdMove
    public static double computeTotalPellet(int src, int dest, double[] pellet, Path path,
                                            HashMap<Integer,Boolean> takenPellets) {
        double pelletCount = 0;
        int step = 0;
        int i = path.next[src][dest];
        if(i == -1) {
            System.err.println("WARNING! Wall provided in computeTotalPellet");
            return 0;
        }
        //fwd count [ends until i]
        while( step < Globs.fwdMove && i != dest) {
            if(!takenPellets.containsKey(i)) pelletCount += pellet[i];
            i = path.next[i][dest];
            step++;
        }
        //bwd count [not crossing i]
        if(i != dest) {
            int j = path.last[src][dest];
            step = 0;
            //bwd count [ends until j]
            while(step < Globs.bwdMove && j != i) {
                if(!takenPellets.containsKey(j)) pelletCount += pellet[j];
                j = path.last[src][j];
                step++;
            }
            if(step < Globs.bwdMove) {
                if(!takenPellets.containsKey(j)) pelletCount += pellet[j];
            }
        }

        if(!takenPellets.containsKey(dest)) pelletCount += pellet[dest];
        return pelletCount;
    }

    public static int computeBackUpDist(int src, int dest, Path path, Board board, ArrayList<Integer>[] adjacentList,
                                        HashMap<Integer,Root> getRealRoot,
                                        HashMap< Root,ArrayList<Integer> > realCaveMap) {  //computing backUp distance
        if(!Globs.backUpDistEnable)  return 0;
        if(adjacentList[dest].size() > 2) return 0;  //dest is a real joint cell

        Root rootDest = getRealRoot.get(dest);
        if(rootDest == null)  return 0;  //dest is not in a [real] cave
        int destDist = path.dist[rootDest.root][dest];

        if(adjacentList[src].size() > 2)  return destDist;
        Root rootSrc = getRealRoot.get(src);
        if( rootSrc == null || !rootDest.equals(rootSrc) )  return destDist;
        //src & dest in same cave [if not returned by now]
        int srcDist = path.dist[rootSrc.root][src];
        if(srcDist >= destDist)  return 0;
        else  return destDist-srcDist;
    }

    public static ArrayList<Integer> realCaveExtend(int back, int now, ArrayList<Integer>[] adjacentList,
                                                    ArrayList<Integer> cave) {  //cave must be from now [to back] to end
        Stack<Integer> outPath = new Stack<>();
        while(adjacentList[now].size() == 2) {  //must never be equal to 1
            ArrayList<Integer> adjList = new ArrayList<>(adjacentList[now]);
            int finalBack = back;
            adjList.removeIf(i -> i.equals(finalBack));
            if(adjList.size() == 0) {  //debug print
                System.err.println("Error in caveOut: Inconsistent adjacentList provided.");
            }
            back = now;
            now = adjList.remove(0);
            outPath.push(now);
        }
        ArrayList<Integer> realCave = new ArrayList<>();
        while(!outPath.empty()) {
            realCave.add(outPath.pop());
        }
        realCave.addAll(cave);
        return realCave;
    }

    public static ArrayList<Integer> realCaveForm(int corner, ArrayList<Integer>[] adjacentList) {
        if(adjacentList[corner].size() != 1) {
            System.err.println("Error in realCaveForm: Wrong corner provided.");
        }
        int back = corner;
        int now = adjacentList[corner].get(0);
        return Ops.realCaveExtend( back, now, adjacentList, new ArrayList<Integer>(Arrays.asList(now,back)) );
    }

    public static void placeRealCave(ArrayList<Integer> realCave, HashMap< Root,ArrayList<Integer> > realCaveMap,
                                     HashMap<Integer,Root> getRealRoot) {
        Root realRoot = new Root(realCave.get(0),realCave.get(1));
        realCaveMap.put(realRoot,realCave);
        for(int i : realCave) {
            getRealRoot.put(i,realRoot);
        }
    }

    public static void markPellets(int src, int dest, int[][] next, int[][] last, HashMap<Integer,Boolean> takenPellets) {
        //multiple pacs don't count the same pellet in greedy
        int step = 0;
        int i = next[src][dest];
        if(i == -1) {
            System.err.println("WARNING! Wall provided in markPellets");
            return;
        }
        //fwd count [ends until i]
        while( step < Globs.fwdPelletZero && i != dest) {
            takenPellets.put(i,true);
            i = next[i][dest];
            step++;
        }
        //bwd count [not crossing i]
        if(i != dest) {
            int j = last[src][dest];
            step = 0;
            //bwd count [ends until j]
            while(step < Globs.bwdPelletZero && j != i) {
                takenPellets.put(j,true);
                j = last[src][j];
                step++;
            }
            if(step < Globs.bwdPelletZero) {
                takenPellets.put(j,true);
            }
        }

        if(Globs.destPelletZero) {
            takenPellets.put(dest,true);
        }
    }
    /*
    public static ArrayList<Root> bridgeFind(ArrayList<Integer>[] adjList) {
        int cells = adjList.length;
        boolean[] visited = new boolean[cells];
        int[] timeIn = new int[cells];
        int[] low = new int[cells];
        int timer = 0, start = 0, parent = -1;
        for (int i = 0; i < cells; i++) {
            visited[i] = false;
        }
        ArrayList<Root> bridgeList = new ArrayList<>();
        bridgeFindDfs(adjList, bridgeList, visited, timeIn, low, timer, start, parent);
        return bridgeList;
    }

    private static void bridgeFindDfs(ArrayList<Integer>[] adjList, ArrayList<Root> bridgeList,
                                      boolean[] visited, int[] timeIn, int[] low, int timer, int start, int parent) {
        visited[start] = true;
        timeIn[start] = low[start] = timer++;
        for(Integer to : adjList[start]) {
            if(to == parent)  continue;
            if(visited[to]) {
                low[start] = Math.min(low[start], timeIn[to]);
            }
            else {
                bridgeFindDfs(adjList, bridgeList, visited, timeIn, low, timer, to, start);
                low[start] = Math.min(low[start], low[to]);
                if(low[to] > timeIn[start]) {
                    bridgeList.add(new Root(start,to));
                }
            }
        }
        //looped through each edge of start
    }
    //end function
    */  //bridge-finding algorithm for cavePlus [want to avoid for TLE risk]

    public static int reflectedCell(int cell, int width) {
        Coordinate xy = decode(cell, width);
        xy.x = width - xy.x - 1;  //reflection along middle-Y
        return encode(xy,width);
    }

    public static Coordinate reflectedCoord(Coordinate xy, int width) {
        return new Coordinate(width - xy.x - 1, xy.y);
    }

    public static void predictCorridorPellet(Root root, HashMap<Root, Root> rootPairs, double[] pellet,
                                             ArrayList<Integer>[] adjacentList,
                                             HashMap<Root, ArrayList<Integer>> caveMap,
                                             HashMap<Root, ArrayList<Integer>> pipeMap) {
        if(!rootPairs.containsKey(root)) {  //cave found
            ArrayList<Integer> cavePath = caveMap.get(root);
            if(cavePath == null) {  //debug check
                System.err.println("Error in predictCorridorPellet: Cave not found.");
                return;
            }
            double lastPellet = -Globs.INF;  //must NOT be a possible pellet value
            int lastCell = -1;
            boolean foundZero = false, foundOne = false;
            for(int i : cavePath) {
                if(pellet[i] == Globs.superPelletWeight) {
                    lastPellet = 1;
                    lastCell = i;
                    if(foundZero) {
                        foundZero = false;
                        foundOne = true;
                    }
                    continue;
                }
                if(foundZero) {
                    if(pellet[i] == 0 || pellet[i] == 1)  {
                        System.err.println("WARNING! Cave structure inconsistency.");
                        break;
                    }
                    pellet[i] = Globs.caveZeroWt;
                }
                else if(foundOne) {
                    if(pellet[i] == 0 || pellet[i] == 1)  {
                        System.err.println("WARNING! Cave structure inconsistency.");
                        break;
                    }
                    pellet[i] = Globs.caveOneWt;
                }
                else if(pellet[i] != 0 && pellet[i] != 1) { //1 & 0 pellet values must be reserved for known pellets & blanks
                    if(lastPellet == -Globs.INF)  break;
                    else if(lastPellet == 0 && adjacentList[lastCell].size() <= 2) {
                        if(adjacentList[lastCell].size() < 2) {  //debug check
                            System.err.println("Error in predictCorridorPellet: Wrong cave structure.");
                            break;
                        }
                        foundZero = true;
                        pellet[i] = Globs.caveZeroWt;
                    }
                    else if(lastPellet == 1) {
                        foundOne = true;
                        pellet[i] = Globs.caveOneWt;
                    }
                    else  break;
                }
                lastPellet = pellet[i];
                lastCell = i;
            }
            //looped through cavePath [pellet weights updated.]

            if(Globs.reZeroCavePipe) {  //revert to blindPelletWt [0 at real joint cell root]
                if (pellet[cavePath.get(0)] == 0 && adjacentList[cavePath.get(0)].size() > 2) {
                    for (int i = 1; i < cavePath.size(); i++) {
                        if (pellet[cavePath.get(i)] == Globs.caveOneWt)
                            pellet[cavePath.get(i)] = Globs.blindPelletWt;
                        else break;
                    }
                }
            }
        }
        //reversion to blindPellet done [if flag is enabled]

        //cave work done
        else {  //pipe found

            Root root2 = rootPairs.get(root);
            ArrayList<Integer>[] pipePath = new ArrayList[]{pipeMap.get(root),pipeMap.get(root2)};

            if(pipePath[0] == null || pipePath[1] == null) {  //debug check
                System.err.println("Error in predictCorridorPellet: Pipe(s) not found.");
                return;
            }
            double[] lastPellet = new double[]{-Globs.INF, -Globs.INF};  //must NOT be a possible pellet value
            int[] lastCell = new int[]{-1, -1};  //must NOT be a possible pellet value
            int[] depth = new int[]{0,0};
            boolean[] foundZero = new boolean[]{false,false}, foundOne = new boolean[]{false, false};
            for(int entry = 0; entry < 2; entry++) {
                for (int i : pipePath[entry]) {
                    if(pellet[i] == Globs.superPelletWeight) {
                        lastPellet[entry] = 1;
                        lastCell[entry] = i;
                        depth[entry]++;
                        continue;
                    }
                    if (pellet[i] != 0 && pellet[i] != 1) { //1 & 0 pellet values must be reserved for known pellets & blanks
                        if (lastPellet[entry] == 0 && adjacentList[lastCell[entry]].size() <= 2) {
                            if (adjacentList[lastCell[entry]].size() < 2) {  //debug check
                                System.err.println("Error in predictCorridorPellet: Wrong pipe structure.");
                                break;
                            }
                            foundZero[entry] = true;
                        }
                        else if (lastPellet[entry] == 1) {
                            foundOne[entry] = true;
                        }
                        break;
                    }
                    lastPellet[entry] = pellet[i];
                    lastCell[entry] = i;
                    depth[entry]++;
                }
                //looped through pipePath [one-way] [pellet weights not updated.]
            }
            //looped through pipePath [both-way] [pellet weights not updated.]
            if( (foundZero[0] && foundOne[1]) || (foundZero[1] && foundOne[0] ) ) {  //conflict
                //doing blindPelletWeight with pipePath[0]  //any one is alright
                for (int i = depth[0]; i < pipePath[0].size(); i++) {
                    if(pellet[i] == Globs.superPelletWeight)  continue;
                    if(pellet[i] == 0 || pellet[i] == 1)  break;
                    pellet[i] = Globs.blindPelletWt;
                }
            }
            else if(foundZero[0] || foundZero[1]) {  //0 prediction [without conflict]
                //doing pipeZeroWeight with pipePath[0]  //any one is alright
                int foundIndex = 1;
                if(foundZero[0])  foundIndex = 0;
                boolean triggerOtherEnd = false;
                for (int i = depth[foundIndex]; i < pipePath[foundIndex].size(); i++) {
                    if(pellet[pipePath[foundIndex].get(i)] == Globs.superPelletWeight)  {
                        triggerOtherEnd = true;
                        break;
                    }
                    if(pellet[pipePath[foundIndex].get(i)] == 0 || pellet[pipePath[foundIndex].get(i)] == 1)  break;
                    pellet[pipePath[foundIndex].get(i)] = Globs.pipeZeroWt;
                }
                if(triggerOtherEnd) {
                    foundIndex = 1 - foundIndex;
                    for (int i = depth[foundIndex]; i < pipePath[foundIndex].size(); i++) {
                        if(pellet[pipePath[foundIndex].get(i)] == Globs.superPelletWeight)  break;
                        if(pellet[pipePath[foundIndex].get(i)] == 0 || pellet[pipePath[foundIndex].get(i)] == 1)  break;
                        pellet[pipePath[foundIndex].get(i)] = Globs.pipeZeroWt;
                    }
                }
            }
            else if(foundOne[0] || foundOne[1]) {  //1 prediction [without conflict]
                int foundIndex = 1;
                if(foundOne[0])  foundIndex = 0;
                //doing pipeOneWeight with pipePath[0]  //any one is alright
                for (int i = depth[foundIndex]; i < pipePath[foundIndex].size(); i++) {
                    if(pellet[pipePath[foundIndex].get(i)] == Globs.superPelletWeight)  continue;
                    if(pellet[pipePath[foundIndex].get(i)] == 0 || pellet[pipePath[foundIndex].get(i)] == 1)  break;
                    pellet[pipePath[foundIndex].get(i)] = Globs.pipeOneWt;
                }
            }
            if(Globs.reZeroCavePipe) {  //revert to blindPelletWt [0 at real joint cell root]
                for (int entry = 0; entry < 2; entry++) {
                    if (pellet[pipePath[entry].get(0)] == 0 && adjacentList[pipePath[entry].get(0)].size() > 2) {
                        for (int i = 1; i < pipePath[entry].size(); i++) {
                            if (pellet[pipePath[entry].get(i)] == Globs.pipeOneWt)
                                pellet[pipePath[entry].get(i)] = Globs.blindPelletWt;
                            else break;
                        }
                    }
                }
            }
            //reversion to blindPellet done [if flag is enabled]
        }
        //pipe work done
    }
    //end function
    public static void xpolForOpponent(int oppCell, Type theirType, int theirSpeed, int theirCool, ArrayList<Integer> oppCorridor,
                                       double[] pellet, int width, Path path, Queue<Coordinate> myXY, Queue<Type> myType,
                                       Queue<Integer> mySpeed) {
        //static 0 for opponent in corridor
        //NEED UPDATE: better algo. [Refer to oppXpolOneWt]
        //NEED UPDATE: Work with individualSpeedTurnsLeft for super-pellet
        //evaluate eatability [for each cell in oppCorridor]
        for(int cell: oppCorridor) {
            if(pellet[cell] == 0)  continue;
            if(pellet[cell] > 0) {
                boolean iCanEat = false;
                Iterator<Integer> iterMySpeed = mySpeed.iterator();
                Iterator<Type> iterMyType = myType.iterator();
                //keep comparing distance of cell for each myPac [with oppPac]
                for(Coordinate myLoc: myXY) {
                    int myCell = Ops.encode(myLoc, width);
                    if(!iterMySpeed.hasNext()) {
                        System.err.println("Queue length mismatch for myXY and mySpeed in xpolForOpponent");
                        break;
                    }
                    int oneSpeed = iterMySpeed.next();
                    if(!iterMyType.hasNext()) {
                        System.err.println("Queue length mismatch for myXY and myType in xpolForOpponent");
                        break;
                    }
                    Type oneType = iterMyType.next();
                    double myDist = path.dist[myCell][cell];
                    int myTurn;
                    if(oneSpeed >= myDist / 2) {
                        if(oneSpeed <= 0) {  //debug check
                            System.err.println("Error in xpolForOpponent: Unexpected behaviour of oneSpeed");
                            continue;
                        }
                        myTurn = (int) Math.ceil(myDist/2);
                    }
                    else {
                        myTurn = (int)myDist - oneSpeed;
                    }
                    double theirDist = path.dist[oppCell][cell];
                    int theirTurn;
                    if(theirSpeed >= theirDist / 2) {
                        if(theirSpeed <= 0) {  //debug check
                            System.err.println("Error in xpolForOpponent: Unexpected behaviour of theirSpeed");
                            continue;
                        }
                        theirTurn = (int) Math.ceil(theirDist/2);
                    }
                    else {
                        theirTurn = (int)theirDist - theirSpeed;
                    }
                    if(myTurn <= theirTurn) {  //NEED UPDATE: Eating code could be tweaked here [not recommended. may have issues]
                        //check if pacMan falls in path, if true -> check if eatable [with pac!]
                        int now = myCell, next = path.next[myCell][cell];
                        while(next != oppCell && next != cell) {
                            now = next;
                            next = path.next[now][cell];
                        }
                        if(next == oppCell && next == cell) {
                            System.err.println("Error in xPolForOpponent: Same cell contains oppPac & food");
                            continue;
                        }
                        if(next == cell && myTurn < theirTurn) {  //oppPac not in path
                            iCanEat = true;
                            break;
                        }
                        else {  //next == oppCell [oppPac in path! -> check if greater (considering speed & coolDown)]
                            if(!oneType.gt(theirType))  continue;
                            int eatTurn = 0;  ///eatTurn for cannot eat
                            if(oneSpeed > theirSpeed && oneSpeed-theirSpeed >= path.dist[myCell][oppCell]) {
                                eatTurn = theirSpeed + path.dist[myCell][oppCell];
                            }
                            if(eatTurn > 0 && eatTurn <= theirCool) {
                                iCanEat = true;
                                break;
                            }
                            else continue;
                        }
                    }
                    //myTurn <= theirTurn analyzed
                    else continue;
                }
                //eatability for "cell" determined
                if(iCanEat) {
                    pellet[cell] = Globs.oppFoodEatWt;
                }
                else  pellet[cell] = Globs.oppXpolZeroWt;
            }
            //for pellet[cell] > 0, pellet[cell] reWeighted [for a cell]
        }
        //all cells in oppCorridor extrapolated
    }
    //end function
}

/**
 * Grab the pellets as fast as you can!
 **/
class Player {

    public static void main(String args[]) throws Exception {
        //System.err.println("Checkpost 0");
        //System.err.flush();
        Scanner in = new Scanner(System.in);
        int width = in.nextInt(); // size of the grid
        Globs.width = width;  //debugging purposes
        int height = in.nextInt(); // top left corner is (x=0, y=0)
        if (in.hasNextLine()) {
            in.nextLine();
        }
        //initializations
        int cells = width * height;
        int[][] rawBoard = new int[width][];
        for(int i = 0; i < width; i++) {
            rawBoard[i] = new int[height];
        }
        int[] cellBoard = new int[cells];
        for (int i = 0; i < cells; i++) {
            cellBoard[i] = 0;
        }
        //width is x, height is y
        //floor is 0, wall is 1
        Board board = new Board(width,height,rawBoard);  //for passing args to functions

        //rawBoard buildUp
        for (int i = 0, k = 0; i < height; i++) {
            String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for(int j = 0; j < width; j++,k++) {
                if(row.charAt(j) == ' ') {
                    rawBoard[j][i] = 0;
                    cellBoard[k] = 0;
                }
                else {
                    rawBoard[j][i] = 1;
                    cellBoard[k] = 1;
                }
            }
            if(i == height - 1 && k != cells) {
                System.err.println("cellBoard construction malfunctioning.");
            }
        }

        //game-start initializations
        ArrayList<Integer>[] adjacentList = (ArrayList<Integer>[]) new ArrayList[cells];
        int[][] dist = new int[cells][];
        int[][] next = new int[cells][];  //FORWARD path construct (NODE after first)
        int[][] last = new int[cells][];  //BACKWARD path construct (NODE before last)

        Path path = new Path(dist,next,last);  //for passing args to functions

        //System.err.println("Checkpost 0.1");
        //System.err.flush();
        for(int i = 0; i < cells; i++) {
            dist[i] = new int[cells];
            next[i] = new int[cells];
            last[i] = new int[cells];
            for(int j = 0; j < cells; j++) {
                dist[i][j] = Globs.INF;
                next[i][j] = -1;
                last[i][j] = -1;
            }
        }
        double[] pellet  = new double[cells];  //0 = no pellet, 1 = pellet, superPelletWeight = big pellet

        //set all pellets
        for(int i = 0; i < cells; i++) {
            Coordinate loc = Ops.decode(i,width);
            if(rawBoard[loc.x][loc.y] == 0)
                pellet[i] = Globs.blindPelletWt;
            else
                pellet[i] = 0;
        }
        //System.err.println("Checkpost 0.2");
        //System.err.flush();
        //graph buildUp [with adjacentList]
        for (int i = 0; i < cells; i++) {
            adjacentList[i] = Ops.getAdjacentCells(i, board);
        }

        //Floyd-Warshall on "dist";
        //with "next" as FWD path
        //with "last" as BWD path
        for(int i = 0; i < cells; i++) {
            dist[i][i] = 0;
            next[i][i] = i;
            last[i][i] = i;
            for(Integer j : adjacentList[i]) {
                dist[i][j] = 1;
                next[i][j] = j;
                last[i][j] = i;
            }
        }
        //System.err.println("Checkpost 0.25 -> " + "cells = " + cells);
        //System.err.flush();
        for(int k = 0; k < cells; k++) {
            if(cellBoard[k] == 1)  continue;
            for(int i = 0; i < cells; i++) {
                if(cellBoard[i] == 1)  continue;
                for(int j = 0; j < cells; j++) {
                    if(dist[i][j] > dist[i][k] + dist[k][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        next[i][j] = next[i][k];
                        last[i][j] = last[k][j];
                    }
                }
            }
            if(k%(cells/10) == 0) {
                //System.err.println("Checkpost 0.28 -> " + k);
                //System.err.flush();
            }
        }
        //System.err.println("Checkpost 0.3");
        //System.err.flush();
        //game start initializations 2
        //ArrayList<Root> bridgeList = Ops.bridgeFind(adjacentList);  //(root--next) forms a bridge edge

        HashMap< Root, ArrayList<Integer> > caveMap = new HashMap<>();  //queue structure
        HashMap< Root, ArrayList<Integer> > realCaveMap = new HashMap<>();  //queue structure
        HashMap< Root, ArrayList<Integer> > pipeMap = new HashMap<>(64);  //computed from both ends  //queue structure
        //HashMap< Root, ArrayList<Integer> > cavePlusMap = new HashMap<>();  //[from bridge of graph] //disjoint set with head

        //INFO: cavePlus includes all caves [in a disjoint set form, rather than a queue form]

        HashMap<Integer,Root> getRoot = new HashMap<>(cells);  //cell->root [cave & pipe only]
        //[refers to only one root for pipe]
        HashMap<Integer,Root> getRealRoot = new HashMap<>(cells/2);  //cell->root  [real cave only]

        HashMap< Integer, ArrayList<Root> > getRoots = new HashMap<>();  //cell->roots
        // [only for cells with multiple roots (only most real joint cells)]

        HashMap<Root,Root> rootPairs = new HashMap<>();  //root pairs for pipe [contains both perms]
        //HashMap<Integer,HashMap<Root,Boolean>> getEntry = new HashMap<>();  //cell->entry [cavePlus only]

        //CavePlus computation [to be done]

        //game-start initializations 3
        Queue<Coordinate> myXY = new LinkedList<>(), oppXY = new LinkedList<>();
        Queue<Integer> myPac = new LinkedList<>(), oppPac = new LinkedList<>();
        ArrayList<Integer> myPacList = new ArrayList<>();
        Queue<Type> myType = new LinkedList<>(), oppType = new LinkedList<>();
        ArrayList<Type> myTypeList = new ArrayList<>();
        Queue<Integer> mySpeed = new LinkedList<>(), oppSpeed = new LinkedList<>();
        ArrayList<Integer> mySpeedList = new ArrayList<>();
        Queue<Integer> myCool = new LinkedList<>(), oppCool = new LinkedList<>();

        Deque<Coordinate> myXYHistory[] = null;  //new histories are added at head

        HashMap<Integer,Boolean> usedNext = new HashMap<>();  //avoid own team collision (->O<- type)
        HashMap<Integer,Boolean> takenPellets = new HashMap<>( (height+width) * 4 );
        int turnMod10 = -1;

        int[] dangerDist = null;

        boolean firstTurn = true;
        //System.err.println("Checkpost 0.4");
        //System.err.flush();
        // game loop [per turn]
        while (true) {
            //my initializations
            myXY.clear();       oppXY.clear();
            myPac.clear();      oppPac.clear();
            myPacList.clear();
            myType.clear();     oppType.clear();
            myTypeList.clear();
            mySpeed.clear();    oppSpeed.clear();
            mySpeedList.clear();
            myCool.clear();     oppCool.clear();

            usedNext.clear();
            takenPellets.clear();
            //update turnMod10  [speedUp when turnMod = 0, except if danger <= dangerOverSpeed]
            turnMod10++;
            turnMod10 %= 10;
            //avoid being eaten (init.)
            //dontGo.clear();

            //System input per turn
            int myScore = in.nextInt();
            int opponentScore = in.nextInt();
            int visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            boolean globalMySpeedLeft = false;  //used for modifying danger distance

            if(firstTurn) {  //enter only once
                myXYHistory = new LinkedList[visiblePacCount];  //visiblePacCount <= totalPacCount
                for (int pacI = 0; pacI < visiblePacCount; pacI++) {
                    myXYHistory[pacI] = new LinkedList<Coordinate>();
                    for (int loop = 0; loop < Globs.xyHistorySize; loop++) {
                        myXYHistory[pacI].add(new Coordinate());  //adding dummy invalid coordinates
                    }
                }
            }

            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt(); // pac number (unique within a team)
                boolean mine = in.nextInt() != 0; // true if this pac is yours
                int x = in.nextInt(); // position in the grid
                int y = in.nextInt(); // position in the grid
                String typeId = in.next(); // unused in wood leagues
                int speedTurnsLeft = in.nextInt(); // unused in wood leagues

                if(mine && speedTurnsLeft > 0) {
                    globalMySpeedLeft = true;
                }
                int abilityCooldown = in.nextInt(); // unused in wood leagues
                //all input taken

                if(mine && !typeId.equals("DEAD")) {  //coping with new rules of silver league
                    myXY.add(new Coordinate(x,y));
                    myPac.add(pacId);
                    myPacList.add(pacId);
                    myType.add(Ops.getType(typeId));
                    myTypeList.add(Ops.getType(typeId));
                    mySpeed.add(speedTurnsLeft);
                    mySpeedList.add(speedTurnsLeft);
                    myCool.add(abilityCooldown);

                    myXYHistory[pacId].addFirst(new Coordinate(x,y));
                    myXYHistory[pacId].removeLast();
                }
                else if(!typeId.equals("DEAD") && !firstTurn) {  //coping with new rules of silver league
                    oppXY.add(new Coordinate(x,y));
                    oppPac.add(pacId);  //probably not needed
                    oppType.add(Ops.getType(typeId));
                    oppSpeed.add(speedTurnsLeft);
                    oppCool.add(abilityCooldown);
                }
                if(firstTurn) {
                    for(Coordinate oneXY: myXY) {
                        oppXY.add(Ops.reflectedCoord(oneXY, width));
                    }
                    oppPac.addAll(myPac);
                    oppType.addAll(myType);
                    oppSpeed.addAll(mySpeed);
                    oppCool.addAll(myCool);
                }
            }
            //System.err.println("Checkpost 1");
            //System.err.flush();

            //Init. danger distance
            if(firstTurn) {
                dangerDist = new int[myXY.size()];
                int index = 0;
                for(int speed : mySpeed) {
                    dangerDist[index] = Globs.dangerDist + ((speed > 0)? 0 : -1);
                }
            }

            if(firstTurn) {
                HashMap<Integer, Boolean> myPacStart = new HashMap<>();
                HashMap<Integer, Boolean> oppPacStart = new HashMap<>();
                for(Coordinate pacXY: myXY) {
                    int pacCell = Ops.encode(pacXY, width);
                    myPacStart.put(pacCell, true);
                    oppPacStart.put(Ops.reflectedCell(pacCell, width), true);
                }
                HashMap<Integer, Boolean> rootCells = new HashMap<>(cells/2);  //pacStarts are considered in calculation
                for (int i = 0; i < cells; i++) {
                    if(adjacentList[i].size() > 2 && !myPacStart.containsKey(i) && !oppPacStart.containsKey(i)) {
                        rootCells.put(i,true);
                    }
                }

                for(Integer pacCell : myPacStart.keySet()) {
                    for(Integer adjPacCell : adjacentList[pacCell]) {
                        rootCells.put(adjPacCell,true);
                    }
                }

                for(Integer pacCell : oppPacStart.keySet()) {
                    for(Integer adjPacCell : adjacentList[pacCell]) {
                        rootCells.put(adjPacCell,true);
                    }
                }

                HashMap<Root,Boolean> prospectRoots = new HashMap<>();  //contains each root with next (in direction) element
                for (int rootCell : rootCells.keySet()) {
                    for(int nextAble: adjacentList[rootCell]) {
                        if(adjacentList[nextAble].size() <= 2
                                && !myPacStart.containsKey(nextAble) && !oppPacStart.containsKey(nextAble)) {
                            prospectRoots.put(new Root(rootCell,nextAble), true);
                        }
                    }
                }

                //Corridor [Cave,Pipe] computation
                for(Root root : prospectRoots.keySet()) {
                    if(rootPairs.containsKey(root))  continue;  //pipe already counted for the other end
                    int now = root.root, forward = root.next;
                    ArrayList<Integer> corridor = new ArrayList<>(Collections.singletonList(now));
                    while(true) {
                        ArrayList<Integer> forwardAdjList = new ArrayList<>(adjacentList[forward]);
                        if(myPacStart.containsKey(forward) || oppPacStart.containsKey(forward)) {  //pipe found
                            //[ending before a pacStart] [at least 2 length]
                            for(int cell: corridor) {  //setting up getRoot
                                getRoot.put(cell, root);
                            }
                            pipeMap.put(root, corridor);
                            //setting up getRoots
                            int corridorEntry = corridor.get(0);
                            if(adjacentList[corridorEntry].size() > 2) {
                                if(!getRoots.containsKey(corridorEntry)) {
                                    getRoots.put(corridorEntry, new ArrayList<>(4));
                                }
                                getRoots.get(corridorEntry).add(root);
                            }
                            ArrayList<Integer> corridor2 = new ArrayList<>(corridor);  //pipe from the other end
                            Collections.reverse(corridor2);
                            Root root2 = new Root(corridor2.get(0), corridor2.get(1));  //root from the other end
                            pipeMap.put(root2, corridor2);
                            rootPairs.put(root, root2);  //setting up rootPair
                            rootPairs.put(root2, root);  //setting up rootPair
                            break;
                        }
                        else if(forwardAdjList.size() == 1) {  //cave found [at least 2 length]
                            corridor.add(forward);
                            for(int cell : corridor) {  //setting up getRoot
                                getRoot.put(cell, root);
                            }
                            caveMap.put(root, corridor);
                            //setting up getRoots
                            int corridorEntry = corridor.get(0);
                            if(adjacentList[corridorEntry].size() > 2) {
                                if(!getRoots.containsKey(corridorEntry)) {
                                    getRoots.put(corridorEntry, new ArrayList<>(4));
                                }
                                getRoots.get(corridorEntry).add(root);
                            }
                            break;
                        }
                        else if(forwardAdjList.size() > 2) {  //pipe found [at least 2 length]  //add from both sides
                            corridor.add(forward);
                            for(int cell: corridor) {  //setting up getRoot
                                getRoot.put(cell, root);
                            }
                            pipeMap.put(root, corridor);
                            //setting up getRoots
                            int corridorEntry = corridor.get(0);
                            if(adjacentList[corridorEntry].size() > 2) {
                                if(!getRoots.containsKey(corridorEntry)) {
                                    getRoots.put(corridorEntry, new ArrayList<>(4));
                                }
                                getRoots.get(corridorEntry).add(root);
                            }
                            ArrayList<Integer> corridor2 = new ArrayList<>(corridor);  //pipe from the other end
                            Collections.reverse(corridor2);
                            Root root2 = new Root(corridor2.get(0), corridor2.get(1));  //root from the other end
                            pipeMap.put(root2, corridor2);
                            //setting up getRoots
                            corridorEntry = corridor2.get(0);
                            if(adjacentList[corridorEntry].size() > 2) {
                                if(!getRoots.containsKey(corridorEntry)) {
                                    getRoots.put(corridorEntry, new ArrayList<>(4));
                                }
                                getRoots.get(corridorEntry).add(root2);
                            }
                            rootPairs.put(root, root2);  //setting up rootPair
                            rootPairs.put(root2, root);  //setting up rootPair
                            break;
                        }
                        else {  //keep going forward  //forwardAdjList.size() == 2
                            int finalNow = now;
                            forwardAdjList.removeIf(i -> i.equals(finalNow));
                            now = forward;
                            forward = forwardAdjList.remove(0);
                            corridor.add(now);
                        }
                    }
                }
                //Corridor [Cave,Pipe] computation done

                //realCaveMap computation
                caveMap.forEach((root,cave) -> {
                    if(adjacentList[root.root].size() > 2) {
                        realCaveMap.put(root,cave);
                        for(int i : cave) {
                            getRealRoot.put(i,root);
                        }
                    }
                    else {  //adjacentList[root.root].size() == 2
                        int now = root.root, back = root.next;
                        ArrayList<Integer> realCave = Ops.realCaveExtend(back, now, adjacentList, cave);
                        Ops.placeRealCave(realCave, realCaveMap, getRealRoot);
                    }
                });

                for(int myStart : myPacStart.keySet()) {
                    if(adjacentList[myStart].size() == 1) {
                        ArrayList<Integer> realCave = Ops.realCaveForm(myStart, adjacentList);
                        Ops.placeRealCave(realCave, realCaveMap, getRealRoot);
                        continue;
                    }
                    for(int adjacent : adjacentList[myStart]) {
                        if(adjacentList[adjacent].size() == 1) {
                            ArrayList<Integer> realCave = Ops.realCaveForm(adjacent, adjacentList);
                            Ops.placeRealCave(realCave, realCaveMap, getRealRoot);
                        }
                    }
                }
                for(int oppStart : oppPacStart.keySet()) {
                    if(adjacentList[oppStart].size() == 1) {
                        ArrayList<Integer> realCave = Ops.realCaveForm(oppStart, adjacentList);
                        Ops.placeRealCave(realCave, realCaveMap, getRealRoot);
                        continue;
                    }
                    for(int adjacent : adjacentList[oppStart]) {
                        if(adjacentList[adjacent].size() == 1) {
                            ArrayList<Integer> realCave = Ops.realCaveForm(adjacent, adjacentList);
                            Ops.placeRealCave(realCave, realCaveMap, getRealRoot);
                        }
                    }
                }
                //realCaveMap computation done

                //comeOutPipe computation for opponent pacs
                for(Integer pacCell : oppPacStart.keySet()) {
                    ArrayList<Integer> pacAdjList = new ArrayList<Integer>(adjacentList[pacCell]);
                    if(pacAdjList.size() == 2) {
                        pacAdjList.removeIf(i -> adjacentList[i].size() == 1 || ( adjacentList[i].size() == 2 &&
                                getRoot.get(i) != null && caveMap.containsKey(getRoot.get(i)) ) );
                        if(pacAdjList.size() == 0) {  //debug check
                            System.err.println("WARNING! OppPac is imprisoned.");
                            continue;
                        }
                    }
                    if(pacAdjList.size() == 1) {
                        if(adjacentList[pacAdjList.get(0)].size() > 2) {
                            pellet[pacAdjList.remove(0)] = Globs.oppComeOutWt;
                        }
                        else if(adjacentList[pacAdjList.get(0)].size() < 2) {  //debug check
                            System.err.println("Error: oppPac is imprisoned.");
                            continue;
                        }
                        else {  //adjacentList[pacAdjList.get(0)].size() == 2
                            Root oppOutRoot = getRoot.get(pacAdjList.remove(0));
                            if(oppOutRoot == null) {
                                System.err.println("Error: No root found for what should be a pipe.");
                                continue;
                            }
                            ArrayList<Integer> comeOutPipe = pipeMap.get(oppOutRoot);
                            if(comeOutPipe == null)  {
                                System.err.println("Error: comeOutPipe should exist, but not found.");
                                if(caveMap.containsKey(oppOutRoot))
                                    System.err.println("Error: Cave found. oppPac is imprisoned.");
                                continue;
                            }
                            for(Integer i: comeOutPipe) {
                                pellet[i] = Globs.oppComeOutWt;
                            }
                        }
                    }
                }
                //comeOutPipe computation [and pellet prediction] done
                //end of first turn [cave,pipe] analysis

                //setting exploration weights  //NOT WORKING CORRECTLY
                pipeMap.forEach((root,pipe) -> {
                    if(adjacentList[root.root].size() > 2) {
                        pellet[root.root] += pipe.size() * Globs.pipeSeeWt;
                    }
                });
                caveMap.forEach((root,cave) -> {
                    if(adjacentList[root.root].size() > 2) {
                        pellet[root.root] += cave.size() * Globs.CaveSeeWt;
                    }
                });
                //tie-break of exploration weights with 1
                for (int i = 0; i < cells; i++) {
                    if(pellet[i] == 1) {
                        pellet[i] += (Globs.seeWtOverOne? +Globs.delta : -Globs.delta);
                    }
                }
            }
            //end of first turn [cave,pipe] analysis & exploration weight setting
            //System.err.println("Checkpost 2");
            //System.err.flush();

            int visiblePelletCount = in.nextInt(); // all pellets in sight

            //clear super pellets
            for(int i = 0; i < cells; i++) {
                if(pellet[i] == Globs.superPelletWeight) {
                    pellet[i] = 0;
                }
            }

            HashMap<Root, Boolean> rootCheck = new HashMap<>();  //roots [for corridors] to be checked this turn
            //any one root for each pipe
            //clear all visible pellets and populate rootCheck
            for(Coordinate xy: myXY) {
                Queue<Integer> visibleCells = Ops.getVisibleCells(Ops.encode(xy,width), board);
                for(Integer i: visibleCells) {
                    pellet[i] = 0;
                    //populate rootCheck
                    if(adjacentList[i].size() > 2 && getRoots.containsKey(i)) {
                        for(Root root : getRoots.get(i)) {
                            rootCheck.put(root,true);
                        }
                    }
                    else if(adjacentList[i].size() > 2 && getRoot.containsKey(i)) {  //debug check
                        System.err.println("Error: Real joint cell is in \"getRoot\" but not in \"getRoots\"");
                    }
                    else if(getRoot.containsKey(i)) {  //adjacentList[i].size() <= 2
                        Root root = getRoot.get(i);
                        rootCheck.put(root, true);
                    }
                    //rootCheck populated
                }
            }

            //take only one root of a rootPair in rootCheck
            rootPairs.forEach((root, root2) -> {
                if(rootCheck.containsKey(root)) {
                    rootCheck.remove(root2);
                }
            });
            //rootCheck fully constructed.

            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth
                //update pellet array
                if(value == 10)  pellet[Ops.encode(x,y,width)] = Globs.superPelletWeight;
                else  pellet[Ops.encode(x,y,width)] = value;
            }

            //Corridor [cave,pipe] prediction
            for(Root root : rootCheck.keySet()) {
                Ops.predictCorridorPellet(root,rootPairs,pellet,adjacentList,caveMap,pipeMap);  //LOL!
            }

            //opponent in corridor extrapolation
            Iterator<Integer> iterOppSpeed = oppSpeed.iterator();
            Iterator<Type> iterOppType = oppType.iterator();
            Iterator<Integer> iterOppCool = oppCool.iterator();
            for(Coordinate enemyXY : oppXY) {
                if(!iterOppSpeed.hasNext()) {
                    System.err.println("Queue length mismatch for oppXY and oppSpeed");
                    break;
                }
                int theirSpeed = iterOppSpeed.next();
                if(!iterOppType.hasNext()) {
                    System.err.println("Queue length mismatch for oppXY and oppType");
                    break;
                }
                Type theirType = iterOppType.next();
                if(!iterOppCool.hasNext()) {
                    System.err.println("Queue length mismatch for oppXY and oppCool");
                }
                int theirCool = iterOppCool.next();
                int enemyCell = Ops.encode(enemyXY, width);
                if(adjacentList[enemyCell].size() <= 2 && getRoot.containsKey(enemyCell)) { //size < 2 should be redundant for now
                    Root oppCorrRoot = getRoot.get(enemyCell);
                    if(caveMap.containsKey(oppCorrRoot))
                        Ops.xpolForOpponent(enemyCell, theirType, theirSpeed, theirCool, caveMap.get(oppCorrRoot),
                                pellet, width, path, myXY, myType, mySpeed);
                    else if(pipeMap.containsKey(oppCorrRoot))
                        Ops.xpolForOpponent(enemyCell, theirType, theirSpeed, theirCool,  pipeMap.get(oppCorrRoot),
                                pellet, width, path, myXY, myType, mySpeed);
                    else
                        System.err.println("Error: Neither cave nor pipe found for a valid root.");
                }
            }
            //System.err.println("Checkpost 3");
            //System.err.flush();

            //per turn init.
            int pacCount = myXY.size();
            boolean[] pacMove = new boolean[pacCount];
            for (int i = 0; i < pacCount; i++) {
                pacMove[i] = false;
            }
            /*int dangerDist = Globs.dangerDist;
            if(!globalMySpeedLeft) {
                dangerDist--;
            }*/

            //declare per game turn
            int[] myCell = new int[pacCount];
            int[] nextCell = new int[pacCount];
            int[] minDist = new int[pacCount];
            int[] minDistPlus = new int[pacCount];  //minimum distance + backup distance [used in computing gain]
            double[] maxPellet = new double[pacCount];
            Iterator<Coordinate> iterMyXY = myXY.iterator();

            int[] dangerUp = new int[pacCount];  //upcoming danger distance
            int[] dangerNext = new int[pacCount];  //next move into danger
            int[] predator = new int[pacCount];  //upcoming predator
            int[] eatUp = new int[pacCount];  //upcoming eating distance
            int[] eatNext = new int[pacCount];  //next move into eating
            int[] foodPac = new int[pacCount];  //upcoming food
            Type[] closeType = new Type[pacCount];  //pac type of close opponent [based on dangerDist]
            int[] closeDist = new int[pacCount];  //distance of close opponent [based on dangerDist]

            //init. per game turn [greedy parameters and close-eat-danger]
            for(int i = 0; i < pacCount; i++) {
                if(!iterMyXY.hasNext()) {
                    System.err.println("Length mismatch for pacCount and myXY");
                    break;
                }
                myCell[i] = Ops.encode(iterMyXY.next(), width);
                nextCell[i] = myCell[i];  //DEFAULT MOVE: Don't move
                //NEED UPDATE: Could use random move
                //nextCell[i] = Ops.generateRandom(0, cells - 1);  //target random cell if no pellet

                //minDist[i] = Globs.INF;  //updated per turn later
                //minDistPlus[i] = Globs.INF;    //updated per turn later
                //maxPellet[i] = 0;    //updated per turn later

                dangerUp[i] = Globs.INF;
                dangerNext[i] = -1;
                predator[i] = -1;
                eatUp[i] = Globs.INF;
                eatNext[i] = -1;
                foodPac[i] = -1;
                closeType[i] = null;  //NEED UPDATE: Unnecessary, as done by default
                closeDist[i] = Globs.INF;
            }


            Iterator<Type> iterMyType = myType.iterator();
            //Iterator<Type> iterOppType;
            iterMyXY = myXY.iterator();
            //recording possible charge and escape conditions [incl. SWITCH]
            for (int i = 0; i < pacCount; i++) {
                if(!iterMyType.hasNext()) {
                    System.err.println("Length mismatch for pacCount and myType");
                    break;
                }
                Type oneType = iterMyType.next();
                if(!iterMyXY.hasNext()) {
                    System.err.println("Length mismatch for pacCount and myXY");
                    break;
                }
                int myPos = Ops.encode(iterMyXY.next(),width);
                iterOppType = oppType.iterator();
                iterOppSpeed = oppSpeed.iterator();
                for (Coordinate oppLoc : oppXY) {
                    if (!iterOppType.hasNext()) {
                        System.err.println("Queue length mismatch for oppXY and oppType");
                        break;
                    }
                    Type theirType = iterOppType.next();
                    if (!iterOppSpeed.hasNext()) {
                        System.err.println("Queue length mismatch for oppXY and oppSpeed");
                        break;
                    }
                    int theirSpeed = iterOppSpeed.next();

                    //if (!theirType.eq(oneType)) continue;  //ignore if equal type

                    int oppCell = Ops.encode(oppLoc, width);
                    int oppDist = dist[myCell[i]][oppCell];
                    if( oppDist <= (theirSpeed > 0? Globs.switchDistance : Globs.switchDistance-1) ) {
                        //System.err.println("ENEMY PAC UP CLOSE");
                        //randomly switches for one opponent
                        //NEED UPDATE [based on otherwise-greedy direction]
                        if(oppDist < closeDist[i] || (oppDist == closeDist[i] && Ops.generateRandom(0,1) == 1) ) {
                            closeDist[i] = oppDist;
                            closeType[i] = theirType;
                        }
                    }
                    if (oppDist <= ( dangerDist[i] + (theirSpeed > 0? 0 : -1) ) ) {
                        //ignores equal type pacs
                        if(theirType.lt(oneType)) {  //charge if lower type  [CODING NOT DONE] [Good?] [NEED UPDATE]
                            //System.err.println("CAN CHARGE ENEMY PAC");
                            if(oppDist < eatUp[i] || (oppDist == eatUp[i] && Ops.generateRandom(0,1) == 1) ) {
                                eatUp[i] = oppDist;
                                eatNext[i] = next[myPos][oppCell];
                                foodPac[i] = oppCell;
                            }
                        }
                        else if(theirType.gt(oneType)) {  //escape if higher type
                            //System.err.println("CAN AVOID DANGER");
                            //randomly avoids one danger
                            //NEED UPDATE [avoid all randers]
                            if (oppDist < dangerUp[i] || (oppDist == dangerUp[i] && Ops.generateRandom(0,1) == 1) ) {
                                dangerUp[i] = oppDist;
                                dangerNext[i] = next[myPos][oppCell];
                                predator[i] = oppCell;
                            }
                        }
                    }
                }
            }

            //System.err.println("Checkpost 4");
            //System.err.flush();

            //give turn for all pacs
            for(int moveI = 0; moveI < pacCount; moveI++) {
                //init. per pac turn
                for(int i = 0; i < pacCount; i++) {
                    nextCell[i] = myCell[i];  //DEFAULT MOVE: Don't move
                    //NEED UPDATE: Could use random move
                    //nextCell[i] = Ops.generateRandom(0, cells - 1);  //target random cell if no pellet
                    minDist[i] = Globs.INF;
                    minDistPlus[i] = Globs.INF;
                    maxPellet[i] = 0;
                }

                iterMyXY = myXY.iterator();
                Iterator<Integer> iterMyPac = myPac.iterator();
                iterMyType = myType.iterator();
                Iterator<Integer> iterMySpeed = mySpeed.iterator();
                //do greedy for available pacs
                for(int pacNow = 0; pacNow < pacCount; pacNow++) {
                    if(pacMove[pacNow])  continue;  //if pac has already moved, ignore.
                    //get next data
                    if(!iterMyXY.hasNext()) {
                        System.err.println("Length mismatch for pacCount and myXY");
                        break;
                    }
                    Coordinate oneXY = iterMyXY.next();
                    if(!iterMyPac.hasNext()) {
                        System.err.println("Length mismatch for pacCount and myPac");
                        break;
                    }
                    int onePac = iterMyPac.next();
                    if(!iterMyType.hasNext()) {
                        System.err.println("Length mismatch for pacCount and myType");
                        break;
                    }
                    Type oneType = iterMyType.next();
                    if(!iterMySpeed.hasNext()) {
                        System.err.println("Length mismatch for pacCount and mySpeed");
                        break;
                    }
                    int oneSpeed = iterMySpeed.next();
                    for (int i = 0; i < cells; i++) {
                        //greedy choice of next move
                        if (i == myCell[pacNow]) continue;
                        if (pellet[i] == 0) continue;
                        //ignore one danger advance in greedy
                        int move1 = next[myCell[pacNow]][i];
                        if (move1 == dangerNext[pacNow]) continue;
                        //NEED UPDATE: Must ignore multiple danger advances [use arrayList or Queue for dangerNext?]
                        //NEED UPDATE: May encourage eating with a payoff
                        int move2 = next[move1][i];
                        //ignore [->O<-] collision in greedy with 2-step usedNext
                        if(usedNext.containsKey(move1) || usedNext.containsKey(move2)) {
                            continue;
                        }

                        double pelletI = Ops.computeTotalPellet(myCell[pacNow], i, pellet, path, takenPellets);
                        int distOne = dist[myCell[pacNow]][i];
                        if(oneSpeed > 0 && distOne == 1)  distOne = 2;  //1-move consumes 2 moves during speedBoost
                        int distPlus = distOne +
                                Ops.computeBackUpDist(myCell[pacNow],i,path,board,adjacentList, getRealRoot, realCaveMap);

                        if (pelletI / distPlus > maxPellet[pacNow] / minDistPlus[pacNow]) {
                            nextCell[pacNow] = i;
                            minDist[pacNow] = distOne;
                            minDistPlus[pacNow] = distPlus;
                            maxPellet[pacNow] = pelletI;
                        }
                        else if (pelletI / distPlus == maxPellet[pacNow] / minDistPlus[pacNow]) {
                            boolean gt = false, eq = false;  //comparison based on distPlusPriority
                            if(Globs.distPlusPriority) {
                                if(distPlus > minDistPlus[pacNow])  gt = true;
                                else if(distPlus == minDistPlus[pacNow])  eq = true;
                            }
                            else {
                                if(distOne > minDist[pacNow])  gt = true;
                                else if(distOne == minDist[pacNow])  eq = true;
                            }
                            //tie breaking with distance (or distPlus, based on flag) comparison
                            if (gt) {
                                //encourage longer distance [or distPlus] greedy
                                nextCell[pacNow] = i;
                                minDist[pacNow] = distOne;
                                minDistPlus[pacNow] = distPlus;
                                maxPellet[pacNow] = pelletI;
                            } else if (eq) {
                                //randomize for equal distance [or distPlus], equal payoff
                                int choose = Ops.generateRandom(0, 1);
                                if (choose == 1) {
                                    nextCell[pacNow] = i;
                                    minDist[pacNow] = distOne;
                                    minDistPlus[pacNow] = distPlus;
                                    maxPellet[pacNow] = pelletI;
                                }
                            }
                            //distance comparison ended
                        }
                    }
                    //greedy done for all cells of a pac
                }
                //greedy done for available pacs

                //set choosePac to first non-moved pac
                int choosePac = 0;
                while(pacMove[choosePac])  choosePac++;
                for(int i = choosePac+1; i < pacCount; i++) {
                    if(pacMove[i])  continue;  //ignore comparison with moved pacs
                    //comparison with unmoved pacs
                    if(dangerUp[choosePac] < dangerUp[i])  continue;
                    else if(dangerUp[i] < dangerUp[choosePac]) {
                        choosePac = i;
                        continue;
                    }
                    else {
                        //Division by zero will not occur during computing gains here.
                        double gainChoosePac = maxPellet[choosePac]/minDistPlus[choosePac];
                        double gainI = maxPellet[i]/minDistPlus[i];
                        if(gainChoosePac > gainI)  continue;
                        else if(gainI > gainChoosePac)  choosePac = i;
                        else if(Ops.generateRandom(0,1) == 1)  choosePac = i;
                    }
                }
                //pac chosen for moving comparing "danger", then "greedy values"

                //NEED UPDATE: short circuit for speed or switch [before greedy]

                int move1 = next[myCell[choosePac]][nextCell[choosePac]];  //1st move
                int move2 = next[move1][nextCell[choosePac]];  //2nd move

                //print pellet array with characters
                /*if(moveI == 0) {  //debug print must be before all moves have been printed
                    for (int i = 0; i < cells; i++) {
                        if(i != 0 && (i%width) % 5 == 0) {
                            System.err.print(">");
                        }
                        if (i != 0 && i % width == 0) {
                            System.err.println("-->" + (i / width - 1) );
                        }

                        if (pellet[i] == 0) System.err.print("0");
                        else if (pellet[i] == 1) System.err.print("1");
                        else if (pellet[i] == Globs.superPelletWeight) System.err.print("T");
                        else if (pellet[i] == Globs.blindPelletWt) System.err.print("B");
                        else if (pellet[i] == Globs.oppComeOutWt) System.err.print("S");  //S->start
                        else if (pellet[i] == Globs.caveZeroWt) System.err.print("D");
                        else if (pellet[i] == Globs.caveOneWt) System.err.print("C");
                        else if (pellet[i] == Globs.pipeZeroWt) System.err.print("Q");
                        else if (pellet[i] == Globs.pipeOneWt) System.err.print("P");
                        else if (pellet[i] == Globs.oppXpolZeroWt) System.err.print("B");  //B->beta
                        else if (pellet[i] == Globs.oppFoodEatWt) System.err.print("A");  //A->alpha
                        else System.err.print("E");  //Rest->exploration wt
                    }
                    System.err.println("-->" + (height-1) );
                }*/

                //print pellet array with decimals
                /*if(moveI == 0) {  //debug print must be before all moves have been printed
                    for (int i = 0; i < cells; i++) {
                        if(i != 0 && (i%width) % 5 == 0) {
                            System.err.print( (i/width < 10? "0" : "") + "" + (i/width) + "--" + (i%width)/5 + "/");
                        }
                        if (i != 0 && i % width == 0) {
                            System.err.println("-->" + (i / width - 1) );
                        }

                        System.err.print(String.format("%.2f", pellet[i]) + "/");
                    }
                    System.err.println("-->" + (height-1) );
                }*/

                //System.err.println("<" + myPacList.get(choosePac) + "> goes to " + Ops.decode(nextCell[choosePac], width));
                //System.err.println("Pellet: " + maxPellet[choosePac]);
                //System.err.println("Distance: " + minDist[choosePac]);
                //System.err.println("Distance plus: " + minDistPlus[choosePac]);

                //giving move for one pac
                boolean powerUpped = false;  //powerup flag
                if(turnMod10 == 0) {
                    //SWITCH first [see-SWITCH]
                    if(closeType[choosePac] != null && closeType[choosePac].gt(myTypeList.get(choosePac))) {
                        System.out.print("SWITCH " + myPacList.get(choosePac) + " " + closeType[choosePac].greater().name);
                        powerUpped = true;
                    }
                    //SWITCH first [corner-clash-SWITCH] [judged by being in same position for switchAfter turns]
                    else {
                        //history checking
                        Iterator<Coordinate> iterHistory = myXYHistory[choosePac].iterator();
                        if(!iterHistory.hasNext()) {
                            System.err.println("Error: iterHistory is empty!");
                        }
                        Coordinate nowPos = iterHistory.next();
                        int match = 0;
                        //System.err.println(choosePac + "->" + myXYHistory);
                        while(iterHistory.hasNext() && match < Globs.switchAfter) {
                            if(!iterHistory.next().equals(nowPos))  break;
                            match++;
                        }
                        //history checking done
                        if(match == Globs.switchAfter) {  //>= is redundant here
                            System.out.print("SWITCH " + myPacList.get(choosePac) + " " + myTypeList.get(choosePac).greater().name);
                            powerUpped = true;
                        }
                        //SPEED second
                        else if (nextCell[choosePac] == myCell[choosePac] || dangerUp[choosePac] > Globs.dangerOverSpeed) {
                            System.out.print("SPEED " + myPacList.get(choosePac));
                            powerUpped = true;
                        }
                        //NORMAL_MOVE third
                        else {
                            Coordinate moveXY = Ops.decode(move2, width);
                            System.out.print("MOVE " + myPacList.get(choosePac) + " " + moveXY.x + " " + moveXY.y);
                        }
                    }
                }
                //NORMAL_MOVE
                else {
                    Coordinate moveXY = Ops.decode(move2, width);
                    System.out.print("MOVE " + myPacList.get(choosePac) + " " + moveXY.x + " " + moveXY.y);
                }
                System.out.print(" " + Globs.caveOneWt);
                if(!powerUpped) {
                    //marking usedNext(s)
                    usedNext.put(move1,true);
                    usedNext.put(move2,true);
                    //marking taken pellets
                    Ops.markPellets(myCell[choosePac], nextCell[choosePac], next, last, takenPellets);
                }
                //output formatting for multiple pacs
                if(moveI == pacCount - 1) {
                    System.out.println();
                }
                else {
                    System.out.print(" | ");
                }
                pacMove[choosePac] = true;  //setting moved flag
                //usedMap print
                /*System.err.print("Used map keys: ");
                for(Integer key: usedNext.keySet()) {
                    System.err.print(Ops.decode(key,width) + " ");
                }
                System.err.println();*/
                //takenPellets print
                /*System.err.print("taken pellets: ");
                for(Integer key: takenPellets.keySet()) {
                    System.err.print(Ops.decode(key,width) + " ");
                }
                System.err.println();*/
                //System.err.println("Checkpost 5 <for each pac>");
                //System.err.flush();
            }
            //given turns for all pacs
            firstTurn = false;
            //System.err.println("Checkpost 6 [turn finished]");
            //System.err.flush();
            System.err.println("caveOneWt: " + Globs.caveOneWt);
        }
        //end of endless while loop [LOL]
    }
}