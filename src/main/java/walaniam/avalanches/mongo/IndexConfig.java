package walaniam.avalanches.mongo;

record IndexConfig(
    String name,
    boolean ascending,
    boolean unique
) {

    String mongoName() {
        var suffix = ascending ? "_1" : "_-1";
        return name + suffix;
    }
}
