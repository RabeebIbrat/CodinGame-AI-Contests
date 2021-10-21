import java.rmi.ServerError;
import java.text.DecimalFormat;
import java.util.*;
import java.io.PrintStream;

class Cell {
    int index;
    int richness;
    /**
     * adj[dist][dir]
     */
    int[][] adj = new int[Player.maxTreeSize+1][Player.totalDirs];
    /**
     * adjCount[dist] = adjacent rich cells at distance 'dist' or less<br>
     * <i>a.k.a., shadowCount, coverCount</i>
     */
    int[] adjCount = new int[Player.maxTreeSize+1];
    /**
     * adjSeed[treeSize] -> adjacent seedable cells for tree of size treeSize
     */
    HashMap<Integer, Boolean>[] adjSeed = new HashMap[Player.maxTreeSize+1];

    public Cell(int index, int richness) {
        this.index = index;
        this.richness = richness;
    }

    /*
        PER TURN VARIABLES
     */

    /**
     * 0 -> no tree<br>
     * +p -> my tree of size (p-1)<br>
     * -p -> opp tree of size (p-1)<br>
     * <i>Set in turnInput()</i>
     */
    int treeState;
    /**
     * inShadowDays[tree size] = shadow days count <i>on a tree of "tree size"</i> on the cell
     */
    int[] inShadowDays = new int[Player.maxTreeSize+1];
    /**
     * <b>shadowCount[treeSize][dir]</b> = no. of effective shadow on a tree of "treeSize"
     * from "dir" direction<br>
     */
    int[][] shadowCount = new int[Player.maxTreeSize+1][Player.totalDirs];
}

class Tree {
    int index, size;  //index -> tree's cellIndex
    boolean dormant;

    public Tree(int index, int size, boolean dormant) {
        this.index = index;
        this.size = size;
        this.dormant = dormant;
    }
}

class ActionStatus {
    int index = -1;
    double sunPts = -1; // In case of seeds, sunPts = lost suns by the seeder
    int[] sunSpend;

    public ActionStatus() {
        sunSpend = new int[Player.sunAheadCoeff+1];
    }

    public ActionStatus(int index, double sunPts) {
        this.index = index;
        this.sunPts = sunPts;
        sunSpend = new int[Player.sunAheadCoeff+1];
    }

    public ActionStatus(int index, double sunPts, int[] sunSpend) {
        this.index = index;
        this.sunPts = sunPts;
        this.sunSpend = sunSpend;
    }
}

class Input {
    public static void initInput(Scanner in) {
        Player.cellSize = in.nextInt(); // 37
        Player.cells = new Cell[Player.cellSize];

        Cell[] cells = Player.cells;

        for (int i = 0; i < Player.cellSize; i++) {
            int index = in.nextInt(); // 0 is the center cell, the next cells spiral outwards
            int richness = in.nextInt(); // 0 if the cell is unusable, 1-3 for usable cells

            cells[index] = new Cell(index, richness);
            Cell cell = cells[index];

            for(int j = 0; j < Player.totalDirs; j++) {
                cell.adj[1][j] = in.nextInt(); // the index of the neighbouring cell for each direction
                cell.adj[0][j] = -1; // no adjCell
            }

            for(int j = 0; j <= Player.maxTreeSize; j++) {
                cell.adjCount[j] = 0;
                cell.adjSeed[j] = new HashMap<>();
            }
        }

        int shadowDays = (Player.totalDays+1)/Player.totalDirs;
        for(int i = 0; i < Player.totalDirs; i++) {
            Player.shadowDays[i] = shadowDays;
        }
        for(int ahead = 0; ahead <= Player.shadowAheadCoeff; ahead++) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {
                Player.shadowDaysAhead[ahead][dir] = shadowDays;
            }
            for(int i = 0; i < ahead; i++) {
                Player.shadowDaysAhead[ahead][i%6]--;
            }
        }
    }

    public static void turnInput(Scanner in) {
        Player.day = in.nextInt(); // the game lasts 24 days: 0-23
        Player.moreDays = Player.totalDays - Player.day;
        Player.nutrients = in.nextInt(); // the base score you gain from the next COMPLETE action

        Player.mySun = in.nextInt(); // your sun points
        Player.myScore = in.nextInt(); // your current score
        Player.oppSun = in.nextInt(); // opponent's sun points
        Player.oppScore = in.nextInt(); // opponent's score

        Player.oppWait = in.nextInt() != 0; // whether your opponent is asleep until the next day

        int numberOfTrees = in.nextInt(); // the current amount of trees
        for (int i = 0; i < numberOfTrees; i++) {
            int cellIndex = in.nextInt(); // location of this tree
            int size = in.nextInt(); // size of this tree: 0-3
            boolean isMine = in.nextInt() != 0; // 1 if this is your tree
            boolean isDormant = in.nextInt() != 0; // 1 if this tree is dormant

            if(isMine) {
                Player.myTrees.add(new Tree(cellIndex, size, isDormant));
                Player.myTreeCount[size]++;
                Player.cells[cellIndex].treeState = size+1;
            }
            else {
                Player.oppTrees.add(new Tree(cellIndex, size, isDormant));
                Player.oppTreeCount[size]++;
                Player.cells[cellIndex].treeState = -size-1;
            }
        }

        // REDUNDANT PART
        int noOfActions = in.nextInt(); // all legal actions
        if (in.hasNextLine()) {
            in.nextLine();
        }
        for (int i = 0; i < noOfActions; i++) {
            String inputAction = in.nextLine(); // try printing something from here to start with
        }
    }

    /**
     * executes at the start of per turn <b>before turnInput()</b><br>
     * clears all per turn variables
     */
    public static void preProcess() {
        Player.myTrees.clear();
        Player.oppTrees.clear();

        Player.lastDay = Player.day;

        for(int i = 0; i <= Player.maxTreeSize; i++) {
            Player.myTreeCount[i] = 0;
            Player.oppTreeCount[i] = 0;
        }

        Player.turnGiven = false;

        //CELL PROPERTIES
        for(Cell cell: Player.cells) {
            cell.treeState = 0;
            for(int j = 0; j <= Player.maxTreeSize; j++) {
                cell.inShadowDays[j] = 0;

                for(int dir = 0; dir < Player.totalDirs; dir++) {
                    cell.shadowCount[j][dir] = 0;
                }
            }
        }
    }

}

class Diag {
    /**
     * update myTotalSun, oppTotalSun, myOldSun, oppOldSun -> <i>per turn</i><br>
     */
    private static void updateSun() {
        if(Player.day != Player.lastDay) {
            Player.mySunEarn += Player.mySun - Player.myOldSun;
            Player.oppSunEarn += Player.oppSun - Player.oppOldSun;
        }
        Player.myOldSun = Player.mySun;
        Player.oppOldSun = Player.oppSun;
    }

