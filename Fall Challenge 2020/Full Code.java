import java.util.Comparator;
import java.util.*;
import java.util.*;
import java.util.*;
import java.util.*;
class BrewPlan {
    public SpellCombo brewPath;
    public Action brew;

    public BrewPlan() {
    }

    public BrewPlan(SpellCombo brewPath, Action brew) {
        this.brewPath = brewPath;
        this.brew = brew;
    }
}

class PlanChoice implements Comparator<BrewPlan> {

    public static final PlanChoice obj = new PlanChoice();

    @Override
    public int compare(BrewPlan a1, BrewPlan a2) {
        if(a1.brewPath.moveCount == a2.brewPath.moveCount)
            return a2.brew.price - a1.brew.price;

        if(a1.brewPath.moveCount == 0 || a2.brewPath.moveCount == 0)
            return a1.brewPath.moveCount - a2.brewPath.moveCount;

        double rupeePerMove1, rupeePerMove2;
        rupeePerMove1 = (double) a1.brew.price/a1.brewPath.moveCount;
        rupeePerMove2 = (double) a2.brew.price/a2.brewPath.moveCount;
        if(rupeePerMove1 != rupeePerMove2)
            return Double.compare(rupeePerMove2, rupeePerMove1);
        else
            return a1.brewPath.moveCount - a2.brewPath.moveCount;
    }
}

class ReqChoice implements Comparator<Delta> {  //choosing best requirement delta

    public static final ReqChoice obj = new ReqChoice();

    @Override
    public int compare(Delta t1, Delta t2) {
        if(t1.sum() != t2.sum()) {
            return t1.sum() - t2.sum();
        }
        for(int i = K.itemTypes-1; i > 0; i--) {
            if(t1.index[i] != t2.index[i])
                return t1.index[i]-t2.index[i];
        }
        return t1.index[0]-t2.index[0];
    }
}

class BrewChoice implements Comparator<Action> {

    public static final BrewChoice obj = new BrewChoice();

    @Override
    public int compare(Action t1, Action t2) {
        if(t1.price != t2.price)
            return t2.price - t1.price;

        int items1 = t1.delta.sum(), items2 = t2.delta.sum();
        if(items1 != items2)
            return items1 - items2;

        return t1.actionID - t2.actionID;
    }
}
interface Copyable<Type> {
    public Type copy();
}


class Fx {
    /**
     * Needs set: <b>K.spells, Player.spells</b>
     */
    public static HashMap<ComboKey, SpellCombo> generateCombos() {
        HashMap<ComboKey, SpellCombo> combos = new HashMap<>();

        Queue<SpellCombo> comboQueue = new LinkedList<>();
        comboQueue.add(new SpellCombo());
        comboQueue.add(null);  //depth separator


        SpellCombo nowCombo, genCombo;
        int depth = 0;
        while(depth < K.comboDepth) {
            nowCombo = comboQueue.remove();
            if(nowCombo == null) { //depth separator reached
                depth++;
                comboQueue.add(null);  //adding depth separator for new depth
                continue;
            }
            for(int i = 0;  i < K.spells; i++) {
                genCombo = nowCombo.copy().addSpell(Player.spells.get(i), i, nowCombo);
                if(genCombo != null) {  //if valid new combo is generated
                    if (genCombo.isBetterEqual( combos.get(genCombo.getKey()) )) {
                        combos.put(genCombo.getKey(), genCombo);
                        comboQueue.add(genCombo);
                    }
                }
            }
            //each new spell added
        }
        //all depths produced
        return combos;
    }

    public static Queue<Delta> generateParts(int sum) {
        Queue<Delta> partitions = new LinkedList<>();
        partitions.add(new Delta());
        generateParts(partitions, sum, 0);
        return partitions;
    }

