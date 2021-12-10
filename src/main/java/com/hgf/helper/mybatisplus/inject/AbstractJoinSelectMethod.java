package com.hgf.helper.mybatisplus.inject;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;

public abstract class AbstractJoinSelectMethod extends AbstractMethod {
    public static final String JOIN_SQL = "joinSql";

    public String getJoinSql(){
        return String.format("${%s.%s}", WRAPPER, JOIN_SQL);
    }

    public String sqlScriptTemp() {
        return "<script>%s SELECT %s FROM %s %s  %s %s\n</script>";
    }


}
