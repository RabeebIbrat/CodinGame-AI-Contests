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
