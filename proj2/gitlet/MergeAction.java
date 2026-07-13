package gitlet;

public enum MergeAction {
    OVERWRITE_FROM_TARGET, // add file to staging
    CONFLICT,
    DO_NOTHING
}
