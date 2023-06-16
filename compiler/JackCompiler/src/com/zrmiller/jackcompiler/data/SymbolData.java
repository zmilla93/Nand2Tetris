package com.zrmiller.jackcompiler.data;

import com.zrmiller.jackcompiler.enums.SymbolKind;

public class SymbolData {

    public final String type;
    public final SymbolKind kind;
    public final int index;

    public SymbolData(String type, SymbolKind kind, int index) {
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

}
