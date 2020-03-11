package com.zrmiller.jackcompiler.enums;

public enum SymbolKind {

    STATIC, FIELD, ARG, VAR;

    public Segment toSegment() {
        //TODO : important! check this!
        switch (this) {
            case STATIC:
                return Segment.STATIC;
            case FIELD:
                return Segment.THIS;
            case ARG:
                return Segment.ARG;
            case VAR:
                return Segment.LOCAL;
            default:
                return Segment.UNKNOWN;
        }

    }

}
