package com.platform.common.ds.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DsResult<T> {

    private int code;
    private String msg;

    @JsonProperty("data")
    private T data;

    public boolean isSuccess() {
        return code == 0;
    }

    public static <T> DsResult<T> ok() {
        DsResult<T> r = new DsResult<>();
        r.setCode(0);
        r.setMsg("success");
        return r;
    }

    public static <T> DsResult<T> fail(String message) {
        DsResult<T> r = new DsResult<>();
        r.setCode(-1);
        r.setMsg(message);
        return r;
    }
}