    private static void generateParts(Queue<Delta> partitions, int sum, int startIndex) {
        if(startIndex == K.itemTypes-1) {
            for(Delta split: partitions) {
                split.index[startIndex] = sum - split.sum();
            }
            return;
        }

        partitions.add(null);  //end boundary

        Delta now, update;
        while(true) {
            now = partitions.remove();
            if(now == null)  //boundary reached
                break;
            for(int i = 0; i <= sum-now.sum(); i++) {
                update = now.copy();
                update.index[startIndex] = i;
                partitions.add(update);
            }
        }
        generateParts(partitions, sum,startIndex+1);
    }

    public static TreeSet<Delta> generateSubsets(Delta has) {
        Queue<Delta> subsets = new LinkedList<>();
        subsets.add(new Delta());
        generateSubsets(subsets, has, 0);

        TreeSet<Delta> subsetsArr = new TreeSet<>(ReqChoice.obj);
        while(!subsets.isEmpty()) {
            subsetsArr.add(subsets.remove());
        }
        return subsetsArr;
    }

    private static void generateSubsets(Queue<Delta> subParts, Delta has, int startIndex) {

        subParts.add(null);  //end boundary

        Delta now, update;
        while(true) {
            now = subParts.remove();
            if(now == null)  //boundary reached
                break;
            for(int i = 0; i <= has.index[startIndex]; i++) {
                update = now.copy();
                update.index[startIndex] = i;
                subParts.add(update);
            }
        }

        if(startIndex != K.itemTypes-1)
            generateSubsets(subParts, has, startIndex+1);
    }
}

class Action implements Copyable<Action> {
    public int actionID;
    public String actionType;
    public Delta delta;
    public int price;
    public int tomeIndex;
    public int taxCount;
    public boolean castable;
    public boolean repeatable;

    public Action(){}  //empty constructor

    public Action(int actionID, String actionType, int[] delta, int price, int tomeIndex, int taxCount, boolean castable, boolean repeatable) {
        this.actionID = actionID;
        this.actionType = actionType;
        this.delta = new Delta(delta);
        this.price = price;
        this.tomeIndex = tomeIndex;
        this.taxCount = taxCount;
        this.castable = castable;
        this.repeatable = repeatable;
    }

    @Override
    public Action copy() {
        Action action = new Action();

        action.actionID = actionID;
        action.actionType = actionType;
        if(delta != null)  action.delta = delta.copy();
        else  action.delta = null;
        action.price = price;
        action.tomeIndex = tomeIndex;
        action.taxCount = taxCount;
        action.castable = castable;
        action.repeatable = repeatable;

        return action;
    }

    public static Action findAction(ArrayList<Action> list, int actionID) {
        Action retAction = null;
        for(Action action: list) {
            if (action.actionID == actionID)
                return action;
        }
        return null;
    }
}


class Delta implements Copyable<Delta> {
    public int[] index;

    public Delta() {
        index = new int[K.itemTypes];  //all entries 0 by default
    }

    public Delta(int[] index) {
        this.index = index;
    }

    @Override
    public Delta copy() {
        Delta copy = new Delta();
        System.arraycopy(index, 0, copy.index, 0, K.itemTypes);
        return copy;
    }

    public int sum() {
        int sum = 0;
        for(int i = 0; i < K.itemTypes; i++)
            sum += index[i];
        return sum;
    }

    public Delta add(Delta other) {
        for(int i = 0; i < K.itemTypes; i++) {
            index[i] += other.index[i];
        }
        return this;
    }

    public Delta sub(Delta other) {
        for(int i = 0; i < K.itemTypes; i++) {
            index[i] -= other.index[i];
        }
        return this;
    }

    public Delta abs(boolean positive) {
        int signChange = positive? 1: -1;
        for(int i = 0; i < K.itemTypes; i++) {
            if(index[i]*signChange < 0)
                index[i] = 0;
        }
        return this;
    }

    public Delta abs() {  //positive only
        return abs(true);
    }

    public Delta negate() {
        for(int i = 0; i < K.itemTypes; i++) {
            index[i] = -index[i];
        }
        return this;
    }

