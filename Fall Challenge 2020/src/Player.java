import java.util.*;

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

