import java.util.Comparator;

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