    public boolean isSubsetOf(Delta right) {  //positive only
        for(int i = 0; i < K.itemTypes; i++) {
            if(index[i] > right.index[i])
                return false;
        }
        return true;
    }

    public boolean isSuperSetOf(Delta right) {  //positive only
        return right.isSubsetOf(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Delta delta1 = (Delta) o;
        return Arrays.equals(index, delta1.index);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(index);
    }

    @Override
    public String toString() {
        return "Delta{" +
                "index=" + Arrays.toString(index) +
                '}';
    }
}


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

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    //inputs
    public static TreeSet<Action> brews = new TreeSet<>(BrewChoice.obj);
    public static ArrayList<Action> spells = new ArrayList<>();;
    public static Delta myStore, oppStore;
    public static  int myScore, oppScore;

    //pre-processing
    public static HashMap<ComboKey, SpellCombo> combos;
    public static Queue<Delta>[] partitions = (Queue<Delta>[])new Queue[K.maxScale+1];

    //moves
    public static Stack<SpellCombo> brewPath = new Stack<>();
    //public static Action brewNext = null;  //coupled with brewPath
    public static PriorityQueue<BrewPlan> brewPlans = new PriorityQueue<>(PlanChoice.obj);

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        boolean firstMove = true;

        // game loop
        nextMove:
        while (true) {

            /*
             * CLEARING VARIABLES
             */

            brews.clear();
            spells.clear();
            brewPath.clear();
            brewPlans.clear();


            int actionCount = in.nextInt(); // the number of spells and recipes in play
            for (int i = 0; i < actionCount; i++) {
                int actionId = in.nextInt(); // the unique ID of this spell or recipe
                String actionType = in.next(); // in the first league: BREW; later: CAST, OPPONENT_CAST, LEARN, BREW

                int[] delta = new int[K.itemTypes];
                for(int j = 0; j < K.itemTypes; j++) {
                    delta[j] = in.nextInt(); // tier-j ingredient change
                }
                int price = in.nextInt(); // the price in rupees if this is a potion
                int tomeIndex = in.nextInt(); // in the first two leagues: always 0; later: the index in the tome if this is a tome spell, equal to the read-ahead tax; For brews, this is the value of the current urgency bonus
                int taxCount = in.nextInt(); // in the first two leagues: always 0; later: the amount of taxed tier-0 ingredients you gain from learning this spell; For brews, this is how many times you can still gain an urgency bonus
                boolean castable = in.nextInt() != 0; // in the first league: always 0; later: 1 if this is a castable player spell
                boolean repeatable = in.nextInt() != 0; // for the first two leagues: always 0; later: 1 if this is a repeatable player spell

                Action action = new Action(actionId, actionType, delta, price, tomeIndex, taxCount, castable, repeatable);

                if(actionType.equals("BREW")) {
                    action.delta.negate();  //brew.delta -> positive
                    brews.add(action);
                }
                else if(actionType.equals("CAST"))
                    spells.add(action);
            }

            if(firstMove) {  //spell dependent inits.
                K.spells = spells.size();
                K.downMoveStart = K.comboDepth + K.comboDepth/K.spells;  //min. needed rests added
            }

            int[] inv = new int[K.itemTypes];
            for(int i = 0; i < K.itemTypes; i++) {
                inv[i] = in.nextInt(); // tier-i ingredients in my inventory
            }
            myStore = new Delta(inv);
            myScore = in.nextInt(); // amount of my rupees

            inv = new int[K.itemTypes];
            for(int i = 0; i < K.itemTypes; i++) {
                inv[i] = in.nextInt(); // tier-i ingredients in opp inventory
            }
            oppStore = new Delta(inv);
            oppScore = in.nextInt(); // amount of opp rupees

            /*
             * PREPROCESSING
             */

            if(firstMove) {
                combos = Fx.generateCombos();

                for(int i = 0; i < partitions.length; i++) {
                    partitions[i] = Fx.generateParts(i);
                }
            }

            firstMove = false;

            /*
             * MOVE!
             */

            // after first league: BREW <id> | CAST <id> [<times>] | LEARN <id> | REST | WAIT

            System.err.println("IMM BREW");

            //try immediate brewing
            for(Action brew: brews) {
                if(brew.delta.isSubsetOf(myStore)) {
                    System.out.println("BREW " + brew.actionID);
                    continue nextMove;
                }
            }

            TreeSet<Delta> requires = Fx.generateSubsets(myStore);  //positive

            /*
             * UPSCALE SEARCH -> Complete brew plan
             * 1. move count
             * 2. upscale [0 incl.]
             * 3. requires
             * 4. needed space
             * return at first find [for now]
             *
             * [Done for every brew]
             */

            System.err.println("UPSCALE");

            for(Action brew: brews) {
                Delta mustProduce = brew.delta.copy().sub(myStore).abs();
                SpellCombo searchResult = null;
                searchPlan:
                for(int move = 1; move <= K.moveDepth; move++) {
                    for(int up = Math.min(K.upScale, K.storage-brew.delta.sum()); up >= 0; up--) {
                        for(Delta upDelta: partitions[up]) {
                            for(Delta req: requires) {
                                for(int spaceReq = 0; spaceReq <= K.storage-myStore.sum(); spaceReq++) {
                                    //innermost loop
                                    //parameters: move, upDelta, req, spaceReq
                                    //ComboKey(requires, produces, needSpace, moveCount)
                                    ComboKey look = new ComboKey(req, mustProduce.copy().add(upDelta), spaceReq, move);
                                    if( (searchResult = combos.get(look)) != null )
                                        break searchPlan;
                                }
                            }
                        }
                    }
                }
                //end of searchPlan
                //brew path found/not found
                if(searchResult != null) {
                    brewPlans.add(new BrewPlan(searchResult,brew));
                }
            }

            //try 1st step of upscale search result
            if(!brewPlans.isEmpty()) {
                SpellCombo spellNode = brewPlans.peek().brewPath;
                giveMove(spellNode);
                continue;
            }

            /*
             * DOWNSCALE SEARCH -> Partial brew plan
             * 1. downscale
             * 2. move count
             * 3. brew
             * 4. requires
             * 5. needed space
             */

            System.err.println("DOWNSCALE");

            SpellCombo searchResult = null;
            Action chosenBrew = null;
            searchPlan:
            for(int down = 1; down <= K.downScale; down++) {
                for (Delta downDelta : partitions[down]) {
                    for (int move = K.downMoveStart; move <= K.moveDepth; move++) {
                        for (Action brew : brews) {
                            Delta mustProduce = brew.delta.copy().sub(myStore).abs();
                            if(mustProduce.sum() < down)
                                continue;
                            for (Delta req : requires) {
                                for (int spaceReq = 0; spaceReq <= K.storage - myStore.sum(); spaceReq++) {
                                    //innermost loop
                                    //parameters: move, downDelta, req, spaceReq
                                    //ComboKey(requires, produces, needSpace, moveCount)
                                    ComboKey look = new ComboKey(req, mustProduce.copy().sub(downDelta), spaceReq, move);
                                    if ((searchResult = combos.get(look)) != null) {  //non-absolute "produces" always gives null
                                        chosenBrew = brew;
                                        break searchPlan;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //end of searchPlan
            //brew path found/not found

            //try 1st step of downscale search result
            if(searchResult != null) {
                giveMove(searchResult);
                continue;
            }

        }
        //end of game loop
    }

    private static void giveMove(SpellCombo spellNode) {
        brewPath.add(spellNode);
        while(brewPath.peek().parent.spell != null) {
            brewPath.push(brewPath.peek().parent);
        }
        spellNode = brewPath.peek();
        Action currentSpell = null;
        for(Action spell: spells) {
            if(spell.actionID == spellNode.spell.actionID) {
                currentSpell = spell;
                break;
            }
        }
        if(currentSpell == null) {
            System.err.println("Error: Spell not found");
            System.out.println("REST");
            return;
        }
        if(currentSpell.castable) {
            if(currentSpell.repeatable) {
                int castCount = 0;
                while (!brewPath.isEmpty() && brewPath.peek().spell.actionID == currentSpell.actionID) {
                    brewPath.pop();
                    castCount++;
                }
                System.out.println("CAST " + currentSpell.actionID + " " + castCount);
            }
            else
                System.out.println("CAST " + currentSpell.actionID);
        }
        else
            System.out.println("REST");
    }
}


class SpellCombo implements Copyable<SpellCombo> {
    public Action spell;
    public SpellCombo parent;
    public boolean[] usedSpell;  //size must be K.spells

    /*
     * UNIQUENESS PARAMETERS
     */
    public Delta requires;  // [negative] >= -K.storage
    public Delta produces;  // [non-absolute] <= maxSpace
    public int needSpace;  // <= K.storage
    public int moveCount;  //includes rest

    public SpellCombo() {  //default/empty spell combo
        spell = null;
        parent = null;
        usedSpell = new boolean[K.spells];
        requires = new Delta();
        produces = new Delta();
        needSpace = 0;
        moveCount = 0;
    }

    @Override
    public SpellCombo copy() {
        SpellCombo copy = new SpellCombo();

        if(spell != null)  copy.spell = spell.copy();
        if(parent != null) copy.parent = parent.copy();
        for(int i = 0; i < K.spells; i++) {
            copy.usedSpell[i] = usedSpell[i];
        }
        copy.requires = requires.copy();
        copy.produces = produces.copy();
        copy.needSpace = needSpace;
        copy.moveCount = moveCount;

        return copy;
    }

    public SpellCombo addSpell(Action newSpell, int spellID, SpellCombo parent) {  //modifies existing copy
        this.parent = parent;
        spell = newSpell;
        if(usedSpell[spellID]) {
            moveCount++;  //needs to rest one turn
            for(int i = 0; i < K.spells; i++) {
                usedSpell[i] = false;
            }
        }
        usedSpell[spellID] = true;
        for(int i = 0;  i < K.itemTypes; i++) {
            produces.index[i] += newSpell.delta.index[i];
            if(produces.index[i] < requires.index[i]) {
                requires.index[i] = produces.index[i];
            }
        }
        if(requires.sum() < -K.storage)  //cannot require no. of elements more than storage
            return null;

        int nowSpace = produces.sum();
        if(nowSpace > K.storage)  //cannot store no. of elements more than storage
            return null;
        if(nowSpace > needSpace)
            needSpace = nowSpace;

        if(!spell.repeatable || spell.actionID != parent.spell.actionID)
            moveCount++;  //this spell added
        return this;
    }
    /**
     * Makes <b> requires -> positive </b> <br>
     * Makes <b> produces -> absolute </b>
     */
    public final ComboKey getKey() {
        return new ComboKey(requires.copy().negate(), produces.copy().abs(), needSpace, moveCount);
    }

    public boolean isBetterEqual(SpellCombo right) {
        /*
        if (right == null)
            return true;
        return this.usedSpells() < right.usedSpells();
        */
        return right==null;  //overwrite off
    }

    public int usedSpells() {
        int used = 0;
        for(int i = 0; i < K.spells; i++) {
            if (usedSpell[i]) used++;
        }
        return used;
    }
}

class ComboKey {
    public Delta requires;  // [positive] <= K.storage
    public Delta produces;  // [absolute] <= maxSpace
    public int needSpace;  // <= K.storage
    public int moveCount;  //includes rest

    public ComboKey(Delta requires, Delta produces, int needSpace, int moveCount) {
        this.requires = requires;
        this.produces = produces;
        this.needSpace = needSpace;
        this.moveCount = moveCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComboKey comboKey = (ComboKey) o;
        return needSpace == comboKey.needSpace &&
                moveCount == comboKey.moveCount &&
                Objects.equals(requires, comboKey.requires) &&
                Objects.equals(produces, comboKey.produces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requires, produces, needSpace, moveCount);
    }
}