    /**
     * update myTotalCompletes, oppTotalCompletes, myCompletePts, oppCompletePts,
     * myTreeRich, oppTreeRich, activeTrees -> <i>per turn</i><br>
     */
    private static void updateTree(boolean mine, int[] nut) {
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;
        HashMap<Integer, Boolean> fullTrees = mine? Player.myFullTrees : Player.oppFullTrees;
        HashMap<Integer,Boolean> newFullTrees = new HashMap<>();

        for(Tree tree : trees) {
            if(tree.size == Player.maxTreeSize) {
                newFullTrees.put(tree.index, true);
                fullTrees.remove(tree.index);
            }
        }

        for(int index: fullTrees.keySet()) {
            if(mine) {
                Player.myTotalCompletes++;
                Player.myCompletePts += (++nut[0]);
                Player.myRichPts += Player.richPts[Player.cells[index].richness];
            }
            else {
                Player.oppTotalCompletes++;
                Player.oppCompletePts += (++nut[0]);
                Player.oppRichPts += Player.richPts[Player.cells[index].richness];
            }
        }

        if(mine)
            Player.myFullTrees = newFullTrees;
        else
            Player.oppFullTrees = newFullTrees;
    }

    /**
     * per turn diag update <b>AFTER</b> Fx.turnInput()
     */
    public static void perTurnUpdate() {
        updateSun();

        int[] nut = {Player.nutrients};
        updateTree(false,nut);  //MUST update for oppTrees first
        updateTree(true,nut);
    }

    /**
     * update oppTotalCompletes, oppCompletePts, oppRichPts -> <i>at game end</i>
     */
    private static void endUpdateTree() {
        //max heap
        PriorityQueue<Integer> richList = new PriorityQueue<>(Collections.reverseOrder());
        for(Tree tree : Player.oppTrees) {
            if(tree.size == Player.maxTreeSize && !tree.dormant)
                richList.offer(Player.cells[tree.index].richness);
        }

        int oppCompletes = Math.min(richList.size(), Player.oppSun/Player.growCost[Player.maxTreeSize]);

        int nut = Player.nutrients;
        for(int i = 0; i < oppCompletes; i++) {
            int richness = richList.poll();
            if(nut + Player.richPts[richness] <= 1)
                break;

            Player.oppTotalCompletes++;
            Player.oppCompletePts += nut--;
            Player.oppRichPts += Player.richPts[richness];
        }
    }

    /**
     * end game diag update
     */
    public static void gameEndUpdate() {
        endUpdateTree();
    }

    /**
     * prints non-cumulative version of sunAhead/sunSpendAhead array
     */
    public static void printSunAhead(int[] sunAhead, String text) {
        int oldSun = 0;
        System.err.print(text + ": ");
        for(int sunNow: sunAhead) {
            System.err.print((sunNow-oldSun) + " ");
            oldSun = sunNow;
        }
        System.err.println();
    }
}

class Fx {
    /*
        GLOBAL FX
     */
    private static void genAdj() {
        for(int dist = 1; dist < Player.maxTreeSize; dist++) {
            for(Cell cell: Player.cells) {
                int[][] adj = cell.adj;

                for(int dir = 0; dir < Player.totalDirs; dir++) {
                    if(adj[dist][dir] == -1) {
                        adj[dist+1][dir] = -1;
                    }
                    else {
                        adj[dist+1][dir] = Player.cells[adj[dist][dir]].adj[1][dir];
                    }
                }

            }
        }
        //all adj's set
    }

    /**
     * MUST be called AFTER genAdj()
     */
    private static void genAdjCount() {
        for(Cell cell: Player.cells) {
            for(int len = 1; len <= Player.maxTreeSize; len++) {
                for(int dir = 0; dir < Player.totalDirs; dir++) {
                    int adjIndex = cell.adj[len][dir];
                    if(adjIndex != -1 && Player.cells[adjIndex].richness > 0)
                        cell.adjCount[len]++;
                }
                cell.adjCount[len] += cell.adjCount[len-1];
            }

        }
    }

    private static void genAdjSeed() {

        for(Cell cell: Player.cells) {
            HashMap<Integer,Boolean>[] adjSeed = cell.adjSeed;
            adjSeed[0].put(cell.index, true);

            for(int dist = 1; dist <= Player.maxTreeSize; dist++) {

                for(int nowCell : adjSeed[dist-1].keySet()) {
                    adjSeed[dist].put(nowCell,true); // no move
                    for(int dir = 0; dir < Player.totalDirs; dir++) //move 1 step
                        adjSeed[dist].put(Player.cells[nowCell].adj[1][dir], true);
                }
                adjSeed[dist].remove(-1);  //remove invalid cell
            }

            for(int dist = 0; dist <= Player.maxTreeSize; dist++)
                adjSeed[dist].remove(cell.index);  //remove self
        }
    }

    public static void computeAllAdjs() {
        genAdj();
        genAdjCount();
        genAdjSeed();
    }

    /*
        PER TURN FX
     */

    /**
     * number of "mine" trees complete-able today <b>(considering sun)</b><br>
     */
    public static int computeComplete(boolean mine) {
        int sun  = mine? Player.mySun : Player.oppSun;
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;

        int completeCount = 0;

        for(Tree tree: trees) {
            if(tree.size == Player.maxTreeSize && !tree.dormant)
                completeCount++;
        }
        return Math.min(completeCount, sun/Player.growCost[Player.maxTreeSize]);
    }

