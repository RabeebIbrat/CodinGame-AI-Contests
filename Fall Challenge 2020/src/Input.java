import java.util.*;

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


