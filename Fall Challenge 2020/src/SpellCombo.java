import java.util.*;

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