    private static void addShadowCount(List<Tree> trees) {
        for(Tree tree : trees) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {
                for(int len = 1; len <= tree.size; len++) {
                    int adjCell = Player.cells[tree.index].adj[len][dir];
                    if(adjCell == -1)
                        continue;

                    for(int i = 1; i <= tree.size; i++) {
                        Player.cells[adjCell].shadowCount[i][dir]++;
                    }
                }
            }
            //shadowCount added for a tree
        }
    }

    /**
     * MUST be called AFTER updating Player.shadowDays[]
     */
    private static void computeCellInShadowDays() {
        addShadowCount(Player.myTrees);
        addShadowCount(Player.oppTrees);

        for(Cell cell: Player.cells) {
            for(int treeSize = 1; treeSize <= Player.maxTreeSize; treeSize++) {
                for(int dir = 0; dir < Player.totalDirs; dir++) {
                    if(cell.shadowCount[treeSize][dir] > 0)
                        cell.inShadowDays[treeSize] += Player.shadowDays[dir];
                }
            }
        }
    }

    /**
     * MUST be called AFTER turnInput()<br>
     * <i>includes computeShadows()</i>
     */
    public static void shadowUpdate() {
        if(Player.day != Player.lastDay) {
            Player.shadowDays[Player.day % Player.totalDirs]--;

            for(int ahead = 0; ahead <= Player.shadowAheadCoeff; ahead++) {
                if (Player.moreDays >= ahead) {
                    Player.shadowDaysAhead[ahead][(Player.day + ahead) % 6]--;
                }
            }
        }

        computeCellInShadowDays();
    }

    /*
        ACCESSIBILITY
     */
    public static int revDir(int dir) {
        return (dir+3) % 6;
    }

    public static HashMap<Integer,Boolean> seedCells(Tree tree) {
        HashMap<Integer,Boolean> seeds = new HashMap<>();

        if(tree.dormant) {
            return seeds;
        }

        for(int seedIndex: Player.cells[tree.index].adjSeed[tree.size].keySet()) {
            if(Player.cells[seedIndex].treeState == 0 && Player.cells[seedIndex].richness > 0)
                seeds.put(seedIndex,true);
        }

        return seeds;
    }

    /**
     * counts the number of empty cells around that cell
     */
    public static List<Integer> adjBlanks(Cell cell) {
        List<Integer> adjCells = new ArrayList<>();
        Cell[] cells = Player.cells;
        for(int dist = 1; dist <= Player.maxTreeSize; dist++) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {
                int adjIndex = cell.adj[dist][dir];
                if(adjIndex >= 0 && cells[adjIndex].richness > 0 && cells[adjIndex].treeState == 0)
                    adjCells.add(adjIndex);
            }
        }

        return adjCells;
    }
}

/**
 * sun computations with <b>no change in trees</b>
 */
class SunFx {
    /**
     * total obtained sun <b>if no change in trees</b><br>
     */
    public static int totalSun(boolean mine) {
        int sun = 0;
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;
        for(Tree tree: trees) {
            if(tree.size == 0)
                continue;

            Cell cell = Player.cells[tree.index];
            sun += tree.size * (Player.moreDays - cell.inShadowDays[tree.size]);
        }
        return sun;
    }

    /**
     * total obtained sun for this "cell" <b>if no change in trees</b><br>
     */
    public static int cellSun(Cell cell) {
        if(cell.treeState == 0)
            return 0;

        int size = Math.abs(cell.treeState)-1;
        return size * (Player.moreDays - cell.inShadowDays[size]);
    }

    /**
     * <i>A crude lower bound of suns required for completion of all trees of "mine"</i>
     */
    public static int completeSun(boolean mine, boolean newSeed) {
        int[] treeCount = mine? Player.myTreeCount : Player.oppTreeCount;
        int totalTrees = treeCount[0] + (newSeed?1:0);
        int sunCost = 0;
        for(int i = 1; i <= Player.maxTreeSize; i++) {
            sunCost += Player.growCost[i-1]*totalTrees;
            totalTrees += treeCount[i];
        }
        sunCost += Player.growCost[Player.maxTreeSize] * totalTrees;
        return sunCost;
    }

    /**
     * updates sunAhead[], clears spentSun[]<br>
     * <i>doesn't check for the "Player.moreDays" boundary</i>
     */
    public static void updateSuns(boolean mine) {
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;
        int[] sunAhead = mine? Player.mySunAhead : Player.oppSunAhead;

        if(Player.day != Player.lastDay)
            sunAhead[0] = mine? Player.mySun: Player.oppSun;

        int nowDir = Player.day;
        for(int ahead = 1; ahead <= Player.sunAheadCoeff; ahead++) {
            sunAhead[ahead] = 0;
            nowDir = (++nowDir)%Player.totalDirs;

            for(Tree tree: trees) {
                if(Player.cells[tree.index].shadowCount[tree.size][nowDir] == 0)
                    sunAhead[ahead] += tree.size;
            }
            sunAhead[ahead] += sunAhead[ahead-1];
        }

        if(mine) {
            if(Player.day != Player.lastDay) {
                for(int i = 0; i <= Player.sunAheadCoeff; i++)
                    Player.mySunSpent[i] = 0;
            }

            Player.myTotalSun = totalSun(mine);
            Player.mySeedCompleteSun = completeSun(true, true);
            Player.myCompleteSun = completeSun(true, false);
        }
    }
}

class SimpleHeur {
    /**
     * increase in total obtained sun for growing/completing "tree in cell" <b>if no change in trees</b><br>
     * <i>A crude lower bound of increase in suns</i>
     */
    public static int growSunPts(Cell cell, boolean points) {
        if(cell.treeState == 0)
            return 0;

        int size = Math.abs(cell.treeState);  //size+1
        if(size == Player.maxTreeSize+1) {
            int adder = points? (Player.nutrients + Player.richPts[cell.richness]) * 3: 0;  //pts equiv to sun
            return -Player.maxTreeSize * (Player.moreDays - cell.inShadowDays[Player.maxTreeSize]) + adder;
        }
        return Player.moreDays-cell.inShadowDays[size-1] + size*(cell.inShadowDays[size-1] - cell.inShadowDays[size]);
    }
    /**
     * increase in total covered sun <b><i>(of the owner of the cell)</i></b>
     * for growing/completing "tree in cell" <b>if no change in trees</b><br>
     * <i>A crude lower bound of cover of suns</i>
     */
    public static int growCover(Cell cell) {
        if(cell.treeState == 0)
            return 0;

        int mySize = Math.abs(cell.treeState) - 1;

        int sunCover = 0;
        for(int len = 1; len <= Math.min(mySize+1, Player.maxTreeSize); len++) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {
                int adjIndex = cell.adj[len][dir];
                if (adjIndex == -1)
                    continue;

                Cell adjCell = Player.cells[adjIndex];
                if(adjCell.treeState == 0)  //empty cell
                    continue;

                int coverSign = cell.treeState*adjCell.treeState > 0? -1:1;  //impact of covering this tree
                int otherSize = Math.abs(adjCell.treeState) - 1;

                int[][] shadowCount = adjCell.shadowCount;

                if(mySize == Player.maxTreeSize) {  //Complete tree lifecycle
                    if(shadowCount[otherSize][dir] == 1)
                        sunCover -= coverSign * otherSize * Player.shadowDays[dir];
                }
                else if(otherSize <= mySize) {  //grow tree
                    if(shadowCount[otherSize][dir] == 0)
                        sunCover += coverSign * otherSize * Player.shadowDays[dir];
                }
            }
            //sunCover in a specific "dir" done
        }
        return sunCover;
    }

    public static int growTotalPts(Cell cell, boolean points) {
        return growSunPts(cell, points) + growCover(cell);
    }

    /**
     * actual: 1 -> actual grow cost; 0 -> effective grow cost
     */
    public static int growCost(Cell cell, boolean mine, boolean actual, boolean dormant) {
        int treeSize = cell.treeState * (mine? 1:-1) - 1;
        if(treeSize < -1)  // tree not "mine"
            return 0;

        if(Player.moreDays < (Player.maxTreeSize+1 - treeSize) - (dormant?0:1)) // tree cannot be completed
            actual = true;

        int cost = 0;
        if( (Player.enableGrowCost || actual) && treeSize >= 0 )
            cost += Player.growCost[treeSize];
        if(treeSize < Player.maxTreeSize) {
            cost += Player.myTreeCount[treeSize + 1];
        }
        return cost;
    }

    /**
     * new seedable cells acquired for growing "tree in cell"<br>
     * <i>works on both myTree & oppTree</i><br>
     */
    public static List<Integer> growSeedCells(Cell cell) {
        List<Integer> newSeeds = new ArrayList<>();

        int treeSize = Math.abs(cell.treeState);  //size+1
        if(treeSize == 0|| treeSize == Player.maxTreeSize+1) {  //empty cell  OR  max size tree
            return newSeeds;
        }

        for(int seedIndex: cell.adjSeed[treeSize].keySet()) {
            if(!cell.adjSeed[treeSize-1].containsKey(seedIndex) &&
                    Player.cells[seedIndex].treeState == 0 && Player.cells[seedIndex].richness > 0)
                newSeeds.add(seedIndex);
        }

        return newSeeds;
    }

    /**
     * minimum days needed for completing growth of "mine" trees
     */
    public static int compactCompleteDays(boolean mine) {
        int completeDays = 0;
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;

        for(Tree tree: trees) {
            completeDays = Math.max(completeDays, Player.maxTreeSize-tree.size + (tree.dormant?1:0));
        }
        return completeDays;
    }
}

