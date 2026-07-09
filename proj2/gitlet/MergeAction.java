package gitlet;

public enum MergeAction {
    OVERWRITE_FROM_TARGET, // add file to staging
    REMOVE,
    CONFLICT,
    DO_NOTHING
}
