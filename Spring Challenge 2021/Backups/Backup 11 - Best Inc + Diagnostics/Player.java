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

class TreeState {
    int index, suns;

    public TreeState(int index, int suns) {
        this.index = index;
        this.suns = suns;
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
        for(int ahead = 0; ahead <= Player.maxTreeSize+1; ahead++) {
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

        for(int i = 0; i <= Player.sunAheadCoeff; i++) {
            Player.mySunAhead[i] = 0;
            Player.oppSunAhead[i] = 0;
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

            for(int ahead = 0; ahead <= Player.maxTreeSize + 1; ahead++) {
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

    public static List<Integer> seedCells(Tree tree) {
        List<Integer> seeds = new ArrayList<>();

        if(tree.dormant) {
            return seeds;
        }

        for(int seedIndex: Player.cells[tree.index].adjSeed[tree.size].keySet()) {
            if(Player.cells[seedIndex].treeState == 0 && Player.cells[seedIndex].richness > 0)
                seeds.add(seedIndex);
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

    public static void updateSunAhead(boolean mine) {
        List<Tree> trees = mine? Player.myTrees : Player.oppTrees;
        int[] sunAhead = mine? Player.mySunAhead : Player.oppSunAhead;

        sunAhead[0] = mine? Player.mySun: Player.oppSun;
        int nowDir = Player.day;
        for(int ahead = 1; ahead <= Player.sunAheadCoeff; ahead++) {
            nowDir = (++nowDir)%Player.totalDirs;
            for(Tree tree: trees) {
                if(Player.cells[tree.index].shadowCount[tree.size][nowDir] == 0)
                    sunAhead[ahead] += tree.size;
            }
            sunAhead[ahead] += sunAhead[ahead-1];
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
     * for incrementally growing/completing "tree in cell" <b>if no change in trees</b><br>
     * <i>An improved lower bound of cover of suns</i>
     */
    public static int growCover(Cell cell) {
        int mySign = (cell.treeState >= 0)? 1:-1;
        int mySize = Math.abs(cell.treeState) - 1;

        int sunCover = 0;

        int dayPassed = 1, treeLen = Math.min(mySize+1, Player.maxTreeSize);

        for(; treeLen < Math.min(Player.maxTreeSize,mySize+1 + Player.moreDays);
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

class IncPartHeur {
    /*
        growSunPts, growCover, growTotalPts, growCost, (growSeedCost), effBlanks/maxEffBlanks
        updateSpendSunAhead
        --> set minPts = 0
     */
}

class Player {
    /*
        STATIC PARAMETERS
     */
    public static final int[] growCost = {1,3,7,4};
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
    /**
     * shadowDaysAhead[ahead][dir] = number of days left with shadow in "dir" direction
     */
    public static int[][] shadowDaysAhead = new int[maxTreeSize+2][totalDirs]; // init. in initInput()

    //------SUN VARIABLES------
    public static final int sunAheadCoeff = Player.maxTreeSize + 2;
    /**
     * sunAhead[dayAhead] = <b>total suns</b> in day "day+dayAhead" <i>(assuming no change in trees)</i>
     */
    public static int[] sunAhead = new int[sunAheadCoeff+1];
    public static int[] mySunAhead = new int[sunAheadCoeff+1];
    public static int[] oppSunAhead = new int[sunAheadCoeff+1];

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
    public static final boolean showHeurStatus = false;

    /*
        TUNERS
     */
    public static final double simpleSeedEff = 0.2;  //obsolete -> SimpleHeur
    public static final double oppSeedProb = 0;  //0 to inactivate
    /**
     * Consider growCost[.] in growCost()
     */
    public static final boolean enableGrowCost = false;

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
            SunFx.updateSunAhead(true);
            SunFx.updateSunAhead(false);

            /*
                MOVE PROCESSING
             */

            /*---------- SENSITIVE PART ----------*/
            //int myCompleteDays = IncHeur.linearCompleteDays(true);
            int myCompleteDays = SimpleHeur.compactCompleteDays(true);
            int myCompletes = Fx.computeComplete(true);
            int oppCompletes = Fx.computeComplete(false);

            HashMap<Integer, TreeState> mySeedList = new HashMap<>();
            HashMap<Integer, Boolean> oppSeedList = new HashMap<>();

            boolean points = (moreDays <= myCompleteDays);

            int seedCost = myTreeCount[0], seedIncCost = IncHeur.growSeedCost(true,points);
            boolean seedAble = (mySun >= seedCost),
                    oppSeedAble = (mySun >= Player.oppTreeCount[0]);
            boolean seedIncAble = (mySun >= seedIncCost),
                    oppSeedIncAble = (oppSun >= IncHeur.growSeedCost(false, points));

            int maxSunPts = -1, moveIndex = -1, moveSunCost = -1;

            for(Tree tree: oppTrees) {
                for(int seedIndex: Fx.seedCells(tree))
                    oppSeedList.put(seedIndex,true);
            }

            /*
                GROW CHOICE
             */
            for(Tree tree: myTrees) {
                if(tree.dormant)
                    continue;
                Cell cell = cells[tree.index];

                /*
                    TRY "IncHeur" IF POSSIBLE
                 */
                int sunCost = IncHeur.growCost(cell, true, points, true, false); // actual cost
                int sunEffCost = IncHeur.growCost(cell, true, points, false, false);
                int sunPts = (tree.size == maxTreeSize)? oppTreeCount[maxTreeSize]*3 : 0;

                //my lost points for opp growing trees
                if(tree.size == maxTreeSize && oppCompletes > 0)  //does opp really grow trees?
                    sunPts += myTreeCount[maxTreeSize]*3;

                TreeState treeState;

                if(sunCost <= mySun) {
                    sunPts += IncHeur.growTotalPts(cell, points) - sunEffCost;
                    treeState = new TreeState(tree.index, sunPts);

                    if(showHeurStatus) {
                        System.err.println("Tree " + tree.index);
                        System.err.println("Grow inc sun pts: " + IncHeur.growSunPts(cell, points));
                        System.err.println("Grow inc cover: " + IncHeur.growCover(cell));
                        System.err.println("Sun inc (eff) cost: " + sunEffCost);
                        System.err.println("Total = " + sunPts);
                        System.err.println();
                    }
                }
                /*
                    ELSE TRY "SimpleHeur"
                 */
                else {
                    sunCost = SimpleHeur.growCost(cell, true, true, false);
                    sunEffCost = SimpleHeur.growCost(cell, true, false, false);
                    sunPts += SimpleHeur.growTotalPts(cell, points) - sunEffCost;

                    if(sunCost <= mySun)
                        treeState = new TreeState(tree.index, sunPts);
                    else
                        treeState = new TreeState(tree.index, -INF);

                    if(showHeurStatus) {
                        System.err.println("Tree " + tree.index);
                        System.err.println("Grow sun pts: " + SimpleHeur.growSunPts(cell, points));
                        System.err.println("Grow cover: " + SimpleHeur.growCover(cell));
                        System.err.println("Sun (eff) cost: " + sunEffCost);
                        System.err.println("Total = " + sunPts);
                        System.err.println();
                    }
                }

                if(seedAble) {
                    List<Integer> seeds = Fx.seedCells(tree);
                    for(int seed: seeds) {
                        if(!mySeedList.containsKey(seed) || mySeedList.get(seed).suns > treeState.suns)
                            mySeedList.put(seed, treeState);
                    }
                }

                if(sunPts > maxSunPts && sunCost <= mySun) {
                    maxSunPts = sunPts;
                    moveIndex = tree.index;
                    moveSunCost = sunCost;
                }
            }

            /*
                SEED CHOICE
             */
            if(seedAble /*&& !points*/) {  //FORCE SEED OFF: INACTIVATED
                for(int index : mySeedList.keySet()) {
                    Cell seed = cells[index];
                    int sunPts;

                    if(seedIncAble) {
                        if(oppSeedList.containsKey(index) && oppSeedIncAble)
                            sunPts = (int) Math.round((1 + oppSeedProb) * IncHeur.growSunPts(seed, points)
                                    + (1 - oppSeedProb) * IncHeur.growCover(seed));
                        else
                            sunPts = IncHeur.growTotalPts(seed, points);

                        sunPts -= seedIncCost;
                        //if(moreDays >= myCompleteDays) // points for completing this tree added
                        //sunPts += ( Math.max(0,nutrients-myCompletes-oppCompletes) + richPts[seed.richness] )*3;

                        if(showHeurStatus) {
                            System.err.println("Seed " + index);
                            System.err.println("Seed inc sun pts: " + IncHeur.growSunPts(seed, points));
                            System.err.println("Seed inc cover pts: " + IncHeur.growCover(seed));
                            System.err.println("Seed inc cost: " + seedIncCost);
                            System.err.println("Total = " + sunPts);
                            System.err.println();
                        }
                    }

                    else {
                        sunPts = -seedCost;

                        if(showHeurStatus) {
                            System.err.println("Seed " + index);
                            System.err.println("Seed cost: " + seedCost);
                            System.err.println("Total = " + sunPts);
                            System.err.println();
                        }
                    }

                    if(seedCost <= mySun && ( sunPts > maxSunPts || sunPts == maxSunPts && moveIndex > 0 &&
                            IncHeur.maxEffBlanks(cells[index],false).size() >
                                    IncHeur.maxEffBlanks(cells[moveIndex], false).size() )) {
                        maxSunPts = sunPts;
                        moveIndex = index;
                        moveSunCost = seedIncAble? seedIncCost : seedCost;
                    }
                }
            }

            //GAME END DIAGNOSTICS UPDATE
            if( day == Player.totalDays && (moveIndex == -1 || moveSunCost > mySun) )
                Diag.gameEndUpdate();

            /*
                GIVE MOVE
             */
            if(moveIndex == -1 || moveSunCost > mySun)
                System.out.print("WAIT");
            else {
                int treeState = cells[moveIndex].treeState;

                if (treeState == 0)
                    System.out.print("SEED " + mySeedList.get(moveIndex).index + " " + moveIndex);
                else if (treeState == Player.maxTreeSize+1)
                    System.out.print("COMPLETE " + moveIndex);
                else
                    System.out.print("GROW " + moveIndex);
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
        }
    }
}