class IncHeur {
    /**
     * increase in total obtained sun for incrementally growing/completing "tree in cell"
     * <b>if no change in other trees</b><br>
     * <i>An improved lower bound of increase in suns</i>
     */
    public static int growSunPts(Cell cell, boolean points) {
        int adder = points? (Player.nutrients + Player.richPts[cell.richness]) * 3 : 0;  //pts equiv to sun
        int size = Math.abs(cell.treeState); // size+1

        if(size == Player.maxTreeSize + 1) {
            return -Player.maxTreeSize * (Player.moreDays - cell.inShadowDays[Player.maxTreeSize]) + adder;
        }

        int sunPts = 0;
        int defSize = Math.max(size-1,0); // accommodates defSize = 0

        int shadows = cell.inShadowDays[Player.maxTreeSize];
        int shadowsDef = cell.inShadowDays[defSize];
        int[][] shadowCount = cell.shadowCount;

        int growDays = Player.maxTreeSize - size, dayPassed = 1;

        int len;
        for(len = size; len < Math.min(Player.maxTreeSize,size+Player.moreDays); len++, dayPassed++) {
            int dir = (Player.day + dayPassed) % Player.totalDirs;

            if(shadowCount[Player.maxTreeSize][dir] > 0)
                shadows--;
            if(shadowCount[defSize][dir] > 0) {
                shadowsDef--;
            }
            if(shadowCount[len][dir] == 0)
                sunPts += shadowCount[defSize][dir] == 0? (len-defSize) : len;
        }
        int diffShadow = shadowsDef - shadows; // if defSize > 0, we MUST have diffShadow > 0
        int moreDays = Player.moreDays-growDays;
        if(moreDays > 0) {
            sunPts += (Player.maxTreeSize - defSize) * (moreDays - shadowsDef) + Player.maxTreeSize * (diffShadow);
            sunPts += adder;
        }

        return sunPts;
    }

    /**
     *
     * increase in total covered sun <b><i>(for the owner of the cell, mine for empty cell)</i></b>
     * for incrementally growing/completing "tree in cell" <b>if no change in other trees</b><br>
     * <i>An improved lower bound of cover of suns</i>
     */
    public static int growCover(Cell cell) {
        int mySign = (cell.treeState >= 0)? 1:-1;
        int mySize = Math.abs(cell.treeState) - 1;

        int sunCover = 0;

        int dayPassed = 1;
        for(int treeLen = mySize+1; treeLen < Math.min(Player.maxTreeSize,mySize+1 + Player.moreDays);
            treeLen++, dayPassed++) {  //growing tree with size < Player.maxTreeSize

            int nowDir = (Player.day + dayPassed) % Player.totalDirs;

            for (int len = 1; len <= treeLen; len++) {
                int adjIndex = cell.adj[len][nowDir];
                if (adjIndex == -1)
                    continue;

                Cell adjCell = Player.cells[adjIndex];
                int otherSize = Math.abs(adjCell.treeState) - 1;
                if (otherSize <= 0)  //empty cell  OR  size 0 tree
                    continue;

                int coverSign = (adjCell.treeState * mySign >= 0)? -1 : 1;

                if(otherSize <= mySize) {
                    if(adjCell.shadowCount[otherSize][nowDir] == 0) //otherTree not shadowed
                        sunCover += coverSign * otherSize;
                }
            }
        }
        //sunCover before "maxTreeSize" done  OR  moreDays finished

        for(int len = 1; len <= Player.maxTreeSize; len++) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {

                int adjIndex = cell.adj[len][dir];
                if (adjIndex == -1)
                    continue;

                Cell adjCell = Player.cells[adjIndex];
                int otherSize = Math.abs(adjCell.treeState) - 1;
                if (otherSize <= 0)  //empty cell  OR  size 0 tree
                    continue;

                int coverSign = (adjCell.treeState * mySign >= 0)? -1 : 1;
                int[][] myShadowCount = adjCell.shadowCount;

                if(mySize == Player.maxTreeSize) {  //Complete tree lifecycle
                    if(myShadowCount[otherSize][dir] == 1)
                        sunCover -= coverSign * otherSize * Player.shadowDays[dir];
                }
                else {  //grow tree
                    if(myShadowCount[otherSize][dir] == 0)
                        sunCover += coverSign * otherSize * Player.shadowDaysAhead[dayPassed-1][dir];
                }
            }
            //sunCover in a specific "dir" done
        }

