import java.util.*;

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
