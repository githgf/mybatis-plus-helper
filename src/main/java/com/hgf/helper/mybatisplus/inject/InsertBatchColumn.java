package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;

public class InsertBatchColumn extends InsertBatchSomeColumn {

    public InsertBatchColumn() {

        super(tableFieldInfo -> tableFieldInfo.getInsertSqlColumnMaybeIf(null) != null);
    }
}