        return sunCover;
    }

    /**
     * growSunPts(..) + growCover(..)
     */
    public static int growTotalPts(Cell cell, boolean points) {
        return growSunPts(cell, points) + growCover(cell);
    }

    /**
     * actual: 1 -> actual grow cost; 0 -> effective grow cost
     */
    public static int growCost(Cell cell, boolean mine, boolean points, boolean actual, boolean dormant) {
        int size = cell.treeState * (mine? 1:-1) - 1;
        if(size < -1)  // tree not "mine"
            return 0;

        if(Player.moreDays < (Player.maxTreeSize+1 - size) - (dormant?0:1)) // tree cannot be completed
            actual = true;

        int[] treeCount = mine? Player.myTreeCount : Player.oppTreeCount;
        int cost = 0;
        for(int len = size; len < Math.min(Player.maxTreeSize,size+Player.moreDays+1); len++) {
            if( (Player.enableGrowCost || actual) && len >= 0 )
                cost += Player.growCost[len];
            cost += treeCount[len+1];
        }
        if( (Player.enableGrowCost || actual) && points && size + Player.moreDays+1 > Player.maxTreeSize )
            cost += Player.growCost[Player.maxTreeSize];

        return cost;
    }

    /**
     * actual seed grow cost considered<br>
     */
    public static int growSeedCost(boolean mine, boolean points) {
        int[] treeCount = mine? Player.myTreeCount : Player.oppTreeCount;
        int cost = treeCount[0];
        for(int len = 0; len < Math.min(Player.maxTreeSize,Player.moreDays-1); len++) {
            cost += Player.growCost[len];
            cost += treeCount[len+1];
        }
        if(points && (-1) + Player.moreDays > Player.maxTreeSize)
            cost += Player.growCost[Player.maxTreeSize];

        return cost;
    }

    /**
     * maximum number of blanks <b>effectively reachable</b> from this cell<br>
     * <i>set dormant = false for seeds</i><br>
     * An upper bound on effective blanks that can be shadowed <i>(further improvable)</i>
     */
    public static List<Integer> maxEffBlanks(Cell cell, boolean dormant) {
        List<Integer> adjCells = new ArrayList<>();

        Cell[] cells = Player.cells;
        int size = Math.abs(cell.treeState) - 1;
        int maxDist = Math.min(Player.maxTreeSize, size + Player.moreDays-1 + (dormant?0:1));

        for(int dist = 1; dist <= maxDist; dist++) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {
                if(Player.shadowDays[dir] == 0)
                    continue;

                int adjIndex = cell.adj[dist][dir];
                if(adjIndex >= 0 && cells[adjIndex].richness > 0 && cells[adjIndex].treeState == 0)
                    adjCells.add(adjIndex);
            }
        }

        return adjCells;
    }

    /**
     * <b>more days</b> needed for completing growth of "mine" trees with minimal grow cost<br>
     * <i>[one-by-one growth]</i>
     */
    public static int linearCompleteDays(boolean mine) {
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;
        /*
            treeCount[treeSize][state]
            state: 0->dormant, 1-> active
         */
        int[][] treeCount = new int[Player.maxTreeSize+1][2];
        int dormant = 0, active = 1;
        int leftTrees = 0;

        for(int i = 0; i <= Player.maxTreeSize; i++) {
            treeCount[i][dormant] = treeCount[i][active] = 0;
        }
        for(Tree tree: trees) {
            treeCount[tree.size][tree.dormant? dormant:active]++;
            leftTrees++;
        }

        int day = 0;
        while(true) {
            leftTrees -= treeCount[Player.maxTreeSize][active];
            treeCount[Player.maxTreeSize][active] = 0;

            for(int size = Player.maxTreeSize-1; size >= 0; size--){
                if(treeCount[size+1][active] + treeCount[size+1][dormant] == 0
                        && treeCount[size][active] > 0) {
                    treeCount[size][active]--;
                    treeCount[size+1][dormant]++;
                }
            }
            for(int size = Player.maxTreeSize; size >= 0; size--) {
                treeCount[size][active] += treeCount[size][dormant];
                treeCount[size][dormant] = 0;
            }

            if(leftTrees == 0)
                break;
            day++;
        }
        return day;
    }
}

/**
 * accommodates SimpleHeur, IncHeur functionality
 */
class IncPartHeur {
    /**
     * increase in total obtained sun for partially incrementally growing/completing "tree in cell"
     * <b>if no change in other trees</b><br>
     * <b><i>Setting maxSize > Player.maxTreeSize INCLUDES immediate completion of the tree in "cell"</b></i><br>
     * use dormant = true for "next day seed"<br>
     * <i>An improved lower bound of increase in suns</i><br>
     */
    public static double growSunPts(Cell cell, int maxSize, boolean points, boolean dormant) {
        int adder = 0;
        int size = Math.abs(cell.treeState); // size+1
        if(maxSize < size) // nothing to grow
            return 0;
        maxSize = Math.min(maxSize,Player.maxTreeSize+1);
        int growMaxSize = Math.min(maxSize,Player.maxTreeSize);

        int sunPts = 0;

        int defSize = Math.max(size-1,0); // accommodates empty cells [size-1 = -1]

        int shadows = cell.inShadowDays[growMaxSize];
        int shadowsDef = cell.inShadowDays[defSize];
        int[][] shadowCount = cell.shadowCount;

        size -= dormant? 1:0;
        //int growDays = growMaxSize - size;
        int dayPassed = 1;

        int len;
        for(len = size; len < Math.min(maxSize,size+Player.moreDays); len++, dayPassed++) {
            int dir = (Player.day + dayPassed) % Player.totalDirs;

            if(shadowCount[growMaxSize][dir] > 0)
                shadows--;
            if(shadowCount[defSize][dir] > 0) {
                shadowsDef--;
            }
            if(len > 0 && shadowCount[len][dir] == 0)
                sunPts += shadowCount[defSize][dir] == 0? (len-defSize) : len;
        }

        int diffShadow = shadowsDef - shadows; // if defSize > 0, we MUST have diffShadow >= 0
        int moreDays = Player.moreDays - (dayPassed-1);
        if(moreDays > 0) {
            if(maxSize > Player.maxTreeSize) {
                if(points)
                    adder += (Player.nutrients + Player.richPts[cell.richness]) * 3;
                sunPts -= defSize * (moreDays - shadowsDef);
            }
            else
                sunPts += (growMaxSize - defSize) * (moreDays - shadowsDef) + growMaxSize * (diffShadow);
        }

        if(Player.sunToNut) {
            int nextNut =  Player.nutrients - Player.myTotalCompletes - Player.oppTotalCompletes;
            double nutPts = 0;
            int sunSign = sunPts > 0? 1:-1;

            if(Player.debug)
                System.err.print("<" + sunPts + ">");

            while( nextNut >= 5 &&
                    (double) sunPts*sunSign >= (double)Player.fullGrowCost * Math.max(1,Player.getNutRatio) ) {
                sunPts -= sunSign * Player.fullGrowCost;
                nutPts += sunSign * nextNut--;
            }
            if(sunSign * sunPts >= Player.getNutRatio * Player.fullGrowCost && nextNut >= 5)
                nutPts += (double) sunPts/Player.fullGrowCost * nextNut;
            else
                nutPts += (double) sunPts/3;
            return 3*nutPts + adder;
        }

        return sunPts + adder;
    }

