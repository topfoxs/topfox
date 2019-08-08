package com.topfox.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Setter
@Getter
@Accessors(chain = true)
public class FeignResponse<T> extends Response<T> {

    public FeignResponse(){
        super();
    }

    public FeignResponse(String code, String msg) {
        super(code, msg);
    }

    public FeignResponse(ResponseCode responseCode) {
        super(responseCode);
    }

    public FeignResponse(ResponseCode responseCode, String msg) {
        super(responseCode, msg);
    }

    public FeignResponse(Throwable e, ResponseCode responseCode) {
        super(e, responseCode);
    }
    public FeignResponse(Throwable e, ResponseCode responseCode, String msg) {
        super(e, responseCode, msg);
    }
    public FeignResponse(Throwable e) {
        super(e);
    }

    public FeignResponse(T data) {
        super(data);
    }

    public FeignResponse(T data, Integer totalCount) {
        super(data, totalCount);
    }
}
