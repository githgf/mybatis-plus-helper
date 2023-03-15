package com.hgf.helper.mybatisplus.helper;

import com.hgf.helper.mybatisplus.utils.CollectionUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SelectBuilder implements Serializable {
    private static final long serialVersionUID = -1536422416594422874L;

    private List<String> parts;

    public SelectBuilder() {
        this.parts = new ArrayList<>();
    }

    public void toEmpty() {
        parts.clear();
    }

    /**
     * 置 null
     *
     * @since 3.3.1
     */
    public void toNull() {
        parts = null;
    }

    public String getStringValue(){
        if (CollectionUtil.isNotEmpty(parts)) {
            return String.join(",", parts);
        }else {
            return null;
        }
    }

}