    /**
     * increase in total covered sun <b><i>(for the owner of the cell, mine for empty cell)</i></b>
     * for partially incrementally growing/completing "tree in cell" <b>if no change in other trees</b><br>
     * <i>Setting maxSize > Player.maxTreeSize INCLUDES immediate completion of the tree in "cell"</i><br>
     * use dormant = true for "next day seed"<br>
     * <i>An improved lower bound of cover of suns</i>
     */
    public static int growCover(Cell cell, int maxSize, boolean dormant) {
        int mySign = (cell.treeState >= 0)? 1:-1;
        int mySize = Math.abs(cell.treeState) - 1;
        if(maxSize <= mySize)  //nothing to grow
            return 0;
        maxSize = Math.min(maxSize, Player.maxTreeSize+1);
        int growMaxSize = Math.min(maxSize, Player.maxTreeSize);

        int sunCover = 0;

        int dayPassed = 1;
        for(int treeLen = mySize+(dormant?0:1); treeLen < Math.min(maxSize,mySize + (dormant?0:1) + Player.moreDays);
            treeLen++, dayPassed++) {  //growing tree with size < Player.maxTreeSize

            int nowDir = (Player.day + dayPassed) % Player.totalDirs;

            for (int len = 1; len <= treeLen; len++) {
                int adjIndex = cell.adj[len][nowDir];
                if (adjIndex == -1)
                    continue;

                Cell adjCell = Player.cells[adjIndex];
                int otherSize = Math.abs(adjCell.treeState) - 1;
                if (otherSize <= 0 || otherSize > mySize)  //empty cell  OR  size 0 tree  OR  larger tree
                    continue;
                if(adjCell.treeState*mySign < 0 &&
                        otherSize > mySize + Player.oppCoverAhead) // limit on large oppTree cover
                    continue;

                int coverSign = (adjCell.treeState * mySign >= 0)? -1 : 1;
                if(adjCell.shadowCount[otherSize][nowDir] == 0) //otherTree not shadowed
                    sunCover += coverSign * otherSize;
            }
        }
        //sunCover before "maxTreeSize" done  OR  moreDays finished

        for(int len = 1; len <= growMaxSize; len++) {
            for(int dir = 0; dir < Player.totalDirs; dir++) {

                int adjIndex = cell.adj[len][dir];
                if (adjIndex == -1)
                    continue;

                Cell adjCell = Player.cells[adjIndex];
                int otherSize = Math.abs(adjCell.treeState) - 1;
                if (otherSize < 0 || otherSize > growMaxSize)  //empty cell  OR  larger tree
                    continue;
                if(adjCell.treeState*mySign < 0 &&
                        otherSize > mySize + Player.oppCoverAhead) // limit on large oppTree cover
                    continue;

                int coverSign = (adjCell.treeState * mySign >= 0)? -1 : 1;
                int[][] myShadowCount = adjCell.shadowCount;

                if(maxSize > Player.maxTreeSize) {  //Complete tree lifecycle
                    if(myShadowCount[otherSize][dir] == 1)
                        sunCover -= coverSign * otherSize * Player.shadowDaysAhead[dayPassed - 1][dir];
                }
                else {  //grow tree
                    if(myShadowCount[otherSize][dir] == 0) {
                        sunCover += coverSign * otherSize * Player.shadowDaysAhead[dayPassed - 1][dir];
                    }
                }
            }
            //sunCover in a specific "dir" done
        }

        return sunCover;
    }

    /**
     * <i>Setting maxSize > Player.maxTreeSize INCLUDES immediate completion of the tree in "cell"</i><br>
     * use dormant = true for "next day seed"<br>
     */
    public static double growTotalPts(Cell cell, int maxSize, boolean points, boolean dormant) {
        return growSunPts(cell,maxSize,points,dormant) + growCover(cell,maxSize,dormant);
    }

    /**
     * use dormant = true for "next day seed"
     */
    public static int effGrowCost(Cell cell, boolean mine, int maxSize, boolean dormant) {
        int treeSize = cell.treeState * (mine? 1:-1) - 1;
        if(treeSize < -1)  // tree not "mine"
            return 0;
        maxSize = Math.min(maxSize,Player.maxTreeSize+1);

        int maxGrowableSize = treeSize+Player.moreDays+(dormant?0:1); // considering Player.moreDays
        maxSize = Math.min(maxSize,maxGrowableSize);

        //DEBATABLE
        boolean actual = (maxGrowableSize <= Player.maxTreeSize ||  treeSize == -1 ||
                IncHeur.growCost(cell,mine,true,true,dormant) > Player.myTotalSun);

        int[] treeCount = mine? Player.myTreeCount : Player.oppTreeCount;
        int cost = 0;
        for(int len = treeSize; len < maxSize; len++) {
            if( (Player.enableGrowCost || actual) && len >= 0 )
                cost += Player.growCost[len];
            if(len < Player.maxTreeSize)
                cost += treeCount[len+1];
        }

        return cost;
    }

    /**
     * <i>doesn't check for the "Player.moreDays" boundary</i><br>
     * use dormant = true for "next day seed"
     */
    public static int[] growSunSpend(Cell cell, boolean mine, int maxSize, boolean dormant) {
        int[] sunSpend = new int[Player.sunAheadCoeff+2];
        int size = cell.treeState*(mine? 1:-1) - 1; // size
        if(size < -1)  // tree not "mine"
            return sunSpend;
        maxSize = Math.min(maxSize,Player.maxTreeSize+1);

        int[] treeCount = mine? Player.myTreeCount : Player.oppTreeCount;
        if(dormant && size > 0 && cell.shadowCount[size][Player.day%6] == 0) //sun earning for dormant day
            sunSpend[1] -= size;

        for(int ahead = dormant?1:0; ahead <= Player.sunAheadCoeff; ahead++, size += size<maxSize?1:0) {
            //sun spending: size -> size+1
            if(size < maxSize) {
                if(size >= 0)
                    sunSpend[ahead] += Player.growCost[size];
                if(size < Player.maxTreeSize)
                    sunSpend[ahead] += treeCount[size + 1];
            }
            //sun earning
            int nowDir = (Player.day+ahead) % 6;
            if(size > 0 && size <= Player.maxTreeSize && cell.shadowCount[size][nowDir] == 0)
                sunSpend[ahead+1] -= size;
            //cumulative counting
            if(ahead > 0)
                sunSpend[ahead] += sunSpend[ahead-1];
        }
        sunSpend[Player.sunAheadCoeff+1] += sunSpend[Player.sunAheadCoeff];

        return sunSpend;
    }

    /**
     * check if my tree/seed is growable, considering available suns<br>
     */
    public static boolean growAble(int[] sunSpend) {
        for(int i = 0; i <= Math.min(Player.sunAheadCoeff,Player.moreDays); i++) {
            if(sunSpend[i] > Player.mySunAhead[i] - Player.mySunSpent[i])
                return false;
        }
        return true;
    }

    /**
     * cells seedable "next day"<br>
     * <i>includes cells seedable today</i>
     */
    public static HashMap<Integer,Boolean> seedNextCells(Tree tree) {
        HashMap<Integer,Boolean> seeds;

        if(tree.dormant) {
            tree.dormant = false;
            seeds = Fx.seedCells(tree);
            tree.dormant = true;
        }
        else if(tree.size < Player.maxTreeSize){
            tree.size++;
            seeds = Fx.seedCells(tree);
            tree.size--;
        }
        else
            seeds = Fx.seedCells(tree);

        return seeds;
    }

    public static void addSeedActions(HashMap<Integer,ActionStatus> seedList,
                                      PriorityQueue<ActionStatus> actionsList, boolean points, int minSunPts) {

        for(Map.Entry<Integer, ActionStatus> seedDetails: seedList.entrySet()) {
            int seedIndex = seedDetails.getKey();
            Cell cell = Player.cells[seedIndex];

            int[] treeSunSpend = seedDetails.getValue().sunSpend;
            int treeSunCost = (int) seedDetails.getValue().sunPts;

            for(int size = 1; size <= Math.min(Player.maxTreeSize+1,1 + Player.moreDays); size++) {
                int[] sunSpend = IncPartHeur.growSunSpend(cell, true, size, false);
                double sunPts = IncPartHeur.growTotalPts(cell, size, points, false)
                        - IncPartHeur.effGrowCost(cell, true, size, false);

                for(int i = 0; i <= Player.sunAheadCoeff; i++) {
                    sunSpend[i] += treeSunSpend[i];
                }

                if(Player.myTotalSun >= Player.mySeedCompleteSun && Player.moreDays >= Player.maxTreeSize+1)
                    sunPts += 3 * Player.richPts[cell.richness];

                int spentDays = size+1;
                //heuristics status print [for debugging]
                if(Player.showSeedHeurStatus) {
                    System.err.println("Seed.size: " + seedIndex + "." + size);
                    System.err.print("SunPts: " + sunPts + " = " + IncPartHeur.growSunPts(cell,size,points,false));
                    System.err.print(" + " + IncPartHeur.growCover(cell,size,false));
                    System.err.println(" - " + IncPartHeur.effGrowCost(cell, true, size, false));
                    System.err.println("Eff sun pts: " + ( (double)sunPts/(size+1)) );
                    //Diag.printSunAhead(sunSpend, "sun spend");
                }

                if(IncPartHeur.growAble(sunSpend) && sunPts > minSunPts) {
                    actionsList.offer(new ActionStatus(seedIndex,(double)sunPts/spentDays,sunSpend));
                }
            }
        }
    }
}

class Player {
    /*
        STATIC PARAMETERS
     */
    public static final int[] growCost = {1,3,7,4};
    public static final int fullGrowCost = 15;
    public static final int[] richPts = {0,0,2,4};
    public static final int totalDays = 24-1; // 0 ~ totalDays
    public static final int maxTreeSize = 3;
    public static final int totalDirs = 6;

    /*
        INIT. GLOBALS
     */
    public static int cellSize;
    public static Cell[] cells;

    public static final int INF = 900000000;

    /*
        PER TURN GLOBALS
     */
    public static List<Tree> myTrees = new ArrayList<>();
    public static List<Tree> oppTrees = new ArrayList<>();

    public static int day = -1, nutrients, lastDay;

    public static int mySun, oppSun;
    public static int myScore, oppScore;

    public static boolean oppWait;

    public static int[] myTreeCount = new int[maxTreeSize+1];
    public static int[] oppTreeCount = new int[maxTreeSize+1];

    public static boolean turnGiven;
    /**
     * <i>Set in turnInput()</i><br>
     */
    public static int moreDays;

    //------SHADOW VARIABLES------
    /**
     * shadowDays[dir] = number of days left with shadow in "dir" direction
     */
    public static int[] shadowDays = new int[totalDirs]; // init. in initInput()
    public static final int shadowAheadCoeff = 2*Player.maxTreeSize;
    /**
     * shadowDaysAhead[ahead][dir] = number of days left with shadow in "dir" direction
     */
    public static int[][] shadowDaysAhead = new int[shadowAheadCoeff+1][totalDirs]; // init. in initInput()

    //------SUN VARIABLES------
    public static final int sunAheadCoeff = Player.maxTreeSize + 2;
    /**
     * sunAhead[dayAhead] = <b>total suns</b> in day "day+dayAhead" <i>(assuming no change in trees)</i>
     */
    public static int[] mySunAhead = new int[sunAheadCoeff+1];
    public static int[] oppSunAhead = new int[sunAheadCoeff+1];
    public static int[] mySunSpent = new int[sunAheadCoeff+1];  //my spent sun on already chosen incPart moves

    public static int myTotalSun;
    public static int myCompleteSun;
    public static int mySeedCompleteSun;

    //------COMPLETE VARIABLES
    public static int myCompleteDays;
    public static int myCompletes;
    public static int oppCompletes;

    /*
        DIAGNOSTICS
     */
    public static final boolean showDiag = true;

    public static int myOldSun = 0, oppOldSun = 0;
    public static int mySunEarn = 0, oppSunEarn = 0;

    public static HashMap<Integer,Boolean> myFullTrees = new HashMap<>();
    public static HashMap<Integer,Boolean> oppFullTrees = new HashMap<>();

    public static int myTotalCompletes = 0, oppTotalCompletes = 0;
    public static int myCompletePts = 0, oppCompletePts = 0;
    public static int myRichPts = 0, oppRichPts = 0;

    //DEBUGGER
    public static boolean debug = false;
    public static final boolean showTreeHeurStatus = true;
    public static final boolean showSeedHeurStatus = false;
    public static final boolean showSunStatus = true;

    /*
        TUNERS
     */
    public static final boolean sunToNut = true;
    public static final double getNutRatio = 19.0/15;
    /**
     * Consider growCost[.] in growCost()
     */
    public static final boolean enableGrowCost = false;
    public static final int completePtsFrom = 3; // provides treeCompletePts from "completePtsFrom" size trees
    public static final int oppCoverAhead = 1; // inc part growth only covers opp trees of at most "oppCoverAhead" more size
    public static final int oppCoverMin = 1; // small opp cover tree size increased to oppCoverMin


    public static void main(String[] args) {
        // INIT. INPUT
        Scanner in = new Scanner(System.in);
        Input.initInput(in);

        //COMPUTE ADJ PROPERTIES
        Fx.computeAllAdjs();

        // game loop
        while (true) {
            // INPUT PRE-PROCESSING
            Input.preProcess();

            // PER TURN INPUT
            Input.turnInput(in);

            // DIAGNOSTICS UPDATE
            Diag.perTurnUpdate();

            //UPDATE SHADOWS
            Fx.shadowUpdate();

            //UPDATE SUN AHEAD
            SunFx.updateSuns(true);
            SunFx.updateSuns(false);

            if(showSunStatus) {
                Diag.printSunAhead(mySunAhead, "Sun ahead");
                Diag.printSunAhead(mySunSpent, "Sun spent");
            }

            /*---------- SENSITIVE PART ----------*/
            //STORE COMPLETE STATUS
            myCompleteDays = SimpleHeur.compactCompleteDays(true);
            myCompletes = Fx.computeComplete(true);
            oppCompletes = Fx.computeComplete(false);

            /*
                MOVE PROCESSING
             */

            HashMap<Integer, ActionStatus> mySeedList = new HashMap<>();
            HashMap<Integer, Boolean> oppSeedList = new HashMap<>();

            //SENSITIVE!
            boolean points = (moreDays <= myCompleteDays);

            int minSunPts = 0;

            for(Tree tree: oppTrees) {
                for(int seedIndex: Fx.seedCells(tree).keySet())
                    oppSeedList.put(seedIndex,true);
            }

            PriorityQueue<ActionStatus> growActions = new PriorityQueue<>((x,y)-> Double.compare(y.sunPts,x.sunPts));
            PriorityQueue<ActionStatus> seedActions = new PriorityQueue<>((x,y)-> Double.compare(y.sunPts,x.sunPts));

            /*
                GROW ACTIONS
             */
            for(Tree tree: myTrees) {
                if(tree.dormant) {
                    continue;
                }

                Cell cell = cells[tree.index];
                HashMap<Integer,Boolean> seedList = Fx.seedCells(tree);

                /*
                    IncPartHeur
                 */
                int treeCompletePts = (oppTreeCount[maxTreeSize] + (oppCompletes > 0? myTreeCount[maxTreeSize]:0))
                        * (tree.size >= completePtsFrom? 3:0);

                int lostSuns = 0;

                for(int size = tree.size+1; size <= Math.min(maxTreeSize+1,tree.size+1 + moreDays); size++) {
                    int[] sunSpend = IncPartHeur.growSunSpend(cell, true, size, false);
                    double sunPts = IncPartHeur.growTotalPts(cell, size, points, false)
                            - IncPartHeur.effGrowCost(cell, true, size, false);

                    sunPts += size > maxTreeSize? treeCompletePts:0;

                    //heuristics status print [for debugging]
                    if(showTreeHeurStatus) {
                        System.err.println("Tree.size: " + tree.index + "." + size);
                        debug = true;
                        System.err.print("SunPts: " + sunPts + " = " + IncPartHeur.growSunPts(cell,size,points,false));
                        debug = false;
                        System.err.print(" + " + IncPartHeur.growCover(cell,size,false));
                        System.err.print(" - " + IncPartHeur.effGrowCost(cell, true, size, false));
                        System.err.println(" + " + (size > maxTreeSize?treeCompletePts:0));
                        System.err.println("Eff sun pts: " + ((double)sunPts/(size-tree.size)));
                        Diag.printSunAhead(sunSpend, "sun spend");
                    }

                    int spentDays = size-tree.size;
                    //adding doable "GROW/COMPLETE" moves
                    if(IncPartHeur.growAble(sunSpend) && sunPts > minSunPts) {
                        growActions.offer(new ActionStatus(tree.index,sunPts /spentDays,sunSpend));

                        if(moreDays == (maxTreeSize-tree.size) //exactly "moreDays" days needed for tree completion
                                && size > maxTreeSize) { // completing tree was the best move
                            lostSuns = treeCompletePts;
                        }
                    }
                }

                //storing seed cells
                ActionStatus seedStatus = new ActionStatus(tree.index,lostSuns);
                for(int seed: seedList.keySet()) {
                    if(!mySeedList.containsKey(seed) || mySeedList.get(seed).sunPts > seedStatus.sunPts)
                        mySeedList.put(seed,seedStatus);
                }

            }

            //------SEED ACTIONS------
            IncPartHeur.addSeedActions(mySeedList,seedActions,points,minSunPts);

            /*
                MOVE CHOICE
             */
            ActionStatus move = new ActionStatus();

            if(!growActions.isEmpty())
                move = growActions.peek();

            if(myTreeCount[0] == 0 && !seedActions.isEmpty() && seedActions.peek().sunPts > move.sunPts)
                move = seedActions.peek();
            // adding spent suns to mySunSpent
            for(int i = 0; i <= sunAheadCoeff; i++)
                Player.mySunSpent[i] += move.sunSpend[i];

            //GAME END DIAGNOSTICS UPDATE
            if( day == Player.totalDays && (move.index == -1) )
                Diag.gameEndUpdate();

            /*
                GIVE MOVE
             */
            if(move.index == -1) // WAIT
                System.out.print("WAIT");

            else {
                int treeState = cells[move.index].treeState;

                if(treeState == 0)// SEED
                    System.out.print("SEED " + mySeedList.get(move.index).index + " " + move.index);

                else if (treeState == Player.maxTreeSize+1) // GROW
                    System.out.print("COMPLETE " + move.index);

                else // COMPLETE
                    System.out.print("GROW " + move.index);

            }

            System.out.print(showDiag?" " : "\n");

            /*
                DIAG PRINT
             */
            DecimalFormat dec = new DecimalFormat("#.#");

            PrintStream diagShow = showDiag? System.out : System.err;
            if(points)
                diagShow.print("EG; ");

            diagShow.print("S:" + mySunEarn + "," + oppSunEarn + "; ");
            diagShow.print("T:" + myTotalCompletes + "," + oppTotalCompletes + "; ");
            diagShow.print("C:" + dec.format((double)myCompletePts/myTotalCompletes) +
                    "," + dec.format((double)oppCompletePts/oppTotalCompletes) + "; ");
            diagShow.print("R:" + dec.format((double)myRichPts/myTotalCompletes) +
                    "," + dec.format((double)oppRichPts/oppTotalCompletes) + "; ");

            diagShow.println();

            /*
                DEBUG PRINT
             */
            System.err.println("Nut: " + nutrients);
            System.err.println("My complete days: " + myCompleteDays);
            Diag.printSunAhead(mySunSpent, "sun spent: ");
        }
    }
}